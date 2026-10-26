package com.huaqiu.xiaohashu.oss.strategy.impl;

import com.huaqiu.xiaohashu.oss.strategy.FileStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
public class AliyunOssFileStrategyImpl implements FileStrategy {
    @Override
    public String uploadFile(MultipartFile file, String bucketName) {
        log.info("使用aliyun上传");
        return "";
    }
}
