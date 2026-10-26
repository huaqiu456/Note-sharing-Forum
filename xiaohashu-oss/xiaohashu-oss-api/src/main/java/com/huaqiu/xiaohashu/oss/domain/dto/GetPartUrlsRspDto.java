package com.huaqiu.xiaohashu.oss.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 获取 Multipart 分片预签名 URL 响应。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetPartUrlsRspDto {
    /**
     * 分片编号与预签名 URL 的对应列表。
     */
    private List<UploadPartUrlDto> urls;
}
