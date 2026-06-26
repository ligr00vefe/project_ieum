package com.project.ieum.dto.chat;

import com.project.ieum.entity.conversation.Message;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class MessageResponse {
    private Long id;
    private Long conversationId;
    private Long senderId;
    private String senderName;
    private String body;
    private String attachmentUrl;
    private String attachmentType;
    private Boolean hasRead;
    private Boolean mine;
    private String senderRole;
    private LocalDateTime sentAt;

    public static MessageResponse from(Message message, Long currentUserId) {
        return MessageResponse.builder()
                .id(message.getId())
                .conversationId(message.getConversation().getId())
                .senderId(message.getSender().getId())
                .senderName(message.getSender().getProfile() != null
                        ? message.getSender().getProfile().getFullName()
                        : message.getSender().getEmail())
                .body(message.getBody())
                .attachmentUrl(message.getAttachmentUrl())
                .attachmentType(message.getAttachmentType())
                .hasRead(message.getHasRead())
                .mine(message.getSender().getId().equals(currentUserId))
                .senderRole(message.getSender().getRole().name())
                .sentAt(message.getSentAt())
                .build();
    }
}
