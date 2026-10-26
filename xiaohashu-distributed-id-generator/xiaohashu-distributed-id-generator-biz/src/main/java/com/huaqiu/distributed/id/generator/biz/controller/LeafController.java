package com.huaqiu.distributed.id.generator.biz.controller;

import com.huaqiu.distributed.id.generator.biz.core.common.Result;
import com.huaqiu.distributed.id.generator.biz.core.common.Status;
import com.huaqiu.distributed.id.generator.biz.exception.LeafServerException;
import com.huaqiu.distributed.id.generator.biz.exception.NoKeyException;
import com.huaqiu.distributed.id.generator.biz.service.SegmentService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/id")
@Slf4j
public class LeafController {

    @Resource
    private SegmentService segmentService;


    @RequestMapping(value = "/segment/get/{key}")
    public String getSegmentId(@PathVariable("key") String key) {
        return get(key, segmentService.getId(key));
    }


    private String get(@PathVariable("key") String key, Result id) {
        Result result;
        if (key == null || key.isEmpty()) {
            throw new NoKeyException();
        }
        result = id;
        if (result.getStatus().equals(Status.EXCEPTION)) {
            throw new LeafServerException(result.toString());
        }
        return String.valueOf(result.getId());
    }
}

