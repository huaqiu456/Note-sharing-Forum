package com.huaqiu.distributed.id.generator.api;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@FeignClient(name = "xiaohashu-distributed-id-generator")
public interface DistributedIdGeneratorFeignApi {

    @RequestMapping(value = "/id/segment/get/{key}")
    public String getSegmentId(@PathVariable("key") String key) ;

}
