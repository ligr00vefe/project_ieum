package com.project.ieum.dto.recommend;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RecommendationScoreDetail {
    private int regionScore;
    private int timeScore;
    private int personalityScore;
    private int ratingScore;
    private int certificationScore;
}
