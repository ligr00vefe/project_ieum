package com.project.ieum.repository.search;

import com.project.ieum.dto.search.HelpRequestSearchCondition;
import com.project.ieum.entity.request.QHelpRequest;
import com.project.ieum.entity.request.HelpRequest;
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

        if (condition.getRegionId() != null) {
            where.and(request.region.id.eq(condition.getRegionId()));
        }
        if (condition.getServiceCategoryId() != null) {
            where.and(request.serviceCategory.id.eq(condition.getServiceCategoryId()));
        }
        if (condition.getFromDate() != null) {
            where.and(request.desiredDate.goe(condition.getFromDate()));
        }
        if (condition.getToDate() != null) {
            where.and(request.desiredDate.loe(condition.getToDate()));
        }
        if (condition.getStatus() != null) {
            where.and(request.status.eq(condition.getStatus()));
        }

        List<HelpRequest> content = queryFactory
                .selectFrom(request)
                .leftJoin(request.requester).fetchJoin()
                .leftJoin(request.region).fetchJoin()
                .leftJoin(request.serviceCategory).fetchJoin()
                .where(where)
                .orderBy(request.desiredDate.asc(), request.id.desc())
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
