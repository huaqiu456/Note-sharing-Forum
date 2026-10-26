package com.huaqiu.xiaohashu.note.config;

import com.huaqiu.xiaohashu.note.contants.MQConstants;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson 延时队列配置（替代 RocketMQ 的延时消息）
 *
 * 设计说明：
 * 1. RBlockingQueue 是最终消费的阻塞队列，消费者从这里 take()。
 * 2. RDelayedQueue 是延时队列包装器，生产者 offer(value, delay, unit) 后，
 *    到期消息会被 Redisson 后台任务自动搬运到 RBlockingQueue 中。
 * 3. RDelayedQueue 必须作为常驻 Bean 存在，否则后台搬运任务会随对象销毁而停止。
 *
 * 依赖注入说明：
 * 4. @Bean 方法的参数由 Spring 容器自动注入，不需要手动 new。
 *    默认按参数类型匹配容器中已有的 Bean：
 *    - RedissonClient 由 RedissionConfig 中的 redissonClient Bean 注入；
 *    - RBlockingQueue 由本类上面的 redisCacheDeleteQueue Bean 注入。
 * 5. 若容器中存在多个同类型的 Bean，需要配合 @Qualifier 指定 Bean 名称；
 *    本项目中 RBlockingQueue 只有一个 Bean，因此无需 @Qualifier。
 */
@Configuration
public class CacheDelayQueueConfig {

    /**
     * 目标阻塞队列：延时到期后，消息会进入这里等待消费
     */
    @Bean
    public RBlockingQueue<String> redisCacheDeleteQueue(RedissonClient redissonClient) {
        return redissonClient.getBlockingQueue(MQConstants.QUEUE_DELETE_NOTE_REDIS_CACHE);
    }

    /**
     * 延时队列：保存未到期消息，并负责到期搬运到上面的阻塞队列
     *
     * 参数注入说明（Spring @Bean 自动注入）：
     * - redissonClient：按类型从容器中注入 RedissonClient Bean（来自 RedissionConfig）。
     * - redisCacheDeleteQueue：按类型从容器中注入 RBlockingQueue Bean（来自上面第一个 @Bean）。
     * 方法内部等价于：redissonClient.getDelayedQueue(redisCacheDeleteQueue)。
     *
     * destroyMethod = "destroy"：应用关闭时由 Spring 调用 destroy() 清理延时队列结构与后台线程，
     * 避免 Redis 中残留未消费数据。
     */
    @Bean(destroyMethod = "destroy")
    public RDelayedQueue<String> redisCacheDelayedQueue(RedissonClient redissonClient,
                                                        RBlockingQueue<String> redisCacheDeleteQueue) {
        return redissonClient.getDelayedQueue(redisCacheDeleteQueue);
    }

}
