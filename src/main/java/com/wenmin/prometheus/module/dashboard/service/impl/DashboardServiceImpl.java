package com.wenmin.prometheus.module.dashboard.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wenmin.prometheus.common.exception.BusinessException;
import com.wenmin.prometheus.module.dashboard.entity.PromDashboard;
import com.wenmin.prometheus.module.dashboard.entity.PromDashboardTemplate;
import com.wenmin.prometheus.module.dashboard.mapper.DashboardMapper;
import com.wenmin.prometheus.module.dashboard.mapper.DashboardTemplateMapper;
import com.wenmin.prometheus.module.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final DashboardMapper dashboardMapper;
    private final DashboardTemplateMapper dashboardTemplateMapper;

    @Override
    public Map<String, Object> listDashboards(String tag, String keyword, Integer page, Integer pageSize) {
        LambdaQueryWrapper<PromDashboard> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(keyword)) {
            wrapper.like(PromDashboard::getName, keyword);
        }

        if (StringUtils.hasText(tag)) {
            wrapper.apply("JSON_CONTAINS(tags, CONCAT('\"', {0}, '\"'))", tag);
        }

        wrapper.orderByDesc(PromDashboard::getUpdatedAt);

        Page<PromDashboard> pageObj = new Page<>(page, pageSize);
        IPage<PromDashboard> pageResult = dashboardMapper.selectPage(pageObj, wrapper);

        Map<String, Object> result = new HashMap<>();
        result.put("list", pageResult.getRecords());
        result.put("total", pageResult.getTotal());
        return result;
    }

    @Override
    public PromDashboard getById(String id) {
        PromDashboard dashboard = dashboardMapper.selectById(id);
        if (dashboard == null) {
            throw new BusinessException("Dashboard not found: " + id);
        }
        return dashboard;
    }

    @Override
    public PromDashboard create(PromDashboard dashboard) {
        LocalDateTime now = LocalDateTime.now();
        dashboard.setCreatedAt(now);
        dashboard.setUpdatedAt(now);
        dashboard.setDeleted(0);
        if (dashboard.getFavorite() == null) {
            dashboard.setFavorite(false);
        }
        if (dashboard.getPanels() == null) {
            dashboard.setPanels(new ArrayList<>());
        }
        if (dashboard.getTags() == null) {
            dashboard.setTags(new ArrayList<>());
        }
        dashboardMapper.insert(dashboard);
        return dashboard;
    }

    @Override
    public PromDashboard update(String id, PromDashboard dashboard) {
        PromDashboard existing = dashboardMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("Dashboard not found: " + id);
        }
        dashboard.setId(id);
        dashboard.setUpdatedAt(LocalDateTime.now());
        // Preserve original creation info
        dashboard.setCreatedAt(existing.getCreatedAt());
        dashboard.setCreatedBy(existing.getCreatedBy());
        dashboard.setDeleted(existing.getDeleted());
        dashboardMapper.updateById(dashboard);
        return dashboardMapper.selectById(id);
    }

    @Override
    public void delete(String id) {
        PromDashboard existing = dashboardMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("Dashboard not found: " + id);
        }
        dashboardMapper.deleteById(id);
    }

    @Override
    public Map<String, Object> listTemplates(String category, String exporterType, String keyword, Integer page, Integer pageSize) {
        LambdaQueryWrapper<PromDashboardTemplate> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(category)) {
            wrapper.eq(PromDashboardTemplate::getCategory, category);
        }

        if (StringUtils.hasText(exporterType)) {
            wrapper.eq(PromDashboardTemplate::getExporterType, exporterType);
        }

        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(PromDashboardTemplate::getName, keyword)
                    .or().like(PromDashboardTemplate::getDescription, keyword));
        }

        wrapper.orderByAsc(PromDashboardTemplate::getCategory)
                .orderByDesc(PromDashboardTemplate::getCreatedAt);

        Page<PromDashboardTemplate> pageObj = new Page<>(page, pageSize);
        IPage<PromDashboardTemplate> pageResult = dashboardTemplateMapper.selectPage(pageObj, wrapper);

        Map<String, Object> result = new HashMap<>();
        result.put("list", pageResult.getRecords());
        result.put("total", pageResult.getTotal());
        return result;
    }

    @Override
    public PromDashboardTemplate getTemplateById(String id) {
        PromDashboardTemplate template = dashboardTemplateMapper.selectById(id);
        if (template == null) {
            throw new BusinessException("Dashboard template not found: " + id);
        }
        return template;
    }

    @Override
    public List<Map<String, Object>> getTemplateTree() {
        List<PromDashboardTemplate> all = dashboardTemplateMapper.selectList(
                new LambdaQueryWrapper<PromDashboardTemplate>()
                        .orderByAsc(PromDashboardTemplate::getCategory)
                        .orderByAsc(PromDashboardTemplate::getSubCategory)
        );

        Map<String, Map<String, List<PromDashboardTemplate>>> grouped = all.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getCategory() != null ? t.getCategory() : "",
                        LinkedHashMap::new,
                        Collectors.groupingBy(
                                t -> t.getSubCategory() != null ? t.getSubCategory() : "",
                                LinkedHashMap::new,
                                Collectors.toList()
                        )
                ));

        List<Map<String, Object>> tree = new ArrayList<>();
        for (Map.Entry<String, Map<String, List<PromDashboardTemplate>>> catEntry : grouped.entrySet()) {
            Map<String, Object> catNode = new LinkedHashMap<>();
            catNode.put("label", catEntry.getKey());

            List<Map<String, Object>> subNodes = new ArrayList<>();
            int catCount = 0;
            for (Map.Entry<String, List<PromDashboardTemplate>> subEntry : catEntry.getValue().entrySet()) {
                if (StringUtils.hasText(subEntry.getKey())) {
                    Map<String, Object> subNode = new LinkedHashMap<>();
                    subNode.put("label", subEntry.getKey());
                    subNode.put("count", subEntry.getValue().size());
                    subNodes.add(subNode);
                }
                catCount += subEntry.getValue().size();
            }

            catNode.put("count", catCount);
            if (!subNodes.isEmpty()) {
                catNode.put("children", subNodes);
            }
            tree.add(catNode);
        }

        return tree;
    }

    @Override
    public PromDashboard importTemplate(String templateId) {
        PromDashboardTemplate template = dashboardTemplateMapper.selectById(templateId);
        if (template == null) {
            throw new BusinessException("Dashboard template not found: " + templateId);
        }

        PromDashboard dashboard = new PromDashboard();
        dashboard.setName(template.getName());
        dashboard.setDescription(template.getDescription());
        dashboard.setPanels(template.getPanels());
        dashboard.setTags(template.getTags());
        dashboard.setRefreshInterval(30);
        dashboard.setFavorite(false);

        LocalDateTime now = LocalDateTime.now();
        dashboard.setCreatedAt(now);
        dashboard.setUpdatedAt(now);
        dashboard.setDeleted(0);

        dashboardMapper.insert(dashboard);
        return dashboard;
    }

    @Override
    public PromDashboard exportDashboard(String id) {
        return getById(id);
    }

    @Override
    @SuppressWarnings("unchecked")
    public PromDashboard importGrafanaJson(Map<String, Object> grafanaJson) {
        // The Grafana JSON may be wrapped in a "dashboard" key or be the dashboard itself
        Map<String, Object> dashJson = grafanaJson.containsKey("dashboard")
                ? (Map<String, Object>) grafanaJson.get("dashboard")
                : grafanaJson;

        PromDashboard dashboard = new PromDashboard();

        // Extract title -> name
        String title = (String) dashJson.getOrDefault("title", "Imported Dashboard");
        dashboard.setName(title);

        // Extract description
        String description = (String) dashJson.get("description");
        dashboard.setDescription(description);

        // Extract and convert tags
        List<String> tags = new ArrayList<>();
        Object tagsObj = dashJson.get("tags");
        if (tagsObj instanceof List<?>) {
            for (Object t : (List<?>) tagsObj) {
                if (t != null) {
                    tags.add(t.toString());
                }
            }
        }
        dashboard.setTags(tags);

        // Extract and convert panels from Grafana format to internal format
        List<Map<String, Object>> panels = new ArrayList<>();
        Object panelsObj = dashJson.get("panels");
        if (panelsObj instanceof List<?>) {
            for (Object p : (List<?>) panelsObj) {
                if (p instanceof Map) {
                    Map<String, Object> grafanaPanel = (Map<String, Object>) p;
                    Map<String, Object> internalPanel = convertGrafanaPanelToInternal(grafanaPanel);
                    panels.add(internalPanel);
                }
            }
        }
        dashboard.setPanels(panels);

        // Extract time range
        Object timeObj = dashJson.get("time");
        if (timeObj instanceof Map) {
            dashboard.setTimeRange((Map<String, Object>) timeObj);
        } else {
            Map<String, Object> defaultTime = new LinkedHashMap<>();
            defaultTime.put("from", "now-1h");
            defaultTime.put("to", "now");
            dashboard.setTimeRange(defaultTime);
        }

        // Extract refresh interval
        Object refreshObj = dashJson.get("refresh");
        if (refreshObj instanceof String) {
            dashboard.setRefreshInterval(parseRefreshInterval((String) refreshObj));
        } else {
            dashboard.setRefreshInterval(30);
        }

        dashboard.setFavorite(false);

        // Persist via existing create method
        return create(dashboard);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> exportAsGrafanaJson(String id) {
        PromDashboard dashboard = getById(id);

        Map<String, Object> grafana = new LinkedHashMap<>();

        // __inputs: datasource placeholders
        List<Map<String, Object>> inputs = new ArrayList<>();
        Map<String, Object> dsInput = new LinkedHashMap<>();
        dsInput.put("name", "DS_PROMETHEUS");
        dsInput.put("label", "Prometheus");
        dsInput.put("description", "");
        dsInput.put("type", "datasource");
        dsInput.put("pluginId", "prometheus");
        dsInput.put("pluginName", "Prometheus");
        inputs.add(dsInput);
        grafana.put("__inputs", inputs);

        // __requires
        List<Map<String, Object>> requires = new ArrayList<>();
        Map<String, Object> grafanaReq = new LinkedHashMap<>();
        grafanaReq.put("type", "grafana");
        grafanaReq.put("id", "grafana");
        grafanaReq.put("name", "Grafana");
        grafanaReq.put("version", "9.0.0");
        requires.add(grafanaReq);
        Map<String, Object> promReq = new LinkedHashMap<>();
        promReq.put("type", "datasource");
        promReq.put("id", "prometheus");
        promReq.put("name", "Prometheus");
        promReq.put("version", "1.0.0");
        requires.add(promReq);
        grafana.put("__requires", requires);

        grafana.put("id", null);
        grafana.put("uid", dashboard.getId());
        grafana.put("title", dashboard.getName());
        grafana.put("description", dashboard.getDescription());
        grafana.put("tags", dashboard.getTags() != null ? dashboard.getTags() : new ArrayList<>());

        // Convert internal panels to Grafana panel format
        List<Map<String, Object>> grafanaPanels = new ArrayList<>();
        if (dashboard.getPanels() != null) {
            int panelId = 1;
            for (Map<String, Object> internalPanel : dashboard.getPanels()) {
                Map<String, Object> gPanel = convertInternalPanelToGrafana(internalPanel, panelId++);
                grafanaPanels.add(gPanel);
            }
        }
        grafana.put("panels", grafanaPanels);

        // Time range
        if (dashboard.getTimeRange() != null) {
            grafana.put("time", dashboard.getTimeRange());
        } else {
            Map<String, Object> defaultTime = new LinkedHashMap<>();
            defaultTime.put("from", "now-1h");
            defaultTime.put("to", "now");
            grafana.put("time", defaultTime);
        }

        // Refresh
        grafana.put("refresh", formatRefreshInterval(dashboard.getRefreshInterval()));

        grafana.put("schemaVersion", 38);
        grafana.put("version", dashboard.getVersion() != null ? dashboard.getVersion() : 0);
        grafana.put("editable", true);

        return grafana;
    }

    // ---- Grafana conversion helpers ----

    @SuppressWarnings("unchecked")
    private Map<String, Object> convertGrafanaPanelToInternal(Map<String, Object> gPanel) {
        Map<String, Object> panel = new LinkedHashMap<>();
        panel.put("title", gPanel.getOrDefault("title", ""));

        // Map Grafana panel type to internal type
        String grafanaType = (String) gPanel.getOrDefault("type", "timeseries");
        panel.put("type", mapGrafanaTypeToInternal(grafanaType));

        // Extract PromQL targets as queries
        List<String> queries = new ArrayList<>();
        Object targetsObj = gPanel.get("targets");
        if (targetsObj instanceof List<?>) {
            for (Object t : (List<?>) targetsObj) {
                if (t instanceof Map) {
                    Map<String, Object> target = (Map<String, Object>) t;
                    String expr = (String) target.get("expr");
                    if (expr != null && !expr.isBlank()) {
                        queries.add(expr);
                    }
                }
            }
        }
        panel.put("queries", queries);

        // Carry over gridPos for layout
        Object gridPos = gPanel.get("gridPos");
        if (gridPos instanceof Map) {
            panel.put("gridPos", gridPos);
        }

        // Carry over description if present
        Object desc = gPanel.get("description");
        if (desc != null) {
            panel.put("description", desc);
        }

        return panel;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> convertInternalPanelToGrafana(Map<String, Object> internalPanel, int panelId) {
        Map<String, Object> gPanel = new LinkedHashMap<>();
        gPanel.put("id", panelId);
        gPanel.put("title", internalPanel.getOrDefault("title", ""));

        // Map internal type back to Grafana type
        String internalType = (String) internalPanel.getOrDefault("type", "line");
        gPanel.put("type", mapInternalTypeToGrafana(internalType));

        // Build Grafana targets from queries
        List<Map<String, Object>> targets = new ArrayList<>();
        Object queriesObj = internalPanel.get("queries");
        if (queriesObj instanceof List<?>) {
            int refIdx = 0;
            for (Object q : (List<?>) queriesObj) {
                Map<String, Object> target = new LinkedHashMap<>();
                target.put("expr", q.toString());
                target.put("refId", String.valueOf((char) ('A' + refIdx)));
                target.put("datasource", Map.of("type", "prometheus", "uid", "${DS_PROMETHEUS}"));
                targets.add(target);
                refIdx++;
            }
        }
        gPanel.put("targets", targets);

        // Grid position
        Object gridPos = internalPanel.get("gridPos");
        if (gridPos instanceof Map) {
            gPanel.put("gridPos", gridPos);
        } else {
            Map<String, Object> defaultPos = new LinkedHashMap<>();
            defaultPos.put("h", 8);
            defaultPos.put("w", 12);
            defaultPos.put("x", 0);
            defaultPos.put("y", (panelId - 1) * 8);
            gPanel.put("gridPos", defaultPos);
        }

        // Description
        Object desc = internalPanel.get("description");
        if (desc != null) {
            gPanel.put("description", desc);
        }

        gPanel.put("datasource", Map.of("type", "prometheus", "uid", "${DS_PROMETHEUS}"));

        return gPanel;
    }

    private String mapGrafanaTypeToInternal(String grafanaType) {
        if (grafanaType == null) return "line";
        return switch (grafanaType) {
            case "timeseries", "graph" -> "line";
            case "barchart", "bargauge" -> "bar";
            case "stat", "singlestat" -> "stat";
            case "gauge" -> "gauge";
            case "table", "table-old" -> "table";
            case "piechart" -> "pie";
            case "heatmap" -> "heatmap";
            case "text" -> "text";
            default -> grafanaType;
        };
    }

    private String mapInternalTypeToGrafana(String internalType) {
        if (internalType == null) return "timeseries";
        return switch (internalType) {
            case "line" -> "timeseries";
            case "bar" -> "barchart";
            case "stat" -> "stat";
            case "gauge" -> "gauge";
            case "table" -> "table";
            case "pie" -> "piechart";
            case "heatmap" -> "heatmap";
            case "text" -> "text";
            default -> internalType;
        };
    }

    private int parseRefreshInterval(String refresh) {
        if (refresh == null || refresh.isBlank()) return 30;
        try {
            if (refresh.endsWith("s")) {
                return Integer.parseInt(refresh.replace("s", "").trim());
            } else if (refresh.endsWith("m")) {
                return Integer.parseInt(refresh.replace("m", "").trim()) * 60;
            } else if (refresh.endsWith("h")) {
                return Integer.parseInt(refresh.replace("h", "").trim()) * 3600;
            }
            return Integer.parseInt(refresh);
        } catch (NumberFormatException e) {
            return 30;
        }
    }

    private String formatRefreshInterval(Integer seconds) {
        if (seconds == null || seconds <= 0) return "30s";
        if (seconds >= 3600 && seconds % 3600 == 0) return (seconds / 3600) + "h";
        if (seconds >= 60 && seconds % 60 == 0) return (seconds / 60) + "m";
        return seconds + "s";
    }
}
