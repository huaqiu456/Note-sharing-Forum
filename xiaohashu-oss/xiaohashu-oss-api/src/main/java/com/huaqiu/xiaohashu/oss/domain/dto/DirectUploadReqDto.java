package com.huaqiu.xiaohashu.oss.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 小文件直传预签名请求参数。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DirectUploadReqDto {
    /**
     * 原始文件名。
     */
    private String fileName;

    /**
     * 文件大小，单位字节，必须小于等于 10MB。
     */
    private Long fileSize;

    /**
     * 文件 MIME 类型。
     */
    private String contentType;
}
