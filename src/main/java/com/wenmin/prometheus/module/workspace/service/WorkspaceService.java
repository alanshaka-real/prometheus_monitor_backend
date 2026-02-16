package com.wenmin.prometheus.module.workspace.service;

import com.wenmin.prometheus.module.workspace.entity.PromWorkspace;

import java.util.List;
import java.util.Map;

public interface WorkspaceService {

    Map<String, Object> listWorkspaces(String keyword, String status);

    Map<String, Object> getWorkspaceDetail(String id);

    PromWorkspace create(PromWorkspace workspace);

    PromWorkspace update(String id, PromWorkspace workspace);

    void delete(String id);

    void publishDashboard(String workspaceId, String dashboardId);

    void removeDashboard(String workspaceId, String dashboardId);

    void updateDashboardSort(String workspaceId, List<Map<String, Object>> sortList);
}
