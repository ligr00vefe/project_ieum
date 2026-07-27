package com.project.ieum.service;

import com.project.ieum.dto.recommend.MatchTagInfo;
import com.project.ieum.dto.request.ActivityHandshakeView;
import com.project.ieum.dto.request.ApplyRequest;
import com.project.ieum.dto.request.MatchedPartyView;
import com.project.ieum.entity.ApplicationStatus;
import com.project.ieum.entity.User;
import com.project.ieum.entity.UserRole;
import com.project.ieum.entity.UserStatus;
import com.project.ieum.entity.caregiver.CaregiverProfile;
import com.project.ieum.entity.conversation.Conversation;
import com.project.ieum.entity.conversation.ConversationStatus;
import com.project.ieum.entity.conversation.Message;
import com.project.ieum.entity.notification.NotificationType;
import com.project.ieum.entity.request.CaregiverInvitation;
import com.project.ieum.entity.request.ConfirmParty;
import com.project.ieum.entity.request.HelpRequest;
import com.project.ieum.entity.request.HelpRequestApplication;
import com.project.ieum.entity.request.HelpRequestPersonalityTag;
import com.project.ieum.entity.request.HelpRequestStatus;
import com.project.ieum.entity.request.InvitationStatus;
import com.project.ieum.exception.ForbiddenException;
import com.project.ieum.exception.NotFoundException;
import com.project.ieum.repository.*;
import com.project.ieum.repository.CaregiverInvitationRepository;
import com.project.ieum.service.common.CurrentUserService;
import com.project.ieum.service.notification.NotificationService;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MatchingService {

    @Value("${ieum.base-url:http://localhost:8080}")
    private String baseUrl;

    private final AsyncMailService asyncMailService;
    private final HelpRequestRepository helpRequestRepository;
    private final HelpRequestApplicationRepository applicationRepository;
    private final CaregiverProfileRepository caregiverProfileRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final CurrentUserService currentUserService;
    private final NotificationService notificationService;
    private final HelpRequestPersonalityTagRepository helpRequestPersonalityTagRepository;
    private final CaregiverPersonalityTagRepository caregiverPersonalityTagRepository;
    private final CaregiverInvitationRepository invitationRepository;
    private final ChatAnnouncementRepository chatAnnouncementRepository;

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
        // (#22) 정지(BAN)된 사용자 보호 — 정지된 활동지원사는 지원 불가, 정지된 요청자의 글에도 지원 불가.
        // (중복지원 체크 뒤에 둔다 — 정상 지원 경로에서만 requester.getUser() 접근, 불필요한 lazy 로딩/NPE 회피)
        if (currentUser.getStatus() == UserStatus.BANNED) {
            throw new ForbiddenException("정지된 계정은 지원할 수 없습니다.");
        }
        if (helpRequest.getRequester().getUser().getStatus() == UserStatus.BANNED) {
            throw new ForbiddenException("정지된 사용자의 요청에는 지원할 수 없습니다.");
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
                "/disabled/board/" + requestId + "/applicants"
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
        helpRequest.match();

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
        application.getHelpRequest().close();
        conversationRepository.findByApplication_Id(applicationId).ifPresent(Conversation::close);
    }

    public void withdraw(Long applicationId) {
        User currentUser = requireRole(UserRole.CAREGIVER);
        HelpRequestApplication application = getApplication(applicationId);
        if (!application.getCaregiver().getUserId().equals(currentUser.getId())) {
            throw new ForbiddenException("본인의 지원만 취소할 수 있습니다.");
        }
        HelpRequest helpRequest = application.getHelpRequest();
        boolean wasAccepted = application.getStatus() == ApplicationStatus.ACCEPTED;

        if (wasAccepted) {
            // 매칭 확정 상태에서 취소 → 게시물 CLOSED, 이용자에게 알림
            application.cancel();
            conversationRepository.findByApplication_Id(applicationId).ifPresent(Conversation::close);
            helpRequest.close();
            notificationService.create(
                    helpRequest.getRequester().getUser(),
                    NotificationType.MATCHING,
                    "매칭이 취소되었어요",
                    application.getCaregiver().getFullName() + " 활동지원사가 '" + helpRequest.getTitle() + "' 매칭을 취소했습니다.",
                    "/disabled/board/" + helpRequest.getId()
            );
        } else {
            // PENDING 상태에서 취소 → 지원 전 상태로 완전 복원 (레코드 삭제)
            conversationRepository.findByApplication_Id(applicationId).ifPresent(conv -> {
                chatAnnouncementRepository.deleteByConversationId(conv.getId());
                messageRepository.deleteByConversationId(conv.getId());
                conversationRepository.delete(conv);
            });
            applicationRepository.delete(application);
        }
    }

    // 활동 시작 확인(핸드셰이크): 이용자·활동지원사가 각자 호출. 양측이 모두 확인하면 MATCHED→IN_PROGRESS.
    public void confirmStart(Long requestId) {
        User currentUser = currentUserService.getCurrentUser();
        HelpRequest helpRequest = getRequest(requestId);
        if (helpRequest.getStatus() != HelpRequestStatus.MATCHED) {
            throw new IllegalStateException("매칭 확정 상태에서만 활동 시작을 확인할 수 있습니다.");
        }
        HelpRequestApplication matched = findAcceptedApplication(requestId);
        ConfirmParty party = resolveParty(helpRequest, matched, currentUser.getId());

        matched.confirmStartBy(party);
        if (matched.bothStartConfirmed()) {
            helpRequest.startProgress();
            notifyBothParties(helpRequest, matched, "활동이 시작되었어요",
                    helpRequest.getTitle() + " 활동이 시작되었습니다.");
        }
    }

    // 활동 종료 확인(핸드셰이크): 양측이 모두 확인하면 IN_PROGRESS→COMPLETED + 지원 COMPLETED.
    public void confirmEnd(Long requestId) {
        User currentUser = currentUserService.getCurrentUser();
        HelpRequest helpRequest = getRequest(requestId);
        if (helpRequest.getStatus() != HelpRequestStatus.IN_PROGRESS) {
            throw new IllegalStateException("진행 중 상태에서만 활동 종료를 확인할 수 있습니다.");
        }
        HelpRequestApplication matched = findAcceptedApplication(requestId);
        ConfirmParty party = resolveParty(helpRequest, matched, currentUser.getId());

        matched.confirmEndBy(party);
        if (matched.bothEndConfirmed()) {
            helpRequest.complete();
            matched.complete();
            notifyBothParties(helpRequest, matched, "활동이 완료되었어요",
                    helpRequest.getTitle() + " 활동이 완료되었습니다. 후기를 남겨보세요.");
        }
    }

    @Transactional(readOnly = true)
    public List<HelpRequestApplication> getApplicationsForRequest(Long requestId) {
        User currentUser = requireRole(UserRole.USER);
        HelpRequest helpRequest = getRequest(requestId);
        ensureRequester(helpRequest, currentUser.getId());
        return applicationRepository.findByHelpRequest_IdOrderByCreatedAtDesc(requestId);
    }

    @Transactional(readOnly = true)
    public int countApplicationsForRequest(Long requestId) {
        return (int) applicationRepository.countByHelpRequest_IdAndStatusIn(
                requestId, List.of(ApplicationStatus.PENDING, ApplicationStatus.ACCEPTED, ApplicationStatus.COMPLETED));
    }

    @Transactional(readOnly = true)
    public boolean hasApplied(Long requestId, Long userId) {
        return applicationRepository.existsByHelpRequest_IdAndCaregiver_UserIdAndStatusIn(
                requestId, userId, List.of(ApplicationStatus.PENDING, ApplicationStatus.ACCEPTED, ApplicationStatus.COMPLETED));
    }

    // 현재 사용자가 이 요청에 선정된 활동지원사인가(매칭~진행~완료 전 구간).
    // 핸드셰이크 뷰는 MATCHED/IN_PROGRESS에서만 visible이라 COMPLETED 식별에 쓸 수 없어 별도 판별이 필요하다.
    @Transactional(readOnly = true)
    public boolean isSelectedCaregiver(Long requestId, Long userId) {
        return applicationRepository.findByHelpRequest_IdAndStatusIn(
                        requestId, List.of(ApplicationStatus.ACCEPTED, ApplicationStatus.COMPLETED))
                .stream()
                .anyMatch(app -> app.getCaregiver().getUserId().equals(userId));
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
                .stream().map(t -> t.getTag().getId()).collect(Collectors.toSet());
        Map<Long, Integer> result = new HashMap<>();
        for (HelpRequestApplication app : applications) {
            if (requestTagIds.isEmpty()) {
                result.put(app.getId(), 0);
                continue;
            }
            Set<Long> caregiverTagIds = caregiverPersonalityTagRepository.findByCaregiver(app.getCaregiver())
                    .stream().map(t -> t.getTag().getId()).collect(Collectors.toSet());
            long matched = requestTagIds.stream().filter(caregiverTagIds::contains).count();
            result.put(app.getId(), (int) (matched * 100 / requestTagIds.size()));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public Map<Long, List<MatchTagInfo>> getMatchTagDetailMap(Long requestId, List<HelpRequestApplication> applications) {
        List<HelpRequestPersonalityTag> requestTags =
                helpRequestPersonalityTagRepository.findByHelpRequest_Id(requestId);
        Map<Long, List<MatchTagInfo>> result = new HashMap<>();
        for (HelpRequestApplication app : applications) {
            Set<Long> caregiverTagIds = caregiverPersonalityTagRepository.findByCaregiver(app.getCaregiver())
                    .stream()
                    .map(t -> t.getTag().getId())
                    .collect(Collectors.toSet());
            List<MatchTagInfo> tagInfos = requestTags.stream()
                    .map(rt -> new MatchTagInfo(rt.getTag().getName(),
                            caregiverTagIds.contains(rt.getTag().getId())))
                    .toList();
            result.put(app.getId(), tagInfos);
        }
        return result;
    }

    // ── 초대 기능 ──────────────────────────────────────────────────────────────

    public CaregiverInvitation invite(Long requestId, Long caregiverId) {
        User currentUser = requireRole(UserRole.USER);
        HelpRequest helpRequest = getRequest(requestId);
        ensureRequester(helpRequest, currentUser.getId());
        if (helpRequest.getStatus() != HelpRequestStatus.OPEN) {
            throw new IllegalStateException("모집 중인 게시물에만 초대할 수 있습니다.");
        }
        if (invitationRepository.existsByHelpRequest_IdAndCaregiver_UserId(requestId, caregiverId)) {
            throw new IllegalStateException("이미 초대한 활동지원사입니다.");
        }
        CaregiverProfile caregiver = caregiverProfileRepository.findById(caregiverId)
                .orElseThrow(() -> new NotFoundException("활동지원사를 찾을 수 없습니다."));

        CaregiverInvitation invitation = invitationRepository.save(CaregiverInvitation.builder()
                .helpRequest(helpRequest)
                .caregiver(caregiver)
                .status(InvitationStatus.PENDING)
                .build());

        notificationService.create(
                caregiver.getUser(),
                NotificationType.INVITATION,
                "도움 요청 초대가 왔어요",
                currentUser.getEmail().split("@")[0] + " 이용자가 '" + helpRequest.getTitle() + "' 요청에 초대했습니다.",
                "/caregiver/board/" + requestId
        );
        sendInvitationEmail(caregiver.getUser().getEmail(), caregiver.getFullName(), helpRequest, requestId);
        return invitation;
    }

    private void sendInvitationEmail(String to, String caregiverName, HelpRequest req, Long requestId) {
        String postUrl = baseUrl + "/caregiver/board/" + requestId;
        String requesterName = req.getRequester() != null ? req.getRequester().getFullName() : "";
        String category = req.getServiceCategory() != null ? req.getServiceCategory().getName() : "";
        String timeRange = "";
        if (req.getDesiredStartDatetime() != null) {
            timeRange = req.getDesiredStartDatetime()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm"));
            if (req.getDesiredEndDatetime() != null) {
                timeRange += " ~ " + req.getDesiredEndDatetime()
                        .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
            }
        }
        String address = req.getRoadAddress() != null ? req.getRoadAddress() : "";
        if (req.getAddressDetail() != null && !req.getAddressDetail().isBlank()) {
            address += " " + req.getAddressDetail();
        }
        String departure = req.getDepartureAddress() != null ? req.getDepartureAddress() : "";
        String destination = req.getDestinationAddress() != null ? req.getDestinationAddress() : "";

        String detailRows = buildDetailRow("요청자", requesterName)
                + buildDetailRow("서비스 유형", category)
                + buildDetailRow("활동 일시", timeRange)
                + buildDetailRow("활동 장소", address)
                + (departure.isBlank() ? "" : buildDetailRow("출발지", departure))
                + (destination.isBlank() ? "" : buildDetailRow("도착지", destination));

        String html = """
                <div style="font-family:sans-serif;max-width:560px;margin:0 auto;padding:32px;background:#f8fafc;border-radius:16px;">
                  <h2 style="color:#0d9488;margin-bottom:8px;">이음 — 매칭 초대가 도착했어요!</h2>
                  <p style="color:#374151;line-height:1.6;">
                    안녕하세요, <strong>%s</strong> 활동지원사님.<br>
                    이음 케어메이트로부터 아래 도움 요청에 대한 초대 쪽지가 도착했습니다.
                  </p>
                  <div style="margin:20px 0;padding:16px 20px;background:#fff;border-radius:12px;border-left:4px solid #0d9488;">
                    <p style="margin:0 0 12px;font-size:16px;font-weight:700;color:#111827;">%s</p>
                    <table style="width:100%%;border-collapse:collapse;font-size:14px;">%s</table>
                  </div>
                  <p style="color:#374151;line-height:1.6;">
                    사이트에서 확인 후 수락 또는 거절하실 수 있습니다.<br>
                    수락하시면 매칭이 확정되고 대화방이 열립니다.
                  </p>
                  <a href="%s"
                     style="display:inline-block;margin:24px 0 8px;padding:14px 28px;background:#0d9488;color:#fff;border-radius:10px;text-decoration:none;font-weight:600;">
                    사이트에서 확인하기
                  </a>
                  <p style="color:#9ca3af;font-size:13px;margin-top:16px;">
                    본인에게 온 초대가 아니라면 이 메일을 무시해 주세요.
                  </p>
                </div>
                """.formatted(caregiverName, req.getTitle(), detailRows, postUrl);

        // 본문은 트랜잭션 안에서 완성하고, 발송만 비동기로 넘긴다.
        // 동기 발송이면 SMTP가 멈추는 동안 이 트랜잭션의 DB 커넥션이 그대로 묶인다.
        asyncMailService.sendHtml(to, "[이음] 매칭 초대 쪽지가 도착했습니다 — " + req.getTitle(), html);
    }

    private String buildDetailRow(String label, String value) {
        if (value == null || value.isBlank()) return "";
        return "<tr><td style=\"color:#6b7280;padding:4px 8px 4px 0;white-space:nowrap;vertical-align:top;\">" + label
                + "</td><td style=\"color:#111827;padding:4px 0;\">" + value + "</td></tr>";
    }

    public Long acceptInvitation(Long invitationId) {
        User currentUser = requireRole(UserRole.CAREGIVER);
        CaregiverInvitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new NotFoundException("초대를 찾을 수 없습니다."));
        if (!invitation.getCaregiver().getUserId().equals(currentUser.getId())) {
            throw new ForbiddenException("본인에게 온 초대만 수락할 수 있습니다.");
        }
        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new IllegalStateException("대기 중인 초대만 수락할 수 있습니다.");
        }
        HelpRequest helpRequest = invitation.getHelpRequest();
        if (helpRequest.getStatus() != HelpRequestStatus.OPEN) {
            throw new IllegalStateException("모집 중인 게시물만 수락할 수 있습니다.");
        }

        CaregiverProfile caregiver = invitation.getCaregiver();
        HelpRequestApplication application = applicationRepository.save(HelpRequestApplication.builder()
                .helpRequest(helpRequest)
                .caregiver(caregiver)
                .status(com.project.ieum.entity.ApplicationStatus.PENDING)
                .build());
        Conversation conversation = conversationRepository.save(com.project.ieum.entity.conversation.Conversation.builder()
                .application(application)
                .requester(helpRequest.getRequester())
                .caregiver(caregiver)
                .status(ConversationStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .lastMessageAt(LocalDateTime.now())
                .build());

        invitation.accept();
        application.accept();
        helpRequest.match();

        applicationRepository.findByHelpRequest_IdAndStatus(helpRequest.getId(), com.project.ieum.entity.ApplicationStatus.PENDING)
                .forEach(app -> {
                    if (!app.getId().equals(application.getId())) {
                        app.cancel();
                        conversationRepository.findByApplication_Id(app.getId()).ifPresent(Conversation::close);
                    }
                });
        invitationRepository.findByHelpRequest_IdAndStatus(helpRequest.getId(), InvitationStatus.PENDING)
                .forEach(inv -> {
                    if (!inv.getId().equals(invitation.getId())) inv.reject();
                });

        notificationService.create(
                helpRequest.getRequester().getUser(),
                NotificationType.INVITATION,
                "초대를 수락했어요",
                caregiver.getFullName() + " 활동지원사가 '" + helpRequest.getTitle() + "' 초대를 수락했습니다.",
                "/chat/conversations/" + conversation.getId()
        );
        return conversation.getId();
    }

    public void rejectInvitation(Long invitationId) {
        User currentUser = requireRole(UserRole.CAREGIVER);
        CaregiverInvitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new NotFoundException("초대를 찾을 수 없습니다."));
        if (!invitation.getCaregiver().getUserId().equals(currentUser.getId())) {
            throw new ForbiddenException("본인에게 온 초대만 거절할 수 있습니다.");
        }
        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new IllegalStateException("대기 중인 초대만 거절할 수 있습니다.");
        }
        invitation.reject();

        notificationService.create(
                invitation.getHelpRequest().getRequester().getUser(),
                NotificationType.INVITATION,
                "초대를 거절했어요",
                invitation.getCaregiver().getFullName() + " 활동지원사가 '" + invitation.getHelpRequest().getTitle() + "' 초대를 거절했습니다.",
                "/disabled/board/" + invitation.getHelpRequest().getId()
        );
    }

    @Transactional(readOnly = true)
    public java.util.Optional<CaregiverInvitation> getMyPendingInvitation(Long requestId, Long caregiverId) {
        return invitationRepository.findByHelpRequest_IdAndCaregiver_UserId(requestId, caregiverId)
                .filter(inv -> inv.getStatus() == InvitationStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public List<HelpRequestApplication> getMyApplications() {
        User currentUser = requireRole(UserRole.CAREGIVER);
        return applicationRepository.findByCaregiver_UserIdOrderByCreatedAtDesc(currentUser.getId());
    }

    @Transactional(readOnly = true)
    public HelpRequestApplication findAcceptedApplication(Long requestId) {
        return applicationRepository.findByHelpRequest_IdAndStatusIn(
                        requestId, List.of(ApplicationStatus.ACCEPTED, ApplicationStatus.COMPLETED))
                .stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException("확정된 매칭을 찾을 수 없습니다."));
    }

    // 상세 페이지 핸드셰이크 패널용 파생 뷰. 현재 사용자가 매칭 참여자가 아니면 hidden.
    @Transactional(readOnly = true)
    public ActivityHandshakeView getHandshakeView(Long requestId, Long userId) {
        HelpRequest helpRequest = getRequest(requestId);
        HelpRequestStatus status = helpRequest.getStatus();
        if (status != HelpRequestStatus.MATCHED && status != HelpRequestStatus.IN_PROGRESS) {
            return ActivityHandshakeView.hidden();
        }
        HelpRequestApplication matched = applicationRepository
                .findByHelpRequest_IdAndStatus(requestId, ApplicationStatus.ACCEPTED)
                .stream().findFirst().orElse(null);
        if (matched == null) {
            return ActivityHandshakeView.hidden();
        }

        final ConfirmParty party;
        if (helpRequest.getRequester().getUserId().equals(userId)) {
            party = ConfirmParty.REQUESTER;
        } else if (matched.getCaregiver().getUserId().equals(userId)) {
            party = ConfirmParty.CAREGIVER;
        } else {
            return ActivityHandshakeView.hidden();
        }
        ConfirmParty other = (party == ConfirmParty.REQUESTER) ? ConfirmParty.CAREGIVER : ConfirmParty.REQUESTER;

        if (status == HelpRequestStatus.MATCHED) {
            return new ActivityHandshakeView(true, "START",
                    matched.startConfirmedBy(party), matched.startConfirmedBy(other), matched.bothStartConfirmed());
        }
        return new ActivityHandshakeView(true, "END",
                matched.endConfirmedBy(party), matched.endConfirmedBy(other), matched.bothEndConfirmed());
    }

    // 상세 페이지(이용자 측)에서 선정된 활동지원사·대화방을 보여주기 위한 파생 뷰.
    // ACCEPTED 지원이 없으면(아직 미선정·이미 종료) Optional.empty.
    @Transactional(readOnly = true)
    public java.util.Optional<MatchedPartyView> getMatchedParty(Long requestId) {
        return applicationRepository.findByHelpRequest_IdAndStatus(requestId, ApplicationStatus.ACCEPTED)
                .stream().findFirst()
                .map(app -> new MatchedPartyView(
                        app.getCaregiver().getFullName(),
                        conversationRepository.findByApplication_Id(app.getId())
                                .map(Conversation::getId)
                                .orElse(null)));
    }

    // ── 시간 기반 자동전이 (스케줄러가 호출) ──
    // 상태 가드로 멱등: 전이 후 상태가 바뀌어 다음 주기엔 같은 행이 재선택되지 않는다.
    @Transactional
    public void runLifecycleTransitions(LocalDateTime now) {
        expireOpenRequests(now);          // OPEN  : now > 희망시작 − 1h → CLOSED + 지원 일괄 CANCELLED
        closeNoShowMatches(now);          // MATCHED: now > 희망시작 + 30m → CLOSED + 선정 지원 CANCELLED
        autoCompleteOverdueActivities(now); // IN_PROGRESS: now > 희망종료 + 30m → COMPLETED
        closeCompletedConversations(now); // COMPLETED 대화방: 희망종료 + 1h 경과 → CLOSED
    }

    private void expireOpenRequests(LocalDateTime now) {
        for (HelpRequest helpRequest : helpRequestRepository
                .findByStatusAndDesiredStartDatetimeBefore(HelpRequestStatus.OPEN, now.plusHours(1))) {
            helpRequest.close();
            cancelApplicationsAndCloseConversations(helpRequest.getId(), ApplicationStatus.PENDING);
            notificationService.create(
                    helpRequest.getRequester().getUser(),
                    NotificationType.MATCHING,
                    "도움 요청이 자동 마감됐어요",
                    "'" + helpRequest.getTitle() + "' 요청의 희망 시작 시간이 지나 자동 마감되었습니다.",
                    "/disabled/board/" + helpRequest.getId()
            );
        }
    }

    private void closeNoShowMatches(LocalDateTime now) {
        for (HelpRequest helpRequest : helpRequestRepository
                .findByStatusAndDesiredStartDatetimeBefore(HelpRequestStatus.MATCHED, now.minusMinutes(30))) {
            helpRequest.close();
            cancelApplicationsAndCloseConversations(helpRequest.getId(), ApplicationStatus.ACCEPTED);
        }
    }

    private void autoCompleteOverdueActivities(LocalDateTime now) {
        for (HelpRequest helpRequest : helpRequestRepository
                .findByStatusAndDesiredEndDatetimeBefore(HelpRequestStatus.IN_PROGRESS, now.minusMinutes(30))) {
            helpRequest.complete();
            applicationRepository.findByHelpRequest_IdAndStatus(helpRequest.getId(), ApplicationStatus.ACCEPTED)
                    .stream().findFirst().ifPresent(HelpRequestApplication::complete);
        }
    }

    private void closeCompletedConversations(LocalDateTime now) {
        conversationRepository.findActiveConversationsForCompletedRequestsEndedBefore(now.minusHours(1))
                .forEach(Conversation::close);
    }

    private void cancelApplicationsAndCloseConversations(Long requestId, ApplicationStatus targetStatus) {
        applicationRepository.findByHelpRequest_IdAndStatus(requestId, targetStatus)
                .forEach(application -> {
                    application.cancel();
                    conversationRepository.findByApplication_Id(application.getId()).ifPresent(Conversation::close);
                });
    }

    // 현재 사용자를 매칭의 확인 주체(이용자/활동지원사)로 해석. 둘 다 아니면 거부.
    private ConfirmParty resolveParty(HelpRequest helpRequest, HelpRequestApplication matched, Long userId) {
        if (helpRequest.getRequester().getUserId().equals(userId)) {
            return ConfirmParty.REQUESTER;
        }
        if (matched.getCaregiver().getUserId().equals(userId)) {
            return ConfirmParty.CAREGIVER;
        }
        throw new ForbiddenException("매칭 참여자만 활동 상태를 변경할 수 있습니다.");
    }

    // 핸드셰이크 전이 성사 시 양측에게 동일 알림(대화방 링크).
    private void notifyBothParties(HelpRequest helpRequest, HelpRequestApplication matched,
                                   String title, String body) {
        String link = conversationRepository.findByApplication_Id(matched.getId())
                .map(conversation -> "/chat/conversations/" + conversation.getId())
                .orElse("/chat/conversations");
        notificationService.create(helpRequest.getRequester().getUser(), NotificationType.MATCHING, title, body, link);
        notificationService.create(matched.getCaregiver().getUser(), NotificationType.MATCHING, title, body, link);
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
