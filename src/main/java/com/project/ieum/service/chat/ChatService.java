package com.project.ieum.service.chat;

import com.project.ieum.dto.chat.AnnouncementResponse;
import com.project.ieum.dto.chat.ConversationSummaryResponse;
import com.project.ieum.dto.chat.MessageResponse;
import com.project.ieum.dto.chat.SendMessageRequest;
import com.project.ieum.entity.User;
import com.project.ieum.entity.UserRole;
import com.project.ieum.entity.conversation.ChatAnnouncement;
import com.project.ieum.entity.conversation.Conversation;
import com.project.ieum.entity.conversation.ConversationStatus;
import com.project.ieum.entity.conversation.Message;
import com.project.ieum.entity.notification.NotificationType;
import com.project.ieum.repository.ChatAnnouncementRepository;
import com.project.ieum.repository.ConversationRepository;
import com.project.ieum.repository.MessageRepository;
import com.project.ieum.service.common.CurrentUserService;
import com.project.ieum.service.notification.NotificationService;
import com.project.ieum.util.UserDisplayName;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final ChatAnnouncementRepository chatAnnouncementRepository;
    private final CurrentUserService currentUserService;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public Page<ConversationSummaryResponse> getMyConversations(Pageable pageable) {
        User currentUser = currentUserService.getCurrentUser();
        return conversationRepository.findMyConversations(currentUser.getId(), pageable)
                .map(conversation -> toSummary(conversation, currentUser.getId()));
    }

    @Transactional(readOnly = true)
    public Page<MessageResponse> getMessages(Long conversationId, Pageable pageable) {
        User currentUser = currentUserService.getCurrentUser();
        Conversation conversation = getConversationForUser(conversationId, currentUser);
        return messageRepository.findByConversationIdOrderBySentAtDesc(conversation.getId(), pageable)
                .map(message -> MessageResponse.from(message, currentUser.getId()));
    }

    public MessageResponse sendMessage(Long conversationId, String senderEmail, SendMessageRequest request) {
        User sender = currentUserService.getByEmail(senderEmail);
        Conversation conversation = getConversationForUser(conversationId, sender);

        if (conversation.getStatus() != ConversationStatus.ACTIVE) {
            throw new IllegalStateException("종료된 대화방에는 메시지를 보낼 수 없습니다.");
        }

        String body = request.getBody() == null ? "" : request.getBody().trim();
        if (body.isBlank()) {
            throw new IllegalArgumentException("메시지를 입력해주세요.");
        }

        Message message = Message.builder()
                .conversation(conversation)
                .sender(sender)
                .body(body)
                .attachmentUrl(request.getAttachmentUrl())
                .attachmentType(request.getAttachmentType())
                .hasRead(false)
                .sentAt(LocalDateTime.now())
                .build();

        Message saved = messageRepository.save(message);
        conversation.touchLastMessage();

        User receiver = getOpponentUser(conversation, sender.getId());
        notificationService.create(
                receiver,
                NotificationType.MESSAGE,
                "새 메시지",
                UserDisplayName.of(sender) + "님이 메시지를 보냈습니다.",
                "/chat/conversations/" + conversationId
        );

        return MessageResponse.from(saved, sender.getId());
    }

    public int markRead(Long conversationId) {
        User currentUser = currentUserService.getCurrentUser();
        Conversation conversation = getConversationForUser(conversationId, currentUser);
        return messageRepository.markRead(conversation.getId(), currentUser.getId());
    }

    @Transactional(readOnly = true)
    public Conversation getConversationForUser(Long conversationId, User user) {
        Conversation conversation = conversationRepository.findWithParticipantsById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("대화방을 찾을 수 없습니다."));

        if (user.getRole() != UserRole.ADMIN && !isParticipant(conversation, user.getId())) {
            throw new SecurityException("대화방 참여자만 접근할 수 있습니다.");
        }
        return conversation;
    }

    @Transactional(readOnly = true)
    public Optional<AnnouncementResponse> getAnnouncement(Long conversationId) {
        return chatAnnouncementRepository.findByConversationId(conversationId)
                .map(AnnouncementResponse::from);
    }

    public AnnouncementResponse saveAnnouncement(Long conversationId, String body) {
        User admin = currentUserService.getCurrentUser();
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("대화방을 찾을 수 없습니다."));
        ChatAnnouncement announcement = chatAnnouncementRepository.findByConversationId(conversationId)
                .orElseGet(() -> chatAnnouncementRepository.save(ChatAnnouncement.builder()
                        .conversation(conversation)
                        .admin(admin)
                        .body(body)
                        .build()));
        if (!announcement.getBody().equals(body)) {
            announcement.updateBody(body);
        }
        return AnnouncementResponse.from(announcement);
    }

    private boolean isParticipant(Conversation conversation, Long userId) {
        return conversation.getRequester().getUserId().equals(userId)
                || conversation.getCaregiver().getUserId().equals(userId);
    }

    private User getOpponentUser(Conversation conversation, Long userId) {
        if (conversation.getRequester().getUserId().equals(userId)) {
            return conversation.getCaregiver().getUser();
        }
        return conversation.getRequester().getUser();
    }

    private ConversationSummaryResponse toSummary(Conversation conversation, Long currentUserId) {
        boolean requester = conversation.getRequester().getUserId().equals(currentUserId);
        Long opponentUserId = requester ? conversation.getCaregiver().getUserId() : conversation.getRequester().getUserId();
        String opponentName = requester ? conversation.getCaregiver().getFullName() : conversation.getRequester().getFullName();

        Message lastMessage = messageRepository.findTopByConversationIdOrderBySentAtDesc(conversation.getId())
                .orElse(null);
        long unreadCount = messageRepository.countUnread(conversation.getId(), currentUserId);

        return ConversationSummaryResponse.builder()
                .conversationId(conversation.getId())
                .opponentUserId(opponentUserId)
                .opponentName(opponentName)
                .status(conversation.getStatus())
                .lastMessage(lastMessage == null ? null : lastMessage.getBody())
                .lastMessageAt(lastMessage == null ? conversation.getLastMessageAt() : lastMessage.getSentAt())
                .unreadCount(unreadCount)
                .build();
    }
}
