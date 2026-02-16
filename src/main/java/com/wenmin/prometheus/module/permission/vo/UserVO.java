package com.wenmin.prometheus.module.permission.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class UserVO {
    private String id;

    @JsonProperty("userName")
    private String username;

    @JsonProperty("userEmail")
    private String email;

    @JsonProperty("userPhone")
    private String phone;

    private String avatar;

    @JsonProperty("userRoles")
    private List<String> roles;

    private String status;

    private String userGender;

    private String nickName;

    private LocalDateTime lastLogin;

    @JsonProperty("createTime")
    private LocalDateTime createdAt;
}
