package com.huaqiu.xiaohashu.oss.util;

/**
 * 预签名 URL 前缀处理。
 *
 * <p>签名时 Path 不包含 /vaults3；返回浏览器前插入 /vaults3；
 * Nginx 转发 VaultS3 时再去掉该前缀。
 */
public final class PresignedUrlUtils {

    private PresignedUrlUtils() {
    }

    /**
     * 在 URL Path 前插入前缀，保留 Query 原样不变。
     */
    public static String insertPathPrefix(String url, String prefix) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("预签名 URL 不能为空");
        }
        if (prefix == null || prefix.isBlank() || "/".equals(prefix)) {
            return url;
        }
        String normalizedPrefix = prefix.startsWith("/") ? prefix : "/" + prefix;

        int schemeIndex = url.indexOf("://");
        if (schemeIndex < 0) {
            throw new IllegalArgumentException("预签名 URL 格式错误");
        }
        int pathStart = url.indexOf('/', schemeIndex + 3);
        if (pathStart < 0) {
            return url + normalizedPrefix;
        }

        String pathAndQuery = url.substring(pathStart);
        if (pathAndQuery.equals(normalizedPrefix)
                || pathAndQuery.startsWith(normalizedPrefix + "/")) {
            return url;
        }

        return url.substring(0, pathStart) + normalizedPrefix + pathAndQuery;
    }
}
