package com.wenmin.prometheus.module.permission.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class RoleVO {
    @JsonProperty("roleId")
    private String id;

    @JsonProperty("roleName")
    private String name;

    @JsonProperty("roleCode")
    private String code;

    private String description;

    private List<PermissionVO> permissions;

    private Boolean builtIn;

    private Boolean enabled;

    private Integer userCount;

    @JsonProperty("createTime")
    private LocalDateTime createdAt;
}
