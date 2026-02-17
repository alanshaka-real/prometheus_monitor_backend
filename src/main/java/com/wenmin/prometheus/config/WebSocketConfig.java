package com.wenmin.prometheus.config;

import com.wenmin.prometheus.websocket.ClusterTopologyWebSocketHandler;
import com.wenmin.prometheus.websocket.DistributeWebSocketHandler;
import com.wenmin.prometheus.websocket.MessageWebSocketHandler;
import com.wenmin.prometheus.websocket.WebSocketAuthInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final DistributeWebSocketHandler distributeWebSocketHandler;
    private final MessageWebSocketHandler messageWebSocketHandler;
    private final ClusterTopologyWebSocketHandler clusterTopologyWebSocketHandler;
    private final WebSocketAuthInterceptor webSocketAuthInterceptor;

    @Value("${cors.allowed-origins:http://localhost:3006}")
    private String allowedOrigins;

    public WebSocketConfig(DistributeWebSocketHandler distributeWebSocketHandler,
                           MessageWebSocketHandler messageWebSocketHandler,
                           ClusterTopologyWebSocketHandler clusterTopologyWebSocketHandler,
                           WebSocketAuthInterceptor webSocketAuthInterceptor) {
        this.distributeWebSocketHandler = distributeWebSocketHandler;
        this.messageWebSocketHandler = messageWebSocketHandler;
        this.clusterTopologyWebSocketHandler = clusterTopologyWebSocketHandler;
        this.webSocketAuthInterceptor = webSocketAuthInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        String[] origins = allowedOrigins.split(",");

        registry.addHandler(distributeWebSocketHandler, "/ws/distribute")
                .addInterceptors(webSocketAuthInterceptor)
                .setAllowedOriginPatterns(origins);

        registry.addHandler(messageWebSocketHandler, "/ws/message")
                .addInterceptors(webSocketAuthInterceptor)
                .setAllowedOriginPatterns(origins);

        registry.addHandler(clusterTopologyWebSocketHandler, "/ws/cluster-topology")
                .addInterceptors(webSocketAuthInterceptor)
                .setAllowedOriginPatterns(origins);
    }
}
