package com.project.ieum.repository.search;

import com.project.ieum.dto.search.CaregiverSearchCondition;
import com.project.ieum.entity.caregiver.CaregiverAvailabilityStatus;
import com.project.ieum.entity.caregiver.CaregiverProfile;
import com.project.ieum.entity.caregiver.QCaregiverAvailability;
import com.project.ieum.entity.caregiver.QCaregiverPersonalityTag;
import com.project.ieum.entity.caregiver.QCaregiverProfile;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

@RequiredArgsConstructor
public class CaregiverSearchRepositoryImpl implements CaregiverSearchRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<CaregiverProfile> searchCaregivers(CaregiverSearchCondition condition, Pageable pageable) {
        QCaregiverProfile caregiver = QCaregiverProfile.caregiverProfile;
        QCaregiverAvailability availability = QCaregiverAvailability.caregiverAvailability;
        QCaregiverPersonalityTag personalityTag = QCaregiverPersonalityTag.caregiverPersonalityTag;

        BooleanBuilder where = new BooleanBuilder();
        where.and(caregiver.availabilityStatus.eq(CaregiverAvailabilityStatus.AVAILABLE));

        if (condition.getMinRating() != null) {
            where.and(caregiver.avgRating.goe(condition.getMinRating()));
        }
        if (condition.getHasCertification() != null) {
            where.and(caregiver.hasCertification.eq(condition.getHasCertification()));
        }
        // 서비스권역 정규화(#11)로 regionId 필터 제거 — 전역 검색으로 전환
        if (condition.getDayOfWeek() != null) {
            where.and(availability.dayOfWeek.eq(condition.getDayOfWeek()));
        }
        if (condition.getStartTime() != null && condition.getEndTime() != null) {
            where.and(availability.startTime.loe(condition.getStartTime()))
                    .and(availability.endTime.goe(condition.getEndTime()));
        }
        if (condition.getTagIds() != null && !condition.getTagIds().isEmpty()) {
            where.and(personalityTag.tag.id.in(condition.getTagIds()));
        }

        List<CaregiverProfile> content = queryFactory
                .selectDistinct(caregiver)
                .from(caregiver)
                .leftJoin(availability).on(availability.caregiver.eq(caregiver))
                .leftJoin(personalityTag).on(personalityTag.caregiver.eq(caregiver))
                .where(where)
                .orderBy(caregiver.avgRating.desc(), caregiver.totalReviews.desc(), caregiver.userId.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(caregiver.countDistinct())
                .from(caregiver)
                .leftJoin(availability).on(availability.caregiver.eq(caregiver))
                .leftJoin(personalityTag).on(personalityTag.caregiver.eq(caregiver))
                .where(where)
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }
}
