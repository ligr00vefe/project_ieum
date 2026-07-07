package com.project.ieum.service.market;

import com.project.ieum.dto.market.MarketMessageResponse;
import com.project.ieum.service.common.SseChannelRegistry;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 마켓 채팅 실시간 수신용 SSE.
 * 채팅방별(chatId 채널)로 구독하며, 메시지 저장 후 참여자 전원에게 푸시한다.
 * 이벤트명: "message", 페이로드: MarketMessageResponse(JSON)
 * 참여자 검증은 구독 전에 컨트롤러에서 수행한다.
 */
@Service
public class MarketChatSseService {

    private static final long TIMEOUT_MILLIS = 30 * 60 * 1000L; // 30분 — 만료 시 브라우저가 자동 재연결

    private final SseChannelRegistry registry = new SseChannelRegistry(TIMEOUT_MILLIS);

    public SseEmitter subscribe(Long chatId) {
        return registry.subscribe(chatId);
    }

    public void push(Long chatId, MarketMessageResponse message) {
        registry.send(chatId, "message", message);
    }
}
