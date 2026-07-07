package com.project.ieum.controller.market;

import com.project.ieum.dto.market.MarketMessageResponse;
import com.project.ieum.entity.User;
import com.project.ieum.entity.market.MarketMessage;
import com.project.ieum.service.common.CurrentUserService;
import com.project.ieum.service.market.MarketChatService;
import com.project.ieum.service.market.MarketChatSseService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/market/chat")
public class MarketChatRestController {

    private final MarketChatService marketChatService;
    private final CurrentUserService currentUserService;
    private final MarketChatSseService marketChatSseService;

    // ── 메시지 목록 조회 ──
    // GET /api/market/chat/{chatId}/messages
    // 채팅방 첫 진입 시 이전 메시지 로딩
    @GetMapping("/{chatId}/messages")
    public Page<MarketMessageResponse> messages(
            @PathVariable Long chatId,
            Pageable pageable) {
        return marketChatService.getMessages(chatId, pageable);
    }

    // ── 실시간 수신 스트림 (SSE) ──
    // GET /api/market/chat/{chatId}/stream
    // 채팅방 화면의 EventSource가 구독 — 새 메시지가 "message" 이벤트로 푸시된다
    @GetMapping(value = "/{chatId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@PathVariable Long chatId) {
        marketChatService.getChatForUser(chatId); // 참여자 검증
        return marketChatSseService.subscribe(chatId);
    }

    // ── 메시지 전송 ──
    // POST /api/market/chat/{chatId}/messages
    @PostMapping("/{chatId}/messages")
    public MarketMessageResponse sendMessage(
            @PathVariable Long chatId,
            @RequestBody MarketSendMessageRequest request,
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new SecurityException("로그인이 필요합니다.");
        }

        MarketMessageResponse response = marketChatService.sendMessage(chatId, request.getBody());

        // 저장 완료 후 채팅방 구독자 전원에게 SSE 푸시
        marketChatSseService.push(chatId, response);

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