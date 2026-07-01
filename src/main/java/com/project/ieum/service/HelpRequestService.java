package com.project.ieum.service;

import com.project.ieum.dto.mypage.CompletedMatchingView;
import com.project.ieum.dto.request.HelpRequestForm;
import com.project.ieum.dto.search.HelpRequestSearchCondition;
import com.project.ieum.dto.search.RegionOption;
import com.project.ieum.entity.ApplicationStatus;
import com.project.ieum.entity.PersonalityTag;
import com.project.ieum.entity.User;
import com.project.ieum.entity.UserRole;
import com.project.ieum.entity.conversation.Conversation;
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
import com.project.ieum.service.geocoding.GeoPoint;
import com.project.ieum.service.geocoding.GeocodingService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class HelpRequestService {

    // 시간대 겹침 충돌 대상으로 보는 활성 상태(종료상태 COMPLETED/CLOSED 제외).
    private static final List<HelpRequestStatus> ACTIVE_STATUSES =
            List.of(HelpRequestStatus.OPEN, HelpRequestStatus.MATCHED, HelpRequestStatus.IN_PROGRESS);

    private final HelpRequestRepository helpRequestRepository;
    private final HelpRequestApplicationRepository applicationRepository;
    private final ConversationRepository conversationRepository;
    private final UserProfileRepository userProfileRepository;
    private final ServiceCategoryRepository serviceCategoryRepository;
    private final PersonalityTagRepository personalityTagRepository;
    private final HelpRequestPersonalityTagRepository helpRequestPersonalityTagRepository;
    private final HelpRequestApplicationRepository helpRequestApplicationRepository;
    private final ReviewRepository reviewRepository;
    private final CurrentUserService currentUserService;
    private final GeocodingService geocodingService;
    // (#68) 트랜잭션 경계 분리용 self 프록시 — geocode(외부 HTTP)를 트랜잭션 밖에서 수행하기 위함.
    private final ObjectProvider<HelpRequestService> selfProvider;

    // (#68) 지오코딩(외부 HTTP)은 DB 커넥션을 잡지 않도록 트랜잭션 "밖"에서 먼저 수행하고, 좌표를 영속 메서드에
    // 인자로 전달한다. self 프록시로 호출해야 persistCreated의 트랜잭션이 적용된다(같은 빈 직접 호출은 프록시 미적용).
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public HelpRequest create(HelpRequestForm form) {
        GeoPoint geoPoint = geocodingService.geocode(form.getRoadAddress()).orElse(null);
        return selfProvider.getObject().persistCreated(form, geoPoint);
    }

    // 트랜잭션 경계 — 겹침검사(검사-삽입 원자성)와 저장을 한 트랜잭션에 묶는다. 좌표는 트랜잭션 밖에서 받은 값.
    @Transactional
    public HelpRequest persistCreated(HelpRequestForm form, GeoPoint geoPoint) {
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

        // 위치 스냅샷 좌표는 생성 시점에 1회만 채운다(write-once). 좌표는 발견/정렬용이라 미확보(null)여도 막지 않는다.
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
                .latitude(geoPoint != null ? geoPoint.latitude() : null)
                .longitude(geoPoint != null ? geoPoint.longitude() : null)
                .departureAddress(form.getDepartureAddress())
                .destinationAddress(form.getDestinationAddress())
                .specialNotes(form.getSpecialNotes())
                .build();

        HelpRequest saved = helpRequestRepository.save(helpRequest);
        replaceTags(saved, form.getPersonalityTagIds());
        return saved;
    }

    // OPEN 상태 요청 수정 — 지원자가 있으면 수정 불가(신뢰성 보호).
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void update(Long requestId, HelpRequestForm form) {
        GeoPoint geoPoint = geocodingService.geocode(form.getRoadAddress()).orElse(null);
        selfProvider.getObject().persistUpdated(requestId, form, geoPoint);
    }

    @Transactional
    public void persistUpdated(Long requestId, HelpRequestForm form, GeoPoint geoPoint) {
        User currentUser = requireRole(UserRole.USER);
        HelpRequest helpRequest = getOwnedRequest(requestId, currentUser.getId());
        if (helpRequest.getStatus() != HelpRequestStatus.OPEN) {
            throw InvalidRequestStateException.cannotClose();
        }
        boolean hasApplicants = applicationRepository
                .findByHelpRequest_IdOrderByCreatedAtDesc(requestId).stream()
                .anyMatch(a -> a.getStatus() == ApplicationStatus.PENDING);
        if (hasApplicants) {
            throw new BadRequestException("이미 지원자가 있어 수정할 수 없습니다. 마감 후 재작성해 주세요.");
        }
        ServiceCategory category = getServiceCategory(form.getServiceCategoryId());
        helpRequest.updateDetails(
                form.getTitle(), form.getBody(),
                form.getDesiredStartDatetime(), form.getDesiredEndDatetime(),
                form.getRoadAddress(), form.getAddressDetail(),
                form.getSido(), form.getSigungu(), form.getBname(), form.getZonecode(), form.getBcode(),
                geoPoint != null ? geoPoint.latitude() : null,
                geoPoint != null ? geoPoint.longitude() : null,
                form.getDepartureAddress(), form.getDestinationAddress(), form.getSpecialNotes(),
                category);
        replaceTags(helpRequest, form.getPersonalityTagIds());
    }

    @Transactional(readOnly = true)
    public HelpRequestForm buildEditForm(Long requestId) {
        User currentUser = requireRole(UserRole.USER);
        HelpRequest source = getOwnedRequest(requestId, currentUser.getId());
        HelpRequestForm form = new HelpRequestForm();
        form.setTitle(source.getTitle());
        form.setBody(source.getBody());
        form.setServiceCategoryId(source.getServiceCategory().getId());
        form.setDesiredStartDatetime(source.getDesiredStartDatetime());
        form.setDesiredEndDatetime(source.getDesiredEndDatetime());
        form.setRoadAddress(source.getRoadAddress());
        form.setAddressDetail(source.getAddressDetail());
        form.setSido(source.getSido());
        form.setSigungu(source.getSigungu());
        form.setBname(source.getBname());
        form.setZonecode(source.getZonecode());
        form.setBcode(source.getBcode());
        form.setDepartureAddress(source.getDepartureAddress());
        form.setDestinationAddress(source.getDestinationAddress());
        form.setSpecialNotes(source.getSpecialNotes());
        form.setPersonalityTagIds(
                helpRequestPersonalityTagRepository.findByHelpRequest_Id(requestId).stream()
                        .map(t -> t.getTag().getId()).toList());
        return form;
    }

    // (이슈 #8 정본) update()/toForm() 제거: HelpRequest는 write-once.
    // 활동지원사가 본 내용/시간/위치가 지원 후 바뀌면 신뢰성이 깨지므로 생성 후 본문 수정을 막는다.
    // 변경이 필요하면 마감(cancel→CLOSED) 후 새로 작성한다.

    // 재게시 — 마감(CLOSED)·완료(COMPLETED)된 본인 요청의 내용을 작성 폼에 프리필한다.
    // 원본은 보존하고, 제출 시 create()로 새 OPEN 요청이 생긴다(기존 작성 흐름과 동일).
    // 날짜는 이미 지났으면(@FutureOrPresent 위반) 비워 사용자가 다시 고르게 한다.
    @Transactional(readOnly = true)
    public HelpRequestForm buildRepostForm(Long requestId) {
        User currentUser = requireRole(UserRole.USER);
        HelpRequest source = getOwnedRequest(requestId, currentUser.getId());
        if (source.getStatus() != HelpRequestStatus.CLOSED
                && source.getStatus() != HelpRequestStatus.COMPLETED) {
            throw InvalidRequestStateException.cannotRepost();
        }

        HelpRequestForm form = new HelpRequestForm();
        form.setTitle(source.getTitle());
        form.setBody(source.getBody());
        form.setServiceCategoryId(source.getServiceCategory().getId());
        form.setRoadAddress(source.getRoadAddress());
        form.setAddressDetail(source.getAddressDetail());
        form.setSido(source.getSido());
        form.setSigungu(source.getSigungu());
        form.setBname(source.getBname());
        form.setZonecode(source.getZonecode());
        form.setBcode(source.getBcode());
        form.setDepartureAddress(source.getDepartureAddress());
        form.setDestinationAddress(source.getDestinationAddress());
        form.setSpecialNotes(source.getSpecialNotes());
        form.setPersonalityTagIds(
                helpRequestPersonalityTagRepository.findByHelpRequest_Id(requestId).stream()
                        .map(tag -> tag.getTag().getId())
                        .toList());

        LocalDateTime now = LocalDateTime.now();
        if (source.getDesiredStartDatetime() != null && !source.getDesiredStartDatetime().isBefore(now)) {
            form.setDesiredStartDatetime(source.getDesiredStartDatetime());
        }
        if (source.getDesiredEndDatetime() != null && !source.getDesiredEndDatetime().isBefore(now)) {
            form.setDesiredEndDatetime(source.getDesiredEndDatetime());
        }
        return form;
    }

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
        helpRequest.close();
        applicationRepository.findByHelpRequest_IdAndStatus(helpRequest.getId(), ApplicationStatus.PENDING)
                .forEach(app -> {
                    app.cancel();
                    conversationRepository.findByApplication_Id(app.getId()).ifPresent(Conversation::close);
                });
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
    public Page<HelpRequest> getAllActiveRequestsPaged(int page) {
        return helpRequestRepository.findByStatusInOrderByCreatedAtDesc(
                List.of(HelpRequestStatus.OPEN, HelpRequestStatus.MATCHED),
                PageRequest.of(page, 10));
    }

    @Transactional(readOnly = true)
    public Page<HelpRequest> getOpenRequests(Pageable pageable) {
        return helpRequestRepository.findByStatusOrderByCreatedAtDesc(HelpRequestStatus.OPEN, pageable);
    }

    // 게시판 동적 검색 — 모집중(OPEN)만 대상으로 조건 필터링. 좌표가 있으면 가까운 순 정렬(#66 보존).
    @Transactional(readOnly = true)
    public Page<HelpRequest> searchOpenRequests(HelpRequestSearchCondition condition, Pageable pageable,
                                                Double lat, Double lng) {
        condition.setStatus(HelpRequestStatus.OPEN);
        return helpRequestRepository.searchHelpRequests(condition, pageable, lat, lng);
    }

    // 이용자 둘러보기 보드 — 남의 OPEN + 내 모든 상태(최신순). 관리자는 전체 OPEN만 조회.
    @Transactional(readOnly = true)
    public Page<HelpRequest> searchBoardForUser(HelpRequestSearchCondition condition, Pageable pageable) {
        User me = currentUserService.getCurrentUser();
        if (me.getRole() != UserRole.ADMIN) {
            condition.setOwnerScopeUserId(me.getId());
        }
        return helpRequestRepository.searchHelpRequests(condition, pageable, null, null);
    }

    // 게시판 지역 필터 옵션 — 모집중 요청에 존재하는 시/도 → [시군구...] 맵(드롭다운 cascade용, 입력 순서 보존).
    @Transactional(readOnly = true)
    public Map<String, List<String>> getOpenRegionOptions() {
        Map<String, List<String>> map = new LinkedHashMap<>();
        for (RegionOption opt : helpRequestRepository.findDistinctRegionsByStatus(HelpRequestStatus.OPEN)) {
            map.computeIfAbsent(opt.sido(), key -> new ArrayList<>()).add(opt.sigungu());
        }
        return map;
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
