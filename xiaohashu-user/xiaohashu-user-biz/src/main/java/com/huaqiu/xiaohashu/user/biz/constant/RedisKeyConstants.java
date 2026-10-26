package com.huaqiu.xiaohashu.user.biz.constant;

/**
 * @version: v1.0.0
 * @description: TODO
 **/
public class RedisKeyConstants {

    // 省略...

    /**
     * 用户信息数据 KEY 前缀
     */
    private static final String USER_INFO_KEY_PREFIX = "user:info:";

    // 省略...

    /**
     * 构建角色对应的权限集合 KEY
     * @param userId
     * @return
     */
    public static String buildUserInfoKey(Long userId) {
        return USER_INFO_KEY_PREFIX + userId;
    }
}

