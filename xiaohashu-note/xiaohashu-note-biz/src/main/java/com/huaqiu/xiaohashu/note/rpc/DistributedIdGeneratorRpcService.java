package com.huaqiu.xiaohashu.note.rpc;


import com.huaqiu.distributed.id.generator.api.DistributedIdGeneratorFeignApi;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * @version: v1.0.0
 * @description: 分布式 ID 服务
 **/
@Component
public class DistributedIdGeneratorRpcService {

    /**
     * 笔记 ID 号段标签。当前数据库已有 leaf-segment-test，直接复用，避免额外 DDL。
     */
    private static final String NOTE_ID_SEGMENT_KEY = "leaf-segment-test";

    @Resource
    private DistributedIdGeneratorFeignApi distributedIdGeneratorFeignApi;

    /**
     * 生成号段模式 ID
     *
     * @return
     */
    public String getSegmentId() {
        return distributedIdGeneratorFeignApi.getSegmentId(NOTE_ID_SEGMENT_KEY);
    }

}

