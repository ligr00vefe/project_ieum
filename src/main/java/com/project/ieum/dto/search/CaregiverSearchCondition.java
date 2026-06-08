package com.project.ieum.dto.search;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

@Getter
@Setter
public class CaregiverSearchCondition {
    private Long regionId;
    private Short dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private List<Long> tagIds;
    private BigDecimal minRating;
    private Boolean hasCertification;
}
