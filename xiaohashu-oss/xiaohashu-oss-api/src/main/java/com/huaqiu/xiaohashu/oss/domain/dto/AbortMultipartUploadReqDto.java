package com.huaqiu.xiaohashu.oss.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 取消 Multipart Upload 请求参数。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AbortMultipartUploadReqDto {
    /**
     * 待取消的 Multipart Upload ID。
     */
    private String uploadId;
}
