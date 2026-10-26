package com.huaqiu.xiaohashu.auth.constant;


public class RedisKeyConstants {
    // 省略...

    /**
     * 小哈书全局 ID 生成器 KEY
     */
    public static final String XIAOHASHU_ID_GENERATOR_KEY = "xiaohashu_id_generator";
    // 权限同步标记 Key
    public static final String PUSH_PERMISSION_FLAG = "push_permission_flag";


    private static final String VERIFICATION_CODE_KEY_PREFIX = "verification_code:" ;

    /**
     * 构建手机验证码的 Redis key
     *
     * @param phone 手机号
     * @return 形如 {@code verification_code:13800138000}
     */
    public static String buildVerificationCodeKey(String phone){
        return VERIFICATION_CODE_KEY_PREFIX+phone;
    }
}
