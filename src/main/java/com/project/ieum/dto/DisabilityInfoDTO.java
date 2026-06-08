package com.project.ieum.dto;

import lombok.*;

import java.util.List;

@Data
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DisabilityInfoDTO {

    private List<Long> disabilityTypeIds;

    private String activityRange;

    private String avoidSituations;
}
