package com.wenmin.prometheus.module.distribute.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wenmin.prometheus.module.distribute.config.GitHubReleaseConfig;
import com.wenmin.prometheus.module.distribute.config.GitHubReleaseConfig.ComponentConfig;
import com.wenmin.prometheus.module.distribute.config.GitHubReleaseConfig.NamingStrategy;
import com.wenmin.prometheus.module.distribute.entity.PromDistributeSoftware;
import com.wenmin.prometheus.module.distribute.mapper.PromDistributeSoftwareMapper;
import com.wenmin.prometheus.module.distribute.vo.SoftwareDownloadVO;
import com.wenmin.prometheus.module.distribute.vo.SoftwareDownloadVO.ComponentDownloadResult;
import com.wenmin.prometheus.websocket.DistributeWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class SoftwareDownloadExecutor {

    private final RestTemplate restTemplate;
    private final PromDistributeSoftwareMapper softwareMapper;
    private final DistributeWebSocketHandler webSocketHandler;

    @Value("${distribute.software-path}")
    private String softwarePath;

    @Value("${distribute.github-token:}")
    private String githubToken;

    private final ConcurrentHashMap<String, SoftwareDownloadVO> downloadSessions = new ConcurrentHashMap<>();

    public SoftwareDownloadVO getStatus(String downloadId) {
        return downloadSessions.get(downloadId);
    }

    @Async("distributeTaskExecutor")
    public void executeDownload(String downloadId, List<String> components) {
        SoftwareDownloadVO vo = new SoftwareDownloadVO();
        vo.setDownloadId(downloadId);
        vo.setStatus("checking");
        vo.setTotalComponents(components.size());
        downloadSessions.put(downloadId, vo);

        sendLog(downloadId, "info", "开始检查更新，共 " + components.size() + " 个组件");

        // ═══ Phase 1: Check versions against GitHub ═══
        record VersionCheck(ComponentConfig config, String tagName, String latestVersion,
                            String currentVersion, boolean needsUpdate) {}
        List<VersionCheck> checks = new ArrayList<>();

        for (String componentName : components) {
            ComponentConfig config = GitHubReleaseConfig.COMPONENTS.get(componentName);
            if (config == null) {
                sendLog(downloadId, "warn", "未知组件: " + componentName + "，跳过");
                continue;
            }

            vo.setCurrentComponent(config.displayName());
            sendLog(downloadId, "info", "[" + config.displayName() + "] 查询最新版本...");

            ComponentDownloadResult result = new ComponentDownloadResult();
            result.setName(config.localName());
            result.setDisplayName(config.displayName());

            Map<String, Object> releaseInfo = fetchLatestRelease(config);
            if (releaseInfo == null) {
                result.setStatus("failed");
                result.setNewVersion(null);
                sendLog(downloadId, "error", "[" + config.displayName() + "] 获取最新版本失败");
                vo.getResults().add(result);
                vo.setCompletedComponents(vo.getCompletedComponents() + 1);
                continue;
            }

            String tagName = (String) releaseInfo.get("tag_name");
            String latestVersion = GitHubReleaseConfig.parseVersion(tagName);
            String currentVersion = getCurrentVersion(config.localName());

            result.setNewVersion(latestVersion);
            result.setPreviousVersion(currentVersion);

            if (latestVersion.equals(currentVersion)) {
                result.setStatus("skipped");
                sendLog(downloadId, "info", "[" + config.displayName() + "] v" + currentVersion + " 已是最新版本");
                vo.getResults().add(result);
                vo.setCompletedComponents(vo.getCompletedComponents() + 1);
                vo.setSkippedComponents(vo.getSkippedComponents() + 1);
                vo.setSkippedFiles(vo.getSkippedFiles() + config.architectures().size());
                checks.add(new VersionCheck(config, tagName, latestVersion, currentVersion, false));
            } else {
                String msg = currentVersion == null
                        ? "[" + config.displayName() + "] 本地无记录 → v" + latestVersion
                        : "[" + config.displayName() + "] v" + currentVersion + " → v" + latestVersion + " 需要更新";
                sendLog(downloadId, "info", msg);
                checks.add(new VersionCheck(config, tagName, latestVersion, currentVersion, true));
            }
        }

        // Count components that actually need downloading
        List<VersionCheck> toDownload = checks.stream().filter(VersionCheck::needsUpdate).toList();

        // ═══ All up to date? Finish early ═══
        if (toDownload.isEmpty()) {
            vo.setStatus("completed");
            vo.setCurrentComponent(null);
            vo.setTotalFiles(0);
            vo.setMessage("所有组件均已是最新版本，无需更新");
            sendLog(downloadId, "info", "━━━ 所有组件均已是最新版本 ━━━");
            sendStatus(downloadId, "completed", vo.getMessage());
            scheduleCleanup(downloadId);
            return;
        }

        // ═══ Phase 2: Download only components that need updating ═══
        int totalFiles = toDownload.stream().mapToInt(c -> c.config().architectures().size()).sum();
        vo.setTotalFiles(totalFiles);
        vo.setStatus("running");
        sendLog(downloadId, "info", "━━━ 开始下载 " + toDownload.size() + " 个组件（共 " + totalFiles + " 个文件）━━━");

        int downloadedFiles = 0;
        int failedFiles = 0;

        for (VersionCheck check : toDownload) {
            ComponentConfig config = check.config();
            String tagName = check.tagName();
            String version = check.latestVersion();

            vo.setCurrentComponent(config.displayName());
            sendLog(downloadId, "info", "━━━ 下载 " + config.displayName() + " v" + version + " ━━━");

            ComponentDownloadResult result = new ComponentDownloadResult();
            result.setName(config.localName());
            result.setDisplayName(config.displayName());
            result.setPreviousVersion(check.currentVersion());
            result.setNewVersion(version);

            try {
                File componentDir = new File(softwarePath, config.localName());
                if (!componentDir.exists()) {
                    componentDir.mkdirs();
                }

                int compDownloaded = 0;
                int compFailed = 0;

                for (String arch : config.architectures()) {
                    String localFileName = GitHubReleaseConfig.buildLocalFileName(config, version, arch);
                    vo.setCurrentFile(localFileName);

                    try {
                        String downloadUrl = GitHubReleaseConfig.buildDownloadUrl(config, tagName, version, arch);
                        File targetFile = new File(componentDir, localFileName);

                        if (config.namingStrategy() == NamingStrategy.CADVISOR) {
                            downloadCadvisor(downloadUrl, targetFile, config, version, arch);
                        } else {
                            downloadFile(downloadUrl, targetFile);
                        }

                        compDownloaded++;
                        downloadedFiles++;
                        sendLog(downloadId, "info", "  [OK] " + localFileName);
                    } catch (Exception e) {
                        compFailed++;
                        failedFiles++;
                        sendLog(downloadId, "warn", "  [FAIL] " + localFileName + ": " + e.getMessage());
                        log.warn("Failed to download {} {}: {}", config.localName(), arch, e.getMessage());
                    }

                    vo.setDownloadedFiles(downloadedFiles);
                    vo.setFailedFiles(failedFiles);
                    updateProgress(downloadId, vo);
                }

                result.setFilesDownloaded(compDownloaded);
                result.setFilesFailed(compFailed);

                if (compFailed == 0) {
                    result.setStatus("success");
                } else if (compDownloaded > 0) {
                    result.setStatus("partial");
                } else {
                    result.setStatus("failed");
                }

                // Clean up old version files and database records
                if (compDownloaded > 0) {
                    cleanupOldVersions(config.localName(), version, componentDir);
                    sendLog(downloadId, "info", "[" + config.displayName() + "] 旧版本已清理");

                    int registered = registerNewFiles(config, version, componentDir);
                    sendLog(downloadId, "info", "[" + config.displayName() + "] 注册 " + registered + " 个新文件");
                }

            } catch (Exception e) {
                result.setStatus("failed");
                sendLog(downloadId, "error", "[" + config.displayName() + "] 处理异常: " + e.getMessage());
                log.error("Error processing component {}", config.localName(), e);
            }

            vo.getResults().add(result);
            vo.setCompletedComponents(vo.getCompletedComponents() + 1);
        }

        // ═══ Finalize ═══
        vo.setCurrentComponent(null);
        vo.setCurrentFile(null);
        vo.setDownloadedFiles(downloadedFiles);
        vo.setFailedFiles(failedFiles);

        if (failedFiles == 0) {
            vo.setStatus("completed");
            vo.setMessage("更新完成，下载 " + downloadedFiles + " 个文件");
        } else if (downloadedFiles > 0) {
            vo.setStatus("completed");
            vo.setMessage("更新完成，成功 " + downloadedFiles + " / 失败 " + failedFiles);
        } else {
            vo.setStatus("failed");
            vo.setMessage("更新失败");
        }

        sendLog(downloadId, "info", "━━━ 更新完成 ━━━");
        sendLog(downloadId, "info",
                "需更新: " + toDownload.size() + " 组件, 已跳过: " + vo.getSkippedComponents()
                        + " 组件, 下载成功: " + downloadedFiles + ", 失败: " + failedFiles);
        sendStatus(downloadId, vo.getStatus(), vo.getMessage());
        scheduleCleanup(downloadId);
    }

    private void scheduleCleanup(String downloadId) {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                downloadSessions.remove(downloadId);
            }
        }, 600_000);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchLatestRelease(ComponentConfig config) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", "application/vnd.github.v3+json");
            headers.set("User-Agent", "PrometheusMonitor");
            if (githubToken != null && !githubToken.isBlank()) {
                headers.set("Authorization", "Bearer " + githubToken);
            }

            String url = GitHubReleaseConfig.getApiUrl(config);
            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to fetch latest release for {}/{}: {}",
                    config.owner(), config.repo(), e.getMessage());
            return null;
        }
    }

    private String getCurrentVersion(String componentName) {
        LambdaQueryWrapper<PromDistributeSoftware> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PromDistributeSoftware::getName, componentName)
                .orderByDesc(PromDistributeSoftware::getVersion)
                .last("LIMIT 1");
        PromDistributeSoftware software = softwareMapper.selectOne(wrapper);
        return software != null ? software.getVersion() : null;
    }

    private void downloadFile(String url, File targetFile) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
        conn.setInstanceFollowRedirects(true);
        conn.setRequestProperty("User-Agent", "PrometheusMonitor");
        conn.setConnectTimeout(30_000);
        conn.setReadTimeout(300_000);

        // Handle GitHub redirects (302 → CDN)
        int status = conn.getResponseCode();
        if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM
                || status == 307 || status == 303) {
            String redirectUrl = conn.getHeaderField("Location");
            conn.disconnect();
            conn = (HttpURLConnection) URI.create(redirectUrl).toURL().openConnection();
            conn.setRequestProperty("User-Agent", "PrometheusMonitor");
            conn.setConnectTimeout(30_000);
            conn.setReadTimeout(300_000);
        }

        try (InputStream in = conn.getInputStream()) {
            Files.copy(in, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } finally {
            conn.disconnect();
        }
    }

    private void downloadCadvisor(String binaryUrl, File targetFile,
                                   ComponentConfig config, String version, String arch) throws IOException {
        // Download binary to temp dir
        Path tmpDir = Files.createTempDirectory("cadvisor-");
        try {
            String dirName = "cadvisor-" + version + ".linux-" + arch;
            Path innerDir = tmpDir.resolve(dirName);
            Files.createDirectories(innerDir);

            Path binaryFile = innerDir.resolve("cadvisor");
            downloadFile(binaryUrl, binaryFile.toFile());
            binaryFile.toFile().setExecutable(true);

            // Package into tar.gz
            ProcessBuilder pb = new ProcessBuilder(
                    "tar", "-czf", targetFile.getAbsolutePath(), "-C", tmpDir.toString(), dirName
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException("tar command failed with exit code " + exitCode);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("tar packaging interrupted", e);
        } finally {
            // Clean up temp directory
            deleteDirectory(tmpDir.toFile());
        }
    }

    private void deleteDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    deleteDirectory(f);
                } else {
                    f.delete();
                }
            }
        }
        dir.delete();
    }

    private void cleanupOldVersions(String componentName, String newVersion, File componentDir) {
        // Delete old files from disk
        File[] files = componentDir.listFiles((dir, name) ->
                name.startsWith(componentName + "-") && name.endsWith(".tar.gz")
                        && !name.contains("-" + newVersion + "."));
        if (files != null) {
            for (File f : files) {
                if (f.delete()) {
                    log.info("Deleted old version file: {}", f.getName());
                }
            }
        }

        // Delete old database records
        LambdaQueryWrapper<PromDistributeSoftware> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PromDistributeSoftware::getName, componentName)
                .ne(PromDistributeSoftware::getVersion, newVersion);
        softwareMapper.delete(wrapper);
    }

    private int registerNewFiles(ComponentConfig config, String version, File componentDir) {
        int count = 0;
        File[] files = componentDir.listFiles((dir, name) ->
                name.startsWith(config.localName() + "-" + version) && name.endsWith(".tar.gz"));
        if (files == null) return 0;

        Map<String, String> displayNames = Map.of(
                "node_exporter", "Node Exporter",
                "blackbox_exporter", "Blackbox Exporter",
                "process_exporter", "Process Exporter",
                "cadvisor", "cAdvisor",
                "mysql_exporter", "MySQL Exporter",
                "redis_exporter", "Redis Exporter",
                "nginx_exporter", "Nginx Exporter",
                "prometheus", "Prometheus Server"
        );
        Map<String, Integer> defaultPorts = Map.of(
                "node_exporter", 9100,
                "blackbox_exporter", 9115,
                "process_exporter", 9256,
                "cadvisor", 8080,
                "mysql_exporter", 9104,
                "redis_exporter", 9121,
                "nginx_exporter", 9113,
                "prometheus", 9090
        );

        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                "^(.+?)-(\\d+[\\d.]+\\d+)\\.([a-z]+)-(\\w+)\\.tar\\.gz$");

        for (File file : files) {
            java.util.regex.Matcher matcher = pattern.matcher(file.getName());
            if (!matcher.matches()) continue;

            String name = matcher.group(1);
            String ver = matcher.group(2);
            String osType = matcher.group(3);
            String osArch = matcher.group(4);

            // Check if already registered
            LambdaQueryWrapper<PromDistributeSoftware> check = new LambdaQueryWrapper<>();
            check.eq(PromDistributeSoftware::getName, name)
                    .eq(PromDistributeSoftware::getVersion, ver)
                    .eq(PromDistributeSoftware::getOsType, osType)
                    .eq(PromDistributeSoftware::getOsArch, osArch);
            if (softwareMapper.selectCount(check) > 0) continue;

            PromDistributeSoftware software = new PromDistributeSoftware();
            software.setName(name);
            software.setDisplayName(displayNames.getOrDefault(name, name));
            software.setVersion(ver);
            software.setOsType(osType);
            software.setOsArch(osArch);
            software.setFileName(file.getName());
            software.setFileSize(file.length());
            software.setDefaultPort(defaultPorts.get(name));
            software.setCreatedAt(LocalDateTime.now());
            software.setUpdatedAt(LocalDateTime.now());
            softwareMapper.insert(software);
            count++;
        }
        return count;
    }

    private void sendLog(String downloadId, String level, String message) {
        webSocketHandler.sendLog(downloadId, "", "", message, level);
    }

    private void sendStatus(String downloadId, String status, String message) {
        webSocketHandler.sendStatus(downloadId, "", "", status, message);
    }

    private void updateProgress(String downloadId, SoftwareDownloadVO vo) {
        int totalFiles = vo.getTotalFiles();
        int processed = vo.getDownloadedFiles() + vo.getFailedFiles();
        int progress = totalFiles > 0 ? (processed * 100 / totalFiles) : 0;
        webSocketHandler.sendProgress(downloadId, "", "",
                progress, "下载中: " + vo.getCurrentFile(), vo.getCurrentComponent());
    }
}
