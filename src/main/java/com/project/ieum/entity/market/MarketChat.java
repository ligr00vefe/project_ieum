package com.project.ieum.entity.market;

import com.project.ieum.entity.User;
import com.project.ieum.entity.conversation.ConversationStatus;  // 기존 enum 재사용
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "market_chats",
        uniqueConstraints = @UniqueConstraint(
                // 동일 게시글 + 동일 구매자 → 채팅방 1개만 허용 (중복 방지)
                name = "uq_mc_post_buyer", columnNames = {"post_id", "buyer_id"}),
        indexes = {
                @Index(name = "idx_mc_seller", columnList = "seller_id,last_message_at"),
                @Index(name = "idx_mc_buyer",  columnList = "buyer_id,last_message_at")
        })
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
public class MarketChat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 어떤 게시글에 대한 채팅인지
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    @ToString.Exclude
    private MarketPost post;

    // 판매자 (post.seller와 동일값 — 채팅 목록 조회 편의를 위해 별도 저장)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seller_id", nullable = false)
    @ToString.Exclude
    private User seller;

    // 구매 희망자
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "buyer_id", nullable = false)
    @ToString.Exclude
    private User buyer;

    // 채팅방 상태 — 기존 ConversationStatus 재사용 (ACTIVE / CLOSED)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    @Builder.Default
    private ConversationStatus status = ConversationStatus.ACTIVE;

    // 거래 확정 핸드셰이크 플래그
    // HelpRequestApplication의 requesterStartConfirmed/caregiverStartConfirmed 패턴 동일하게 적용
    // 판매자가 "거래완료" 버튼 클릭 시 true
    @Column(name = "seller_confirmed", nullable = false)
    @Builder.Default
    private boolean sellerConfirmed = false;

    // 구매자가 "거래완료" 버튼 클릭 시 true
    // 양쪽 모두 true → MarketChatService에서 MarketPost.complete() 호출
    @Column(name = "buyer_confirmed", nullable = false)
    @Builder.Default
    private boolean buyerConfirmed = false;

    // 마지막 메시지 수신 시각 — 채팅 목록 최신순 정렬용
    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ── 상태 변경 메서드 ──

    // 메시지 수신 시 호출 — 기존 Conversation.touchLastMessage()와 동일
    public void touchLastMessage() {
        this.lastMessageAt = LocalDateTime.now();
    }

    // 판매자 거래확정
    public void confirmBySeller() {
        this.sellerConfirmed = true;
    }

    // 구매자 거래확정
    public void confirmByBuyer() {
        this.buyerConfirmed = true;
    }

    // 양쪽 모두 확정했는지 확인 — Service에서 이 값이 true면 MarketPost.complete() 호출
    public boolean isBothConfirmed() {
        return sellerConfirmed && buyerConfirmed;
    }

    public void close() {
        this.status = ConversationStatus.CLOSED;
    }
}