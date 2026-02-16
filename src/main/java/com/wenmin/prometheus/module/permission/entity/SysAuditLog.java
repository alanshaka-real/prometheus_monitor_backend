package com.wenmin.prometheus.module.permission.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_audit_log")
public class SysAuditLog {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String userId;
    private String username;
    private String action;
    private String resource;
    private String resourceId;
    private String detail;
    private String ip;
    private String userAgent;
    private String status;
    private LocalDateTime timestamp;
}
