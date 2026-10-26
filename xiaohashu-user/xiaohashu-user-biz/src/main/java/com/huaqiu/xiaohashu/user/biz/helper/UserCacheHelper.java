package com.huaqiu.xiaohashu.user.biz.helper;

import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
@Component
public class UserCacheHelper {
    private static final String USER_CACHE_KEY = "user:info";
    private static final String NULL_VALUE = "NULL";

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public String buildKey(Long userId) {
        return USER_CACHE_KEY + userId;
    }

    public void set(Long userId, String value, long ttlSeconds) {
        stringRedisTemplate.opsForValue().set(
                buildKey(userId),
                value,
                ttlSeconds,
                TimeUnit.SECONDS
        );
    }

    public String get(Long userId) {
        return stringRedisTemplate.opsForValue().get(buildKey(userId));
    }

    public void delete(Long userId) {
        stringRedisTemplate.delete(buildKey(userId));
    }
}
