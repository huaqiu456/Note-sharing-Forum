package com.huaqiu.xiaohashu.oss.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 初始化 Multipart Upload 请求参数。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InitMultipartUploadReqDto {
    /**
     * 原始文件名。
     */
    private String fileName;

    /**
     * 文件总大小，单位字节。
     */
    private Long fileSize;

    /**
     * 文件 MIME 类型。
     */
    private String contentType;

    /**
     * 文件指纹，用于识别同一用户重新选择同一文件并恢复上传。
     */
    private String fileFingerprint;

    /**
     * 分片大小，单位字节。
     */
    private Long partSize;

    /**
     * 分片总数。
     */
    private Integer totalParts;
}
