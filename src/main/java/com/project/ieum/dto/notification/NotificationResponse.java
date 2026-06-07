package com.project.ieum.dto.notification;

import com.project.ieum.entity.notification.Notification;
import com.project.ieum.entity.notification.NotificationType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class NotificationResponse {
    private Long id;
    private NotificationType type;
    private String title;
    private String content;
    private String url;
    private Boolean hasRead;
    private LocalDateTime createdAt;

    public static NotificationResponse from(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .type(notification.getType())
                .title(notification.getTitle())
                .content(notification.getContent())
                .url(notification.getUrl())
                .hasRead(notification.getHasRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
