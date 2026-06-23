package com.project.ieum.repository.search;

import com.project.ieum.dto.market.MarketPostSearchCondition;
import com.project.ieum.entity.market.MarketPost;
import com.project.ieum.entity.market.MarketPostStatus;
import com.project.ieum.entity.market.QMarketPost;          // QueryDSL 빌드 후 생성됨
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.util.StringUtils;

import java.util.List;

@RequiredArgsConstructor
public class MarketPostSearchRepositoryImpl implements MarketPostSearchRepository {

    private final JPAQueryFactory queryFactory;
    // JPAQueryFactory는 QuerydslConfig.java에 이미 @Bean으로 등록되어 있어서 주입만 하면 됩니다.

    @Override
    public Page<MarketPost> searchMarketPosts(MarketPostSearchCondition condition, Pageable pageable) {
        // 좌표 없는 버전 → 오버로드된 메서드에 null 넘김
        return searchMarketPosts(condition, pageable, null, null);
    }

    @Override
    public Page<MarketPost> searchMarketPosts(MarketPostSearchCondition condition, Pageable pageable,
                                              Double lat, Double lng) {
        // Q클래스: QueryDSL이 MarketPost 엔티티를 분석해 자동 생성
        // 빌드 전엔 빨간 줄 표시 → ./gradlew compileJava 실행 후 해결됨
        QMarketPost post = QMarketPost.marketPost;

        // BooleanBuilder: AND 조건을 동적으로 쌓는 QueryDSL 객체
        // 조건이 null이면 자동으로 해당 and()를 무시
        BooleanBuilder where = new BooleanBuilder();

        // REMOVED 상태는 목록에서 항상 제외
        where.and(post.status.ne(MarketPostStatus.REMOVED));

        // 상태 필터 (ACTIVE / RESERVED / SOLD)
        // condition.status가 null이면 이 조건 추가 안 됨 → 전체 상태 조회
        if (condition.getStatus() != null) {
            where.and(post.status.eq(condition.getStatus()));
        }

        // 카테고리 필터
        if (condition.getCategoryId() != null) {
            where.and(post.category.id.eq(condition.getCategoryId()));
        }

        // 키워드 — 제목 또는 본문에 포함 (대소문자 무시)
        // StringUtils.hasText: null, 빈 문자열, 공백만 있는 경우 모두 false 처리
        if (StringUtils.hasText(condition.getKeyword())) {
            String kw = condition.getKeyword().trim();
            where.and(post.title.containsIgnoreCase(kw)
                    .or(post.description.containsIgnoreCase(kw)));
        }

        // 지역 필터 — 시/도 정확 일치
        if (StringUtils.hasText(condition.getSido())) {
            where.and(post.sido.eq(condition.getSido().trim()));
        }

        // 지역 필터 — 시/군/구 정확 일치
        if (StringUtils.hasText(condition.getSigungu())) {
            where.and(post.sigungu.eq(condition.getSigungu().trim()));
        }

        // 나눔/판매 필터
        if (condition.getSharing() != null) {
            where.and(post.sharing.eq(condition.getSharing()));
        }

        // 가격 범위 필터
        if (condition.getMinPrice() != null) {
            where.and(post.price.goe(condition.getMinPrice())); // goe = greater or equal (이상)
        }
        if (condition.getMaxPrice() != null) {
            where.and(post.price.loe(condition.getMaxPrice())); // loe = less or equal (이하)
        }

        // 기본 쿼리 — seller와 category를 fetchJoin으로 함께 로딩 (N+1 방지)
        // fetchJoin: 연관 엔티티를 별도 쿼리 없이 한 번에 가져옴
        JPAQuery<MarketPost> query = queryFactory
                .selectFrom(post)
                .leftJoin(post.seller).fetchJoin()
                .leftJoin(post.category).fetchJoin()
                .where(where);

        // ── 정렬 조건 ──
        if (lat != null && lng != null) {
            // 거리 기반 정렬 — HelpRequestSearchRepositoryImpl과 완전히 동일한 공식
            // nullCoordLast: 좌표(위도/경도)가 null인 게시글을 맨 뒤로 보내는 CASE 표현식
            NumberExpression<Integer> nullCoordLast = Expressions.numberTemplate(
                    Integer.class,
                    "case when {0} is null or {1} is null then 1 else 0 end",
                    post.latitude, post.longitude);

            // cosineSimilarity: 두 좌표 간 구면 코사인 유사도
            // 값이 클수록 가까운 거리 → desc() 정렬로 가까운 순
            // 0.017453292519943295 = π/180 (도 → 라디안 변환 상수)
            NumberExpression<Double> cosineSimilarity = Expressions.numberTemplate(
                    Double.class,
                    "( sin({0} * 0.017453292519943295) * sin({1} * 0.017453292519943295) "
                            + "+ cos({0} * 0.017453292519943295) * cos({1} * 0.017453292519943295) "
                            + "* cos({2} * 0.017453292519943295 - {3} * 0.017453292519943295) )",
                    lat, post.latitude, post.longitude, lng);

            query.orderBy(
                    nullCoordLast.asc(),        // 좌표 없는 게시글 → 뒤로
                    cosineSimilarity.desc(),    // 가까운 순
                    post.createdAt.desc(),      // 동거리면 최신순 (마켓은 시작시각 없으니 createdAt)
                    post.id.desc()              // 완전 동률 → id 역순으로 결정적 정렬
            );
        } else {
            // 좌표 없으면 단순 최신순
            query.orderBy(post.createdAt.desc(), post.id.desc());
        }

        // 페이지네이션 적용
        List<MarketPost> content = query
                .offset(pageable.getOffset())   // 몇 번째부터 (페이지 * 사이즈)
                .limit(pageable.getPageSize())  // 한 페이지에 몇 개
                .fetch();

        // 전체 건수 쿼리 (페이지네이션 UI용) — content 쿼리와 where 조건 동일
        Long total = queryFactory
                .select(post.count())
                .from(post)
                .where(where)
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }
}