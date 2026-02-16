package com.wenmin.prometheus.module.dashboard.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
    public Map<String, Object> listDashboards(String tag, String keyword) {
        LambdaQueryWrapper<PromDashboard> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(keyword)) {
            wrapper.like(PromDashboard::getName, keyword);
        }

        if (StringUtils.hasText(tag)) {
            wrapper.apply("JSON_CONTAINS(tags, CONCAT('\"', {0}, '\"'))", tag);
        }

        wrapper.orderByDesc(PromDashboard::getUpdatedAt);

        List<PromDashboard> list = dashboardMapper.selectList(wrapper);

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", list.size());
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
    public Map<String, Object> listTemplates(String category, String exporterType, String keyword) {
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

        List<PromDashboardTemplate> list = dashboardTemplateMapper.selectList(wrapper);

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", list.size());
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
}
