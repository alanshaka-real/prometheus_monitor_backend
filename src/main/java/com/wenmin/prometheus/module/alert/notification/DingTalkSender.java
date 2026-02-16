package com.wenmin.prometheus.module.alert.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DingTalkSender implements NotificationSender {

    private final RestTemplate restTemplate;

    @Override
    public String getType() {
        return "dingtalk";
    }

    @Override
    public boolean send(Map<String, String> config, String message) {
        try {
            String webhook = config.get("webhook");
            Map<String, Object> body = Map.of(
                    "msgtype", "text",
                    "text", Map.of("content", message)
            );
            restTemplate.postForObject(webhook, body, String.class);
            return true;
        } catch (Exception e) {
            log.error("DingTalk send failed", e);
            return false;
        }
    }
}
