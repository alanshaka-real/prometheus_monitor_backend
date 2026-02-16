package com.wenmin.prometheus.module.permission.dto;

import lombok.Data;
import java.util.List;

@Data
public class RoleDTO {
    private String name;
    private String code;
    private String description;
    private List<String> permissionIds;
}
