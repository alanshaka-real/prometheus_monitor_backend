package com.wenmin.prometheus.module.workspace.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName(value = "prom_workspace_dashboard")
public class PromWorkspaceDashboard {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String workspaceId;

    private String dashboardId;

    private Integer sortOrder;

    private LocalDateTime publishedAt;

    private String publishedBy;
}
