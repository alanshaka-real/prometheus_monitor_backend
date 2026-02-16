package com.wenmin.prometheus.module.settings.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_system_log")
public class SysSystemLog {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String level;
    private String message;
    private String source;
    private String stackTrace;
    private LocalDateTime createdAt;
}
