package com.project.ieum.service;

import com.project.ieum.entity.ApplicationStatus;
import com.project.ieum.entity.User;
import com.project.ieum.entity.UserRole;
import com.project.ieum.entity.caregiver.CaregiverProfile;
import com.project.ieum.entity.conversation.Conversation;
import com.project.ieum.entity.conversation.ConversationStatus;
import com.project.ieum.entity.request.ConfirmParty;
import com.project.ieum.entity.request.HelpRequest;
import com.project.ieum.entity.request.HelpRequestApplication;
import com.project.ieum.entity.request.HelpRequestStatus;
import com.project.ieum.entity.user.UserProfile;
import com.project.ieum.exception.ForbiddenException;
import com.project.ieum.repository.*;
import com.project.ieum.service.common.CurrentUserService;
import com.project.ieum.service.notification.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * {@link MatchingService} 단위 검증 (Mockito).
 *
 * <p>이슈 #11 PR-1 정본교정 4건의 회귀가드 포함:
 * ① accept() 미선택자 CANCELLED(REJECTED 아님)
 * ② cancelMatch() CLOSED(OPEN 재개방 없음) + 지원서 CANCELLED + 대화방 CLOSED
 */
@ExtendWith(MockitoExtension.class)
class MatchingServiceTest {

    @Mock HelpRequestRepository helpRequestRepository;
    @Mock HelpRequestApplicationRepository applicationRepository;
    @Mock CaregiverProfileRepository caregiverProfileRepository;
    @Mock ConversationRepository conversationRepository;
    @Mock MessageRepository messageRepository;
    @Mock CurrentUserService currentUserService;
    @Mock NotificationService notificationService;
    @Mock HelpRequestPersonalityTagRepository helpRequestPersonalityTagRepository;
    @Mock CaregiverPersonalityTagRepository caregiverPersonalityTagRepository;

    @InjectMocks
    MatchingService matchingService;

    // ── accept() ────────────────────────────────────────────────────

    @Nested
    @DisplayName("수락(accept)")
    class Accept {

        @Test
        @DisplayName("수락된 지원자는 ACCEPTED, 요청은 MATCHED 상태가 된다")
        void accept_selectedBecomesAcceptedAndRequestBecomesMatched() {
            loginAsUser(1L);
            HelpRequest helpRequest = helpRequest(10L, userProfile(1L), HelpRequestStatus.OPEN);
            HelpRequestApplication selected = application(1L, helpRequest, caregiver(2L), ApplicationStatus.PENDING);

            when(applicationRepository.findById(1L)).thenReturn(Optional.of(selected));
            when(applicationRepository.findByHelpRequest_IdAndStatus(10L, ApplicationStatus.PENDING))
                    .thenReturn(List.of(selected));
            when(conversationRepository.findByApplication_Id(1L)).thenReturn(Optional.empty());

            matchingService.accept(1L);

            assertThat(selected.getStatus()).isEqualTo(ApplicationStatus.ACCEPTED);
            assertThat(helpRequest.getStatus()).isEqualTo(HelpRequestStatus.MATCHED);
        }

        @Test
        @DisplayName("선택되지 않은 지원자는 CANCELLED — REJECTED 아님 (버그 ① 회귀가드)")
        void accept_nonSelectedApplicationsBecomeCANCELLED_notREJECTED() {
            loginAsUser(1L);
            HelpRequest helpRequest = helpRequest(10L, userProfile(1L), HelpRequestStatus.OPEN);
            HelpRequestApplication selected = application(1L, helpRequest, caregiver(2L), ApplicationStatus.PENDING);
            HelpRequestApplication other   = application(2L, helpRequest, caregiver(3L), ApplicationStatus.PENDING);
            Conversation otherConv = conversation(20L);

            when(applicationRepository.findById(1L)).thenReturn(Optional.of(selected));
            when(applicationRepository.findByHelpRequest_IdAndStatus(10L, ApplicationStatus.PENDING))
                    .thenReturn(List.of(selected, other));
            when(conversationRepository.findByApplication_Id(2L)).thenReturn(Optional.of(otherConv));
            when(conversationRepository.findByApplication_Id(1L)).thenReturn(Optional.empty());

            matchingService.accept(1L);

            assertThat(other.getStatus()).isEqualTo(ApplicationStatus.CANCELLED);
            assertThat(other.getStatus()).isNotEqualTo(ApplicationStatus.REJECTED);
        }

        @Test
        @DisplayName("선택되지 않은 지원자의 대화방은 즉시 CLOSED된다")
        void accept_nonSelectedConversationIsClosed() {
            loginAsUser(1L);
            HelpRequest helpRequest = helpRequest(10L, userProfile(1L), HelpRequestStatus.OPEN);
            HelpRequestApplication selected = application(1L, helpRequest, caregiver(2L), ApplicationStatus.PENDING);
            HelpRequestApplication other   = application(2L, helpRequest, caregiver(3L), ApplicationStatus.PENDING);
            Conversation otherConv = conversation(20L);

            when(applicationRepository.findById(1L)).thenReturn(Optional.of(selected));
            when(applicationRepository.findByHelpRequest_IdAndStatus(10L, ApplicationStatus.PENDING))
                    .thenReturn(List.of(selected, other));
            when(conversationRepository.findByApplication_Id(2L)).thenReturn(Optional.of(otherConv));
            when(conversationRepository.findByApplication_Id(1L)).thenReturn(Optional.empty());

            matchingService.accept(1L);

            assertThat(otherConv.getStatus()).isEqualTo(ConversationStatus.CLOSED);
        }

        @Test
        @DisplayName("요청자가 아닌 사용자가 수락하면 ForbiddenException")
        void accept_notOwner_throws() {
            loginAsUser(99L);
            HelpRequest helpRequest = helpRequest(10L, userProfile(1L), HelpRequestStatus.OPEN);
            HelpRequestApplication app = application(1L, helpRequest, caregiver(2L), ApplicationStatus.PENDING);

            when(applicationRepository.findById(1L)).thenReturn(Optional.of(app));

            assertThatThrownBy(() -> matchingService.accept(1L))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("PENDING 상태가 아닌 지원을 수락하면 IllegalStateException")
        void accept_notPending_throws() {
            loginAsUser(1L);
            HelpRequest helpRequest = helpRequest(10L, userProfile(1L), HelpRequestStatus.MATCHED);
            HelpRequestApplication app = application(1L, helpRequest, caregiver(2L), ApplicationStatus.ACCEPTED);

            when(applicationRepository.findById(1L)).thenReturn(Optional.of(app));

            assertThatThrownBy(() -> matchingService.accept(1L))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    // ── cancelMatch() ───────────────────────────────────────────────

    @Nested
    @DisplayName("매칭취소(cancelMatch)")
    class CancelMatch {

        @Test
        @DisplayName("매칭취소 시 지원서는 CANCELLED, 요청은 CLOSED — OPEN 재개방 없음 (버그 ② 회귀가드)")
        void cancelMatch_applicationCancelledAndRequestClosed_notReopened() {
            loginAsUser(1L);
            HelpRequest helpRequest = helpRequest(10L, userProfile(1L), HelpRequestStatus.MATCHED);
            HelpRequestApplication app = application(5L, helpRequest, caregiver(2L), ApplicationStatus.ACCEPTED);

            when(applicationRepository.findById(5L)).thenReturn(Optional.of(app));
            when(conversationRepository.findByApplication_Id(5L)).thenReturn(Optional.empty());

            matchingService.cancelMatch(5L);

            assertThat(app.getStatus()).isEqualTo(ApplicationStatus.CANCELLED);
            assertThat(helpRequest.getStatus()).isEqualTo(HelpRequestStatus.CLOSED);
            assertThat(helpRequest.getStatus()).isNotEqualTo(HelpRequestStatus.OPEN);
        }

        @Test
        @DisplayName("매칭취소 시 대화방이 CLOSED된다 (버그 ② 회귀가드)")
        void cancelMatch_conversationIsClosed() {
            loginAsUser(1L);
            HelpRequest helpRequest = helpRequest(10L, userProfile(1L), HelpRequestStatus.MATCHED);
            HelpRequestApplication app = application(5L, helpRequest, caregiver(2L), ApplicationStatus.ACCEPTED);
            Conversation conv = conversation(30L);

            when(applicationRepository.findById(5L)).thenReturn(Optional.of(app));
            when(conversationRepository.findByApplication_Id(5L)).thenReturn(Optional.of(conv));

            matchingService.cancelMatch(5L);

            assertThat(conv.getStatus()).isEqualTo(ConversationStatus.CLOSED);
        }

        @Test
        @DisplayName("ACCEPTED 상태가 아닌 지원을 매칭취소하면 IllegalStateException")
        void cancelMatch_notAccepted_throws() {
            loginAsUser(1L);
            HelpRequest helpRequest = helpRequest(10L, userProfile(1L), HelpRequestStatus.OPEN);
            HelpRequestApplication app = application(5L, helpRequest, caregiver(2L), ApplicationStatus.PENDING);

            when(applicationRepository.findById(5L)).thenReturn(Optional.of(app));

            assertThatThrownBy(() -> matchingService.cancelMatch(5L))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    // ── confirmStart() / confirmEnd() 핸드셰이크 ─────────────────────

    @Nested
    @DisplayName("활동 시작/종료 양측 확인(handshake)")
    class Handshake {

        @Test
        @DisplayName("한쪽만 시작 확인하면 전이 없이 확인 플래그만 선다 (MATCHED 유지)")
        void confirmStart_oneParty_noTransition() {
            loginAsUser(1L); // 이용자(요청자)
            HelpRequest helpRequest = helpRequest(10L, userProfile(1L), HelpRequestStatus.MATCHED);
            HelpRequestApplication matched = application(5L, helpRequest, caregiver(2L), ApplicationStatus.ACCEPTED);

            when(helpRequestRepository.findById(10L)).thenReturn(Optional.of(helpRequest));
            when(applicationRepository.findByHelpRequest_IdAndStatusIn(10L, List.of(ApplicationStatus.ACCEPTED, ApplicationStatus.COMPLETED)))
                    .thenReturn(List.of(matched));

            matchingService.confirmStart(10L);

            assertThat(matched.startConfirmedBy(ConfirmParty.REQUESTER)).isTrue();
            assertThat(matched.bothStartConfirmed()).isFalse();
            assertThat(helpRequest.getStatus()).isEqualTo(HelpRequestStatus.MATCHED);
        }

        @Test
        @DisplayName("양측이 시작 확인하면 IN_PROGRESS로 전이된다")
        void confirmStart_bothParties_transitionsToInProgress() {
            loginAsCaregiver(2L); // 도우미가 두 번째로 확인
            HelpRequest helpRequest = helpRequest(10L, userProfile(1L), HelpRequestStatus.MATCHED);
            HelpRequestApplication matched = application(5L, helpRequest, caregiver(2L), ApplicationStatus.ACCEPTED)
                    .toBuilder().requesterStartConfirmed(true).build();

            when(helpRequestRepository.findById(10L)).thenReturn(Optional.of(helpRequest));
            when(applicationRepository.findByHelpRequest_IdAndStatusIn(10L, List.of(ApplicationStatus.ACCEPTED, ApplicationStatus.COMPLETED)))
                    .thenReturn(List.of(matched));
            when(conversationRepository.findByApplication_Id(5L)).thenReturn(Optional.empty());

            matchingService.confirmStart(10L);

            assertThat(matched.bothStartConfirmed()).isTrue();
            assertThat(helpRequest.getStatus()).isEqualTo(HelpRequestStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("매칭 참여자가 아니면 ForbiddenException")
        void confirmStart_notParticipant_throws() {
            loginAsUser(99L);
            HelpRequest helpRequest = helpRequest(10L, userProfile(1L), HelpRequestStatus.MATCHED);
            HelpRequestApplication matched = application(5L, helpRequest, caregiver(2L), ApplicationStatus.ACCEPTED);

            when(helpRequestRepository.findById(10L)).thenReturn(Optional.of(helpRequest));
            when(applicationRepository.findByHelpRequest_IdAndStatusIn(10L, List.of(ApplicationStatus.ACCEPTED, ApplicationStatus.COMPLETED)))
                    .thenReturn(List.of(matched));

            assertThatThrownBy(() -> matchingService.confirmStart(10L))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("MATCHED가 아니면 시작 확인은 IllegalStateException")
        void confirmStart_notMatched_throws() {
            loginAsUser(1L);
            HelpRequest helpRequest = helpRequest(10L, userProfile(1L), HelpRequestStatus.OPEN);

            when(helpRequestRepository.findById(10L)).thenReturn(Optional.of(helpRequest));

            assertThatThrownBy(() -> matchingService.confirmStart(10L))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("양측이 종료 확인하면 COMPLETED로 전이되고 지원도 COMPLETED가 된다")
        void confirmEnd_bothParties_completes() {
            loginAsCaregiver(2L);
            HelpRequest helpRequest = helpRequest(10L, userProfile(1L), HelpRequestStatus.IN_PROGRESS);
            HelpRequestApplication matched = application(5L, helpRequest, caregiver(2L), ApplicationStatus.ACCEPTED)
                    .toBuilder().requesterEndConfirmed(true).build();

            when(helpRequestRepository.findById(10L)).thenReturn(Optional.of(helpRequest));
            when(applicationRepository.findByHelpRequest_IdAndStatusIn(10L, List.of(ApplicationStatus.ACCEPTED, ApplicationStatus.COMPLETED)))
                    .thenReturn(List.of(matched));
            when(conversationRepository.findByApplication_Id(5L)).thenReturn(Optional.empty());

            matchingService.confirmEnd(10L);

            assertThat(helpRequest.getStatus()).isEqualTo(HelpRequestStatus.COMPLETED);
            assertThat(matched.getStatus()).isEqualTo(ApplicationStatus.COMPLETED);
        }

        @Test
        @DisplayName("IN_PROGRESS가 아니면 종료 확인은 IllegalStateException")
        void confirmEnd_notInProgress_throws() {
            loginAsUser(1L);
            HelpRequest helpRequest = helpRequest(10L, userProfile(1L), HelpRequestStatus.MATCHED);

            when(helpRequestRepository.findById(10L)).thenReturn(Optional.of(helpRequest));

            assertThatThrownBy(() -> matchingService.confirmEnd(10L))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    // ── apply() ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("지원(apply)")
    class Apply {

        @Test
        @DisplayName("OPEN이 아닌 요청에 지원하면 IllegalStateException")
        void apply_nonOpenRequest_throws() {
            loginAsCaregiver(2L);
            when(caregiverProfileRepository.findById(2L)).thenReturn(Optional.of(caregiver(2L)));
            when(helpRequestRepository.findById(10L))
                    .thenReturn(Optional.of(helpRequest(10L, userProfile(1L), HelpRequestStatus.MATCHED)));

            assertThatThrownBy(() -> matchingService.apply(10L, null))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("이미 지원한 요청에 재지원하면 IllegalStateException")
        void apply_duplicateApplication_throws() {
            loginAsCaregiver(2L);
            when(caregiverProfileRepository.findById(2L)).thenReturn(Optional.of(caregiver(2L)));
            when(helpRequestRepository.findById(10L))
                    .thenReturn(Optional.of(helpRequest(10L, userProfile(1L), HelpRequestStatus.OPEN)));
            when(applicationRepository.existsByHelpRequest_IdAndCaregiver_UserId(10L, 2L)).thenReturn(true);

            assertThatThrownBy(() -> matchingService.apply(10L, null))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    // ── 스케줄러 시간 기반 자동전이(runLifecycleTransitions) ──────────

    @Nested
    @DisplayName("시간 기반 자동전이(scheduler)")
    class Scheduler {

        @Test
        @DisplayName("OPEN 만료: CLOSED + 대기 지원 일괄 CANCELLED + 대화방 CLOSED")
        void expireOpenRequests_closesAndCancelsPending() {
            HelpRequest open = helpRequest(10L, userProfile(1L), HelpRequestStatus.OPEN);
            HelpRequestApplication pending = application(1L, open, caregiver(2L), ApplicationStatus.PENDING);
            Conversation conv = conversation(20L);

            when(helpRequestRepository.findByStatusAndDesiredStartDatetimeBefore(eq(HelpRequestStatus.OPEN), any()))
                    .thenReturn(List.of(open));
            when(helpRequestRepository.findByStatusAndDesiredStartDatetimeBefore(eq(HelpRequestStatus.MATCHED), any()))
                    .thenReturn(List.of()); // 같은 파인더의 MATCHED 호출(노쇼 잡) — 이 테스트에선 대상 없음
            when(applicationRepository.findByHelpRequest_IdAndStatus(10L, ApplicationStatus.PENDING))
                    .thenReturn(List.of(pending));
            when(conversationRepository.findByApplication_Id(1L)).thenReturn(Optional.of(conv));

            matchingService.runLifecycleTransitions(LocalDateTime.now());

            assertThat(open.getStatus()).isEqualTo(HelpRequestStatus.CLOSED);
            assertThat(pending.getStatus()).isEqualTo(ApplicationStatus.CANCELLED);
            assertThat(conv.getStatus()).isEqualTo(ConversationStatus.CLOSED);
        }

        @Test
        @DisplayName("MATCHED 노쇼: CLOSED + 선정 지원 CANCELLED + 대화방 CLOSED")
        void closeNoShowMatches_closesAndCancelsAccepted() {
            HelpRequest matched = helpRequest(10L, userProfile(1L), HelpRequestStatus.MATCHED);
            HelpRequestApplication accepted = application(5L, matched, caregiver(2L), ApplicationStatus.ACCEPTED);
            Conversation conv = conversation(30L);

            when(helpRequestRepository.findByStatusAndDesiredStartDatetimeBefore(eq(HelpRequestStatus.OPEN), any()))
                    .thenReturn(List.of()); // 같은 파인더의 OPEN 호출(만료 잡) — 이 테스트에선 대상 없음
            when(helpRequestRepository.findByStatusAndDesiredStartDatetimeBefore(eq(HelpRequestStatus.MATCHED), any()))
                    .thenReturn(List.of(matched));
            when(applicationRepository.findByHelpRequest_IdAndStatus(10L, ApplicationStatus.ACCEPTED))
                    .thenReturn(List.of(accepted));
            when(conversationRepository.findByApplication_Id(5L)).thenReturn(Optional.of(conv));

            matchingService.runLifecycleTransitions(LocalDateTime.now());

            assertThat(matched.getStatus()).isEqualTo(HelpRequestStatus.CLOSED);
            assertThat(accepted.getStatus()).isEqualTo(ApplicationStatus.CANCELLED);
            assertThat(conv.getStatus()).isEqualTo(ConversationStatus.CLOSED);
        }

        @Test
        @DisplayName("IN_PROGRESS 종료누락: 요청·지원 모두 COMPLETED")
        void autoCompleteOverdue_completesRequestAndApplication() {
            HelpRequest inProgress = helpRequest(10L, userProfile(1L), HelpRequestStatus.IN_PROGRESS);
            HelpRequestApplication accepted = application(5L, inProgress, caregiver(2L), ApplicationStatus.ACCEPTED);

            when(helpRequestRepository.findByStatusAndDesiredEndDatetimeBefore(eq(HelpRequestStatus.IN_PROGRESS), any()))
                    .thenReturn(List.of(inProgress));
            when(applicationRepository.findByHelpRequest_IdAndStatus(10L, ApplicationStatus.ACCEPTED))
                    .thenReturn(List.of(accepted));

            matchingService.runLifecycleTransitions(LocalDateTime.now());

            assertThat(inProgress.getStatus()).isEqualTo(HelpRequestStatus.COMPLETED);
            assertThat(accepted.getStatus()).isEqualTo(ApplicationStatus.COMPLETED);
        }

        @Test
        @DisplayName("COMPLETED 매칭의 열린 대화방은 희망종료+1h 경과 시 CLOSED")
        void closeCompletedConversations_closesActiveOnes() {
            Conversation conv = conversation(40L);
            when(conversationRepository.findActiveConversationsForCompletedRequestsEndedBefore(any()))
                    .thenReturn(List.of(conv));

            matchingService.runLifecycleTransitions(LocalDateTime.now());

            assertThat(conv.getStatus()).isEqualTo(ConversationStatus.CLOSED);
        }
    }

    // ── 픽스처 헬퍼 ────────────────────────────────────────────────

    private void loginAsUser(Long userId) {
        when(currentUserService.getCurrentUser())
                .thenReturn(User.builder().id(userId).role(UserRole.USER).build());
    }

    private void loginAsCaregiver(Long userId) {
        when(currentUserService.getCurrentUser())
                .thenReturn(User.builder().id(userId).role(UserRole.CAREGIVER).build());
    }

    private UserProfile userProfile(Long userId) {
        return UserProfile.builder().userId(userId).fullName("이용자").build();
    }

    private CaregiverProfile caregiver(Long userId) {
        User user = User.builder().id(userId).role(UserRole.CAREGIVER).build();
        return CaregiverProfile.builder().userId(userId).user(user).fullName("활동지원사").build();
    }

    private HelpRequest helpRequest(Long id, UserProfile requester, HelpRequestStatus status) {
        return HelpRequest.builder()
                .id(id)
                .requester(requester)
                .status(status)
                .title("도움 요청")
                .desiredStartDatetime(LocalDateTime.of(2026, 6, 15, 10, 0))
                .build();
    }

    private HelpRequestApplication application(Long id, HelpRequest helpRequest,
                                               CaregiverProfile caregiver, ApplicationStatus status) {
        return HelpRequestApplication.builder()
                .id(id)
                .helpRequest(helpRequest)
                .caregiver(caregiver)
                .status(status)
                .build();
    }

    private Conversation conversation(Long id) {
        return Conversation.builder()
                .id(id)
                .status(ConversationStatus.ACTIVE)
                .build();
    }
}
