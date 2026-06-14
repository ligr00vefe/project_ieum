package com.project.ieum.service;

import com.project.ieum.dto.request.ApplyRequest;
import com.project.ieum.entity.ApplicationStatus;
import com.project.ieum.entity.User;
import com.project.ieum.entity.UserRole;
import com.project.ieum.entity.caregiver.CaregiverProfile;
import com.project.ieum.entity.conversation.Conversation;
import com.project.ieum.entity.conversation.ConversationStatus;
import com.project.ieum.entity.conversation.Message;
import com.project.ieum.entity.notification.NotificationType;
import com.project.ieum.entity.request.HelpRequest;
import com.project.ieum.entity.request.HelpRequestApplication;
import com.project.ieum.entity.request.HelpRequestStatus;
import com.project.ieum.exception.ForbiddenException;
import com.project.ieum.exception.NotFoundException;
import com.project.ieum.repository.*;
import com.project.ieum.service.common.CurrentUserService;
import com.project.ieum.service.notification.NotificationService;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class MatchingService {

    private final HelpRequestRepository helpRequestRepository;
    private final HelpRequestApplicationRepository applicationRepository;
    private final CaregiverProfileRepository caregiverProfileRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final CurrentUserService currentUserService;
    private final NotificationService notificationService;
    private final HelpRequestPersonalityTagRepository helpRequestPersonalityTagRepository;
    private final CaregiverPersonalityTagRepository caregiverPersonalityTagRepository;

    public HelpRequestApplication apply(Long requestId, ApplyRequest applyRequest) {
        User currentUser = requireRole(UserRole.CAREGIVER);
        CaregiverProfile caregiver = caregiverProfileRepository.findById(currentUser.getId())
                .orElseThrow(() -> new NotFoundException("활동지원사 프로필을 찾을 수 없습니다."));
        HelpRequest helpRequest = getRequest(requestId);

        if (helpRequest.getStatus() != HelpRequestStatus.OPEN) {
            throw new IllegalStateException("열린 요청에만 지원할 수 있습니다.");
        }
        if (applicationRepository.existsByHelpRequest_IdAndCaregiver_UserId(requestId, currentUser.getId())) {
            throw new IllegalStateException("이미 지원한 요청입니다.");
        }

        HelpRequestApplication application = applicationRepository.save(HelpRequestApplication.builder()
                .helpRequest(helpRequest)
                .caregiver(caregiver)
                .status(ApplicationStatus.PENDING)
                .build());

        Conversation conversation = conversationRepository.save(Conversation.builder()
                .application(application)
                .requester(helpRequest.getRequester())
                .caregiver(caregiver)
                .status(ConversationStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .lastMessageAt(LocalDateTime.now())
                .build());

        String firstMessage = applyRequest == null ? null : applyRequest.getFirstMessage();
        if (firstMessage != null && !firstMessage.isBlank()) {
            Message message = Message.builder()
                    .conversation(conversation)
                    .sender(currentUser)
                    .body(firstMessage.trim())
                    .hasRead(false)
                    .sentAt(LocalDateTime.now())
                    .build();
            messageRepository.save(message);
            conversation.touchLastMessage();
        }

        notificationService.create(
                helpRequest.getRequester().getUser(),
                NotificationType.MATCHING,
                "새 지원자가 있어요",
                caregiver.getFullName() + " 활동지원사가 도움 요청에 지원했습니다.",
                "/my/requests/" + requestId + "/applications"
        );
        return application;
    }

    public Long acceptAndGetConversationId(Long applicationId) {
        accept(applicationId);
        return conversationRepository.findByApplication_Id(applicationId)
                .map(c -> c.getId())
                .orElseThrow(() -> new NotFoundException("채팅방을 찾을 수 없습니다."));
    }

    public void accept(Long applicationId) {
        User currentUser = requireRole(UserRole.USER);
        HelpRequestApplication selected = getApplication(applicationId);
        HelpRequest helpRequest = selected.getHelpRequest();
        ensureRequester(helpRequest, currentUser.getId());
        if (selected.getStatus() != ApplicationStatus.PENDING) {
            throw new IllegalStateException("대기 중인 지원만 수락할 수 있습니다.");
        }

        selected.accept();
        helpRequest.changeStatus(HelpRequestStatus.MATCHED);

        List<HelpRequestApplication> pendingApplications = applicationRepository
                .findByHelpRequest_IdAndStatus(helpRequest.getId(), ApplicationStatus.PENDING);
        for (HelpRequestApplication application : pendingApplications) {
            if (!application.getId().equals(selected.getId())) {
                application.cancel();
                conversationRepository.findByApplication_Id(application.getId()).ifPresent(Conversation::close);
            }
        }

        notificationService.create(
                selected.getCaregiver().getUser(),
                NotificationType.MATCHING,
                "매칭이 확정되었습니다",
                helpRequest.getTitle() + " 요청의 지원이 수락되었습니다.",
                conversationRepository.findByApplication_Id(selected.getId())
                        .map(conversation -> "/chat/conversations/" + conversation.getId())
                        .orElse("/chat/conversations")
        );
    }

    public void cancelMatch(Long applicationId) {
        User currentUser = requireRole(UserRole.USER);
        HelpRequestApplication application = getApplication(applicationId);
        ensureRequester(application.getHelpRequest(), currentUser.getId());
        if (application.getStatus() != ApplicationStatus.ACCEPTED) {
            throw new IllegalStateException("수락된 지원만 매칭 취소할 수 있습니다.");
        }
        application.cancel();
        application.getHelpRequest().changeStatus(HelpRequestStatus.CLOSED);
        conversationRepository.findByApplication_Id(applicationId).ifPresent(Conversation::close);
    }

    public void reject(Long applicationId) {
        User currentUser = requireRole(UserRole.USER);
        HelpRequestApplication application = getApplication(applicationId);
        ensureRequester(application.getHelpRequest(), currentUser.getId());
        application.reject();
        conversationRepository.findByApplication_Id(applicationId).ifPresent(Conversation::close);
    }

    public void withdraw(Long applicationId) {
        User currentUser = requireRole(UserRole.CAREGIVER);
        HelpRequestApplication application = getApplication(applicationId);
        if (!application.getCaregiver().getUserId().equals(currentUser.getId())) {
            throw new ForbiddenException("본인의 지원만 취소할 수 있습니다.");
        }
        application.cancel();
        conversationRepository.findByApplication_Id(applicationId).ifPresent(Conversation::close);
    }

    public void startActivity(Long requestId) {
        User currentUser = currentUserService.getCurrentUser();
        HelpRequest helpRequest = getRequest(requestId);
        if (!canManageMatchedRequest(helpRequest, currentUser.getId())) {
            throw new ForbiddenException("매칭 참여자만 활동을 시작할 수 있습니다.");
        }
        if (helpRequest.getStatus() != HelpRequestStatus.MATCHED) {
            throw new IllegalStateException("매칭 확정 상태에서만 활동을 시작할 수 있습니다.");
        }
        helpRequest.changeStatus(HelpRequestStatus.IN_PROGRESS);
    }

    public void completeActivity(Long requestId) {
        User currentUser = requireRole(UserRole.USER);
        HelpRequest helpRequest = getRequest(requestId);
        ensureRequester(helpRequest, currentUser.getId());
        if (helpRequest.getStatus() != HelpRequestStatus.IN_PROGRESS) {
            throw new IllegalStateException("진행 중인 활동만 완료할 수 있습니다.");
        }
        helpRequest.changeStatus(HelpRequestStatus.COMPLETED);
        findAcceptedApplication(helpRequest.getId()).complete();
    }

    @Transactional(readOnly = true)
    public List<HelpRequestApplication> getApplicationsForRequest(Long requestId) {
        User currentUser = requireRole(UserRole.USER);
        HelpRequest helpRequest = getRequest(requestId);
        ensureRequester(helpRequest, currentUser.getId());
        return applicationRepository.findByHelpRequest_IdOrderByCreatedAtDesc(requestId);
    }

    @Transactional(readOnly = true)
    public boolean hasApplied(Long requestId, Long userId) {
        return applicationRepository.existsByHelpRequest_IdAndCaregiver_UserId(requestId, userId);
    }

    @Transactional(readOnly = true)
    public java.util.Optional<Long> getMyConversationId(Long requestId, Long userId) {
        return applicationRepository.findByHelpRequest_IdAndCaregiver_UserId(requestId, userId)
                .flatMap(app -> conversationRepository.findByApplication_Id(app.getId()))
                .map(c -> c.getId());
    }

    @Transactional(readOnly = true)
    public Map<Long, Long> getConversationIdMap(List<HelpRequestApplication> applications) {
        List<Long> appIds = applications.stream().map(HelpRequestApplication::getId).toList();
        Map<Long, Long> result = new HashMap<>();
        conversationRepository.findByApplication_IdIn(appIds)
                .forEach(c -> result.put(c.getApplication().getId(), c.getId()));
        return result;
    }

    @Transactional(readOnly = true)
    public Map<Long, Integer> getMatchPercentMap(Long requestId, List<HelpRequestApplication> applications) {
        Set<Long> requestTagIds = helpRequestPersonalityTagRepository.findByHelpRequest_Id(requestId)
                .stream()
                .map(t -> t.getTag().getId())
                .collect(Collectors.toSet());

        Map<Long, Integer> result = new HashMap<>();
        for (HelpRequestApplication app : applications) {
            if (requestTagIds.isEmpty()) {
                result.put(app.getId(), 0);
                continue;
            }
            Set<Long> caregiverTagIds = caregiverPersonalityTagRepository.findByCaregiver(app.getCaregiver())
                    .stream()
                    .map(t -> t.getTag().getId())
                    .collect(Collectors.toSet());
            long matched = requestTagIds.stream().filter(caregiverTagIds::contains).count();
            int percent = (int) (matched * 100 / requestTagIds.size());
            result.put(app.getId(), percent);
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<HelpRequestApplication> getMyApplications() {
        User currentUser = requireRole(UserRole.CAREGIVER);
        return applicationRepository.findByCaregiver_UserIdOrderByCreatedAtDesc(currentUser.getId());
    }

    @Transactional(readOnly = true)
    public HelpRequestApplication findAcceptedApplication(Long requestId) {
        return applicationRepository.findByHelpRequest_IdAndStatus(requestId, ApplicationStatus.ACCEPTED)
                .stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException("확정된 매칭을 찾을 수 없습니다."));
    }

    private boolean canManageMatchedRequest(HelpRequest helpRequest, Long userId) {
        if (helpRequest.getRequester().getUserId().equals(userId)) {
            return true;
        }
        return applicationRepository.findByHelpRequest_IdAndStatus(helpRequest.getId(), ApplicationStatus.ACCEPTED)
                .stream()
                .anyMatch(application -> application.getCaregiver().getUserId().equals(userId));
    }

    private void ensureRequester(HelpRequest helpRequest, Long userId) {
        if (!helpRequest.getRequester().getUserId().equals(userId)) {
            throw new ForbiddenException("요청 작성자만 처리할 수 있습니다.");
        }
    }

    private HelpRequest getRequest(Long requestId) {
        return helpRequestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("도움 요청을 찾을 수 없습니다."));
    }

    private HelpRequestApplication getApplication(Long applicationId) {
        return applicationRepository.findById(applicationId)
                .orElseThrow(() -> new NotFoundException("지원 내역을 찾을 수 없습니다."));
    }

    private User requireRole(UserRole role) {
        User currentUser = currentUserService.getCurrentUser();
        if (currentUser.getRole() != role) {
            throw new ForbiddenException("해당 역할만 사용할 수 있습니다.");
        }
        return currentUser;
    }
}
