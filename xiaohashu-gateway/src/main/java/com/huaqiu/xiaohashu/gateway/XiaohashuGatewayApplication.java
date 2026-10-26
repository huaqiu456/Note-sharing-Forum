package com.huaqiu.xiaohashu.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 网关服务启动类
 *
 * <p>职责：
 * <ul>
 *   <li>路由转发（{@code lb://服务名}，基于 Nacos 服务发现）</li>
 *   <li>Sa-Token 统一鉴权（见 {@code SaTokenConfigure}）</li>
 *   <li>向下游透传 userId 请求头（见 {@code AddUserId2HeaderFilter}）</li>
 * </ul>
 *
 * <p><b>历史说明</b>：这里曾使用 {@code @EnableFeignClients} 让网关通过 Feign 回源 auth
 * 查询权限（v1「权限数据降级回源方案」）。「权限进会话」改造后，网关只读 Sa-Token 会话
 * （见 {@code StpInterfaceImpl}），不再有任何跨服务调用，因此该注解与 Feign 依赖均已移除。
 */
@SpringBootApplication
public class XiaohashuGatewayApplication {

    /**
     * 网关服务入口
     *
     * @param args 启动参数（Spring Boot 标准参数）
     */
    public static void main(String[] args){
        SpringApplication.run(XiaohashuGatewayApplication.class,args);
    }
}
