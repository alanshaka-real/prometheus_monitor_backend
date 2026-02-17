package com.wenmin.prometheus.module.datasource.controller;

import com.wenmin.prometheus.common.result.R;
import com.wenmin.prometheus.module.datasource.dto.BatchCreateExporterDTO;
import com.wenmin.prometheus.module.datasource.dto.ExporterLinkMachineDTO;
import com.wenmin.prometheus.module.datasource.dto.ServiceDetectRequestDTO;
import com.wenmin.prometheus.module.datasource.entity.PromExporter;
import com.wenmin.prometheus.module.datasource.entity.PromInstance;
import com.wenmin.prometheus.module.datasource.service.DatasourceService;
import com.wenmin.prometheus.module.datasource.vo.*;
import com.wenmin.prometheus.module.distribute.dto.PrometheusConfigDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "数据源管理")
@RestController
@RequestMapping("/api/prometheus")
@RequiredArgsConstructor
public class DatasourceController {

    private final DatasourceService datasourceService;

    // ==================== Prometheus Instances ====================

    @Operation(summary = "获取Prometheus实例列表")
    @GetMapping("/instances")
    public R<Map<String, Object>> listInstances(
            @RequestParam(required = false) String group,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return R.ok(datasourceService.listInstances(group, status, page, pageSize));
    }

    @Operation(summary = "获取Prometheus实例详情")
    @GetMapping("/instances/{id}")
    public R<PromInstance> getInstance(@PathVariable String id) {
        return R.ok(datasourceService.getInstanceById(id));
    }

    @Operation(summary = "创建Prometheus实例")
    @PostMapping("/instances")
    public R<PromInstance> createInstance(@Valid @RequestBody PromInstance instance) {
        return R.ok(datasourceService.createInstance(instance));
    }

    @Operation(summary = "更新Prometheus实例")
    @PutMapping("/instances/{id}")
    public R<PromInstance> updateInstance(@PathVariable String id, @Valid @RequestBody PromInstance instance) {
        return R.ok(datasourceService.updateInstance(id, instance));
    }

    @Operation(summary = "删除Prometheus实例")
    @DeleteMapping("/instances/{id}")
    public R<Void> deleteInstance(@PathVariable String id) {
        datasourceService.deleteInstance(id);
        return R.ok();
    }

    @Operation(summary = "测试Prometheus连接")
    @PostMapping("/instances/{id}/test")
    public R<ConnectionTestVO> testConnection(@PathVariable String id) {
        return R.ok(datasourceService.testConnection(id));
    }

    // ==================== Instance Config Management ====================

    @Operation(summary = "获取实例配置")
    @GetMapping("/instances/{id}/config")
    public R<PrometheusConfigDTO> getInstanceConfig(@PathVariable String id) {
        return R.ok(datasourceService.getInstanceConfig(id));
    }

    @Operation(summary = "推送实例配置")
    @PostMapping("/instances/{id}/config/push")
    public R<Map<String, Object>> pushInstanceConfig(
            @PathVariable String id,
            @Valid @RequestBody PrometheusConfigDTO config) {
        return R.ok(datasourceService.pushInstanceConfig(id, config));
    }

    @Operation(summary = "上传YAML配置")
    @PostMapping("/instances/{id}/config/upload")
    public R<Map<String, Object>> uploadInstanceConfig(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        return R.ok(datasourceService.uploadInstanceConfig(id, body.get("yamlContent")));
    }

    @Operation(summary = "热加载Prometheus")
    @PostMapping("/instances/{id}/reload")
    public R<Map<String, Object>> reloadInstance(@PathVariable String id) {
        return R.ok(datasourceService.reloadInstance(id));
    }

    @Operation(summary = "关联机器")
    @PostMapping("/instances/{id}/link-machine")
    public R<Void> linkMachine(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        datasourceService.linkMachine(id, body.get("machineId"));
        return R.ok();
    }

    @Operation(summary = "解绑机器")
    @PostMapping("/instances/{id}/unlink-machine")
    public R<Void> unlinkMachine(@PathVariable String id) {
        datasourceService.linkMachine(id, null);
        return R.ok();
    }

    // ==================== Exporters ====================

    @Operation(summary = "获取Exporter列表")
    @GetMapping("/exporters")
    public R<Map<String, Object>> listExporters(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String instanceId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return R.ok(datasourceService.listExporters(type, instanceId, page, pageSize));
    }

    @Operation(summary = "创建Exporter")
    @PostMapping("/exporters")
    public R<PromExporter> createExporter(@Valid @RequestBody PromExporter exporter) {
        return R.ok(datasourceService.createExporter(exporter));
    }

    @Operation(summary = "更新Exporter")
    @PutMapping("/exporters/{id}")
    public R<PromExporter> updateExporter(@PathVariable String id, @Valid @RequestBody PromExporter exporter) {
        return R.ok(datasourceService.updateExporter(id, exporter));
    }

    @Operation(summary = "删除Exporter")
    @DeleteMapping("/exporters/{id}")
    public R<Void> deleteExporter(@PathVariable String id) {
        datasourceService.deleteExporter(id);
        return R.ok();
    }

    @Operation(summary = "启动Exporter")
    @PostMapping("/exporters/{id}/start")
    public R<Void> startExporter(@PathVariable String id) {
        datasourceService.startExporter(id);
        return R.ok();
    }

    @Operation(summary = "停止Exporter")
    @PostMapping("/exporters/{id}/stop")
    public R<Void> stopExporter(@PathVariable String id) {
        datasourceService.stopExporter(id);
        return R.ok();
    }

    @Operation(summary = "批量导入Exporter")
    @PostMapping("/exporters/batch")
    public R<BatchCreateResultVO> batchCreateExporters(@Valid @RequestBody BatchCreateExporterDTO dto) {
        return R.ok(datasourceService.batchCreateExporters(dto));
    }

    @Operation(summary = "自动探测服务")
    @PostMapping("/exporters/detect")
    public R<ServiceDetectResultVO> detectServices(@Valid @RequestBody ServiceDetectRequestDTO request) {
        return R.ok(datasourceService.detectServices(request));
    }

    // ==================== Exporter Machine & Remote Operations ====================

    @Operation(summary = "关联Exporter到机器")
    @PostMapping("/exporters/{id}/link-machine")
    public R<Void> linkExporterMachine(
            @PathVariable String id,
            @Valid @RequestBody ExporterLinkMachineDTO dto) {
        datasourceService.linkExporterMachine(id, dto);
        return R.ok();
    }

    @Operation(summary = "解除Exporter机器关联")
    @PostMapping("/exporters/{id}/unlink-machine")
    public R<Void> unlinkExporterMachine(@PathVariable String id) {
        datasourceService.unlinkExporterMachine(id);
        return R.ok();
    }

    @Operation(summary = "重启Exporter")
    @PostMapping("/exporters/{id}/restart")
    public R<Map<String, Object>> restartExporter(@PathVariable String id) {
        return R.ok(datasourceService.restartExporter(id));
    }

    @Operation(summary = "检查Exporter真实状态")
    @GetMapping("/exporters/{id}/status")
    public R<ExporterStatusVO> checkExporterStatus(@PathVariable String id) {
        return R.ok(datasourceService.checkExporterStatus(id));
    }

    @Operation(summary = "批量检查Exporter状态")
    @PostMapping("/exporters/batch-status")
    public R<List<ExporterStatusVO>> batchCheckExporterStatus(@RequestBody List<String> exporterIds) {
        return R.ok(datasourceService.batchCheckExporterStatus(exporterIds));
    }

    @Operation(summary = "获取Exporter配置")
    @GetMapping("/exporters/{id}/config")
    public R<ExporterConfigVO> getExporterConfig(@PathVariable String id) {
        return R.ok(datasourceService.getExporterConfig(id));
    }

    @Operation(summary = "更新Exporter配置")
    @PostMapping("/exporters/{id}/config")
    public R<Map<String, Object>> updateExporterConfig(
            @PathVariable String id,
            @RequestBody Map<String, Object> body) {
        String cliFlags = (String) body.get("cliFlags");
        String serviceFileContent = (String) body.get("serviceFileContent");
        boolean restartAfterUpdate = Boolean.TRUE.equals(body.get("restartAfterUpdate"));
        return R.ok(datasourceService.updateExporterConfig(id, cliFlags, serviceFileContent, restartAfterUpdate));
    }

    @Operation(summary = "获取Exporter日志")
    @GetMapping("/exporters/{id}/logs")
    public R<ExporterLogVO> getExporterLogs(
            @PathVariable String id,
            @RequestParam(required = false) Integer lines,
            @RequestParam(required = false) String since) {
        return R.ok(datasourceService.getExporterLogs(id, lines, since));
    }

    @Operation(summary = "获取Exporter操作记录")
    @GetMapping("/exporters/{id}/operations")
    public R<Map<String, Object>> listExporterOperationLogs(@PathVariable String id) {
        return R.ok(datasourceService.listExporterOperationLogs(id));
    }

    // ==================== Metric Metadata ====================

    @Operation(summary = "获取指标元数据列表")
    @GetMapping("/metrics/metadata")
    public R<Map<String, Object>> listMetricMeta(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String exporter) {
        return R.ok(datasourceService.listMetricMeta(name, type, exporter));
    }

    @Operation(summary = "切换指标收藏状态")
    @PostMapping("/metrics/metadata/{id}/favorite")
    public R<Void> toggleMetricFavorite(@PathVariable String id) {
        datasourceService.toggleMetricFavorite(id);
        return R.ok();
    }
}
