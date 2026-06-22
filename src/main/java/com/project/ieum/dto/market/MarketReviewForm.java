package com.project.ieum.dto.market;

import com.project.ieum.entity.request.ReviewVisibility;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MarketReviewForm {

    @NotNull(message = "별점을 선택해주세요.")
    @Min(value = 1, message = "별점은 1 이상이어야 합니다.")
    @Max(value = 5, message = "별점은 5 이하이어야 합니다.")
    private Short rating;             // 별점 1~5

    private String body;              // 후기 본문 (선택)

    @NotNull(message = "공개 여부를 선택해주세요.")
    private ReviewVisibility visibility; // PUBLIC / PRIVATE
}