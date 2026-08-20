package com.project.ieum.controller.chat;

import com.project.ieum.dto.chat.SendMessageRequest;
import com.project.ieum.service.chat.ChatService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;

import java.security.Principal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * STOMP 개인 에러 큐 회신 검증.
 *
 * <p>여기서 돌려보낸 값은 {@code chat/room.html}이 alert로 그대로 띄운다. HTTP 응답과 같은
 * 노출 경로인데 전역 핸들러를 타지 않으므로, 규칙이 따로 적용됐는지 여기서 확인한다.
 *
 * <p>검증 애노테이션에 message가 없으면 {@code getDefaultMessage()}가 null이고,
 * {@code Map.of}는 null 값을 거부해 예외 핸들러 안에서 다시 터진다. 그 자리도 함께 가드한다.
 */
@ExtendWith(MockitoExtension.class)
class ChatStompControllerTest {

    @Mock private ChatService chatService;
    @Mock private SimpMessagingTemplate messagingTemplate;

    @InjectMocks private ChatStompController controller;

    /** @Valid 실패를 실제 예외 타입으로 재현한다 — 핸들러가 BindingResult를 어떻게 다루는지가 대상이다. */
    private static MethodArgumentNotValidException validationErrorWith(String defaultMessage) throws Exception {
        BeanPropertyBindingResult binding =
                new BeanPropertyBindingResult(new SendMessageRequest(), "sendMessageRequest");
        binding.addError(new FieldError("sendMessageRequest", "body", null, false, null, null, defaultMessage));

        MethodParameter parameter = new MethodParameter(
                ChatStompController.class.getMethod("send", Long.class, SendMessageRequest.class, Principal.class), 1);
        return new MethodArgumentNotValidException(
                MessageBuilder.withPayload("payload").build(), parameter, binding);
    }

    @Test
    @DisplayName("프레임워크가 만든 예외 — 원문 대신 전송 실패 문구가 나간다")
    void frameworkExceptionIsReplaced() {
        IllegalArgumentException e = null;
        try {
            PageRequest.of(-1, 10);
        } catch (IllegalArgumentException caught) {
            e = caught;
        }
        assertThat(e).isNotNull();

        Map<String, String> result = controller.handleError(e);

        assertThat(result.get("message"))
                .isEqualTo("전송에 실패했습니다.")
                .doesNotContain("Page index");
    }

    @Test
    @DisplayName("도메인 예외 — 사용자 안내가 그대로 전달된다")
    void domainExceptionKeepsMessage() {
        Map<String, String> result =
                controller.handleError(new IllegalStateException("종료된 대화방에는 메시지를 보낼 수 없습니다."));

        assertThat(result.get("message")).isEqualTo("종료된 대화방에는 메시지를 보낼 수 없습니다.");
    }

    @Test
    @DisplayName("메시지가 없는 예외 — null이 그대로 나가지 않는다")
    void nullMessageDoesNotEscape() {
        Map<String, String> result = controller.handleError(new IllegalStateException());

        assertThat(result.get("message")).isEqualTo("전송에 실패했습니다.");
    }

    @Test
    @DisplayName("검증 실패 메시지가 null이어도 핸들러 안에서 다시 터지지 않는다")
    void nullDefaultMessageDoesNotCrashHandler() throws Exception {
        Map<String, String> result = controller.handleValidation(validationErrorWith(null));

        assertThat(result.get("message")).isEqualTo("메시지 형식이 올바르지 않습니다.");
    }

    @Test
    @DisplayName("검증 실패 메시지가 있으면 그것을 쓴다")
    void usesFieldMessageWhenPresent() throws Exception {
        Map<String, String> result = controller.handleValidation(validationErrorWith("메시지를 입력해주세요."));

        assertThat(result.get("message")).isEqualTo("메시지를 입력해주세요.");
    }
}
