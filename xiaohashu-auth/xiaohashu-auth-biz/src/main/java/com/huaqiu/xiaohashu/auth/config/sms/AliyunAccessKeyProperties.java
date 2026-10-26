package com.huaqiu.xiaohashu.auth.config.sms;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Data
@ConfigurationProperties(prefix = "aliyun")
public class AliyunAccessKeyProperties {
    private String accessKeyId;

    private String accessKeySecret;


}
