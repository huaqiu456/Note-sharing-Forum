package com.huaqiu.xiaohashu.oss.factory;

import com.huaqiu.framework.common.exception.BizException;
import com.huaqiu.xiaohashu.oss.strategy.FileStrategy;
import com.huaqiu.xiaohashu.oss.strategy.impl.AliyunOssFileStrategyImpl;
import com.huaqiu.xiaohashu.oss.strategy.impl.MinioOssFileStrategyImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@RefreshScope
@Configuration
public class FileStrategyFactory {

    @Value("${storage.type}")
    private String type;

    @Bean
    @RefreshScope
    public FileStrategy getFileStrategy(){
        if(type.equals("Minio")){
            return new MinioOssFileStrategyImpl();
        } else if (type.equals("Aliyun")) {
            return new AliyunOssFileStrategyImpl();
        }
        throw new IllegalArgumentException("不可用存储类型");

    }
}
