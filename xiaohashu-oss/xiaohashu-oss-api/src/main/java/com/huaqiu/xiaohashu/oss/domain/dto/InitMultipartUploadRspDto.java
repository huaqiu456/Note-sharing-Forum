package com.huaqiu.xiaohashu.oss.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 初始化 Multipart Upload 响应。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InitMultipartUploadRspDto {
    /**
     * Multipart Upload ID。
     */
    private String uploadId;

    /**
     * 服务端生成的 VaultS3 对象名。
     */
    private String objectName;

    /**
     * 实际使用的分片大小，单位字节。
     */
    private Long partSize;

    /**
     * 分片总数。
     */
    private Integer totalParts;

    /**
     * 已经上传成功的分片编号列表。
     */
    private List<Integer> uploadedParts;

    /**
     * 上传会话过期时间。
     */
    private String expiresAt;
}
