package com.project.ieum.dto.request;

import com.project.ieum.entity.request.ReviewVisibility;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewForm {

    @NotNull(message = "평점을 선택해주세요.")
    @Min(value = 1, message = "평점은 1점 이상이어야 합니다.")
    @Max(value = 5, message = "평점은 5점 이하이어야 합니다.")
    private Short rating;

    @Size(max = 2000, message = "후기는 2000자 이하로 입력해주세요.")
    private String body;

    @NotNull(message = "공개 여부를 선택해주세요.")
    private ReviewVisibility visibility = ReviewVisibility.PUBLIC;
}
