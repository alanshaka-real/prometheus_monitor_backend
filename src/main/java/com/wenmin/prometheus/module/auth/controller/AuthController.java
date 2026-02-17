package com.wenmin.prometheus.module.auth.controller;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.wenmin.prometheus.common.result.R;
import com.wenmin.prometheus.module.auth.dto.LoginDTO;
import com.wenmin.prometheus.module.auth.service.AuthService;
import com.wenmin.prometheus.module.auth.vo.LoginVO;
import com.wenmin.prometheus.security.JwtTokenProvider;
import com.wenmin.prometheus.security.LoginRateLimiter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "认证管理")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final LoginRateLimiter rateLimiter;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${jwt.access-token-expire}")
    private long accessTokenExpire;

    @Operation(summary = "登录")
    @PostMapping("/auth/login")
    public R<LoginVO> login(@Valid @RequestBody LoginDTO dto,
                            HttpServletRequest request,
                            HttpServletResponse response) {
        String clientIp = getClientIp(request);
        if (!rateLimiter.tryAcquire(clientIp)) {
            return R.fail(429, "登录尝试过于频繁，请稍后再试");
        }
        LoginVO loginVO = authService.login(dto);

        // Set httpOnly cookie for access token
        setTokenCookie(response, "access_token", loginVO.getToken(), (int) (accessTokenExpire / 1000));

        // Also return token in body for backward compatibility
        return R.ok(loginVO);
    }

    @Operation(summary = "刷新令牌")
    @PostMapping("/auth/refresh")
    public R<Map<String, String>> refreshToken(@RequestBody Map<String, String> body,
                                               HttpServletResponse response) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken == null || refreshToken.isBlank()) {
            return R.fail(400, "refreshToken 不能为空");
        }

        DecodedJWT jwt = jwtTokenProvider.verifyToken(refreshToken);
        if (jwt == null) {
            return R.fail(401, "refreshToken 无效或已过期");
        }

        String userId = jwtTokenProvider.getUserId(jwt);
        String newAccessToken = authService.generateNewAccessToken(userId);

        // Update httpOnly cookie
        setTokenCookie(response, "access_token", newAccessToken, (int) (accessTokenExpire / 1000));

        Map<String, String> result = new HashMap<>();
        result.put("token", newAccessToken);
        result.put("refreshToken", refreshToken);
        return R.ok(result);
    }

    @Operation(summary = "退出登录")
    @PostMapping("/auth/logout")
    public R<Void> logout(HttpServletResponse response) {
        // Clear the httpOnly cookie
        setTokenCookie(response, "access_token", "", 0);
        return R.ok();
    }

    private void setTokenCookie(HttpServletResponse response, String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setPath("/api");
        cookie.setMaxAge(maxAge);
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
    }

    /**
     * 获取客户端真实 IP，支持代理头。
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) {
            // 取第一个 IP（可能经过多级代理）
            ip = ip.split(",")[0].trim();
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
