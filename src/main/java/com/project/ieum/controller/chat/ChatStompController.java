package com.project.ieum.controller.chat;

import com.project.ieum.dto.chat.MessageResponse;
import com.project.ieum.dto.chat.SendMessageRequest;
import com.project.ieum.exception.ClientSafeMessage;
import com.project.ieum.service.chat.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatStompController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat/{conversationId}/send")
    public void send(
            @DestinationVariable Long conversationId,
            @Payload @Valid SendMessageRequest request,
            Principal principal) {

        if (principal == null) {
            throw new SecurityException("로그인이 필요합니다.");
        }

        MessageResponse response = chatService.sendMessage(conversationId, principal.getName(), request);
        messagingTemplate.convertAndSend("/topic/conversations/" + conversationId, response);
    }

    // STOMP에는 HTTP 상태 코드가 없으므로 실패를 보낸 사람의 개인 큐로 회신한다.
    @MessageExceptionHandler(MethodArgumentNotValidException.class)
    @SendToUser("/queue/errors")
    public Map<String, String> handleValidation(MethodArgumentNotValidException e) {
        String message = "메시지 형식이 올바르지 않습니다.";
        if (e.getBindingResult() != null && e.getBindingResult().getFieldError() != null) {
            // 제약 애노테이션에 message를 안 붙이면 null이 올 수 있고,
            // Map.of는 null 값을 받지 않아 핸들러 안에서 다시 터진다.
            String fieldMessage = e.getBindingResult().getFieldError().getDefaultMessage();
            if (fieldMessage != null && !fieldMessage.isBlank()) {
                message = fieldMessage;
            }
        }
        return Map.of("message", message);
    }

    /**
     * 여기서 돌려보낸 메시지를 chat/room.html이 alert로 그대로 띄운다. HTTP 쪽과
     * 같은 규칙으로 거른다({@link ClientSafeMessage}) — 안 그러면 전역 핸들러를 고쳐도
     * 이 경로로 동일한 누출이 그대로 남는다.
     */
    @MessageExceptionHandler(Exception.class)
    @SendToUser("/queue/errors")
    public Map<String, String> handleError(Exception e) {
        log.warn("STOMP message handling failed", e);
        return Map.of("message", ClientSafeMessage.of(e, "전송에 실패했습니다."));
    }
}
