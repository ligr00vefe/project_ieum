package com.project.ieum.controller.notification;

import com.project.ieum.dto.notification.NotificationResponse;
import com.project.ieum.dto.notification.UnreadNotificationResponse;
import com.project.ieum.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public Page<NotificationResponse> list(Pageable pageable) {
        return notificationService.getMyNotifications(pageable);
    }

    @GetMapping("/unread-count")
    public UnreadNotificationResponse unreadCount() {
        return new UnreadNotificationResponse(notificationService.countMyUnread());
    }

    @PostMapping("/read-all")
    public UnreadNotificationResponse readAll() {
        notificationService.markAllRead();
        return new UnreadNotificationResponse(notificationService.countMyUnread());
    }
}
