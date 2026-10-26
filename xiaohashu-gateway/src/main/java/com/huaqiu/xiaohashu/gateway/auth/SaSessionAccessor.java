package com.huaqiu.xiaohashu.gateway.auth;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.stereotype.Component;

/**
 * Sa-Token 会话读取封装（网关侧）
 *
 * <p><b>为什么要有这个类</b>：{@code StpUtil} 是静态工具类，直接在 {@link StpInterfaceImpl}
 * 里调用会无法单元测试。这里把「取会话」包成一个小 Bean，测试时用 Mockito 替换即可。
 *
 * <p>对应文档：版本/v2/2026-10-13-角色权限进会话方案/04-详细设计.md 第三节
 */
@Component
public class SaSessionAccessor {

    /**
     * 读取账号会话；会话不存在时返回 {@code null}，**不会创建会话**
     *
     * <p>必须使用 {@code isCreate = false}：默认的 {@code getSessionByLoginId(loginId)}
     * 会在会话不存在时创建并落库，导致鉴权 miss 时往 Redis 写入大量空会话。
     *
     * @param loginId 登录 ID（本项目即 userId）
     * @return 账号会话，可能为 null
     */
    public SaSession getSessionIfPresent(Object loginId) {
        return StpUtil.getSessionByLoginId(loginId, false);
    }
}
