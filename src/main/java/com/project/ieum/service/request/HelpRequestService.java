package com.project.ieum.service.request;

import com.project.ieum.dto.HelpRequestCardView;
import com.project.ieum.dto.HelpRequestCreateForm;
import com.project.ieum.dto.HelpRequestDetailView;
import com.project.ieum.entity.request.HelpRequest;
import com.project.ieum.entity.request.HelpRequestStatus;
import com.project.ieum.entity.request.ServiceCategory;
import com.project.ieum.entity.user.UserProfile;
import com.project.ieum.exception.*;
import com.project.ieum.repository.HelpRequestRepository;
import com.project.ieum.repository.ServiceCategoryRepository;
import com.project.ieum.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// 도움 요청(HelpRequest) 이용자 측 유스케이스. 컨트롤러→서비스→리포지토리 경계에서
// 트랜잭션·검증·권한을 전담한다(컨트롤러는 얇게, 리포지토리는 영속성만).
//
// 도메인 규칙 요약:
//  · write-once: 본문 수정 없음. 가변은 status 전이뿐.
//  · 작성 시 같은 요청자의 활성 요청과 시간대가 겹치면 거부(existsOverlapping).
//  · close = 게시자 && OPEN(매칭 전 취소). delete = 게시자 && CLOSED(2단계 삭제의 2단계).
@Slf4j
@Service
@RequiredArgsConstructor
public class HelpRequestService {

    // 생성자 주입(@RequiredArgsConstructor + final). 필드 주입 금지(CLAUDE.md §6).
    private final HelpRequestRepository helpRequestRepository;
    private final UserProfileRepository userProfileRepository;     // requesterId → UserProfile(요청자)
    private final ServiceCategoryRepository serviceCategoryRepository;

    // 시간대 겹침 검사 대상 = "활성" 상태만(종료 상태 COMPLETED/CLOSED는 충돌 대상이 아니다).
    private static final List<HelpRequestStatus> ACTIVE_STATUSES =
        List.of(HelpRequestStatus.OPEN, HelpRequestStatus.MATCHED, HelpRequestStatus.IN_PROGRESS);

    // 작성
    // 작성자는 로그인 이용자(requesterId). status는 폼에서 받지 않고 서버가 OPEN으로 고정한다.
    // 시간 순서(start<=end)는 폼 @AssertTrue가 1차 검증, 엔티티 @PrePersist가 한번 더 검증
    // 반환값 = 저장된 id(컨트롤러 PRG redirect 대상).
    @Transactional
    public Long post(HelpRequestCreateForm form, Long requesterId) {
        UserProfile requester = userProfileRepository.getReferenceById(requesterId);

        if (helpRequestRepository.existsOverlapping(
            requester,
            form.getDesiredStartDatetime(),
            form.getDesiredEndDatetime(),
            ACTIVE_STATUSES)) {
            throw new RequestTimeConflictException();
        }

        ServiceCategory serviceCategory = serviceCategoryRepository
            .findById(form.getServiceCategoryId())
            .orElseThrow(() -> new BadRequestException("등록되지 않은 서비스 분류입니다."));

        return helpRequestRepository.save(HelpRequest.builder()
                .requester(requester)
                .serviceCategory(serviceCategory)
                .title(form.getTitle())
                .body(form.getBody())
                .specialNotes(form.getSpecialNotes())
                .desiredStartDatetime(form.getDesiredStartDatetime())
                .desiredEndDatetime(form.getDesiredEndDatetime())
                .roadAddress(form.getRoadAddress())
                .addressDetail(form.getAddressDetail())
                .sido(form.getSido())
                .sigungu(form.getSigungu())
                .bname(form.getBname())
                .build())
            .getId();
    }

    // 상세 조회
    // viewerId는 권한 계산(canClose/canDelete)에만 쓰인다. 비로그인(null)이면 둘 다 false.
    @Transactional(readOnly = true)
    public HelpRequestDetailView getDetail(Long id, Long viewerId) {
        return HelpRequestDetailView.from(
            helpRequestRepository
                .getHelpRequestById(id)
                .orElseThrow(() -> new HelpRequestNotFoundException(id)),
            viewerId);
    }

    // 공개 목록(도우미용)
    // OPEN만, 시작 임박 순. 카드 라벨은 거친 위치(sido+sigungu)만 노출.
    @Transactional(readOnly = true)
    public Page<HelpRequestCardView> listOpen(Pageable pageable) {
        return helpRequestRepository
            .findByStatusOrderByDesiredStartDatetimeAsc(HelpRequestStatus.OPEN, pageable)
            .map(HelpRequestCardView::from);
    }

    // 내 요청 목록(게시자용)
    @Transactional(readOnly = true)
    public Page<HelpRequestCardView> listMine(Long requesterId, Pageable pageable) {
        return helpRequestRepository
            .findByRequester_UserIdOrderByCreatedAtDesc(requesterId, pageable)
            .map(HelpRequestCardView::from);
    }

    // 마감(취소)
    // 게시자가 매칭 전(OPEN 상태)에 직접 마감. 사전조건: 게시자 본인 && status==OPEN.
    @Transactional
    public void close(Long id, Long actorId) {
        HelpRequest helpRequest = helpRequestRepository
            .findById(id)
            .orElseThrow(() -> new HelpRequestNotFoundException(id));

        if (!helpRequest.getRequester().getUserId().equals(actorId)) {
            throw new NotRequestOwnerException();
        }

        if (helpRequest.getStatus() != HelpRequestStatus.OPEN) {
            throw InvalidRequestStateException.cannotClose();
        }

        helpRequest.changeStatus(HelpRequestStatus.CLOSED);

        // TODO(#11): 매칭/지원 기능이 생기면 여기에 지원서 일괄취소 seam
        //            (HelpRequestApplicationService.cancelAllByRequest). #8 시점엔 대상이 없어 no-op.
        //            cascade=옵션 D — god service를 만들지 않고 #11에 위임.
    }

    // 삭제
    // CLOSED 상태에서만 게시자가 명시적 하드 삭제. COMPLETED 등 이력 보존 상태는 삭제 불가.
    @Transactional
    public void delete(Long id, Long requesterId) {
        HelpRequest helpRequest = helpRequestRepository
            .findById(id)
            .orElseThrow(() -> new HelpRequestNotFoundException(id));

        if (!helpRequest.getRequester().getUserId().equals(requesterId)) {
            throw new NotRequestOwnerException();
        }

        if (helpRequest.getStatus() != HelpRequestStatus.CLOSED) {
            throw InvalidRequestStateException.cannotDelete();
        }

        helpRequestRepository.delete(helpRequest);
    }
}
