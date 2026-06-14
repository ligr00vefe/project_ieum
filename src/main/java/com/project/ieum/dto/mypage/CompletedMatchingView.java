package com.project.ieum.dto.mypage;

import com.project.ieum.entity.request.Review;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class CompletedMatchingView {
    private Long requestId;
    private String serviceCategory;
    private String location;
    private LocalDateTime startDatetime;
    private LocalDateTime endDatetime;
    private String caregiverName;
    private boolean completed;
    private Review review;
}
