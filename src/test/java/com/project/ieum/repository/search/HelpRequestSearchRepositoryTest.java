package com.project.ieum.repository.search;

import com.project.ieum.config.JpaAuditingConfig;
import com.project.ieum.config.QuerydslConfig;
import com.project.ieum.dto.search.HelpRequestSearchCondition;
import com.project.ieum.dto.search.RegionOption;
import com.project.ieum.entity.User;
import com.project.ieum.entity.UserRole;
import com.project.ieum.entity.UserStatus;
import com.project.ieum.entity.request.HelpRequest;
import com.project.ieum.entity.request.HelpRequestStatus;
import com.project.ieum.entity.request.ServiceCategory;
import com.project.ieum.entity.user.UserProfile;
import com.project.ieum.repository.HelpRequestRepository;
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
 * 게시판 동적 검색 QueryDSL 경로({@link HelpRequestSearchRepository#searchHelpRequests}) 슬라이스 검증.
 *
 * <p>배경: 키워드/지역 필터와 거리순 정렬은 QueryDSL {@code Expressions.numberTemplate}(HQL 조각)로
 * 표현돼 javac가 검증하지 못한다 → 잘못되면 쿼리 실행 시점에야 드러난다. H2 슬라이스로 문법과
 * 필터·정렬 의미를 회귀 가드한다. distinct 지역 옵션 조회도 함께 검증한다.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import({JpaAuditingConfig.class, QuerydslConfig.class})
class HelpRequestSearchRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private HelpRequestRepository helpRequestRepository;

    private UserProfile requester;
    private ServiceCategory move;
    private ServiceCategory companion;

    @BeforeEach
    void setUp() {
        requester = persistUserProfile("requester@ieum.test");
        move = persistCategory("MOVE", "이동 보조");
        companion = persistCategory("COMPANION", "외출 동행");
    }

    @Test
    @DisplayName("키워드 — 제목 또는 본문에 부분일치하는 요청만 반환")
    void keyword_matchesTitleOrBody() {
        HelpRequest titleHit = persist(b -> b.title("병원 동행 도와주세요").body("정기 진료"));
        HelpRequest bodyHit = persist(b -> b.title("도움 요청").body("병원까지 함께 가주실 분"));
        persist(b -> b.title("장보기").body("마트 동행"));

        HelpRequestSearchCondition cond = new HelpRequestSearchCondition();
        cond.setKeyword("병원");

        var page = helpRequestRepository.searchHelpRequests(cond, PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting(HelpRequest::getId)
                .containsExactlyInAnyOrder(titleHit.getId(), bodyHit.getId());
    }

    @Test
    @DisplayName("지역 — 시/도와 시군구가 모두 일치하는 요청만 반환")
    void region_filtersBySidoAndSigungu() {
        HelpRequest gangnam = persist(b -> b.sido("서울특별시").sigungu("강남구"));
        persist(b -> b.sido("서울특별시").sigungu("마포구"));
        persist(b -> b.sido("부산광역시").sigungu("해운대구"));

        HelpRequestSearchCondition cond = new HelpRequestSearchCondition();
        cond.setSido("서울특별시");
        cond.setSigungu("강남구");

        var page = helpRequestRepository.searchHelpRequests(cond, PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting(HelpRequest::getId).containsExactly(gangnam.getId());
    }

    @Test
    @DisplayName("서비스 카테고리 다중 — 선택한 카테고리(IN)에 속한 요청만 반환")
    void serviceCategories_inFilter() {
        HelpRequest moveReq = persist(b -> b.serviceCategory(move));
        HelpRequest companionReq = persist(b -> b.serviceCategory(companion));
        ServiceCategory other = persistCategory("CLEAN", "가사 지원");
        persist(b -> b.serviceCategory(other));

        HelpRequestSearchCondition cond = new HelpRequestSearchCondition();
        cond.setServiceCategoryIds(List.of(move.getId(), companion.getId()));

        var page = helpRequestRepository.searchHelpRequests(cond, PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting(HelpRequest::getId)
                .containsExactlyInAnyOrder(moveReq.getId(), companionReq.getId());
    }

    @Test
    @DisplayName("서비스 카테고리 — ids 비었을 때 단일 serviceCategoryId로 폴백(/api/search 호환)")
    void serviceCategory_singleFallback() {
        HelpRequest moveReq = persist(b -> b.serviceCategory(move));
        persist(b -> b.serviceCategory(companion));

        HelpRequestSearchCondition cond = new HelpRequestSearchCondition();
        cond.setServiceCategoryId(move.getId());

        var page = helpRequestRepository.searchHelpRequests(cond, PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting(HelpRequest::getId).containsExactly(moveReq.getId());
    }

    @Test
    @DisplayName("거리순 정렬 — 좌표를 주면 가까운 순, 좌표 없는 요청은 뒤로")
    void distanceOrder_whenCoordsProvided() {
        // 기준점: 서울시청(37.5665, 126.9780)
        HelpRequest near = persist(b -> b.latitude(new BigDecimal("37.5759")).longitude(new BigDecimal("126.9769"))); // 광화문
        HelpRequest far = persist(b -> b.latitude(new BigDecimal("35.1796")).longitude(new BigDecimal("129.0756")));  // 부산
        HelpRequest noCoord = persist(b -> b.latitude(null).longitude(null));

        var page = helpRequestRepository.searchHelpRequests(
                new HelpRequestSearchCondition(), PageRequest.of(0, 10), 37.5665, 126.9780);

        assertThat(page.getContent()).extracting(HelpRequest::getId)
                .containsExactly(near.getId(), far.getId(), noCoord.getId());
    }

    @Test
    @DisplayName("좌표 미지정 시 시작시각 오름차순 정렬")
    void startTimeOrder_whenNoCoords() {
        HelpRequest later = persist(b -> b.desiredStartDatetime(at(14)));
        HelpRequest earlier = persist(b -> b.desiredStartDatetime(at(9)));

        var page = helpRequestRepository.searchHelpRequests(
                new HelpRequestSearchCondition(), PageRequest.of(0, 10), null, null);

        assertThat(page.getContent()).extracting(HelpRequest::getId)
                .containsExactly(earlier.getId(), later.getId());
    }

    @Test
    @DisplayName("distinct 지역 옵션 — OPEN 요청의 (시/도, 시군구) 중복 제거, 비OPEN 제외")
    void distinctRegions_openOnly() {
        persist(b -> b.sido("서울특별시").sigungu("강남구"));
        persist(b -> b.sido("서울특별시").sigungu("강남구")); // 중복
        persist(b -> b.sido("서울특별시").sigungu("마포구"));
        persist(b -> b.sido("부산광역시").sigungu("해운대구").status(HelpRequestStatus.CLOSED)); // 제외

        List<RegionOption> regions = helpRequestRepository.findDistinctRegionsByStatus(HelpRequestStatus.OPEN);

        assertThat(regions).containsExactlyInAnyOrder(
                new RegionOption("서울특별시", "강남구"),
                new RegionOption("서울특별시", "마포구"));
    }

    // ── 픽스처 헬퍼 ────────────────────────────────────────────────

    private UserProfile persistUserProfile(String email) {
        User user = em.persist(User.builder()
                .email(email)
                .passwordHash("hash")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build());
        return em.persist(UserProfile.builder()
                .user(user)
                .fullName("테스트요청자")
                .build());
    }

    private ServiceCategory persistCategory(String code, String name) {
        return em.persist(ServiceCategory.builder().code(code).name(name).build());
    }

    // 기본값으로 채운 OPEN 요청 빌더에 테스트별 커스터마이즈를 적용해 저장.
    private HelpRequest persist(java.util.function.UnaryOperator<HelpRequest.HelpRequestBuilder> customizer) {
        HelpRequest.HelpRequestBuilder builder = HelpRequest.builder()
                .requester(requester)
                .serviceCategory(move)
                .title("도움 요청")
                .body("내용")
                .desiredStartDatetime(at(10))
                .roadAddress("서울특별시 강남구 테헤란로 152")
                .sido("서울특별시")
                .sigungu("강남구")
                .latitude(new BigDecimal("37.500123"))
                .longitude(new BigDecimal("127.036456"))
                .status(HelpRequestStatus.OPEN);
        return em.persist(customizer.apply(builder).build());
    }

    private static LocalDateTime at(int hour) {
        return LocalDateTime.of(2026, 6, 10, hour, 0);
    }
}
