package com.wenmin.prometheus.module.permission.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_permission")
public class SysPermission {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String name;
    private String code;
    private String type;
    private String parentId;
    private Integer sortOrder;
    private LocalDateTime createdAt;
}
