package com.wenmin.prometheus.module.settings.service;

import java.util.Map;

public interface SettingsService {
    Map<String, String> getGlobalSettings();
    void updateGlobalSettings(Map<String, String> settings);
    Map<String, Object> listSystemLogs(String level, String startTime, String endTime, Integer page, Integer pageSize);
    byte[] exportSystemLogs(String level, String startTime, String endTime);
    void clearSystemLogs(String before);
}
