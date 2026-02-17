package com.wenmin.prometheus.event;

import com.wenmin.prometheus.module.auth.entity.SysUser;
import com.wenmin.prometheus.module.auth.mapper.SysUserMapper;
import com.wenmin.prometheus.module.notification.entity.SysNotification;
import com.wenmin.prometheus.module.notification.mapper.SysNotificationMapper;
import com.wenmin.prometheus.module.notification.service.NotificationService;
import com.wenmin.prometheus.websocket.WebSocketSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class SystemEventListener {

    private final SysNotificationMapper notificationMapper;
    private final SysUserMapper userMapper;
    private final WebSocketSessionManager sessionManager;
    private final NotificationService notificationService;

    @Async
    @EventListener
    public void onSystemEvent(SystemEvent event) {
        try {
            List<String> targetUserIds = event.getTargetUserIds();
            if (targetUserIds == null) {
                // Send to all users
                List<SysUser> allUsers = userMapper.selectList(null);
                targetUserIds = allUsers.stream().map(SysUser::getId).toList();
            }

            for (String userId : targetUserIds) {
                SysNotification notification = new SysNotification();
                notification.setId(UUID.randomUUID().toString());
                notification.setUserId(userId);
                notification.setCategory(event.getCategory());
                notification.setType(event.getEventType());
                notification.setTitle(event.getTitle());
                notification.setContent(event.getContent());
                notification.setReferenceId(event.getReferenceId());
                notification.setReferenceType(event.getReferenceType());
                notification.setIsRead(0);
                notification.setIsHandled(0);
                notification.setCreatedAt(LocalDateTime.now());
                notificationMapper.insert(notification);

                // Push via WebSocket
                Map<String, Object> wsMsg = Map.of(
                        "type", "notification",
                        "data", Map.of(
                                "id", notification.getId(),
                                "category", notification.getCategory(),
                                "notificationType", notification.getType(),
                                "title", notification.getTitle(),
                                "content", notification.getContent() != null ? notification.getContent() : "",
                                "createdAt", notification.getCreatedAt().toString()
                        )
                );
                sessionManager.sendToUser(userId, wsMsg);
            }

            // Push updated counts to all affected users
            for (String userId : targetUserIds) {
                pushNotificationCount(userId);
            }

            log.info("SystemEvent processed: type={}, category={}, targets={}",
                    event.getEventType(), event.getCategory(), targetUserIds.size());
        } catch (Exception e) {
            log.error("Failed to process system event: {}", e.getMessage(), e);
        }
    }

    private void pushNotificationCount(String userId) {
        try {
            Map<String, Object> counts = notificationService.getCounts(userId);
            Map<String, Object> wsMsg = Map.of(
                    "type", "notification_count",
                    "data", counts
            );
            sessionManager.sendToUser(userId, wsMsg);
        } catch (Exception e) {
            log.warn("Failed to push notification count to user {}: {}", userId, e.getMessage());
        }
    }
}
