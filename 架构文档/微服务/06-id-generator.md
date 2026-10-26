# 分布式ID服务 xiaohashu-distributed-id-generator

## 基本信息

| 项 | 值 |
| --- | --- |
| 端口 | 8085 |
| 服务名 | xiaohashu-distributed-id-generator |
| 依赖 | MySQL `leaf`（`leaf_alloc` 表）、Nacos |
| 来源 | 整合美团 Leaf 的号段模式 |

## 职责

对外提供号段模式的分布式ID生成能力，被 auth（用户ID/小红书号）与 note（笔记ID）通过 Feign 调用。

## 接口清单

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/id/segment/get/{key}` | 按业务标识取号段ID |
| GET | `cache` / `db` | Leaf 自带监控页（非业务接口） |

## 号段模式原理

1. `leaf_alloc` 表按 `biz_tag` 存 `max_id` 与 `step`。
2. 服务取号时执行 `UPDATE max_id = max_id + step`，把一段ID加载到内存缓存。
3. 后续请求直接在内存中自增返回，号段用尽再取下一段。

优点：DB 压力小，吞吐高（压测可达 20000/s 量级）。

## 业务标识（biz_tag）

| biz_tag | 用途 |
| --- | --- |
| `leaf-segment-user-id` | 用户ID |
| `leaf-segment-xiaohashu-id` | 小红书号 |
| `leaf-segment-test` | 笔记ID |

## 数据表

```sql
CREATE TABLE leaf_alloc (
  biz_tag     varchar(128) NOT NULL DEFAULT '',
  max_id      bigint(20)   NOT NULL DEFAULT '1',
  step        int(11)      NOT NULL,
  description varchar(256) DEFAULT NULL,
  update_time timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (biz_tag)
) ENGINE=InnoDB;
```

## 配置说明

- JDBC 连接来自 `leaf.properties`，同时支持通过 `LEAF_JDBC_URL` / `LEAF_JDBC_USERNAME` / `LEAF_JDBC_PASSWORD` 环境变量覆盖（逐项回退）。
- 号段线程池已由无界改为有界 `(2, 4)`，避免线程膨胀。

## 注意事项

- 新增业务标识必须先在 `leaf_alloc` 插入对应记录，否则取号会失败并抛异常。
- ID 服务不可用时，注册与发布笔记都会失败（强依赖）。
