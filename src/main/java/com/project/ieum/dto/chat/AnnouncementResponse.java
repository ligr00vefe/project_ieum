package com.project.ieum.dto.chat;

import com.project.ieum.entity.conversation.ChatAnnouncement;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class AnnouncementResponse {
    private Long id;
    private String body;
    private LocalDateTime updatedAt;

    public static AnnouncementResponse from(ChatAnnouncement a) {
        return new AnnouncementResponse(a.getId(), a.getBody(), a.getUpdatedAt());
    }
}
