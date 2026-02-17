package com.wenmin.prometheus.websocket;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wenmin.prometheus.module.chat.service.ChatService;
import com.wenmin.prometheus.security.JwtTokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class MessageWebSocketHandler extends TextWebSocketHandler {

    private static final long AUTH_TIMEOUT_MS = 30_000;

    private final ObjectMapper objectMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final WebSocketSessionManager sessionManager;
    private final ChatService chatService;

    private final ScheduledExecutorService authTimeoutScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "msg-ws-auth-timeout");
        t.setDaemon(true);
        return t;
    });

    public MessageWebSocketHandler(ObjectMapper objectMapper,
                                   JwtTokenProvider jwtTokenProvider,
                                   WebSocketSessionManager sessionManager,
                                   @Lazy ChatService chatService) {
        this.objectMapper = objectMapper;
        this.jwtTokenProvider = jwtTokenProvider;
        this.sessionManager = sessionManager;
        this.chatService = chatService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Boolean authenticated = (Boolean) session.getAttributes().get("authenticated");
        if (Boolean.TRUE.equals(authenticated)) {
            String userId = (String) session.getAttributes().get("userId");
            sessionManager.register(userId, session);
            broadcastUserStatus(userId, true);
        } else {
            log.info("Message WebSocket connected (pending auth): sessionId={}", session.getId());
            authTimeoutScheduler.schedule(() -> {
                Boolean auth = (Boolean) session.getAttributes().get("authenticated");
                if (!Boolean.TRUE.equals(auth) && session.isOpen()) {
                    log.warn("Message WebSocket auth timeout: sessionId={}", session.getId());
                    try {
                        session.close(new CloseStatus(4001, "Authentication timeout"));
                    } catch (IOException e) {
                        log.warn("Failed to close unauthenticated session: {}", e.getMessage());
                    }
                }
            }, AUTH_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String userId = (String) session.getAttributes().get("userId");
        if (userId != null) {
            sessionManager.unregister(userId, session.getId());
            if (!sessionManager.isOnline(userId)) {
                broadcastUserStatus(userId, false);
            }
        }
        log.info("Message WebSocket disconnected: sessionId={}, status={}", session.getId(), status);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String payload = message.getPayload();

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
            handleAuthMessage(session, payload);
            return;
        }

        handleBusinessMessage(session, payload);
    }

    private void handleAuthMessage(WebSocketSession session, String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            String type = node.has("type") ? node.get("type").asText() : null;
            String token = node.has("token") ? node.get("token").asText() : null;

            if (!"auth".equals(type) || token == null || token.isBlank()) {
                sendAuthResult(session, false, "Authentication required");
                session.close(new CloseStatus(4003, "Authentication required"));
                return;
            }

            DecodedJWT jwt = jwtTokenProvider.verifyToken(token);
            if (jwt == null) {
                sendAuthResult(session, false, "Invalid token");
                session.close(new CloseStatus(4002, "Invalid token"));
                return;
            }

            Map<String, Object> attributes = session.getAttributes();
            String userId = jwtTokenProvider.getUserId(jwt);
            attributes.put("authenticated", true);
            attributes.put("userId", userId);
            attributes.put("username", jwtTokenProvider.getUsername(jwt));

            sessionManager.register(userId, session);
            sendAuthResult(session, true, "Authenticated");
            broadcastUserStatus(userId, true);

            log.info("Message WebSocket authenticated: sessionId={}, userId={}", session.getId(), userId);
        } catch (Exception e) {
            log.error("Failed to process auth message: {}", e.getMessage());
            try {
                sendAuthResult(session, false, "Authentication failed");
                session.close(new CloseStatus(4002, "Authentication failed"));
            } catch (IOException ex) {
                log.warn("Failed to close session: {}", ex.getMessage());
            }
        }
    }

    private void handleBusinessMessage(WebSocketSession session, String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            String type = node.has("type") ? node.get("type").asText() : "";
            String userId = (String) session.getAttributes().get("userId");

            switch (type) {
                case "chat_message" -> {
                    String conversationId = node.get("conversationId").asText();
                    String content = node.get("content").asText();
                    chatService.sendMessageViaWebSocket(userId, conversationId, content);
                }
                case "mark_read" -> {
                    String conversationId = node.get("conversationId").asText();
                    String messageId = node.has("messageId") ? node.get("messageId").asText() : null;
                    chatService.markConversationRead(userId, conversationId, messageId);
                }
                case "typing" -> {
                    String conversationId = node.get("conversationId").asText();
                    chatService.broadcastTyping(userId, conversationId);
                }
                default -> log.warn("Unknown message type: {}", type);
            }
        } catch (Exception e) {
            log.error("Failed to handle business message: {}", e.getMessage());
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

    private void broadcastUserStatus(String userId, boolean online) {
        Map<String, Object> msg = Map.of(
                "type", online ? "user_online" : "user_offline",
                "userId", userId
        );
        sessionManager.sendToUsers(sessionManager.getOnlineUserIds(), msg);
    }
}
