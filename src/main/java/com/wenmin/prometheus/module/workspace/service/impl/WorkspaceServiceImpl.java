package com.wenmin.prometheus.module.workspace.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wenmin.prometheus.common.exception.BusinessException;
import com.wenmin.prometheus.module.dashboard.entity.PromDashboard;
import com.wenmin.prometheus.module.dashboard.mapper.DashboardMapper;
import com.wenmin.prometheus.module.workspace.entity.PromWorkspace;
import com.wenmin.prometheus.module.workspace.entity.PromWorkspaceDashboard;
import com.wenmin.prometheus.module.workspace.mapper.WorkspaceDashboardMapper;
import com.wenmin.prometheus.module.workspace.mapper.WorkspaceMapper;
import com.wenmin.prometheus.module.workspace.service.WorkspaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkspaceServiceImpl implements WorkspaceService {

    private final WorkspaceMapper workspaceMapper;
    private final WorkspaceDashboardMapper workspaceDashboardMapper;
    private final DashboardMapper dashboardMapper;

    @Override
    public Map<String, Object> listWorkspaces(String keyword, String status) {
        LambdaQueryWrapper<PromWorkspace> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(keyword)) {
            wrapper.like(PromWorkspace::getName, keyword);
        }

        if (StringUtils.hasText(status)) {
            wrapper.eq(PromWorkspace::getStatus, status);
        }

        wrapper.orderByDesc(PromWorkspace::getUpdatedAt);

        List<PromWorkspace> list = workspaceMapper.selectList(wrapper);

        // Attach dashboard count for each workspace
        List<Map<String, Object>> enriched = list.stream().map(ws -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", ws.getId());
            map.put("name", ws.getName());
            map.put("description", ws.getDescription());
            map.put("icon", ws.getIcon());
            map.put("coverImage", ws.getCoverImage());
            map.put("owner", ws.getOwner());
            map.put("status", ws.getStatus());
            map.put("createdAt", ws.getCreatedAt());
            map.put("updatedAt", ws.getUpdatedAt());

            Long dashCount = workspaceDashboardMapper.selectCount(
                    new LambdaQueryWrapper<PromWorkspaceDashboard>()
                            .eq(PromWorkspaceDashboard::getWorkspaceId, ws.getId())
            );
            map.put("dashboardCount", dashCount);
            return map;
        }).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("list", enriched);
        result.put("total", enriched.size());
        return result;
    }

    @Override
    public Map<String, Object> getWorkspaceDetail(String id) {
        PromWorkspace workspace = workspaceMapper.selectById(id);
        if (workspace == null) {
            throw new BusinessException("Workspace not found: " + id);
        }

        // Get published dashboards
        List<PromWorkspaceDashboard> wdList = workspaceDashboardMapper.selectList(
                new LambdaQueryWrapper<PromWorkspaceDashboard>()
                        .eq(PromWorkspaceDashboard::getWorkspaceId, id)
                        .orderByAsc(PromWorkspaceDashboard::getSortOrder)
        );

        List<Map<String, Object>> dashboards = new ArrayList<>();
        for (PromWorkspaceDashboard wd : wdList) {
            PromDashboard dash = dashboardMapper.selectById(wd.getDashboardId());
            if (dash != null) {
                Map<String, Object> dashMap = new LinkedHashMap<>();
                dashMap.put("id", dash.getId());
                dashMap.put("name", dash.getName());
                dashMap.put("description", dash.getDescription());
                dashMap.put("tags", dash.getTags());
                dashMap.put("panelCount", dash.getPanels() != null ? dash.getPanels().size() : 0);
                dashMap.put("sortOrder", wd.getSortOrder());
                dashMap.put("publishedAt", wd.getPublishedAt());
                dashMap.put("publishedBy", wd.getPublishedBy());
                dashboards.add(dashMap);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("workspace", workspace);
        result.put("dashboards", dashboards);
        result.put("dashboardCount", dashboards.size());
        return result;
    }

    @Override
    public PromWorkspace create(PromWorkspace workspace) {
        LocalDateTime now = LocalDateTime.now();
        workspace.setCreatedAt(now);
        workspace.setUpdatedAt(now);
        workspace.setDeleted(0);
        if (workspace.getStatus() == null) {
            workspace.setStatus("active");
        }
        if (workspace.getOwner() == null) {
            workspace.setOwner("admin");
        }
        if (workspace.getIcon() == null) {
            workspace.setIcon("Monitor");
        }
        workspaceMapper.insert(workspace);
        return workspace;
    }

    @Override
    public PromWorkspace update(String id, PromWorkspace workspace) {
        PromWorkspace existing = workspaceMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("Workspace not found: " + id);
        }
        workspace.setId(id);
        workspace.setUpdatedAt(LocalDateTime.now());
        workspace.setCreatedAt(existing.getCreatedAt());
        workspace.setOwner(existing.getOwner());
        workspace.setDeleted(existing.getDeleted());
        workspaceMapper.updateById(workspace);
        return workspaceMapper.selectById(id);
    }

    @Override
    public void delete(String id) {
        PromWorkspace existing = workspaceMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("Workspace not found: " + id);
        }
        workspaceMapper.deleteById(id);
    }

    @Override
    public void publishDashboard(String workspaceId, String dashboardId) {
        // Check workspace exists
        PromWorkspace workspace = workspaceMapper.selectById(workspaceId);
        if (workspace == null) {
            throw new BusinessException("Workspace not found: " + workspaceId);
        }

        // Check dashboard exists
        PromDashboard dashboard = dashboardMapper.selectById(dashboardId);
        if (dashboard == null) {
            throw new BusinessException("Dashboard not found: " + dashboardId);
        }

        // Check not already published
        Long existing = workspaceDashboardMapper.selectCount(
                new LambdaQueryWrapper<PromWorkspaceDashboard>()
                        .eq(PromWorkspaceDashboard::getWorkspaceId, workspaceId)
                        .eq(PromWorkspaceDashboard::getDashboardId, dashboardId)
        );
        if (existing > 0) {
            throw new BusinessException("Dashboard already published to this workspace");
        }

        // Get max sort order
        Long count = workspaceDashboardMapper.selectCount(
                new LambdaQueryWrapper<PromWorkspaceDashboard>()
                        .eq(PromWorkspaceDashboard::getWorkspaceId, workspaceId)
        );

        PromWorkspaceDashboard wd = new PromWorkspaceDashboard();
        wd.setWorkspaceId(workspaceId);
        wd.setDashboardId(dashboardId);
        wd.setSortOrder(count.intValue());
        wd.setPublishedAt(LocalDateTime.now());
        wd.setPublishedBy("admin");
        workspaceDashboardMapper.insert(wd);
    }

    @Override
    public void removeDashboard(String workspaceId, String dashboardId) {
        workspaceDashboardMapper.delete(
                new LambdaQueryWrapper<PromWorkspaceDashboard>()
                        .eq(PromWorkspaceDashboard::getWorkspaceId, workspaceId)
                        .eq(PromWorkspaceDashboard::getDashboardId, dashboardId)
        );
    }

    @Override
    public void updateDashboardSort(String workspaceId, List<Map<String, Object>> sortList) {
        for (Map<String, Object> item : sortList) {
            String dashboardId = (String) item.get("dashboardId");
            Integer sortOrder = (Integer) item.get("sortOrder");
            if (dashboardId != null && sortOrder != null) {
                PromWorkspaceDashboard wd = new PromWorkspaceDashboard();
                wd.setSortOrder(sortOrder);
                workspaceDashboardMapper.update(wd,
                        new LambdaQueryWrapper<PromWorkspaceDashboard>()
                                .eq(PromWorkspaceDashboard::getWorkspaceId, workspaceId)
                                .eq(PromWorkspaceDashboard::getDashboardId, dashboardId)
                );
            }
        }
    }
}
