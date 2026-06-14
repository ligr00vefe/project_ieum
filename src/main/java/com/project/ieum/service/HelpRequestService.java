package com.project.ieum.service;

import com.project.ieum.dto.mypage.CompletedMatchingView;
import com.project.ieum.dto.request.HelpRequestForm;
import com.project.ieum.entity.ApplicationStatus;
import com.project.ieum.entity.PersonalityTag;
import com.project.ieum.entity.User;
import com.project.ieum.entity.UserRole;
import com.project.ieum.entity.request.HelpRequest;
import com.project.ieum.entity.request.HelpRequestApplication;
import com.project.ieum.entity.request.HelpRequestPersonalityTag;
import com.project.ieum.entity.request.HelpRequestStatus;
import com.project.ieum.entity.request.Review;
import com.project.ieum.entity.request.ServiceCategory;
import com.project.ieum.entity.user.UserProfile;
import com.project.ieum.exception.BadRequestException;
import com.project.ieum.exception.ForbiddenException;
import com.project.ieum.exception.HelpRequestNotFoundException;
import com.project.ieum.exception.InvalidRequestStateException;
import com.project.ieum.exception.NotFoundException;
import com.project.ieum.exception.NotRequestOwnerException;
import com.project.ieum.exception.RequestTimeConflictException;
import com.project.ieum.repository.*;
import com.project.ieum.service.common.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class HelpRequestService {

    // 시간대 겹침 충돌 대상으로 보는 활성 상태(종료상태 COMPLETED/CLOSED 제외).
    private static final List<HelpRequestStatus> ACTIVE_STATUSES =
            List.of(HelpRequestStatus.OPEN, HelpRequestStatus.MATCHED, HelpRequestStatus.IN_PROGRESS);

    private final HelpRequestRepository helpRequestRepository;
    private final UserProfileRepository userProfileRepository;
    private final ServiceCategoryRepository serviceCategoryRepository;
    private final PersonalityTagRepository personalityTagRepository;
    private final HelpRequestPersonalityTagRepository helpRequestPersonalityTagRepository;
    private final HelpRequestApplicationRepository helpRequestApplicationRepository;
    private final ReviewRepository reviewRepository;
    private final CurrentUserService currentUserService;

    public HelpRequest create(HelpRequestForm form) {
        User currentUser = requireRole(UserRole.USER);
        UserProfile requester = userProfileRepository.findById(currentUser.getId())
                .orElseThrow(() -> new NotFoundException("이용자 프로필을 찾을 수 없습니다."));

        LocalDateTime start = form.getDesiredStartDatetime();
        LocalDateTime end = form.getDesiredEndDatetime() != null
                ? form.getDesiredEndDatetime()
                : start.plusHours(1);

        // (이슈 #8 정본) 시간대 겹침은 도메인 4xx 예외로 — IllegalStateException(→500 오매핑) 대신 400.
        if (helpRequestRepository.existsOverlapping(requester, start, end, ACTIVE_STATUSES)) {
            throw new RequestTimeConflictException();
        }

        // status는 지정하지 않는다 — 엔티티 @Builder.Default = OPEN 이 생성 시 고정값을 보장(누락 방지).
        HelpRequest helpRequest = HelpRequest.builder()
                .requester(requester)
                .serviceCategory(getServiceCategory(form.getServiceCategoryId()))
                .title(form.getTitle())
                .body(form.getBody())
                .desiredStartDatetime(start)
                .desiredEndDatetime(form.getDesiredEndDatetime())
                .roadAddress(form.getRoadAddress())
                .addressDetail(form.getAddressDetail())
                .sido(form.getSido())
                .sigungu(form.getSigungu())
                .bname(form.getBname())
                .zonecode(form.getZonecode())
                .bcode(form.getBcode())
                .departureAddress(form.getDepartureAddress())
                .destinationAddress(form.getDestinationAddress())
                .specialNotes(form.getSpecialNotes())
                .build();

        HelpRequest saved = helpRequestRepository.save(helpRequest);
        replaceTags(saved, form.getPersonalityTagIds());
        return saved;
    }

    // (이슈 #8 정본) update()/toForm() 제거: HelpRequest는 write-once.
    // 도우미가 본 내용/시간/위치가 지원 후 바뀌면 신뢰성이 깨지므로 생성 후 본문 수정을 막는다.
    // 변경이 필요하면 마감(cancel→CLOSED) 후 새로 작성한다.

    // 마감(취소) — 매칭 전(OPEN)에만 게시자가 직접. 2단계 삭제의 1단계.
    public void cancel(Long requestId) {
        User currentUser = requireRole(UserRole.USER);
        HelpRequest helpRequest = get(requestId);
        // (이슈 #8 정본) 권한 검사 순서 = 소유자(403) → 상태(400). 비소유자에게 상태를 노출하지 않는다.
        if (!helpRequest.getRequester().getUserId().equals(currentUser.getId())) {
            throw new NotRequestOwnerException();
        }
        if (helpRequest.getStatus() != HelpRequestStatus.OPEN) {
            throw InvalidRequestStateException.cannotClose();
        }
        helpRequest.changeStatus(HelpRequestStatus.CLOSED);
        // TODO(#11): 지원/매칭 기능이 생기면 여기서 지원서를 일괄 취소한다(cascade 옵션 D, MatchingService에 위임).
    }

    // 하드 삭제 — 2단계 삭제의 2단계. CLOSED 상태에서만 게시자가 명시적으로.
    public void delete(Long requestId) {
        User currentUser = requireRole(UserRole.USER);
        HelpRequest helpRequest = get(requestId);
        if (!helpRequest.getRequester().getUserId().equals(currentUser.getId())) {
            throw new NotRequestOwnerException();
        }
        if (helpRequest.getStatus() != HelpRequestStatus.CLOSED) {
            throw InvalidRequestStateException.cannotDelete();
        }
        helpRequestPersonalityTagRepository.deleteAll(
                helpRequestPersonalityTagRepository.findByHelpRequest_Id(requestId));
        helpRequestRepository.delete(helpRequest);
    }

    @Transactional(readOnly = true)
    public HelpRequest get(Long requestId) {
        return helpRequestRepository.findById(requestId)
                .orElseThrow(() -> new HelpRequestNotFoundException(requestId));
    }

    @Transactional(readOnly = true)
    public HelpRequest getOwnedRequest(Long requestId, Long userId) {
        HelpRequest helpRequest = get(requestId);
        if (!helpRequest.getRequester().getUserId().equals(userId)) {
            throw new NotRequestOwnerException();
        }
        return helpRequest;
    }

    @Transactional(readOnly = true)
    public List<HelpRequest> getMyRequests() {
        User currentUser = requireRole(UserRole.USER);
        UserProfile requester = userProfileRepository.findById(currentUser.getId())
                .orElseThrow(() -> new NotFoundException("이용자 프로필을 찾을 수 없습니다."));
        return helpRequestRepository.findByRequesterOrderByCreatedAtDesc(requester);
    }

    @Transactional(readOnly = true)
    public List<CompletedMatchingView> getMyMatchingViews() {
        User currentUser = requireRole(UserRole.USER);
        UserProfile requester = userProfileRepository.findById(currentUser.getId())
                .orElseThrow(() -> new NotFoundException("이용자 프로필을 찾을 수 없습니다."));
        List<HelpRequest> requests = helpRequestRepository.findByRequesterOrderByCreatedAtDesc(requester);

        return requests.stream()
                .filter(r -> r.getStatus() != HelpRequestStatus.OPEN && r.getStatus() != HelpRequestStatus.CLOSED)
                .map(r -> {
                    boolean completed = r.getStatus() == HelpRequestStatus.COMPLETED;
                    ApplicationStatus targetStatus = completed ? ApplicationStatus.COMPLETED : ApplicationStatus.ACCEPTED;
                    HelpRequestApplication app = helpRequestApplicationRepository
                            .findByHelpRequest_IdAndStatus(r.getId(), targetStatus)
                            .stream().findFirst().orElse(null);
                    Review review = completed ? reviewRepository.findByHelpRequest_Id(r.getId()).orElse(null) : null;
                    return CompletedMatchingView.builder()
                            .requestId(r.getId())
                            .serviceCategory(r.getServiceCategory() != null ? r.getServiceCategory().getName() : null)
                            .location(r.getSido() + " " + r.getSigungu())
                            .startDatetime(r.getDesiredStartDatetime())
                            .endDatetime(r.getDesiredEndDatetime())
                            .caregiverName(app != null ? app.getCaregiver().getFullName() : null)
                            .completed(completed)
                            .review(review)
                            .build();
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<HelpRequest> getAllActiveRequests() {
        return helpRequestRepository.findByStatusInOrderByCreatedAtDesc(
                List.of(HelpRequestStatus.OPEN, HelpRequestStatus.MATCHED));
    }

    @Transactional(readOnly = true)
    public Page<HelpRequest> getOpenRequests(Pageable pageable) {
        return helpRequestRepository.findByStatusOrderByDesiredStartDatetimeAscIdDesc(HelpRequestStatus.OPEN, pageable);
    }

    private void replaceTags(HelpRequest helpRequest, List<Long> tagIds) {
        List<HelpRequestPersonalityTag> existing = helpRequestPersonalityTagRepository.findByHelpRequest_Id(helpRequest.getId());
        helpRequestPersonalityTagRepository.deleteAll(existing);
        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }
        List<PersonalityTag> tags = personalityTagRepository.findAllById(tagIds);
        for (PersonalityTag tag : tags) {
            helpRequestPersonalityTagRepository.save(HelpRequestPersonalityTag.builder()
                    .helpRequest(helpRequest)
                    .tag(tag)
                    .build());
        }
    }

    private User requireRole(UserRole role) {
        User currentUser = currentUserService.getCurrentUser();
        if (currentUser.getRole() != role) {
            throw new ForbiddenException("해당 역할만 사용할 수 있습니다.");
        }
        return currentUser;
    }

    // (이슈 #8 정본) 등록되지 않은 분류 선택은 클라이언트 입력 오류 → 404(NotFound)가 아니라 400(BadRequest).
    private ServiceCategory getServiceCategory(Long id) {
        return serviceCategoryRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("등록되지 않은 서비스 분류입니다."));
    }
}
