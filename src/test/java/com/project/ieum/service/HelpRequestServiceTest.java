package com.project.ieum.service;

import com.project.ieum.dto.request.HelpRequestForm;
import com.project.ieum.entity.User;
import com.project.ieum.entity.UserRole;
import com.project.ieum.entity.request.HelpRequest;
import com.project.ieum.entity.request.HelpRequestStatus;
import com.project.ieum.entity.request.ServiceCategory;
import com.project.ieum.entity.user.UserProfile;
import com.project.ieum.exception.BadRequestException;
import com.project.ieum.exception.HelpRequestNotFoundException;
import com.project.ieum.exception.InvalidRequestStateException;
import com.project.ieum.exception.NotRequestOwnerException;
import com.project.ieum.exception.RequestTimeConflictException;
import com.project.ieum.repository.HelpRequestPersonalityTagRepository;
import com.project.ieum.repository.HelpRequestRepository;
import com.project.ieum.repository.PersonalityTagRepository;
import com.project.ieum.repository.ServiceCategoryRepository;
import com.project.ieum.repository.UserProfileRepository;
import com.project.ieum.service.common.CurrentUserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link HelpRequestService}(평면 정본) 단위 검증 (Mockito).
 *
 * <p>리포지토리·CurrentUserService는 모두 mock — 영속성이 아니라 서비스의 "검증·권한·상태 전이" 결정만 가드한다.
 * 시간대 겹침 쿼리 자체의 의미는 {@code HelpRequestRepositoryTest}(H2 슬라이스)가 담당하므로,
 * 여기선 existsOverlapping의 반환값에 서비스가 어떻게 반응하는지만 본다.
 *
 * <p>이슈 #8 정본 규칙을 회귀가드한다: 겹침 거부(4xx)·미등록 분류 400·생성 status=OPEN·
 * 권한 검사 순서(소유자→상태)·write-once(본문 수정 메서드 부재).
 */
@ExtendWith(MockitoExtension.class)
class HelpRequestServiceTest {

    @Mock
    private HelpRequestRepository helpRequestRepository;
    @Mock
    private UserProfileRepository userProfileRepository;
    @Mock
    private ServiceCategoryRepository serviceCategoryRepository;
    @Mock
    private PersonalityTagRepository personalityTagRepository;
    @Mock
    private HelpRequestPersonalityTagRepository helpRequestPersonalityTagRepository;
    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private HelpRequestService helpRequestService;

    @Nested
    @DisplayName("작성(create)")
    class Create {
        @Test
        @DisplayName("시간대가 겹치면 RequestTimeConflictException — 저장하지 않는다")
        void create_overlapping_throwsAndDoesNotSave() {
            // given
            loginAsUser(1L);
            when(userProfileRepository.findById(1L)).thenReturn(Optional.of(owner(1L)));
            when(helpRequestRepository.existsOverlapping(any(), any(), any(), any()))
                .thenReturn(true);

            // when / then
            assertThatThrownBy(() -> helpRequestService.create(createForm()))
                .isInstanceOf(RequestTimeConflictException.class);
            verify(helpRequestRepository, never()).save(any());
        }

        @Test
        @DisplayName("등록되지 않은 serviceCategoryId면 BadRequestException")
        void create_unknownCategory_throws() {
            // given
            loginAsUser(1L);
            when(userProfileRepository.findById(1L)).thenReturn(Optional.of(owner(1L)));
            when(helpRequestRepository.existsOverlapping(any(), any(), any(), any()))
                .thenReturn(false);
            when(serviceCategoryRepository.findById(any())).thenReturn(Optional.empty());

            // when / then
            assertThatThrownBy(() -> helpRequestService.create(createForm()))
                .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("정상 작성 시 상태는 OPEN으로 저장된다 (엔티티 @Builder.Default)")
        void create_valid_savesWithOpenStatus() {
            // given
            loginAsUser(1L);
            when(userProfileRepository.findById(1L)).thenReturn(Optional.of(owner(1L)));
            when(helpRequestRepository.existsOverlapping(any(), any(), any(), any()))
                .thenReturn(false);
            when(serviceCategoryRepository.findById(any())).thenReturn(Optional.of(category()));
            when(helpRequestRepository.save(any()))
                .thenReturn(HelpRequest.builder().id(100L).build());

            // when
            helpRequestService.create(createForm());

            // then — 서비스가 save에 넘긴 엔티티의 status가 OPEN인지 captor로 가드.
            // (서비스가 .status()를 누락해도 엔티티 default가 OPEN을 박는다 — 이 단언이 그 계약을 고정)
            ArgumentCaptor<HelpRequest> captor = ArgumentCaptor.forClass(HelpRequest.class);
            verify(helpRequestRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(HelpRequestStatus.OPEN);
        }
    }

    @Nested
    @DisplayName("조회(get)")
    class Read {
        @Test
        @DisplayName("존재하지 않는 id 조회는 HelpRequestNotFoundException")
        void get_notFound_throws() {
            // given
            when(helpRequestRepository.findById(any())).thenReturn(Optional.empty());

            // when / then
            assertThatThrownBy(() -> helpRequestService.get(20L))
                .isInstanceOf(HelpRequestNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("취소·마감(cancel)")
    class Cancel {
        @Test
        @DisplayName("게시자가 아니면 NotRequestOwnerException")
        void cancel_notOwner_throws() {
            // given — 로그인=2번, 요청 소유자=1번
            loginAsUser(2L);
            when(helpRequestRepository.findById(any()))
                .thenReturn(Optional.of(helpRequest(1L, HelpRequestStatus.OPEN)));

            // when / then
            assertThatThrownBy(() -> helpRequestService.cancel(50L))
                .isInstanceOf(NotRequestOwnerException.class);
        }

        @Test
        @DisplayName("OPEN이 아니면 InvalidRequestStateException")
        void cancel_notOpen_throws() {
            // given
            loginAsUser(1L);
            when(helpRequestRepository.findById(any()))
                .thenReturn(Optional.of(helpRequest(1L, HelpRequestStatus.MATCHED)));

            // when / then
            assertThatThrownBy(() -> helpRequestService.cancel(1L))
                .isInstanceOf(InvalidRequestStateException.class);
        }

        @Test
        @DisplayName("게시자가 OPEN 요청을 취소하면 status=CLOSED")
        void cancel_ownerAndOpen_setsClosed() {
            // given
            loginAsUser(1L);
            HelpRequest hr = helpRequest(1L, HelpRequestStatus.OPEN);
            when(helpRequestRepository.findById(1L)).thenReturn(Optional.of(hr));

            // when
            helpRequestService.cancel(1L);

            // then
            assertThat(hr.getStatus()).isEqualTo(HelpRequestStatus.CLOSED);
        }
    }

    @Nested
    @DisplayName("삭제(delete)")
    class Delete {
        @Test
        @DisplayName("게시자가 아니면 NotRequestOwnerException")
        void delete_notOwner_throws() {
            // given
            loginAsUser(2L);
            when(helpRequestRepository.findById(any()))
                .thenReturn(Optional.of(helpRequest(1L, HelpRequestStatus.CLOSED)));

            // when / then
            assertThatThrownBy(() -> helpRequestService.delete(20L))
                .isInstanceOf(NotRequestOwnerException.class);
            verify(helpRequestRepository, never()).delete(any());
        }

        @Test
        @DisplayName("CLOSED가 아니면 InvalidRequestStateException — 삭제하지 않는다")
        void delete_notClosed_throws() {
            // given
            loginAsUser(1L);
            when(helpRequestRepository.findById(any()))
                .thenReturn(Optional.of(helpRequest(1L, HelpRequestStatus.OPEN)));

            // when / then
            assertThatThrownBy(() -> helpRequestService.delete(20L))
                .isInstanceOf(InvalidRequestStateException.class);
            verify(helpRequestRepository, never()).delete(any());
        }

        @Test
        @DisplayName("게시자가 CLOSED 요청을 삭제하면 repository.delete 호출")
        void delete_ownerAndClosed_deletes() {
            // given
            loginAsUser(1L);
            HelpRequest hr = helpRequest(1L, HelpRequestStatus.CLOSED);
            when(helpRequestRepository.findById(any())).thenReturn(Optional.of(hr));

            // when
            helpRequestService.delete(1L);

            // then
            verify(helpRequestRepository).delete(hr);
        }
    }

    @Nested
    @DisplayName("재게시(buildRepostForm)")
    class Repost {
        @Test
        @DisplayName("게시자가 아니면 NotRequestOwnerException")
        void repost_notOwner_throws() {
            // given — 로그인=2번, 요청 소유자=1번
            loginAsUser(2L);
            when(helpRequestRepository.findById(any()))
                .thenReturn(Optional.of(closedRequest(1L)));

            // when / then
            assertThatThrownBy(() -> helpRequestService.buildRepostForm(50L))
                .isInstanceOf(NotRequestOwnerException.class);
        }

        @Test
        @DisplayName("CLOSED가 아니면 InvalidRequestStateException")
        void repost_notClosed_throws() {
            // given
            loginAsUser(1L);
            when(helpRequestRepository.findById(any()))
                .thenReturn(Optional.of(helpRequest(1L, HelpRequestStatus.OPEN)));

            // when / then
            assertThatThrownBy(() -> helpRequestService.buildRepostForm(1L))
                .isInstanceOf(InvalidRequestStateException.class);
        }

        @Test
        @DisplayName("CLOSED 본인 요청이면 내용은 프리필하되 지난 일시는 비운다")
        void repost_ownerAndClosed_prefillsContentAndClearsPastDates() {
            // given — 희망일시(2026-06-10)는 현재 기준 과거 → @FutureOrPresent 위반이라 비워야 한다.
            loginAsUser(1L);
            when(helpRequestRepository.findById(1L)).thenReturn(Optional.of(closedRequest(1L)));
            when(helpRequestPersonalityTagRepository.findByHelpRequest_Id(1L))
                .thenReturn(java.util.List.of());

            // when
            HelpRequestForm form = helpRequestService.buildRepostForm(1L);

            // then — 내용은 복사
            assertThat(form.getTitle()).isEqualTo("병원 동행");
            assertThat(form.getBody()).isEqualTo("외래진료 동행");
            assertThat(form.getServiceCategoryId()).isEqualTo(7L);
            assertThat(form.getSido()).isEqualTo("서울특별시");
            assertThat(form.getSigungu()).isEqualTo("강남구");
            // 일시는 과거라 비움
            assertThat(form.getDesiredStartDatetime()).isNull();
            assertThat(form.getDesiredEndDatetime()).isNull();
        }
    }

    // ── 픽스처 헬퍼 ────────────────────────────────────────────────

    // 로그인 사용자(USER 역할) 모킹. requireRole(USER) 통과 + getId() 소유자 비교에 사용.
    private void loginAsUser(Long userId) {
        when(currentUserService.getCurrentUser())
            .thenReturn(User.builder().id(userId).role(UserRole.USER).build());
    }

    // 영속이 아닌 순수 객체 — @MapsId 부작용이 없어 userId를 직접 지정할 수 있다(소유자 비교용).
    private UserProfile owner(Long userId) {
        return UserProfile.builder().userId(userId).fullName("요청자").build();
    }

    // id·requester(userId)·status를 지정한 HelpRequest. cancel/delete의 권한·상태 분기 검증용.
    private HelpRequest helpRequest(Long ownerId, HelpRequestStatus status) {
        return HelpRequest.builder()
            .id(1L)
            .requester(owner(ownerId))
            .status(status)
            .title("도움 요청")
            .desiredStartDatetime(LocalDateTime.of(2026, 6, 10, 10, 0))
            .build();
    }

    // 재게시 원본 — CLOSED·serviceCategory·내용·과거 일시를 갖춘 본인 요청. buildRepostForm 검증용.
    private HelpRequest closedRequest(Long ownerId) {
        return HelpRequest.builder()
            .id(1L)
            .requester(owner(ownerId))
            .status(HelpRequestStatus.CLOSED)
            .serviceCategory(ServiceCategory.builder().id(7L).code("MOVE").name("이동 보조").build())
            .title("병원 동행")
            .body("외래진료 동행")
            .desiredStartDatetime(LocalDateTime.of(2026, 6, 10, 14, 0))
            .desiredEndDatetime(LocalDateTime.of(2026, 6, 10, 16, 0))
            .roadAddress("서울특별시 강남구 테헤란로 152")
            .sido("서울특별시")
            .sigungu("강남구")
            .specialNotes("주의사항")
            .build();
    }

    // 유효한 작성 폼(검증 통과값). create 테스트에서 사용. main 폼은 @Setter 기반.
    private HelpRequestForm createForm() {
        HelpRequestForm form = new HelpRequestForm();
        form.setTitle("병원 동행이 필요해요");
        form.setServiceCategoryId(1L);
        form.setBody("외래진료 동행 부탁드립니다.");
        form.setDesiredStartDatetime(LocalDateTime.of(2026, 6, 10, 14, 0));
        form.setDesiredEndDatetime(LocalDateTime.of(2026, 6, 10, 16, 0));
        form.setRoadAddress("서울특별시 강남구 테헤란로 152");
        form.setSido("서울특별시");
        form.setSigungu("강남구");
        return form;
    }

    // create 정상 케이스에서 serviceCategoryRepository.findById가 반환할 분류.
    private ServiceCategory category() {
        return ServiceCategory.builder().code("MOVE").name("이동 보조").build();
    }
}
