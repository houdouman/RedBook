package com.dobby.xiaohashu.user.relation.biz.constant;

/**
 * @author 橡皮鸭
 * @version 1.0
 * @date 2026/4/15 11:06
 */
public class RedisKeyConstants {

    /**
     * 关注列表key前缀
     */
    private static final String USER_FOLLOWING_KEY_PREFIX = "following:";

    /**
     * 粉丝列表 KEY 前缀
     */
    private static final String USER_FANS_KEY_PREFIX = "fans:";

    /**
     * 构建关注列表完整的key
     * @param userId
     * @return
     */
    public static String buildUserFollowingKey(Long userId){
        return USER_FOLLOWING_KEY_PREFIX + userId;
    }

    /**
     * 构建粉丝列表完整的 KEY
     * @param userId
     * @return
     */
    public static String buildUserFansKey(Long userId) {
        return USER_FANS_KEY_PREFIX + userId;
    }
}
