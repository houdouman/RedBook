package com.dobby.xiaohashu.auth.constant;

/**
 * @author 橡皮鸭
 * @version 1.0
 * @date 2026/3/30 20:03
 * 用于统一管理 Redis Key
 */
public class RedisKeyConstants {

    /**
     * 验证码 KEY 前缀
     */
    private static final String VERIFICATION_CODE_KEY_PREFIX = "verification_code:";


    /**
     * 构建验证码 KEY
     * @param phone
     * @return
     */
    public static String buildVerificationCodeKey(String phone) {
        return VERIFICATION_CODE_KEY_PREFIX + phone;
    }

}
