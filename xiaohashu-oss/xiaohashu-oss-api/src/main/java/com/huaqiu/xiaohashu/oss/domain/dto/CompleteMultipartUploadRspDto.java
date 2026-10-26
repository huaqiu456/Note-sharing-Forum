package com.huaqiu.xiaohashu.oss.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 完成 Multipart Upload 响应。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompleteMultipartUploadRspDto {
    /**
     * 文件访问地址，格式通常为 /images/{objectName}。
     */
    private String url;

    /**
     * VaultS3 对象名。
     */
    private String objectName;

    /**
     * 文件总大小，单位字节。
     */
    private Long size;
}
