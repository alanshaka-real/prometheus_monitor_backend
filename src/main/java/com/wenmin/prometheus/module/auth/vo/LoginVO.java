package com.wenmin.prometheus.module.auth.vo;

import lombok.Data;

@Data
public class LoginVO {
    private String token;
    private String refreshToken;
}
