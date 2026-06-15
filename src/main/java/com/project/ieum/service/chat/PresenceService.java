package com.project.ieum.service.chat;

import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class PresenceService {

    // sessionId -> [conversationId, userId]
    private final ConcurrentHashMap<String, long[]> sessions = new ConcurrentHashMap<>();

    public void enter(String sessionId, Long conversationId, Long userId) {
        sessions.put(sessionId, new long[]{conversationId, userId});
    }

    /** Returns [conversationId, userId] of the disconnected session, or null if not tracked. */
    public long[] disconnect(String sessionId) {
        return sessions.remove(sessionId);
    }

    public Set<Long> getOnlineUsers(Long conversationId) {
        return sessions.values().stream()
                .filter(v -> v[0] == conversationId)
                .map(v -> v[1])
                .collect(Collectors.toSet());
    }
}
