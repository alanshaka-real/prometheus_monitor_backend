package com.wenmin.prometheus.module.auth.vo;

import lombok.Data;
import java.util.List;

@Data
public class UserInfoVO {
    private String userId;
    private String userName;
    private String email;
    private String phone;
    private String avatar;
    private String realName;
    private String nickName;
    private String gender;
    private String address;
    private String description;
    private List<String> roles;
    private List<String> buttons;
}
