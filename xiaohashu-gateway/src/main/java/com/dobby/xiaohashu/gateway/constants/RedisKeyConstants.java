package com.dobby.xiaohashu.gateway.constants;

/**
 * @author 橡皮鸭
 * @version 1.0
 * @date 2026/4/1 15:24
 */
public class RedisKeyConstants {

    /**
     * 用户对应角色集合key前缀
     */
    private static final String USER_ROLE_KEY_PREFIX = "user:roles:";

    /**
     * 角色对应权限集合key前缀
     */
    private static final String ROLE_PERMISSION_KEY_PREFIX = "role:permissions:";

    /**
     * 构建角色对应权限集合key
     * @param roleKey
     * @return
     */
    public static String buildRolePermissionKey(String roleKey) {
        return ROLE_PERMISSION_KEY_PREFIX + roleKey;
    }

    /**
     * 构建用户对应角色集合key
     * @param userId
     * @return
     */
    public static String buildUserRoleKey(Long userId) {
        return USER_ROLE_KEY_PREFIX + userId;
    }
}
