package com.wenmin.prometheus.module.permission.dto;

import lombok.Data;
import java.util.List;

@Data
public class UserDTO {
    private String username;
    private String password;
    private String email;
    private String phone;
    private String avatar;
    private List<String> roleIds;
}
