package com.project.ieum.dto.market;

import com.project.ieum.entity.conversation.ConversationStatus;
import com.project.ieum.entity.market.MarketChat;
import com.project.ieum.entity.market.MarketMessage;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class MarketChatSummaryResponse {

    private Long chatId;

    // 게시글 정보 (채팅 목록에서 상품 요약 표시용)
    private Long postId;
    private String postTitle;
    private String postThumbnailUrl;   // 대표 이미지
    private String postStatusLabel;    // 판매중 / 예약중 / 판매완료

    // 상대방 정보 (현재 사용자 기준으로 판매자 or 구매자)
    private Long opponentId;
    private String opponentEmail;

    // 마지막 메시지
    private String lastMessage;
    private LocalDateTime lastMessageAt;

    // 안 읽은 메시지 수 (뱃지)
    private long unreadCount;

    private ConversationStatus status;

    // 거래 확정 여부 (채팅방 카드에서 "거래완료" 표시용)
    private boolean sellerConfirmed;
    private boolean buyerConfirmed;

    // ── 정적 팩토리 메서드 ──
    public static MarketChatSummaryResponse from(
            MarketChat chat,
            Long currentUserId,
            String postThumbnailUrl,
            MarketMessage lastMsg,
            long unreadCount) {

        // 현재 사용자 기준으로 상대방 결정
        boolean isSeller = chat.getSeller().getId().equals(currentUserId);
        Long opponentId   = isSeller ? chat.getBuyer().getId()    : chat.getSeller().getId();
        String opponentEmail = isSeller ? chat.getBuyer().getEmail() : chat.getSeller().getEmail();

        String postStatusLabel = switch (chat.getPost().getStatus()) {
            case ACTIVE   -> "판매중";
            case RESERVED -> "예약중";
            case SOLD     -> "판매완료";
            case REMOVED  -> "삭제됨";
        };

        return MarketChatSummaryResponse.builder()
                .chatId(chat.getId())
                .postId(chat.getPost().getId())
                .postTitle(chat.getPost().getTitle())
                .postThumbnailUrl(postThumbnailUrl)
                .postStatusLabel(postStatusLabel)
                .opponentId(opponentId)
                .opponentEmail(opponentEmail)
                .lastMessage(lastMsg != null ? lastMsg.getBody() : null)
                .lastMessageAt(lastMsg != null ? lastMsg.getSentAt() : chat.getLastMessageAt())
                .unreadCount(unreadCount)
                .status(chat.getStatus())
                .sellerConfirmed(chat.isSellerConfirmed())
                .buyerConfirmed(chat.isBuyerConfirmed())
                .build();
    }
}