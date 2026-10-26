package com.huaqiu.xiaohashu.auth.service.interfaces;

import com.huaqiu.xiaohashu.auth.domain.dto.UserAuthReloadRspDto;

/**
 * 用户角色与权限查询服务（接口）
 *
 * <p>数据来源为 MySQL：t_user_role_rel + t_role + t_role_permission_rel + t_permission。
 *
 * <p><b>改造说明（v2 权限进会话）</b>：调用方由「网关回源」改为「auth 登录流程」——
 * 登录成功时拿到结果后写入 Sa-Token 会话（见 {@link SessionAuthService}）。
 * 网关不再调用本服务（v1 的跨服务回源链路已整体删除）。
 *
 * @see com.huaqiu.xiaohashu.auth.service.impl.AuthPermissionServiceImpl
 */
public interface AuthPermissionService {

    /**
     * 查询指定用户的角色与权限（只查库组装，不写缓存）。
     *
     * @param userId 用户 ID
     * @return 角色与权限；未查到有效数据时返回空对象
     */
    UserAuthReloadRspDto reloadUserAuth(Long userId);
}
