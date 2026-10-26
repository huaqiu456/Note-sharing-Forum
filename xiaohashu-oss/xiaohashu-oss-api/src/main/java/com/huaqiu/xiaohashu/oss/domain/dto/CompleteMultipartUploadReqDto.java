package com.huaqiu.xiaohashu.oss.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 完成 Multipart Upload 请求参数。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompleteMultipartUploadReqDto {
    /**
     * Multipart Upload ID。
     */
    private String uploadId;

    /**
     * 分片列表。
     *
     * <p>当前后端会以 VaultS3 ListParts 的结果为准，该字段保留用于兼容 S3 协议。
     */
    private List<UploadPartDto> parts;
}
