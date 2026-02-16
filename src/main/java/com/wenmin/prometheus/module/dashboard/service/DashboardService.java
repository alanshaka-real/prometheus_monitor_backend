package com.wenmin.prometheus.module.dashboard.service;

import com.wenmin.prometheus.module.dashboard.entity.PromDashboard;
import com.wenmin.prometheus.module.dashboard.entity.PromDashboardTemplate;

import java.util.List;
import java.util.Map;

public interface DashboardService {

    Map<String, Object> listDashboards(String tag, String keyword);

    PromDashboard getById(String id);

    PromDashboard create(PromDashboard dashboard);

    PromDashboard update(String id, PromDashboard dashboard);

    void delete(String id);

    Map<String, Object> listTemplates(String category, String exporterType, String keyword);

    PromDashboardTemplate getTemplateById(String id);

    List<Map<String, Object>> getTemplateTree();

    PromDashboard importTemplate(String templateId);

    PromDashboard exportDashboard(String id);
}
