package com.project.ieum.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PopupForm {

    @NotBlank(message = "팝업명을 입력해주세요.")
    @Size(max = 100, message = "팝업명은 100자 이내로 입력해주세요.")
    private String name;

    @NotBlank(message = "내용을 입력해주세요.")
    private String content;

    @NotBlank(message = "기간을 선택해주세요.")
    private String duration; // HOUR_1, HOUR_24, WEEK_1, MONTH_1

    private String layout = "AUTO";

    private String linkUrl; // 팝업 클릭 시 이동할 URL (선택)
}
