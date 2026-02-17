package com.wenmin.prometheus.websocket;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wenmin.prometheus.security.JwtTokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class DistributeWebSocketHandler extends TextWebSocketHandler {

    private static final long AUTH_TIMEOUT_MS = 30_000; // 30 seconds

    private final ObjectMapper objectMapper;
    private final JwtTokenProvider jwtTokenProvider;

    // taskId -> { sessionId -> WebSocketSession }
    private final Map<String, Map<String, WebSocketSession>> taskSessions = new ConcurrentHashMap<>();

    // Scheduler for auth timeout checks
    private final ScheduledExecutorService authTimeoutScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "ws-auth-timeout");
        t.setDaemon(true);
        return t;
    });

    public DistributeWebSocketHandler(ObjectMapper objectMapper, JwtTokenProvider jwtTokenProvider) {
        this.objectMapper = objectMapper;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Boolean authenticated = (Boolean) session.getAttributes().get("authenticated");
        if (Boolean.TRUE.equals(authenticated)) {
            // Already authenticated via URL token (backward compatibility)
            registerSession(session);
        } else {
            // Schedule auth timeout: close if not authenticated within AUTH_TIMEOUT_MS
            log.info("WebSocket connected (pending auth): sessionId={}", session.getId());
            authTimeoutScheduler.schedule(() -> {
                Boolean auth = (Boolean) session.getAttributes().get("authenticated");
                if (!Boolean.TRUE.equals(auth) && session.isOpen()) {
                    log.warn("WebSocket auth timeout: sessionId={}", session.getId());
                    try {
                        session.close(new CloseStatus(4001, "Authentication timeout"));
                    } catch (IOException e) {
                        log.warn("Failed to close unauthenticated session: {}", e.getMessage());
                    }
                }
            }, AUTH_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        }
    }

    private void registerSession(WebSocketSession session) {
        String taskId = getTaskId(session);
        if (taskId != null) {
            taskSessions.computeIfAbsent(taskId, k -> new ConcurrentHashMap<>())
                    .put(session.getId(), session);
            log.info("WebSocket registered: sessionId={}, taskId={}", session.getId(), taskId);
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
        String payload = message.getPayload();

        // Handle ping from client (allowed even before auth)
        if ("ping".equals(payload)) {
            try {
                session.sendMessage(new TextMessage("pong"));
            } catch (IOException e) {
                log.warn("Failed to send pong: {}", e.getMessage());
            }
            return;
        }

        Boolean authenticated = (Boolean) session.getAttributes().get("authenticated");
        if (!Boolean.TRUE.equals(authenticated)) {
            // Session not yet authenticated, expect auth message
            handleAuthMessage(session, payload);
            return;
        }

        // Session is authenticated, handle normal messages (currently no-op beyond ping)
    }

    private void handleAuthMessage(WebSocketSession session, String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            String type = node.has("type") ? node.get("type").asText() : null;
            String token = node.has("token") ? node.get("token").asText() : null;

            if (!"auth".equals(type) || token == null || token.isBlank()) {
                log.warn("WebSocket received non-auth message from unauthenticated session: sessionId={}", session.getId());
                sendAuthResult(session, false, "Authentication required. Send {\"type\":\"auth\",\"token\":\"xxx\"} first.");
                session.close(new CloseStatus(4003, "Authentication required"));
                return;
            }

            DecodedJWT jwt = jwtTokenProvider.verifyToken(token);
            if (jwt == null) {
                log.warn("WebSocket auth failed: invalid token, sessionId={}", session.getId());
                sendAuthResult(session, false, "Invalid token");
                session.close(new CloseStatus(4002, "Invalid token"));
                return;
            }

            // Authentication successful
            Map<String, Object> attributes = session.getAttributes();
            attributes.put("authenticated", true);
            attributes.put("userId", jwtTokenProvider.getUserId(jwt));
            attributes.put("username", jwtTokenProvider.getUsername(jwt));

            log.info("WebSocket authenticated via message: sessionId={}, userId={}",
                    session.getId(), jwtTokenProvider.getUserId(jwt));

            sendAuthResult(session, true, "Authenticated");

            // Register session to task after authentication
            registerSession(session);

        } catch (Exception e) {
            log.error("Failed to process auth message: sessionId={}, error={}",
                    session.getId(), e.getMessage());
            try {
                sendAuthResult(session, false, "Authentication failed");
                session.close(new CloseStatus(4002, "Authentication failed"));
            } catch (IOException ex) {
                log.warn("Failed to close session after auth error: {}", ex.getMessage());
            }
        }
    }

    private void sendAuthResult(WebSocketSession session, boolean success, String message) {
        try {
            Map<String, Object> result = Map.of(
                    "type", "auth_result",
                    "success", success,
                    "message", message
            );
            String json = objectMapper.writeValueAsString(result);
            synchronized (session) {
                session.sendMessage(new TextMessage(json));
            }
        } catch (IOException e) {
            log.warn("Failed to send auth result: {}", e.getMessage());
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
