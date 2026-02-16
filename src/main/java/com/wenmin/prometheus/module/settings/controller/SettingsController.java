package com.wenmin.prometheus.module.settings.controller;

import com.wenmin.prometheus.annotation.AuditLog;
import com.wenmin.prometheus.common.result.R;
import com.wenmin.prometheus.module.settings.service.SettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "系统设置")
@RestController
@RequestMapping("/api/prometheus/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final SettingsService settingsService;

    @Operation(summary = "获取全局配置")
    @GetMapping("/global")
    public R<Map<String, String>> getGlobalSettings() {
        return R.ok(settingsService.getGlobalSettings());
    }

    @Operation(summary = "更新全局配置")
    @AuditLog(action = "修改", resource = "全局配置")
    @PutMapping("/global")
    public R<Void> updateGlobalSettings(@RequestBody Map<String, String> settings) {
        settingsService.updateGlobalSettings(settings);
        return R.ok();
    }

    @Operation(summary = "获取系统日志")
    @GetMapping("/logs")
    public R<Object> listSystemLogs(@RequestParam(required = false) String level,
                                     @RequestParam(required = false) String startTime,
                                     @RequestParam(required = false) String endTime,
                                     @RequestParam(required = false) Integer page,
                                     @RequestParam(required = false) Integer pageSize) {
        return R.ok(settingsService.listSystemLogs(level, startTime, endTime, page, pageSize));
    }

    @Operation(summary = "导出系统日志")
    @GetMapping("/logs/export")
    public ResponseEntity<byte[]> exportSystemLogs(@RequestParam(required = false) String level,
                                                    @RequestParam(required = false) String startTime,
                                                    @RequestParam(required = false) String endTime) {
        byte[] data = settingsService.exportSystemLogs(level, startTime, endTime);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=system-logs.csv")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }

    @Operation(summary = "清除系统日志")
    @AuditLog(action = "删除", resource = "系统日志")
    @DeleteMapping("/logs")
    public R<Void> clearSystemLogs(@RequestParam(required = false) String before) {
        settingsService.clearSystemLogs(before);
        return R.ok();
    }
}
