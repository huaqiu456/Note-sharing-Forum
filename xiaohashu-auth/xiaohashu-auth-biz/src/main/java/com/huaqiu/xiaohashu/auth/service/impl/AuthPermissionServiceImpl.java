package com.huaqiu.xiaohashu.auth.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.huaqiu.xiaohashu.auth.domain.dto.UserAuthReloadRspDto;
import com.huaqiu.xiaohashu.auth.domain.po.Permission;
import com.huaqiu.xiaohashu.auth.domain.po.Role;
import com.huaqiu.xiaohashu.auth.domain.po.RolePermission;
import com.huaqiu.xiaohashu.auth.mapper.PermissionMapper;
import com.huaqiu.xiaohashu.auth.mapper.RoleMapper;
import com.huaqiu.xiaohashu.auth.mapper.RolePermissionMapper;
import com.huaqiu.xiaohashu.auth.mapper.UserRoleMapper;
import com.huaqiu.xiaohashu.auth.service.interfaces.AuthPermissionService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 权限回源服务实现
 *
 * 数据来源（数据库是唯一事实来源）：
 *   t_user_role_rel + t_role            -> 用户角色
 *   t_role_permission_rel + t_permission -> 角色权限
 *
 * <p><b>改造说明（v2 权限进会话）</b>：本类原来还会把结果回写到 Redis 的
 * {@code user:roles:{userId}} / {@code role:permissions:{roleKey}} 两个 key。
 * v2 改为「登录时写入 Sa-Token 会话」，网关只读会话，这两个 key 已无任何读取方，
 * 因此这里不再回写 Redis —— 本类只负责「查库 + 组装」并返回结果。
 */
@Service
@Slf4j
public class AuthPermissionServiceImpl implements AuthPermissionService {

    @Resource
    private UserRoleMapper userRoleMapper;

    @Resource
    private RoleMapper roleMapper;

    @Resource
    private RolePermissionMapper rolePermissionMapper;

    @Resource
    private PermissionMapper permissionMapper;

    /**
     * 查询用户的角色与权限（只查库组装，不写任何缓存）
     *
     * <p>步骤：
     * <ol>
     *   <li>{@code t_user_role_rel} → 角色 ID 列表</li>
     *   <li>{@code t_role} → 过滤出「启用（status=0）且未逻辑删除」的角色，取 roleKey</li>
     *   <li>{@code t_role_permission_rel} → 角色 ID 到权限 ID 的映射</li>
     *   <li>{@code t_permission}（APP 端、启用中）→ 权限 ID 到 permissionKey 的映射</li>
     *   <li>汇总：角色标识列表 + 所有角色权限的并集（去重、保持顺序）</li>
     * </ol>
     *
     * <p>任何一步为空都返回 {@link UserAuthReloadRspDto#empty()}，调用方据此按「无权限」处理。
     *
     * <p>返回值会被 {@code SessionAuthService} 写入 Sa-Token 会话；本方法自身不碰 Redis。
     *
     * @param userId 用户 ID
     * @return 角色与权限；无有效数据时返回空对象（角色与权限均为空列表）
     */
    @Override
    public UserAuthReloadRspDto reloadUserAuth(Long userId) {

        // 1. 用户 -> 角色ID
        List<Long> roleIds = userRoleMapper.selectRoleIdsByUserId(userId);
        if (CollUtil.isEmpty(roleIds)) {
            log.warn("## 权限回源：用户无任何角色, userId: {}", userId);
            return UserAuthReloadRspDto.empty();
        }

        // 2. 角色ID -> 启用的角色
        List<Role> roles = roleIds.stream()
                .map(roleMapper::selectByPrimaryKey)
                .filter(Objects::nonNull)
                .filter(role -> Objects.equals(role.getStatus(), (byte) 0))
                .filter(role -> !Boolean.TRUE.equals(role.getIsDeleted()))
                .toList();

        if (CollUtil.isEmpty(roles)) {
            log.warn("## 权限回源：用户角色均已停用或不存在, userId: {}", userId);
            return UserAuthReloadRspDto.empty();
        }

        // 3. 角色 -> 权限ID
        List<Long> enabledRoleIds = roles.stream().map(Role::getId).toList();
        List<RolePermission> rolePermissions = rolePermissionMapper.selectByRoleIds(enabledRoleIds);

        Map<Long, List<Long>> roleIdPermissionIdsMap = CollUtil.isEmpty(rolePermissions)
                ? Map.of()
                : rolePermissions.stream()
                        .filter(rp -> !Boolean.TRUE.equals(rp.getIsDeleted()))
                        .collect(Collectors.groupingBy(
                                RolePermission::getRoleId,
                                Collectors.mapping(RolePermission::getPermissionId, Collectors.toList())));

        // 4. 权限ID -> 启用中的 APP 端权限
        List<Permission> appPermissions = permissionMapper.selectAppEnabledList();
        Map<Long, String> permissionIdKeyMap = CollUtil.isEmpty(appPermissions)
                ? Map.of()
                : appPermissions.stream()
                        .filter(p -> !Boolean.TRUE.equals(p.getIsDeleted()))
                        .collect(Collectors.toMap(Permission::getId, Permission::getPermissionKey, (a, b) -> a));

        // 5. 组装并汇总用户全部权限
        List<String> roleKeys = new ArrayList<>();
        Set<String> allPermissions = new LinkedHashSet<>();

        for (Role role : roles) {
            List<Long> permissionIds = roleIdPermissionIdsMap.getOrDefault(role.getId(), List.of());
            List<String> permissionKeys = permissionIds.stream()
                    .map(permissionIdKeyMap::get)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();

            if (CollUtil.isEmpty(permissionKeys)) {
                continue;
            }
            roleKeys.add(role.getRoleKey());
            allPermissions.addAll(permissionKeys);
        }

        // 6. 无有效权限：返回空
        if (allPermissions.isEmpty()) {
            log.warn("## 权限回源：用户无任何有效权限, userId: {}", userId);
            return UserAuthReloadRspDto.empty();
        }

        log.info("## 权限查询成功, userId: {}, roleKeys: {}, permissions: {}", userId, roleKeys, allPermissions);

        return UserAuthReloadRspDto.builder()
                .roleKeys(roleKeys)
                .permissions(new ArrayList<>(allPermissions))
                .build();
    }
}
