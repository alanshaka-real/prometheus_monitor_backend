package com.wenmin.prometheus.module.settings.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wenmin.prometheus.module.settings.entity.SysGlobalSettings;
import com.wenmin.prometheus.module.settings.entity.SysSystemLog;
import com.wenmin.prometheus.module.settings.mapper.GlobalSettingsMapper;
import com.wenmin.prometheus.module.settings.mapper.SystemLogMapper;
import com.wenmin.prometheus.module.settings.service.SettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SettingsServiceImpl implements SettingsService {

    private final GlobalSettingsMapper settingsMapper;
    private final SystemLogMapper logMapper;

    @Override
    public Map<String, String> getGlobalSettings() {
        List<SysGlobalSettings> list = settingsMapper.selectList(null);
        return list.stream().collect(Collectors.toMap(
                SysGlobalSettings::getSettingKey,
                s -> s.getSettingValue() != null ? s.getSettingValue() : "",
                (a, b) -> b,
                LinkedHashMap::new));
    }

    @Override
    public void updateGlobalSettings(Map<String, String> settings) {
        settings.forEach((key, value) -> {
            SysGlobalSettings existing = settingsMapper.selectOne(
                    new LambdaQueryWrapper<SysGlobalSettings>().eq(SysGlobalSettings::getSettingKey, key));
            if (existing != null) {
                existing.setSettingValue(value);
                existing.setUpdatedAt(LocalDateTime.now());
                settingsMapper.updateById(existing);
            } else {
                SysGlobalSettings newSetting = new SysGlobalSettings();
                newSetting.setId(UUID.randomUUID().toString());
                newSetting.setSettingKey(key);
                newSetting.setSettingValue(value);
                settingsMapper.insert(newSetting);
            }
        });
    }

    @Override
    public Map<String, Object> listSystemLogs(String level, String startTime, String endTime, Integer page, Integer pageSize) {
        LambdaQueryWrapper<SysSystemLog> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(level)) {
            wrapper.eq(SysSystemLog::getLevel, level);
        }
        if (StringUtils.hasText(startTime)) {
            wrapper.ge(SysSystemLog::getCreatedAt, LocalDateTime.parse(startTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        if (StringUtils.hasText(endTime)) {
            wrapper.le(SysSystemLog::getCreatedAt, LocalDateTime.parse(endTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        wrapper.orderByDesc(SysSystemLog::getCreatedAt);

        if (page != null && pageSize != null) {
            Page<SysSystemLog> p = logMapper.selectPage(new Page<>(page, pageSize), wrapper);
            return Map.of("list", p.getRecords(), "total", p.getTotal());
        }
        List<SysSystemLog> list = logMapper.selectList(wrapper);
        return Map.of("list", list, "total", list.size());
    }

    @SuppressWarnings("unchecked")
    @Override
    public byte[] exportSystemLogs(String level, String startTime, String endTime) {
        Map<String, Object> result = listSystemLogs(level, startTime, endTime, null, null);
        List<SysSystemLog> logs = (List<SysSystemLog>) result.get("list");
        StringBuilder csv = new StringBuilder();
        csv.append("ID,Level,Message,Source,CreatedAt\n");
        for (SysSystemLog log : logs) {
            csv.append(String.format("%s,%s,\"%s\",%s,%s\n",
                    log.getId(), log.getLevel(),
                    log.getMessage() != null ? log.getMessage().replace("\"", "\"\"") : "",
                    log.getSource() != null ? log.getSource() : "",
                    log.getCreatedAt() != null ? log.getCreatedAt().toString() : ""));
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public void clearSystemLogs(String before) {
        LambdaQueryWrapper<SysSystemLog> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(before)) {
            wrapper.le(SysSystemLog::getCreatedAt, LocalDateTime.parse(before, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        logMapper.delete(wrapper);
    }
}
