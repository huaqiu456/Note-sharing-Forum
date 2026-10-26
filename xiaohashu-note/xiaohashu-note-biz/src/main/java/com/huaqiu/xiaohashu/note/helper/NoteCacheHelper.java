package com.huaqiu.xiaohashu.note.helper;

import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class NoteCacheHelper {

    private static final String NOTE_CACHE_KEY = "note:";
    private static final String NULL_VALUE = "NULL";

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public String buildKey(Long noteId) {
        return NOTE_CACHE_KEY + noteId;
    }

    public void set(Long noteId, String value, long ttlSeconds) {
        stringRedisTemplate.opsForValue().set(
                buildKey(noteId),
                value,
                ttlSeconds,
                TimeUnit.SECONDS
        );
    }

    public String get(Long noteId) {
        return stringRedisTemplate.opsForValue().get(buildKey(noteId));
    }

    public void delete(Long noteId){
        stringRedisTemplate.delete(
                buildKey(noteId)
        );
    }
}