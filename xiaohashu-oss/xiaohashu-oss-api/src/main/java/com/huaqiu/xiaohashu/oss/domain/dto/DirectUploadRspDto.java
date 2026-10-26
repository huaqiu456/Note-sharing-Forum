package com.huaqiu.xiaohashu.oss.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 小文件直传预签名响应。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DirectUploadRspDto {
    /**
     * 服务端生成的 VaultS3 对象名。
     */
    private String objectName;

    /**
     * 带 /vaults3 前缀的 PUT 预签名 URL，浏览器直接上传使用。
     */
    private String uploadUrl;

    /**
     * 上传完成后前端展示用的文件地址。
     */
    private String fileUrl;

    /**
     * 预签名 URL 过期时间。
     */
    private String expiresAt;
}
