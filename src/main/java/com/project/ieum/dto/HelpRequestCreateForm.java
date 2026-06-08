package com.project.ieum.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

// 도움 요청 작성 폼 백킹 객체 (Thymeleaf th:object 바인딩).
// 기존 폼 DTO(BasicInfoDTO) 컨벤션: @Data 가변 클래스 + jakarta.validation + @DateTimeFormat.
// "create=모든 정보 필수"는 여기 Bean Validation으로 강제(→ MethodArgumentNotValidException → 400 기존 핸들러).
// 엔티티 verifyTimeRange()는 최종 방어선일 뿐, 사용자 친화 검증은 이 폼에서 끝낸다.
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HelpRequestCreateForm {
// 사용자 입력 내용 필드

    @NotBlank(message = "제목을 입력해주세요.")
    private String title;

    @NotNull(message = "서비스 분류를 선택해주세요.")
    private Long serviceCategoryId;

    @NotBlank(message = "내용을 입력해주세요.")
    private String body;

    private String specialNotes;

    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime desiredStartDatetime;

    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime desiredEndDatetime;

    // 주소 검색 위젯이 자동으로 채울 필드 (roadAddress·sido·sigungu·bname)
    @NotBlank(message = "주소를 검색해주세요")
    private String roadAddress;

    @NotBlank
    private String sido;

    @NotBlank
    private String sigungu;

    private String bname;

    // 상세주소(건물명·호수 등) — 사용자 직접 입력, 선택
    private String addressDetail;


    @AssertTrue(message = "종료 시간은 시작 시간 이후여야 합니다")
    public boolean isEndAfterStart() {
        if (desiredStartDatetime == null || desiredEndDatetime == null)
            return true;

        return !desiredEndDatetime.isBefore(desiredStartDatetime);
    }
}
