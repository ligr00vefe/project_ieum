package com.project.ieum.dto.request;

import jakarta.validation.constraints.AssertTrue;
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
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime desiredStartDatetime;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
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

    private String departureAddress;
    private String destinationAddress;

    private String specialNotes;
    private List<Long> personalityTagIds = new ArrayList<>();

    // (이슈 #8 정본) 시간 순서 검증 — 종료가 시작 이후여야 한다.
    // @AssertTrue 는 true=유효. 둘 중 하나라도 null이면 @NotNull 책임이라 여기선 통과(true)시킨다.
    // 엔티티 verifyTimeRange()는 최종 방어선(→500)이라, 사용자 입력 오류는 폼에서 400으로 끝낸다.
    @AssertTrue(message = "종료 시간은 시작 시간 이후여야 합니다.")
    public boolean isEndAfterStart() {
        if (desiredStartDatetime == null || desiredEndDatetime == null) {
            return true;
        }
        return !desiredEndDatetime.isBefore(desiredStartDatetime);
    }
}
