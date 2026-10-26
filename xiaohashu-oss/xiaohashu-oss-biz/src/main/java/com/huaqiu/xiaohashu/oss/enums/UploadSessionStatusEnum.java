package com.huaqiu.xiaohashu.oss.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * OSS 上传会话状态。
 */
@Getter
@AllArgsConstructor
public enum UploadSessionStatusEnum {

    /**
     * 已初始化，尚未获取分片 URL。
     */
    INIT(0),

    /**
     * 上传中。
     */
    UPLOADING(1),

    /**
     * 已完成。
     */
    COMPLETED(2),

    /**
     * 已取消。
     */
    ABORTED(3),

    /**
     * 已过期。
     */
    EXPIRED(4);

    /**
     * 数据库状态码。
     */
    private final Integer code;

}
