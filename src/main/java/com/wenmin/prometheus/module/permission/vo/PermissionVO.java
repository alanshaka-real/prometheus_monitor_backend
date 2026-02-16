package com.wenmin.prometheus.module.permission.vo;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PermissionVO {
    private String id;
    private String name;
    private String code;
    private String type;
    private String parentId;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private List<PermissionVO> children;
}
