package com.huaqiu.xiaohashu.oss.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

/**
 * VaultS3 的 AWS SDK v2 客户端配置。
 *
 * <p>说明：
 * <ul>
 *   <li>{@link S3Client} 使用内网 endpoint，用于创建/查询/完成 Multipart Upload。</li>
 *   <li>{@link S3Presigner} 使用浏览器可访问的 publicEndpoint，只用于生成预签名 URL。</li>
 *   <li>两者都强制 path-style，兼容自建 VaultS3/MinIO。</li>
 * </ul>
 */
@Configuration
@Slf4j
public class VaultS3Config {

    private final MinioProperties properties;

    public VaultS3Config(MinioProperties properties) {
        this.properties = properties;
    }

    /**
     * 服务端访问 VaultS3 的客户端。
     */
    @Bean
    public S3Client vaultS3Client() {
        log.info("初始化 VaultS3 内部客户端, endpoint={}", properties.getEndpoint());
        return S3Client.builder()
                .endpointOverride(URI.create(properties.getEndpoint()))
                .credentialsProvider(credentialsProvider())
                .region(Region.US_EAST_1)
                .forcePathStyle(true)
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .build();
    }

    /**
     * 生成浏览器直传预签名 URL 的客户端。
     *
     * <p>这里使用 publicEndpoint，URL 的 Host 必须是 Nginx 的公网入口。
     */
    @Bean
    public S3Presigner vaultS3Presigner() {
        String publicEndpoint = properties.getPublicEndpoint();
        if (publicEndpoint == null || publicEndpoint.isBlank()) {
            throw new IllegalStateException("MINIO_PUBLIC_ENDPOINT 未配置");
        }
        if (publicEndpoint.contains("host.docker.internal")) {
            log.warn("MINIO_PUBLIC_ENDPOINT 配置为 host.docker.internal，浏览器无法访问该地址。"
                    + "请改为浏览器可访问的公网地址");
        }
        log.info("初始化 VaultS3 预签名客户端, publicEndpoint={}", publicEndpoint);
        return S3Presigner.builder()
                .endpointOverride(URI.create(publicEndpoint))
                .credentialsProvider(credentialsProvider())
                .region(Region.US_EAST_1)
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();
    }

    private StaticCredentialsProvider credentialsProvider() {
        return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(properties.getAccessKey(), properties.getSecretKey())
        );
    }
}
