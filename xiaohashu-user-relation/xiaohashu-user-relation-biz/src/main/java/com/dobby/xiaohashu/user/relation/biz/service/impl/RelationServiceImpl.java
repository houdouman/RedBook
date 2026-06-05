package com.dobby.xiaohashu.user.relation.biz.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import com.dobby.framework.biz.context.filter.LoginUserContextHolder;
import com.dobby.framework.common.exception.BizException;
import com.dobby.framework.common.response.PageResponse;
import com.dobby.framework.common.response.Response;
import com.dobby.framework.common.util.DateUtils;
import com.dobby.framework.common.util.JsonUtils;
import com.dobby.xiaohashu.user.dto.rsp.FindUserByIdRspDTO;
import com.dobby.xiaohashu.user.relation.biz.constant.MQConstants;
import com.dobby.xiaohashu.user.relation.biz.constant.RedisKeyConstants;
import com.dobby.xiaohashu.user.relation.biz.domain.dataobject.FansDO;
import com.dobby.xiaohashu.user.relation.biz.domain.dataobject.FollowDO;
import com.dobby.xiaohashu.user.relation.biz.domain.mapper.FansDOMapper;
import com.dobby.xiaohashu.user.relation.biz.domain.mapper.FollowDOMapper;
import com.dobby.xiaohashu.user.relation.biz.enums.LuaResultEnum;
import com.dobby.xiaohashu.user.relation.biz.enums.ResponseCodeEnum;
import com.dobby.xiaohashu.user.relation.biz.model.dto.FollowUserMqDTO;
import com.dobby.xiaohashu.user.relation.biz.model.dto.UnfollowUserMqDTO;
import com.dobby.xiaohashu.user.relation.biz.model.vo.*;
import com.dobby.xiaohashu.user.relation.biz.rpc.UserRpcService;
import com.dobby.xiaohashu.user.relation.biz.service.RelationService;
import com.google.common.collect.Lists;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * @author 橡皮鸭
 * @version 1.0
 * @date 2026/4/14 17:41
 */
@Service
@Slf4j
public class RelationServiceImpl implements RelationService {

    @Resource
    private UserRpcService userRpcService;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private FollowDOMapper followingDOMapper;
    @Resource
    private RocketMQTemplate rocketMQTemplate;
    @Resource
    private FansDOMapper fansDOMapper;
    @Resource(name = "taskExecutor")
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;

    /**
     * 关注用户
     * @param followUserReqVO
     * @return
     */
    @Override
    public Response<?> follow(FollowUserReqVO followUserReqVO) {
        // 关注的用户 ID
        Long followUserId = followUserReqVO.getFollowUserId();

        // 当前登录的用户 ID
        Long userId = LoginUserContextHolder.getUserId();

        // 校验：无法关注自己
        if (Objects.equals(userId, followUserId)) {
            throw new BizException(ResponseCodeEnum.CANT_FOLLOW_YOUR_SELF);
        }

        // 校验关注的用户是否存在
        FindUserByIdRspDTO findUserByIdRspDTO = userRpcService.findById(followUserId);

        if (Objects.isNull(findUserByIdRspDTO)) {
            throw new BizException(ResponseCodeEnum.FOLLOW_USER_NOT_EXISTED);
        }

        // 构建当前用户关注列表的 Redis Key
        String followingRedisKey = RedisKeyConstants.buildUserFollowingKey(userId);

        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        // Lua 脚本路径
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/follow_check_and_add.lua")));
        // 返回值类型
        script.setResultType(Long.class);

        // 当前时间
        LocalDateTime now = LocalDateTime.now();
        // 当前时间转时间戳
        long timestamp = DateUtils.localDateTime2Timestamp(now);

        // 执行 Lua 脚本，拿到返回结果
        Long result = redisTemplate.execute(script, Collections.singletonList(followingRedisKey), followUserId, timestamp);

        // 校验 Lua 脚本执行结果
        checkLuaScriptResult(result);

        // ZSET 不存在
        if (Objects.equals(result, LuaResultEnum.ZSET_NOT_EXIST.getCode())) {
            // 从数据库查询当前用户的关注关系记录
            List<FollowDO> followingDOS = followingDOMapper.selectByUserId(userId);

            // 随机过期时间
            // 保底1天+随机秒数
            long expireSeconds = 60*60*24 + RandomUtil.randomInt(60*60*24);

            // 若记录为空，直接 ZADD 对象, 并设置过期时间
            if (CollUtil.isEmpty(followingDOS)) {
                DefaultRedisScript<Long> script2 = new DefaultRedisScript<>();
                script2.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/follow_add_and_expire.lua")));
                script2.setResultType(Long.class);
                //采用统一的过期策略
                redisTemplate.execute(script2, Collections.singletonList(followingRedisKey), followUserId, timestamp, expireSeconds);
            } else { // 若记录不为空，则将关注关系数据全量同步到 Redis 中，并设置过期时间；
                // 构建 Lua 参数
                Object[] luaArgs = buildLuaArgs(followingDOS, expireSeconds);

                // 执行 Lua 脚本，批量同步关注关系数据到 Redis 中
                DefaultRedisScript<Long> script3 = new DefaultRedisScript<>();
                script3.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/follow_batch_add_and_expire.lua")));
                script3.setResultType(Long.class);
                redisTemplate.execute(script3, Collections.singletonList(followingRedisKey), luaArgs);

                // 再次调用上面的 Lua 脚本：follow_check_and_add.lua , 将最新的关注关系添加进去
                result = redisTemplate.execute(script, Collections.singletonList(followingRedisKey), followUserId, timestamp);
                checkLuaScriptResult(result);
            }
        }

        // 发送 MQ
        // 构建消息体 DTO
        FollowUserMqDTO followUserMqDTO = FollowUserMqDTO.builder()
                .userId(userId)
                .followUserId(followUserId)
                .createTime(now)
                .build();

        // 构建消息对象，并将 DTO 转成 Json 字符串设置到消息体中
        Message<String> message = MessageBuilder.withPayload(JsonUtils.toJsonString(followUserMqDTO))
                .build();

        // 通过冒号连接, 可让 MQ 发送给主题 Topic 时，携带上标签 Tag
        String destination = MQConstants.TOPIC_FOLLOW_OR_UNFOLLOW + ":" + MQConstants.TAG_FOLLOW;

        log.info("==> 开始发送关注操作 MQ, 消息体: {}", followUserMqDTO);

        //确保同一用户的关注、取关等操作按顺序进行
        String hashKey = String.valueOf(userId);

        // 异步发送 MQ 消息，提升接口响应速度
        rocketMQTemplate.asyncSendOrderly(destination, message, hashKey, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> MQ 发送成功，SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("==> MQ 发送异常: ", throwable);
            }
        });

        return Response.success();
    }

    /**
     * 取关用户
     * @param unfollowUserReqVO
     * @return
     */
    @Override
    public Response<?> unfollow(UnfollowUserReqVO unfollowUserReqVO) {
        //不能关注自己
        Long unfollowUserId = unfollowUserReqVO.getUnfollowUserId();
        // 当前登录用户 ID
        Long userId = LoginUserContextHolder.getUserId();
        if (Objects.equals(userId, unfollowUserId)) {
            throw new BizException(ResponseCodeEnum.CANT_UNFOLLOW_YOUR_SELF);
        }
        //取关用户不存在
        FindUserByIdRspDTO findUserByIdRspDTO = userRpcService.findById(unfollowUserId);
        if(Objects.isNull(findUserByIdRspDTO)) {
            throw new BizException(ResponseCodeEnum.FOLLOW_USER_NOT_EXISTED);
        }
        //关注用户在redis的数据
        String followingRedisKey = RedisKeyConstants.buildUserFollowingKey(userId);
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/unfollow_check_and_delete.lua")));
        script.setResultType(Long.class);
        // 执行 Lua 脚本，拿到返回结果
        Long result = redisTemplate.execute(script, Collections.singletonList(followingRedisKey), unfollowUserId);
        // 取关的用户不在关注列表中
        if (Objects.equals(result, LuaResultEnum.NOT_FOLLOWED.getCode())) {
            throw new BizException(ResponseCodeEnum.NOT_FOLLOWED);
        }
        //zset关注列表不存在，从数据库获取数据并存入redis，再判断取关
        if (Objects.equals(result, LuaResultEnum.ZSET_NOT_EXIST.getCode())) {
            List<FollowDO> followingDOS = followingDOMapper.selectByUserId(userId);
            // 保底1天+随机秒数
            long expireSeconds = 60*60*24 + RandomUtil.randomInt(60*60*24);
            // 若记录为空，则表示还未关注任何人，提示还未关注对方
            if (CollUtil.isEmpty(followingDOS)) {
                throw new BizException(ResponseCodeEnum.NOT_FOLLOWED);
            } else { // 若记录不为空，则将关注关系数据全量同步到 Redis 中，并设置过期时间；
                // 构建 Lua 参数
                Object[] luaArgs = buildLuaArgs(followingDOS, expireSeconds);

                // 执行 Lua 脚本，批量同步关注关系数据到 Redis 中
                DefaultRedisScript<Long> script3 = new DefaultRedisScript<>();
                script3.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/follow_batch_add_and_expire.lua")));
                script3.setResultType(Long.class);
                redisTemplate.execute(script3, Collections.singletonList(followingRedisKey), luaArgs);
                // 再次调用上面的 Lua 脚本：unfollow_check_and_delete.lua , 将取关的用户删除
                result = redisTemplate.execute(script, Collections.singletonList(followingRedisKey), unfollowUserId);
                // 再次校验结果
                if (Objects.equals(result, LuaResultEnum.NOT_FOLLOWED.getCode())) {
                    throw new BizException(ResponseCodeEnum.NOT_FOLLOWED);
                }
            }
        }

        //构建消息体DTO
        UnfollowUserMqDTO unfollowUserMqDTO = UnfollowUserMqDTO.builder()
                .userId(userId)
                .unfollowUserId(unfollowUserId)
                .createTime(LocalDateTime.now())
                .build();
        Message<String> message = MessageBuilder.withPayload(JsonUtils.toJsonString(unfollowUserMqDTO)).build();
        String destination = MQConstants.TOPIC_FOLLOW_OR_UNFOLLOW + ":" + MQConstants.TAG_UNFOLLOW;
        log.info("==> 开始发送取关操作 MQ, 消息体: {}", unfollowUserMqDTO);
        String hashKey = String.valueOf(userId);

        // 异步发送 MQ 消息，提升接口响应速度
        rocketMQTemplate.asyncSendOrderly(destination, message, hashKey, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> MQ 发送成功，SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("==> MQ 发送异常: ", throwable);
            }
        });
        return Response.success();
    }

    /**
     * 获取用户关注列表详细信息
     * @param findFollowingListReqVO
     * @return
     */
    @Override
    public PageResponse<FindFollowingUserRspVO> findFollowingList(FindFollowingListReqVO findFollowingListReqVO) {
        // 想要查询的用户 ID
        Long userId = findFollowingListReqVO.getUserId();
        // 页码
        Integer pageNo = findFollowingListReqVO.getPageNo();

        //从 Redis 中查询
        String followingListRedisKey = RedisKeyConstants.buildUserFollowingKey(userId);
        //查询目标用户关注列表 ZSet 的总大小
        long total = redisTemplate.opsForZSet().zCard(followingListRedisKey);
        //返参
        List<FindFollowingUserRspVO> findFollowingUserRspVOS = null;
        long limit = 10;
        if(total>0){
            long totalPage = PageResponse.getTotalPage(total, limit);
            //请求页码超出总页数
            if (pageNo > totalPage) return PageResponse.success(null, pageNo, total);
            long offset = (pageNo-1)*limit; //偏移量
            //按score降序获取元素，并使用limit字句实现分页;使用Double.POSITIVE_INFINITY和Double.NEGATIVE_INFINITY作为分数范围
            Set<Object> followingUserIdsSet = redisTemplate.opsForZSet()
                    .reverseRangeByScore(followingListRedisKey, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, offset, limit);
            if (CollUtil.isNotEmpty(followingUserIdsSet)) {
                //提取所有用户ID到集合中
                List<Long> userIds = followingUserIdsSet.stream().map(object -> Long.valueOf(object.toString())).toList();
                //RPC: 批量查询用户信息
                List<FindUserByIdRspDTO> findUserByIdRspDTOS = userRpcService.findByIds(userIds);
                if (CollUtil.isNotEmpty(findUserByIdRspDTOS)) {
                    findFollowingUserRspVOS = findUserByIdRspDTOS.stream()
                            .map(dto -> FindFollowingUserRspVO.builder()
                                    .userId(dto.getId())
                                    .avatar(dto.getAvatar())
                                    .nickname(dto.getNickName())
                                    .introduction(dto.getIntroduction())
                                    .build())
                            .toList();
                }
            }
        } else {
            //若 Redis 中没有数据，则从数据库查询
            long count = followingDOMapper.selectCountByUserId(userId);
            long totalPage = PageResponse.getTotalPage(count, limit);
            if (pageNo > totalPage) return PageResponse.success(null, pageNo, count);
            long offset = PageResponse.getOffset(pageNo, limit);
            //分页查询
            List<FollowDO> followingDOS = followingDOMapper.selectPageListByUserId(userId, offset, limit);
            //赋值真实的记录总数
            total = count;
            // 若记录不为空
            if (CollUtil.isNotEmpty(followingDOS)) {
                // 提取所有关注用户 ID 到集合中
                List<Long> userIds = followingDOS.stream().map(FollowDO::getFollowingUserId).toList();
                // RPC: 调用用户服务，并将 DTO 转换为 VO
                findFollowingUserRspVOS = rpcUserServiceAndDTO2VO(userIds, findFollowingUserRspVOS);

                //异步将关注列表全量同步到 Redis
                threadPoolTaskExecutor.submit(() -> syncFollowingList2Redis(userId));
            }
        }

        return PageResponse.success(findFollowingUserRspVOS, pageNo, total);
    }

    @Override
    public PageResponse<FindFansUserRspVO> findFansList(FindFansListReqVO findFansListReqVO) {
        Long userId = findFansListReqVO.getUserId();
        Integer pageNo = findFansListReqVO.getPageNo();
        //先从redis查询
        String fansListRedisKey = RedisKeyConstants.buildUserFansKey(userId);
        //获取总数
        long total = redisTemplate.opsForZSet().zCard(fansListRedisKey);
        List<FindFansUserRspVO> findFansUserRspVOS = null;
        long limit = 10;
        if(total>0){
            //计算一共多少页
            long totalPage = PageResponse.getTotalPage(total, limit);
            if (pageNo > totalPage) return PageResponse.success(null, pageNo, total);
            long offset = PageResponse.getOffset(pageNo, limit);
            Set<Object> followingUserIdsSet = redisTemplate.opsForZSet()
                    .reverseRangeByScore(fansListRedisKey, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, offset, limit);
            if (CollUtil.isNotEmpty(followingUserIdsSet)) {
                //获取用户id
                List<Long> userIds = followingUserIdsSet.stream().map(object -> Long.valueOf(object.toString())).toList();
                // RPC: 批量查询用户信息
                findFansUserRspVOS = rpcUserServiceAndCountServiceAndDTO2VO(userIds, findFansUserRspVOS);
            }
        }else{
            total = fansDOMapper.selectCountByUserId(userId);
            long totalPage = PageResponse.getTotalPage(total, limit);
            if (pageNo > totalPage) return PageResponse.success(null, pageNo, total);
            long offset = PageResponse.getOffset(pageNo, limit);
            //分页查询
            List<FansDO> fansDOS = fansDOMapper.selectPageListByUserId(userId, offset, limit);
            if(CollUtil.isNotEmpty(fansDOS)){
                List<Long> userIds = fansDOS.stream().map(FansDO::getFansUserId).toList();
                //RPC：调用用户服务
                findFansUserRspVOS = rpcUserServiceAndCountServiceAndDTO2VO(userIds, findFansUserRspVOS);
                // 异步将粉丝列表同步到 Redis（最多5000条）
                threadPoolTaskExecutor.submit(() -> syncFansList2Redis(userId));
            }
        }

        return PageResponse.success(findFansUserRspVOS, pageNo, total);
    }

    /**
     * RPC: 调用用户服务，并将 DTO 转换为 VO
     * @param userIds
     * @param findFollowingUserRspVOS
     * @return
     */
    private List<FindFollowingUserRspVO> rpcUserServiceAndDTO2VO(List<Long> userIds, List<FindFollowingUserRspVO> findFollowingUserRspVOS) {
        // RPC: 批量查询用户信息
        List<FindUserByIdRspDTO> findUserByIdRspDTOS = userRpcService.findByIds(userIds);

        // 若不为空，DTO 转 VO
        if (CollUtil.isNotEmpty(findUserByIdRspDTOS)) {
            findFollowingUserRspVOS = findUserByIdRspDTOS.stream()
                    .map(dto -> FindFollowingUserRspVO.builder()
                            .userId(dto.getId())
                            .avatar(dto.getAvatar())
                            .nickname(dto.getNickName())
                            .introduction(dto.getIntroduction())
                            .build())
                    .toList();
        }
        return findFollowingUserRspVOS;
    }

    /**
     * RPC: 调用用户服务、计数服务，并将 DTO 转换为 VO 粉丝列表
     * @param userIds
     * @param findFansUserRspVOS
     * @return
     */
    private List<FindFansUserRspVO> rpcUserServiceAndCountServiceAndDTO2VO(List<Long> userIds, List<FindFansUserRspVO> findFansUserRspVOS) {
        // RPC: 批量查询用户信息
        List<FindUserByIdRspDTO> findUserByIdRspDTOS = userRpcService.findByIds(userIds);

        // TODO RPC: 批量查询用户的计数数据（笔记总数、粉丝总数）

        // 若不为空，DTO 转 VO
        if (CollUtil.isNotEmpty(findUserByIdRspDTOS)) {
            findFansUserRspVOS = findUserByIdRspDTOS.stream()
                    .map(dto -> FindFansUserRspVO.builder()
                            .userId(dto.getId())
                            .avatar(dto.getAvatar())
                            .nickname(dto.getNickName())
                            .noteTotal(0L)
                            .fansTotal(0L)
                            .build())
                    .toList();
        }
        return findFansUserRspVOS;
    }

    /**
     * 全量同步关注列表至 Redis 中
     */
    private void syncFollowingList2Redis(Long userId) {
        //查询全量关注用户列表（最多1000位用户）
        List<FollowDO> followingDOS = followingDOMapper.selectAllByUserId(userId);
        if (CollUtil.isNotEmpty(followingDOS)) {
            // 用户关注列表 Redis Key
            String followingListRedisKey = RedisKeyConstants.buildUserFollowingKey(userId);
            // 随机过期时间
            // 保底1天+随机秒数
            long expireSeconds = 60*60*24 + RandomUtil.randomInt(60*60*24);
            // 构建 Lua 参数
            Object[] luaArgs = buildLuaArgs(followingDOS, expireSeconds);

            // 执行 Lua 脚本，批量同步关注关系数据到 Redis 中
            DefaultRedisScript<Long> script = new DefaultRedisScript<>();
            script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/follow_batch_add_and_expire.lua")));
            script.setResultType(Long.class);
            redisTemplate.execute(script, Collections.singletonList(followingListRedisKey), luaArgs);
        }
    }

    /**
     * 粉丝列表同步到 Redis（最多5000条）
     * @param userId
     */
    private void syncFansList2Redis(Long userId) {
        List<FansDO> fansDOS = fansDOMapper.select5000FansByUserId(userId);
        if (CollUtil.isNotEmpty(fansDOS)) {
            String fansUserRedisKey = RedisKeyConstants.buildUserFansKey(userId);
            long expireSeconds = 60*60*24 + RandomUtil.randomInt(60*60*24);
            Object[] luaArgs = buildFansZSetLuaArgs(fansDOS, expireSeconds);
            DefaultRedisScript<Long> script = new DefaultRedisScript<>();
            script.setScriptSource(new ResourceScriptSource( new ClassPathResource("/lua/follow_batch_add_and_expire.lua")));
            script.setResultType(Long.class);
            redisTemplate.execute(script, Collections.singletonList(fansUserRedisKey), luaArgs);
        }
    }

    /**
     * 校验 Lua 脚本结果，根据状态码抛出对应的业务异常
     * @param result
     */
    private static void checkLuaScriptResult(Long result) {
        LuaResultEnum luaResultEnum = LuaResultEnum.valueOf(result);

        if (Objects.isNull(luaResultEnum)) throw new RuntimeException("Lua 返回结果错误");
        // 校验 Lua 脚本执行结果
        switch (luaResultEnum) {
            // 关注数已达到上限
            case FOLLOW_LIMIT -> throw new BizException(ResponseCodeEnum.FOLLOWING_COUNT_LIMIT);
            // 已经关注了该用户
            case ALREADY_FOLLOWED -> throw new BizException(ResponseCodeEnum.ALREADY_FOLLOWED);
        }
    }

    /**
     * 构建 Lua 脚本参数
     *
     * @param followingDOS
     * @param expireSeconds
     * @return
     */
    private static Object[] buildLuaArgs(List<FollowDO> followingDOS, long expireSeconds) {
        int argsLength = followingDOS.size() * 2 + 1; // 每个关注关系有 2 个参数（score 和 value），再加一个过期时间
        Object[] luaArgs = new Object[argsLength];

        int i = 0;
        for (FollowDO following : followingDOS) {
            luaArgs[i] = DateUtils.localDateTime2Timestamp(following.getCreateTime()); // 关注时间作为 score
            luaArgs[i + 1] = following.getFollowingUserId();          // 关注的用户 ID 作为 ZSet value
            i += 2;
        }

        luaArgs[argsLength - 1] = expireSeconds; // 最后一个参数是 ZSet 的过期时间
        return luaArgs;
    }


    /**
     * 构建 Lua 脚本参数：粉丝列表
     * @param fansDOS
     * @param expireSeconds
     * @return
     */
    private static Object[] buildFansZSetLuaArgs(List<FansDO> fansDOS, long expireSeconds) {
        int argsLength = fansDOS.size() * 2 + 1; // 每个粉丝关系有 2 个参数（score 和 value），再加一个过期时间
        Object[] luaArgs = new Object[argsLength];

        int i = 0;
        for (FansDO fansDO : fansDOS) {
            luaArgs[i] = DateUtils.localDateTime2Timestamp(fansDO.getCreateTime()); // 粉丝的关注时间作为 score
            luaArgs[i + 1] = fansDO.getFansUserId();          // 粉丝的用户 ID 作为 ZSet value
            i += 2;
        }

        luaArgs[argsLength - 1] = expireSeconds; // 最后一个参数是 ZSet 的过期时间
        return luaArgs;
    }
}
