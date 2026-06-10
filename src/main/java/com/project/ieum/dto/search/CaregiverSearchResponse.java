package com.project.ieum.dto.search;

import com.project.ieum.entity.caregiver.CaregiverAvailabilityStatus;
import com.project.ieum.entity.caregiver.CaregiverProfile;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class CaregiverSearchResponse {
    private Long caregiverId;
    private String fullName;
    private String introShort;
    private Boolean hasCertification;
    private BigDecimal avgRating;
    private Integer totalReviews;
    private CaregiverAvailabilityStatus availabilityStatus;

    public static CaregiverSearchResponse from(CaregiverProfile profile) {
        return CaregiverSearchResponse.builder()
                .caregiverId(profile.getUserId())
                .fullName(profile.getFullName())
                .introShort(profile.getIntroShort())
                .hasCertification(profile.getHasCertification())
                .avgRating(profile.getAvgRating())
                .totalReviews(profile.getTotalReviews())
                .availabilityStatus(profile.getAvailabilityStatus())
                .build();
    }
}
