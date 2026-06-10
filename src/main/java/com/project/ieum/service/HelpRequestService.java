package com.project.ieum.service;

import com.project.ieum.dto.request.HelpRequestForm;
import com.project.ieum.entity.PersonalityTag;
import com.project.ieum.entity.User;
import com.project.ieum.entity.UserRole;
import com.project.ieum.entity.request.HelpRequest;
import com.project.ieum.entity.request.HelpRequestPersonalityTag;
import com.project.ieum.entity.request.HelpRequestStatus;
import com.project.ieum.entity.request.ServiceCategory;
import com.project.ieum.entity.user.UserProfile;
import com.project.ieum.exception.ForbiddenException;
import com.project.ieum.exception.NotFoundException;
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

    private final HelpRequestRepository helpRequestRepository;
    private final UserProfileRepository userProfileRepository;
    private final ServiceCategoryRepository serviceCategoryRepository;
    private final PersonalityTagRepository personalityTagRepository;
    private final HelpRequestPersonalityTagRepository helpRequestPersonalityTagRepository;
    private final CurrentUserService currentUserService;

    public HelpRequest create(HelpRequestForm form) {
        User currentUser = requireRole(UserRole.USER);
        UserProfile requester = userProfileRepository.findById(currentUser.getId())
                .orElseThrow(() -> new NotFoundException("이용자 프로필을 찾을 수 없습니다."));

        // 시간대 겹침 검사
        LocalDateTime start = form.getDesiredStartDatetime();
        LocalDateTime end = form.getDesiredEndDatetime() != null
                ? form.getDesiredEndDatetime()
                : start.plusHours(1);

        if (helpRequestRepository.existsOverlapping(
                requester, start, end,
                List.of(HelpRequestStatus.OPEN, HelpRequestStatus.MATCHED, HelpRequestStatus.IN_PROGRESS))) {
            throw new IllegalStateException("해당 시간대에 이미 다른 도움 요청이 있습니다.");
        }

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
                .status(HelpRequestStatus.OPEN)
                .build();

        HelpRequest saved = helpRequestRepository.save(helpRequest);
        replaceTags(saved, form.getPersonalityTagIds());
        return saved;
    }

    public HelpRequest update(Long requestId, HelpRequestForm form) {
        User currentUser = requireRole(UserRole.USER);
        HelpRequest helpRequest = getOwnedRequest(requestId, currentUser.getId());
        if (helpRequest.getStatus() != HelpRequestStatus.OPEN) {
            throw new IllegalStateException("열린 상태의 요청만 수정할 수 있습니다.");
        }

        helpRequest.updateDetails(
                getServiceCategory(form.getServiceCategoryId()),
                form.getTitle(),
                form.getBody(),
                form.getDesiredStartDatetime(),
                form.getDesiredEndDatetime(),
                form.getRoadAddress(),
                form.getAddressDetail(),
                form.getSido(),
                form.getSigungu(),
                form.getSpecialNotes());
        replaceTags(helpRequest, form.getPersonalityTagIds());
        return helpRequest;
    }

    public void cancel(Long requestId) {
        User currentUser = requireRole(UserRole.USER);
        HelpRequest helpRequest = getOwnedRequest(requestId, currentUser.getId());
        if (helpRequest.getStatus() == HelpRequestStatus.COMPLETED) {
            throw new IllegalStateException("완료된 요청은 취소할 수 없습니다.");
        }
        helpRequest.changeStatus(HelpRequestStatus.CANCELLED);
    }

    @Transactional(readOnly = true)
    public HelpRequest get(Long requestId) {
        return helpRequestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("도움 요청을 찾을 수 없습니다."));
    }

    @Transactional(readOnly = true)
    public HelpRequest getOwnedRequest(Long requestId, Long userId) {
        HelpRequest helpRequest = get(requestId);
        if (!helpRequest.getRequester().getUserId().equals(userId)) {
            throw new ForbiddenException("본인의 요청만 접근할 수 있습니다.");
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
    public List<HelpRequest> getAllActiveRequests() {
        return helpRequestRepository.findByStatusInOrderByCreatedAtDesc(
                List.of(HelpRequestStatus.OPEN, HelpRequestStatus.MATCHED));
    }

    @Transactional(readOnly = true)
    public Page<HelpRequest> getOpenRequests(Pageable pageable) {
        return helpRequestRepository.findByStatusOrderByDesiredStartDatetimeAscIdDesc(HelpRequestStatus.OPEN, pageable);
    }

    public HelpRequestForm toForm(HelpRequest helpRequest) {
        HelpRequestForm form = new HelpRequestForm();
        form.setTitle(helpRequest.getTitle());
        form.setBody(helpRequest.getBody());
        form.setServiceCategoryId(helpRequest.getServiceCategory().getId());
        form.setDesiredStartDatetime(helpRequest.getDesiredStartDatetime());
        form.setDesiredEndDatetime(helpRequest.getDesiredEndDatetime());
        form.setRoadAddress(helpRequest.getRoadAddress());
        form.setAddressDetail(helpRequest.getAddressDetail());
        form.setSido(helpRequest.getSido());
        form.setSigungu(helpRequest.getSigungu());
        form.setBname(helpRequest.getBname());
        form.setZonecode(helpRequest.getZonecode());
        form.setBcode(helpRequest.getBcode());
        form.setSpecialNotes(helpRequest.getSpecialNotes());
        form.setPersonalityTagIds(helpRequestPersonalityTagRepository.findByHelpRequest_Id(helpRequest.getId())
                .stream()
                .map(tag -> tag.getTag().getId())
                .toList());
        return form;
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

    private ServiceCategory getServiceCategory(Long id) {
        return serviceCategoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("서비스 종류를 찾을 수 없습니다."));
    }
}
