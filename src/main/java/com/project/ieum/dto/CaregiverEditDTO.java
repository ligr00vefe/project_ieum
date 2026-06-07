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

    private List<String> availableTimeSlots;

    private List<Long> regionIds;

    private List<Long> personalityTagIds;
}
