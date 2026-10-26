package com.huaqiu.xiaohashu.oss.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "minio")
@Component
@Data
public class MinioProperties {
    /**
     * 服务端内部访问 VaultS3 的地址，例如 http://host.docker.internal:9000。
     */
    private String endpoint;

    /**
     * 浏览器可访问的 Nginx 公网地址，例如 http://server-ip:8087。
     */
    private String publicEndpoint;

    /**
     * VaultS3 AccessKey。
     */
    private String accessKey;

    /**
     * VaultS3 SecretKey。
     */
    private String secretKey;

    /**
     * VaultS3 桶名。
     */
    private String bucketName;

    /**
     * 小文件直传阈值，默认 10MB。
     */
    private long directUploadLimit = 10L * 1024 * 1024;

    /**
     * 分片大小，默认 10MiB。
     */
    private long partSize = 10L * 1024 * 1024;

    /**
     * 单文件最大大小，默认 100MB。
     */
    private long maxFileSize = 100L * 1024 * 1024;

    /**
     * 上传会话过期小时数，默认 24 小时。
     */
    private long sessionTtlHours = 24;
}

