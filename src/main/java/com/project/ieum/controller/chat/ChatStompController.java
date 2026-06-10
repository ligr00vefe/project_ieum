package com.project.ieum.controller.chat;

import com.project.ieum.dto.chat.MessageResponse;
import com.project.ieum.dto.chat.SendMessageRequest;
import com.project.ieum.service.chat.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

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
}
