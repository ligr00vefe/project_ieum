package com.project.ieum.service.notification;

import com.project.ieum.dto.notification.NotificationResponse;
import com.project.ieum.entity.User;
import com.project.ieum.entity.notification.Notification;
import com.project.ieum.entity.notification.NotificationType;
import com.project.ieum.repository.NotificationRepository;
import com.project.ieum.service.common.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final CurrentUserService currentUserService;

    public Notification create(User receiver, NotificationType type, String title, String content, String url) {
        Notification notification = Notification.builder()
                .receiver(receiver)
                .type(type)
                .title(title)
                .content(content)
                .url(url)
                .build();
        return notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getMyNotifications(Pageable pageable) {
        User currentUser = currentUserService.getCurrentUser();
        return notificationRepository.findByReceiverIdOrderByCreatedAtDesc(currentUser.getId(), pageable)
                .map(NotificationResponse::from);
    }

    @Transactional(readOnly = true)
    public long countMyUnread() {
        User currentUser = currentUserService.getCurrentUser();
        return notificationRepository.countByReceiverIdAndHasReadFalse(currentUser.getId());
    }

    public int markAllRead() {
        User currentUser = currentUserService.getCurrentUser();
        return notificationRepository.markAllRead(currentUser.getId());
    }
}
