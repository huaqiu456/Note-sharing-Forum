package com.huaqiu.xiaohashu.gateway.auth;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpInterface;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 网关：Sa-Token 角色/权限数据源
 *
 * <p>对应文档：版本/v2/2026-10-13-角色权限进会话方案/04-详细设计.md 第三节
 *
 * <p><b>数据来源：Sa-Token 会话</b>
 *
 * <p>auth 服务在「登录成功」时把角色与权限算好写进会话
 * （{@code SaSession.ROLE_LIST} / {@code SaSession.PERMISSION_LIST}），
 * 网关鉴权时直接读会话 —— <b>不调用任何远程服务、不访问数据库</b>。
 *
 * <p><b>为什么不回源</b>：会话就是登录态本身，会话丢了用户本来也要重新登录，
 * 重新登录时权限会被重新写入。因此「权限数据与登录态一起丢、一起重建」，
 * 不存在「登录态在、权限没了」的中间状态，也就不需要回源兜底。
 *
 * <p><b>失败策略：fail-closed</b>
 * <ul>
 *   <li>会话不存在 → 返回空列表（403）</li>
 *   <li>属性缺失 / 为空 / 类型异常 → 返回空列表（403）</li>
 *   <li>读取抛异常（如 Redis 不可用）→ 捕获并返回空列表（403）</li>
 * </ul>
 * 任何「不确定」的情况都拒绝，绝不默认放行。
 */
@Component
@Slf4j
public class StpInterfaceImpl implements StpInterface {

    private final SaSessionAccessor saSessionAccessor;

    public StpInterfaceImpl(SaSessionAccessor saSessionAccessor) {
        this.saSessionAccessor = saSessionAccessor;
    }

    /**
     * 获取用户角色
     *
     * @param loginId   Sa-Token 的登录 ID（本项目即 userId）
     * @param loginType 登录类型
     * @return 角色标识列表，如 ["common"]；读不到时返回空列表
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        return readSessionList(loginId, SaSession.ROLE_LIST);
    }

    /**
     * 获取用户权限
     *
     * @param loginId   Sa-Token 的登录 ID（本项目即 userId）
     * @param loginType 登录类型
     * @return 权限标识列表，如 ["app:note:view"]；读不到时返回空列表
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return readSessionList(loginId, SaSession.PERMISSION_LIST);
    }

    /**
     * 从会话读取字符串列表属性（fail-closed）
     *
     * @param loginId Sa-Token 的登录 ID
     * @param attrKey 会话属性名（{@code SaSession.ROLE_LIST} 或 {@code SaSession.PERMISSION_LIST}）
     * @return 属性值；任何异常情况一律返回空列表
     */
    private List<String> readSessionList(Object loginId, String attrKey) {
        try {
            // isCreate = false：读不到时不要凭空创建空会话
            SaSession session = saSessionAccessor.getSessionIfPresent(loginId);
            if (session == null) {
                log.warn("## 会话不存在，按无权限处理, loginId: {}", loginId);
                return List.of();
            }

            Object value = session.get(attrKey);

            // 类型防御：Redis 反序列化出来的可能是 ArrayList / JSONArray 等实现
            if (!(value instanceof List<?> list) || list.isEmpty()) {
                log.warn("## 会话中缺少 {}，按无权限处理, loginId: {}", attrKey, loginId);
                return List.of();
            }

            return list.stream().map(String::valueOf).toList();
        } catch (Exception e) {
            log.error("## 读取会话权限失败，按无权限处理, loginId: {}", loginId, e);
            return List.of();
        }
    }
}
