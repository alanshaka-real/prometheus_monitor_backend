package com.wenmin.prometheus.module.query.controller;

import com.wenmin.prometheus.common.result.R;
import com.wenmin.prometheus.module.query.service.QueryService;
import com.wenmin.prometheus.module.query.vo.ExporterInstanceVO;
import com.wenmin.prometheus.module.query.vo.TemplateTreeVO;
import com.wenmin.prometheus.security.SecurityUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "查询管理")
@RestController
@RequestMapping("/api/prometheus")
@RequiredArgsConstructor
public class QueryController {

    private final QueryService queryService;

    @Operation(summary = "即时查询")
    @GetMapping("/query")
    public R<Object> instantQuery(@RequestParam String query,
                                  @RequestParam(required = false) Long time,
                                  @RequestParam(required = false) String instanceId) {
        return R.ok(queryService.instantQuery(query, time, instanceId));
    }

    @Operation(summary = "范围查询")
    @GetMapping("/query_range")
    public R<Object> rangeQuery(@RequestParam String query,
                                @RequestParam Long start,
                                @RequestParam Long end,
                                @RequestParam String step,
                                @RequestParam(required = false) String instanceId) {
        return R.ok(queryService.rangeQuery(query, start, end, step, instanceId));
    }

    @Operation(summary = "获取指标名称列表")
    @GetMapping("/labels/__name__/values")
    public R<Object> getMetricNames(@RequestParam(required = false) String instanceId) {
        return R.ok(queryService.getMetricNames(instanceId));
    }

    @Operation(summary = "获取标签值列表")
    @GetMapping("/labels/{label}/values")
    public R<Object> getLabelValues(@PathVariable String label,
                                   @RequestParam(required = false) String instanceId) {
        return R.ok(queryService.getLabelValues(label, instanceId));
    }

    @Operation(summary = "获取查询历史")
    @GetMapping("/query/history")
    public R<Object> listHistory(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return R.ok(queryService.listHistory(page, pageSize));
    }

    @Operation(summary = "保存查询历史")
    @PostMapping("/query/history")
    public R<Void> saveHistory(@RequestBody Map<String, Object> body,
                               @AuthenticationPrincipal SecurityUser user) {
        queryService.saveHistory(
                (String) body.get("query"),
                body.get("duration") != null ? Double.parseDouble(body.get("duration").toString()) : 0.0,
                body.get("resultCount") != null ? Integer.parseInt(body.get("resultCount").toString()) : 0,
                user != null ? user.getUserId() : null);
        return R.ok();
    }

    @Operation(summary = "获取PromQL模板")
    @GetMapping("/query/templates")
    public R<Object> listTemplates() {
        return R.ok(queryService.listTemplates());
    }

    @Operation(summary = "获取模板树形结构")
    @GetMapping("/query/templates/tree")
    public R<List<TemplateTreeVO>> listTemplateTree() {
        return R.ok(queryService.listTemplateTree());
    }

    @Operation(summary = "获取指定类型的Exporter实例")
    @GetMapping("/query/exporter-instances")
    public R<List<ExporterInstanceVO>> listExporterInstances(
            @RequestParam String exporterType) {
        return R.ok(queryService.listExporterInstances(exporterType));
    }
}
