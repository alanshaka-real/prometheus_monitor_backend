package com.wenmin.prometheus.module.query.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wenmin.prometheus.common.exception.BusinessException;
import com.wenmin.prometheus.module.datasource.entity.PromExporter;
import com.wenmin.prometheus.module.datasource.entity.PromInstance;
import com.wenmin.prometheus.module.datasource.mapper.PromExporterMapper;
import com.wenmin.prometheus.module.datasource.mapper.PromInstanceMapper;
import com.wenmin.prometheus.module.query.entity.PromPromqlTemplate;
import com.wenmin.prometheus.module.query.entity.PromQueryHistory;
import com.wenmin.prometheus.module.query.mapper.PromqlTemplateMapper;
import com.wenmin.prometheus.module.query.mapper.QueryHistoryMapper;
import com.wenmin.prometheus.module.query.service.QueryService;
import com.wenmin.prometheus.module.query.vo.ExporterInstanceVO;
import com.wenmin.prometheus.module.query.vo.TemplateTreeVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class QueryServiceImpl implements QueryService {

    private final PromInstanceMapper instanceMapper;
    private final QueryHistoryMapper historyMapper;
    private final PromqlTemplateMapper templateMapper;
    private final PromExporterMapper exporterMapper;
    private final RestTemplate restTemplate;

    private String getPrometheusUrl(String instanceId) {
        PromInstance instance;
        if (instanceId != null && !instanceId.isEmpty()) {
            instance = instanceMapper.selectById(instanceId);
            if (instance == null) {
                throw new BusinessException("指定的 Prometheus 实例不存在");
            }
        } else {
            instance = instanceMapper.selectOne(
                    new LambdaQueryWrapper<PromInstance>()
                            .eq(PromInstance::getStatus, "online")
                            .last("LIMIT 1"));
            if (instance == null) {
                throw new BusinessException("没有可用的 Prometheus 实例");
            }
        }
        return instance.getUrl();
    }

    @Override
    public Object instantQuery(String query, Long time, String instanceId) {
        String url = getPrometheusUrl(instanceId);
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url + "/api/v1/query")
                .queryParam("query", query);
        if (time != null) {
            builder.queryParam("time", time);
        }
        try {
            URI uri = builder.build().encode().toUri();
            return restTemplate.getForObject(uri, Object.class);
        } catch (Exception e) {
            log.error("Prometheus query failed", e);
            throw new BusinessException("Prometheus 查询失败: " + e.getMessage());
        }
    }

    @Override
    public Object rangeQuery(String query, Long start, Long end, String step, String instanceId) {
        String url = getPrometheusUrl(instanceId);
        URI uri = UriComponentsBuilder.fromHttpUrl(url + "/api/v1/query_range")
                .queryParam("query", query)
                .queryParam("start", start)
                .queryParam("end", end)
                .queryParam("step", step)
                .build().encode().toUri();
        try {
            return restTemplate.getForObject(uri, Object.class);
        } catch (Exception e) {
            log.error("Prometheus range query failed", e);
            throw new BusinessException("Prometheus 范围查询失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getMetricNames(String instanceId) {
        try {
            String url = getPrometheusUrl(instanceId);
            Object result = restTemplate.getForObject(url + "/api/v1/label/__name__/values", Object.class);
            return Map.of("list", result != null ? result : List.of(), "total", 0);
        } catch (Exception e) {
            log.warn("Failed to fetch metric names from Prometheus, returning empty list", e);
            List<String> names = new ArrayList<>();
            return Map.of("list", names, "total", names.size());
        }
    }

    @Override
    public Map<String, Object> getLabelValues(String label, String instanceId) {
        try {
            String url = getPrometheusUrl(instanceId);
            Object result = restTemplate.getForObject(url + "/api/v1/label/" + label + "/values", Object.class);
            return Map.of("list", result != null ? result : List.of(), "total", 0);
        } catch (Exception e) {
            log.warn("Failed to fetch label values from Prometheus for label: {}", label, e);
            return Map.of("list", List.of(), "total", 0);
        }
    }

    @Override
    public Map<String, Object> listHistory() {
        List<PromQueryHistory> list = historyMapper.selectList(
                new LambdaQueryWrapper<PromQueryHistory>()
                        .orderByDesc(PromQueryHistory::getExecutedAt));
        return Map.of("list", list, "total", list.size());
    }

    @Override
    public void saveHistory(String query, Double duration, Integer resultCount, String userId) {
        PromQueryHistory history = new PromQueryHistory();
        history.setQuery(query);
        history.setExecutedAt(LocalDateTime.now());
        history.setDuration(duration);
        history.setResultCount(resultCount);
        history.setFavorite(false);
        history.setUserId(userId);
        historyMapper.insert(history);
    }

    @Override
    public Map<String, Object> listTemplates() {
        List<PromPromqlTemplate> list = templateMapper.selectList(null);
        return Map.of("list", list, "total", list.size());
    }

    @Override
    public List<TemplateTreeVO> listTemplateTree() {
        List<PromPromqlTemplate> all = templateMapper.selectList(
                new LambdaQueryWrapper<PromPromqlTemplate>()
                        .orderByAsc(PromPromqlTemplate::getSortOrder));

        // Group by category
        Map<String, List<PromPromqlTemplate>> byCategory = all.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getCategory() != null ? t.getCategory() : "",
                        LinkedHashMap::new,
                        Collectors.toList()));

        List<TemplateTreeVO> tree = new ArrayList<>();
        for (Map.Entry<String, List<PromPromqlTemplate>> entry : byCategory.entrySet()) {
            TemplateTreeVO node = new TemplateTreeVO();
            node.setCategory(entry.getKey());
            // Use exporterType from first template in category
            node.setExporterType(entry.getValue().get(0).getExporterType());

            // Group by sub_category
            Map<String, List<PromPromqlTemplate>> bySub = entry.getValue().stream()
                    .collect(Collectors.groupingBy(
                            t -> t.getSubCategory() != null ? t.getSubCategory() : "",
                            LinkedHashMap::new,
                            Collectors.toList()));

            List<TemplateTreeVO.SubCategoryGroup> subGroups = new ArrayList<>();
            for (Map.Entry<String, List<PromPromqlTemplate>> subEntry : bySub.entrySet()) {
                TemplateTreeVO.SubCategoryGroup group = new TemplateTreeVO.SubCategoryGroup();
                group.setSubCategory(subEntry.getKey());
                group.setTemplates(subEntry.getValue().stream().map(t -> {
                    TemplateTreeVO.TemplateItem item = new TemplateTreeVO.TemplateItem();
                    item.setId(t.getId());
                    item.setName(t.getName());
                    item.setQuery(t.getQuery());
                    item.setDescription(t.getDescription());
                    item.setVariables(t.getVariables());
                    item.setUnit(t.getUnit());
                    item.setUnitFormat(t.getUnitFormat());
                    return item;
                }).collect(Collectors.toList()));
                subGroups.add(group);
            }
            node.setSubCategories(subGroups);
            tree.add(node);
        }
        return tree;
    }

    @Override
    public List<ExporterInstanceVO> listExporterInstances(String exporterType) {
        List<PromExporter> exporters = exporterMapper.selectList(
                new LambdaQueryWrapper<PromExporter>()
                        .eq(PromExporter::getType, exporterType)
                        .orderByAsc(PromExporter::getName));

        return exporters.stream().map(e -> {
            ExporterInstanceVO vo = new ExporterInstanceVO();
            vo.setId(e.getId());
            vo.setName(e.getName());
            vo.setAddress(e.getHost() + ":" + e.getPort());
            vo.setStatus(e.getStatus());
            vo.setLabels(e.getLabels());
            return vo;
        }).collect(Collectors.toList());
    }
}
