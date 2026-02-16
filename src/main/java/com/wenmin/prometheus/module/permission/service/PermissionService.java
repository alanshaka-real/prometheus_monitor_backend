package com.wenmin.prometheus.module.permission.service;

import com.wenmin.prometheus.module.permission.dto.PermissionDTO;
import com.wenmin.prometheus.module.permission.dto.RoleDTO;
import com.wenmin.prometheus.module.permission.dto.UserDTO;
import com.wenmin.prometheus.module.permission.vo.PermissionVO;
import com.wenmin.prometheus.module.permission.vo.RoleVO;
import com.wenmin.prometheus.module.permission.vo.UserVO;

import java.util.List;
import java.util.Map;

public interface PermissionService {

    // ---- Users ----

    Map<String, Object> listUsers(String keyword, String status, Integer current, Integer size);

    UserVO createUser(UserDTO dto);

    UserVO updateUser(String id, UserDTO dto);

    void deleteUser(String id);

    void toggleUser(String id, String status);

    // ---- Roles ----

    Map<String, Object> listRoles();

    RoleVO createRole(RoleDTO dto);

    RoleVO updateRole(String id, RoleDTO dto);

    void deleteRole(String id);

    // ---- Permissions ----

    Map<String, Object> listPermissions();

    PermissionVO createPermission(PermissionDTO dto);

    PermissionVO updatePermission(String id, PermissionDTO dto);

    void deletePermission(String id);

    // ---- Audit Logs ----

    Map<String, Object> listAuditLogs(String userId, String action, String startTime, String endTime);
}
