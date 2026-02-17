package com.wenmin.prometheus.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketSessionManager {

    private final ObjectMapper objectMapper;

    // userId -> { sessionId -> WebSocketSession }
    private final Map<String, Map<String, WebSocketSession>> userSessions = new ConcurrentHashMap<>();

    public void register(String userId, WebSocketSession session) {
        userSessions.computeIfAbsent(userId, k -> new ConcurrentHashMap<>())
                .put(session.getId(), session);
        log.info("WebSocket session registered: userId={}, sessionId={}", userId, session.getId());
    }

    public void unregister(String userId, String sessionId) {
        Map<String, WebSocketSession> sessions = userSessions.get(userId);
        if (sessions != null) {
            sessions.remove(sessionId);
            if (sessions.isEmpty()) {
                userSessions.remove(userId);
            }
        }
        log.info("WebSocket session unregistered: userId={}, sessionId={}", userId, sessionId);
    }

    public void sendToUser(String userId, Object message) {
        Map<String, WebSocketSession> sessions = userSessions.get(userId);
        if (sessions == null || sessions.isEmpty()) return;

        try {
            String json = objectMapper.writeValueAsString(message);
            TextMessage textMessage = new TextMessage(json);
            for (WebSocketSession session : sessions.values()) {
                if (session.isOpen()) {
                    try {
                        synchronized (session) {
                            session.sendMessage(textMessage);
                        }
                    } catch (IOException e) {
                        log.warn("Failed to send message to session {}: {}", session.getId(), e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to serialize WebSocket message", e);
        }
    }

    public void sendToUsers(Collection<String> userIds, Object message) {
        for (String userId : userIds) {
            sendToUser(userId, message);
        }
    }

    public boolean isOnline(String userId) {
        Map<String, WebSocketSession> sessions = userSessions.get(userId);
        return sessions != null && !sessions.isEmpty();
    }

    public Set<String> getOnlineUserIds() {
        return Collections.unmodifiableSet(userSessions.keySet());
    }
}
