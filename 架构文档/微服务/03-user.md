# 用户服务 xiaohashu-user

## 基本信息

| 项 | 值 |
| --- | --- |
| 端口 | 8082 |
| 服务名 | xiaohashu-user |
| 依赖 | MySQL `xiaohashu`、Redis、Nacos、OSS服务 |

## 职责

1. 查询用户信息（按 userId 或当前登录用户）。
2. 修改个人资料：昵称、性别、生日、简介、头像、背景图。
3. 为其它服务（如 note）提供用户信息查询能力。

## 接口清单

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/user/me` | 查询当前登录用户信息 |
| POST | `/user/findById` | 按 userId 查询用户信息（供 Feign 调用） |
| POST | `/user/update` | 修改资料（multipart，含文件） |
| GET | `/user/test` | 连通性测试 |

## 核心实现

### 查询用户（`findUserById`）

1. 先查 Redis 缓存 `user:info{userId}`；命中 `NULL` 表示不存在（防穿透）。
2. 未命中则查 MySQL `t_user`。
3. 组装 `FindUserInfoRspDto` 返回。
4. 异步写回缓存（TTL 1~2 天随机，防雪崩）。

### 修改资料（`updateUserInfo`）

1. 从 `LoginUserContextHolder` 取当前用户 ID。
2. 逐项校验并组装待更新字段：
   - 昵称、小红书号：正则校验
   - 性别：枚举校验（0 女 / 1 男）
   - 简介：长度 ≤ 100
   - 头像 / 背景图：调 OSS 服务上传后存 URL
3. 更新 `t_user`，并**删除** Redis 用户缓存。

## 数据表

| 表 | 用途 |
| --- | --- |
| `t_user` | 用户资料（与 auth 共用同一张表） |

## Redis 键

| Key | 内容 |
| --- | --- |
| `user:info{userId}` | 用户信息 JSON，`NULL` 为空值占位 |

> 注意：`UserCacheHelper` 的 key 是 `user:info` 直接拼 userId（无分隔冒号）。

## 关键类

| 类 | 作用 |
| --- | --- |
| `UserServiceImpl` | 资料查询与修改 |
| `UserCacheHelper` | 用户缓存读写/删除 |
| `OssFileFeignClient` | 调用 OSS 上传头像/背景图 |
| `SexEnum` | 性别枚举 |
