package com.project.ieum.controller.market;

import com.project.ieum.dto.market.MarketMessageResponse;
import com.project.ieum.entity.User;
import com.project.ieum.entity.market.MarketMessage;
import com.project.ieum.service.common.CurrentUserService;
import com.project.ieum.service.market.MarketChatService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/market/chat")
public class MarketChatRestController {

    private final MarketChatService marketChatService;
    private final CurrentUserService currentUserService;
    private final SimpMessagingTemplate messagingTemplate; // 기존 Bean 재사용 — 설정 변경 없음

    // ── 메시지 목록 조회 ──
    // GET /api/market/chat/{chatId}/messages
    // 채팅방 첫 진입 시 이전 메시지 로딩
    @GetMapping("/{chatId}/messages")
    public Page<MarketMessageResponse> messages(
            @PathVariable Long chatId,
            Pageable pageable) {
        return marketChatService.getMessages(chatId, pageable);
    }

    // ── 메시지 전송 (REST 경로) ──
    // POST /api/market/chat/{chatId}/messages
    // STOMP 연결 실패 시 폴백 경로 (또는 파일 첨부 시)
    @PostMapping("/{chatId}/messages")
    public MarketMessageResponse sendMessage(
            @PathVariable Long chatId,
            @RequestBody MarketSendMessageRequest request,
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new SecurityException("로그인이 필요합니다.");
        }

        MarketMessageResponse response = marketChatService.sendMessage(chatId, request.getBody());

        // STOMP 브로드캐스트 — 기존 ChatRestController와 동일한 패턴
        // 마켓 채팅은 /topic/market/{chatId} 토픽 사용
        messagingTemplate.convertAndSend("/topic/market/" + chatId, response);

        return response;
    }

    // ── 읽음 처리 ──
    // POST /api/market/chat/{chatId}/read
    @PostMapping("/{chatId}/read")
    public Map<String, Integer> markRead(@PathVariable Long chatId) {
        int count = marketChatService.markRead(chatId);
        return Map.of("updatedCount", count);
    }

    // ── 거래 확정 ──
    // POST /api/market/chat/{chatId}/confirm
    @PostMapping("/{chatId}/confirm")
    public Map<String, String> confirm(@PathVariable Long chatId) {
        marketChatService.confirmPurchase(chatId);
        return Map.of("message", "거래 확정 처리되었습니다.");
    }

    // 메시지 전송 요청 DTO (내부 클래스로 간단히 처리)
    @Getter
    @Setter
    public static class MarketSendMessageRequest {
        @NotBlank(message = "메시지를 입력해주세요.")
        @Size(max = 2000)
        private String body;
    }
}