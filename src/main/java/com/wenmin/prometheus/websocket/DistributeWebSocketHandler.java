package com.wenmin.prometheus.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class DistributeWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;

    // taskId -> { sessionId -> WebSocketSession }
    private final Map<String, Map<String, WebSocketSession>> taskSessions = new ConcurrentHashMap<>();

    public DistributeWebSocketHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String taskId = getTaskId(session);
        if (taskId != null) {
            taskSessions.computeIfAbsent(taskId, k -> new ConcurrentHashMap<>())
                    .put(session.getId(), session);
            log.info("WebSocket connected: sessionId={}, taskId={}", session.getId(), taskId);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String taskId = getTaskId(session);
        if (taskId != null) {
            Map<String, WebSocketSession> sessions = taskSessions.get(taskId);
            if (sessions != null) {
                sessions.remove(session.getId());
                if (sessions.isEmpty()) {
                    taskSessions.remove(taskId);
                }
            }
        }
        log.info("WebSocket disconnected: sessionId={}, status={}", session.getId(), status);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // Handle ping from client
        if ("ping".equals(message.getPayload())) {
            try {
                session.sendMessage(new TextMessage("pong"));
            } catch (IOException e) {
                log.warn("Failed to send pong: {}", e.getMessage());
            }
        }
    }

    public void sendProgress(String taskId, String detailId, String machineIp,
                             int progress, String currentStep, String component) {
        Map<String, Object> msg = Map.of(
                "type", "progress",
                "taskId", taskId,
                "detailId", detailId,
                "machineIp", machineIp,
                "progress", progress,
                "currentStep", currentStep,
                "component", component
        );
        broadcastToTask(taskId, msg);
    }

    public void sendLog(String taskId, String detailId, String machineIp,
                        String line, String level) {
        Map<String, Object> msg = Map.of(
                "type", "log",
                "taskId", taskId,
                "detailId", detailId,
                "machineIp", machineIp,
                "line", line,
                "timestamp", java.time.LocalDateTime.now().toString(),
                "level", level
        );
        broadcastToTask(taskId, msg);
    }

    public void sendStatus(String taskId, String detailId, String machineIp,
                           String status, String message) {
        Map<String, Object> msg = Map.of(
                "type", "status",
                "taskId", taskId,
                "detailId", detailId,
                "machineIp", machineIp,
                "status", status,
                "message", message
        );
        broadcastToTask(taskId, msg);
    }

    private void broadcastToTask(String taskId, Map<String, Object> message) {
        Map<String, WebSocketSession> sessions = taskSessions.get(taskId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }

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
                        log.warn("Failed to send WebSocket message to session {}: {}",
                                session.getId(), e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to serialize WebSocket message", e);
        }
    }

    private String getTaskId(WebSocketSession session) {
        Map<String, Object> attributes = session.getAttributes();
        return (String) attributes.get("taskId");
    }
}
