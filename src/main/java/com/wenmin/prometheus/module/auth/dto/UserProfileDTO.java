package com.wenmin.prometheus.module.auth.dto;

import lombok.Data;

@Data
public class UserProfileDTO {
    private String realName;
    private String nickName;
    private String email;
    private String phone;
    private String gender;
    private String address;
    private String description;
    private String avatar;
}
