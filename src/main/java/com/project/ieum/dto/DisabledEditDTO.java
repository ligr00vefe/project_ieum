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
public class DisabledEditDTO {

    private String name;

    private String phone;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthDate;

    private Gender gender;

    private String guardianName;

    private String guardianPhone;

    private List<Long> disabilityTypeIds;

    private String activityRange;

    private String avoidSituations;

    private List<Long> communicationMethodIds;

    private List<Long> personalityTagIds;

    private String profileImageUrl;
}
