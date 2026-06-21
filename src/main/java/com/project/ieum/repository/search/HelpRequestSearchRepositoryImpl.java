package com.project.ieum.repository.search;

import com.project.ieum.dto.search.HelpRequestSearchCondition;
import com.project.ieum.entity.request.HelpRequest;
import com.project.ieum.entity.request.HelpRequestStatus;
import com.project.ieum.entity.request.QHelpRequest;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;

@RequiredArgsConstructor
public class HelpRequestSearchRepositoryImpl implements HelpRequestSearchRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<HelpRequest> searchHelpRequests(HelpRequestSearchCondition condition, Pageable pageable) {
        return searchHelpRequests(condition, pageable, null, null);
    }

    @Override
    public Page<HelpRequest> searchHelpRequests(HelpRequestSearchCondition condition, Pageable pageable,
                                                Double lat, Double lng) {
        QHelpRequest request = QHelpRequest.helpRequest;
        BooleanBuilder where = new BooleanBuilder();

        // regionId 필터 제거 — 서비스권역 정규화(#11)로 region FK 삭제됨
        // 서비스 카테고리 — 다중 선택(IN) 우선, 없으면 단일 serviceCategoryId(/api/search 호환) 폴백.
        if (!CollectionUtils.isEmpty(condition.getServiceCategoryIds())) {
            where.and(request.serviceCategory.id.in(condition.getServiceCategoryIds()));
        } else if (condition.getServiceCategoryId() != null) {
            where.and(request.serviceCategory.id.eq(condition.getServiceCategoryId()));
        }
        if (condition.getFromDate() != null) {
            where.and(request.desiredStartDatetime.goe(condition.getFromDate().atStartOfDay()));
        }
        if (condition.getToDate() != null) {
            where.and(request.desiredStartDatetime.loe(condition.getToDate().atTime(23, 59, 59)));
        }
        // 보드 모드(ownerScope): 남의 OPEN + 내 모든 상태. 그 외엔 기존 단일 status 필터.
        if (condition.getOwnerScopeUserId() != null) {
            where.and(request.status.eq(HelpRequestStatus.OPEN)
                    .or(request.requester.userId.eq(condition.getOwnerScopeUserId())));
        } else if (condition.getStatus() != null) {
            where.and(request.status.eq(condition.getStatus()));
        }
        // 키워드 — 제목/본문 부분일치(대소문자 무시).
        if (StringUtils.hasText(condition.getKeyword())) {
            String kw = condition.getKeyword().trim();
            where.and(request.title.containsIgnoreCase(kw).or(request.body.containsIgnoreCase(kw)));
        }
        // 지역 — 위치 스냅샷(sido/sigungu) 정확일치.
        if (StringUtils.hasText(condition.getSido())) {
            where.and(request.sido.eq(condition.getSido().trim()));
        }
        if (StringUtils.hasText(condition.getSigungu())) {
            where.and(request.sigungu.eq(condition.getSigungu().trim()));
        }

        JPAQuery<HelpRequest> query = queryFactory
                .selectFrom(request)
                .leftJoin(request.requester).fetchJoin()
                .leftJoin(request.serviceCategory).fetchJoin()
                .where(where);

        if (lat != null && lng != null) {
            // #66 거리정렬과 동일 — acos 없이 "코사인 유사도(구면 내적) 내림차순 = 거리 오름차순".
            //   · sin/cos만 사용(HQL 표준), 도→라디안은 리터럴 상수(π/180)를 곱해 dialect 함수 의존 제거.
            //   · 좌표 없는(null) 요청은 항상 뒤로(CASE), 동률은 시작시각 오름차순 폴백.
            NumberExpression<Integer> nullCoordLast = Expressions.numberTemplate(Integer.class,
                    "case when {0} is null or {1} is null then 1 else 0 end",
                    request.latitude, request.longitude);
            NumberExpression<Double> cosineSimilarity = Expressions.numberTemplate(Double.class,
                    "( sin({0} * 0.017453292519943295) * sin({1} * 0.017453292519943295) "
                  + "+ cos({0} * 0.017453292519943295) * cos({1} * 0.017453292519943295) "
                  + "* cos({2} * 0.017453292519943295 - {3} * 0.017453292519943295) )",
                    lat, request.latitude, request.longitude, lng);
            // id.desc() — 좌표·시작시각이 모두 동률일 때 결정적 순서를 보장(페이지 경계 중복/누락 방지).
            query.orderBy(nullCoordLast.asc(), cosineSimilarity.desc(),
                    request.desiredStartDatetime.asc(), request.id.desc());
        } else if (condition.getOwnerScopeUserId() != null) {
            // 둘러보기 보드 — 최신순(생성시각 내림차순), 동률은 id로 결정성 보장.
            query.orderBy(request.createdAt.desc(), request.id.desc());
        } else {
            query.orderBy(request.desiredStartDatetime.asc(), request.id.desc());
        }

        List<HelpRequest> content = query
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(request.count())
                .from(request)
                .where(where)
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }
}
