package com.huaqiu.xiaohashu.note.contants;

/**
 * 缓存失效相关常量（已由 RocketMQ Topic 改为 Redisson 的 Redis key / 频道名）
 */
public interface MQConstants {

    /**
     * 广播：删除本地缓存（Redisson RTopic 的频道名）
     */
    String TOPIC_DELETE_NOTE_LOCAL_CACHE = "note:cache:local:delete";

    /**
     * 延时：删除 Redis 缓存（Redisson 阻塞队列 key，延时消息到期后进入此队列）
     */
    String QUEUE_DELETE_NOTE_REDIS_CACHE = "note:cache:redis:delete:queue";

}
