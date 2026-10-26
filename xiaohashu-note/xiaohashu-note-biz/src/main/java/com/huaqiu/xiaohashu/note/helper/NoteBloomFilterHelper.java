package com.huaqiu.xiaohashu.note.helper;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

@Component
public class NoteBloomFilterHelper {

    private static final String BLOOM_KEY = "note:bloom";

    @Resource
    private RedissonClient redissonClient;

    /**
     * 当前使用的 BloomFilter（核心引用）
     */
    private volatile RBloomFilter<Long> bloomFilter;

    @PostConstruct
    public void init() {
        this.bloomFilter = createBloom(BLOOM_KEY);
    }

    /**
     * 创建一个 BloomFilter（用于初始化 or 重建）
     */
    private RBloomFilter<Long> createBloom(String key) {
        RBloomFilter<Long> filter = redissonClient.getBloomFilter(key);
        filter.tryInit(1_000_000, 0.001); // 幂等初始化
        return filter;
    }

    /**
     * 添加
     */
    public void add(Long noteId) {
        bloomFilter.add(noteId);
    }

    /**
     * 判断是否存在
     */
    public boolean contains(Long noteId) {
        return bloomFilter.contains(noteId);
    }

    /**
     * 🔥 创建一个“全新 Bloom”（给 WarmUp 用）
     */
    public NoteBloomFilterHelper createNew() {
        NoteBloomFilterHelper helper = new NoteBloomFilterHelper();
        helper.redissonClient = this.redissonClient;

        // 用一个新 key（避免污染旧数据）
        String newKey = BLOOM_KEY + ":tmp:" + System.currentTimeMillis();
        helper.bloomFilter = helper.createBloom(newKey);

        return helper;
    }

    /**
     * 🔥 替换当前 Bloom（核心方法）
     */
    public void replaceWith(NoteBloomFilterHelper newBloom) {
        this.bloomFilter = newBloom.bloomFilter;
    }
}