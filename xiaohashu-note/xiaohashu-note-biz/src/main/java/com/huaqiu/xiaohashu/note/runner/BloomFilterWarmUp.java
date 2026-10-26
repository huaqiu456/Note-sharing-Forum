package com.huaqiu.xiaohashu.note.runner;

import com.huaqiu.xiaohashu.note.helper.NoteBloomFilterHelper;
import com.huaqiu.xiaohashu.note.mapper.NoteMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class BloomFilterWarmUp {

    private final NoteMapper noteMapper;
    private final NoteBloomFilterHelper bloomFilter;
    private final StringRedisTemplate redisTemplate;

    private static final String LOCK_KEY = "bloom:note:warmup:lock";

    @PostConstruct
    public void init() {
        // 🚀 启动时执行一次
        new Thread(this::tryWarmUp).start();
    }

    /**
     * ⏰ 定时重建 BloomFilter（每天重复）
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void scheduledRebuild() {
        new Thread(this::tryWarmUp).start();
    }

    public void tryWarmUp() {
        String lockValue = UUID.randomUUID().toString();

        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(LOCK_KEY, lockValue, 10, TimeUnit.MINUTES);

        if (Boolean.FALSE.equals(success)) {
            System.out.println("❌ 未获取到分布式锁，跳过 Bloom 预热");
            return;
        }

        try {
            System.out.println("✅ 获取分布式锁成功，开始重建 Bloom");

            // 🔥 核心：创建一个新的 BloomFilterHelper
            NoteBloomFilterHelper newBloomFilter = bloomFilter.createNew();

            // 预热新 Bloom
            rebuild(newBloomFilter);

            // 🔥 替换旧 Bloom（关键一步）
            bloomFilter.replaceWith(newBloomFilter);

        } finally {
            String currentVal = redisTemplate.opsForValue().get(LOCK_KEY);
            if (lockValue.equals(currentVal)) {
                redisTemplate.delete(LOCK_KEY);
            }
        }
    }

    /**
     * 用于填充新的 BloomFilter
     */
    private void rebuild(NoteBloomFilterHelper targetBloom) {
        int pageSize = 1000;
        long lastId = 0L;

        while (true) {
            List<Long> ids = noteMapper.selectIdsByPage(lastId, pageSize);
            if (ids.isEmpty()) {
                break;
            }

            for (Long id : ids) {
                targetBloom.add(id);
            }

            lastId = ids.get(ids.size() - 1);
        }

        System.out.println("✅ 新 BloomFilter 构建完成");
    }
}