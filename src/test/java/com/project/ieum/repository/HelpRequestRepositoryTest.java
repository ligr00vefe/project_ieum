package com.project.ieum.repository;

import com.project.ieum.config.JpaAuditingConfig;
import com.project.ieum.config.QuerydslConfig;
import com.project.ieum.entity.User;
import com.project.ieum.entity.UserRole;
import com.project.ieum.entity.UserStatus;
import com.project.ieum.entity.request.HelpRequest;
import com.project.ieum.entity.request.HelpRequestStatus;
import com.project.ieum.entity.request.ServiceCategory;
import com.project.ieum.entity.user.UserProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * (#9) {@link HelpRequestRepository#existsOverlapping} 슬라이스 검증.
 *
 * <p>배경: DB unique(requester, desired_start_datetime)를 제거(#9)하고 "시간대 구간 겹침"을
 * 서비스 계층 검사로 대체했다. 그 핵심 조회가 JPQL(text block + coalesce)이라 javac가 검증하지
 * 못한다 → 잘못되면 Spring 컨텍스트 부팅 시점에야 드러나는 startup 리스크. 본 테스트는 H2
 * 슬라이스로 쿼리의 문법·겹침 의미(경계 포함)를 회귀 가드한다.
 *
 * <p>겹침 정의: {@code 기존.start < 후보.end  AND  후보.start < coalesce(기존.end, 기존.start)}.
 * 끝시각이 없는(시점) 요청은 시작시각을 끝으로 간주한다.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import({JpaAuditingConfig.class, QuerydslConfig.class})
class HelpRequestRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private HelpRequestRepository helpRequestRepository;

    private static final List<HelpRequestStatus> ACTIVE =
            List.of(HelpRequestStatus.OPEN, HelpRequestStatus.MATCHED, HelpRequestStatus.IN_PROGRESS);

    private UserProfile requester;
    private ServiceCategory category;

    @BeforeEach
    void setUp() {
        requester = persistUserProfile("requester@ieum.test");
        category = persistCategory("MOVE", "이동 보조");
    }

    @Test
    @DisplayName("구간이 겹치면 true (기존 10-12, 후보 11-13)")
    void overlapping_returnsTrue() {
        persistRequest(requester, at(10), at(12), HelpRequestStatus.OPEN);

        boolean result = helpRequestRepository.existsOverlapping(requester, at(11), at(13), ACTIVE);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("후보가 기존 구간에 완전히 포함되면 true (기존 10-12, 후보 10:30-11)")
    void contained_returnsTrue() {
        persistRequest(requester, at(10), at(12), HelpRequestStatus.OPEN);

        boolean result = helpRequestRepository.existsOverlapping(
                requester, atMin(10, 30), at(11), ACTIVE);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("경계가 맞닿기만 하면 겹침 아님 (기존 10-12, 후보 12-13)")
    void adjacent_returnsFalse() {
        persistRequest(requester, at(10), at(12), HelpRequestStatus.OPEN);

        boolean result = helpRequestRepository.existsOverlapping(requester, at(12), at(13), ACTIVE);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("끝시각 없는(시점) 기존 요청은 시작시각을 끝으로 간주 — 겹치면 true")
    void nullEnd_existing_overlaps() {
        persistRequest(requester, at(10), null, HelpRequestStatus.OPEN);

        // 후보 09-11 은 시점 10:00 을 포함 → 겹침
        assertThat(helpRequestRepository.existsOverlapping(requester, at(9), at(11), ACTIVE)).isTrue();
        // 후보 10-11 은 시작이 시점과 같음(경계) → 겹침 아님
        assertThat(helpRequestRepository.existsOverlapping(requester, at(10), at(11), ACTIVE)).isFalse();
    }

    @Test
    @DisplayName("다른 요청자의 겹치는 요청은 충돌 대상 아님")
    void differentRequester_returnsFalse() {
        UserProfile other = persistUserProfile("other@ieum.test");
        persistRequest(other, at(10), at(12), HelpRequestStatus.OPEN);

        boolean result = helpRequestRepository.existsOverlapping(requester, at(10), at(12), ACTIVE);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("비활성 상태(CLOSED) 요청은 충돌 대상에서 제외")
    void inactiveStatus_excluded() {
        persistRequest(requester, at(10), at(12), HelpRequestStatus.CLOSED);

        boolean result = helpRequestRepository.existsOverlapping(requester, at(11), at(13), ACTIVE);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("거리순 정렬 — 기준 좌표에 가까운 OPEN 요청부터, 좌표 없는 요청은 뒤로")
    void orderByDistance_nearestFirst_nullsLast() {
        // 기준점: 서울시청(37.5665, 126.9780)
        HelpRequest near = persistRequestAt(at(10), HelpRequestStatus.OPEN, "37.5759", "126.9769"); // 광화문 ~1km
        HelpRequest mid = persistRequestAt(at(11), HelpRequestStatus.OPEN, "37.4979", "127.0276");  // 강남 ~11km
        HelpRequest far = persistRequestAt(at(12), HelpRequestStatus.OPEN, "35.1796", "129.0756");  // 부산 ~325km
        HelpRequest noCoord = persistRequestAt(at(9), HelpRequestStatus.OPEN, null, null);          // 좌표 없음 → 뒤로

        var page = helpRequestRepository.findByStatusOrderByDistance(
                HelpRequestStatus.OPEN, 37.5665, 126.9780, PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting(HelpRequest::getId)
                .containsExactly(near.getId(), mid.getId(), far.getId(), noCoord.getId());
    }

    @Test
    @DisplayName("거리순 정렬 — OPEN이 아닌 요청은 제외")
    void orderByDistance_onlyOpenStatus() {
        persistRequestAt(at(10), HelpRequestStatus.MATCHED, "37.5759", "126.9769");
        HelpRequest open = persistRequestAt(at(11), HelpRequestStatus.OPEN, "35.1796", "129.0756");

        var page = helpRequestRepository.findByStatusOrderByDistance(
                HelpRequestStatus.OPEN, 37.5665, 126.9780, PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting(HelpRequest::getId).containsExactly(open.getId());
    }

    // ── 픽스처 헬퍼 ────────────────────────────────────────────────

    private UserProfile persistUserProfile(String email) {
        User user = em.persist(User.builder()
                .email(email)
                .passwordHash("hash")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build());
        // @MapsId — userId는 user.id에서 파생되므로 명시 금지
        return em.persist(UserProfile.builder()
                .user(user)
                .fullName("테스트요청자")
                .build());
    }

    private ServiceCategory persistCategory(String code, String name) {
        return em.persist(ServiceCategory.builder().code(code).name(name).build());
    }

    private HelpRequest persistRequest(UserProfile who, LocalDateTime start, LocalDateTime end,
                                       HelpRequestStatus status) {
        // 위치 스냅샷: road_address/sido/sigungu는 NOT NULL. 좌표(BigDecimal)도 채워
        // #10 스냅샷 컬럼의 매핑·persist 라운드트립을 함께 가드한다.
        return em.persist(HelpRequest.builder()
                .requester(who)
                .serviceCategory(category)
                .title("도움 요청")
                .desiredStartDatetime(start)
                .desiredEndDatetime(end)
                .roadAddress("서울특별시 강남구 테헤란로 152")
                .sido("서울특별시")
                .sigungu("강남구")
                .latitude(new BigDecimal("37.500123"))
                .longitude(new BigDecimal("127.036456"))
                .status(status)
                .build());
    }

    // 좌표를 지정(또는 null)해 저장 — 거리순 정렬 검증용. 좌표 외 NOT NULL 컬럼은 기본값으로 채운다.
    private HelpRequest persistRequestAt(LocalDateTime start, HelpRequestStatus status, String lat, String lng) {
        return em.persist(HelpRequest.builder()
                .requester(requester)
                .serviceCategory(category)
                .title("도움 요청")
                .desiredStartDatetime(start)
                .roadAddress("서울특별시 강남구 테헤란로 152")
                .sido("서울특별시")
                .sigungu("강남구")
                .latitude(lat != null ? new BigDecimal(lat) : null)
                .longitude(lng != null ? new BigDecimal(lng) : null)
                .status(status)
                .build());
    }

    private static LocalDateTime at(int hour) {
        return LocalDateTime.of(2026, 6, 10, hour, 0);
    }

    private static LocalDateTime atMin(int hour, int minute) {
        return LocalDateTime.of(2026, 6, 10, hour, minute);
    }
}
