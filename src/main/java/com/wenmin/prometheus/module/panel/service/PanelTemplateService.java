package com.wenmin.prometheus.module.panel.service;

import com.wenmin.prometheus.module.panel.entity.PromPanelTemplate;

import java.util.List;
import java.util.Map;

public interface PanelTemplateService {

    Map<String, Object> listPanelTemplates(String category, String exporterType, String keyword);

    PromPanelTemplate getById(String id);

    List<Map<String, Object>> getCategoryTree();
}
