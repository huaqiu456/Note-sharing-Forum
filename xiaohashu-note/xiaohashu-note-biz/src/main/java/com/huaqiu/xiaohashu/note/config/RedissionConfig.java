package com.huaqiu.xiaohashu.note.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson 配置
 *
 * 地址从 spring.data.redis.* 读取：
 * - 本地 dev 使用 application-dev.yml 的 127.0.0.1:6379；
 * - 生产 prod 通过 SPRING_DATA_REDIS_HOST 等环境变量覆盖。
 */
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

    @Bean(destroyMethod = "shutdown") // 关闭时释放资源
    public RedissonClient redissonClient() {
        Config config = new Config();

        config.useSingleServer()
                .setAddress("redis://" + host + ":" + port)
                .setPassword(password)
                .setDatabase(database);

        return Redisson.create(config);
    }
}
