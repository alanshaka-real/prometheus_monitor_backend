package com.wenmin.prometheus.module.datasource.service;

import com.wenmin.prometheus.module.datasource.dto.BatchCreateExporterDTO;
import com.wenmin.prometheus.module.datasource.dto.ExporterLinkMachineDTO;
import com.wenmin.prometheus.module.datasource.dto.ServiceDetectRequestDTO;
import com.wenmin.prometheus.module.datasource.entity.PromExporter;
import com.wenmin.prometheus.module.datasource.entity.PromInstance;
import com.wenmin.prometheus.module.datasource.vo.*;
import com.wenmin.prometheus.module.distribute.dto.PrometheusConfigDTO;

import java.util.List;
import java.util.Map;

public interface DatasourceService {

    // ---- Prometheus Instances ----

    Map<String, Object> listInstances(String group, String status, Integer page, Integer pageSize);

    PromInstance getInstanceById(String id);

    PromInstance createInstance(PromInstance instance);

    PromInstance updateInstance(String id, PromInstance instance);

    void deleteInstance(String id);

    ConnectionTestVO testConnection(String id);

    // ---- Instance Config Management ----

    /** 获取实例配置（优先从 DB 读取，其次 SSH 读取远程文件） */
    PrometheusConfigDTO getInstanceConfig(String instanceId);

    /** 推送配置到远程服务器（生成 YAML → SSH 写入 → 可选热加载） */
    Map<String, Object> pushInstanceConfig(String instanceId, PrometheusConfigDTO config);

    /** 上传 YAML 配置文件（解析 → SSH 写入 → 可选热加载） */
    Map<String, Object> uploadInstanceConfig(String instanceId, String yamlContent);

    /** 热加载 Prometheus（POST /-/reload） */
    Map<String, Object> reloadInstance(String instanceId);

    /** 关联/解绑机器 */
    void linkMachine(String instanceId, String machineId);

    // ---- Exporters ----

    Map<String, Object> listExporters(String type, String instanceId, Integer page, Integer pageSize);

    PromExporter createExporter(PromExporter exporter);

    PromExporter updateExporter(String id, PromExporter exporter);

    void deleteExporter(String id);

    void startExporter(String id);

    void stopExporter(String id);

    BatchCreateResultVO batchCreateExporters(BatchCreateExporterDTO dto);

    ServiceDetectResultVO detectServices(ServiceDetectRequestDTO request);

    // ---- Exporter 机器关联 ----

    void linkExporterMachine(String exporterId, ExporterLinkMachineDTO dto);

    void unlinkExporterMachine(String exporterId);

    // ---- Exporter 远程操作 ----

    Map<String, Object> startExporterRemote(String exporterId);

    Map<String, Object> stopExporterRemote(String exporterId);

    Map<String, Object> restartExporter(String exporterId);

    ExporterStatusVO checkExporterStatus(String exporterId);

    List<ExporterStatusVO> batchCheckExporterStatus(List<String> exporterIds);

    // ---- 配置管理 ----

    ExporterConfigVO getExporterConfig(String exporterId);

    Map<String, Object> updateExporterConfig(String exporterId, String cliFlags,
                                              String serviceFileContent, boolean restartAfterUpdate);

    // ---- 日志 ----

    ExporterLogVO getExporterLogs(String exporterId, Integer lines, String since);

    // ---- 操作记录 ----

    Map<String, Object> listExporterOperationLogs(String exporterId);

    // ---- Metric Metadata ----

    Map<String, Object> listMetricMeta(String name, String type, String exporter);

    void toggleMetricFavorite(String id);
}
