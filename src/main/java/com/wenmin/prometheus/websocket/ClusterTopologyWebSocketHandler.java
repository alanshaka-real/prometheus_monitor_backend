package com.wenmin.prometheus.websocket;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wenmin.prometheus.module.cluster.service.ClusterService;
import com.wenmin.prometheus.security.JwtTokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket handler for real-time cluster topology updates.
 * Clients subscribe to topology changes and receive updates
 * when cluster health status changes.
 */
@Slf4j
@Component
public class ClusterTopologyWebSocketHandler extends TextWebSocketHandler {

    private static final long AUTH_TIMEOUT_MS = 30_000;

    private final ObjectMapper objectMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final ClusterService clusterService;

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public ClusterTopologyWebSocketHandler(ObjectMapper objectMapper,
                                           JwtTokenProvider jwtTokenProvider,
                                           ClusterService clusterService) {
        this.objectMapper = objectMapper;
        this.jwtTokenProvider = jwtTokenProvider;
        this.clusterService = clusterService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.debug("Cluster topology WebSocket connected: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        JsonNode json = objectMapper.readTree(payload);

        String type = json.has("type") ? json.get("type").asText() : "";

        if ("auth".equals(type)) {
            handleAuth(session, json);
        } else if ("subscribe".equals(type)) {
            handleSubscribe(session, json);
        } else if ("ping".equals(type)) {
            session.sendMessage(new TextMessage("{\"type\":\"pong\"}"));
        }
    }

    private void handleAuth(WebSocketSession session, JsonNode json) throws IOException {
        String token = json.has("token") ? json.get("token").asText() : "";
        DecodedJWT jwt = jwtTokenProvider.verifyToken(token);

        if (jwt == null) {
            session.sendMessage(new TextMessage("{\"type\":\"auth_failed\",\"message\":\"Token无效\"}"));
            session.close(CloseStatus.NOT_ACCEPTABLE);
            return;
        }

        session.getAttributes().put("authenticated", true);
        session.getAttributes().put("userId", jwtTokenProvider.getUserId(jwt));
        session.getAttributes().put("username", jwtTokenProvider.getUsername(jwt));
        sessions.put(session.getId(), session);

        session.sendMessage(new TextMessage("{\"type\":\"auth_success\"}"));
        log.debug("Cluster topology WS authenticated: user={}", jwtTokenProvider.getUsername(jwt));

        // Send initial topology data
        sendTopologyUpdate(session);
    }

    private void handleSubscribe(WebSocketSession session, JsonNode json) throws IOException {
        if (!Boolean.TRUE.equals(session.getAttributes().get("authenticated"))) {
            session.sendMessage(new TextMessage("{\"type\":\"error\",\"message\":\"未认证\"}"));
            return;
        }
        // Re-send current topology data
        sendTopologyUpdate(session);
    }

    private void sendTopologyUpdate(WebSocketSession session) throws IOException {
        try {
            Map<String, Object> topology = clusterService.listClusters();
            String json = objectMapper.writeValueAsString(Map.of(
                    "type", "topology_update",
                    "data", topology
            ));
            session.sendMessage(new TextMessage(json));
        } catch (Exception e) {
            log.error("Failed to send topology update", e);
        }
    }

    /**
     * Broadcast topology updates to all authenticated sessions.
     * Called periodically or when cluster health changes.
     */
    @Scheduled(fixedRateString = "${cluster.topology.push-interval-ms:30000}")
    public void broadcastTopologyUpdates() {
        if (sessions.isEmpty()) return;

        try {
            Map<String, Object> topology = clusterService.listClusters();
            String json = objectMapper.writeValueAsString(Map.of(
                    "type", "topology_update",
                    "data", topology
            ));
            TextMessage message = new TextMessage(json);

            for (Map.Entry<String, WebSocketSession> entry : sessions.entrySet()) {
                WebSocketSession session = entry.getValue();
                if (session.isOpen() && Boolean.TRUE.equals(session.getAttributes().get("authenticated"))) {
                    try {
                        session.sendMessage(message);
                    } catch (IOException e) {
                        log.debug("Failed to send topology to session {}: {}", entry.getKey(), e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to broadcast topology updates", e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session.getId());
        log.debug("Cluster topology WS disconnected: {}", session.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        sessions.remove(session.getId());
        log.debug("Cluster topology WS transport error: {}", session.getId());
    }
}
