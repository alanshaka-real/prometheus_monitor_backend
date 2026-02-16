package com.wenmin.prometheus.module.distribute.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wenmin.prometheus.common.exception.BusinessException;
import com.wenmin.prometheus.module.distribute.config.GitHubReleaseConfig;
import com.wenmin.prometheus.module.distribute.entity.PromDistributeSoftware;
import com.wenmin.prometheus.module.distribute.mapper.PromDistributeSoftwareMapper;
import com.wenmin.prometheus.module.distribute.service.SoftwareService;
import com.wenmin.prometheus.module.distribute.vo.SoftwareDownloadVO;
import com.wenmin.prometheus.module.distribute.vo.SoftwareUploadVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class SoftwareServiceImpl implements SoftwareService {

    private final PromDistributeSoftwareMapper softwareMapper;
    private final SoftwareDownloadExecutor downloadExecutor;

    @Value("${distribute.software-path}")
    private String softwarePath;

    // Pattern: {name}-{version}.{os}-{arch}.tar.gz
    private static final Pattern FILE_PATTERN =
            Pattern.compile("^(.+?)-(\\d+[\\d.]+\\d+)\\.([a-z]+)-(\\w+)\\.tar\\.gz$");

    private static final Map<String, String> DISPLAY_NAMES = Map.of(
            "node_exporter", "Node Exporter",
            "blackbox_exporter", "Blackbox Exporter",
            "process_exporter", "Process Exporter",
            "cadvisor", "cAdvisor",
            "mysql_exporter", "MySQL Exporter",
            "redis_exporter", "Redis Exporter",
            "nginx_exporter", "Nginx Exporter",
            "prometheus", "Prometheus Server"
    );

    private static final Map<String, Integer> DEFAULT_PORTS = Map.of(
            "node_exporter", 9100,
            "blackbox_exporter", 9115,
            "process_exporter", 9256,
            "cadvisor", 8080,
            "mysql_exporter", 9104,
            "redis_exporter", 9121,
            "nginx_exporter", 9113,
            "prometheus", 9090
    );

    @Override
    public Map<String, Object> listSoftware(String name, String osType, String osArch) {
        LambdaQueryWrapper<PromDistributeSoftware> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(name)) {
            wrapper.like(PromDistributeSoftware::getName, name);
        }
        if (StringUtils.hasText(osType)) {
            wrapper.eq(PromDistributeSoftware::getOsType, osType);
        }
        if (StringUtils.hasText(osArch)) {
            wrapper.eq(PromDistributeSoftware::getOsArch, osArch);
        }
        wrapper.orderByAsc(PromDistributeSoftware::getName)
                .orderByDesc(PromDistributeSoftware::getVersion);

        List<PromDistributeSoftware> list = softwareMapper.selectList(wrapper);
        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", list.size());
        return result;
    }

    @Override
    public int scanDirectory() {
        File baseDir = new File(softwarePath);
        if (!baseDir.exists() || !baseDir.isDirectory()) {
            log.warn("Software directory does not exist: {}", softwarePath);
            return 0;
        }

        int count = 0;
        File[] componentDirs = baseDir.listFiles(File::isDirectory);
        if (componentDirs == null) return 0;

        for (File componentDir : componentDirs) {
            File[] files = componentDir.listFiles((dir, name) -> name.endsWith(".tar.gz"));
            if (files == null) continue;

            for (File file : files) {
                Matcher matcher = FILE_PATTERN.matcher(file.getName());
                if (!matcher.matches()) {
                    log.debug("Skipping file with unrecognized pattern: {}", file.getName());
                    continue;
                }

                String name = matcher.group(1);
                String version = matcher.group(2);
                String osType = matcher.group(3);
                String osArch = matcher.group(4);

                // Check if already registered
                LambdaQueryWrapper<PromDistributeSoftware> check = new LambdaQueryWrapper<>();
                check.eq(PromDistributeSoftware::getName, name)
                        .eq(PromDistributeSoftware::getVersion, version)
                        .eq(PromDistributeSoftware::getOsType, osType)
                        .eq(PromDistributeSoftware::getOsArch, osArch);

                if (softwareMapper.selectCount(check) > 0) {
                    continue;
                }

                PromDistributeSoftware software = new PromDistributeSoftware();
                software.setName(name);
                software.setDisplayName(DISPLAY_NAMES.getOrDefault(name, name));
                software.setVersion(version);
                software.setOsType(osType);
                software.setOsArch(osArch);
                software.setFileName(file.getName());
                software.setFileSize(file.length());
                software.setDefaultPort(DEFAULT_PORTS.get(name));
                software.setCreatedAt(LocalDateTime.now());
                software.setUpdatedAt(LocalDateTime.now());

                softwareMapper.insert(software);
                count++;
                log.info("Registered software: {} v{} ({}/{})", name, version, osType, osArch);
            }
        }

        log.info("Software scan completed, {} new packages registered", count);
        return count;
    }

    @Override
    public PromDistributeSoftware findSoftware(String name, String osType, String osArch) {
        LambdaQueryWrapper<PromDistributeSoftware> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PromDistributeSoftware::getName, name)
                .eq(PromDistributeSoftware::getOsType, osType)
                .eq(PromDistributeSoftware::getOsArch, osArch)
                .orderByDesc(PromDistributeSoftware::getVersion)
                .last("LIMIT 1");
        return softwareMapper.selectOne(wrapper);
    }

    @Override
    public String getSoftwareFilePath(PromDistributeSoftware software) {
        return softwarePath + "/" + software.getName() + "/" + software.getFileName();
    }

    @Override
    public String downloadLatest(List<String> components) {
        // Validate component names
        for (String name : components) {
            if (!GitHubReleaseConfig.COMPONENTS.containsKey(name)) {
                throw new BusinessException("未知组件: " + name);
            }
        }
        String downloadId = UUID.randomUUID().toString().replace("-", "");
        downloadExecutor.executeDownload(downloadId, components);
        return downloadId;
    }

    @Override
    public SoftwareDownloadVO getDownloadStatus(String downloadId) {
        SoftwareDownloadVO vo = downloadExecutor.getStatus(downloadId);
        if (vo == null) {
            throw new BusinessException("下载任务不存在或已过期: " + downloadId);
        }
        return vo;
    }

    @Override
    public SoftwareUploadVO uploadSoftware(MultipartFile file) {
        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.isBlank()) {
            throw new BusinessException("文件名不能为空");
        }

        Matcher matcher = FILE_PATTERN.matcher(fileName);
        if (!matcher.matches()) {
            throw new BusinessException("文件名不符合规范，正确格式: {name}-{version}.{os}-{arch}.tar.gz");
        }

        String name = matcher.group(1);
        String version = matcher.group(2);
        String osType = matcher.group(3);
        String osArch = matcher.group(4);

        if (!DISPLAY_NAMES.containsKey(name)) {
            throw new BusinessException("不支持的组件名: " + name + "，支持的组件: " + String.join(", ", DISPLAY_NAMES.keySet()));
        }

        // Save file to {softwarePath}/{name}/{fileName}
        File componentDir = new File(softwarePath, name);
        if (!componentDir.exists()) {
            componentDir.mkdirs();
        }
        File targetFile = new File(componentDir, fileName);
        try {
            file.transferTo(targetFile);
        } catch (IOException e) {
            throw new BusinessException("文件保存失败: " + e.getMessage());
        }

        // Delete old versions with same name/os/arch
        LambdaQueryWrapper<PromDistributeSoftware> oldWrapper = new LambdaQueryWrapper<>();
        oldWrapper.eq(PromDistributeSoftware::getName, name)
                .eq(PromDistributeSoftware::getOsType, osType)
                .eq(PromDistributeSoftware::getOsArch, osArch);
        List<PromDistributeSoftware> oldRecords = softwareMapper.selectList(oldWrapper);

        int replacedCount = 0;
        for (PromDistributeSoftware old : oldRecords) {
            // Delete old file from disk
            File oldFile = new File(componentDir, old.getFileName());
            if (oldFile.exists() && !oldFile.getName().equals(fileName)) {
                oldFile.delete();
            }
            softwareMapper.deleteById(old.getId());
            replacedCount++;
        }

        // Create new database record
        PromDistributeSoftware software = new PromDistributeSoftware();
        software.setName(name);
        software.setDisplayName(DISPLAY_NAMES.getOrDefault(name, name));
        software.setVersion(version);
        software.setOsType(osType);
        software.setOsArch(osArch);
        software.setFileName(fileName);
        software.setFileSize(targetFile.length());
        software.setDefaultPort(DEFAULT_PORTS.get(name));
        software.setCreatedAt(LocalDateTime.now());
        software.setUpdatedAt(LocalDateTime.now());
        softwareMapper.insert(software);

        log.info("Uploaded software: {} v{} ({}/{}), replaced {} old records",
                name, version, osType, osArch, replacedCount);

        SoftwareUploadVO vo = new SoftwareUploadVO();
        vo.setName(name);
        vo.setDisplayName(DISPLAY_NAMES.getOrDefault(name, name));
        vo.setVersion(version);
        vo.setOsType(osType);
        vo.setOsArch(osArch);
        vo.setFileName(fileName);
        vo.setFileSize(targetFile.length());
        vo.setReplacedCount(replacedCount);
        vo.setMessage("上传成功" + (replacedCount > 0 ? "，替换了 " + replacedCount + " 个旧版本" : ""));
        return vo;
    }
}
