package com.project.ieum.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApplyRequest {

    @Size(max = 500, message = "첫 메시지는 500자 이하로 입력해주세요.")
    private String firstMessage;
}
