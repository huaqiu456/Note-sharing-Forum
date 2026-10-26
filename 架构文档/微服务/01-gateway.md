# 网关服务 xiaohashu-gateway

## 基本信息

| 项 | 值 |
| --- | --- |
| 端口 | 8000 |
| 服务名 | xiaohashu-gateway |
| 技术 | Spring Cloud Gateway（WebFlux）+ Sa-Token |
| 依赖 | Nacos（服务发现）、Redis（会话/权限） |

## 职责

1. 统一入口与路由转发（按路径前缀分发到各微服务）。
2. Sa-Token 登录态与权限校验。
3. 解析登录用户 ID，注入 `userId` 请求头透传给下游。
4. 全局异常处理，把鉴权异常转换成统一 JSON 响应。

## 路由配置

| id | 路径断言 | 目标 | 过滤器 |
| --- | --- | --- | --- |
| auth | `/auth/**` | `lb://xiaohashu-auth` | `StripPrefix=1` |
| user | `/user/**` | `lb://xiaohashu-user` | 无 |
| note | `/note/**` | `lb://xiaohashu-note` | 无 |
| oss | `/file/**` | `lb://xiaohashu-oss` | 无 |

> `auth` 服务自身接口无 `/auth` 前缀，所以要剥一层；其它服务 Controller 自带前缀，不能剥。

## 关键组件

| 类 | 作用 |
| --- | --- |
| `SaTokenConfigure` | Sa-Reactor 全局过滤器：登录校验 + `/note/publish` 权限校验 |
| `AddUserId2HeaderFilter` | 从登录态取 userId，写入下游请求头 `userId` |
| `StpInterfaceImpl` | 从 Redis 读取用户角色与角色权限，供 Sa-Token 判定 |
| `GlobalExceptionHandler` | 鉴权异常统一处理：未登录 401、无权限 403 |
| `RedisTemplateConfig` | RedisTemplate 序列化配置（Jackson） |

## 鉴权规则

```
所有请求
  ├─ /auth/login、/auth/verification/code/send  → 放行
  └─ 其它 → 必须登录
        └─ /note/publish → 还需 app:note:publish 权限
```

## Redis 键

| Key | 内容 | 写入方 |
| --- | --- | --- |
| `user:roles:{userId}` | 用户拥有的角色标识列表，如 `["common"]` | auth 注册时写入 |
| `role:permissions:{roleKey}` | 角色拥有的权限列表 | auth 启动时推送 |

## 错误码约定

| 场景 | HTTP 状态 | 响应 |
| --- | --- | --- |
| 未登录 | 401 | `UNAUTHORIZED` |
| 已登录但无权限 | 403 | `FORBIDDEN`（权限不足，无法发布） |
| 其它异常 | 200 | `SYSTEM_ERROR` 业务失败响应 |

## 注意事项

- 权限校验**只发生在网关**，因此各业务服务不应直接对公网暴露。
- 访问 Redis 中不存在的角色/权限键会导致权限判定失败（拒绝），需保证 auth 已推送权限数据。
