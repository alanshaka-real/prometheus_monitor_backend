package com.wenmin.prometheus.module.datasource.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wenmin.prometheus.module.datasource.entity.PromExporter;
import com.wenmin.prometheus.module.datasource.mapper.PromExporterMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
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
 * Exporter 状态定期同步调度器。
 * 每隔固定时间检查所有处于 running 状态的 Exporter 是否可达，
 * 不可达时将状态更新为 stopped。
 */
@Slf4j
@Component
public class ExporterStatusScheduler {

    private final PromExporterMapper exporterMapper;
    private final RestTemplate statusCheckRestTemplate;
    private final ExecutorService checkExecutor;

    @Value("${exporter.status-check.enabled:true}")
    private boolean enabled;

    public ExporterStatusScheduler(
            PromExporterMapper exporterMapper,
            @Value("${exporter.status-check.connect-timeout-ms:3000}") int connectTimeoutMs,
            @Value("${exporter.status-check.read-timeout-ms:3000}") int readTimeoutMs) {
        this.exporterMapper = exporterMapper;

        // Build a dedicated RestTemplate with short timeouts for status checks
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(50);
        connectionManager.setDefaultMaxPerRoute(10);

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofMilliseconds(connectTimeoutMs))
                .setResponseTimeout(Timeout.ofMilliseconds(readTimeoutMs))
                .build();

        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .build();

        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
        this.statusCheckRestTemplate = new RestTemplate(factory);

        // Thread pool for parallel status checks
        this.checkExecutor = Executors.newFixedThreadPool(10);
    }

    @Scheduled(fixedRateString = "${exporter.status-check.interval-ms:300000}")
    public void syncExporterStatus() {
        if (!enabled) {
            return;
        }

        log.debug("开始 Exporter 状态同步...");

        // Query all running exporters
        LambdaQueryWrapper<PromExporter> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PromExporter::getStatus, "running");
        List<PromExporter> exporters = exporterMapper.selectList(wrapper);

        if (exporters.isEmpty()) {
            log.debug("没有 running 状态的 Exporter，跳过同步");
            return;
        }

        log.info("开始同步 {} 个 Exporter 的状态", exporters.size());

        // Check each exporter in parallel
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (PromExporter exporter : exporters) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                checkAndUpdateStatus(exporter);
            }, checkExecutor);
            futures.add(future);
        }

        // Wait for all checks to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        log.info("Exporter 状态同步完成，共检查 {} 个", exporters.size());
    }

    private void checkAndUpdateStatus(PromExporter exporter) {
        String host = exporter.getHost();
        Integer port = exporter.getPort();
        String metricsPath = exporter.getMetricsPath();
        if (metricsPath == null || metricsPath.isBlank()) {
            metricsPath = "/metrics";
        }
        if (!metricsPath.startsWith("/")) {
            metricsPath = "/" + metricsPath;
        }

        String url = "http://" + host + ":" + port + metricsPath;
        boolean reachable = false;

        try {
            statusCheckRestTemplate.getForEntity(url, String.class);
            reachable = true;
        } catch (Exception e) {
            log.debug("Exporter 不可达: {} ({}:{}), 原因: {}", exporter.getName(), host, port, e.getMessage());
        }

        // Update status if it changed from running to stopped
        if (!reachable) {
            exporter.setStatus("stopped");
            exporter.setLastStatusCheck(LocalDateTime.now());
            exporter.setUpdatedAt(LocalDateTime.now());
            exporterMapper.updateById(exporter);
            log.info("Exporter 状态更新为 stopped: {} ({}:{})", exporter.getName(), host, port);
        } else {
            // Update last check time even if still running
            exporter.setLastStatusCheck(LocalDateTime.now());
            exporter.setUpdatedAt(LocalDateTime.now());
            exporterMapper.updateById(exporter);
        }
    }
}
