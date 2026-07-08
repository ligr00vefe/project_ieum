package com.project.ieum.controller.chat;

import com.project.ieum.dto.chat.AnnouncementResponse;
import com.project.ieum.dto.chat.ConversationSummaryResponse;
import com.project.ieum.dto.chat.MessageResponse;
import com.project.ieum.dto.chat.ReadMessageResponse;
import com.project.ieum.service.chat.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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

    @PostMapping("/conversations/{conversationId}/read")
    public ReadMessageResponse markRead(@PathVariable Long conversationId) {
        return new ReadMessageResponse(chatService.markRead(conversationId));
    }

    @GetMapping("/conversations/{conversationId}/announcement")
    public ResponseEntity<AnnouncementResponse> getAnnouncement(@PathVariable Long conversationId) {
        return chatService.getAnnouncement(conversationId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @PostMapping("/conversations/{conversationId}/announcement")
    @PreAuthorize("hasRole('ADMIN')")
    public AnnouncementResponse saveAnnouncement(
            @PathVariable Long conversationId,
            @RequestBody Map<String, String> body) {
        return chatService.saveAnnouncement(conversationId, body.getOrDefault("body", "").trim());
    }
}
