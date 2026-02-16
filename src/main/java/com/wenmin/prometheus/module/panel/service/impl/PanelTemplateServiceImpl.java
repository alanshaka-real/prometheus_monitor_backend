package com.wenmin.prometheus.module.panel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wenmin.prometheus.common.exception.BusinessException;
import com.wenmin.prometheus.module.panel.entity.PromPanelTemplate;
import com.wenmin.prometheus.module.panel.mapper.PanelTemplateMapper;
import com.wenmin.prometheus.module.panel.service.PanelTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PanelTemplateServiceImpl implements PanelTemplateService {

    private final PanelTemplateMapper panelTemplateMapper;

    @Override
    public Map<String, Object> listPanelTemplates(String category, String exporterType, String keyword) {
        LambdaQueryWrapper<PromPanelTemplate> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(category)) {
            wrapper.eq(PromPanelTemplate::getCategory, category);
        }

        if (StringUtils.hasText(exporterType)) {
            wrapper.eq(PromPanelTemplate::getExporterType, exporterType);
        }

        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(PromPanelTemplate::getName, keyword)
                    .or().like(PromPanelTemplate::getPromql, keyword)
                    .or().like(PromPanelTemplate::getDescription, keyword));
        }

        wrapper.orderByAsc(PromPanelTemplate::getCategory)
                .orderByAsc(PromPanelTemplate::getSortOrder);

        List<PromPanelTemplate> list = panelTemplateMapper.selectList(wrapper);

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", list.size());
        return result;
    }

    @Override
    public PromPanelTemplate getById(String id) {
        PromPanelTemplate panel = panelTemplateMapper.selectById(id);
        if (panel == null) {
            throw new BusinessException("Panel template not found: " + id);
        }
        return panel;
    }

    @Override
    public List<Map<String, Object>> getCategoryTree() {
        List<PromPanelTemplate> all = panelTemplateMapper.selectList(
                new LambdaQueryWrapper<PromPanelTemplate>()
                        .orderByAsc(PromPanelTemplate::getCategory)
                        .orderByAsc(PromPanelTemplate::getSubCategory)
                        .orderByAsc(PromPanelTemplate::getSortOrder)
        );

        // Group by category -> sub_category
        Map<String, Map<String, List<PromPanelTemplate>>> grouped = all.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getCategory() != null ? p.getCategory() : "",
                        LinkedHashMap::new,
                        Collectors.groupingBy(
                                p -> p.getSubCategory() != null ? p.getSubCategory() : "",
                                LinkedHashMap::new,
                                Collectors.toList()
                        )
                ));

        List<Map<String, Object>> tree = new ArrayList<>();
        for (Map.Entry<String, Map<String, List<PromPanelTemplate>>> catEntry : grouped.entrySet()) {
            Map<String, Object> catNode = new LinkedHashMap<>();
            catNode.put("label", catEntry.getKey());
            catNode.put("exporterType", catEntry.getValue().values().stream()
                    .flatMap(Collection::stream)
                    .findFirst()
                    .map(PromPanelTemplate::getExporterType)
                    .orElse(""));

            List<Map<String, Object>> subNodes = new ArrayList<>();
            int catCount = 0;
            for (Map.Entry<String, List<PromPanelTemplate>> subEntry : catEntry.getValue().entrySet()) {
                if (StringUtils.hasText(subEntry.getKey())) {
                    Map<String, Object> subNode = new LinkedHashMap<>();
                    subNode.put("label", subEntry.getKey());
                    subNode.put("count", subEntry.getValue().size());
                    subNode.put("panels", subEntry.getValue());
                    subNodes.add(subNode);
                }
                catCount += subEntry.getValue().size();
            }

            catNode.put("count", catCount);
            if (!subNodes.isEmpty()) {
                catNode.put("children", subNodes);
            } else {
                catNode.put("panels", catEntry.getValue().values().stream()
                        .flatMap(Collection::stream)
                        .collect(Collectors.toList()));
            }
            tree.add(catNode);
        }

        return tree;
    }
}
