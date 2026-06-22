package com.project.ieum.entity.market;

import com.project.ieum.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "market_messages",
        indexes = {
                // 채팅방별 메시지 조회 (시간순)
                @Index(name = "idx_mm_chat_sent", columnList = "chat_id,sent_at"),
                // 발신자 기반 조회
                @Index(name = "idx_mm_sender", columnList = "sender_id")
        })
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
public class MarketMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 어떤 채팅방의 메시지인지
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chat_id", nullable = false)
    @ToString.Exclude
    private MarketChat chat;

    // 발신자
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    @ToString.Exclude
    private User sender;

    // 메시지 본문
    @Column(columnDefinition = "TEXT", nullable = false)
    private String body;

    // 읽음 여부 — false: 안 읽음, true: 읽음
    @Column(name = "has_read", nullable = false)
    @Builder.Default
    private boolean hasRead = false;

    // 발신 시각
    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    // 읽음 처리
    public void read() {
        this.hasRead = true;
    }
}