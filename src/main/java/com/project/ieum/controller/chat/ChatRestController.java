package com.project.ieum.controller.chat;

import com.project.ieum.dto.chat.ConversationSummaryResponse;
import com.project.ieum.dto.chat.MessageResponse;
import com.project.ieum.dto.chat.ReadMessageResponse;
import com.project.ieum.dto.chat.SendMessageRequest;
import com.project.ieum.service.chat.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatRestController {

    private final ChatService chatService;

    @GetMapping("/conversations")
    public Page<ConversationSummaryResponse> conversations(Pageable pageable) {
        return chatService.getMyConversations(pageable);
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public Page<MessageResponse> messages(@PathVariable Long conversationId, Pageable pageable) {
        return chatService.getMessages(conversationId, pageable);
    }

    @PostMapping("/conversations/{conversationId}/messages")
    public MessageResponse sendMessage(
            @PathVariable Long conversationId,
            @Valid @RequestBody SendMessageRequest request,
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new SecurityException("로그인이 필요합니다.");
        }

        return chatService.sendMessage(conversationId, authentication.getName(), request);
    }

    @PostMapping("/conversations/{conversationId}/read")
    public ReadMessageResponse markRead(@PathVariable Long conversationId) {
        return new ReadMessageResponse(chatService.markRead(conversationId));
    }
}
