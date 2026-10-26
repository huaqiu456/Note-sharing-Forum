package com.huaqiu.xiaohashu.oss.domain.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * OSS 分片上传会话。
 *
 * <p>只持久化会话元数据；分片事实来源仍然以 VaultS3 ListParts 为准。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OssUploadSession {

    /**
     * 主键 ID。
     */
    private Long id;

    /**
     * VaultS3 Multipart Upload ID。
     */
    private String uploadId;

    /**
     * 上传用户 ID。
     */
    private Long userId;

    /**
     * VaultS3 桶名。
     */
    private String bucketName;

    /**
     * VaultS3 对象名。
     */
    private String objectName;

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

    /**
     * 上传会话状态，对应 UploadSessionStatusEnum.code。
     */
    private Integer status;

    /**
     * 上传会话过期时间。
     */
    private LocalDateTime expiresAt;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间。
     */
    private LocalDateTime updatedAt;
}
