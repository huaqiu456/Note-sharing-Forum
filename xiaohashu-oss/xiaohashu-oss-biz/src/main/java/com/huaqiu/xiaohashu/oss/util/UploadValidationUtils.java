package com.huaqiu.xiaohashu.oss.util;

/**
 * 文件上传大小与分片参数校验。
 *
 * <p>业务规则：
 * <ul>
 *   <li>小文件：0 &lt; size &lt;= 10MB</li>
 *   <li>大文件：10MB &lt; size &lt;= 100MB</li>
 *   <li>分片大小：10MiB</li>
 * </ul>
 */
public final class UploadValidationUtils {

    public static final long MB = 1024L * 1024L;
    public static final long DIRECT_UPLOAD_LIMIT = 10L * MB;
    public static final long MAX_FILE_SIZE = 100L * MB;
    public static final long DEFAULT_PART_SIZE = 10L * MB;
    public static final int MAX_PARTS = 10_000;

    private UploadValidationUtils() {
    }

    /**
     * 校验小文件直传。
     */
    public static void validateDirect(Long fileSize) {
        if (fileSize == null || fileSize <= 0) {
            throw new IllegalArgumentException("文件大小不能为空");
        }
        if (fileSize > DIRECT_UPLOAD_LIMIT) {
            throw new IllegalArgumentException("小文件直传不能超过 10MB");
        }
    }

    /**
     * 校验 Multipart Upload 参数。
     */
    public static void validateMultipart(Long fileSize, Long partSize, Integer totalParts) {
        if (fileSize == null || fileSize <= DIRECT_UPLOAD_LIMIT) {
            throw new IllegalArgumentException("Multipart 文件必须大于 10MB");
        }
        if (fileSize > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("文件不能超过 100MB");
        }
        if (partSize == null || partSize <= 0) {
            throw new IllegalArgumentException("分片大小必须大于 0");
        }
        if (totalParts == null || totalParts <= 0 || totalParts > MAX_PARTS) {
            throw new IllegalArgumentException("分片数量非法");
        }

        long maxCovered = partSize * totalParts;
        long minRequired = partSize * (totalParts - 1L);
        if (fileSize <= minRequired || fileSize > maxCovered) {
            throw new IllegalArgumentException("分片参数无法覆盖文件大小");
        }
    }
}
