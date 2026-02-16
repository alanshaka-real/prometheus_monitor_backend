package com.wenmin.prometheus.module.distribute.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wenmin.prometheus.common.exception.BusinessException;
import com.wenmin.prometheus.module.distribute.entity.*;
import com.wenmin.prometheus.module.distribute.mapper.*;
import com.wenmin.prometheus.module.distribute.service.DistributeService;
import com.wenmin.prometheus.module.distribute.vo.ComponentDetectVO;
import com.wenmin.prometheus.module.distribute.vo.MachineDetectVO;
import com.wenmin.prometheus.util.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.IOUtils;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class DistributeServiceImpl implements DistributeService {

    private final PromDistributeMachineMapper machineMapper;
    private final PromDistributeTaskMapper taskMapper;
    private final PromDistributeTaskDetailMapper taskDetailMapper;
    private final DistributeTaskExecutor taskExecutor;

    @Value("${distribute.encryption-key}")
    private String encryptionKey;

    // ==================== Machine Management ====================

    @Override
    public Map<String, Object> listMachines(String status, String keyword) {
        LambdaQueryWrapper<PromDistributeMachine> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(status)) {
            wrapper.eq(PromDistributeMachine::getStatus, status);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(PromDistributeMachine::getName, keyword)
                    .or().like(PromDistributeMachine::getIp, keyword));
        }
        wrapper.orderByDesc(PromDistributeMachine::getCreatedAt);

        List<PromDistributeMachine> list = machineMapper.selectList(wrapper);
        // Remove password from output
        list.forEach(m -> m.setSshPassword(null));

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", list.size());
        return result;
    }

    @Override
    public PromDistributeMachine createMachine(PromDistributeMachine machine) {
        machine.setId(null);
        if (machine.getSshPort() == null) {
            machine.setSshPort(22);
        }
        if (machine.getStatus() == null) {
            machine.setStatus("unknown");
        }
        // Encrypt password
        if (StringUtils.hasText(machine.getSshPassword())) {
            machine.setSshPassword(EncryptionUtil.encrypt(machine.getSshPassword(), encryptionKey));
        }
        machine.setCreatedAt(LocalDateTime.now());
        machine.setUpdatedAt(LocalDateTime.now());
        machineMapper.insert(machine);

        machine.setSshPassword(null);
        return machine;
    }

    @Override
    public PromDistributeMachine updateMachine(String id, PromDistributeMachine machine) {
        PromDistributeMachine existing = machineMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("目标机器不存在");
        }
        machine.setId(id);
        // Encrypt password if provided
        if (StringUtils.hasText(machine.getSshPassword())) {
            machine.setSshPassword(EncryptionUtil.encrypt(machine.getSshPassword(), encryptionKey));
        } else {
            machine.setSshPassword(existing.getSshPassword());
        }
        machine.setUpdatedAt(LocalDateTime.now());
        machineMapper.updateById(machine);

        PromDistributeMachine updated = machineMapper.selectById(id);
        updated.setSshPassword(null);
        return updated;
    }

    @Override
    public void deleteMachine(String id) {
        PromDistributeMachine existing = machineMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("目标机器不存在");
        }
        machineMapper.deleteById(id);
    }

    @Override
    public MachineDetectVO detectMachine(String id) {
        PromDistributeMachine machine = machineMapper.selectById(id);
        if (machine == null) {
            throw new BusinessException("目标机器不存在");
        }

        String password = decryptPassword(machine.getSshPassword());
        MachineDetectVO vo = detectOS(machine.getIp(), machine.getSshPort(),
                machine.getSshUsername(), password);

        vo.setMachineId(machine.getId());
        vo.setMachineName(machine.getName());
        vo.setMachineIp(machine.getIp());

        if (vo.isSuccess()) {
            machine.setOsType(vo.getOsType());
            machine.setOsArch(vo.getOsArch());
            machine.setOsDistribution(vo.getOsDistribution());
            machine.setStatus("online");
            machine.setLastCheckedAt(LocalDateTime.now());
            machine.setUpdatedAt(LocalDateTime.now());
            machineMapper.updateById(machine);
        }
        return vo;
    }

    @Override
    public boolean testSshConnection(String ip, Integer port, String username, String password) {
        try {
            SSHClient ssh = new SSHClient();
            ssh.addHostKeyVerifier(new PromiscuousVerifier());
            ssh.setConnectTimeout(10000);
            ssh.connect(ip, port != null ? port : 22);
            ssh.authPassword(username, password);
            ssh.close();
            return true;
        } catch (Exception e) {
            log.warn("SSH connection test failed for {}:{} - {}", ip, port, e.getMessage());
            return false;
        }
    }

    @Override
    public Map<String, Object> testMachineConnection(String machineId) {
        PromDistributeMachine machine = machineMapper.selectById(machineId);
        if (machine == null) {
            throw new BusinessException("目标机器不存在");
        }
        String password = decryptPassword(machine.getSshPassword());
        boolean success = testSshConnection(machine.getIp(), machine.getSshPort(),
                machine.getSshUsername(), password);

        if (success) {
            machine.setStatus("online");
            machine.setLastCheckedAt(LocalDateTime.now());
            machine.setUpdatedAt(LocalDateTime.now());
            machineMapper.updateById(machine);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("message", success ? "SSH 连接成功" : "SSH 连接失败");
        return result;
    }

    @Override
    public List<MachineDetectVO> batchDetect(List<String> machineIds) {
        List<MachineDetectVO> results = new ArrayList<>();
        for (String id : machineIds) {
            try {
                results.add(detectMachine(id));
            } catch (Exception e) {
                PromDistributeMachine machine = machineMapper.selectById(id);
                MachineDetectVO vo = new MachineDetectVO();
                vo.setSuccess(false);
                vo.setMessage("检测失败: " + e.getMessage());
                if (machine != null) {
                    vo.setMachineId(machine.getId());
                    vo.setMachineName(machine.getName());
                    vo.setMachineIp(machine.getIp());
                }
                results.add(vo);
            }
        }
        return results;
    }

    // ==================== Task Management ====================

    @Override
    @SuppressWarnings("unchecked")
    public PromDistributeTask createTask(Map<String, Object> request) {
        PromDistributeTask task = new PromDistributeTask();
        task.setName((String) request.get("name"));
        task.setMode((String) request.getOrDefault("mode", "batch_unified"));
        task.setComponents((List<String>) request.get("components"));
        task.setConfig((Map<String, Object>) request.get("config"));
        task.setStatus("pending");
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());

        List<Map<String, Object>> machines = (List<Map<String, Object>>) request.get("machines");
        task.setMachineCount(machines.size());
        task.setSuccessCount(0);
        task.setFailCount(0);
        taskMapper.insert(task);

        // Create task details for each machine
        for (Map<String, Object> machineInfo : machines) {
            String machineId = (String) machineInfo.get("machineId");
            PromDistributeMachine machine = machineMapper.selectById(machineId);
            if (machine == null) continue;

            PromDistributeTaskDetail detail = new PromDistributeTaskDetail();
            detail.setTaskId(task.getId());
            detail.setMachineId(machineId);
            detail.setMachineIp(machine.getIp());
            detail.setComponents(task.getComponents());
            detail.setComponentConfig((Map<String, Object>) machineInfo.get("componentConfig"));
            detail.setStatus("pending");
            detail.setProgress(0);
            detail.setCreatedAt(LocalDateTime.now());
            detail.setUpdatedAt(LocalDateTime.now());
            taskDetailMapper.insert(detail);
        }

        // FIX: Delegate to separate bean so @Async proxy works correctly
        taskExecutor.executeTaskAsync(task.getId());

        return task;
    }

    @Override
    public Map<String, Object> listTasks(String status) {
        LambdaQueryWrapper<PromDistributeTask> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(status)) {
            wrapper.eq(PromDistributeTask::getStatus, status);
        }
        wrapper.orderByDesc(PromDistributeTask::getCreatedAt);

        List<PromDistributeTask> list = taskMapper.selectList(wrapper);
        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", list.size());
        return result;
    }

    @Override
    public PromDistributeTask getTask(String id) {
        PromDistributeTask task = taskMapper.selectById(id);
        if (task == null) {
            throw new BusinessException("分发任务不存在");
        }
        return task;
    }

    @Override
    public List<PromDistributeTaskDetail> getTaskDetails(String taskId) {
        LambdaQueryWrapper<PromDistributeTaskDetail> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PromDistributeTaskDetail::getTaskId, taskId)
                .orderByAsc(PromDistributeTaskDetail::getCreatedAt);
        return taskDetailMapper.selectList(wrapper);
    }

    @Override
    public void cancelTask(String taskId) {
        PromDistributeTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException("分发任务不存在");
        }
        taskExecutor.markCancelled(taskId);
        task.setStatus("cancelled");
        task.setFinishedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        taskMapper.updateById(task);

        // Cancel pending details
        LambdaQueryWrapper<PromDistributeTaskDetail> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PromDistributeTaskDetail::getTaskId, taskId)
                .eq(PromDistributeTaskDetail::getStatus, "pending");
        List<PromDistributeTaskDetail> pendingDetails = taskDetailMapper.selectList(wrapper);
        for (PromDistributeTaskDetail detail : pendingDetails) {
            detail.setStatus("cancelled");
            detail.setUpdatedAt(LocalDateTime.now());
            taskDetailMapper.updateById(detail);
        }
    }

    @Override
    public void retryTaskDetail(String taskId, String detailId) {
        PromDistributeTaskDetail detail = taskDetailMapper.selectById(detailId);
        if (detail == null || !detail.getTaskId().equals(taskId)) {
            throw new BusinessException("任务明细不存在");
        }
        if (!"failed".equals(detail.getStatus())) {
            throw new BusinessException("只能重试失败的任务");
        }

        detail.setStatus("pending");
        detail.setProgress(0);
        detail.setCurrentStep(null);
        detail.setLogText(null);
        detail.setErrorMessage(null);
        detail.setStartedAt(null);
        detail.setFinishedAt(null);
        detail.setUpdatedAt(LocalDateTime.now());
        taskDetailMapper.updateById(detail);

        // FIX: Delegate to separate bean so @Async proxy works correctly
        taskExecutor.executeDetailAsync(taskId, detailId);
    }

    // ==================== Helper Methods ====================

    private String decryptPassword(String encrypted) {
        if (encrypted == null || encrypted.isBlank()) return "";
        try {
            return EncryptionUtil.decrypt(encrypted, encryptionKey);
        } catch (Exception e) {
            log.error("Password decryption failed", e);
            throw new BusinessException("SSH 密码解密失败");
        }
    }

    private MachineDetectVO detectOS(String host, int port, String username, String password) {
        MachineDetectVO vo = new MachineDetectVO();
        try {
            SSHClient ssh = new SSHClient();
            ssh.addHostKeyVerifier(new PromiscuousVerifier());
            ssh.setConnectTimeout(10000);
            ssh.connect(host, port);
            ssh.authPassword(username, password);

            // OS type
            String uname = execCmd(ssh, "uname -s").trim().toLowerCase();
            vo.setOsType(uname.contains("linux") ? "linux" : uname.contains("darwin") ? "darwin" : uname);

            // Architecture
            String arch = execCmd(ssh, "uname -m").trim();
            vo.setOsArch(normalizeArch(arch));

            // Distribution
            String distro = execCmd(ssh, "cat /etc/os-release 2>/dev/null | grep PRETTY_NAME | cut -d'\"' -f2").trim();
            if (distro.isEmpty()) {
                distro = execCmd(ssh, "cat /etc/redhat-release 2>/dev/null").trim();
            }
            if (distro.isEmpty()) {
                distro = vo.getOsType();
            }
            vo.setOsDistribution(distro);

            // Hostname
            vo.setHostname(execCmd(ssh, "hostname").trim());

            // Kernel version
            vo.setKernelVersion(execCmd(ssh, "uname -r").trim());

            // CPU cores
            try {
                String cores = execCmd(ssh, "nproc 2>/dev/null || grep -c ^processor /proc/cpuinfo").trim();
                vo.setCpuCores(Integer.parseInt(cores));
            } catch (Exception ignored) {}

            // CPU model
            String cpuModel = execCmd(ssh, "grep 'model name' /proc/cpuinfo 2>/dev/null | head -1 | cut -d':' -f2").trim();
            if (cpuModel.isEmpty()) {
                cpuModel = execCmd(ssh, "lscpu 2>/dev/null | grep 'Model name' | cut -d':' -f2").trim();
            }
            vo.setCpuModel(cpuModel.isEmpty() ? null : cpuModel);

            // Memory total
            String memKb = execCmd(ssh, "grep MemTotal /proc/meminfo 2>/dev/null | awk '{print $2}'").trim();
            if (!memKb.isEmpty()) {
                try {
                    long kb = Long.parseLong(memKb);
                    if (kb >= 1048576) {
                        vo.setMemoryTotal(String.format("%.1f GB", kb / 1048576.0));
                    } else {
                        vo.setMemoryTotal(String.format("%.0f MB", kb / 1024.0));
                    }
                } catch (NumberFormatException ignored) {}
            }

            // Disk info (root partition)
            String dfLine = execCmd(ssh, "df -h / 2>/dev/null | tail -1").trim();
            if (!dfLine.isEmpty()) {
                String[] parts = dfLine.split("\\s+");
                if (parts.length >= 4) {
                    vo.setDiskTotal(parts[1]);
                    vo.setDiskUsed(parts[2]);
                    vo.setDiskAvail(parts[3]);
                }
            }

            // Uptime
            vo.setUptime(execCmd(ssh, "uptime -p 2>/dev/null || uptime | sed 's/.*up /up /' | sed 's/,.*//'"  ).trim());

            // Component detection
            vo.setComponents(detectComponents(ssh));

            vo.setSuccess(true);
            vo.setMessage("检测成功");
            ssh.close();
        } catch (Exception e) {
            vo.setSuccess(false);
            vo.setMessage("检测失败: " + e.getMessage());
        }
        return vo;
    }

    private List<ComponentDetectVO> detectComponents(SSHClient ssh) {
        List<ComponentDetectVO> components = new ArrayList<>();
        String[][] componentDefs = {
            {"prometheus", "Prometheus Server", "prometheus", "9090"},
            {"node_exporter", "Node Exporter", "node_exporter", "9100"},
            {"alertmanager", "Alertmanager", "alertmanager", "9093"},
            {"blackbox_exporter", "Blackbox Exporter", "blackbox_exporter", "9115"},
            {"pushgateway", "Pushgateway", "pushgateway", "9091"},
            {"grafana", "Grafana", "grafana-server", "3000"}
        };

        for (String[] def : componentDefs) {
            String name = def[0];
            String displayName = def[1];
            String binary = def[2];
            int defaultPort = Integer.parseInt(def[3]);

            ComponentDetectVO comp = new ComponentDetectVO();
            comp.setName(name);
            comp.setDisplayName(displayName);
            comp.setPort(defaultPort);
            comp.setRunningStatus("unknown");

            try {
                // 1. Find binary
                String binaryPath = execCmd(ssh, "which " + binary + " 2>/dev/null").trim();
                if (binaryPath.isEmpty()) {
                    String findCmd = "grafana-server".equals(binary)
                        ? "find /usr -maxdepth 4 -name " + binary + " -type f 2>/dev/null | head -1"
                        : "find ~ -maxdepth 4 -name " + binary + " -type f 2>/dev/null | head -1";
                    binaryPath = execCmd(ssh, findCmd).trim();
                }

                if (binaryPath.isEmpty()) {
                    comp.setInstalled(false);
                    comp.setRunningStatus("unknown");
                    components.add(comp);
                    continue;
                }

                comp.setInstalled(true);

                // 2. Install path (parent directory of binary)
                String installDir = execCmd(ssh, "dirname \"" + binaryPath + "\"").trim();
                comp.setInstallPath(installDir);

                // 3. Check systemd service
                String serviceCheck = execCmd(ssh, "systemctl list-unit-files " + name + ".service 2>/dev/null | grep " + name).trim();
                comp.setServiceExists(!serviceCheck.isEmpty());

                // 4. Running status
                String activeStatus = execCmd(ssh, "systemctl is-active " + name + ".service 2>/dev/null").trim();
                if ("active".equals(activeStatus)) {
                    comp.setRunningStatus("running");
                } else if ("inactive".equals(activeStatus)) {
                    comp.setRunningStatus("stopped");
                } else {
                    // Also check via process
                    String psCheck = execCmd(ssh, "pgrep -x " + binary + " 2>/dev/null").trim();
                    if (!psCheck.isEmpty()) {
                        comp.setRunningStatus("running");
                    } else {
                        comp.setRunningStatus("stopped");
                    }
                }

                // 5. Version
                try {
                    String versionOutput = execCmd(ssh, "\"" + binaryPath + "\" --version 2>&1 | head -1").trim();
                    // Extract version number pattern like x.y.z
                    java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(\\d+\\.\\d+\\.\\d+)").matcher(versionOutput);
                    if (matcher.find()) {
                        comp.setVersion(matcher.group(1));
                    } else if (!versionOutput.isEmpty()) {
                        comp.setVersion(versionOutput);
                    }
                } catch (Exception ignored) {}

                // 6. Check listening port
                String portCheck = execCmd(ssh, "ss -tlnp 2>/dev/null | grep :" + defaultPort).trim();
                if (portCheck.isEmpty()) {
                    comp.setPort(null); // Port not listening
                }

            } catch (Exception e) {
                log.warn("Failed to detect component {}: {}", name, e.getMessage());
                comp.setInstalled(false);
                comp.setRunningStatus("unknown");
            }

            components.add(comp);
        }
        return components;
    }

    private String execCmd(SSHClient ssh, String command) throws Exception {
        try (Session session = ssh.startSession()) {
            Session.Command cmd = session.exec(command);
            cmd.join(30, TimeUnit.SECONDS);
            String output = IOUtils.readFully(cmd.getInputStream()).toString();
            return output;
        }
    }

    private String normalizeArch(String arch) {
        return switch (arch) {
            case "x86_64", "amd64" -> "amd64";
            case "aarch64", "arm64" -> "arm64";
            case "armv7l", "armhf" -> "armv7";
            default -> arch;
        };
    }
}
