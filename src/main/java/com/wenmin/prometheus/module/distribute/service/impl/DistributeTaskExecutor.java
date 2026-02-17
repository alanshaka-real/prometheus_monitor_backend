package com.wenmin.prometheus.module.distribute.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wenmin.prometheus.module.distribute.dto.ComponentConfigDTO;
import com.wenmin.prometheus.module.distribute.dto.PrometheusConfigDTO;
import com.wenmin.prometheus.module.distribute.entity.*;
import com.wenmin.prometheus.module.distribute.mapper.*;
import com.wenmin.prometheus.module.distribute.service.SoftwareService;
import com.wenmin.prometheus.module.distribute.util.ComponentConfigGenerator;
import com.wenmin.prometheus.module.distribute.util.PrometheusYamlGenerator;
import com.wenmin.prometheus.util.EncryptionUtil;
import com.wenmin.prometheus.websocket.DistributeWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.IOUtils;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.xfer.FileSystemFile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 独立的异步任务执行器 Bean。
 * 从 DistributeServiceImpl 中提取出来，解决 @Async 自调用（self-invocation）
 * 导致 Spring AOP 代理被绕过、异步注解失效的问题。
 */
@Slf4j
@Component("distributeTaskRunner")
@RequiredArgsConstructor
public class DistributeTaskExecutor {

    private final PromDistributeMachineMapper machineMapper;
    private final PromDistributeTaskMapper taskMapper;
    private final PromDistributeTaskDetailMapper taskDetailMapper;
    private final SoftwareService softwareService;
    private final DistributeWebSocketHandler webSocketHandler;
    private final ObjectMapper objectMapper;
    private final DeploymentLockManager deploymentLockManager;

    @Value("${distribute.encryption-key}")
    private String encryptionKey;

    @Value("${distribute.software-path}")
    private String softwarePath;

    @Value("${distribute.ssh.strict-host-key-checking:false}")
    private boolean strictHostKeyChecking;

    @Value("${distribute.task-timeout-minutes:30}")
    private int taskTimeoutMinutes;

    // Track cancelled tasks
    private final Set<String> cancelledTasks = ConcurrentHashMap.newKeySet();

    public void markCancelled(String taskId) {
        cancelledTasks.add(taskId);
    }

    @Async("distributeTaskExecutor")
    public void executeTaskAsync(String taskId) {
        PromDistributeTask task = taskMapper.selectById(taskId);
        if (task == null) return;

        task.setStatus("running");
        task.setStartedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        taskMapper.updateById(task);

        LambdaQueryWrapper<PromDistributeTaskDetail> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PromDistributeTaskDetail::getTaskId, taskId)
                .eq(PromDistributeTaskDetail::getStatus, "pending");
        List<PromDistributeTaskDetail> details = taskDetailMapper.selectList(wrapper);

        int successCount = 0;
        int failCount = 0;

        for (PromDistributeTaskDetail detail : details) {
            if (cancelledTasks.contains(taskId)) {
                break;
            }
            try {
                executeDetail(taskId, detail);
                successCount++;
            } catch (Exception e) {
                failCount++;
                log.error("Task detail execution failed: {}", detail.getId(), e);
            }
        }

        // Update task final status
        if (!cancelledTasks.remove(taskId)) {
            task = taskMapper.selectById(taskId);
            task.setSuccessCount(successCount);
            task.setFailCount(failCount);
            task.setFinishedAt(LocalDateTime.now());
            task.setUpdatedAt(LocalDateTime.now());

            if (failCount == 0) {
                task.setStatus("success");
            } else if (successCount == 0) {
                task.setStatus("failed");
            } else {
                task.setStatus("partial_fail");
            }
            taskMapper.updateById(task);
        }
    }

    @Async("distributeTaskExecutor")
    public void executeDetailAsync(String taskId, String detailId) {
        PromDistributeTaskDetail detail = taskDetailMapper.selectById(detailId);
        if (detail == null) return;

        try {
            executeDetail(taskId, detail);
            updateTaskCounts(taskId);
        } catch (Exception e) {
            log.error("Retry execution failed: {}", detailId, e);
            updateTaskCounts(taskId);
        }
    }

    private void executeDetail(String taskId, PromDistributeTaskDetail detail) {
        long startTime = System.currentTimeMillis();
        long timeoutMillis = taskTimeoutMinutes * 60L * 1000L;

        PromDistributeMachine machine = machineMapper.selectById(detail.getMachineId());
        if (machine == null) {
            markDetailFailed(detail, "目标机器不存在");
            return;
        }

        // Try to acquire deployment lock for this machine
        String machineKey = machine.getIp();
        if (!deploymentLockManager.tryLock(machineKey, taskId)) {
            String holder = deploymentLockManager.getHolder(machineKey);
            String msg = "该主机正在执行其他部署任务 (任务ID: " + holder + ")";
            log.warn("部署锁获取失败: {} - {}", machineKey, msg);
            markDetailFailed(detail, msg);
            return;
        }

        try {
            doExecuteDetail(taskId, detail, machine, startTime, timeoutMillis);
        } finally {
            deploymentLockManager.unlock(machineKey, taskId);
        }
    }

    /**
     * Check if the task has exceeded the configured timeout.
     * @throws RuntimeException if timed out
     */
    private void checkTimeout(long startTime, long timeoutMillis) {
        long elapsed = System.currentTimeMillis() - startTime;
        if (elapsed > timeoutMillis) {
            throw new RuntimeException("部署任务超时 (已运行 " + (elapsed / 60000) + " 分钟，上限 " + taskTimeoutMinutes + " 分钟)");
        }
    }

    private void doExecuteDetail(String taskId, PromDistributeTaskDetail detail,
                                  PromDistributeMachine machine, long startTime, long timeoutMillis) {
        // Extract configs from task
        PromDistributeTask task = taskMapper.selectById(taskId);
        PrometheusConfigDTO promConfig = extractPrometheusConfig(task);
        Map<String, ComponentConfigDTO> componentConfigMap = extractComponentConfigs(task);

        String password = decryptPassword(machine.getSshPassword());
        detail.setStatus("running");
        detail.setStartedAt(LocalDateTime.now());
        detail.setUpdatedAt(LocalDateTime.now());
        taskDetailMapper.updateById(detail);

        StringBuilder logBuilder = new StringBuilder();
        List<String> components = detail.getComponents();
        int totalSteps = 0;
        for (String c : components) {
            if ("prometheus".equals(c)) {
                totalSteps += 13;
            } else if ("blackbox_exporter".equals(c) || "mysql_exporter".equals(c)) {
                totalSteps += 11; // extra step for writing config file
            } else if ("process_exporter".equals(c)) {
                // Extra step if using config file mode
                ComponentConfigDTO peCfg = componentConfigMap.get(c);
                if (peCfg != null && Boolean.TRUE.equals(peCfg.getProcessUseConfig())) {
                    totalSteps += 11;
                } else {
                    totalSteps += 10;
                }
            } else {
                totalSteps += 10;
            }
        }
        int currentStepNum = 0;

        try {
            for (String component : components) {
                if (cancelledTasks.contains(taskId)) {
                    markDetailFailed(detail, "任务已取消");
                    return;
                }

                // Check timeout before each component
                checkTimeout(startTime, timeoutMillis);

                PromDistributeSoftware software = softwareService.findSoftware(
                        component, machine.getOsType(), machine.getOsArch());
                if (software == null) {
                    throw new RuntimeException("未找到匹配的软件包: " + component
                            + " (" + machine.getOsType() + "/" + machine.getOsArch() + ")");
                }

                String filePath = softwareService.getSoftwareFilePath(software);
                if (!new File(filePath).exists()) {
                    throw new RuntimeException("软件包文件不存在: " + filePath);
                }

                boolean isPrometheus = "prometheus".equals(component);
                ComponentConfigDTO compCfg = componentConfigMap.get(component);
                int port = compCfg != null && compCfg.getPort() != null
                        ? compCfg.getPort()
                        : (software.getDefaultPort() != null ? software.getDefaultPort() : 9100);

                SSHClient ssh = null;
                try {
                    // Step 1: SSH Connect
                    currentStepNum++;
                    updateProgress(taskId, detail, currentStepNum, totalSteps, "SSH 连接", component, logBuilder);
                    ssh = new SSHClient();
                    if (!strictHostKeyChecking) {
                        log.warn("SSH strict host key checking is DISABLED for {}. Enable via distribute.ssh.strict-host-key-checking=true in production.", machine.getIp());
                        ssh.addHostKeyVerifier(new PromiscuousVerifier());
                    } else {
                        ssh.loadKnownHosts();
                    }
                    ssh.setConnectTimeout(10000);
                    ssh.connect(machine.getIp(), machine.getSshPort());
                    ssh.authPassword(machine.getSshUsername(), password);
                    appendAndSendLog(logBuilder, taskId, detail, machine.getIp(), "SSH 连接成功", "info");

                    // Check timeout after SSH connect
                    checkTimeout(startTime, timeoutMillis);

                    // Determine install directory
                    String installDir;
                    if (isPrometheus && promConfig != null
                            && promConfig.getInstallDir() != null && !promConfig.getInstallDir().isBlank()) {
                        installDir = promConfig.getInstallDir();
                    } else if (!isPrometheus && compCfg != null
                            && compCfg.getInstallDir() != null && !compCfg.getInstallDir().isBlank()) {
                        installDir = compCfg.getInstallDir();
                    } else {
                        installDir = "~/prometheus/" + component;
                    }
                    installDir = resolveInstallDir(ssh, installDir, machine.getSshUsername());

                    // Update promConfig with resolved installDir
                    if (isPrometheus && promConfig != null) {
                        promConfig.setInstallDir(installDir);
                    }

                    // Step 2: Detect OS
                    currentStepNum++;
                    updateProgress(taskId, detail, currentStepNum, totalSteps, "检测操作系统", component, logBuilder);
                    String uname = execCmdChecked(ssh, "uname -s -m", password).trim();
                    appendAndSendLog(logBuilder, taskId, detail, machine.getIp(), "操作系统: " + uname, "info");

                    // Step 3: Create directory (no sudo for user home paths)
                    currentStepNum++;
                    updateProgress(taskId, detail, currentStepNum, totalSteps, "创建安装目录", component, logBuilder);
                    execCmdWithSudo(ssh, "mkdir -p " + installDir, password, installDir, machine.getSshUsername());
                    appendAndSendLog(logBuilder, taskId, detail, machine.getIp(), "创建目录: " + installDir, "info");

                    // Check timeout after directory creation
                    checkTimeout(startTime, timeoutMillis);

                    // Step 4: Upload tar
                    currentStepNum++;
                    updateProgress(taskId, detail, currentStepNum, totalSteps, "上传 " + component, component, logBuilder);
                    String remoteTarPath = "/tmp/" + software.getFileName();
                    ssh.newSCPFileTransfer().upload(new FileSystemFile(new File(filePath)), "/tmp/");
                    appendAndSendLog(logBuilder, taskId, detail, machine.getIp(), "上传完成: " + software.getFileName(), "info");

                    // Check timeout after upload
                    checkTimeout(startTime, timeoutMillis);

                    // Step 5: Extract (120s timeout for large tar.gz files)
                    currentStepNum++;
                    updateProgress(taskId, detail, currentStepNum, totalSteps, "解压安装包", component, logBuilder);
                    execCmdWithSudo(ssh, "tar -xzf " + remoteTarPath + " -C " + installDir + " --strip-components=1", password, installDir, machine.getSshUsername(), 120);
                    // Verify extraction succeeded
                    String lsOutput = execCmd(ssh, "ls " + installDir).trim();
                    appendAndSendLog(logBuilder, taskId, detail, machine.getIp(), "解压文件列表: " + lsOutput, "info");
                    if (lsOutput.isEmpty()) {
                        throw new RuntimeException("解压后目录为空: " + installDir);
                    }
                    // Ensure correct ownership for user-writable paths
                    String homeDir = execCmd(ssh, "echo $HOME").trim();
                    if (installDir.startsWith(homeDir)) {
                        execCmd(ssh, "chown -R " + machine.getSshUsername() + ":" + machine.getSshUsername() + " " + installDir);
                    }
                    appendAndSendLog(logBuilder, taskId, detail, machine.getIp(), "解压完成", "info");

                    // Check timeout after extraction
                    checkTimeout(startTime, timeoutMillis);

                    if (isPrometheus) {
                        // Prometheus Step 6: Create TSDB data directory
                        String dataDir = (promConfig != null && promConfig.getDataDir() != null && !promConfig.getDataDir().isBlank())
                                ? promConfig.getDataDir() : installDir + "/data";
                        if (dataDir.startsWith("~")) {
                            dataDir = resolveInstallDir(ssh, dataDir, machine.getSshUsername());
                        }
                        // Update promConfig with resolved dataDir
                        if (promConfig != null) {
                            promConfig.setDataDir(dataDir);
                        }

                        currentStepNum++;
                        updateProgress(taskId, detail, currentStepNum, totalSteps, "创建数据目录", component, logBuilder);
                        execCmdWithSudo(ssh, "mkdir -p " + dataDir, password, dataDir, machine.getSshUsername());
                        appendAndSendLog(logBuilder, taskId, detail, machine.getIp(), "创建数据目录: " + dataDir, "info");

                        // Prometheus Step 7: Write prometheus.yml
                        currentStepNum++;
                        updateProgress(taskId, detail, currentStepNum, totalSteps, "写入配置文件", component, logBuilder);
                        String yamlContent = PrometheusYamlGenerator.generate(promConfig);
                        String base64Yaml = Base64.getEncoder().encodeToString(yamlContent.getBytes(StandardCharsets.UTF_8));
                        execCmdChecked(ssh, "echo '" + base64Yaml + "' | base64 -d > " + installDir + "/prometheus.yml", password);
                        appendAndSendLog(logBuilder, taskId, detail, machine.getIp(), "prometheus.yml 写入完成", "info");

                        // Check timeout after config write
                        checkTimeout(startTime, timeoutMillis);

                        // Prometheus Step 8: Validate config
                        currentStepNum++;
                        updateProgress(taskId, detail, currentStepNum, totalSteps, "验证配置文件", component, logBuilder);
                        try {
                            String promtoolPath = installDir + "/promtool";
                            String checkCmd = "test -f " + promtoolPath + " && " + promtoolPath + " check config " + installDir + "/prometheus.yml 2>&1 || echo 'promtool not found, skipping validation'";
                            String checkOutput = execCmd(ssh, checkCmd).trim();
                            appendAndSendLog(logBuilder, taskId, detail, machine.getIp(), "配置验证: " + checkOutput, "info");
                        } catch (Exception e) {
                            appendAndSendLog(logBuilder, taskId, detail, machine.getIp(), "WARNING: 配置验证跳过 - " + sanitizeErrorMessage(e.getMessage()), "warn");
                        }

                        // Prometheus Step 9: Write systemd service (requires root)
                        currentStepNum++;
                        updateProgress(taskId, detail, currentStepNum, totalSteps, "配置 systemd 服务", component, logBuilder);
                        String serviceContent = generatePrometheusSystemdService(installDir, promConfig, machine.getSshUsername());
                        writeSudoFile(ssh, serviceContent, "/etc/systemd/system/prometheus.service", password);
                        appendAndSendLog(logBuilder, taskId, detail, machine.getIp(), "systemd 服务配置完成", "info");
                    } else {
                        // Non-Prometheus: write component-specific config files if needed
                        if ("blackbox_exporter".equals(component)) {
                            currentStepNum++;
                            updateProgress(taskId, detail, currentStepNum, totalSteps, "写入 blackbox.yml", component, logBuilder);
                            String blackboxYaml = ComponentConfigGenerator.generateBlackboxYaml(compCfg);
                            String base64Blackbox = Base64.getEncoder().encodeToString(blackboxYaml.getBytes(StandardCharsets.UTF_8));
                            execCmdChecked(ssh, "echo '" + base64Blackbox + "' | base64 -d > " + installDir + "/blackbox.yml", password);
                            appendAndSendLog(logBuilder, taskId, detail, machine.getIp(), "blackbox.yml 写入完成", "info");
                        }

                        if ("process_exporter".equals(component)) {
                            String peConfig = ComponentConfigGenerator.generateProcessExporterConfig(compCfg);
                            if (peConfig != null) {
                                currentStepNum++;
                                updateProgress(taskId, detail, currentStepNum, totalSteps, "写入 process_exporter 配置", component, logBuilder);
                                String base64PeConfig = Base64.getEncoder().encodeToString(peConfig.getBytes(StandardCharsets.UTF_8));
                                execCmdChecked(ssh, "echo '" + base64PeConfig + "' | base64 -d > " + installDir + "/process_exporter_config.yml", password);
                                appendAndSendLog(logBuilder, taskId, detail, machine.getIp(), "process_exporter_config.yml 写入完成", "info");
                            }
                        }

                        if ("mysql_exporter".equals(component)) {
                            currentStepNum++;
                            updateProgress(taskId, detail, currentStepNum, totalSteps, "写入 MySQL 凭据", component, logBuilder);
                            String myCnf = ComponentConfigGenerator.generateMyCnf(compCfg != null ? compCfg : new ComponentConfigDTO());
                            String mysqlHomeDir = execCmd(ssh, "echo $HOME").trim();
                            String myCnfPath = mysqlHomeDir + "/.my.cnf";
                            String base64MyCnf = Base64.getEncoder().encodeToString(myCnf.getBytes(StandardCharsets.UTF_8));
                            execCmdChecked(ssh, "echo '" + base64MyCnf + "' | base64 -d > " + myCnfPath, password);
                            execCmdChecked(ssh, "chmod 600 " + myCnfPath, password);
                            appendAndSendLog(logBuilder, taskId, detail, machine.getIp(), ".my.cnf 写入完成 (权限 600)", "info");
                        }

                        // Write systemd service using ComponentConfigGenerator
                        currentStepNum++;
                        updateProgress(taskId, detail, currentStepNum, totalSteps, "配置 systemd 服务", component, logBuilder);
                        String serviceContent = ComponentConfigGenerator.generateSystemdService(
                                component, installDir, compCfg, machine.getSshUsername());
                        writeSudoFile(ssh, serviceContent, "/etc/systemd/system/" + component + ".service", password);
                        appendAndSendLog(logBuilder, taskId, detail, machine.getIp(), "systemd 服务配置完成", "info");
                    }

                    // Check timeout before service operations
                    checkTimeout(startTime, timeoutMillis);

                    // daemon-reload + enable (requires root)
                    currentStepNum++;
                    updateProgress(taskId, detail, currentStepNum, totalSteps, "启用服务", component, logBuilder);
                    execCmdWithStdinPassword(ssh, "sudo -S systemctl daemon-reload && sudo -S systemctl enable " + escapeShellArg(component) + " 2>&1", password);
                    appendAndSendLog(logBuilder, taskId, detail, machine.getIp(), "服务已启用", "info");

                    // Start service (requires root)
                    currentStepNum++;
                    updateProgress(taskId, detail, currentStepNum, totalSteps, "启动服务", component, logBuilder);
                    execCmdWithStdinPassword(ssh, "sudo -S systemctl start " + escapeShellArg(component) + " 2>&1", password);
                    appendAndSendLog(logBuilder, taskId, detail, machine.getIp(), "服务已启动", "info");

                    // Verify
                    currentStepNum++;
                    updateProgress(taskId, detail, currentStepNum, totalSteps, "验证服务状态", component, logBuilder);
                    Thread.sleep(2000);
                    String statusOutput = execCmd(ssh, "systemctl is-active " + component).trim();
                    appendAndSendLog(logBuilder, taskId, detail, machine.getIp(), "服务状态: " + statusOutput, "info");
                    if (!"active".equals(statusOutput)) {
                        String detailedStatus = execCmd(ssh, "systemctl status " + component + " 2>&1 || true").trim();
                        appendAndSendLog(logBuilder, taskId, detail, machine.getIp(), "服务详细状态:\n" + detailedStatus, "error");
                        String journalLogs = execCmd(ssh, "journalctl -u " + component + " --no-pager -n 20 2>&1 || true").trim();
                        appendAndSendLog(logBuilder, taskId, detail, machine.getIp(), "最近日志:\n" + journalLogs, "error");
                        throw new RuntimeException(component + " 服务启动失败，状态: " + statusOutput);
                    }

                    // Cleanup
                    currentStepNum++;
                    updateProgress(taskId, detail, currentStepNum, totalSteps, "清理临时文件", component, logBuilder);
                    execCmd(ssh, "rm -f " + remoteTarPath);
                    appendAndSendLog(logBuilder, taskId, detail, machine.getIp(), component + " 安装完成", "info");
                } finally {
                    if (ssh != null) {
                        try { ssh.close(); } catch (Exception ignored) {}
                    }
                }
            }

            // Mark success
            detail.setStatus("success");
            detail.setProgress(100);
            detail.setCurrentStep("安装完成");
            detail.setLogText(logBuilder.toString());
            detail.setFinishedAt(LocalDateTime.now());
            detail.setUpdatedAt(LocalDateTime.now());
            taskDetailMapper.updateById(detail);

            webSocketHandler.sendStatus(taskId, detail.getId(), machine.getIp(),
                    "success", "安装完成");

        } catch (Exception e) {
            String safeMsg = sanitizeErrorMessage(e.getMessage());
            log.error("Installation failed on {}:{}", machine.getIp(), e.getMessage(), e);
            appendLog(logBuilder, "ERROR: " + safeMsg);
            detail.setStatus("failed");
            detail.setErrorMessage(safeMsg);
            detail.setLogText(logBuilder.toString());
            detail.setFinishedAt(LocalDateTime.now());
            detail.setUpdatedAt(LocalDateTime.now());
            taskDetailMapper.updateById(detail);

            webSocketHandler.sendStatus(taskId, detail.getId(), machine.getIp(),
                    "failed", safeMsg);
            throw new RuntimeException(safeMsg, e);
        }
    }

    // ==================== Helper Methods ====================

    private void updateProgress(String taskId, PromDistributeTaskDetail detail,
                                int step, int totalSteps, String stepName,
                                String component, StringBuilder logBuilder) {
        int progress = (int) ((double) step / totalSteps * 100);
        detail.setProgress(progress);
        detail.setCurrentStep(stepName);
        detail.setUpdatedAt(LocalDateTime.now());
        taskDetailMapper.updateById(detail);

        appendLog(logBuilder, "[" + component + "] " + stepName);

        webSocketHandler.sendProgress(taskId, detail.getId(), detail.getMachineIp(),
                progress, stepName, component);
    }

    private void appendAndSendLog(StringBuilder sb, String taskId, PromDistributeTaskDetail detail,
                                   String machineIp, String line, String level) {
        appendLog(sb, line);
        webSocketHandler.sendLog(taskId, detail.getId(), machineIp, line, level);
    }

    private void markDetailFailed(PromDistributeTaskDetail detail, String message) {
        detail.setStatus("failed");
        detail.setErrorMessage(message);
        detail.setFinishedAt(LocalDateTime.now());
        detail.setUpdatedAt(LocalDateTime.now());
        taskDetailMapper.updateById(detail);
    }

    private void updateTaskCounts(String taskId) {
        PromDistributeTask task = taskMapper.selectById(taskId);
        if (task == null) return;

        LambdaQueryWrapper<PromDistributeTaskDetail> successWrapper = new LambdaQueryWrapper<>();
        successWrapper.eq(PromDistributeTaskDetail::getTaskId, taskId)
                .eq(PromDistributeTaskDetail::getStatus, "success");
        long successCount = taskDetailMapper.selectCount(successWrapper);

        LambdaQueryWrapper<PromDistributeTaskDetail> failWrapper = new LambdaQueryWrapper<>();
        failWrapper.eq(PromDistributeTaskDetail::getTaskId, taskId)
                .eq(PromDistributeTaskDetail::getStatus, "failed");
        long failCount = taskDetailMapper.selectCount(failWrapper);

        task.setSuccessCount((int) successCount);
        task.setFailCount((int) failCount);

        LambdaQueryWrapper<PromDistributeTaskDetail> pendingWrapper = new LambdaQueryWrapper<>();
        pendingWrapper.eq(PromDistributeTaskDetail::getTaskId, taskId)
                .in(PromDistributeTaskDetail::getStatus, "pending", "running");
        long pendingCount = taskDetailMapper.selectCount(pendingWrapper);

        if (pendingCount == 0) {
            task.setFinishedAt(LocalDateTime.now());
            if (failCount == 0) {
                task.setStatus("success");
            } else if (successCount == 0) {
                task.setStatus("failed");
            } else {
                task.setStatus("partial_fail");
            }
        }

        task.setUpdatedAt(LocalDateTime.now());
        taskMapper.updateById(task);
    }

    private String decryptPassword(String encrypted) {
        if (encrypted == null || encrypted.isBlank()) return "";
        try {
            return EncryptionUtil.decrypt(encrypted, encryptionKey);
        } catch (Exception e) {
            log.error("Password decryption failed", e);
            throw new RuntimeException("SSH 密码解密失败");
        }
    }

    /**
     * Execute command, return stdout only (backward-compatible, no exit code check).
     * IMPORTANT: cmd.join() MUST be called BEFORE readFully() to ensure the command
     * has finished; otherwise SSHJ may return immediately with empty/partial data.
     */
    private String execCmd(SSHClient ssh, String command) throws Exception {
        try (Session session = ssh.startSession()) {
            Session.Command cmd = session.exec(command);
            cmd.join(60, TimeUnit.SECONDS);
            String output = IOUtils.readFully(cmd.getInputStream()).toString();
            return output;
        }
    }

    /**
     * Execute command with exit code checking. Throws RuntimeException on failure.
     * Reads both stdout and stderr for better error reporting.
     *
     * @param timeoutSec command timeout in seconds (default 60)
     */
    private String execCmdChecked(SSHClient ssh, String command, String password, int timeoutSec) throws Exception {
        try (Session session = ssh.startSession()) {
            Session.Command cmd = session.exec(command);
            cmd.join(timeoutSec, TimeUnit.SECONDS);

            String stdout = IOUtils.readFully(cmd.getInputStream()).toString();
            String stderr = IOUtils.readFully(cmd.getErrorStream()).toString();

            Integer exitCode = cmd.getExitStatus();
            if (exitCode == null) {
                log.warn("Command timed out ({}s): {}", timeoutSec, command.substring(0, Math.min(80, command.length())));
                throw new RuntimeException("命令执行超时 (" + timeoutSec + "秒): " + command.substring(0, Math.min(60, command.length())));
            }
            if (exitCode != 0) {
                // Filter out sudo password prompt noise from stderr
                String cleanStderr = stderr.replaceAll("\\[sudo\\] password for \\w+:", "").trim();
                String errorMsg = cleanStderr.isEmpty() ? stdout.trim() : cleanStderr;
                if (errorMsg.isEmpty()) {
                    errorMsg = "exit code " + exitCode;
                }
                log.warn("Command failed (exit={}): {} -> {}", exitCode, command.substring(0, Math.min(80, command.length())), errorMsg);
                throw new RuntimeException("命令执行失败: " + errorMsg);
            }
            return stdout;
        }
    }

    private String execCmdChecked(SSHClient ssh, String command, String password) throws Exception {
        return execCmdChecked(ssh, command, password, 60);
    }

    /**
     * Execute command with sudo if path is outside user home directory.
     * Uses sudo -S and feeds password via stdin to avoid command injection.
     *
     * @param timeoutSec command timeout in seconds
     */
    private void execCmdWithSudo(SSHClient ssh, String command, String password, String targetPath, String sshUsername, int timeoutSec) throws Exception {
        String homeDir = execCmd(ssh, "echo $HOME").trim();
        boolean needsSudo = !targetPath.startsWith(homeDir);

        if (needsSudo) {
            execCmdWithStdinPassword(ssh, "sudo -S " + command + " 2>&1", password, timeoutSec);
        } else {
            execCmdChecked(ssh, command + " 2>&1", password, timeoutSec);
        }
    }

    private void execCmdWithSudo(SSHClient ssh, String command, String password, String targetPath, String sshUsername) throws Exception {
        execCmdWithSudo(ssh, command, password, targetPath, sshUsername, 60);
    }

    /**
     * Execute a command that requires sudo, feeding the password via stdin
     * instead of embedding it in the command string. This prevents command injection.
     */
    private String execCmdWithStdinPassword(SSHClient ssh, String command, String password) throws Exception {
        return execCmdWithStdinPassword(ssh, command, password, 60);
    }

    private String execCmdWithStdinPassword(SSHClient ssh, String command, String password, int timeoutSec) throws Exception {
        try (Session session = ssh.startSession()) {
            Session.Command cmd = session.exec(command);

            // Feed password via stdin for sudo -S
            OutputStream stdin = cmd.getOutputStream();
            stdin.write((password + "\n").getBytes(StandardCharsets.UTF_8));
            stdin.flush();
            stdin.close();

            cmd.join(timeoutSec, TimeUnit.SECONDS);

            String stdout = IOUtils.readFully(cmd.getInputStream()).toString();
            String stderr = IOUtils.readFully(cmd.getErrorStream()).toString();

            Integer exitCode = cmd.getExitStatus();
            if (exitCode == null) {
                log.warn("Command timed out ({}s): {}", timeoutSec, command.substring(0, Math.min(80, command.length())));
                throw new RuntimeException("命令执行超时 (" + timeoutSec + "秒): " + command.substring(0, Math.min(60, command.length())));
            }
            if (exitCode != 0) {
                String cleanStderr = stderr.replaceAll("\\[sudo\\] password for \\w+:", "").trim();
                String errorMsg = cleanStderr.isEmpty() ? stdout.trim() : cleanStderr;
                if (errorMsg.isEmpty()) {
                    errorMsg = "exit code " + exitCode;
                }
                log.warn("Command failed (exit={}): {} -> {}", exitCode, command.substring(0, Math.min(80, command.length())), errorMsg);
                throw new RuntimeException("命令执行失败: " + errorMsg);
            }
            return stdout;
        }
    }

    /**
     * Write content to a systemd service file using sudo.
     * Avoids the pipe-into-sudo-tee problem by writing to a temp file first,
     * then using sudo cp to move it to /etc/systemd/system/.
     * Password is fed via stdin to sudo -S to prevent command injection.
     */
    private void writeSudoFile(SSHClient ssh, String content, String remotePath, String password) throws Exception {
        String base64Content = Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8));
        String tmpFile = "/tmp/service_" + System.currentTimeMillis() + ".tmp";

        // Step 1: Decode base64 to temp file (no sudo needed for /tmp)
        execCmdChecked(ssh, "echo '" + base64Content + "' | base64 -d > " + escapeShellArg(tmpFile), password);

        // Step 2: Use sudo to copy temp file to target path (password via stdin)
        execCmdWithStdinPassword(ssh, "sudo -S cp " + escapeShellArg(tmpFile) + " " + escapeShellArg(remotePath) + " 2>&1", password);

        // Step 3: Set correct permissions and cleanup (password via stdin)
        execCmdWithStdinPassword(ssh, "sudo -S chmod 644 " + escapeShellArg(remotePath) + " 2>&1", password);
        execCmd(ssh, "rm -f " + escapeShellArg(tmpFile));
    }

    /**
     * Escape a string for safe use as a shell argument.
     * Wraps the argument in single quotes and escapes any embedded single quotes.
     */
    private static String escapeShellArg(String arg) {
        if (arg == null) return "''";
        // Replace each single quote with: end-quote, escaped-quote, start-quote
        return "'" + arg.replace("'", "'\\''") + "'";
    }

    private String resolveInstallDir(SSHClient ssh, String installDir, String sshUsername) throws Exception {
        if (installDir == null || installDir.isBlank()) {
            installDir = "~/prometheus";
        }
        if (installDir.startsWith("~")) {
            String homeDir = execCmd(ssh, "echo $HOME").trim();
            if (homeDir.isEmpty()) {
                homeDir = "/home/" + sshUsername;
            }
            installDir = installDir.replaceFirst("^~", homeDir);
        }
        return installDir;
    }

    /**
     * Sanitize error messages to prevent leaking sensitive information
     * such as SSH passwords, encryption keys, or credentials.
     */
    private String sanitizeErrorMessage(String msg) {
        if (msg == null || msg.isBlank()) {
            return "操作执行失败";
        }
        // Remove password-like patterns: password=xxx, passwd=xxx, pwd=xxx
        String sanitized = msg.replaceAll("(?i)(password|passwd|pwd|secret|token|key|credential)\\s*[=:]\\s*\\S+", "$1=***");
        // Remove authentication failure details that may echo back credentials
        sanitized = sanitized.replaceAll("(?i)auth.*fail.*:\\s*\\S+", "认证失败");
        // Remove any base64-encoded blocks that might contain credentials (long base64 strings > 20 chars)
        sanitized = sanitized.replaceAll("[A-Za-z0-9+/]{20,}={0,2}", "***");
        // Remove IP:port@password patterns from SSH connection strings
        sanitized = sanitized.replaceAll("@[^\\s]+", "@***");
        return sanitized;
    }

    private void appendLog(StringBuilder sb, String line) {
        sb.append("[").append(LocalDateTime.now().toString()).append("] ").append(line).append("\n");
    }

    private PrometheusConfigDTO extractPrometheusConfig(PromDistributeTask task) {
        if (task == null || task.getConfig() == null) return null;
        Object promCfg = task.getConfig().get("prometheusConfig");
        if (promCfg == null) return null;
        try {
            return objectMapper.convertValue(promCfg, PrometheusConfigDTO.class);
        } catch (Exception e) {
            log.warn("Failed to parse Prometheus config, using defaults: {}", e.getMessage());
            return new PrometheusConfigDTO();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, ComponentConfigDTO> extractComponentConfigs(PromDistributeTask task) {
        Map<String, ComponentConfigDTO> result = new HashMap<>();
        if (task == null || task.getConfig() == null) return result;
        Object compConfigs = task.getConfig().get("componentConfigs");
        if (compConfigs == null) return result;
        try {
            Map<String, Object> map = (Map<String, Object>) compConfigs;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                ComponentConfigDTO dto = objectMapper.convertValue(entry.getValue(), ComponentConfigDTO.class);
                result.put(entry.getKey(), dto);
            }
        } catch (Exception e) {
            log.warn("Failed to parse component configs: {}", e.getMessage());
        }
        return result;
    }

    private String generatePrometheusSystemdService(String installDir, PrometheusConfigDTO config, String sshUsername) {
        if (config == null) config = new PrometheusConfigDTO();
        String cliFlags = PrometheusYamlGenerator.buildCliFlags(config, installDir);

        return "[Unit]\n"
                + "Description=Prometheus Server\n"
                + "Documentation=https://prometheus.io/docs/\n"
                + "After=network-online.target\n"
                + "Wants=network-online.target\n"
                + "\n"
                + "[Service]\n"
                + "Type=simple\n"
                + "User=" + sshUsername + "\n"
                + "ExecStart=" + installDir + "/prometheus \\\n  " + cliFlags + "\n"
                + "ExecReload=/bin/kill -HUP $MAINPID\n"
                + "Restart=always\n"
                + "RestartSec=5\n"
                + "LimitNOFILE=65536\n"
                + "\n"
                + "[Install]\n"
                + "WantedBy=multi-user.target\n";
    }
}
