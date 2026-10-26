package com.huaqiu.xiaohashu.auth.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 用户角色与权限
 *
 * <p><b>迁移说明</b>：本类原先放在独立的 {@code xiaohashu-auth-api} 模块里，
 * 目的是让网关通过 pom 引入 Feign 客户端时能复用 DTO。
 * v2（权限进会话）后网关不再调用 auth，该模块已无外部消费者，
 * 因此把它移回 auth-biz 自身（包名保持不变，所有引用无需修改）。
 *
 * <p>当前唯一用途：登录成功时由 {@code AuthPermissionService} 组装、
 * 交给 {@code SessionAuthService} 写入 Sa-Token 会话。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserAuthReloadRspDto {

    /** 用户拥有的角色标识，如 ["common"] */
    private List<String> roleKeys;

    /** 用户拥有的权限标识（所有角色权限的并集），如 ["app:note:view"] */
    private List<String> permissions;

    /**
     * 空结果（用户无角色 / 角色无权限 / 角色已停用）
     *
     * @return 角色与权限均为空列表的对象
     */
    public static UserAuthReloadRspDto empty() {
        return UserAuthReloadRspDto.builder()
                .roleKeys(List.of())
                .permissions(List.of())
                .build();
    }
}
