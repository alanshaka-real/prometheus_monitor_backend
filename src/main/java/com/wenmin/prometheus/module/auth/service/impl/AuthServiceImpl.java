package com.wenmin.prometheus.module.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wenmin.prometheus.common.exception.BusinessException;
import com.wenmin.prometheus.module.auth.dto.ChangePasswordDTO;
import com.wenmin.prometheus.module.auth.dto.LoginDTO;
import com.wenmin.prometheus.module.auth.dto.UserProfileDTO;
import com.wenmin.prometheus.module.auth.entity.SysUser;
import com.wenmin.prometheus.module.auth.mapper.SysUserMapper;
import com.wenmin.prometheus.module.auth.service.AuthService;
import com.wenmin.prometheus.module.auth.vo.LoginVO;
import com.wenmin.prometheus.module.auth.vo.UserInfoVO;
import com.wenmin.prometheus.module.permission.entity.SysRole;
import com.wenmin.prometheus.module.permission.entity.SysUserRole;
import com.wenmin.prometheus.module.permission.entity.SysPermission;
import com.wenmin.prometheus.module.permission.entity.SysRolePermission;
import com.wenmin.prometheus.module.permission.mapper.RoleMapper;
import com.wenmin.prometheus.module.permission.mapper.UserRoleMapper;
import com.wenmin.prometheus.module.permission.mapper.PermissionMapper;
import com.wenmin.prometheus.module.permission.mapper.RolePermissionMapper;
import com.wenmin.prometheus.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    @Override
    public LoginVO login(LoginDTO dto) {
        SysUser user = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, dto.getUserName()));
        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }
        if ("disabled".equals(user.getStatus())) {
            throw new BusinessException("账号已被禁用");
        }
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

        // Get role names
        List<String> roleNames = getRoleNames(user.getId());

        // Update last login
        user.setLastLogin(LocalDateTime.now());
        userMapper.updateById(user);

        // Generate tokens
        LoginVO vo = new LoginVO();
        vo.setToken(jwtTokenProvider.generateAccessToken(user.getId(), user.getUsername(), roleNames));
        vo.setRefreshToken(jwtTokenProvider.generateRefreshToken(user.getId()));
        return vo;
    }

    @Override
    public UserInfoVO getUserInfo(String userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(401, "用户不存在");
        }

        List<String> roleNames = getRoleNames(userId);
        List<String> buttons = getPermissionCodes(userId);

        UserInfoVO vo = new UserInfoVO();
        vo.setUserId(user.getId());
        vo.setUserName(user.getUsername());
        vo.setEmail(user.getEmail());
        vo.setPhone(user.getPhone());
        vo.setAvatar(user.getAvatar());
        vo.setRealName(user.getRealName());
        vo.setNickName(user.getNickName());
        vo.setGender(user.getGender());
        vo.setAddress(user.getAddress());
        vo.setDescription(user.getDescription());
        vo.setRoles(roleNames);
        vo.setButtons(buttons);
        return vo;
    }

    @Override
    @Transactional
    public UserInfoVO updateProfile(String userId, UserProfileDTO dto) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        if (dto.getRealName() != null) {
            user.setRealName(dto.getRealName());
        }
        if (dto.getNickName() != null) {
            user.setNickName(dto.getNickName());
        }
        if (dto.getEmail() != null) {
            user.setEmail(dto.getEmail());
        }
        if (dto.getPhone() != null) {
            user.setPhone(dto.getPhone());
        }
        if (dto.getGender() != null) {
            user.setGender(dto.getGender());
        }
        if (dto.getAddress() != null) {
            user.setAddress(dto.getAddress());
        }
        if (dto.getDescription() != null) {
            user.setDescription(dto.getDescription());
        }
        if (dto.getAvatar() != null) {
            user.setAvatar(dto.getAvatar());
        }
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);

        return getUserInfo(userId);
    }

    @Override
    @Transactional
    public void changePassword(String userId, ChangePasswordDTO dto) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getPassword())) {
            throw new BusinessException("当前密码不正确");
        }

        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            throw new BusinessException("两次输入的新密码不一致");
        }

        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
    }

    private List<String> getRoleNames(String userId) {
        List<SysUserRole> userRoles = userRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        if (userRoles.isEmpty()) return List.of();

        List<String> roleIds = userRoles.stream().map(SysUserRole::getRoleId).toList();
        List<SysRole> roles = roleMapper.selectBatchIds(roleIds);
        return roles.stream().map(SysRole::getCode).collect(Collectors.toList());
    }

    private List<String> getPermissionCodes(String userId) {
        List<SysUserRole> userRoles = userRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        if (userRoles.isEmpty()) return List.of();

        List<String> roleIds = userRoles.stream().map(SysUserRole::getRoleId).toList();
        List<SysRolePermission> rolePerms = rolePermissionMapper.selectList(
                new LambdaQueryWrapper<SysRolePermission>().in(SysRolePermission::getRoleId, roleIds));
        if (rolePerms.isEmpty()) return List.of();

        List<String> permIds = rolePerms.stream().map(SysRolePermission::getPermissionId).distinct().toList();
        List<SysPermission> perms = permissionMapper.selectBatchIds(permIds);
        return perms.stream().map(SysPermission::getCode).collect(Collectors.toList());
    }
}
