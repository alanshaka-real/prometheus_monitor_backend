package com.wenmin.prometheus.module.distribute.service.impl;

import com.wenmin.prometheus.module.distribute.service.SshService;
import com.wenmin.prometheus.module.distribute.vo.MachineDetectVO;
import lombok.extern.slf4j.Slf4j;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.IOUtils;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.xfer.FileSystemFile;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class SshServiceImpl implements SshService {

    private static final int CONNECT_TIMEOUT = 10000;
    private static final int COMMAND_TIMEOUT = 30;

    @Override
    public boolean testConnection(String host, int port, String username, String password) {
        try (SSHClient ssh = createClient(host, port, username, password)) {
            return true;
        } catch (Exception e) {
            log.warn("SSH connection test failed for {}:{} - {}", host, port, e.getMessage());
            return false;
        }
    }

    @Override
    public MachineDetectVO detectOS(String host, int port, String username, String password) {
        MachineDetectVO vo = new MachineDetectVO();
        try (SSHClient ssh = createClient(host, port, username, password)) {
            // Detect OS type
            String uname = execCommand(ssh, "uname -s").trim().toLowerCase();
            vo.setOsType(uname.contains("linux") ? "linux" : uname.contains("darwin") ? "darwin" : uname);

            // Detect architecture
            String arch = execCommand(ssh, "uname -m").trim();
            vo.setOsArch(normalizeArch(arch));

            // Detect distribution
            String distro = execCommand(ssh, "cat /etc/os-release 2>/dev/null | grep PRETTY_NAME | cut -d'\"' -f2").trim();
            if (distro.isEmpty()) {
                distro = execCommand(ssh, "cat /etc/redhat-release 2>/dev/null").trim();
            }
            if (distro.isEmpty()) {
                distro = vo.getOsType();
            }
            vo.setOsDistribution(distro);

            vo.setSuccess(true);
            vo.setMessage("检测成功");
        } catch (Exception e) {
            log.error("OS detection failed for {}:{} - {}", host, port, e.getMessage());
            vo.setSuccess(false);
            vo.setMessage("检测失败: " + e.getMessage());
        }
        return vo;
    }

    @Override
    public String executeCommand(String host, int port, String username, String password, String command) {
        try (SSHClient ssh = createClient(host, port, username, password)) {
            return execCommand(ssh, command);
        } catch (Exception e) {
            log.error("Command execution failed on {}:{} - {}", host, port, e.getMessage());
            throw new RuntimeException("命令执行失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void uploadFile(String host, int port, String username, String password,
                           String localPath, String remotePath) {
        try (SSHClient ssh = createClient(host, port, username, password)) {
            ssh.newSCPFileTransfer().upload(new FileSystemFile(new File(localPath)), remotePath);
            log.info("File uploaded to {}:{} - {} -> {}", host, port, localPath, remotePath);
        } catch (Exception e) {
            log.error("File upload failed to {}:{} - {}", host, port, e.getMessage());
            throw new RuntimeException("文件上传失败: " + e.getMessage(), e);
        }
    }

    public String execCommand(SSHClient ssh, String command) throws Exception {
        try (Session session = ssh.startSession()) {
            Session.Command cmd = session.exec(command);
            String output = IOUtils.readFully(cmd.getInputStream()).toString();
            cmd.join(COMMAND_TIMEOUT, TimeUnit.SECONDS);
            return output;
        }
    }

    public SSHClient createClient(String host, int port, String username, String password) throws Exception {
        SSHClient ssh = new SSHClient();
        ssh.addHostKeyVerifier(new PromiscuousVerifier());
        ssh.setConnectTimeout(CONNECT_TIMEOUT);
        ssh.connect(host, port);
        ssh.authPassword(username, password);
        return ssh;
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
