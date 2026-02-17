package com.wenmin.prometheus.module.datasource.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wenmin.prometheus.common.exception.BusinessException;
import com.wenmin.prometheus.module.datasource.dto.BatchCreateExporterDTO;
import com.wenmin.prometheus.module.datasource.dto.ExporterLinkMachineDTO;
import com.wenmin.prometheus.module.datasource.dto.ServiceDetectRequestDTO;
import com.wenmin.prometheus.module.datasource.entity.PromExporter;
import com.wenmin.prometheus.module.datasource.entity.PromExporterOperationLog;
import com.wenmin.prometheus.module.datasource.entity.PromInstance;
import com.wenmin.prometheus.module.datasource.entity.PromMetricMeta;
import com.wenmin.prometheus.module.datasource.mapper.PromExporterMapper;
import com.wenmin.prometheus.module.datasource.mapper.PromExporterOperationLogMapper;
import com.wenmin.prometheus.module.datasource.mapper.PromInstanceMapper;
import com.wenmin.prometheus.module.datasource.mapper.PromMetricMetaMapper;
import com.wenmin.prometheus.module.datasource.service.DatasourceService;
import com.wenmin.prometheus.module.datasource.vo.*;
import com.wenmin.prometheus.module.distribute.dto.PrometheusConfigDTO;
import com.wenmin.prometheus.module.distribute.entity.PromDistributeMachine;
import com.wenmin.prometheus.module.distribute.mapper.PromDistributeMachineMapper;
import com.wenmin.prometheus.module.distribute.util.PrometheusYamlGenerator;
import com.wenmin.prometheus.util.EncryptionUtil;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.IOUtils;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DatasourceServiceImpl implements DatasourceService {

    private final PromInstanceMapper instanceMapper;
    private final PromExporterMapper exporterMapper;
    private final PromMetricMetaMapper metricMetaMapper;
    private final PromExporterOperationLogMapper operationLogMapper;
    private final RestTemplate restTemplate;
    private final PromDistributeMachineMapper machineMapper;
    private final ObjectMapper objectMapper;

    @Resource(name = "serviceDetectExecutor")
    private Executor serviceDetectExecutor;

    @Value("${distribute.encryption-key}")
    private String encryptionKey;

    @Value("${distribute.ssh.strict-host-key-checking:false}")
    private boolean strictHostKeyChecking;

    // 服务标识 → 默认端口
    private static final Map<String, Integer> DETECT_DEFAULT_PORTS = Map.of(
            "node_exporter", 9100, "blackbox_exporter", 9115,
            "process_exporter", 9256, "cadvisor", 8080,
            "mysql_exporter", 9104, "redis_exporter", 9121,
            "nginx_exporter", 9113, "prometheus", 9090
    );

    // 服务标识 → PromExporter.type
    private static final Map<String, String> SERVICE_TO_EXPORTER_TYPE = Map.of(
            "node_exporter", "node", "blackbox_exporter", "blackbox",
            "process_exporter", "process", "cadvisor", "cadvisor",
            "mysql_exporter", "mysqld", "redis_exporter", "redis",
            "nginx_exporter", "nginx", "prometheus", "custom"
    );

    // ==================== Prometheus Instances ====================

    @Override
    public Map<String, Object> listInstances(String group, String status, Integer page, Integer pageSize) {
        LambdaQueryWrapper<PromInstance> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(group)) {
            wrapper.eq(PromInstance::getGroupName, group);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(PromInstance::getStatus, status);
        }
        wrapper.orderByDesc(PromInstance::getCreatedAt);

        Page<PromInstance> pageObj = new Page<>(page, pageSize);
        IPage<PromInstance> pageResult = instanceMapper.selectPage(pageObj, wrapper);
        Map<String, Object> result = new HashMap<>();
        result.put("list", pageResult.getRecords());
        result.put("total", pageResult.getTotal());
        return result;
    }

    @Override
    public PromInstance getInstanceById(String id) {
        PromInstance instance = instanceMapper.selectById(id);
        if (instance == null) {
            throw new BusinessException("Prometheus实例不存在");
        }
        return instance;
    }

    @Override
    public PromInstance createInstance(PromInstance instance) {
        instance.setId(null);
        instance.setCreatedAt(LocalDateTime.now());
        instance.setUpdatedAt(LocalDateTime.now());
        if (instance.getStatus() == null) {
            instance.setStatus("unknown");
        }
        instanceMapper.insert(instance);
        return instance;
    }

    @Override
    public PromInstance updateInstance(String id, PromInstance instance) {
        PromInstance existing = instanceMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("Prometheus实例不存在");
        }
        instance.setId(id);
        instance.setUpdatedAt(LocalDateTime.now());
        instanceMapper.updateById(instance);
        return instanceMapper.selectById(id);
    }

    @Override
    @Transactional
    public void deleteInstance(String id) {
        PromInstance existing = instanceMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("Prometheus实例不存在");
        }
        // 级联软删除关联 Exporter
        LambdaQueryWrapper<PromExporter> exporterWrapper = new LambdaQueryWrapper<>();
        exporterWrapper.eq(PromExporter::getInstanceId, id);
        List<PromExporter> exporters = exporterMapper.selectList(exporterWrapper);
        for (PromExporter exporter : exporters) {
            exporterMapper.deleteById(exporter.getId());
        }
        // 删除实例
        instanceMapper.deleteById(id);
    }

    @Override
    @SuppressWarnings("unchecked")
    public ConnectionTestVO testConnection(String id) {
        PromInstance instance = instanceMapper.selectById(id);
        if (instance == null) {
            throw new BusinessException("Prometheus实例不存在");
        }

        ConnectionTestVO vo = new ConnectionTestVO();
        long start = System.currentTimeMillis();

        try {
            String url = instance.getUrl() + "/api/v1/status/buildinfo";
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            long latency = System.currentTimeMillis() - start;

            vo.setSuccess(true);
            vo.setLatency(latency);

            Map<String, Object> body = response.getBody();
            if (body != null && body.containsKey("data")) {
                Map<String, Object> data = (Map<String, Object>) body.get("data");
                if (data != null && data.containsKey("version")) {
                    vo.setVersion((String) data.get("version"));
                }
            }

            // Update instance status and version
            instance.setStatus("online");
            if (vo.getVersion() != null) {
                instance.setVersion(vo.getVersion());
            }
            instance.setUpdatedAt(LocalDateTime.now());
            instanceMapper.updateById(instance);

        } catch (Exception e) {
            long latency = System.currentTimeMillis() - start;
            log.warn("Prometheus连接测试失败: {}", e.getMessage());

            vo.setSuccess(false);
            vo.setLatency(latency);
            vo.setMessage(e.getMessage());

            // Update instance status to offline
            instance.setStatus("offline");
            instance.setUpdatedAt(LocalDateTime.now());
            instanceMapper.updateById(instance);
        }

        return vo;
    }

    // ==================== Instance Config Management ====================

    @Override
    public PrometheusConfigDTO getInstanceConfig(String instanceId) {
        PromInstance instance = instanceMapper.selectById(instanceId);
        if (instance == null) {
            throw new BusinessException("Prometheus实例不存在");
        }

        // 1. Try reading from DB configJson
        if (instance.getConfigJson() != null && !instance.getConfigJson().isEmpty()) {
            try {
                return objectMapper.convertValue(instance.getConfigJson(), PrometheusConfigDTO.class);
            } catch (Exception e) {
                log.warn("Failed to deserialize configJson for instance {}: {}", instanceId, e.getMessage());
            }
        }

        // 2. If machineId is set, try reading remote prometheus.yml via SSH
        if (StringUtils.hasText(instance.getMachineId())) {
            SSHClient ssh = null;
            try {
                ssh = connectToMachine(instance.getMachineId());

                // Try to find prometheus.yml path
                String yamlContent = "";
                String user = machineMapper.selectById(instance.getMachineId()).getSshUsername();

                // Try common paths
                String findCmd = String.format(
                    "cat /home/%s/prometheus*/prometheus.yml 2>/dev/null || " +
                    "cat /opt/prometheus*/prometheus.yml 2>/dev/null || " +
                    "cat /usr/local/prometheus*/prometheus.yml 2>/dev/null",
                    user);
                yamlContent = execCmd(ssh, findCmd).trim();

                if (!yamlContent.isEmpty()) {
                    PrometheusConfigDTO dto = new PrometheusConfigDTO();
                    dto.setRawYaml(yamlContent);
                    return dto;
                }
            } catch (Exception e) {
                log.warn("Failed to read remote prometheus.yml for instance {}: {}", instanceId, e.getMessage());
            } finally {
                if (ssh != null) {
                    try { ssh.close(); } catch (Exception ignored) {}
                }
            }
        }

        // 3. Return default config
        PrometheusConfigDTO defaultConfig = new PrometheusConfigDTO();
        defaultConfig.setGlobal(new PrometheusConfigDTO.GlobalConfig());
        defaultConfig.setCliFlags(new PrometheusConfigDTO.CliFlags());
        defaultConfig.setScrapeConfigs(new ArrayList<>());
        return defaultConfig;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> pushInstanceConfig(String instanceId, PrometheusConfigDTO config) {
        PromInstance instance = instanceMapper.selectById(instanceId);
        if (instance == null) {
            throw new BusinessException("Prometheus实例不存在");
        }
        if (!StringUtils.hasText(instance.getMachineId())) {
            throw new BusinessException("该实例未关联机器，无法推送配置");
        }

        Map<String, Object> result = new HashMap<>();
        SSHClient ssh = null;

        try {
            ssh = connectToMachine(instance.getMachineId());

            // Clear rawYaml if structured config is present, so generator uses structured fields
            if (config.getScrapeConfigs() != null && !config.getScrapeConfigs().isEmpty()) {
                config.setRawYaml(null);
            }

            // Filter out empty targets from scrape configs
            if (config.getScrapeConfigs() != null) {
                for (PrometheusConfigDTO.ScrapeConfig sc : config.getScrapeConfigs()) {
                    if (sc.getStaticConfigs() != null) {
                        for (PrometheusConfigDTO.StaticConfig stc : sc.getStaticConfigs()) {
                            if (stc.getTargets() != null) {
                                stc.getTargets().removeIf(t -> t == null || t.isBlank());
                            }
                        }
                    }
                }
            }

            // Generate YAML
            String yamlContent = PrometheusYamlGenerator.generate(config);

            // Determine install dir
            String installDir = config.getInstallDir();
            if (!StringUtils.hasText(installDir)) {
                // Try to detect install path
                String detectPath = execCmd(ssh, "dirname $(which prometheus 2>/dev/null) 2>/dev/null").trim();
                if (detectPath.isEmpty()) {
                    PromDistributeMachine machine = machineMapper.selectById(instance.getMachineId());
                    detectPath = execCmd(ssh, String.format(
                        "find /home/%s -maxdepth 3 -name prometheus -type f 2>/dev/null | head -1 | xargs dirname 2>/dev/null",
                        machine.getSshUsername())).trim();
                }
                if (detectPath.isEmpty()) {
                    detectPath = "/opt/prometheus";
                }
                installDir = detectPath;
            }

            // Write prometheus.yml via base64 + cp pattern
            String configPath = installDir + "/prometheus.yml";
            writeRemoteFile(ssh, configPath, yamlContent);

            // Optional: validate with promtool
            String promtoolPath = installDir + "/promtool";
            String promtoolCheck = execCmd(ssh, "[ -f \"" + promtoolPath + "\" ] && \"" + promtoolPath + "\" check config \"" + configPath + "\" 2>&1 || echo 'promtool not found, skipping validation'").trim();
            log.info("promtool check result: {}", promtoolCheck);

            // Save config to DB (always clear rawYaml to prevent stale override on next push)
            config.setRawYaml(null);
            Map<String, Object> configMap = objectMapper.convertValue(config, Map.class);
            instance.setConfigJson(configMap);
            instance.setUpdatedAt(LocalDateTime.now());
            instanceMapper.updateById(instance);

            result.put("success", true);
            result.put("message", "配置已成功写入 " + configPath);

            // Attempt hot reload if lifecycle is enabled
            if (Boolean.TRUE.equals(instance.getLifecycleEnabled())) {
                try {
                    Map<String, Object> reloadResult = reloadInstance(instanceId);
                    result.put("reloaded", Boolean.TRUE.equals(reloadResult.get("success")));
                    result.put("reloadMessage", reloadResult.get("message"));
                } catch (Exception e) {
                    result.put("reloaded", false);
                    result.put("reloadMessage", "热加载失败: " + e.getMessage());
                }
            } else {
                result.put("reloaded", false);
                result.put("reloadMessage", "未启用 --web.enable-lifecycle，请手动重启 Prometheus 使配置生效");
            }

        } catch (Exception e) {
            log.error("Push config failed for instance {}: {}", instanceId, e.getMessage(), e);
            result.put("success", false);
            result.put("message", "推送配置失败: " + e.getMessage());
            result.put("reloaded", false);
        } finally {
            if (ssh != null) {
                try { ssh.close(); } catch (Exception ignored) {}
            }
        }

        return result;
    }

    @Override
    public Map<String, Object> uploadInstanceConfig(String instanceId, String yamlContent) {
        if (!StringUtils.hasText(yamlContent)) {
            throw new BusinessException("YAML 内容不能为空");
        }
        PrometheusConfigDTO dto = new PrometheusConfigDTO();
        dto.setRawYaml(yamlContent);
        return pushInstanceConfig(instanceId, dto);
    }

    @Override
    public Map<String, Object> reloadInstance(String instanceId) {
        PromInstance instance = instanceMapper.selectById(instanceId);
        if (instance == null) {
            throw new BusinessException("Prometheus实例不存在");
        }

        Map<String, Object> result = new HashMap<>();
        try {
            String url = instance.getUrl() + "/-/reload";
            restTemplate.postForEntity(url, null, String.class);
            result.put("success", true);
            result.put("message", "Prometheus 热加载成功");
        } catch (Exception e) {
            log.warn("Prometheus reload failed for {}: {}", instance.getUrl(), e.getMessage());
            result.put("success", false);
            result.put("message", "热加载失败: " + e.getMessage() + "。可能未启用 --web.enable-lifecycle 启动参数");
        }
        return result;
    }

    @Override
    public void linkMachine(String instanceId, String machineId) {
        PromInstance instance = instanceMapper.selectById(instanceId);
        if (instance == null) {
            throw new BusinessException("Prometheus实例不存在");
        }

        if (StringUtils.hasText(machineId)) {
            PromDistributeMachine machine = machineMapper.selectById(machineId);
            if (machine == null) {
                throw new BusinessException("目标机器不存在");
            }

            instance.setMachineId(machineId);

            // Try to detect lifecycle flag via SSH
            SSHClient ssh = null;
            try {
                ssh = connectToMachine(machineId);
                String lifecycleCheck = execCmd(ssh,
                    "cat /proc/$(pgrep -x prometheus 2>/dev/null)/cmdline 2>/dev/null | tr '\\0' '\\n' | grep lifecycle || echo ''").trim();
                instance.setLifecycleEnabled(!lifecycleCheck.isEmpty());
            } catch (Exception e) {
                log.warn("Failed to detect lifecycle flag for machine {}: {}", machineId, e.getMessage());
                instance.setLifecycleEnabled(false);
            } finally {
                if (ssh != null) {
                    try { ssh.close(); } catch (Exception ignored) {}
                }
            }
        } else {
            // Unlink
            instance.setMachineId(null);
            instance.setLifecycleEnabled(false);
        }

        instance.setUpdatedAt(LocalDateTime.now());
        instanceMapper.updateById(instance);
    }

    // ==================== SSH Helper Methods ====================

    private SSHClient connectToMachine(String machineId) throws Exception {
        PromDistributeMachine machine = machineMapper.selectById(machineId);
        if (machine == null) {
            throw new BusinessException("目标机器不存在");
        }
        String password = decryptPassword(machine.getSshPassword());
        SSHClient ssh = new SSHClient();
        if (!strictHostKeyChecking) {
            log.warn("SSH strict host key checking is DISABLED for {}. Enable via distribute.ssh.strict-host-key-checking=true in production.", machine.getIp());
            ssh.addHostKeyVerifier(new PromiscuousVerifier());
        } else {
            ssh.loadKnownHosts();
        }
        ssh.setConnectTimeout(10000);
        ssh.connect(machine.getIp(), machine.getSshPort());
        ssh.authPassword(machine.getSshUsername(), password);
        return ssh;
    }

    private String execCmd(SSHClient ssh, String command) throws Exception {
        try (Session session = ssh.startSession()) {
            Session.Command cmd = session.exec(command);
            cmd.join(60, TimeUnit.SECONDS);
            return IOUtils.readFully(cmd.getInputStream()).toString();
        }
    }

    /**
     * Execute a command that requires sudo, feeding the password via stdin
     * instead of embedding it in the command string. This prevents command injection.
     */
    private String execCmdWithStdinPassword(SSHClient ssh, String command, String password) throws Exception {
        try (Session session = ssh.startSession()) {
            Session.Command cmd = session.exec(command);

            // Feed password via stdin for sudo -S
            OutputStream stdin = cmd.getOutputStream();
            stdin.write((password + "\n").getBytes(StandardCharsets.UTF_8));
            stdin.flush();
            stdin.close();

            cmd.join(60, TimeUnit.SECONDS);

            String stdout = IOUtils.readFully(cmd.getInputStream()).toString();
            String stderr = IOUtils.readFully(cmd.getErrorStream()).toString();

            Integer exitCode = cmd.getExitStatus();
            if (exitCode == null) {
                throw new RuntimeException("命令执行超时");
            }
            if (exitCode != 0) {
                String cleanStderr = stderr.replaceAll("\\[sudo\\] password for \\w+:", "").trim();
                String errorMsg = cleanStderr.isEmpty() ? stdout.trim() : cleanStderr;
                if (errorMsg.isEmpty()) {
                    errorMsg = "exit code " + exitCode;
                }
                throw new RuntimeException("命令执行失败: " + errorMsg);
            }
            return stdout;
        }
    }

    private String decryptPassword(String encrypted) {
        if (encrypted == null || encrypted.isBlank()) return "";
        try {
            return EncryptionUtil.decrypt(encrypted, encryptionKey);
        } catch (Exception e) {
            log.error("Password decryption failed", e);
            throw new BusinessException("SSH 密码解密失败");
        }
    }

    private void writeRemoteFile(SSHClient ssh, String path, String content) throws Exception {
        String base64Content = Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8));
        String tmpFile = "/tmp/prom_config_" + System.currentTimeMillis() + ".yml";
        String writeCmd = String.format("echo '%s' | base64 -d > %s && cp %s %s && rm -f %s",
            base64Content, tmpFile, tmpFile, path, tmpFile);
        try (Session session = ssh.startSession()) {
            Session.Command cmd = session.exec(writeCmd);
            cmd.join(60, TimeUnit.SECONDS);
            String stdout = IOUtils.readFully(cmd.getInputStream()).toString().trim();
            String stderr = IOUtils.readFully(cmd.getErrorStream()).toString().trim();
            Integer exitCode = cmd.getExitStatus();
            log.debug("writeRemoteFile stdout: {}, stderr: {}, exitCode: {}", stdout, stderr, exitCode);
            if (exitCode != null && exitCode != 0) {
                throw new RuntimeException("写入远程文件失败 (exit " + exitCode + "): " + stderr);
            }
            if (exitCode == null) {
                throw new RuntimeException("写入远程文件超时");
            }
        }
    }

    // ==================== Exporters ====================

    @Override
    public Map<String, Object> listExporters(String type, String instanceId, Integer page, Integer pageSize) {
        LambdaQueryWrapper<PromExporter> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(type)) {
            wrapper.eq(PromExporter::getType, type);
        }
        if (StringUtils.hasText(instanceId)) {
            wrapper.eq(PromExporter::getInstanceId, instanceId);
        }
        wrapper.orderByDesc(PromExporter::getCreatedAt);

        Page<PromExporter> pageObj = new Page<>(page, pageSize);
        IPage<PromExporter> pageResult = exporterMapper.selectPage(pageObj, wrapper);
        Map<String, Object> result = new HashMap<>();
        result.put("list", pageResult.getRecords());
        result.put("total", pageResult.getTotal());
        return result;
    }

    @Override
    public PromExporter createExporter(PromExporter exporter) {
        exporter.setId(null);
        exporter.setCreatedAt(LocalDateTime.now());
        exporter.setUpdatedAt(LocalDateTime.now());
        if (exporter.getStatus() == null) {
            exporter.setStatus("stopped");
        }
        exporterMapper.insert(exporter);
        return exporter;
    }

    @Override
    public PromExporter updateExporter(String id, PromExporter exporter) {
        PromExporter existing = exporterMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("Exporter不存在");
        }
        exporter.setId(id);
        exporter.setUpdatedAt(LocalDateTime.now());
        exporterMapper.updateById(exporter);
        return exporterMapper.selectById(id);
    }

    @Override
    public void deleteExporter(String id) {
        PromExporter existing = exporterMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("Exporter不存在");
        }
        exporterMapper.deleteById(id);
    }

    @Override
    public void startExporter(String id) {
        PromExporter exporter = exporterMapper.selectById(id);
        if (exporter == null) {
            throw new BusinessException("Exporter不存在");
        }
        if (StringUtils.hasText(exporter.getMachineId())) {
            startExporterRemote(id);
            return;
        }
        exporter.setStatus("running");
        exporter.setUpdatedAt(LocalDateTime.now());
        exporterMapper.updateById(exporter);
    }

    @Override
    public void stopExporter(String id) {
        PromExporter exporter = exporterMapper.selectById(id);
        if (exporter == null) {
            throw new BusinessException("Exporter不存在");
        }
        if (StringUtils.hasText(exporter.getMachineId())) {
            stopExporterRemote(id);
            return;
        }
        exporter.setStatus("stopped");
        exporter.setUpdatedAt(LocalDateTime.now());
        exporterMapper.updateById(exporter);
    }

    @Override
    @Transactional
    public BatchCreateResultVO batchCreateExporters(BatchCreateExporterDTO dto) {
        // 1. Query all existing exporter host:port keys
        List<PromExporter> allExporters = exporterMapper.selectList(new LambdaQueryWrapper<>());
        Set<String> existingKeys = allExporters.stream()
                .map(e -> e.getHost() + ":" + e.getPort())
                .collect(Collectors.toCollection(HashSet::new));

        BatchCreateResultVO result = new BatchCreateResultVO();
        List<String> skippedTargets = new ArrayList<>();
        int created = 0;

        // 2. Iterate and check for duplicates
        for (BatchCreateExporterDTO.ExporterItem item : dto.getExporters()) {
            String key = item.getHost() + ":" + item.getPort();
            if (existingKeys.contains(key)) {
                skippedTargets.add(key);
                continue;
            }

            // 3. Create new PromExporter
            PromExporter exporter = new PromExporter();
            exporter.setType(item.getType());
            exporter.setName(item.getName());
            exporter.setHost(item.getHost());
            exporter.setPort(item.getPort());
            exporter.setInterval(StringUtils.hasText(item.getInterval()) ? item.getInterval() : "15s");
            exporter.setMetricsPath(StringUtils.hasText(item.getMetricsPath()) ? item.getMetricsPath() : "/metrics");
            exporter.setStatus("stopped");
            exporter.setInstanceId(dto.getInstanceId());
            exporter.setLabels(item.getLabels());
            exporter.setCreatedAt(LocalDateTime.now());
            exporter.setUpdatedAt(LocalDateTime.now());
            exporterMapper.insert(exporter);

            // Prevent intra-batch duplicates
            existingKeys.add(key);
            created++;
        }

        // 4. Build result
        result.setTotal(dto.getExporters().size());
        result.setCreated(created);
        result.setSkipped(skippedTargets.size());
        result.setSkippedTargets(skippedTargets);
        return result;
    }

    @Override
    public ServiceDetectResultVO detectServices(ServiceDetectRequestDTO request) {
        // 1. 查询机器列表
        List<PromDistributeMachine> machines;
        if (request.getMachineIds() != null && !request.getMachineIds().isEmpty()) {
            machines = machineMapper.selectBatchIds(request.getMachineIds());
        } else {
            machines = machineMapper.selectList(new LambdaQueryWrapper<>());
        }

        // 2. 确定要探测的服务类型及端口
        Map<String, Integer> serviceTypes = request.getServiceTypes();
        if (serviceTypes == null || serviceTypes.isEmpty()) {
            serviceTypes = new HashMap<>(DETECT_DEFAULT_PORTS);
        }

        // 3. 构建已有 Exporter 的 host:port → id Map 用于去重
        List<PromExporter> allExporters = exporterMapper.selectList(new LambdaQueryWrapper<>());
        Map<String, String> existingMap = allExporters.stream()
                .collect(Collectors.toMap(
                        e -> e.getHost() + ":" + e.getPort(),
                        PromExporter::getId,
                        (a, b) -> a
                ));

        // 4. 创建短超时 RestTemplate
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(3000);
        RestTemplate detectRestTemplate = new RestTemplate(factory);

        // 5. 为每个 (机器, 服务类型) 组合创建探测任务
        List<CompletableFuture<ServiceDetectResultVO.DetectedService>> futures = new ArrayList<>();
        for (PromDistributeMachine machine : machines) {
            for (Map.Entry<String, Integer> entry : serviceTypes.entrySet()) {
                String serviceType = entry.getKey();
                int port = entry.getValue();
                futures.add(CompletableFuture.supplyAsync(() -> {
                    ServiceDetectResultVO.DetectedService ds = new ServiceDetectResultVO.DetectedService();
                    ds.setMachineId(machine.getId());
                    ds.setMachineName(machine.getName());
                    ds.setMachineIp(machine.getIp());
                    ds.setServiceType(serviceType);
                    ds.setExporterType(SERVICE_TO_EXPORTER_TYPE.getOrDefault(serviceType, "custom"));
                    ds.setPort(port);

                    String key = machine.getIp() + ":" + port;
                    if (existingMap.containsKey(key)) {
                        ds.setAlreadyRegistered(true);
                        ds.setExistingExporterId(existingMap.get(key));
                    }

                    String path = "prometheus".equals(serviceType) ? "/api/v1/status/buildinfo" : "/metrics";
                    String url = "http://" + machine.getIp() + ":" + port + path;

                    long start = System.currentTimeMillis();
                    try {
                        detectRestTemplate.getForEntity(url, String.class);
                        long latency = System.currentTimeMillis() - start;
                        ds.setDetected(true);
                        ds.setLatencyMs(latency);
                    } catch (Exception e) {
                        long latency = System.currentTimeMillis() - start;
                        ds.setDetected(false);
                        ds.setLatencyMs(latency);
                        ds.setMessage(e.getMessage());
                    }
                    return ds;
                }, serviceDetectExecutor));
            }
        }

        // 6. 等待全部完成，聚合结果
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        List<ServiceDetectResultVO.DetectedService> services = futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());

        ServiceDetectResultVO vo = new ServiceDetectResultVO();
        vo.setTotalProbes(services.size());
        vo.setDetectedCount((int) services.stream().filter(ServiceDetectResultVO.DetectedService::isDetected).count());
        vo.setAlreadyRegisteredCount((int) services.stream()
                .filter(s -> s.isDetected() && s.isAlreadyRegistered()).count());
        vo.setServices(services);
        return vo;
    }

    // ==================== Exporter Machine Link ====================

    @Override
    public void linkExporterMachine(String exporterId, ExporterLinkMachineDTO dto) {
        PromExporter exporter = exporterMapper.selectById(exporterId);
        if (exporter == null) {
            throw new BusinessException("Exporter不存在");
        }

        PromDistributeMachine machine = machineMapper.selectById(dto.getMachineId());
        if (machine == null) {
            throw new BusinessException("目标机器不存在");
        }

        exporter.setMachineId(dto.getMachineId());

        // Determine binary name from exporter type
        String binaryName = resolveBinaryName(exporter.getType());
        String serviceName = StringUtils.hasText(dto.getServiceName()) ? dto.getServiceName() : binaryName;
        exporter.setServiceName(serviceName);

        // Auto-detect via SSH
        SSHClient ssh = null;
        try {
            ssh = connectToMachine(dto.getMachineId());

            // 1. Find binary path
            String binaryPath = execCmd(ssh, "which " + binaryName + " 2>/dev/null").trim();
            if (binaryPath.isEmpty()) {
                binaryPath = execCmd(ssh, "find /usr/local /opt /home -maxdepth 5 -name " + binaryName + " -type f 2>/dev/null | head -1").trim();
            }
            if (!binaryPath.isEmpty()) {
                exporter.setBinaryPath(binaryPath);
                String installDir = execCmd(ssh, "dirname \"" + binaryPath + "\"").trim();
                exporter.setInstallDir(installDir);
            }

            // 2. Check systemd service
            String serviceCheck = execCmd(ssh, "systemctl list-unit-files " + serviceName + ".service 2>/dev/null | grep " + serviceName).trim();
            if (serviceCheck.isEmpty() && !serviceName.equals(binaryName)) {
                // Try binary name as service name
                serviceCheck = execCmd(ssh, "systemctl list-unit-files " + binaryName + ".service 2>/dev/null | grep " + binaryName).trim();
                if (!serviceCheck.isEmpty()) {
                    exporter.setServiceName(binaryName);
                }
            }

            // 3. Check running status
            String activeStatus = execCmd(ssh, "systemctl is-active " + exporter.getServiceName() + ".service 2>/dev/null").trim();
            if ("active".equals(activeStatus)) {
                exporter.setStatus("running");
            } else if ("inactive".equals(activeStatus)) {
                exporter.setStatus("stopped");
            } else {
                String psCheck = execCmd(ssh, "pgrep -x " + binaryName + " 2>/dev/null").trim();
                if (!psCheck.isEmpty()) {
                    exporter.setStatus("running");
                    try {
                        exporter.setPid(Integer.parseInt(psCheck.split("\\n")[0].trim()));
                    } catch (NumberFormatException ignored) {}
                }
            }

            // 4. Get PID from systemd if running
            if ("running".equals(exporter.getStatus())) {
                String pidStr = execCmd(ssh, "systemctl show " + exporter.getServiceName() + ".service --property=MainPID 2>/dev/null | cut -d= -f2").trim();
                try {
                    int pid = Integer.parseInt(pidStr);
                    if (pid > 0) exporter.setPid(pid);
                } catch (NumberFormatException ignored) {}
            }

            exporter.setLastStatusCheck(LocalDateTime.now());
        } catch (Exception e) {
            log.warn("Auto-detect failed for exporter {} on machine {}: {}", exporterId, dto.getMachineId(), e.getMessage());
        } finally {
            if (ssh != null) {
                try { ssh.close(); } catch (Exception ignored) {}
            }
        }

        exporter.setUpdatedAt(LocalDateTime.now());
        exporterMapper.updateById(exporter);
    }

    @Override
    public void unlinkExporterMachine(String exporterId) {
        PromExporter exporter = exporterMapper.selectById(exporterId);
        if (exporter == null) {
            throw new BusinessException("Exporter不存在");
        }
        exporter.setMachineId(null);
        exporter.setServiceName(null);
        exporter.setInstallDir(null);
        exporter.setBinaryPath(null);
        exporter.setCliFlags(null);
        exporter.setPid(null);
        exporter.setLastStatusCheck(null);
        exporter.setUpdatedAt(LocalDateTime.now());
        exporterMapper.updateById(exporter);
    }

    // ==================== Exporter Remote Operations ====================

    @Override
    public Map<String, Object> startExporterRemote(String exporterId) {
        return executeExporterSystemctl(exporterId, "start");
    }

    @Override
    public Map<String, Object> stopExporterRemote(String exporterId) {
        return executeExporterSystemctl(exporterId, "stop");
    }

    @Override
    public Map<String, Object> restartExporter(String exporterId) {
        return executeExporterSystemctl(exporterId, "restart");
    }

    private Map<String, Object> executeExporterSystemctl(String exporterId, String action) {
        PromExporter exporter = exporterMapper.selectById(exporterId);
        if (exporter == null) {
            throw new BusinessException("Exporter不存在");
        }
        if (!StringUtils.hasText(exporter.getMachineId())) {
            throw new BusinessException("请先关联机器");
        }
        if (!StringUtils.hasText(exporter.getServiceName())) {
            throw new BusinessException("服务名未配置");
        }

        Map<String, Object> result = new HashMap<>();
        SSHClient ssh = null;
        String output = "";

        try {
            ssh = connectToMachine(exporter.getMachineId());
            PromDistributeMachine machine = machineMapper.selectById(exporter.getMachineId());
            String password = decryptPassword(machine.getSshPassword());

            // Execute systemctl command with sudo (password via stdin)
            String cmd = String.format("sudo -S systemctl %s %s 2>&1", action, exporter.getServiceName());
            output = execCmdWithStdinPassword(ssh, cmd, password).trim();

            // Wait briefly for service state to settle
            Thread.sleep(2000);

            // Verify status
            String activeStatus = execCmd(ssh, "systemctl is-active " + exporter.getServiceName() + " 2>/dev/null").trim();

            String expectedStatus;
            if ("stop".equals(action)) {
                expectedStatus = "inactive";
            } else {
                expectedStatus = "active";
            }

            boolean success = activeStatus.equals(expectedStatus);

            // Update DB
            if ("active".equals(activeStatus)) {
                exporter.setStatus("running");
                String pidStr = execCmd(ssh, "systemctl show " + exporter.getServiceName() + " --property=MainPID 2>/dev/null | cut -d= -f2").trim();
                try {
                    int pid = Integer.parseInt(pidStr);
                    if (pid > 0) exporter.setPid(pid);
                } catch (NumberFormatException ignored) {}
            } else if ("inactive".equals(activeStatus)) {
                exporter.setStatus("stopped");
                exporter.setPid(null);
            } else {
                exporter.setStatus("error");
            }
            exporter.setLastStatusCheck(LocalDateTime.now());
            exporter.setUpdatedAt(LocalDateTime.now());
            exporterMapper.updateById(exporter);

            // Write operation log
            saveOperationLog(exporterId, action, success, success ? action + " 成功" : action + " 失败: " + activeStatus, output);

            result.put("success", success);
            result.put("message", success ? action + " 成功" : action + " 失败，当前状态: " + activeStatus);
            result.put("output", output);
            result.put("status", exporter.getStatus());
        } catch (Exception e) {
            log.error("Failed to {} exporter {}: {}", action, exporterId, e.getMessage(), e);
            saveOperationLog(exporterId, action, false, action + " 异常: " + e.getMessage(), output);
            result.put("success", false);
            result.put("message", action + " 失败: " + e.getMessage());
        } finally {
            if (ssh != null) {
                try { ssh.close(); } catch (Exception ignored) {}
            }
        }
        return result;
    }

    @Override
    public ExporterStatusVO checkExporterStatus(String exporterId) {
        PromExporter exporter = exporterMapper.selectById(exporterId);
        if (exporter == null) {
            throw new BusinessException("Exporter不存在");
        }
        if (!StringUtils.hasText(exporter.getMachineId())) {
            throw new BusinessException("请先关联机器");
        }

        ExporterStatusVO vo = new ExporterStatusVO();
        vo.setExporterId(exporterId);

        SSHClient ssh = null;
        try {
            ssh = connectToMachine(exporter.getMachineId());
            String serviceName = exporter.getServiceName();
            String binaryName = resolveBinaryName(exporter.getType());

            // 1. systemctl is-active
            String activeStatus = execCmd(ssh, "systemctl is-active " + serviceName + " 2>/dev/null").trim();
            if ("active".equals(activeStatus)) {
                vo.setStatus("running");
            } else if ("inactive".equals(activeStatus)) {
                vo.setStatus("stopped");
            } else if ("failed".equals(activeStatus)) {
                vo.setStatus("error");
            } else {
                // Fallback: check process
                String psCheck = execCmd(ssh, "pgrep -x " + binaryName + " 2>/dev/null").trim();
                vo.setStatus(psCheck.isEmpty() ? "stopped" : "running");
            }

            // 2. Get PID
            if ("running".equals(vo.getStatus())) {
                String pidStr = execCmd(ssh, "systemctl show " + serviceName + " --property=MainPID 2>/dev/null | cut -d= -f2").trim();
                try {
                    int pid = Integer.parseInt(pidStr);
                    if (pid > 0) vo.setPid(pid);
                } catch (NumberFormatException ignored) {
                    String pgrep = execCmd(ssh, "pgrep -x " + binaryName + " 2>/dev/null | head -1").trim();
                    try { vo.setPid(Integer.parseInt(pgrep)); } catch (NumberFormatException ignored2) {}
                }
            }

            // 3. Check port listening
            String portCheck = execCmd(ssh, "ss -tlnp 2>/dev/null | grep :" + exporter.getPort()).trim();
            vo.setPortListening(!portCheck.isEmpty());

            vo.setCheckedAt(LocalDateTime.now());

            // Update DB
            exporter.setStatus(vo.getStatus());
            exporter.setPid(vo.getPid());
            exporter.setLastStatusCheck(LocalDateTime.now());
            exporter.setUpdatedAt(LocalDateTime.now());
            exporterMapper.updateById(exporter);

        } catch (Exception e) {
            log.error("Failed to check status for exporter {}: {}", exporterId, e.getMessage());
            vo.setStatus("unknown");
            vo.setMessage("检查失败: " + e.getMessage());
            vo.setCheckedAt(LocalDateTime.now());
        } finally {
            if (ssh != null) {
                try { ssh.close(); } catch (Exception ignored) {}
            }
        }

        return vo;
    }

    @Override
    public List<ExporterStatusVO> batchCheckExporterStatus(List<String> exporterIds) {
        List<PromExporter> exporters = exporterMapper.selectBatchIds(exporterIds);

        // Group by machineId for SSH connection reuse
        Map<String, List<PromExporter>> byMachine = exporters.stream()
                .filter(e -> StringUtils.hasText(e.getMachineId()))
                .collect(Collectors.groupingBy(PromExporter::getMachineId));

        List<CompletableFuture<List<ExporterStatusVO>>> futures = new ArrayList<>();

        for (Map.Entry<String, List<PromExporter>> entry : byMachine.entrySet()) {
            String machineId = entry.getKey();
            List<PromExporter> machineExporters = entry.getValue();

            futures.add(CompletableFuture.supplyAsync(() -> {
                List<ExporterStatusVO> results = new ArrayList<>();
                SSHClient ssh = null;
                try {
                    ssh = connectToMachine(machineId);
                    for (PromExporter exp : machineExporters) {
                        ExporterStatusVO vo = new ExporterStatusVO();
                        vo.setExporterId(exp.getId());
                        try {
                            String activeStatus = execCmd(ssh, "systemctl is-active " + exp.getServiceName() + " 2>/dev/null").trim();
                            if ("active".equals(activeStatus)) {
                                vo.setStatus("running");
                            } else if ("inactive".equals(activeStatus)) {
                                vo.setStatus("stopped");
                            } else {
                                vo.setStatus("error");
                            }

                            String portCheck = execCmd(ssh, "ss -tlnp 2>/dev/null | grep :" + exp.getPort()).trim();
                            vo.setPortListening(!portCheck.isEmpty());
                            vo.setCheckedAt(LocalDateTime.now());

                            // Update DB
                            exp.setStatus(vo.getStatus());
                            exp.setLastStatusCheck(LocalDateTime.now());
                            exp.setUpdatedAt(LocalDateTime.now());
                            exporterMapper.updateById(exp);
                        } catch (Exception e) {
                            vo.setStatus("unknown");
                            vo.setMessage("检查失败: " + e.getMessage());
                            vo.setCheckedAt(LocalDateTime.now());
                        }
                        results.add(vo);
                    }
                } catch (Exception e) {
                    for (PromExporter exp : machineExporters) {
                        ExporterStatusVO vo = new ExporterStatusVO();
                        vo.setExporterId(exp.getId());
                        vo.setStatus("unknown");
                        vo.setMessage("SSH 连接失败: " + e.getMessage());
                        vo.setCheckedAt(LocalDateTime.now());
                        results.add(vo);
                    }
                } finally {
                    if (ssh != null) {
                        try { ssh.close(); } catch (Exception ignored) {}
                    }
                }
                return results;
            }, serviceDetectExecutor));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        List<ExporterStatusVO> allResults = new ArrayList<>();
        for (CompletableFuture<List<ExporterStatusVO>> f : futures) {
            allResults.addAll(f.join());
        }

        // Add entries for exporters without machineId
        for (PromExporter exp : exporters) {
            if (!StringUtils.hasText(exp.getMachineId())) {
                ExporterStatusVO vo = new ExporterStatusVO();
                vo.setExporterId(exp.getId());
                vo.setStatus(exp.getStatus());
                vo.setMessage("未关联机器，无法远程检查");
                vo.setCheckedAt(LocalDateTime.now());
                allResults.add(vo);
            }
        }

        return allResults;
    }

    // ==================== Exporter Config Management ====================

    @Override
    public ExporterConfigVO getExporterConfig(String exporterId) {
        PromExporter exporter = exporterMapper.selectById(exporterId);
        if (exporter == null) {
            throw new BusinessException("Exporter不存在");
        }
        if (!StringUtils.hasText(exporter.getMachineId())) {
            throw new BusinessException("请先关联机器");
        }

        ExporterConfigVO vo = new ExporterConfigVO();
        SSHClient ssh = null;
        try {
            ssh = connectToMachine(exporter.getMachineId());

            // 1. Read systemd service file
            String serviceName = exporter.getServiceName();
            String serviceFile = execCmd(ssh, "cat /etc/systemd/system/" + serviceName + ".service 2>/dev/null").trim();
            if (serviceFile.isEmpty()) {
                serviceFile = execCmd(ssh, "systemctl cat " + serviceName + " 2>/dev/null").trim();
            }
            vo.setServiceFileContent(serviceFile);
            vo.setServiceExists(!serviceFile.isEmpty());

            // 2. Parse ExecStart for cli flags
            if (!serviceFile.isEmpty()) {
                String cliFlags = parseCliFlagsFromServiceFile(serviceFile);
                vo.setCliFlags(cliFlags);
            } else {
                vo.setCliFlags(exporter.getCliFlags());
            }

            // 3. Get binary version
            String binaryPath = exporter.getBinaryPath();
            if (StringUtils.hasText(binaryPath)) {
                String versionOutput = execCmd(ssh, "\"" + binaryPath + "\" --version 2>&1 | head -1").trim();
                vo.setBinaryVersion(versionOutput);
            }

            // 4. Running status
            String activeStatus = execCmd(ssh, "systemctl is-active " + serviceName + " 2>/dev/null").trim();
            vo.setRunningStatus(activeStatus);

        } catch (Exception e) {
            log.error("Failed to get config for exporter {}: {}", exporterId, e.getMessage());
            throw new BusinessException("获取配置失败: " + e.getMessage());
        } finally {
            if (ssh != null) {
                try { ssh.close(); } catch (Exception ignored) {}
            }
        }

        return vo;
    }

    @Override
    public Map<String, Object> updateExporterConfig(String exporterId, String cliFlags,
                                                     String serviceFileContent, boolean restartAfterUpdate) {
        PromExporter exporter = exporterMapper.selectById(exporterId);
        if (exporter == null) {
            throw new BusinessException("Exporter不存在");
        }
        if (!StringUtils.hasText(exporter.getMachineId())) {
            throw new BusinessException("请先关联机器");
        }

        Map<String, Object> result = new HashMap<>();
        SSHClient ssh = null;
        String output = "";

        try {
            ssh = connectToMachine(exporter.getMachineId());
            PromDistributeMachine machine = machineMapper.selectById(exporter.getMachineId());
            String password = decryptPassword(machine.getSshPassword());
            String serviceName = exporter.getServiceName();

            if (StringUtils.hasText(serviceFileContent)) {
                // Write service file using base64 + sudo cp (password via stdin)
                String base64Content = Base64.getEncoder().encodeToString(serviceFileContent.getBytes(StandardCharsets.UTF_8));
                String tmpFile = "/tmp/prom_svc_" + System.currentTimeMillis() + ".service";
                execCmd(ssh, String.format("echo '%s' | base64 -d > %s", base64Content, tmpFile));
                output = execCmdWithStdinPassword(ssh, String.format("sudo -S cp %s /etc/systemd/system/%s.service && rm -f %s 2>&1", tmpFile, serviceName, tmpFile), password).trim();
            } else if (StringUtils.hasText(cliFlags)) {
                // Generate a new service file with updated cli flags
                String newServiceFile = generateExporterServiceFile(exporter, cliFlags, machine.getSshUsername());
                String base64Content = Base64.getEncoder().encodeToString(newServiceFile.getBytes(StandardCharsets.UTF_8));
                String tmpFile = "/tmp/prom_svc_" + System.currentTimeMillis() + ".service";
                execCmd(ssh, String.format("echo '%s' | base64 -d > %s", base64Content, tmpFile));
                output = execCmdWithStdinPassword(ssh, String.format("sudo -S cp %s /etc/systemd/system/%s.service && rm -f %s 2>&1", tmpFile, serviceName, tmpFile), password).trim();
            }

            // daemon-reload (password via stdin)
            output += "\n" + execCmdWithStdinPassword(ssh, "sudo -S systemctl daemon-reload 2>&1", password).trim();

            // Optional restart
            if (restartAfterUpdate) {
                output += "\n" + execCmdWithStdinPassword(ssh, String.format("sudo -S systemctl restart %s 2>&1", serviceName), password).trim();
                Thread.sleep(2000);

                String activeStatus = execCmd(ssh, "systemctl is-active " + serviceName + " 2>/dev/null").trim();
                exporter.setStatus("active".equals(activeStatus) ? "running" : "error");
                exporter.setLastStatusCheck(LocalDateTime.now());
            }

            // Update DB
            if (StringUtils.hasText(cliFlags)) {
                exporter.setCliFlags(cliFlags);
            }
            exporter.setUpdatedAt(LocalDateTime.now());
            exporterMapper.updateById(exporter);

            saveOperationLog(exporterId, "config_update", true, "配置更新成功", output);

            result.put("success", true);
            result.put("message", "配置更新成功" + (restartAfterUpdate ? "，服务已重启" : ""));
            result.put("output", output);
        } catch (Exception e) {
            log.error("Failed to update config for exporter {}: {}", exporterId, e.getMessage(), e);
            saveOperationLog(exporterId, "config_update", false, "配置更新失败: " + e.getMessage(), output);
            result.put("success", false);
            result.put("message", "配置更新失败: " + e.getMessage());
        } finally {
            if (ssh != null) {
                try { ssh.close(); } catch (Exception ignored) {}
            }
        }

        return result;
    }

    // ==================== Exporter Logs ====================

    @Override
    public ExporterLogVO getExporterLogs(String exporterId, Integer lines, String since) {
        PromExporter exporter = exporterMapper.selectById(exporterId);
        if (exporter == null) {
            throw new BusinessException("Exporter不存在");
        }
        if (!StringUtils.hasText(exporter.getMachineId())) {
            throw new BusinessException("请先关联机器");
        }

        if (lines == null || lines <= 0) {
            lines = 100;
        }

        ExporterLogVO vo = new ExporterLogVO();
        SSHClient ssh = null;

        try {
            ssh = connectToMachine(exporter.getMachineId());

            StringBuilder cmd = new StringBuilder();
            cmd.append("journalctl -u ").append(exporter.getServiceName())
               .append(" --no-pager -n ").append(lines);
            if (StringUtils.hasText(since)) {
                cmd.append(" --since \"").append(since).append("\"");
            }

            String logContent = execCmd(ssh, cmd.toString()).trim();
            vo.setLogContent(logContent);
            vo.setLineCount(logContent.isEmpty() ? 0 : logContent.split("\\n").length);

        } catch (Exception e) {
            log.error("Failed to get logs for exporter {}: {}", exporterId, e.getMessage());
            throw new BusinessException("获取日志失败: " + e.getMessage());
        } finally {
            if (ssh != null) {
                try { ssh.close(); } catch (Exception ignored) {}
            }
        }

        return vo;
    }

    // ==================== Exporter Operation Logs ====================

    @Override
    public Map<String, Object> listExporterOperationLogs(String exporterId) {
        LambdaQueryWrapper<PromExporterOperationLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PromExporterOperationLog::getExporterId, exporterId);
        wrapper.orderByDesc(PromExporterOperationLog::getCreatedAt);

        List<PromExporterOperationLog> logs = operationLogMapper.selectList(wrapper);
        Map<String, Object> result = new HashMap<>();
        result.put("list", logs);
        result.put("total", logs.size());
        return result;
    }

    // ==================== Exporter Helper Methods ====================

    private String resolveBinaryName(String exporterType) {
        return switch (exporterType) {
            case "node" -> "node_exporter";
            case "blackbox" -> "blackbox_exporter";
            case "process" -> "process_exporter";
            case "cadvisor" -> "cadvisor";
            case "mysqld" -> "mysqld_exporter";
            case "redis" -> "redis_exporter";
            case "nginx" -> "nginx_exporter";
            default -> exporterType;
        };
    }

    private void saveOperationLog(String exporterId, String operation, boolean success, String message, String detail) {
        try {
            PromExporterOperationLog log = new PromExporterOperationLog();
            log.setExporterId(exporterId);
            log.setOperation(operation);
            log.setSuccess(success ? 1 : 0);
            log.setMessage(message);
            log.setDetail(detail);
            log.setCreatedAt(LocalDateTime.now());
            operationLogMapper.insert(log);
        } catch (Exception e) {
            DatasourceServiceImpl.log.warn("Failed to save operation log: {}", e.getMessage());
        }
    }

    private String parseCliFlagsFromServiceFile(String serviceFileContent) {
        for (String line : serviceFileContent.split("\\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("ExecStart=")) {
                String execStart = trimmed.substring("ExecStart=".length()).trim();
                // Remove the binary path, keep flags
                // Handle multi-line ExecStart (backslash continuation)
                int firstSpace = execStart.indexOf(' ');
                if (firstSpace > 0) {
                    return execStart.substring(firstSpace + 1).replace(" \\\n", " ").replace("\\", "").trim();
                }
                return "";
            }
        }
        return "";
    }

    private String generateExporterServiceFile(PromExporter exporter, String cliFlags, String sshUsername) {
        String binaryPath = StringUtils.hasText(exporter.getBinaryPath())
                ? exporter.getBinaryPath()
                : (StringUtils.hasText(exporter.getInstallDir())
                    ? exporter.getInstallDir() + "/" + resolveBinaryName(exporter.getType())
                    : resolveBinaryName(exporter.getType()));

        String execStart = binaryPath;
        if (StringUtils.hasText(cliFlags)) {
            execStart += " " + cliFlags;
        }

        return "[Unit]\n"
                + "Description=" + exporter.getServiceName() + "\n"
                + "After=network.target\n"
                + "\n"
                + "[Service]\n"
                + "Type=simple\n"
                + "User=" + sshUsername + "\n"
                + "ExecStart=" + execStart + "\n"
                + "Restart=always\n"
                + "RestartSec=5\n"
                + "\n"
                + "[Install]\n"
                + "WantedBy=multi-user.target\n";
    }

    // ==================== Metric Metadata ====================

    @Override
    public Map<String, Object> listMetricMeta(String name, String type, String exporter) {
        LambdaQueryWrapper<PromMetricMeta> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(name)) {
            wrapper.like(PromMetricMeta::getName, name);
        }
        if (StringUtils.hasText(type)) {
            wrapper.eq(PromMetricMeta::getType, type);
        }
        if (StringUtils.hasText(exporter)) {
            wrapper.eq(PromMetricMeta::getExporter, exporter);
        }
        wrapper.orderByAsc(PromMetricMeta::getName);

        List<PromMetricMeta> list = metricMetaMapper.selectList(wrapper);
        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", list.size());
        return result;
    }

    @Override
    public void toggleMetricFavorite(String id) {
        PromMetricMeta meta = metricMetaMapper.selectById(id);
        if (meta == null) {
            throw new BusinessException("指标不存在");
        }
        meta.setFavorite(!Boolean.TRUE.equals(meta.getFavorite()));
        metricMetaMapper.updateById(meta);
    }
}
