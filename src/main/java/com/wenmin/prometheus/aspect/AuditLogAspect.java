package com.wenmin.prometheus.aspect;

import com.wenmin.prometheus.annotation.AuditLog;
import com.wenmin.prometheus.module.permission.entity.SysAuditLog;
import com.wenmin.prometheus.module.permission.mapper.AuditLogMapper;
import com.wenmin.prometheus.security.SecurityUser;
import com.wenmin.prometheus.util.IpUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {

    private final AuditLogMapper auditLogMapper;

    @Around("@annotation(auditLog)")
    public Object around(ProceedingJoinPoint point, AuditLog auditLog) throws Throwable {
        SysAuditLog logEntry = new SysAuditLog();
        logEntry.setId(UUID.randomUUID().toString());
        logEntry.setAction(auditLog.action());
        logEntry.setResource(auditLog.resource());
        logEntry.setTimestamp(LocalDateTime.now());

        // Get current user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof SecurityUser user) {
            logEntry.setUserId(user.getUserId());
            logEntry.setUsername(user.getUsername());
        }

        // Get request info
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            logEntry.setIp(IpUtil.getClientIp(request));
            logEntry.setUserAgent(request.getHeader("User-Agent"));
        }

        try {
            Object result = point.proceed();
            logEntry.setStatus("success");
            logEntry.setDetail(auditLog.action() + auditLog.resource() + " - 成功");
            return result;
        } catch (Exception e) {
            logEntry.setStatus("failure");
            logEntry.setDetail(auditLog.action() + auditLog.resource() + " - 失败: " + e.getMessage());
            throw e;
        } finally {
            try {
                auditLogMapper.insert(logEntry);
            } catch (Exception e) {
                log.error("Failed to save audit log", e);
            }
        }
    }
}
