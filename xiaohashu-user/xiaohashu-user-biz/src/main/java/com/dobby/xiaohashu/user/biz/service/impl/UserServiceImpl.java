package com.dobby.xiaohashu.user.biz.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import com.dobby.framework.biz.context.filter.LoginUserContextHolder;
import com.dobby.framework.common.enums.DeletedEnum;
import com.dobby.framework.common.enums.StatusEnum;
import com.dobby.framework.common.exception.BizException;
import com.dobby.framework.common.response.Response;
import com.dobby.framework.common.util.JsonUtils;
import com.dobby.framework.common.util.ParamUtils;
import com.dobby.xiaohashu.user.biz.constant.MQConstants;
import com.dobby.xiaohashu.user.biz.constant.RedisKeyConstants;
import com.dobby.xiaohashu.user.biz.constant.RoleConstants;
import com.dobby.xiaohashu.user.biz.domain.dataobject.RoleDO;
import com.dobby.xiaohashu.user.biz.domain.dataobject.UserDO;
import com.dobby.xiaohashu.user.biz.domain.dataobject.UserRoleDO;
import com.dobby.xiaohashu.user.biz.domain.mapper.RoleDOMapper;
import com.dobby.xiaohashu.user.biz.domain.mapper.UserDOMapper;
import com.dobby.xiaohashu.user.biz.domain.mapper.UserRoleDOMapper;
import com.dobby.xiaohashu.user.biz.enums.ResponseCodeEnum;
import com.dobby.xiaohashu.user.biz.enums.SexEnum;
import com.dobby.xiaohashu.user.biz.model.vo.UpdatePasswordReqVO;
import com.dobby.xiaohashu.user.biz.model.vo.UpdateUserInfoReqVO;
import com.dobby.xiaohashu.user.biz.rpc.DistributedIdGeneratorRpcService;
import com.dobby.xiaohashu.user.biz.rpc.OssRpcService;
import com.dobby.xiaohashu.user.biz.service.UserService;
import com.dobby.xiaohashu.user.dto.req.FindUserByIdReqDTO;
import com.dobby.xiaohashu.user.dto.req.FindUserByPhoneReqDTO;
import com.dobby.xiaohashu.user.dto.req.FindUsersByIdsReqDTO;
import com.dobby.xiaohashu.user.dto.req.RegisterUserReqDTO;
import com.dobby.xiaohashu.user.dto.rsp.FindUserByIdRspDTO;
import com.dobby.xiaohashu.user.dto.rsp.FindUserByPhoneRspDTO;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.checkerframework.checker.units.qual.K;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author 橡皮鸭
 * @version 1.0
 * @date 2026/4/3 10:44
 */
@Service
@Slf4j
public class UserServiceImpl implements UserService {

    @Resource
    private UserDOMapper userDOMapper;
    @Resource
    private OssRpcService ossRpcService;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private UserRoleDOMapper userRoleDOMapper;
    @Resource
    private RoleDOMapper roleDOMapper;
    @Resource(name = "taskExecutor")
    private ThreadPoolTaskExecutor taskExecutor;
    @Resource
    private PasswordEncoder passwordEncoder;
    @Resource
    private DistributedIdGeneratorRpcService distributedIdGeneratorRpcService;

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    /**
     * 用户信息本地缓存
     */
    private static final Cache<Long, FindUserByIdRspDTO> LOCAL_CACHE = Caffeine.newBuilder()
            .initialCapacity(10000) // 设置初始容量为 10000 个条目
            .maximumSize(10000) // 设置缓存的最大容量为 10000 个条目
            .expireAfterWrite(1, TimeUnit.HOURS) // 设置缓存条目在写入后 1 小时过期
            .build();
    @Autowired
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;


    /**
     * 更新用户信息
     * @param uptVO
     * @return
     */
    @Override
    public Response<?> updateUserInfo(UpdateUserInfoReqVO uptVO) {
        UserDO userDO = new UserDO();
        //获取当前请求用户id
        userDO.setId(LoginUserContextHolder.getUserId());
        // 标识位：是否需要更新
        boolean needUpdate = false;
        //存储头像并获取链接
        MultipartFile avatar = uptVO.getAvatar();
        if(Objects.nonNull(avatar)){
            log.info("==> 进入对象存储过程...");
            //调用对象存储服务上传文件，并获取图像访问链接
            String urlAt = ossRpcService.uploadFile(avatar);
            log.info("==> 调用oss服务成功，上传头像，url：{}", urlAt);
            //上传头像失败
            if(StringUtils.isBlank(urlAt)){
                throw new BizException(ResponseCodeEnum.UPLOAD_AVATAR_FAIL);
            }
            userDO.setAvatar(urlAt);
            needUpdate = true;
            //删除缓存
            String userRedisKey = RedisKeyConstants.buildUserInfoKey(userDO.getId());
            redisTemplate.delete(userRedisKey);
            rocketMQTemplate.syncSend(MQConstants.TOPIC_DELETE_USER_LOCAL_CACHE, userDO.getId());
            log.info("====> MQ：删除用户本地缓存发送成功...");
        }
        //生日
        LocalDate birthday = uptVO.getBirthday();
        if(Objects.nonNull(birthday)){
            userDO.setBirthday(birthday);
            needUpdate = true;
        }
        //昵称
        String nickname = uptVO.getNickname();
        if(StringUtils.isNotBlank(nickname)){
            Preconditions.checkArgument(ParamUtils.checkNickname(nickname), ResponseCodeEnum.NICK_NAME_VALID_FAIL.getErrorMessage());
            userDO.setNickname(nickname);
            needUpdate = true;
        }
        //小哈书号
        String xiaohashuId = uptVO.getXiaohashuId();
        if(StringUtils.isNotBlank(xiaohashuId)){
            Preconditions.checkArgument(ParamUtils.checkXiaohashuId(xiaohashuId), ResponseCodeEnum.XIAOHASHU_ID_VALID_FAIL.getErrorMessage());
            userDO.setXiaohashuId(xiaohashuId);
            needUpdate = true;
        }
        //性别
        Integer sex = uptVO.getSex();
        if(Objects.nonNull(sex)){
            Preconditions.checkArgument(SexEnum.isValid(sex), ResponseCodeEnum.SEX_VALID_FAIL.getErrorMessage());
            userDO.setSex(sex);
            needUpdate = true;
        }
        //个人介绍
        String introduction = uptVO.getIntroduction();
        if(StringUtils.isNotBlank(introduction)){
            Preconditions.checkArgument(ParamUtils.checkLength(introduction, 100), ResponseCodeEnum.INTRODUCTION_VALID_FAIL.getErrorMessage());
            userDO.setIntroduction(introduction);
            needUpdate = true;
        }
        //背景图
        MultipartFile backgroundImg = uptVO.getBackgroundImg();
        if(Objects.nonNull(backgroundImg)){
            log.info("==> 进入对象存储过程...");
            //调用对象存储服务上传文件，并获取图像访问链接
            String urlBk = ossRpcService.uploadFile(backgroundImg);
            log.info("==> 调用oss服务成功，上传背景图，url：{}", urlBk);
            //上传头像失败
            if(StringUtils.isBlank(urlBk)){
                throw new BizException(ResponseCodeEnum.UPLOAD_BACKGROUND_IMG_FAIL);
            }
            userDO.setBackgroundImg(urlBk);
            needUpdate = true;
        }
        if(needUpdate){
            //更新用户信息
            userDO.setUpdateTime(LocalDateTime.now());
            userDOMapper.updateByPrimaryKeySelective(userDO);
        }
        return Response.success();
    }

    /**
     * 用户注册
     * @param registerUserReqDTO
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<Long> register(RegisterUserReqDTO registerUserReqDTO) {
        String phone = registerUserReqDTO.getPhone();
        // 先判断该手机号是否已被注册
        UserDO userDO1 = userDOMapper.selectByPhone(phone);
        if(Objects.nonNull(userDO1)){
            return Response.success(userDO1.getId());
        }

        //获取全局自增的小哈书id
        String xiaohashuId = distributedIdGeneratorRpcService.getXiaohashuId();
        String userIdStr = distributedIdGeneratorRpcService.getUserId();
        Long userId = Long.valueOf(userIdStr);

        UserDO userDO = UserDO.builder()
                .id(userId)
                .phone(phone)
                .xiaohashuId(xiaohashuId)
                .nickname("小红薯" + xiaohashuId)
                .status(StatusEnum.ENABLED.getValue())
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .isDeleted(DeletedEnum.NO.getValue())
                .build();

        userDOMapper.insert(userDO);
        //给用户分配默认角色
        UserRoleDO userRoleDO = UserRoleDO.builder()
                .roleId(RoleConstants.COMMON_USER_ROLE_ID)
                .userId(userId)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .isDeleted(DeletedEnum.NO.getValue())
                .build();
        userRoleDOMapper.insert(userRoleDO);
        RoleDO roleDO = roleDOMapper.selectByPrimaryKey(RoleConstants.COMMON_USER_ROLE_ID);
        //将用户的角色名存入redis，指定初始容量为1
        List<String> roles = new ArrayList<>(1);
        roles.add(roleDO.getRoleKey());
        String userRolesKey = RedisKeyConstants.buildUserRoleKey(userId);
        redisTemplate.opsForValue().set(userRolesKey, JsonUtils.toJsonString(roles));

        return Response.success(userId);

    }


    /**
     * 修改密码
     * @param updatePasswordReqVO
     * @return
     */
    @Override
    public Response<?> updatePassword(UpdatePasswordReqVO updatePasswordReqVO) {
        String password = updatePasswordReqVO.getNewPassword();
        String encodePassword = passwordEncoder.encode(password);
        log.info("==> 加密后的密码为：{}", encodePassword);
        //获取当前请求对应的用户id
        Long userId = LoginUserContextHolder.getUserId();
        UserDO userDO = UserDO.builder().id(userId).password(encodePassword).updateTime(LocalDateTime.now()).build();

        userDOMapper.updateByPrimaryKey(userDO);
        return Response.success();
    }


    /**
     * 根据手机号查询用户信息
     * @param findUserByPhoneReqDTO
     * @return
     */
    @Override
    public Response<FindUserByPhoneRspDTO> findByPhone(FindUserByPhoneReqDTO findUserByPhoneReqDTO) {
        String phone = findUserByPhoneReqDTO.getPhone();

        // 根据手机号查询用户信息
        UserDO userDO = userDOMapper.selectByPhone(phone);

        // 判空
        if (Objects.isNull(userDO)) {
            throw new BizException(ResponseCodeEnum.USER_NOT_FOUND);
        }

        // 构建返参
        FindUserByPhoneRspDTO findUserByPhoneRspDTO = FindUserByPhoneRspDTO.builder()
                .id(userDO.getId())
                .password(userDO.getPassword())
                .build();

        return Response.success(findUserByPhoneRspDTO);
    }


    /**
     * 根据id查询用户信息
     * 使用缓存
     * @param findUserByIdReqDTO
     * @return
     */
    @Override
    public Response<FindUserByIdRspDTO> findById(FindUserByIdReqDTO findUserByIdReqDTO) {
        Long id = findUserByIdReqDTO.getId();
        //使用二级缓存策略，先在本地缓存查询
        FindUserByIdRspDTO findUserByIdRspDTOLocalCache = LOCAL_CACHE.getIfPresent(id);
        if(Objects.nonNull(findUserByIdRspDTOLocalCache)){
            log.info("==> 命中了本地缓存；{}", findUserByIdRspDTOLocalCache);
            return Response.success(findUserByIdRspDTOLocalCache);
        }
        //再从redis缓存中查找
        String userInfoRedisKey = RedisKeyConstants.buildUserInfoKey(id);
        String userInfoRedisValue = (String) redisTemplate.opsForValue().get(userInfoRedisKey);
        if(StringUtils.isNotBlank(userInfoRedisValue)){
            //将存储的json转换为字符串
            FindUserByIdRspDTO findUserByIdRspDTO = JsonUtils.parseObject(userInfoRedisValue, FindUserByIdRspDTO.class);
            //用线程将数据异步存入本地缓存
            taskExecutor.submit(() -> {
                if(Objects.nonNull(findUserByIdRspDTO)){
                    LOCAL_CACHE.put(id,findUserByIdRspDTO);
                }
            });
            return Response.success(findUserByIdRspDTO);
        }
        //缓存中没找到，从数据库查找
        UserDO userDO = userDOMapper.selectByPrimaryKey(id);
        if (Objects.isNull(userDO)) {
            //使用线程进行异步处理，避免存入缓存影响其他请求
            taskExecutor.execute(() -> {
                //防止缓存穿透，将空数据存入缓存，设置较短过期时间
                long expireSeconds = 60 + RandomUtil.randomInt(60);
                redisTemplate.opsForValue().set(userInfoRedisKey, "null", expireSeconds, TimeUnit.SECONDS);
            });
            throw new BizException(ResponseCodeEnum.USER_NOT_FOUND);
        }
        FindUserByIdRspDTO findUserByIdRspDTO = FindUserByIdRspDTO.builder()
                .id(userDO.getId())
                .nickName(userDO.getNickname())
                .avatar(userDO.getAvatar())
                .introduction(userDO.getIntroduction())
                .build();

        //使用线程异步将用户信息存入缓存
        taskExecutor.submit(() -> {
            //过期时间设置长一点
            long expireSeconds = 60*60*24 + RandomUtil.randomInt(60*60*24);
            redisTemplate.opsForValue().set(userInfoRedisKey, JsonUtils.toJsonString(findUserByIdRspDTO), expireSeconds, TimeUnit.SECONDS);
        });
        return Response.success(findUserByIdRspDTO);
    }

    /**
     * 删除本地用户缓存
     * @param userId
     */
    @Override
    public void deleteUserLocalCache(Long userId) {
        LOCAL_CACHE.invalidate(userId);
    }

    /**
     * 根据批量用户id获取用户信息
     * @param findUsersByIdsReqDTO
     * @return
     */
    @Override
    public Response<List<FindUserByIdRspDTO>> findByIds(FindUsersByIdsReqDTO findUsersByIdsReqDTO) {
        List<Long> userIds = findUsersByIdsReqDTO.getIds();
        //先从redis查找
        List<String> redisKeys = userIds.stream()
                .map(RedisKeyConstants::buildUserInfoKey)
                .toList();
        //批量查询
        List<Object> redisValues = redisTemplate.opsForValue().multiGet(redisKeys);
        if(CollUtil.isNotEmpty(redisValues)){
            //过滤掉为空的数据
            redisValues = redisValues.stream().filter(Objects::nonNull).toList();
        }
        //返参
        List<FindUserByIdRspDTO> findUserByIdRspDTOS = Lists.newArrayList();
        if(CollUtil.isNotEmpty(redisValues)){
            findUserByIdRspDTOS = redisValues.stream()
                    .map(value -> JsonUtils.parseObject(String.valueOf(value), FindUserByIdRspDTO.class))
                    .collect(Collectors.toList());
        }
        //如果被查询的用户信息都在缓存，则直接返回
        if(CollUtil.size(userIds) == CollUtil.size(findUserByIdRspDTOS)){
            return Response.success(findUserByIdRspDTOS);
        }
        //缓存中数据不全，需要从数据库获取
        List<Long> userIdsNeedQuery = null;
        if(CollUtil.isNotEmpty(findUserByIdRspDTOS)){
            // 将 findUserInfoByIdRspDTOS 集合转 Map
            Map<Long, FindUserByIdRspDTO> map = findUserByIdRspDTOS.stream()
                    .collect(Collectors.toMap(FindUserByIdRspDTO::getId, p -> p));
            //筛选需要查询的用户id
            userIdsNeedQuery = userIds.stream().filter(id -> Objects.isNull(map.get(id))).toList();
        }else{
            //缓存无数据，需从数据库获取
            userIdsNeedQuery = userIds;
        }
        //从数据库查询
        List<UserDO> userDOS = userDOMapper.selectByIds(userIdsNeedQuery);
        List<FindUserByIdRspDTO> findUserByIdRspDTOS2 = Lists.newArrayList();
        if(CollUtil.isNotEmpty(userDOS)){
            findUserByIdRspDTOS2 = userDOS.stream()
                    .map(userDO -> FindUserByIdRspDTO.builder()
                            .id(userDO.getId())
                            .avatar(userDO.getAvatar())
                            .introduction(userDO.getIntroduction())
                            .nickName(userDO.getNickname())
                            .build())
                    .collect(Collectors.toList());
            //异步线程将信息同步到redis
            List<FindUserByIdRspDTO> finalFindUserByIdRspDTOS = findUserByIdRspDTOS2;
            threadPoolTaskExecutor.submit(() -> {
                Map<Long, FindUserByIdRspDTO> map = finalFindUserByIdRspDTOS.stream()
                        .collect(Collectors.toMap(FindUserByIdRspDTO::getId, p -> p));
                //执行pipeline操作（允许客户端在一次网络请求中发送多个命令）
                redisTemplate.executePipelined(new SessionCallback<>() {
                    @Override
                    public Object execute(RedisOperations operations) {
                        for (UserDO userDO : userDOS) {
                            Long userId = userDO.getId();
                            // 用户信息缓存 Redis Key
                            String userInfoRedisKey = RedisKeyConstants.buildUserInfoKey(userId);
                            // DTO 转 JSON 字符串
                            FindUserByIdRspDTO findUserInfoByIdRspDTO = map.get(userId);
                            String value = JsonUtils.toJsonString(findUserInfoByIdRspDTO);
                            // 过期时间（保底1天 + 随机秒数，将缓存过期时间打散，防止同一时间大量缓存失效，导致数据库压力太大）
                            long expireSeconds = 60*60*24 + RandomUtil.randomInt(60*60*24);
                            operations.opsForValue().set(userInfoRedisKey, value, expireSeconds, TimeUnit.SECONDS);
                        }
                        return null;
                    }
                });
            });
        }
        if(CollUtil.isNotEmpty(findUserByIdRspDTOS2)){
            findUserByIdRspDTOS.addAll(findUserByIdRspDTOS2);
        }
        return Response.success(findUserByIdRspDTOS);
    }

}
