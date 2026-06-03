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

    // 폼에서 체크박스로 전송되는 가능 업무
    private List<String> serviceCategories;

    // 폼에서 체크박스로 전송되는 가능 시간대 (예: "오전 (09-12)", "오후 (12-18)")
    private List<String> availableTimeSlots;

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
