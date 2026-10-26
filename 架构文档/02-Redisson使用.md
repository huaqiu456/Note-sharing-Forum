# Redisson 使用说明

## 一、为什么引入 Redisson

项目里同时存在两个 Redis 客户端，分工明确：

| 客户端 | 使用场景 |
| --- | --- |
| Spring Data Redis（`StringRedisTemplate` / `RedisTemplate`） | 普通 KV：验证码、用户缓存、笔记详情缓存、Sa-Token 会话 |
| **Redisson** | 需要分布式数据结构或协调能力：布隆过滤器、广播、延时队列 |

版本由根 `pom.xml` 统一管理：`org.redisson:redisson:3.17.7`，**仅 `xiaohashu-note-biz` 引入了该依赖**。

早期笔记更新后的缓存失效依赖 RocketMQ（广播删本地缓存 + 延时双删），现全部改为 Redisson 实现，项目不再依赖 MQ 中间件。

## 二、配置

`note/config/RedissionConfig.java`：

```java
@Configuration
public class RedissionConfig {

    @Value("${spring.data.redis.host:127.0.0.1}")
    private String host;

    @Value("${spring.data.redis.port:6379}")
    private int port;

    @Value("${spring.data.redis.password:}")
    private String password;

    @Value("${spring.data.redis.database:0}")
    private int database;

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://" + host + ":" + port)
                .setPassword(password)
                .setDatabase(database);
        return Redisson.create(config);
    }
}
```

要点：

- 地址从 `spring.data.redis.*` 读取，**与 Spring Data Redis 共用同一份配置**：
  - 本地 dev：`application-dev.yml` 的 `127.0.0.1:6379`
  - 容器 prod：由 `SPRING_DATA_REDIS_HOST` 等环境变量注入
- 当前使用**单机模式**（`useSingleServer`）；若改用集群/哨兵，需同步调整此处。
- `destroyMethod = "shutdown"`：应用关闭时释放连接与后台线程。
- 类名拼写为 `RedissionConfig`（少一个 s），是历史命名，暂未更名。

## 三、使用场景

### 1. RBloomFilter —— 布隆过滤器（防缓存穿透）

`note/helper/NoteBloomFilterHelper.java`

```java
private static final String BLOOM_KEY = "note:bloom";

private RBloomFilter<Long> createBloom(String key) {
    RBloomFilter<Long> filter = redissonClient.getBloomFilter(key);
    filter.tryInit(1_000_000, 0.001);   // 幂等：100万容量、误判率 0.1%
    return filter;
}

public void add(Long noteId)      { bloomFilter.add(noteId); }
public boolean contains(Long id)  { return bloomFilter.contains(id); }
```

- 用途：查询笔记详情前先判断 noteId 是否可能存在，直接拦截无效请求。
- 写入时机：发布笔记成功后 `noteBloomFilterHelper.add(noteId)`。

**预热与重建**（`note/runner/BloomFilterWarmUp.java`）：

```java
@PostConstruct
public void init() { new Thread(this::tryWarmUp).start(); }              // 启动时一次

@Scheduled(cron = "0 0 3 * * ?")
public void scheduledRebuild() { new Thread(this::tryWarmUp).start(); }  // 每天 3 点
```

重建过程：

1. 用 `setIfAbsent` 抢分布式锁 `bloom:note:warmup:lock`（TTL 10 分钟），保证多实例只有一个在重建。
2. `createNew()` 用一个**新 key**（`note:bloom:tmp:{时间戳}`）建过滤器，避免污染线上数据。
3. 分页扫描 `t_note` 全量 ID 填充新过滤器。
4. `replaceWith(newBloom)` 原子替换内存中的引用。
5. 释放锁。

> ⚠️ **已知问题**：note 服务的启动类 `XiaohashuNoteBizApplication` 上没有 `@EnableScheduling`，因此 `@Scheduled` 的**每日 3 点重建不会执行**，目前只有启动时那一次预热生效。要启用定时重建，需在启动类补 `@EnableScheduling`。

### 2. RTopic —— 广播删除本地缓存

替代原 RocketMQ 的广播消息（`DeleteNoteLocalCacheTopic`）。

**生产者**（`NoteServiceImpl.updateNote`）：

```java
RTopic topic = redissonClient.getTopic(MQConstants.TOPIC_DELETE_NOTE_LOCAL_CACHE);
topic.publish(noteId);
```

**订阅者**（`note/consumer/LocalCacheDeleteListener.java`）：

```java
@PostConstruct
public void init() {
    RTopic topic = redissonClient.getTopic(MQConstants.TOPIC_DELETE_NOTE_LOCAL_CACHE);
    topic.addListener(Long.class, (channel, noteId) -> {
        noteService.deleteNoteLocalCache(noteId);   // 清本实例的 Caffeine
    });
}
```

- 频道名：`note:cache:local:delete`
- 原理：底层是 Redis Pub/Sub，**所有订阅该频道的实例都会收到消息**，从而实现广播。
- 更新笔记时，本实例和其他实例的本地 Caffeine 缓存都会被清掉。

### 3. RDelayedQueue + RBlockingQueue —— 延时双删

替代原 RocketMQ 的延时消息（延迟级别 1s）。

**Bean 定义**（`note/config/CacheDelayQueueConfig.java`）：

```java
@Bean
public RBlockingQueue<String> redisCacheDeleteQueue(RedissonClient c) {
    return c.getBlockingQueue(MQConstants.QUEUE_DELETE_NOTE_REDIS_CACHE);
}

@Bean(destroyMethod = "destroy")
public RDelayedQueue<String> redisCacheDelayedQueue(RedissonClient c,
                                                    RBlockingQueue<String> q) {
    return c.getDelayedQueue(q);
}
```

**生产者**（`NoteServiceImpl.updateNote`）：

```java
redisCacheDelayedQueue.offer(String.valueOf(noteId), 1, TimeUnit.SECONDS);
```

**消费者**（`note/consumer/RedisCacheDelayedDeleteConsumer.java`）：

```java
@PostConstruct
public void init() {
    Thread t = new Thread(() -> {
        while (true) {
            try {
                String noteId = redisCacheDeleteQueue.take();   // 阻塞取
                noteCacheHelper.delete(Long.valueOf(noteId));
            } catch (Exception e) { log.error("...", e); }
        }
    }, "redis-cache-delayed-delete");
    t.setDaemon(true);
    t.start();
}
```

- 队列名：`note:cache:redis:delete:queue`
- 原理：未到期的消息先放在 Redis ZSET，由 `RDelayedQueue` 的后台任务到期后搬运到 `RBlockingQueue`，消费者 `take()` 取出执行。
- 作用：更新笔记后“立即删 + 延时 1 秒再删”，覆盖缓存与数据库短暂不一致的窗口。

### 4. 分布式锁（借 `StringRedisTemplate` 实现）

布隆过滤器重建时用普通 Redis 命令加锁：

```java
Boolean success = redisTemplate.opsForValue()
        .setIfAbsent(LOCK_KEY, lockValue, 10, TimeUnit.MINUTES);
```

- Key：`bloom:note:warmup:lock`
- 释放前校验 value 是否为自己写入的，避免误删他人的锁。

> 注意：这里用的是简化版锁（非 Redisson `RLock`），未处理锁续期。如需更强保证可改用 `redissonClient.getLock()`。

## 四、Key 一览

| Key / 频道 | 类型 | 用途 | 定义位置 |
| --- | --- | --- | --- |
| `note:bloom` | RBloomFilter | 笔记 ID 布隆过滤器 | `NoteBloomFilterHelper` |
| `note:bloom:tmp:{ts}` | RBloomFilter | 重建期间的临时过滤器 | `NoteBloomFilterHelper.createNew()` |
| `note:cache:local:delete` | RTopic 频道 | 广播删除本地缓存 | `MQConstants` |
| `note:cache:redis:delete:queue` | RBlockingQueue / RDelayedQueue | 延时双删队列 | `MQConstants` |
| `bloom:note:warmup:lock` | String | 重建分布式锁 | `BloomFilterWarmUp` |

> 笔记详情缓存 `note:{noteId}` 与用户缓存等由 Spring Data Redis 读写，不走 Redisson。

## 五、调用位置汇总

| 位置 | 使用的 Redisson 能力 |
| --- | --- |
| `NoteServiceImpl.publishNote` | `RBloomFilter.add` |
| `NoteServiceImpl.getNoteDetail` | `RBloomFilter.contains` |
| `NoteServiceImpl.updateNote` | `RTopic.publish` + `RDelayedQueue.offer` |
| `LocalCacheDeleteListener` | `RTopic.addListener` |
| `RedisCacheDelayedDeleteConsumer` | `RBlockingQueue.take` |
| `BloomFilterWarmUp` | 通过 `NoteBloomFilterHelper` 重建过滤器 |

## 六、与 RocketMQ 的对照

| 原 RocketMQ 用法 | Redisson 替代 |
| --- | --- |
| 广播消息删除本地缓存（`MessageModel.BROADCASTING`） | `RTopic` 发布订阅（天然广播） |
| 延时消息（延迟级别 1s） | `RDelayedQueue.offer(value, 1, SECONDS)` |
| 消费者 `@RocketMQMessageListener` | `RTopic.addListener` / 常驻线程 `take()` |

替换后：

- 少一套 MQ 中间件，部署更轻；
- 代价是 Redis Pub/Sub **不持久化**，Redis 重启期间的消息会丢失——对“删缓存”场景可接受。

## 七、注意事项与常见问题

| 问题 | 说明 |
| --- | --- |
| `RDelayedQueue` 无常驻引用 | 必须作为 Spring Bean 常驻，否则后台搬运任务停止，消息永远留在 ZSET |
| 定时重建不生效 | 启动类缺 `@EnableScheduling`，`@Scheduled` 不执行（见上文已知问题） |
| Redis 重启丢消息 | 广播与延时消息不持久化；删缓存场景可接受，必要时补一次全量重建 |
| 临时布隆 key 残留 | 每次重建生成 `note:bloom:tmp:{ts}`，旧 key 不会被自动清理，可定期清理 |
| 连接失败 | 检查 `spring.data.redis.host/port/password` 是否与 Spring Data Redis 一致 |
| 类名拼写 | `RedissionConfig` / `RedissionConfig.java` 为历史拼写，引用时注意 |

## 八、验证命令

```bash
# 布隆过滤器与锁
redis-cli EXISTS note:bloom
redis-cli KEYS "note:bloom:tmp:*"
redis-cli TTL bloom:note:warmup:lock

# 广播频道是否有订阅者
redis-cli PUBSUB CHANNELS "note:cache:local:delete"

# 延时队列（未到期消息在 ZSET）
redis-cli ZCARD note:cache:redis:delete:queue
redis-cli LLEN note:cache:redis:delete:queue
```

更新一篇笔记后，观察 note 服务日志应依次出现：

```
====> Redisson 广播：删除笔记本地缓存已发布...
## Redisson 广播：收到删除本地缓存消息, noteId: xxx
## Redisson 延时队列：删除 Redis 笔记缓存, noteId: xxx
```
