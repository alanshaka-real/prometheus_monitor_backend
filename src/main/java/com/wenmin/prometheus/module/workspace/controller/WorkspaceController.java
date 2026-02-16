package com.wenmin.prometheus.module.workspace.controller;

import com.wenmin.prometheus.common.result.R;
import com.wenmin.prometheus.module.workspace.entity.PromWorkspace;
import com.wenmin.prometheus.module.workspace.service.WorkspaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/prometheus/workspaces")
@RequiredArgsConstructor
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    @GetMapping
    public R<Map<String, Object>> listWorkspaces(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        return R.ok(workspaceService.listWorkspaces(keyword, status));
    }

    @GetMapping("/{id}")
    public R<Map<String, Object>> getWorkspaceDetail(@PathVariable String id) {
        return R.ok(workspaceService.getWorkspaceDetail(id));
    }

    @PostMapping
    public R<PromWorkspace> createWorkspace(@RequestBody PromWorkspace workspace) {
        return R.ok(workspaceService.create(workspace));
    }

    @PutMapping("/{id}")
    public R<PromWorkspace> updateWorkspace(@PathVariable String id, @RequestBody PromWorkspace workspace) {
        return R.ok(workspaceService.update(id, workspace));
    }

    @DeleteMapping("/{id}")
    public R<Void> deleteWorkspace(@PathVariable String id) {
        workspaceService.delete(id);
        return R.ok();
    }

    @PostMapping("/{id}/dashboards")
    public R<Void> publishDashboard(@PathVariable String id, @RequestBody Map<String, String> body) {
        workspaceService.publishDashboard(id, body.get("dashboardId"));
        return R.ok();
    }

    @DeleteMapping("/{id}/dashboards/{dashboardId}")
    public R<Void> removeDashboard(@PathVariable String id, @PathVariable String dashboardId) {
        workspaceService.removeDashboard(id, dashboardId);
        return R.ok();
    }

    @PutMapping("/{id}/dashboards/sort")
    public R<Void> updateDashboardSort(@PathVariable String id, @RequestBody List<Map<String, Object>> sortList) {
        workspaceService.updateDashboardSort(id, sortList);
        return R.ok();
    }
}
