package com.huaqiu.xiaohashu.oss.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 单个分片的预签名上传信息。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadPartUrlDto {
    /**
     * 分片编号，从 1 开始。
     */
    private Integer partNumber;

    /**
     * 带 /vaults3 前缀的分片 PUT 预签名 URL。
     */
    private String url;

    /**
     * 预签名 URL 过期时间。
     */
    private String expiresAt;
}
