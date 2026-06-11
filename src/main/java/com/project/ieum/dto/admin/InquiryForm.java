package com.project.ieum.dto.admin;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InquiryForm {

    @NotBlank(message = "답변 내용을 입력해주세요.")
    private String body;
}
