package com.wenmin.prometheus.module.permission.dto;

import lombok.Data;

@Data
public class PermissionDTO {
    private String name;
    private String code;
    private String type;       // "menu", "button", "api"
    private String parentId;
    private Integer sortOrder;
}
