package com.project.ieum.dto.search;

import com.project.ieum.entity.request.HelpRequestStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class HelpRequestSearchCondition {
    private Long regionId;
    private Long serviceCategoryId;
    private LocalDate fromDate;
    private LocalDate toDate;
    private HelpRequestStatus status;
}
