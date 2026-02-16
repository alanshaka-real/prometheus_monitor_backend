package com.wenmin.prometheus.module.dashboard.controller;

import com.wenmin.prometheus.common.result.R;
import com.wenmin.prometheus.module.dashboard.entity.PromDashboard;
import com.wenmin.prometheus.module.dashboard.entity.PromDashboardTemplate;
import com.wenmin.prometheus.module.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/prometheus")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * List dashboards with optional tag and keyword filters
     */
    @GetMapping("/dashboards")
    public R<Map<String, Object>> listDashboards(
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String keyword) {
        return R.ok(dashboardService.listDashboards(tag, keyword));
    }

    /**
     * Get a single dashboard by id
     */
    @GetMapping("/dashboards/{id}")
    public R<PromDashboard> getDashboard(@PathVariable String id) {
        return R.ok(dashboardService.getById(id));
    }

    /**
     * Create a new dashboard
     */
    @PostMapping("/dashboards")
    public R<PromDashboard> createDashboard(@RequestBody PromDashboard dashboard) {
        return R.ok(dashboardService.create(dashboard));
    }

    /**
     * Update an existing dashboard
     */
    @PutMapping("/dashboards/{id}")
    public R<PromDashboard> updateDashboard(
            @PathVariable String id,
            @RequestBody PromDashboard dashboard) {
        return R.ok(dashboardService.update(id, dashboard));
    }

    /**
     * Delete a dashboard (logical delete)
     */
    @DeleteMapping("/dashboards/{id}")
    public R<Void> deleteDashboard(@PathVariable String id) {
        dashboardService.delete(id);
        return R.ok();
    }

    /**
     * List dashboard templates with optional filters
     */
    @GetMapping("/dashboard/templates")
    public R<Map<String, Object>> listTemplates(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String exporterType,
            @RequestParam(required = false) String keyword) {
        return R.ok(dashboardService.listTemplates(category, exporterType, keyword));
    }

    /**
     * Get dashboard template detail by id
     */
    @GetMapping("/dashboard/templates/{id}")
    public R<PromDashboardTemplate> getTemplate(@PathVariable String id) {
        return R.ok(dashboardService.getTemplateById(id));
    }

    /**
     * Get dashboard template category tree
     */
    @GetMapping("/dashboard/templates/tree")
    public R<List<Map<String, Object>>> getTemplateTree() {
        return R.ok(dashboardService.getTemplateTree());
    }

    /**
     * Import a template as a new dashboard
     */
    @PostMapping("/dashboard/templates/{templateId}/import")
    public R<PromDashboard> importTemplate(@PathVariable String templateId) {
        return R.ok(dashboardService.importTemplate(templateId));
    }

    /**
     * Export a dashboard (returns full dashboard data)
     */
    @GetMapping("/dashboards/{id}/export")
    public R<PromDashboard> exportDashboard(@PathVariable String id) {
        return R.ok(dashboardService.exportDashboard(id));
    }
}
