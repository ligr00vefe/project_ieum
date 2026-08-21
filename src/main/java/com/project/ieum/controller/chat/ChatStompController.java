package com.project.ieum.controller.chat;

import com.project.ieum.dto.chat.MessageResponse;
import com.project.ieum.dto.chat.SendMessageRequest;
import com.project.ieum.service.chat.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
            message = e.getBindingResult().getFieldError().getDefaultMessage();
        }
        return Map.of("message", message);
    }

    @MessageExceptionHandler(Exception.class)
    @SendToUser("/queue/errors")
    public Map<String, String> handleError(Exception e) {
        return Map.of("message", "메시지 전송에 실패했습니다.");
    }
}
