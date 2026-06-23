package com.project.ieum.dto.market;

import com.project.ieum.entity.market.MarketMessage;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class MarketMessageResponse {

    private Long id;
    private Long chatId;           // 어떤 채팅방의 메시지인지
    private Long senderId;
    private String senderEmail;
    private String body;
    private boolean hasRead;
    private boolean mine;          // 현재 사용자가 보낸 메시지인지 (말풍선 방향 결정용)
    private LocalDateTime sentAt;

    // ── 정적 팩토리 메서드 ──
    public static MarketMessageResponse from(MarketMessage message, Long currentUserId) {
        return MarketMessageResponse.builder()
                .id(message.getId())
                .chatId(message.getChat().getId())
                .senderId(message.getSender().getId())
                .senderEmail(message.getSender().getEmail())
                .body(message.getBody())
                .hasRead(message.isHasRead())
                .mine(message.getSender().getId().equals(currentUserId))
                .sentAt(message.getSentAt())
                .build();
    }
}