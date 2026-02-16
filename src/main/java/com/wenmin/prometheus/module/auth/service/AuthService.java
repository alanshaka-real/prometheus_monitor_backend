package com.wenmin.prometheus.module.auth.service;

import com.wenmin.prometheus.module.auth.dto.ChangePasswordDTO;
import com.wenmin.prometheus.module.auth.dto.LoginDTO;
import com.wenmin.prometheus.module.auth.dto.UserProfileDTO;
import com.wenmin.prometheus.module.auth.vo.LoginVO;
import com.wenmin.prometheus.module.auth.vo.UserInfoVO;

public interface AuthService {
    LoginVO login(LoginDTO dto);
    UserInfoVO getUserInfo(String userId);
    UserInfoVO updateProfile(String userId, UserProfileDTO dto);
    void changePassword(String userId, ChangePasswordDTO dto);
}
