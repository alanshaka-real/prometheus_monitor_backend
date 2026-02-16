package com.wenmin.prometheus.module.permission.controller;

import com.wenmin.prometheus.annotation.AuditLog;
import com.wenmin.prometheus.common.result.R;
import com.wenmin.prometheus.module.permission.dto.PermissionDTO;
import com.wenmin.prometheus.module.permission.dto.RoleDTO;
import com.wenmin.prometheus.module.permission.dto.UserDTO;
import com.wenmin.prometheus.module.permission.service.PermissionService;
import com.wenmin.prometheus.module.permission.vo.PermissionVO;
import com.wenmin.prometheus.module.permission.vo.RoleVO;
import com.wenmin.prometheus.module.permission.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "权限管理")
@RestController
@RequestMapping("/api/prometheus")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    // ==================== Users ====================

    @Operation(summary = "获取用户列表")
    @GetMapping("/users")
    public R<Map<String, Object>> listUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String userName,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String status) {
        // Support both 'keyword' and 'userName' as search parameter
        String search = keyword != null ? keyword : userName;
        return R.ok(permissionService.listUsers(search, status, current, size));
    }

    @Operation(summary = "创建用户")
    @PostMapping("/users")
    @AuditLog(action = "创建", resource = "用户")
    public R<UserVO> createUser(@RequestBody UserDTO dto) {
        return R.ok(permissionService.createUser(dto));
    }

    @Operation(summary = "更新用户")
    @PutMapping("/users/{id}")
    @AuditLog(action = "更新", resource = "用户")
    public R<UserVO> updateUser(@PathVariable String id, @RequestBody UserDTO dto) {
        return R.ok(permissionService.updateUser(id, dto));
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/users/{id}")
    @AuditLog(action = "删除", resource = "用户")
    public R<Void> deleteUser(@PathVariable String id) {
        permissionService.deleteUser(id);
        return R.ok();
    }

    @Operation(summary = "启用/禁用用户")
    @PostMapping("/users/{id}/toggle")
    @AuditLog(action = "切换状态", resource = "用户")
    public R<Void> toggleUser(@PathVariable String id, @RequestBody Map<String, String> body) {
        permissionService.toggleUser(id, body.get("status"));
        return R.ok();
    }

    // ==================== Roles ====================

    @Operation(summary = "获取角色列表")
    @GetMapping("/roles")
    public R<Map<String, Object>> listRoles() {
        return R.ok(permissionService.listRoles());
    }

    @Operation(summary = "创建角色")
    @PostMapping("/roles")
    @AuditLog(action = "创建", resource = "角色")
    public R<RoleVO> createRole(@RequestBody RoleDTO dto) {
        return R.ok(permissionService.createRole(dto));
    }

    @Operation(summary = "更新角色")
    @PutMapping("/roles/{id}")
    @AuditLog(action = "更新", resource = "角色")
    public R<RoleVO> updateRole(@PathVariable String id, @RequestBody RoleDTO dto) {
        return R.ok(permissionService.updateRole(id, dto));
    }

    @Operation(summary = "删除角色")
    @DeleteMapping("/roles/{id}")
    @AuditLog(action = "删除", resource = "角色")
    public R<Void> deleteRole(@PathVariable String id) {
        permissionService.deleteRole(id);
        return R.ok();
    }

    // ==================== Permissions ====================

    @Operation(summary = "获取权限列表(树结构)")
    @GetMapping("/permissions")
    public R<Map<String, Object>> listPermissions() {
        return R.ok(permissionService.listPermissions());
    }

    @Operation(summary = "创建权限")
    @PostMapping("/permissions")
    @AuditLog(action = "创建", resource = "权限")
    public R<PermissionVO> createPermission(@RequestBody PermissionDTO dto) {
        return R.ok(permissionService.createPermission(dto));
    }

    @Operation(summary = "更新权限")
    @PutMapping("/permissions/{id}")
    @AuditLog(action = "更新", resource = "权限")
    public R<PermissionVO> updatePermission(@PathVariable String id, @RequestBody PermissionDTO dto) {
        return R.ok(permissionService.updatePermission(id, dto));
    }

    @Operation(summary = "删除权限")
    @DeleteMapping("/permissions/{id}")
    @AuditLog(action = "删除", resource = "权限")
    public R<Void> deletePermission(@PathVariable String id) {
        permissionService.deletePermission(id);
        return R.ok();
    }

    // ==================== Audit Logs ====================

    @Operation(summary = "获取审计日志列表")
    @GetMapping("/audit-logs")
    public R<Map<String, Object>> listAuditLogs(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        return R.ok(permissionService.listAuditLogs(userId, action, startTime, endTime));
    }
}
