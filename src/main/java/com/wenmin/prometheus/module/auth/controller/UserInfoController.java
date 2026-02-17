package com.wenmin.prometheus.module.auth.controller;

import com.wenmin.prometheus.annotation.AuditLog;
import com.wenmin.prometheus.common.result.R;
import com.wenmin.prometheus.module.auth.dto.ChangePasswordDTO;
import com.wenmin.prometheus.module.auth.dto.UserProfileDTO;
import com.wenmin.prometheus.module.auth.service.AuthService;
import com.wenmin.prometheus.module.auth.vo.UserInfoVO;
import com.wenmin.prometheus.security.SecurityUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "用户信息")
@RestController
@RequestMapping("/api/prometheus")
@RequiredArgsConstructor
public class UserInfoController {

    private final AuthService authService;

    @Operation(summary = "获取用户信息")
    @GetMapping("/user/info")
    public R<UserInfoVO> getUserInfo(@AuthenticationPrincipal SecurityUser user) {
        return R.ok(authService.getUserInfo(user.getUserId()));
    }

    @Operation(summary = "更新个人资料")
    @PutMapping("/user/profile")
    @AuditLog(action = "更新", resource = "个人资料")
    public R<UserInfoVO> updateProfile(
            @AuthenticationPrincipal SecurityUser user,
            @Valid @RequestBody UserProfileDTO dto) {
        return R.ok(authService.updateProfile(user.getUserId(), dto));
    }

    @Operation(summary = "修改密码")
    @PutMapping("/user/password")
    @AuditLog(action = "修改", resource = "密码")
    public R<Void> changePassword(
            @AuthenticationPrincipal SecurityUser user,
            @Valid @RequestBody ChangePasswordDTO dto) {
        authService.changePassword(user.getUserId(), dto);
        return R.ok();
    }
}
