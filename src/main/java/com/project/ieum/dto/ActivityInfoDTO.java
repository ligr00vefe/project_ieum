package com.project.ieum.dto;

import lombok.*;

import java.time.LocalTime;
import java.util.List;

@Data
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityInfoDTO {

    private String experience;

    private List<AvailabilityTimeDTO> availabilityTimes;

    private List<Long> regionIds;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AvailabilityTimeDTO {
        private Short dayOfWeek; // 0-6 (일요일-토요일)
        private LocalTime startTime;
        private LocalTime endTime;
    }
}
