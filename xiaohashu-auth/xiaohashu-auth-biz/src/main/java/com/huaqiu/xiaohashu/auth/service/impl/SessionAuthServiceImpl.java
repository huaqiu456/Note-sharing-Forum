package com.huaqiu.xiaohashu.auth.service.impl;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import com.huaqiu.xiaohashu.auth.domain.dto.UserAuthReloadRspDto;
import com.huaqiu.xiaohashu.auth.service.interfaces.AuthPermissionService;
import com.huaqiu.xiaohashu.auth.service.interfaces.SessionAuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 权限快照写入 Sa-Token 会话的服务（实现）
 *
 * <p>对应文档：版本/v2/2026-10-13-角色权限进会话方案/04-详细设计.md
 *
 * <p><b>注意（关键）</b>：Sa-Token 的 Account-Session 默认 `timeout = -1`（永不过期），
 * 所以写入后<b>必须</b>调用 {@code updateTimeout(...)}，否则权限快照会永久驻留 Redis。
 *
 * @see com.huaqiu.xiaohashu.auth.service.interfaces.SessionAuthService
 */
@Service
@Slf4j
public class SessionAuthServiceImpl implements SessionAuthService {

    private final AuthPermissionService authPermissionService;

    /** 会话超时时间（秒），与 sa-token.timeout 对齐，默认 30 天 */
    private final long sessionTimeoutSeconds;

    /**
     * @param authPermissionService 查库组装用户角色与权限（已有能力，直接复用）
     * @param sessionTimeoutSeconds 会话超时秒数，取自 {@code sa-token.timeout}，默认 2592000（30 天）
     */
    public SessionAuthServiceImpl(AuthPermissionService authPermissionService,
                                  @Value("${sa-token.timeout:2592000}") long sessionTimeoutSeconds) {
        this.authPermissionService = authPermissionService;
        this.sessionTimeoutSeconds = sessionTimeoutSeconds;
    }

    /**
     * {@inheritDoc}
     *
     * <p>先查库算出角色与权限，然后直接写入 Sa-Token 的 Account-Session。
     *
     * <p>写入细节：
     * <ol>
     *   <li>空值防御：{@code auth} 为 null 或字段为 null 时统一按空列表处理</li>
     *   <li>用 {@link StpUtil#getSessionByLoginId(Object)} 取会话，不存在时创建</li>
     *   <li>分别写入角色列表和权限列表，避免链式调用</li>
     *   <li>调用 {@code updateTimeout} 设置会话 TTL，避免权限快照永久驻留 Redis</li>
     * </ol>
     */
    @Override
    public void refreshSessionAuth(Long userId) {
        UserAuthReloadRspDto auth = authPermissionService.reloadUserAuth(userId);
        List<String> roleKeys = auth == null ? List.of() : auth.getRoleKeys();
        List<String> permissions = auth == null ? List.of() : auth.getPermissions();

        // 登录 / 刷新会话场景需要允许创建 Account-Session。
        SaSession session = StpUtil.getSessionByLoginId(userId);

        // 注意：这里分两次调用，不要链式调用（便于测试与排查）
        session.set(SaSession.ROLE_LIST, roleKeys == null ? List.of() : roleKeys);
        session.set(SaSession.PERMISSION_LIST, permissions == null ? List.of() : permissions);

        // Account-Session 默认 timeout = -1（永不过期），必须显式设置
        session.updateTimeout(sessionTimeoutSeconds);

        log.info("## 权限写入会话成功, userId: {}, roleKeys: {}, permissions: {}",
                userId, roleKeys, permissions);
    }

    /**
     * {@inheritDoc}
     *
     * <p>用 {@code StpUtil.getSessionByLoginId(userId, false)} 取会话（不创建）：
     * 注销一个从未登录过的用户时，
     * 不应该凭空往 Redis 写一个空会话。
     */
    @Override
    public void clearSessionAuth(Long userId) {
        SaSession session = StpUtil.getSessionByLoginId(userId, false);
        if (session == null) {
            return;
        }
        session.delete(SaSession.ROLE_LIST);
        session.delete(SaSession.PERMISSION_LIST);
        log.info("## 会话权限已清理, userId: {}", userId);
    }
}
