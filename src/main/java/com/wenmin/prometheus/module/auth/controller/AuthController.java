package com.wenmin.prometheus.module.auth.controller;

import com.wenmin.prometheus.common.result.R;
import com.wenmin.prometheus.module.auth.dto.LoginDTO;
import com.wenmin.prometheus.module.auth.service.AuthService;
import com.wenmin.prometheus.module.auth.vo.LoginVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "认证管理")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "登录")
    @PostMapping("/auth/login")
    public R<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return R.ok(authService.login(dto));
    }
}
