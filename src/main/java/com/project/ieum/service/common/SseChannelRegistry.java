package com.project.ieum.service.common;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 채널 키(Long)별로 SseEmitter 목록을 관리하는 공용 레지스트리.
 * 알림(userId 채널)과 마켓 채팅(chatId 채널)이 각자 인스턴스를 만들어 사용한다.
 * 같은 채널에 여러 emitter가 붙을 수 있다(멀티 탭 대응).
 */
public class SseChannelRegistry {

    private final ConcurrentHashMap<Long, CopyOnWriteArrayList<SseEmitter>> channels = new ConcurrentHashMap<>();
    private final long timeoutMillis;

    public SseChannelRegistry(long timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }

    /**
     * 채널에 새 emitter를 등록한다.
     * 타임아웃/완료/에러 시 자동으로 목록에서 제거되며,
     * 타임아웃으로 끊긴 연결은 브라우저 EventSource가 스스로 재연결한다.
     */
    public SseEmitter subscribe(Long key) {
        SseEmitter emitter = new SseEmitter(timeoutMillis);
        CopyOnWriteArrayList<SseEmitter> list =
                channels.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>());
        list.add(emitter);

        emitter.onCompletion(() -> remove(key, emitter));
        emitter.onTimeout(() -> remove(key, emitter));
        emitter.onError(e -> remove(key, emitter));

        // 연결 직후 더미 이벤트를 보내 응답 헤더를 즉시 플러시한다.
        try {
            emitter.send(SseEmitter.event().name("connect").data("connected"));
        } catch (IOException e) {
            remove(key, emitter);
        }
        return emitter;
    }

    /** 채널의 모든 구독자에게 이벤트를 보낸다. 전송 실패한 emitter는 끊긴 것으로 보고 제거한다. */
    public void send(Long key, String eventName, Object payload) {
        List<SseEmitter> list = channels.get(key);
        if (list == null) {
            return;
        }
        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(payload));
            } catch (Exception e) {
                remove(key, emitter);
            }
        }
    }

    private void remove(Long key, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> list = channels.get(key);
        if (list == null) {
            return;
        }
        list.remove(emitter);
        if (list.isEmpty()) {
            channels.remove(key, list);
        }
    }
}
