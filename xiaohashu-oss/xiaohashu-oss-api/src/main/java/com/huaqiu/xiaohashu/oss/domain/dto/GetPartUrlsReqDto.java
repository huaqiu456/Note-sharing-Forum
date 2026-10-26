package com.huaqiu.xiaohashu.oss.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 获取 Multipart 分片预签名 URL 请求参数。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetPartUrlsReqDto {
    /**
     * Multipart Upload ID。
     */
    private String uploadId;

    /**
     * 需要生成预签名 URL 的分片编号列表，从 1 开始。
     */
    private List<Integer> partNumbers;
}
