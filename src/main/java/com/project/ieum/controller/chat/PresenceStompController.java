package com.project.ieum.controller.chat;

import com.project.ieum.repository.UserRepository;
import com.project.ieum.service.chat.PresenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Set;

@Controller
@RequiredArgsConstructor
public class PresenceStompController {

    private final PresenceService presenceService;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;

    @MessageMapping("/presence/{conversationId}/enter")
    public void enter(@DestinationVariable Long conversationId,
                      Principal principal,
                      SimpMessageHeaderAccessor headerAccessor) {
        if (principal == null) return;
        userRepository.findByEmail(principal.getName()).ifPresent(user -> {
            presenceService.enter(headerAccessor.getSessionId(), conversationId, user.getId());
            broadcast(conversationId);
        });
    }

    @MessageMapping("/presence/{conversationId}/leave")
    public void leave(@DestinationVariable Long conversationId,
                      SimpMessageHeaderAccessor headerAccessor) {
        presenceService.disconnect(headerAccessor.getSessionId());
        broadcast(conversationId);
    }

    public void broadcast(Long conversationId) {
        Set<Long> online = presenceService.getOnlineUsers(conversationId);
        messagingTemplate.convertAndSend("/topic/presence/" + conversationId, online);
    }
}
