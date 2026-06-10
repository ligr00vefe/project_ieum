package com.project.ieum.dto;

import com.project.ieum.entity.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BasicInfoDTO {

    @NotBlank(message = "이메일을 입력해주세요")
    @Email(message = "이메일 형식이 올바르지 않습니다")
    private String email;

    @NotBlank(message = "비밀번호를 입력해주세요")
//    @Pattern(
//        regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$",
//        message = "비밀번호는 영문·숫자·특수문자(@$!%*#?&)를 포함한 8자 이상이어야 합니다"
//    )
    private String password;

    @NotBlank(message = "이름을 입력해주세요")
    private String name;

    @NotNull(message = "생년월일을 선택해주세요")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthDate;

    @NotNull(message = "성별을 선택해주세요")
    private Gender gender;

    @NotBlank(message = "전화번호를 입력해주세요")
    @Pattern(
        regexp = "^01[016789]-?\\d{3,4}-?\\d{4}$",
        message = "올바른 전화번호 형식으로 입력해주세요 (예: 010-1234-5678)"
    )
    private String phone;

    // 장애인 전용: 보호자 정보 (선택)
    private String guardianName;
    private String guardianPhone;
}
