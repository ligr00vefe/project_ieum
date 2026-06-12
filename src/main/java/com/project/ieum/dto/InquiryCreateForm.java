package com.project.ieum.dto;

import com.project.ieum.entity.inquiry.InquiryCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InquiryCreateForm {

    @NotBlank(message = "제목을 입력해주세요.")
    @Size(max = 200, message = "제목은 200자 이내로 입력해주세요.")
    private String title;

    @NotBlank(message = "내용을 입력해주세요.")
    private String body;

    @NotNull(message = "카테고리를 선택해주세요.")
    private InquiryCategory category;

    private boolean secret = false;
}
