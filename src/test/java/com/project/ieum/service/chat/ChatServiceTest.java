package com.project.ieum.service.chat;

import com.project.ieum.dto.chat.MessageResponse;
import com.project.ieum.dto.chat.SendMessageRequest;
import com.project.ieum.entity.User;
import com.project.ieum.entity.UserRole;
import com.project.ieum.entity.UserStatus;
import com.project.ieum.entity.caregiver.CaregiverProfile;
import com.project.ieum.entity.conversation.Conversation;
import com.project.ieum.entity.conversation.ConversationStatus;
import com.project.ieum.entity.conversation.Message;
import com.project.ieum.entity.notification.NotificationType;
import com.project.ieum.entity.user.UserProfile;
import com.project.ieum.repository.ChatAnnouncementRepository;
import com.project.ieum.repository.ConversationRepository;
import com.project.ieum.repository.MessageRepository;
import com.project.ieum.service.common.CurrentUserService;
import com.project.ieum.service.notification.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 채팅 알림·응답에 이메일이 실리지 않는지에 대한 회귀가드.
 *
 * <p>{@code UserDisplayNameTest}는 유틸 자체만 본다. 그 유틸을 만들어 두고 호출부를 되돌려도
 * 유틸 테스트는 그대로 통과하므로, <b>서비스가 실제로 무엇을 알림에 담는지</b>를 여기서 고정한다.
 *
 * <p>관리자 발신을 함께 두는 이유는 프로필이 없는 유일한 실제 경우이기 때문이다. 무가드로
 * {@code getProfile().getFullName()}을 쓰면 여기서 NPE가 나고, 이메일로 되돌아가면 고치려던
 * 문제가 그대로 남는다.
 */
@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    private static final String REQUESTER_EMAIL = "requester@example.com";
    private static final String CAREGIVER_EMAIL = "caregiver@example.com";
    private static final String ADMIN_EMAIL = "admin@example.com";

    @Mock private ConversationRepository conversationRepository;
    @Mock private MessageRepository messageRepository;
    @Mock private ChatAnnouncementRepository chatAnnouncementRepository;
    @Mock private CurrentUserService currentUserService;
    @Mock private NotificationService notificationService;

    private ChatService chatService;

    private User requesterUser;
    private User caregiverUser;
    private Conversation conversation;

    @BeforeEach
    void setUp() {
        chatService = new ChatService(conversationRepository, messageRepository,
                chatAnnouncementRepository, currentUserService, notificationService);

        // JPA는 양방향을 알아서 이어 주지만 단위 테스트에서는 손으로 이어야 한다.
        // User.profile을 빠뜨리면 표시 이름이 조용히 '익명'이 되어 엉뚱한 것을 검증하게 되고,
        // Profile.user를 빠뜨리면 알림 수신자가 null이 된다.
        requesterUser = user(1L, REQUESTER_EMAIL, UserRole.USER).toBuilder()
                .profile(UserProfile.builder().userId(1L).fullName("홍길동").build())
                .build();
        caregiverUser = user(2L, CAREGIVER_EMAIL, UserRole.CAREGIVER).toBuilder()
                .profile(CaregiverProfile.builder().userId(2L).fullName("김돌봄").build())
                .build();

        conversation = Conversation.builder()
                .id(100L)
                .requester(((UserProfile) requesterUser.getProfile()).toBuilder().user(requesterUser).build())
                .caregiver(((CaregiverProfile) caregiverUser.getProfile()).toBuilder().user(caregiverUser).build())
                .status(ConversationStatus.ACTIVE)
                .build();
    }

    private static User user(Long id, String email, UserRole role) {
        return User.builder().id(id).email(email).role(role).status(UserStatus.ACTIVE).build();
    }

    private MessageResponse send(User sender) {
        when(currentUserService.getByEmail(sender.getEmail())).thenReturn(sender);
        when(conversationRepository.findWithParticipantsById(100L)).thenReturn(Optional.of(conversation));
        when(messageRepository.save(any(Message.class))).thenAnswer(i -> i.getArgument(0));

        SendMessageRequest request = new SendMessageRequest();
        request.setBody("안녕하세요");
        return chatService.sendMessage(100L, sender.getEmail(), request);
    }

    private String capturedNotificationContent() {
        ArgumentCaptor<String> content = ArgumentCaptor.forClass(String.class);
        verify(notificationService).create(any(User.class), eq(NotificationType.MESSAGE),
                any(String.class), content.capture(), any(String.class));
        return content.getValue();
    }

    @Test
    @DisplayName("활동지원사가 보낸 메시지 — 알림에 이름이 들어가고 이메일은 들어가지 않는다")
    void notificationUsesNameNotEmail() {
        send(caregiverUser);

        String content = capturedNotificationContent();
        assertThat(content).isEqualTo("김돌봄님이 메시지를 보냈습니다.");
        assertThat(content).doesNotContain(CAREGIVER_EMAIL).doesNotContain("@");
    }

    @Test
    @DisplayName("관리자가 보낸 메시지 — 프로필이 없어도 NPE 없이 호칭으로 대체된다")
    void adminSenderDoesNotCrashOrLeakEmail() {
        User admin = user(9L, ADMIN_EMAIL, UserRole.ADMIN);

        send(admin);

        String content = capturedNotificationContent();
        assertThat(content).isEqualTo("관리자님이 메시지를 보냈습니다.");
        assertThat(content).doesNotContain("@");
    }

    @Test
    @DisplayName("응답의 senderName도 이메일로 되돌아가지 않는다")
    void responseSenderNameNeverFallsBackToEmail() {
        assertThat(send(caregiverUser).getSenderName()).isEqualTo("김돌봄");

        User admin = user(9L, ADMIN_EMAIL, UserRole.ADMIN);
        assertThat(send(admin).getSenderName()).isEqualTo("관리자").doesNotContain("@");
    }
}
