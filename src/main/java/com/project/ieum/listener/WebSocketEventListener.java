package com.project.ieum.listener;

import com.project.ieum.controller.chat.PresenceStompController;
import com.project.ieum.service.chat.PresenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final PresenceService presenceService;
    private final PresenceStompController presenceStompController;

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        long[] info = presenceService.disconnect(event.getSessionId());
        if (info == null) return;
        Long conversationId = info[0];
        presenceStompController.broadcast(conversationId);
    }
}
