package com.wenmin.prometheus.module.datasource.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wenmin.prometheus.module.datasource.entity.PromInstance;
import com.wenmin.prometheus.module.datasource.entity.PromMetricMeta;
import com.wenmin.prometheus.module.datasource.mapper.PromInstanceMapper;
import com.wenmin.prometheus.module.datasource.mapper.PromMetricMetaMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Prometheus 指标元数据自动同步调度器。
 * 定期从所有在线 Prometheus 实例拉取 /api/v1/metadata，
 * 解析后将指标元数据 upsert 到 prom_metric_meta 表中。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MetadataScheduler {

    private final PromInstanceMapper promInstanceMapper;
    private final PromMetricMetaMapper promMetricMetaMapper;
    private final RestTemplate restTemplate;

    private final ExecutorService syncExecutor = Executors.newFixedThreadPool(5);

    @Value("${metadata.sync.enabled:true}")
    private boolean enabled;

    /**
     * 定时同步 Prometheus 指标元数据。
     * 默认每 30 分钟执行一次，可通过 metadata.sync.interval-ms 配置。
     */
    @Scheduled(fixedRateString = "${metadata.sync.interval-ms:1800000}")
    public void syncMetadata() {
        if (!enabled) {
            log.debug("指标元数据同步已禁用，跳过");
            return;
        }

        log.info("开始同步 Prometheus 指标元数据...");

        // 查询所有在线（online）的 Prometheus 实例
        LambdaQueryWrapper<PromInstance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PromInstance::getStatus, "online");
        List<PromInstance> instances = promInstanceMapper.selectList(wrapper);

        if (instances.isEmpty()) {
            log.info("没有在线的 Prometheus 实例，跳过元数据同步");
            return;
        }

        log.info("发现 {} 个在线 Prometheus 实例，开始拉取元数据", instances.size());

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // 并行拉取每个实例的元数据
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (PromInstance instance : instances) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    int upserted = syncInstanceMetadata(instance);
                    successCount.addAndGet(upserted);
                    log.info("实例 [{}] 元数据同步完成，upsert {} 条记录", instance.getName(), upserted);
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    log.error("实例 [{}]({}) 元数据同步失败: {}", instance.getName(), instance.getUrl(), e.getMessage(), e);
                }
            }, syncExecutor);
            futures.add(future);
        }

        // 等待所有任务完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        log.info("指标元数据同步完成: 共 {} 个实例, upsert {} 条记录, {} 个实例失败",
                instances.size(), successCount.get(), failCount.get());
    }

    /**
     * 从单个 Prometheus 实例拉取并同步指标元数据。
     *
     * @param instance Prometheus 实例
     * @return upsert 的记录数
     */
    private int syncInstanceMetadata(PromInstance instance) {
        String metadataUrl = buildMetadataUrl(instance.getUrl());

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                metadataUrl,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        Map<String, Object> body = response.getBody();
        if (body == null) {
            log.warn("实例 [{}] 返回空响应", instance.getName());
            return 0;
        }

        String status = (String) body.get("status");
        if (!"success".equals(status)) {
            log.warn("实例 [{}] 返回非成功状态: {}", instance.getName(), status);
            return 0;
        }

        // Prometheus /api/v1/metadata 返回格式:
        // { "status": "success", "data": { "metric_name": [ { "type": "...", "help": "...", "unit": "..." } ] } }
        @SuppressWarnings("unchecked")
        Map<String, List<Map<String, String>>> data = (Map<String, List<Map<String, String>>>) body.get("data");
        if (data == null || data.isEmpty()) {
            log.warn("实例 [{}] 返回空元数据", instance.getName());
            return 0;
        }

        int upsertCount = 0;
        for (Map.Entry<String, List<Map<String, String>>> entry : data.entrySet()) {
            String metricName = entry.getKey();
            List<Map<String, String>> metaList = entry.getValue();

            if (metaList == null || metaList.isEmpty()) {
                continue;
            }

            // 取第一条元数据（同名指标可能来自多个 target，元数据通常一致）
            Map<String, String> meta = metaList.get(0);
            String type = meta.getOrDefault("type", "");
            String help = meta.getOrDefault("help", "");
            String unit = meta.getOrDefault("unit", "");

            try {
                upsertMetricMeta(metricName, type, help, unit);
                upsertCount++;
            } catch (Exception e) {
                log.warn("upsert 指标 [{}] 元数据失败: {}", metricName, e.getMessage());
            }
        }

        return upsertCount;
    }

    /**
     * Upsert 指标元数据记录。
     * 如果同名指标已存在则更新 type、help、unit 字段；否则新增。
     */
    private synchronized void upsertMetricMeta(String metricName, String type, String help, String unit) {
        LambdaQueryWrapper<PromMetricMeta> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PromMetricMeta::getName, metricName);
        PromMetricMeta existing = promMetricMetaMapper.selectOne(wrapper);

        if (existing != null) {
            // 仅在有变更时更新
            boolean changed = false;
            if (type != null && !type.isEmpty() && !type.equals(existing.getType())) {
                existing.setType(type);
                changed = true;
            }
            if (help != null && !help.isEmpty() && !help.equals(existing.getHelp())) {
                existing.setHelp(help);
                changed = true;
            }
            if (unit != null && !unit.isEmpty() && !unit.equals(existing.getUnit())) {
                existing.setUnit(unit);
                changed = true;
            }
            if (changed) {
                promMetricMetaMapper.updateById(existing);
            }
        } else {
            // 新增记录
            PromMetricMeta meta = new PromMetricMeta();
            meta.setName(metricName);
            meta.setType(type);
            meta.setHelp(help);
            meta.setUnit(unit != null && !unit.isEmpty() ? unit : null);
            meta.setFavorite(false);
            meta.setCreatedAt(LocalDateTime.now());
            promMetricMetaMapper.insert(meta);
        }
    }

    /**
     * 构建 Prometheus metadata API 的完整 URL。
     * 确保 URL 末尾正确拼接 /api/v1/metadata 路径。
     */
    private String buildMetadataUrl(String prometheusUrl) {
        String baseUrl = prometheusUrl.endsWith("/")
                ? prometheusUrl.substring(0, prometheusUrl.length() - 1)
                : prometheusUrl;
        return baseUrl + "/api/v1/metadata";
    }
}
