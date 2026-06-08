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

    // 가능 시간대 칩(레거시). 정규화(#15) 후 칩→caregiver_availability 변환은 조합 규칙 미확정 스텁.
    // 폼 바인딩 유지 목적. 결정적 저장은 아래 availabilityTimes(구조화 입력)를 사용.
    private List<String> availableTimeSlots;

    // 구조화 가용시간(요일·시작·종료). caregiver_availability와 1:1 대응하는 권장 입력 경로.
    private List<AvailabilityTimeDTO> availabilityTimes;

    // 활동 지역(폼 바인딩). CaregiverServiceRegion 제거(#11, 전역 매칭)로 더 이상 영속되지 않음.
    // TODO(caregiver-region-handoff): 프론트 step3 지역 섹션 제거 시 이 필드 정리.
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
