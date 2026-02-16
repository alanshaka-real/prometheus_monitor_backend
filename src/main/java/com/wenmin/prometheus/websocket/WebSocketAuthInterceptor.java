package com.wenmin.prometheus.websocket;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.wenmin.prometheus.security.JwtTokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Slf4j
@Component
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

    public WebSocketAuthInterceptor(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            String token = servletRequest.getServletRequest().getParameter("token");
            String taskId = servletRequest.getServletRequest().getParameter("taskId");

            if (token == null || token.isBlank()) {
                log.warn("WebSocket handshake rejected: missing token");
                return false;
            }

            DecodedJWT jwt = jwtTokenProvider.verifyToken(token);
            if (jwt == null) {
                log.warn("WebSocket handshake rejected: invalid token");
                return false;
            }

            attributes.put("userId", jwtTokenProvider.getUserId(jwt));
            attributes.put("username", jwtTokenProvider.getUsername(jwt));
            if (taskId != null && !taskId.isBlank()) {
                attributes.put("taskId", taskId);
            }

            return true;
        }
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // No-op
    }
}
