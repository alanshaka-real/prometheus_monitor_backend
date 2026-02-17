package com.wenmin.prometheus.module.query.service;

import com.wenmin.prometheus.module.query.vo.ExporterInstanceVO;
import com.wenmin.prometheus.module.query.vo.TemplateTreeVO;

import java.util.List;
import java.util.Map;

public interface QueryService {

    Object instantQuery(String query, Long time, String instanceId);

    Object rangeQuery(String query, Long start, Long end, String step, String instanceId);

    Map<String, Object> getMetricNames(String instanceId);

    Map<String, Object> getLabelValues(String label, String instanceId);

    Map<String, Object> listHistory(Integer page, Integer pageSize);

    void saveHistory(String query, Double duration, Integer resultCount, String userId);

    Map<String, Object> listTemplates();

    List<TemplateTreeVO> listTemplateTree();

    List<ExporterInstanceVO> listExporterInstances(String exporterType);
}
