package com.huaqiu.xiaohashu.auth.service.interfaces;

/**
 * 权限快照写入 Sa-Token 会话的服务（接口）
 *
 * <p>对应文档：版本/v2/2026-10-13-角色权限进会话方案/04-详细设计.md 第二节
 *
 * <p><b>改造背景</b>：原来「用户 → 角色」「角色 → 权限」是 Redis 里的两个业务 key
 * （`user:roles:{userId}` / `role:permissions:{roleKey}`），与登录态彼此独立，
 * 一旦丢失就会出现「登录态还在、权限没了」的中间状态，需要跨服务回源才能恢复。
 *
 * <p>现在改为：**登录时把角色与权限算好写进会话**，与登录态绑定 ——
 * 会话在则权限在，会话没了则用户本来就登录不了，重新登录即自动重建。
 *
 * @see com.huaqiu.xiaohashu.auth.service.impl.SessionAuthServiceImpl
 */
public interface SessionAuthService {

    /**
     * 重新计算用户角色与权限，并刷新到会话中
     *
     * <p>登录成功后调用；也可供「刷新我的会话」接口复用。
     *
     * <p>实现内不做异常兜底：异常向上抛，由调用方决定是否影响登录
     * （当前策略：**不影响登录**，只记录日志）。
     *
     * @param userId 用户 ID
     */
    void refreshSessionAuth(Long userId);

    /**
     * 清理会话中的权限快照（注销时调用）
     *
     * <p>会话不存在时静默返回，不会创建会话。
     *
     * @param userId 用户 ID
     */
    void clearSessionAuth(Long userId);
}
