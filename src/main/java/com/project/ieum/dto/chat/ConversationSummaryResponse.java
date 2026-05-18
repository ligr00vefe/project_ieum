package com.project.ieum.dto.chat;

import com.project.ieum.entity.conversation.ConversationStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ConversationSummaryResponse {
    private Long conversationId;
    private Long opponentUserId;
    private String opponentName;
    private ConversationStatus status;
    private String lastMessage;
    private LocalDateTime lastMessageAt;
    private long unreadCount;
}
