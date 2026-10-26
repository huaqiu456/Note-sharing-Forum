package com.huaqiu.xiaohashu.oss.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Multipart Upload 状态响应。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MultipartUploadStatusRspDto {
    /**
     * Multipart Upload ID。
     */
    private String uploadId;

    /**
     * 上传会话状态名称，例如 INIT、UPLOADING、COMPLETED。
     */
    private String status;

    /**
     * 分片大小，单位字节。
     */
    private Long partSize;

    /**
     * 分片总数。
     */
    private Integer totalParts;

    /**
     * 已上传成功的分片编号列表。
     */
    private List<Integer> uploadedParts;

    /**
     * 已上传字节数。
     */
    private Long uploadedBytes;

    /**
     * 文件总字节数。
     */
    private Long totalBytes;

    /**
     * 上传会话过期时间。
     */
    private String expiresAt;
}
