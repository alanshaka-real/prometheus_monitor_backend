package com.wenmin.prometheus.config;

import com.wenmin.prometheus.websocket.DistributeWebSocketHandler;
import com.wenmin.prometheus.websocket.WebSocketAuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final DistributeWebSocketHandler distributeWebSocketHandler;
    private final WebSocketAuthInterceptor webSocketAuthInterceptor;

    public WebSocketConfig(DistributeWebSocketHandler distributeWebSocketHandler,
                           WebSocketAuthInterceptor webSocketAuthInterceptor) {
        this.distributeWebSocketHandler = distributeWebSocketHandler;
        this.webSocketAuthInterceptor = webSocketAuthInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(distributeWebSocketHandler, "/ws/distribute")
                .addInterceptors(webSocketAuthInterceptor)
                .setAllowedOrigins("*");
    }
}
