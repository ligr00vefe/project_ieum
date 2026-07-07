package com.project.ieum.service.notification;

import com.project.ieum.repository.NotificationRepository;
import com.project.ieum.service.common.SseChannelRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

/**
 * 알림 뱃지 실시간 갱신용 SSE.
 * 사용자별(userId 채널)로 구독하며, 알림 생성/읽음 처리 시 미읽음 개수를 푸시한다.
 * 이벤트명: "unread", 페이로드: {"unreadCount": n}
 */
@Service
@RequiredArgsConstructor
public class NotificationSseService {

    private static final long TIMEOUT_MILLIS = 30 * 60 * 1000L; // 30분 — 만료 시 브라우저가 자동 재연결

    private final NotificationRepository notificationRepository;
    private final SseChannelRegistry registry = new SseChannelRegistry(TIMEOUT_MILLIS);

    public SseEmitter subscribe(Long userId) {
        return registry.subscribe(userId);
    }

    /**
     * 트랜잭션 커밋 후에 미읽음 개수를 푸시한다.
     * 커밋 전에 보내면 롤백된 알림이 뱃지에 반영될 수 있어 afterCommit으로 미룬다.
     */
    public void pushUnreadCountAfterCommit(Long userId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    pushUnreadCount(userId);
                }
            });
        } else {
            pushUnreadCount(userId);
        }
    }

    private void pushUnreadCount(Long userId) {
        long count = notificationRepository.countByReceiverIdAndHasReadFalse(userId);
        registry.send(userId, "unread", Map.of("unreadCount", count));
    }
}
