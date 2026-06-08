package com.project.ieum.dto;

import com.project.ieum.entity.Gender;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CaregiverEditDTO {

    private String name;

    private String phone;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthDate;

    private Gender gender;

    private String introShort;

    private String introLong;

    private Boolean hasCertification;

    private String certificationType;

    private String experience;

    private List<String> serviceCategories;

    // 가능 시간대 칩(프론트 수정폼 바인딩). 정규화(#15)로 caregiver_availability로 이전 중 —
    // 칩↔행 조합 규칙 미확정이라 현재 영속/표시는 보류(프론트 핸드오프). 필드는 폼 바인딩 유지 목적.
    private List<String> availableTimeSlots;

    // 활동 지역(프론트 수정폼 바인딩). CaregiverServiceRegion 제거(#11, 전역 매칭)로 더 이상 영속되지 않음.
    // TODO(caregiver-region-handoff): 프론트 지역 섹션 제거 시 이 필드 정리.
    private List<Long> regionIds;

    private List<Long> personalityTagIds;
}
