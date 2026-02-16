package com.wenmin.prometheus.module.alert.controller;

import com.wenmin.prometheus.annotation.AuditLog;
import com.wenmin.prometheus.common.result.R;
import com.wenmin.prometheus.module.alert.entity.PromAlertRule;
import com.wenmin.prometheus.module.alert.entity.PromNotificationChannel;
import com.wenmin.prometheus.module.alert.entity.PromSilence;
import com.wenmin.prometheus.module.alert.service.AlertService;
import com.wenmin.prometheus.security.SecurityUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "告警管理")
@RestController
@RequestMapping("/api/prometheus")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    // ==================== Alert Rules ====================

    @Operation(summary = "获取告警规则列表")
    @GetMapping("/alert/rules")
    public R<Map<String, Object>> listAlertRules(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String severity) {
        return R.ok(alertService.listAlertRules(status, severity));
    }

    @Operation(summary = "创建告警规则")
    @AuditLog(action = "创建", resource = "告警规则")
    @PostMapping("/alert/rules")
    public R<PromAlertRule> createAlertRule(@RequestBody PromAlertRule rule) {
        return R.ok(alertService.createAlertRule(rule));
    }

    @Operation(summary = "更新告警规则")
    @AuditLog(action = "修改", resource = "告警规则")
    @PutMapping("/alert/rules/{id}")
    public R<PromAlertRule> updateAlertRule(@PathVariable String id,
                                            @RequestBody PromAlertRule rule) {
        return R.ok(alertService.updateAlertRule(id, rule));
    }

    @Operation(summary = "删除告警规则")
    @AuditLog(action = "删除", resource = "告警规则")
    @DeleteMapping("/alert/rules/{id}")
    public R<Void> deleteAlertRule(@PathVariable String id) {
        alertService.deleteAlertRule(id);
        return R.ok();
    }

    @Operation(summary = "启用/禁用告警规则")
    @AuditLog(action = "切换状态", resource = "告警规则")
    @PostMapping("/alert/rules/{id}/toggle")
    public R<Void> toggleAlertRule(@PathVariable String id,
                                    @RequestBody Map<String, String> body) {
        alertService.toggleAlertRule(id, body.get("status"));
        return R.ok();
    }

    // ==================== Alert History ====================

    @Operation(summary = "获取告警历史")
    @GetMapping("/alert/history")
    public R<Map<String, Object>> listAlertHistory(
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        return R.ok(alertService.listAlertHistory(severity, startTime, endTime));
    }

    @Operation(summary = "确认告警")
    @AuditLog(action = "处理", resource = "告警")
    @PostMapping("/alert/history/{id}/acknowledge")
    public R<Void> acknowledgeAlert(@PathVariable String id,
                                     @RequestBody Map<String, String> body,
                                     @AuthenticationPrincipal SecurityUser user) {
        String handledBy = (user != null) ? user.getUsername() : "";
        alertService.acknowledgeAlert(id, body.get("remark"), handledBy);
        return R.ok();
    }

    // ==================== Silences ====================

    @Operation(summary = "获取静默规则列表")
    @GetMapping("/alert/silences")
    public R<Map<String, Object>> listSilences() {
        return R.ok(alertService.listSilences());
    }

    @Operation(summary = "创建静默规则")
    @AuditLog(action = "创建", resource = "静默规则")
    @PostMapping("/alert/silences")
    public R<PromSilence> createSilence(@RequestBody PromSilence silence) {
        return R.ok(alertService.createSilence(silence));
    }

    @Operation(summary = "删除静默规则")
    @AuditLog(action = "删除", resource = "静默规则")
    @DeleteMapping("/alert/silences/{id}")
    public R<Void> deleteSilence(@PathVariable String id) {
        alertService.deleteSilence(id);
        return R.ok();
    }

    // ==================== Notification Channels ====================

    @Operation(summary = "获取通知渠道列表")
    @GetMapping("/alert/notifications")
    public R<Map<String, Object>> listNotificationChannels() {
        return R.ok(alertService.listNotificationChannels());
    }

    @Operation(summary = "创建通知渠道")
    @AuditLog(action = "创建", resource = "通知渠道")
    @PostMapping("/alert/notifications")
    public R<PromNotificationChannel> createNotificationChannel(
            @RequestBody PromNotificationChannel channel) {
        return R.ok(alertService.createNotificationChannel(channel));
    }

    @Operation(summary = "更新通知渠道")
    @AuditLog(action = "修改", resource = "通知渠道")
    @PutMapping("/alert/notifications/{id}")
    public R<PromNotificationChannel> updateNotificationChannel(
            @PathVariable String id,
            @RequestBody PromNotificationChannel channel) {
        return R.ok(alertService.updateNotificationChannel(id, channel));
    }

    @Operation(summary = "删除通知渠道")
    @AuditLog(action = "删除", resource = "通知渠道")
    @DeleteMapping("/alert/notifications/{id}")
    public R<Void> deleteNotificationChannel(@PathVariable String id) {
        alertService.deleteNotificationChannel(id);
        return R.ok();
    }

    @Operation(summary = "测试通知渠道")
    @AuditLog(action = "测试", resource = "通知渠道")
    @PostMapping("/alert/notifications/{id}/test")
    public R<Void> testNotificationChannel(@PathVariable String id) {
        alertService.testNotificationChannel(id);
        return R.ok();
    }
}
