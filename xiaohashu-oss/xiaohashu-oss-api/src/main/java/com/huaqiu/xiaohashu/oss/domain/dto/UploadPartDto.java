package com.huaqiu.xiaohashu.oss.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Multipart 分片信息。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadPartDto {
    /**
     * 分片编号，从 1 开始。
     */
    private Integer partNumber;

    /**
     * 分片上传成功后 VaultS3 返回的 ETag。
     */
    private String etag;
}
