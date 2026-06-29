package com.project.ieum.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CalendarEventDto {
    private Long requestId;
    private String title;
    private String startDate;  // "yyyy-MM-dd"
    private String endDate;    // "yyyy-MM-dd"
    private String date;       // legacy alias = startDate
    private String type;       // "registered" | "matched" | "applied" | "completed"
}
