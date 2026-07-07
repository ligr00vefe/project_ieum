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
    private final NotificationSseService notificationSseService;

    public Notification create(User receiver, NotificationType type, String title, String content, String url) {
        Notification notification = Notification.builder()
                .receiver(receiver)
                .type(type)
                .title(title)
                .content(content)
                .url(url)
                .build();
        Notification saved = notificationRepository.save(notification);
        notificationSseService.pushUnreadCountAfterCommit(receiver.getId());
        return saved;
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
        int updated = notificationRepository.markAllRead(currentUser.getId());
        notificationSseService.pushUnreadCountAfterCommit(currentUser.getId());
        return updated;
    }

    public void markRead(Long notificationId) {
        User currentUser = currentUserService.getCurrentUser();
        notificationRepository.findById(notificationId).ifPresent(n -> {
            if (n.getReceiver().getId().equals(currentUser.getId())) {
                n.markRead();
                notificationSseService.pushUnreadCountAfterCommit(currentUser.getId());
            }
        });
    }
}
