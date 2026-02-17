package com.wenmin.prometheus.module.distribute.controller;

import com.wenmin.prometheus.annotation.AuditLog;
import com.wenmin.prometheus.common.result.R;
import com.wenmin.prometheus.module.distribute.dto.PrometheusConfigDTO;
import com.wenmin.prometheus.module.distribute.entity.PromDistributeMachine;
import com.wenmin.prometheus.module.distribute.entity.PromDistributeTask;
import com.wenmin.prometheus.module.distribute.entity.PromDistributeTaskDetail;
import com.wenmin.prometheus.module.distribute.service.DistributeService;
import com.wenmin.prometheus.module.distribute.service.SoftwareService;
import com.wenmin.prometheus.module.distribute.util.PrometheusYamlGenerator;
import com.wenmin.prometheus.module.distribute.vo.MachineDetectVO;
import com.wenmin.prometheus.module.distribute.vo.SoftwareDownloadVO;
import com.wenmin.prometheus.module.distribute.vo.SoftwareUploadVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Tag(name = "Prometheus 分发管理")
@RestController
@RequestMapping("/api/prometheus/distribute")
@RequiredArgsConstructor
public class DistributeController {

    private final DistributeService distributeService;
    private final SoftwareService softwareService;

    // ==================== Machine Management ====================

    @Operation(summary = "获取目标机器列表")
    @GetMapping("/machines")
    public R<Map<String, Object>> listMachines(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        return R.ok(distributeService.listMachines(status, keyword));
    }

    @Operation(summary = "新增目标机器")
    @PostMapping("/machines")
    @AuditLog(action = "创建", resource = "分发机器")
    public R<PromDistributeMachine> createMachine(@Valid @RequestBody PromDistributeMachine machine) {
        return R.ok(distributeService.createMachine(machine));
    }

    @Operation(summary = "更新目标机器")
    @PutMapping("/machines/{id}")
    @AuditLog(action = "修改", resource = "分发机器")
    public R<PromDistributeMachine> updateMachine(@PathVariable String id,
                                                   @Valid @RequestBody PromDistributeMachine machine) {
        return R.ok(distributeService.updateMachine(id, machine));
    }

    @Operation(summary = "删除目标机器")
    @DeleteMapping("/machines/{id}")
    @AuditLog(action = "删除", resource = "分发机器")
    public R<Void> deleteMachine(@PathVariable String id) {
        distributeService.deleteMachine(id);
        return R.ok();
    }

    @Operation(summary = "检测目标机器 OS/架构")
    @PostMapping("/machines/{id}/detect")
    public R<MachineDetectVO> detectMachine(@PathVariable String id) {
        return R.ok(distributeService.detectMachine(id));
    }

    @Operation(summary = "测试 SSH 连通性（通过机器ID，使用存储的凭据）")
    @PostMapping("/machines/{id}/test")
    public R<Map<String, Object>> testMachineConnection(@PathVariable String id) {
        return R.ok(distributeService.testMachineConnection(id));
    }

    @Operation(summary = "测试 SSH 连通性（直接传参）")
    @PostMapping("/machines/test")
    public R<Map<String, Object>> testSshConnection(@RequestBody Map<String, Object> params) {
        String ip = (String) params.get("ip");
        Integer port = params.get("port") != null ? ((Number) params.get("port")).intValue() : 22;
        String username = (String) params.get("username");
        String password = (String) params.get("password");

        boolean success = distributeService.testSshConnection(ip, port, username, password);
        return R.ok(Map.of("success", success, "message", success ? "连接成功" : "连接失败"));
    }

    @Operation(summary = "批量检测目标机器")
    @PostMapping("/machines/batch-detect")
    @SuppressWarnings("unchecked")
    public R<Map<String, Object>> batchDetect(@RequestBody Map<String, Object> params) {
        List<String> machineIds = (List<String>) params.get("machineIds");
        List<MachineDetectVO> results = distributeService.batchDetect(machineIds);
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("list", results);
        data.put("total", results.size());
        return R.ok(data);
    }

    // ==================== Task Management ====================

    @Operation(summary = "创建并启动分发任务")
    @PostMapping("/tasks")
    @AuditLog(action = "创建", resource = "分发任务")
    public R<PromDistributeTask> createTask(@RequestBody Map<String, Object> request) {
        return R.ok(distributeService.createTask(request));
    }

    @Operation(summary = "获取任务列表")
    @GetMapping("/tasks")
    public R<Map<String, Object>> listTasks(@RequestParam(required = false) String status) {
        return R.ok(distributeService.listTasks(status));
    }

    @Operation(summary = "获取任务详情")
    @GetMapping("/tasks/{id}")
    public R<PromDistributeTask> getTask(@PathVariable String id) {
        return R.ok(distributeService.getTask(id));
    }

    @Operation(summary = "获取任务明细列表")
    @GetMapping("/tasks/{id}/details")
    public R<List<PromDistributeTaskDetail>> getTaskDetails(@PathVariable String id) {
        return R.ok(distributeService.getTaskDetails(id));
    }

    @Operation(summary = "取消任务")
    @PostMapping("/tasks/{id}/cancel")
    @AuditLog(action = "取消", resource = "分发任务")
    public R<Void> cancelTask(@PathVariable String id) {
        distributeService.cancelTask(id);
        return R.ok();
    }

    @Operation(summary = "重试失败的任务明细")
    @PostMapping("/tasks/{id}/details/{did}/retry")
    @AuditLog(action = "重试", resource = "分发任务")
    public R<Void> retryTaskDetail(@PathVariable String id, @PathVariable String did) {
        distributeService.retryTaskDetail(id, did);
        return R.ok();
    }

    // ==================== Software Management ====================

    @Operation(summary = "获取软件包列表")
    @GetMapping("/software")
    public R<Map<String, Object>> listSoftware(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String osType,
            @RequestParam(required = false) String osArch) {
        return R.ok(softwareService.listSoftware(name, osType, osArch));
    }

    @Operation(summary = "扫描本地软件包目录")
    @PostMapping("/software/scan")
    @AuditLog(action = "扫描", resource = "分发软件包")
    public R<Map<String, Object>> scanSoftware() {
        int count = softwareService.scanDirectory();
        return R.ok(Map.of("registered", count, "message", "扫描完成，新注册 " + count + " 个软件包"));
    }

    @Operation(summary = "在线更新软件包（从 GitHub Releases 下载最新版本）")
    @PostMapping("/software/download")
    @AuditLog(action = "在线更新", resource = "分发软件包")
    @SuppressWarnings("unchecked")
    public R<Map<String, Object>> downloadSoftware(@RequestBody Map<String, Object> params) {
        List<String> components = (List<String>) params.get("components");
        if (components == null || components.isEmpty()) {
            components = List.of("prometheus", "node_exporter", "blackbox_exporter",
                    "process_exporter", "mysql_exporter", "redis_exporter",
                    "nginx_exporter", "cadvisor");
        }
        String downloadId = softwareService.downloadLatest(components);
        return R.ok(Map.of("downloadId", downloadId, "message", "下载任务已启动"));
    }

    @Operation(summary = "查询在线更新进度")
    @GetMapping("/software/download/{downloadId}")
    public R<SoftwareDownloadVO> getDownloadStatus(@PathVariable String downloadId) {
        return R.ok(softwareService.getDownloadStatus(downloadId));
    }

    @Operation(summary = "上传软件包")
    @PostMapping("/software/upload")
    @AuditLog(action = "上传", resource = "分发软件包")
    public R<SoftwareUploadVO> uploadSoftware(@RequestParam("file") MultipartFile file) {
        return R.ok(softwareService.uploadSoftware(file));
    }

    // ==================== Prometheus Config ====================

    @Operation(summary = "验证 Prometheus 配置并预览 YAML")
    @PostMapping("/prometheus/validate-config")
    public R<Map<String, Object>> validatePrometheusConfig(@Valid @RequestBody PrometheusConfigDTO config) {
        String installDir = config.getInstallDir() != null && !config.getInstallDir().isBlank()
                ? config.getInstallDir() : "~/prometheus";
        String yaml = PrometheusYamlGenerator.generate(config);
        String flags = PrometheusYamlGenerator.buildCliFlags(config, installDir);
        return R.ok(Map.of("valid", true, "yamlPreview", yaml, "flagsPreview", flags));
    }
}
