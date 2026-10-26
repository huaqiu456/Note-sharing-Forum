package com.huaqiu.xiaohashu.note.consumer;

import com.huaqiu.xiaohashu.note.contants.MQConstants;
import com.huaqiu.xiaohashu.note.service.NoteService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

/**
 * 本地缓存删除监听器（替代 RocketMQ 的广播消费者 DeleteNoteLocalCacheConsumer）
 *
 * 原理：Redisson 的 RTopic 底层是 Redis Pub/Sub，所有订阅该频道的实例都会收到消息，
 * 从而实现广播效果：某个实例更新笔记后广播 noteId，所有实例都删除各自的本地 Caffeine 缓存。
 */
@Component
@Slf4j
public class LocalCacheDeleteListener {

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private NoteService noteService;

    /**
     * 应用启动时订阅频道，收到 noteId 后删除本地缓存
     */
    @PostConstruct
    public void init() {
        RTopic topic = redissonClient.getTopic(MQConstants.TOPIC_DELETE_NOTE_LOCAL_CACHE);

        topic.addListener(Long.class, (channel, noteId) -> {
            log.info("## Redisson 广播：收到删除本地缓存消息, noteId: {}", noteId);
            noteService.deleteNoteLocalCache(noteId);
        });
    }

}
