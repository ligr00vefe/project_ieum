package com.project.ieum.dto.request;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class HelpRequestForm {

    @NotBlank(message = "제목을 입력해주세요.")
    private String title;

    private String body;

    @NotNull(message = "서비스 종류를 선택해주세요.")
    private Long serviceCategoryId;

    @NotNull(message = "지역을 선택해주세요.")
    private Long regionId;

    @NotNull(message = "희망 날짜를 선택해주세요.")
    @FutureOrPresent(message = "희망 날짜는 오늘 이후여야 합니다.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate desiredDate;

    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime desiredStartTime;

    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime desiredEndTime;

    private String addressDetail;
    private String specialNotes;
    private List<Long> personalityTagIds = new ArrayList<>();
}
