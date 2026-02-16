package com.wenmin.prometheus.module.permission.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wenmin.prometheus.common.exception.BusinessException;
import com.wenmin.prometheus.module.auth.entity.SysUser;
import com.wenmin.prometheus.module.auth.mapper.SysUserMapper;
import com.wenmin.prometheus.module.permission.dto.PermissionDTO;
import com.wenmin.prometheus.module.permission.dto.RoleDTO;
import com.wenmin.prometheus.module.permission.dto.UserDTO;
import com.wenmin.prometheus.module.permission.entity.*;
import com.wenmin.prometheus.module.permission.mapper.*;
import com.wenmin.prometheus.module.permission.service.PermissionService;
import com.wenmin.prometheus.module.permission.vo.PermissionVO;
import com.wenmin.prometheus.module.permission.vo.RoleVO;
import com.wenmin.prometheus.module.permission.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final SysUserMapper sysUserMapper;
    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final UserRoleMapper userRoleMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final AuditLogMapper auditLogMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    // ==================== Users ====================

    @Override
    public Map<String, Object> listUsers(String keyword, String status, Integer current, Integer size) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(SysUser::getUsername, keyword);
        }
        if (StringUtils.hasText(status)) {
            // Map frontend status values to database values
            String dbStatus = mapFrontendStatusToDb(status);
            if (dbStatus != null) {
                wrapper.eq(SysUser::getStatus, dbStatus);
            }
        }
        wrapper.orderByDesc(SysUser::getCreatedAt);

        // Paginated query
        Page<SysUser> page = new Page<>(current, size);
        IPage<SysUser> pageResult = sysUserMapper.selectPage(page, wrapper);

        List<UserVO> voList = pageResult.getRecords().stream()
                .map(this::convertToUserVO)
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("records", voList);
        result.put("list", voList);
        result.put("current", pageResult.getCurrent());
        result.put("size", pageResult.getSize());
        result.put("total", pageResult.getTotal());
        return result;
    }

    /**
     * Map frontend status code to database status value.
     * Frontend: '1'(在线), '2'(离线), '3'(异常) -> DB: 'active'
     * Frontend: '4'(注销) -> DB: 'disabled'
     */
    private String mapFrontendStatusToDb(String frontendStatus) {
        switch (frontendStatus) {
            case "1":
            case "2":
            case "3":
                return "active";
            case "4":
                return "disabled";
            default:
                // If it's already a DB value (e.g. 'active'/'disabled'), pass through
                return frontendStatus;
        }
    }

    /**
     * Map database status value to frontend status code.
     * DB: 'active' -> Frontend: '1'
     * DB: 'disabled' -> Frontend: '4'
     */
    private String mapDbStatusToFrontend(String dbStatus) {
        if ("active".equals(dbStatus)) {
            return "1";
        } else if ("disabled".equals(dbStatus)) {
            return "4";
        }
        return dbStatus;
    }

    @Override
    @Transactional
    public UserVO createUser(UserDTO dto) {
        // Check if username already exists
        LambdaQueryWrapper<SysUser> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(SysUser::getUsername, dto.getUsername());
        if (sysUserMapper.selectCount(checkWrapper) > 0) {
            throw new BusinessException("用户名已存在");
        }

        // Create user
        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setAvatar(dto.getAvatar());
        user.setStatus("active");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        sysUserMapper.insert(user);

        // Insert user-role mappings
        if (dto.getRoleIds() != null && !dto.getRoleIds().isEmpty()) {
            for (String roleId : dto.getRoleIds()) {
                SysUserRole userRole = new SysUserRole();
                userRole.setUserId(user.getId());
                userRole.setRoleId(roleId);
                userRoleMapper.insert(userRole);
            }
        }

        return convertToUserVO(user);
    }

    @Override
    @Transactional
    public UserVO updateUser(String id, UserDTO dto) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // Update user fields
        if (StringUtils.hasText(dto.getUsername())) {
            user.setUsername(dto.getUsername());
        }
        if (StringUtils.hasText(dto.getPassword())) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
        if (dto.getEmail() != null) {
            user.setEmail(dto.getEmail());
        }
        if (dto.getPhone() != null) {
            user.setPhone(dto.getPhone());
        }
        if (dto.getAvatar() != null) {
            user.setAvatar(dto.getAvatar());
        }
        user.setUpdatedAt(LocalDateTime.now());
        sysUserMapper.updateById(user);

        // Update user-role mappings: delete old, insert new
        if (dto.getRoleIds() != null) {
            LambdaQueryWrapper<SysUserRole> deleteWrapper = new LambdaQueryWrapper<>();
            deleteWrapper.eq(SysUserRole::getUserId, id);
            userRoleMapper.delete(deleteWrapper);

            for (String roleId : dto.getRoleIds()) {
                SysUserRole userRole = new SysUserRole();
                userRole.setUserId(id);
                userRole.setRoleId(roleId);
                userRoleMapper.insert(userRole);
            }
        }

        return convertToUserVO(sysUserMapper.selectById(id));
    }

    @Override
    public void deleteUser(String id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        sysUserMapper.deleteById(id);
    }

    @Override
    public void toggleUser(String id, String status) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        // Map frontend status to database value
        user.setStatus(mapFrontendStatusToDb(status));
        user.setUpdatedAt(LocalDateTime.now());
        sysUserMapper.updateById(user);
    }

    // ==================== Roles ====================

    @Override
    public Map<String, Object> listRoles() {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(SysRole::getCreatedAt);
        List<SysRole> roles = roleMapper.selectList(wrapper);

        List<RoleVO> voList = roles.stream().map(this::convertToRoleVO).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("list", voList);
        result.put("total", voList.size());
        return result;
    }

    @Override
    @Transactional
    public RoleVO createRole(RoleDTO dto) {
        // Check if role code already exists
        LambdaQueryWrapper<SysRole> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(SysRole::getCode, dto.getCode());
        if (roleMapper.selectCount(checkWrapper) > 0) {
            throw new BusinessException("角色编码已存在");
        }

        SysRole role = new SysRole();
        role.setName(dto.getName());
        role.setCode(dto.getCode());
        role.setDescription(dto.getDescription());
        role.setBuiltIn(false);
        role.setUserCount(0);
        role.setCreatedAt(LocalDateTime.now());
        role.setUpdatedAt(LocalDateTime.now());
        roleMapper.insert(role);

        // Insert role-permission mappings
        if (dto.getPermissionIds() != null && !dto.getPermissionIds().isEmpty()) {
            for (String permissionId : dto.getPermissionIds()) {
                SysRolePermission rp = new SysRolePermission();
                rp.setRoleId(role.getId());
                rp.setPermissionId(permissionId);
                rolePermissionMapper.insert(rp);
            }
        }

        return convertToRoleVO(role);
    }

    @Override
    @Transactional
    public RoleVO updateRole(String id, RoleDTO dto) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException("角色不存在");
        }

        if (StringUtils.hasText(dto.getName())) {
            role.setName(dto.getName());
        }
        if (StringUtils.hasText(dto.getCode())) {
            role.setCode(dto.getCode());
        }
        if (dto.getDescription() != null) {
            role.setDescription(dto.getDescription());
        }
        role.setUpdatedAt(LocalDateTime.now());
        roleMapper.updateById(role);

        // Update role-permission mappings: delete old, insert new
        if (dto.getPermissionIds() != null) {
            LambdaQueryWrapper<SysRolePermission> deleteWrapper = new LambdaQueryWrapper<>();
            deleteWrapper.eq(SysRolePermission::getRoleId, id);
            rolePermissionMapper.delete(deleteWrapper);

            for (String permissionId : dto.getPermissionIds()) {
                SysRolePermission rp = new SysRolePermission();
                rp.setRoleId(id);
                rp.setPermissionId(permissionId);
                rolePermissionMapper.insert(rp);
            }
        }

        return convertToRoleVO(roleMapper.selectById(id));
    }

    @Override
    public void deleteRole(String id) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException("角色不存在");
        }
        if (Boolean.TRUE.equals(role.getBuiltIn())) {
            throw new BusinessException("内置角色不可删除");
        }
        roleMapper.deleteById(id);
    }

    // ==================== Permissions ====================

    @Override
    public Map<String, Object> listPermissions() {
        List<SysPermission> allPermissions = permissionMapper.selectList(
                new LambdaQueryWrapper<SysPermission>().orderByAsc(SysPermission::getSortOrder)
        );

        List<PermissionVO> tree = buildPermissionTree(allPermissions);

        Map<String, Object> result = new HashMap<>();
        result.put("list", tree);
        result.put("total", allPermissions.size());
        return result;
    }

    @Override
    @Transactional
    public PermissionVO createPermission(PermissionDTO dto) {
        // Check if permission code already exists
        LambdaQueryWrapper<SysPermission> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(SysPermission::getCode, dto.getCode());
        if (permissionMapper.selectCount(checkWrapper) > 0) {
            throw new BusinessException("权限编码已存在");
        }

        SysPermission permission = new SysPermission();
        permission.setName(dto.getName());
        permission.setCode(dto.getCode());
        permission.setType(dto.getType());
        permission.setParentId(dto.getParentId() != null ? dto.getParentId() : "");
        permission.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        permission.setCreatedAt(LocalDateTime.now());
        permissionMapper.insert(permission);

        return convertToPermissionVO(permission);
    }

    @Override
    @Transactional
    public PermissionVO updatePermission(String id, PermissionDTO dto) {
        SysPermission permission = permissionMapper.selectById(id);
        if (permission == null) {
            throw new BusinessException("权限不存在");
        }

        if (StringUtils.hasText(dto.getName())) {
            permission.setName(dto.getName());
        }
        if (StringUtils.hasText(dto.getCode())) {
            permission.setCode(dto.getCode());
        }
        if (StringUtils.hasText(dto.getType())) {
            permission.setType(dto.getType());
        }
        if (dto.getParentId() != null) {
            permission.setParentId(dto.getParentId());
        }
        if (dto.getSortOrder() != null) {
            permission.setSortOrder(dto.getSortOrder());
        }
        permissionMapper.updateById(permission);

        return convertToPermissionVO(permissionMapper.selectById(id));
    }

    @Override
    @Transactional
    public void deletePermission(String id) {
        SysPermission permission = permissionMapper.selectById(id);
        if (permission == null) {
            throw new BusinessException("权限不存在");
        }

        // Check if has children
        LambdaQueryWrapper<SysPermission> childWrapper = new LambdaQueryWrapper<>();
        childWrapper.eq(SysPermission::getParentId, id);
        if (permissionMapper.selectCount(childWrapper) > 0) {
            throw new BusinessException("该权限存在子节点，无法删除");
        }

        // Check if referenced by roles
        LambdaQueryWrapper<SysRolePermission> rpWrapper = new LambdaQueryWrapper<>();
        rpWrapper.eq(SysRolePermission::getPermissionId, id);
        if (rolePermissionMapper.selectCount(rpWrapper) > 0) {
            throw new BusinessException("该权限已被角色引用，请先移除角色关联");
        }

        permissionMapper.deleteById(id);
    }

    // ==================== Audit Logs ====================

    @Override
    public Map<String, Object> listAuditLogs(String userId, String action, String startTime, String endTime) {
        LambdaQueryWrapper<SysAuditLog> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(userId)) {
            wrapper.eq(SysAuditLog::getUserId, userId);
        }
        if (StringUtils.hasText(action)) {
            wrapper.eq(SysAuditLog::getAction, action);
        }
        if (StringUtils.hasText(startTime)) {
            wrapper.ge(SysAuditLog::getTimestamp, LocalDateTime.parse(startTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }
        if (StringUtils.hasText(endTime)) {
            wrapper.le(SysAuditLog::getTimestamp, LocalDateTime.parse(endTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }
        wrapper.orderByDesc(SysAuditLog::getTimestamp);

        List<SysAuditLog> logs = auditLogMapper.selectList(wrapper);

        Map<String, Object> result = new HashMap<>();
        result.put("list", logs);
        result.put("total", logs.size());
        return result;
    }

    // ==================== Helper Methods ====================

    private UserVO convertToUserVO(SysUser user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setEmail(user.getEmail());
        vo.setPhone(user.getPhone());
        vo.setAvatar(user.getAvatar());
        vo.setStatus(mapDbStatusToFrontend(user.getStatus()));
        vo.setLastLogin(user.getLastLogin());
        vo.setCreatedAt(user.getCreatedAt());

        // Get role names for this user
        LambdaQueryWrapper<SysUserRole> urWrapper = new LambdaQueryWrapper<>();
        urWrapper.eq(SysUserRole::getUserId, user.getId());
        List<SysUserRole> userRoles = userRoleMapper.selectList(urWrapper);

        if (!userRoles.isEmpty()) {
            List<String> roleIds = userRoles.stream()
                    .map(SysUserRole::getRoleId)
                    .collect(Collectors.toList());
            List<SysRole> roles = roleMapper.selectBatchIds(roleIds);
            List<String> roleNames = roles.stream()
                    .map(SysRole::getName)
                    .collect(Collectors.toList());
            vo.setRoles(roleNames);
        } else {
            vo.setRoles(new ArrayList<>());
        }

        return vo;
    }

    private RoleVO convertToRoleVO(SysRole role) {
        RoleVO vo = new RoleVO();
        vo.setId(role.getId());
        vo.setName(role.getName());
        vo.setCode(role.getCode());
        vo.setDescription(role.getDescription());
        vo.setBuiltIn(role.getBuiltIn());
        vo.setEnabled(true); // Default to enabled; builtIn roles are always enabled
        vo.setUserCount(role.getUserCount());
        vo.setCreatedAt(role.getCreatedAt());

        // Get permissions for this role
        LambdaQueryWrapper<SysRolePermission> rpWrapper = new LambdaQueryWrapper<>();
        rpWrapper.eq(SysRolePermission::getRoleId, role.getId());
        List<SysRolePermission> rolePermissions = rolePermissionMapper.selectList(rpWrapper);

        if (!rolePermissions.isEmpty()) {
            List<String> permissionIds = rolePermissions.stream()
                    .map(SysRolePermission::getPermissionId)
                    .collect(Collectors.toList());
            List<SysPermission> permissions = permissionMapper.selectBatchIds(permissionIds);
            List<PermissionVO> permissionVOs = permissions.stream()
                    .map(this::convertToPermissionVO)
                    .collect(Collectors.toList());
            vo.setPermissions(permissionVOs);
        } else {
            vo.setPermissions(new ArrayList<>());
        }

        return vo;
    }

    private PermissionVO convertToPermissionVO(SysPermission permission) {
        PermissionVO vo = new PermissionVO();
        vo.setId(permission.getId());
        vo.setName(permission.getName());
        vo.setCode(permission.getCode());
        vo.setType(permission.getType());
        vo.setParentId(permission.getParentId());
        vo.setSortOrder(permission.getSortOrder());
        vo.setCreatedAt(permission.getCreatedAt());
        return vo;
    }

    private List<PermissionVO> buildPermissionTree(List<SysPermission> allPermissions) {
        // Convert all permissions to VOs
        List<PermissionVO> allVOs = allPermissions.stream()
                .map(this::convertToPermissionVO)
                .collect(Collectors.toList());

        // Build a map of id -> PermissionVO for quick lookup
        Map<String, PermissionVO> voMap = new LinkedHashMap<>();
        for (PermissionVO vo : allVOs) {
            vo.setChildren(new ArrayList<>());
            voMap.put(vo.getId(), vo);
        }

        // Build tree: root nodes have empty or null parentId
        List<PermissionVO> roots = new ArrayList<>();
        for (PermissionVO vo : allVOs) {
            if (!StringUtils.hasText(vo.getParentId())) {
                roots.add(vo);
            } else {
                PermissionVO parent = voMap.get(vo.getParentId());
                if (parent != null) {
                    parent.getChildren().add(vo);
                } else {
                    // Orphan node, treat as root
                    roots.add(vo);
                }
            }
        }

        return roots;
    }
}
