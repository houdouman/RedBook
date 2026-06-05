package com.dobby.xiaohashu.gateway.auth;

import cn.dev33.satoken.stp.StpInterface;
import com.dobby.xiaohashu.gateway.constants.RedisKeyConstants;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.util.Lists;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * @author 橡皮鸭
 * @version 1.0
 * @date 2026/4/1 11:13
 * 自定义权限验证接口扩展
 */
@Component
@Slf4j
public class StpInterfaceImpl implements StpInterface {
    @Resource
    private RedisTemplate redisTemplate;
    @Resource
    private ObjectMapper objectMapper;

    /**
     * 获取用户角色
     * @param loginId
     * @param loginType
     * @return
     */
    @SneakyThrows
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        // 返回此 loginId 拥有的权限列表
        log.info("## 获取用户角色列表, loginId: {}", loginId);
        //构建用户对应的角色集合key
        String userRolesKey = RedisKeyConstants.buildUserRoleKey(Long.valueOf(loginId.toString()));
        //通过key获取用户对应角色value
        String roleKey = redisTemplate.opsForValue().get(userRolesKey).toString();
        if(StringUtils.isEmpty(roleKey)){
            return null;
        }
        //将JSON字符串反序列化为List<String>集合，readValue()用于从JSON反序列化为Java对象
        return objectMapper.readValue(userRolesKey, new TypeReference<List<String>>() {});
    }

    @SneakyThrows
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        // 返回此 loginId 拥有的角色列表
        log.info("## 获取用户权限列表, loginId: {}", loginId);
        // 构建 用户-角色 Redis Key
        String userRolesKey = RedisKeyConstants.buildUserRoleKey(Long.valueOf(loginId.toString()));
        // 根据用户 ID ，从 Redis 中获取该用户的角色集合
        String useRolesValue = redisTemplate.opsForValue().get(userRolesKey).toString();

        if (StringUtils.isBlank(useRolesValue)) {
            return null;
        }
        // 将 JSON 字符串转换为 List<String> 角色集合
        List<String> userRoleKeys = objectMapper.readValue(useRolesValue, new TypeReference<>() {});

        if(CollectionUtils.isEmpty(userRoleKeys)){
            return null;
        }
        //构建角色-权限集合key
        List<String> rolePermissionsKeys = userRoleKeys.stream()
                .map(RedisKeyConstants::buildRolePermissionKey)
                .toList();
        //通过key获取角色批量查询对应权限value
        List<String> rolePermissionsValues = redisTemplate.opsForValue().multiGet(rolePermissionsKeys);
        if(CollectionUtils.isEmpty(rolePermissionsValues)){
            return null;
        }
        List<String> permissions = Lists.newArrayList();
        //遍历所有角色的权限集合，统一添加到 permissions 集合中
        rolePermissionsValues.forEach(jsonValue -> {
            try {
                List<String> rolePermissions = objectMapper.readValue(jsonValue, new TypeReference<>() {});
                permissions.addAll(rolePermissions);
            } catch (JsonProcessingException e) {
                log.error("==> JSON 解析错误: ", e);
            }
        });

        return permissions;
    }
}
