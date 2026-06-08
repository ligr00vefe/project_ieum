package com.project.ieum.service;

import com.project.ieum.dto.ActivityInfoDTO.AvailabilityTimeDTO;
import com.project.ieum.entity.caregiver.CaregiverAvailability;
import com.project.ieum.entity.caregiver.CaregiverProfile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 가입·수정 폼 입력을 정규화 테이블 행(CaregiverAvailability)으로 변환한다.
 *
 * 기존 모델은 가능 시간대를 caregiver_profiles.available_time_slots(쉼표 구분 문자열)로 저장했으나,
 * 엔티티 정규화(#15)로 해당 컬럼을 제거하고 caregiver_availability(요일·시작·종료)로 일원화한다.
 *
 * 입력 경로는 둘이다. 우선순위가 다르므로 메서드를 분리한다.
 *
 * 1) 구조화 입력(권장) — {@link #toRows(List, CaregiverProfile)}
 *    AvailabilityTimeDTO(dayOfWeek/startTime/endTime)는 CaregiverAvailability와 1:1로 대응하므로
 *    조합 규칙 없이 결정적으로 변환된다. 프론트가 요일·시각을 직접 보내면 이 경로로 안전하게 저장된다.
 *
 * 2) 시간대 칩(레거시) — {@link #fromTimeSlotChips(List, CaregiverProfile)}
 *    "오전 (09-12)" 같은 UI 표시 문자열 칩은 시각 구간으로는 매핑되나, 요일과의 조합 의미가 미확정이다.
 *    예) "오전"+"주말" = 토·일 09:00~12:00 인지, "오전" 단독 = 전 요일 09:00~12:00 인지.
 *    또한 표시 문자열은 i18n·UI 변경에 취약한 결합점이라 파싱 기반은 견고하지 않다.
 *    조합 규칙이 확정되기 전까지 이 경로는 스텁이며, 우회책은 구조화 입력(1)을 사용하는 것이다.
 */
@Slf4j
@Component
public class CaregiverAvailabilityMapper {

    /**
     * 구조화 가용시간(요일·시작·종료)을 availability 행으로 변환한다(권장 경로).
     *
     * @param availabilityTimes 폼에서 전송된 구조화 가용시간(dayOfWeek/startTime/endTime)
     * @param caregiver         소유 프로필
     * @return 변환된 행. 입력이 비었으면 빈 목록. 필드가 누락된 항목은 건너뛰고 경고를 남긴다.
     */
    public List<CaregiverAvailability> toRows(List<AvailabilityTimeDTO> availabilityTimes, CaregiverProfile caregiver) {
        if (availabilityTimes == null || availabilityTimes.isEmpty()) {
            return Collections.emptyList();
        }
        List<CaregiverAvailability> rows = new ArrayList<>(availabilityTimes.size());
        for (AvailabilityTimeDTO time : availabilityTimes) {
            if (time == null
                    || time.getDayOfWeek() == null
                    || time.getStartTime() == null
                    || time.getEndTime() == null) {
                // 무음 손실 방지: 불완전한 항목은 버리되 가시화한다.
                log.warn("[availability-mapper] 불완전한 가용시간 항목 건너뜀: {}", time);
                continue;
            }
            rows.add(CaregiverAvailability.builder()
                    .caregiver(caregiver)
                    .dayOfWeek(time.getDayOfWeek())
                    .startTime(time.getStartTime())
                    .endTime(time.getEndTime())
                    .build());
        }
        return rows;
    }

    /**
     * 시간대 칩(레거시 표시 문자열)을 availability 행으로 변환한다.
     *
     * 시간대×요일 조합 규칙이 확정되기 전까지는 스텁이다. 구조화 입력({@link #toRows})을 우회책으로 사용하라.
     *
     * @param timeSlotChips 폼에서 전송된 가능 시간대 칩(예: "오전 (09-12)", "주말", "야간")
     * @param caregiver     소유 프로필
     * @return 항상 빈 목록(조합 규칙 미확정). 입력이 있었으면 경고로 가시화한다.
     */
    public List<CaregiverAvailability> fromTimeSlotChips(List<String> timeSlotChips, CaregiverProfile caregiver) {
        if (timeSlotChips == null || timeSlotChips.isEmpty()) {
            return Collections.emptyList();
        }
        // TODO(availability-chip-mapping): 시간대 칩 x 요일 칩 조합 규칙 확정 후 구현. 그 전까지는 구조화 입력(toRows)을 사용.
        log.warn("[availability-mapper] 칩->availability 변환 미구현 — 입력 칩 {}건 저장 보류(구조화 입력 권장): {}",
                timeSlotChips.size(), timeSlotChips);
        return Collections.emptyList();
    }

    /**
     * 정규화 행(CaregiverAvailability)을 마이페이지 수정 폼이 바인딩하는 시간대 칩 문자열로 역변환한다.
     *
     * 행→칩 역매핑 역시 시각 구간→표시 라벨 규칙이 미확정이라 스텁이다. 확정 전까지는 빈 목록을 반환하여
     * 수정 폼의 시간대 칩이 비워진 상태로 표시되며(무손실 — 저장 데이터는 caregiver_availability에 보존),
     * 칩 편집 영속화는 {@link #fromTimeSlotChips}와 함께 조합 규칙 확정 후 활성화한다.
     *
     * @param rows 소유자의 caregiver_availability 행
     * @return 항상 빈 목록(역매핑 규칙 미확정)
     */
    public List<String> toTimeSlotChips(List<CaregiverAvailability> rows) {
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        // TODO(availability-chip-mapping): 행->칩 역매핑(시각구간→표시 라벨) 규칙 확정 후 구현.
        log.warn("[availability-mapper] availability->칩 역변환 미구현 — {}건은 수정 폼에 칩으로 표시되지 않음(데이터는 보존)", rows.size());
        return Collections.emptyList();
    }
}
