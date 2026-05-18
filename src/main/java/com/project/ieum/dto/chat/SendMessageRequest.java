package com.project.ieum.dto.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SendMessageRequest {

    @NotBlank(message = "메시지를 입력해주세요.")
    @Size(max = 2000, message = "메시지는 2000자 이하로 입력해주세요.")
    private String body;

    private String attachmentUrl;
    private String attachmentType;
}
