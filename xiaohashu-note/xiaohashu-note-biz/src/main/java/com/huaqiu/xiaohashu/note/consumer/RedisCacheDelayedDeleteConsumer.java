package com.huaqiu.xiaohashu.note.consumer;

import com.huaqiu.xiaohashu.note.contants.MQConstants;
import com.huaqiu.xiaohashu.note.helper.NoteCacheHelper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBlockingQueue;
import org.springframework.stereotype.Component;

/**
 * Redis 缓存延时删除消费者（替代 RocketMQ 的延时消费者 DelayDeleteNoteRedisCacheConsumer）
 *
 * 原理：生产者通过 RDelayedQueue.offer(noteId, 1, SECONDS) 投递 1 秒后的延时消息，
 * 到期后消息进入 RBlockingQueue，本消费者在独立线程中 take() 并删除 Redis 缓存，
 * 从而实现“延迟双删”策略。
 */
@Component
@Slf4j
public class RedisCacheDelayedDeleteConsumer {

    @Resource
    private RBlockingQueue<String> redisCacheDeleteQueue;

    @Resource
    private NoteCacheHelper noteCacheHelper;

    /**
     * 启动一个常驻线程，阻塞消费延时队列
     */
    @PostConstruct
    public void init() {
        Thread thread = new Thread(() -> {
            while (true) {
                try {
                    // take() 会阻塞，直到有延时到期的消息进入队列
                    String noteId = redisCacheDeleteQueue.take();
                    log.info("## Redisson 延时队列：删除 Redis 笔记缓存, noteId: {}", noteId);
                    noteCacheHelper.delete(Long.valueOf(noteId));
                } catch (Exception e) {
                    log.error("## Redisson 延时队列消费异常: ", e);
                }
            }
        }, "redis-cache-delayed-delete");
        // 设置为守护线程，随应用退出而结束
        thread.setDaemon(true);
        thread.start();
    }

}
