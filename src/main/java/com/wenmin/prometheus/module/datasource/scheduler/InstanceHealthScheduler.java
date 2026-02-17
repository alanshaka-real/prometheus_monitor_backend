package com.wenmin.prometheus.module.datasource.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wenmin.prometheus.module.datasource.entity.PromInstance;
import com.wenmin.prometheus.module.datasource.mapper.PromInstanceMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Prometheus 实例健康检查调度器。
 * 每隔固定时间对所有实例调用 GET /-/healthy 端点，
 * 更新实例状态为 online/offline。
 */
@Slf4j
@Component
public class InstanceHealthScheduler {

    private final PromInstanceMapper instanceMapper;
    private final RestTemplate restTemplate;
    private final ExecutorService checkExecutor;

    @Value("${instance.health-check.enabled:true}")
    private boolean enabled;

    public InstanceHealthScheduler(PromInstanceMapper instanceMapper, RestTemplate restTemplate) {
        this.instanceMapper = instanceMapper;
        this.restTemplate = restTemplate;
        this.checkExecutor = Executors.newFixedThreadPool(5);
    }

    @Scheduled(fixedRateString = "${instance.health-check.interval-ms:300000}")
    public void checkInstanceHealth() {
        if (!enabled) {
            return;
        }

        log.debug("开始 Prometheus 实例健康检查...");

        List<PromInstance> instances = instanceMapper.selectList(
                new LambdaQueryWrapper<PromInstance>());

        if (instances.isEmpty()) {
            log.debug("没有 Prometheus 实例，跳过健康检查");
            return;
        }

        log.info("开始检查 {} 个 Prometheus 实例的健康状态", instances.size());

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (PromInstance instance : instances) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                checkAndUpdateHealth(instance);
            }, checkExecutor);
            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        log.info("Prometheus 实例健康检查完成，共检查 {} 个", instances.size());
    }

    private void checkAndUpdateHealth(PromInstance instance) {
        String url = instance.getUrl();
        if (url == null || url.isBlank()) {
            return;
        }

        // Remove trailing slash
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }

        boolean healthy = false;
        try {
            restTemplate.getForEntity(url + "/-/healthy", String.class);
            healthy = true;
        } catch (Exception e) {
            log.debug("Prometheus 实例不可达: {} ({}), 原因: {}", instance.getName(), url, e.getMessage());
        }

        String newStatus = healthy ? "online" : "offline";
        if (!newStatus.equals(instance.getStatus())) {
            instance.setStatus(newStatus);
            instance.setUpdatedAt(LocalDateTime.now());
            instanceMapper.updateById(instance);
            log.info("Prometheus 实例状态更新: {} ({}) -> {}", instance.getName(), url, newStatus);
        }
    }
}
