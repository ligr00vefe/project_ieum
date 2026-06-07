package com.project.ieum.dto.recommend;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class CaregiverRecommendationResponse {
    private Long caregiverId;
    private String fullName;
    private String introShort;
    private BigDecimal avgRating;
    private Integer totalReviews;
    private Boolean hasCertification;
    private int score;
    private RecommendationScoreDetail detail;
}
