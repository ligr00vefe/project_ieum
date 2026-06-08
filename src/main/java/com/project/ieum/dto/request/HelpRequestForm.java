package com.project.ieum.dto.request;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
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

    @NotNull(message = "희망 시작 일시를 선택해주세요.")
    @FutureOrPresent(message = "희망 일시는 현재 이후여야 합니다.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime desiredStartDatetime;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime desiredEndDatetime;

    @NotBlank(message = "도로명주소를 입력해주세요.")
    private String roadAddress;

    private String addressDetail;

    @NotBlank(message = "시/도를 입력해주세요.")
    private String sido;

    @NotBlank(message = "시/군/구를 입력해주세요.")
    private String sigungu;

    private String bname;
    private String zonecode;
    private String bcode;

    private String specialNotes;
    private List<Long> personalityTagIds = new ArrayList<>();
}
