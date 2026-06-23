package com.project.ieum.controller.market;

import com.project.ieum.dto.market.MarketMessageResponse;
import com.project.ieum.service.market.MarketChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class MarketStompController {

    private final MarketChatService marketChatService;
    private final SimpMessagingTemplate messagingTemplate;

    // STOMP 메시지 수신 경로: /app/market/chat/{chatId}/send
    // 발행(브로드캐스트) 경로:  /topic/market/{chatId}
    // 기존 ChatStompController의 /app/chat/{conversationId}/send 와 경로만 다름
    @MessageMapping("/market/chat/{chatId}/send")
    public void send(
            @DestinationVariable Long chatId,
            @Payload MarketChatRestController.MarketSendMessageRequest request,
            Principal principal) {

        if (principal == null) {
            throw new SecurityException("로그인이 필요합니다.");
        }

        // Service에서 메시지 저장 + DB 처리
        MarketMessageResponse response = marketChatService.sendMessage(chatId, request.getBody());

        // 채팅방 참여자 모두에게 실시간 브로드캐스트
        messagingTemplate.convertAndSend("/topic/market/" + chatId, response);
    }
}