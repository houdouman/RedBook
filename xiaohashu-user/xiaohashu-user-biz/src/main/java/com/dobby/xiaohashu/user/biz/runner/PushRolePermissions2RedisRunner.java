package com.dobby.xiaohashu.user.biz.runner;

import com.dobby.framework.common.util.JsonUtils;
import com.dobby.xiaohashu.user.biz.constant.RedisKeyConstants;
import com.dobby.xiaohashu.user.biz.domain.dataobject.PermissionDO;
import com.dobby.xiaohashu.user.biz.domain.dataobject.RoleDO;
import com.dobby.xiaohashu.user.biz.domain.dataobject.RolePermissionDO;
import com.dobby.xiaohashu.user.biz.domain.mapper.PermissionDOMapper;
import com.dobby.xiaohashu.user.biz.domain.mapper.RoleDOMapper;
import com.dobby.xiaohashu.user.biz.domain.mapper.RolePermissionDOMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author 橡皮鸭
 * @version 1.0
 * @date 2026/3/31 17:12
 */
@Component
@Slf4j
public class PushRolePermissions2RedisRunner implements ApplicationRunner {
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private RoleDOMapper roleDOMapper;
    @Resource
    private RolePermissionDOMapper rolePermissionDOMapper;
    @Resource
    private PermissionDOMapper permissionDOMapper;

    //权限同步标记 Key
    private static final String PUSH_PERMISSION_FLAG = "push.permission.flag";

    @Override
    public void run(ApplicationArguments args) {
        log.info("==> 服务启动，开始同步角色权限数据到 Redis 中...");
        try {
            // 是否能够同步数据: 原子操作
            // 只有在键PUSH_PERMISSION_FLAG不存在时，才会设置该键的值为"1"，并设置过期时间为1天
            boolean canPushed = redisTemplate.opsForValue().setIfAbsent(PUSH_PERMISSION_FLAG, "1", 1, TimeUnit.DAYS);
            if (!canPushed) {
                log.warn("==> 角色权限数据已经同步至 Redis 中，不再同步...");
                return;
            }
            //查询出所有可用角色
            List<RoleDO> roleDOS = roleDOMapper.selectEnabledList();
            if(!CollectionUtils.isEmpty(roleDOS)){
                //获取所有角色id
                List<Long> roleIds = roleDOS.stream().map(RoleDO::getId).toList();
                //根据角色id获取所有角色对应权限
                List<RolePermissionDO> rolePermissionDOS = rolePermissionDOMapper.selectByRoleIds(roleIds);
                //按角色id分组，一个角色id对应多个权限
                Map<Long, List<Long>> rolePerimssionMap = rolePermissionDOS.stream().collect(
                        Collectors.groupingBy(RolePermissionDO::getRoleId,
                                Collectors.mapping(RolePermissionDO::getPermissionId, Collectors.toList()))
                );
                //获取app端所有启用的权限
                List<PermissionDO> permissionDOS = permissionDOMapper.selectAppEnabledList();
                //得到权限id-权限do的map
                Map<Long,PermissionDO> permissionIdDOMap = permissionDOS.stream().collect(
                        Collectors.toMap(PermissionDO::getId, p -> p)
                );
                //组织 角色id-权限 关系
                Map<String, List<String>> roleIdPermissionDOMap = Maps.newHashMap();

                roleDOS.forEach(roleDO -> {
                    //当前角色id
                    Long roleId = roleDO.getId();
                    String roleKey = roleDO.getRoleKey();
                    //角色id对应的权限id集合
                    List<Long> permissionIds = rolePerimssionMap.get(roleId);
                    if(!CollectionUtils.isEmpty(permissionIds)) {
                        List<String> permissionKeys = Lists.newArrayList();
                        permissionIds.forEach(permissionId -> {
                            PermissionDO permissionDO = permissionIdDOMap.get(permissionId);
                            permissionKeys.add(permissionDO.getPermissionKey());
                        });
                        roleIdPermissionDOMap.put(roleKey, permissionKeys);
                    }
                });
                //同步到redis，方便后续网关查询鉴权使用
                roleIdPermissionDOMap.forEach((roleKey, permissionKeys) -> {
                    String key = RedisKeyConstants.buildRolePermissionsKey(roleKey);
                    redisTemplate.opsForValue().set(key, JsonUtils.toJsonString(permissionKeys));
                });
            }
            log.info("==> 服务启动，成功同步角色权限数据到 Redis 中...");
        } catch (Exception e) {
            log.error("==> 同步角色权限数据到 Redis 中失败: ", e);
        }
    }
}
