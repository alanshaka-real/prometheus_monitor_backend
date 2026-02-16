package com.wenmin.prometheus.module.alert.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookSender implements NotificationSender {

    private final RestTemplate restTemplate;

    @Override
    public String getType() {
        return "webhook";
    }

    @Override
    public boolean send(Map<String, String> config, String message) {
        try {
            String url = config.get("url");
            Map<String, Object> body = Map.of("message", message);
            restTemplate.postForObject(url, body, String.class);
            return true;
        } catch (Exception e) {
            log.error("Webhook send failed", e);
            return false;
        }
    }
}
