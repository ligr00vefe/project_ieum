package com.project.ieum.controller.notification;

import com.project.ieum.dto.notification.NotificationResponse;
import com.project.ieum.dto.notification.UnreadNotificationResponse;
import com.project.ieum.service.common.CurrentUserService;
import com.project.ieum.service.notification.NotificationService;
import com.project.ieum.service.notification.NotificationSseService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationSseService notificationSseService;
    private final CurrentUserService currentUserService;

    @GetMapping
    public Page<NotificationResponse> list(Pageable pageable) {
        return notificationService.getMyNotifications(pageable);
    }

    // 알림 뱃지 실시간 스트림 — 헤더의 EventSource가 구독
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        return notificationSseService.subscribe(currentUserService.getCurrentUser().getId());
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

    @PostMapping("/{id}/read")
    public UnreadNotificationResponse readOne(@PathVariable Long id) {
        notificationService.markRead(id);
        return new UnreadNotificationResponse(notificationService.countMyUnread());
    }
}
