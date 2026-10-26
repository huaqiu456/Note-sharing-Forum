# 笔记服务 xiaohashu-note

## 基本信息

| 项 | 值 |
| --- | --- |
| 端口 | 8086 |
| 服务名 | xiaohashu-note |
| 依赖 | MySQL `xiaohashu`、Redis（含 Redisson）、Nacos、Meilisearch、用户服务、ID服务 |

## 职责

1. 笔记发布、更新、详情查询。
2. 首页公开列表、我的笔记列表。
3. 笔记全文搜索（对接 Meilisearch）。
4. 多级缓存与缓存一致性维护。

## 接口清单

| 方法 | 路径 | 说明 | 权限 |
| --- | --- | --- | --- |
| POST | `/note/publish` | 发布笔记 | 需 `app:note:publish` |
| POST | `/note/detail` | 笔记详情 | 登录即可 |
| POST | `/note/update` | 更新笔记 | 登录即可 |
| GET | `/note/list` | 首页列表（分页） | 登录即可 |
| GET | `/note/mine` | 我的笔记（分页） | 登录即可 |
| POST | `/note/search` | 关键词搜索 | 登录即可 |

## 数据模型

### `t_note`（核心表）

| 字段 | 说明 |
| --- | --- |
| `id` | 笔记ID（Leaf 号段生成） |
| `creator_id` | 用户ID |
| `title` | 标题 |
| `content` | 正文（TEXT，已从 Cassandra 迁回） |
| `is_content_empty` | 正文是否为空 |
| `type` | 0 图文 / 1 视频 |
| `img_uris` | 图片URL，逗号分隔 |
| `video_uri` | 视频URL |
| `topic_id` / `topic_name` | 话题 |
| `visible` | 0 公开 / 1 私密 |
| `status` | 0 待审核 / 1 正常 / 2 删除 / 3 下架 |
| `is_top` | 是否置顶 |
| `create_time` / `update_time` | 时间 |

关联表：`t_topic`、`t_channel`。

## 缓存体系

| 层级 | 实现 | Key / 说明 |
| --- | --- | --- |
| 布隆过滤器 | Redisson RBloomFilter | `note:bloom`，拦截不存在的笔记ID |
| 本地缓存 | Caffeine | 进程内，30 秒过期 |
| 分布式缓存 | Redis | `note:{noteId}`，TTL 1~2 天随机 |
| 数据库 | MySQL `t_note` | 最终数据源 |

详情查询顺序：布隆过滤器 → Caffeine → Redis → MySQL。

## 缓存一致性

更新笔记时：

1. 删除 Redis 缓存。
2. **Redisson RTopic 广播**通知所有实例删除本地 Caffeine 缓存（替代 RocketMQ 广播）。
3. **Redisson RDelayedQueue 延时 1 秒**再次删除 Redis 缓存（延迟双删）。

> 早期使用 RocketMQ，现已完全替换为 Redisson，不再依赖 MQ 中间件。

## 搜索实现

- `NoteSearchService`（接口）+ `NoteSearchServiceImpl`（实现）。
- 发布/更新成功后同步索引到 Meilisearch 索引 `note`，主键 `noteId`。
- 可搜索字段：`title`、`content`、`topicName`；可过滤：`type`、`visible`、`status`；可排序：`updateTime`。
- 搜索时过滤 `visible=0 AND status=1`，取回 noteId 列表后回 MySQL 组装。

## 关键类

| 类 | 作用 |
| --- | --- |
| `NoteServiceImpl` | 发布/详情/更新/列表/搜索 |
| `NoteSearchServiceImpl` | Meilisearch 索引同步与搜索 |
| `NoteBloomFilterHelper` | 布隆过滤器 |
| `NoteCacheHelper` | Redis 缓存读写 |
| `CacheDelayQueueConfig` | Redisson 延时队列 Bean |
| `LocalCacheDeleteListener` | RTopic 广播监听，清本地缓存 |
| `RedisCacheDelayedDeleteConsumer` | 延时队列消费，二次删缓存 |
| `UserRpcService` / `DistributedIdGeneratorRpcService` | Feign 调用 |
