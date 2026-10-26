# 认证服务 xiaohashu-auth

## 基本信息

| 项 | 值 |
| --- | --- |
| 端口 | 8080 |
| 服务名 | xiaohashu-auth |
| 依赖 | MySQL `xiaohashu`、Redis、Nacos、分布式ID服务 |

## 职责

1. 手机验证码发送、验证码登录/自动注册、密码登录。
2. 登出、修改密码。
3. RBAC 权限数据初始化并推送到 Redis（供网关鉴权）。
4. 通过 Leaf 号段生成用户ID与小红书号。

## 接口清单

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/verification/code/send` | 发送短信验证码（网关白名单） |
| POST | `/login` | 登录/自动注册，返回 token（网关白名单） |
| POST | `/logout` | 登出 |
| POST | `/password/update` | 修改密码 |
| GET | `/test` | 连通性测试 |

## 核心流程

### 登录/注册（`UserServiceImpl.loginandRegister`）

1. 按 `type` 区分：`1` 验证码登录、`2` 密码登录。
2. 验证码登录：比对 Redis 中 `verification_code:{phone}`。
3. 查 `t_user`：
   - 不存在 → 调用 `registerUser()` 自动注册
   - 存在 → 取出 userId
4. `StpUtil.login(userId)` 生成 token 返回。

### 注册（`registerUser`）

1. 调 ID 服务取 `leaf-segment-xiaohashu-id`、`leaf-segment-user-id`。
2. 插入 `t_user`。
3. 插入 `t_user_role_rel`，绑定 `RoleConstants.COMMON_USER_ROLE_ID = 1L`（普通用户）。
4. 把角色写入 Redis `user:roles:{userId}`。

> 注册在**事务模板**中执行，失败会回滚。

## 权限数据推送

`PushRolePermissions2RedisRunner`（ApplicationRunner）启动时执行：

1. 用 `push.permission.flag` 做 1 天一次的幂等控制。
2. 从 `t_role`（启用）与 `t_role_permission_rel` 组装「角色 → 权限」映射。
3. 写入 Redis `role:permissions:{roleKey}`。

> 改了角色权限数据后，必须 `DEL push.permission.flag` 并重启本服务，否则不会重新推送。

## 数据表

| 表 | 用途 |
| --- | --- |
| `t_user` | 用户账号 |
| `t_role` | 角色（common / admin） |
| `t_permission` | 权限（`type=3` 为 APP 端） |
| `t_role_permission_rel` | 角色-权限绑定（代码实际使用） |
| `t_user_role_rel` | 用户-角色绑定（代码实际使用） |

## Redis 键

| Key | 内容 |
| --- | --- |
| `verification_code:{phone}` | 验证码，3 分钟 |
| `user:roles:{userId}` | 用户角色列表 |
| `role:permissions:{roleKey}` | 角色权限列表 |
| `push.permission.flag` | 权限推送幂等标记，1 天 |

## 关键类

| 类 | 作用 |
| --- | --- |
| `UserServiceImpl` | 登录/注册/登出/改密 |
| `VerificationCodeServiceImpl` | 验证码生成与短信下发（线程池异步） |
| `PushRolePermissions2RedisRunner` | 权限数据预热到 Redis |
| `AliyunSmsHelper` | 短信通道 |
| `RoleConstants` | 角色ID常量 |
