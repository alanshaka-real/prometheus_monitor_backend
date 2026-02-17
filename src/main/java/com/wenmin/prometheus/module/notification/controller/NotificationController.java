package com.wenmin.prometheus.module.notification.controller;

import com.wenmin.prometheus.common.result.R;
import com.wenmin.prometheus.module.notification.service.NotificationService;
import com.wenmin.prometheus.security.SecurityUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "通知管理")
@RestController
@RequestMapping("/api/prometheus/notification")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "获取未读数量")
    @GetMapping("/counts")
    public R<Map<String, Object>> getCounts(@AuthenticationPrincipal SecurityUser user) {
        return R.ok(notificationService.getCounts(user.getUserId()));
    }

    @Operation(summary = "系统通知列表")
    @GetMapping("/notices")
    public R<Map<String, Object>> listNotices(
            @AuthenticationPrincipal SecurityUser user,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return R.ok(notificationService.listNotices(user.getUserId(), page, pageSize));
    }

    @Operation(summary = "未读聊天摘要")
    @GetMapping("/messages")
    public R<Map<String, Object>> listMessages(@AuthenticationPrincipal SecurityUser user) {
        return R.ok(notificationService.listChatMessageSummaries(user.getUserId()));
    }

    @Operation(summary = "待办列表")
    @GetMapping("/pending")
    public R<Map<String, Object>> listPending(
            @AuthenticationPrincipal SecurityUser user,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return R.ok(notificationService.listPending(user.getUserId(), page, pageSize));
    }

    @Operation(summary = "标记已读")
    @PutMapping("/{id}/read")
    public R<Void> markRead(@AuthenticationPrincipal SecurityUser user,
                            @PathVariable String id) {
        notificationService.markRead(user.getUserId(), id);
        return R.ok();
    }

    @Operation(summary = "全部标记已读")
    @PutMapping("/read-all")
    public R<Void> markAllRead(@AuthenticationPrincipal SecurityUser user,
                               @RequestParam String category) {
        notificationService.markAllRead(user.getUserId(), category);
        return R.ok();
    }

    @Operation(summary = "处理待办")
    @PutMapping("/{id}/handle")
    public R<Void> handlePending(@AuthenticationPrincipal SecurityUser user,
                                 @PathVariable String id) {
        notificationService.handlePending(user.getUserId(), id);
        return R.ok();
    }
}
