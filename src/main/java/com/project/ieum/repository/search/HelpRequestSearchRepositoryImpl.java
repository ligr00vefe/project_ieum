package com.project.ieum.repository.search;

import com.project.ieum.dto.search.HelpRequestSearchCondition;
import com.project.ieum.entity.request.HelpRequest;
import com.project.ieum.entity.request.QHelpRequest;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

@RequiredArgsConstructor
public class HelpRequestSearchRepositoryImpl implements HelpRequestSearchRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<HelpRequest> searchHelpRequests(HelpRequestSearchCondition condition, Pageable pageable) {
        QHelpRequest request = QHelpRequest.helpRequest;
        BooleanBuilder where = new BooleanBuilder();

        // regionId 필터 제거 — 서비스권역 정규화(#11)로 region FK 삭제됨
        if (condition.getServiceCategoryId() != null) {
            where.and(request.serviceCategory.id.eq(condition.getServiceCategoryId()));
        }
        if (condition.getFromDate() != null) {
            where.and(request.desiredStartDatetime.goe(condition.getFromDate().atStartOfDay()));
        }
        if (condition.getToDate() != null) {
            where.and(request.desiredStartDatetime.loe(condition.getToDate().atTime(23, 59, 59)));
        }
        if (condition.getStatus() != null) {
            where.and(request.status.eq(condition.getStatus()));
        }

        List<HelpRequest> content = queryFactory
                .selectFrom(request)
                .leftJoin(request.requester).fetchJoin()
                .leftJoin(request.serviceCategory).fetchJoin()
                .where(where)
                .orderBy(request.desiredStartDatetime.asc(), request.id.desc())
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
