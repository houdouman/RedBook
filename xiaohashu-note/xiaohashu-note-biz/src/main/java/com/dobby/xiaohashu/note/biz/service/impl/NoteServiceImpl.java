package com.dobby.xiaohashu.note.biz.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import com.dobby.framework.biz.context.filter.LoginUserContextHolder;
import com.dobby.framework.common.exception.BizException;
import com.dobby.framework.common.response.Response;
import com.dobby.framework.common.util.DateUtils;
import com.dobby.framework.common.util.JsonUtils;
import com.dobby.xiaohashu.note.biz.constant.MQConstants;
import com.dobby.xiaohashu.note.biz.constant.RedisKeyConstants;
import com.dobby.xiaohashu.note.biz.domain.dataobject.NoteCollectionDO;
import com.dobby.xiaohashu.note.biz.domain.dataobject.NoteDO;
import com.dobby.xiaohashu.note.biz.domain.dataobject.NoteLikeDO;
import com.dobby.xiaohashu.note.biz.domain.mapper.NoteCollectionDOMapper;
import com.dobby.xiaohashu.note.biz.domain.mapper.NoteDOMapper;
import com.dobby.xiaohashu.note.biz.domain.mapper.NoteLikeDOMapper;
import com.dobby.xiaohashu.note.biz.domain.mapper.TopicDOMapper;
import com.dobby.xiaohashu.note.biz.enums.*;
import com.dobby.xiaohashu.note.biz.mode.dto.CollectUnCollectNoteMqDTO;
import com.dobby.xiaohashu.note.biz.mode.dto.LikeUnlikeNoteMqDTO;
import com.dobby.xiaohashu.note.biz.mode.dto.NoteOperateMqDTO;
import com.dobby.xiaohashu.note.biz.mode.vo.*;
import com.dobby.xiaohashu.note.biz.rpc.DistributedIdGeneratorRpcService;
import com.dobby.xiaohashu.note.biz.rpc.KeyValueRpcService;
import com.dobby.xiaohashu.note.biz.rpc.UserRpcService;
import com.dobby.xiaohashu.note.biz.service.NoteService;
import com.dobby.xiaohashu.user.dto.rsp.FindUserByIdRspDTO;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author 橡皮鸭
 * @version 1.0
 * @date 2026/4/11 18:57
 */
@Service
@Slf4j
public class NoteServiceImpl implements NoteService {
    @Resource
    private NoteDOMapper noteDOMapper;
    @Resource
    private TopicDOMapper topicDOMapper;
    @Resource
    private DistributedIdGeneratorRpcService distributedIdGeneratorRpcService;
    @Resource
    private KeyValueRpcService keyValueRpcService;
    @Resource
    private UserRpcService userRpcService;
    @Resource(name = "taskExecutor")
    private ThreadPoolTaskExecutor taskExecutor;
    @Resource
    private RedisTemplate<String, String> redisTemplate;
    @Resource
    private RocketMQTemplate rocketMQTemplate;
    @Resource
    private NoteLikeDOMapper noteLikeDOMapper;
    @Resource
    private NoteCollectionDOMapper noteCollectionDOMapper;

    /**
     * 笔记详情本地缓存
     */
    private static final Cache<Long, String> LOCAL_CACHE = Caffeine.newBuilder()
            .initialCapacity(10000) // 设置初始容量为 10000 个条目
            .maximumSize(10000) // 设置缓存的最大容量为 10000 个条目
            .expireAfterWrite(1, TimeUnit.HOURS) // 设置缓存条目在写入后 1 小时过期
            .build();

    /**
     * 发布笔记
     * @param publishNoteReqVO
     * @return
     */
    @Override
    public Response<?> publishNote(PublishNoteReqVO publishNoteReqVO) {
        Integer type = publishNoteReqVO.getType();
        // 获取对应类型的枚举
        NoteTypeEnum noteTypeEnum = NoteTypeEnum.valueOf(type);
        if(Objects.isNull(noteTypeEnum)){
            throw new BizException(ResponseCodeEnum.NOTE_TYPE_ERROR);
        }
        Integer visible = publishNoteReqVO.getVisible();
        if(Objects.isNull(visible) || !NoteVisibleEnum.isValid(visible)){
            throw new BizException(ResponseCodeEnum.NOTE_VISIBLE_NonVALID);
        }
        String imgUris = null;
        // 笔记内容是否为空，默认值为 true，即空
        Boolean isContentEmpty = true;
        String videoUri = null;
        switch (noteTypeEnum){
            case IMAGE_TEXT:
                List<String> imgUriList = publishNoteReqVO.getImgUris();
                // 校验图片是否为空
                Preconditions.checkArgument(CollUtil.isNotEmpty(imgUriList), "笔记图片不能为空");
                // 校验图片数量
                Preconditions.checkArgument(imgUriList.size() <= 8, "笔记图片不能多于 8 张");
                // 将图片链接拼接，以逗号分隔
                imgUris = StringUtils.join(imgUriList, ",");
                break;
            case VIDEO:
                videoUri = publishNoteReqVO.getVideoUri();
                //校验视频是否为空
                Preconditions.checkArgument(StringUtils.isNotBlank(videoUri), "视频内容不能为空");
                break;
            default:
                break;
        }
        //生成笔记ID（使用分布式ID服务）
        String snowflakeId = distributedIdGeneratorRpcService.getSnowflakeId();
        String contentUuid = null;
        String content = publishNoteReqVO.getContent();
        //若笔记内容不为空
        if(StringUtils.isNotBlank(content)){
            isContentEmpty = false;
            contentUuid = UUID.randomUUID().toString();
            //调用KV键值服务，存储短文本
            boolean isSavedSuccess = keyValueRpcService.saveNoteContent(contentUuid, content);
            if(!isSavedSuccess){
                throw new BizException(ResponseCodeEnum.NOTE_PUBLISH_FAIL);
            }
        }
        Long topicId = publishNoteReqVO.getTopicId();
        String topicName = null;
        //话题非空时
        if(Objects.nonNull(topicId)){
            topicName = topicDOMapper.selectNameByPrimaryKey(topicId);
        }
        //获取当前用户id
        Long creatorId = LoginUserContextHolder.getUserId();
        //构建笔记对象
        NoteDO noteDO = NoteDO.builder()
                .id(Long.valueOf(snowflakeId))
                .isContentEmpty(isContentEmpty)
                .creatorId(creatorId)
                .contentUuid(contentUuid)
                .title(publishNoteReqVO.getTitle())
                .topicId(topicId)
                .topicName(topicName)
                .imgUris(imgUris)
                .videoUri(videoUri)
                .isTop(Boolean.FALSE)
                .type(type)
                .visible(visible)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .status(NoteStatusEnum.NORMAL.getCode())
                .build();

        try {
            noteDOMapper.insert(noteDO);
        } catch (Exception e) {
            log.warn("==> 笔记存储失败", e);
            //对应删除笔记内容
            if(StringUtils.isNotBlank(contentUuid)){
                keyValueRpcService.deleteNoteContent(contentUuid);
            }
        }
        // 发送 MQ
        NoteOperateMqDTO noteOperateMqDTO = NoteOperateMqDTO.builder()
                .creatorId(creatorId)
                .noteId(Long.valueOf(snowflakeId))
                .type(NoteOperateEnum.PUBLISH.getCode()) // 发布笔记
                .build();
        // 构建消息对象，并将 DTO 转成 Json 字符串设置到消息体中
        Message<String> message = MessageBuilder.withPayload(JsonUtils.toJsonString(noteOperateMqDTO))
                .build();

        // 通过冒号连接, 可让 MQ 发送给主题 Topic 时，携带上标签 Tag
        String destination = MQConstants.TOPIC_NOTE_OPERATE + ":" + MQConstants.TAG_NOTE_PUBLISH;

        // 异步发送 MQ 消息，提升接口响应速度
        rocketMQTemplate.asyncSend(destination, message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【笔记发布】MQ 发送成功，SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("==> 【笔记发布】MQ 发送异常: ", throwable);
            }
        });


        return Response.success("笔记存储成功");
    }

    /**
     * 查找笔记
     * @param findNoteDetailReqVO
     * @return
     */
    @SneakyThrows
    @Override
    public Response<FindNoteDetailRspVO> findNoteDetail(FindNoteDetailReqVO findNoteDetailReqVO) {
        Long noteId = findNoteDetailReqVO.getId();
        //从本地缓存查找
        String findNoteDetailRspVOStrLocalCache = LOCAL_CACHE.getIfPresent(noteId);
        if(StringUtils.isNotBlank(findNoteDetailRspVOStrLocalCache)){
            FindNoteDetailRspVO findNoteDetailRspVO = JsonUtils.parseObject(findNoteDetailRspVOStrLocalCache, FindNoteDetailRspVO.class);
            log.info("==> 命中了本地缓存；{}", findNoteDetailRspVOStrLocalCache);
            // 可见性校验
            checkNoteVisibleFromVO(LoginUserContextHolder.getUserId(), findNoteDetailRspVO);
            return Response.success(findNoteDetailRspVO);
        }
        //从redis缓存查找
        String noteDetailRedisKey = RedisKeyConstants.buildNoteDetailKey(noteId);
        String findNoteDetailRspVOStrRedis = redisTemplate.opsForValue().get(noteDetailRedisKey);
        if(StringUtils.isNotBlank(findNoteDetailRspVOStrRedis)){
            FindNoteDetailRspVO findNoteDetailRspVO = JsonUtils.parseObject(findNoteDetailRspVOStrRedis, FindNoteDetailRspVO.class);
            //存入本地缓存
            taskExecutor.submit(() -> {
                LOCAL_CACHE.put(noteId, Objects.isNull(findNoteDetailRspVO) ? "null" : JsonUtils.toJsonString(findNoteDetailRspVO));
            });
            // 可见性校验
            checkNoteVisibleFromVO(LoginUserContextHolder.getUserId(), findNoteDetailRspVO);
            return Response.success(findNoteDetailRspVO);
        }
        //从数据库获取
        NoteDO noteDO = noteDOMapper.selectByPrimaryKey(noteId);
        if(Objects.isNull(noteDO)){
            taskExecutor.execute(() -> {
                // 防止缓存穿透，将空数据存入 Redis 缓存 (过期时间不宜设置过长)
                // 保底1分钟 + 随机秒数
                long expireSeconds = 60 + RandomUtil.randomInt(60);
                redisTemplate.opsForValue().set(noteDetailRedisKey, "null", expireSeconds, TimeUnit.SECONDS);
            });
            throw new BizException(ResponseCodeEnum.NOTE_NOT_FOUND);
        }
        //可见性检验
        Integer visible = noteDO.getVisible();
        checkNoteVisible(visible, LoginUserContextHolder.getUserId(), noteDO.getCreatorId());
        //并发查询优化：将 调用用户服务 与 调用KV服务 通过CompletableFuture设置为异步实现
        //获取发布者信息
        Long creatorId = noteDO.getCreatorId();
        CompletableFuture<FindUserByIdRspDTO> userResultFuture = CompletableFuture
                .supplyAsync(() -> userRpcService.findById(creatorId), taskExecutor);

        //获取笔记内容
        CompletableFuture<String> contentResultFuture = CompletableFuture.completedFuture(null);
        if (Objects.equals(noteDO.getIsContentEmpty(), Boolean.FALSE)) {
            contentResultFuture = CompletableFuture
                    .supplyAsync(() -> keyValueRpcService.findNoteContent(noteDO.getContentUuid()), taskExecutor);
        }
        CompletableFuture<String> finalContentResultFuture = contentResultFuture;

        CompletableFuture<FindNoteDetailRspVO> resultFuture = CompletableFuture
                .allOf(userResultFuture, contentResultFuture)
                .thenApply(s ->{
                    FindUserByIdRspDTO findUserByIdRspDTO = userResultFuture.join();
                    String content = finalContentResultFuture.join();
                    // 笔记类型
                    Integer noteType = noteDO.getType();
                    // 图文笔记图片链接(字符串)
                    String imgUrisStr = noteDO.getImgUris();
                    // 图文笔记图片链接(集合)
                    List<String> imgUris = null;
                    // 如果查询的是图文笔记，需要将图片链接的逗号分隔开，转换成集合
                    if (Objects.equals(noteType, NoteTypeEnum.IMAGE_TEXT.getCode())
                            && StringUtils.isNotBlank(imgUrisStr)) {
                        imgUris = List.of(imgUrisStr.split(","));
                    }

                    // 构建返参 VO 实体类
                    return FindNoteDetailRspVO.builder()
                            .id(noteDO.getId())
                            .type(noteDO.getType())
                            .title(noteDO.getTitle())
                            .content(content)
                            .imgUris(imgUris)
                            .topicId(noteDO.getTopicId())
                            .topicName(noteDO.getTopicName())
                            .creatorId(noteDO.getCreatorId())
                            .creatorName(findUserByIdRspDTO.getNickName())
                            .avatar(findUserByIdRspDTO.getAvatar())
                            .videoUri(noteDO.getVideoUri())
                            .updateTime(noteDO.getUpdateTime())
                            .visible(noteDO.getVisible())
                            .build();
                });
        FindNoteDetailRspVO findNoteDetailRspVO = resultFuture.get();
        //存入redis缓存
        taskExecutor.submit(() -> {
            String noteDetailJson1 = JsonUtils.toJsonString(findNoteDetailRspVO);
            // 过期时间（保底1天 + 随机秒数，将缓存过期时间打散，防止同一时间大量缓存失效，导致数据库压力太大）
            long expireSeconds = 60*60*24 + RandomUtil.randomInt(60*60*24);
            redisTemplate.opsForValue().set(noteDetailRedisKey, noteDetailJson1, expireSeconds, TimeUnit.SECONDS);
        });
        return Response.success(findNoteDetailRspVO);
    }

    /**
     * 笔记更新
     * @param updateNoteReqVO
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<?> updateNote(UpdateNoteReqVO updateNoteReqVO) {
        // 笔记 ID
        Long noteId = updateNoteReqVO.getId();
        // 笔记类型
        Integer type = updateNoteReqVO.getType();
        // 获取对应类型的枚举
        NoteTypeEnum noteTypeEnum = NoteTypeEnum.valueOf(type);
        // 获取可见类型
        Integer visible = updateNoteReqVO.getVisible();
        // 若非图文、视频，抛出业务业务异常
        if (Objects.isNull(noteTypeEnum)) {
            throw new BizException(ResponseCodeEnum.NOTE_TYPE_ERROR);
        }
        // 若可见性设置不合法，抛出业务异常
        if(Objects.isNull(visible) || !NoteVisibleEnum.isValid(visible)){
            throw new BizException(ResponseCodeEnum.NOTE_VISIBLE_NonVALID);
        }

        String imgUris = null;
        String videoUri = null;
        switch (noteTypeEnum) {
            case IMAGE_TEXT: // 图文笔记
                List<String> imgUriList = updateNoteReqVO.getImgUris();
                // 校验图片是否为空
                Preconditions.checkArgument(CollUtil.isNotEmpty(imgUriList), "笔记图片不能为空");
                // 校验图片数量
                Preconditions.checkArgument(imgUriList.size() <= 8, "笔记图片不能多于 8 张");

                imgUris = StringUtils.join(imgUriList, ",");
                break;
            case VIDEO: // 视频笔记
                videoUri = updateNoteReqVO.getVideoUri();
                // 校验视频链接是否为空
                Preconditions.checkArgument(StringUtils.isNotBlank(videoUri), "笔记视频不能为空");
                break;
            default:
                break;
        }
        //创建笔记的用户id是否匹配
        Long curruserId = LoginUserContextHolder.getUserId();
        NoteDO selectNoteDO = noteDOMapper.selectByPrimaryKey(noteId);
        //笔记不存在
        if(Objects.isNull(selectNoteDO)){
            throw new BizException(ResponseCodeEnum.NOTE_NOT_FOUND);
        }
        //用户id不匹配
        if(!Objects.equals(selectNoteDO.getCreatorId(), curruserId)){
            throw new BizException(ResponseCodeEnum.NOTE_CANT_OPERATE);
        }
        // 话题
        Long topicId = updateNoteReqVO.getTopicId();
        String topicName = null;
        if (Objects.nonNull(topicId)) {
            topicName = topicDOMapper.selectNameByPrimaryKey(topicId);
            // 判断一下提交的话题, 是否是真实存在的
            if (StringUtils.isBlank(topicName)) throw new BizException(ResponseCodeEnum.TOPIC_NOT_FOUND);
        }
        //延时双删策略
        // 删除 Redis 缓存
        String noteDetailRedisKey = RedisKeyConstants.buildNoteDetailKey(noteId);
        redisTemplate.delete(noteDetailRedisKey);
        //更新笔记元数据表t_note
        String content = updateNoteReqVO.getContent();
        NoteDO noteDO = NoteDO.builder()
                .id(noteId)
                .isContentEmpty(StringUtils.isBlank(content))
                .imgUris(imgUris)
                .title(updateNoteReqVO.getTitle())
                .topicId(updateNoteReqVO.getTopicId())
                .topicName(topicName)
                .type(type)
                .visible(visible)
                .updateTime(LocalDateTime.now())
                .videoUri(videoUri)
                .build();
        noteDOMapper.updateByPrimaryKey(noteDO);
        //删除redis缓存
        Message<String> message = MessageBuilder.withPayload(String.valueOf(noteId)).build();
        // 同步发送广播模式 MQ，将所有实例中的本地缓存都删除掉
        rocketMQTemplate.asyncSend(MQConstants.TOPIC_DELAY_DELETE_NOTE_REDIS_CACHE, message,
                new SendCallback() {
                    @Override
                    public void onSuccess(SendResult sendResult) {
                        log.info("## 延时删除 Redis 笔记缓存消息发送成功...");
                    }

                    @Override
                    public void onException(Throwable e) {
                        log.error("## 延时删除 Redis 笔记缓存消息发送失败...", e);
                    }
                },
                3000, // 超时时间(毫秒)
                1 // 延迟级别，1 表示延时 1s
        );
        // 同步发送广播模式 MQ，将所有实例中的本地缓存都删除掉
        rocketMQTemplate.syncSend(MQConstants.TOPIC_DELETE_NOTE_LOCAL_CACHE, noteId);
        log.info("====> MQ：删除笔记本地缓存发送成功...");
        // 笔记内容更新
        NoteDO noteDO1 = noteDOMapper.selectByPrimaryKey(noteId);
        String contentUuid = noteDO1.getContentUuid();
        // 笔记内容是否更新成功
        boolean isUpdateContentSuccess = false;
        if (StringUtils.isBlank(content)) {
            // 若笔记内容为空，则删除 K-V 存储
            isUpdateContentSuccess = keyValueRpcService.deleteNoteContent(contentUuid);
        } else {
            // 若将无内容的笔记，更新为了有内容的笔记，需要重新生成 UUID
            contentUuid = StringUtils.isBlank(contentUuid) ? UUID.randomUUID().toString() : contentUuid;
            // 调用 K-V 更新短文本
            isUpdateContentSuccess = keyValueRpcService.saveNoteContent(contentUuid, content);
        }
        // 如果更新失败，抛出业务异常，回滚事务
        if (!isUpdateContentSuccess) {
            throw new BizException(ResponseCodeEnum.NOTE_UPDATE_FAIL);
        }

        return Response.success();
    }


    /**
     * 逻辑删除笔记
     * @param deleteNoteReqVO
     * @return
     */
    @Override
    public Response<?> deleteNote(DeleteNoteReqVO deleteNoteReqVO) {
        Long noteId = deleteNoteReqVO.getId();

        //创建笔记的用户id是否匹配
        Long curruserId = LoginUserContextHolder.getUserId();
        NoteDO selectNoteDO = noteDOMapper.selectByPrimaryKey(noteId);
        //笔记不存在
        if(Objects.isNull(selectNoteDO)){
            throw new BizException(ResponseCodeEnum.NOTE_NOT_FOUND);
        }
        //用户id不匹配
        if(!Objects.equals(selectNoteDO.getCreatorId(), curruserId)){
            throw new BizException(ResponseCodeEnum.NOTE_CANT_OPERATE);
        }
        //逻辑删除
        NoteDO noteDO = NoteDO.builder()
                .id(noteId)
                .status(NoteStatusEnum.DELETED.getCode())
                .updateTime(LocalDateTime.now())
                .build();
        noteDOMapper.updateByPrimaryKeySelective(noteDO);
        //删除缓存
        String noteDetailRedisKey = RedisKeyConstants.buildNoteDetailKey(noteId);
        redisTemplate.delete(noteDetailRedisKey);
        //广播模式删除该数据所有本地缓存
        rocketMQTemplate.syncSend(MQConstants.TOPIC_DELETE_NOTE_LOCAL_CACHE, noteId);
        log.info("====> MQ：删除笔记本地缓存发送成功...");
        // 发送 MQ
        NoteOperateMqDTO noteOperateMqDTO = NoteOperateMqDTO.builder()
                .creatorId(selectNoteDO.getCreatorId())
                .noteId(noteId)
                .type(NoteOperateEnum.DELETE.getCode()) // 删除笔记
                .build();

        // 构建消息对象，并将 DTO 转成 Json 字符串设置到消息体中
        Message<String> message = MessageBuilder.withPayload(JsonUtils.toJsonString(noteOperateMqDTO))
                .build();

        // 通过冒号连接, 可让 MQ 发送给主题 Topic 时，携带上标签 Tag
        String destination = MQConstants.TOPIC_NOTE_OPERATE + ":" + MQConstants.TAG_NOTE_DELETE;

        // 异步发送 MQ 消息，提升接口响应速度
        rocketMQTemplate.asyncSend(destination, message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【笔记删除】MQ 发送成功，SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("==> 【笔记删除】MQ 发送异常: ", throwable);
            }
        });

        return Response.success();
    }

    /**
     * 设置笔记仅自己可见
     * @param updateNoteVisibleOnlyMeReqVO
     * @return
     */
    @Override
    public Response<?> visibleOnlyMe(UpdateNoteVisibleOnlyMeReqVO updateNoteVisibleOnlyMeReqVO) {
        Long noteId = updateNoteVisibleOnlyMeReqVO.getId();
        NoteDO noteDO = NoteDO.builder()
                .id(noteId)
                .visible(NoteVisibleEnum.PRIVATE.getCode())
                .updateTime(LocalDateTime.now())
                .build();
        int count = noteDOMapper.updateVisibleOnlyMe(noteDO);
        if (count == 0) {
            throw new BizException(ResponseCodeEnum.NOTE_CANT_VISIBLE_ONLY_ME);
        }
        // 删除 Redis 缓存
        String noteDetailRedisKey = RedisKeyConstants.buildNoteDetailKey(noteId);
        redisTemplate.delete(noteDetailRedisKey);
        // 同步发送广播模式 MQ，将所有实例中的本地缓存都删除掉
        rocketMQTemplate.syncSend(MQConstants.TOPIC_DELETE_NOTE_LOCAL_CACHE, noteId);
        log.info("====> MQ：删除笔记本地缓存发送成功...");

        return Response.success();
    }


    /**
     * 删除本地笔记缓存
     * @param noteId
     */
    @Override
    public void deleteNoteLocalCache(Long noteId) {
        LOCAL_CACHE.invalidate(noteId);
    }


    /**
     * 修改笔记置顶状态
     * @param topNoteReqVO
     * @return
     */
    @Override
    public Response<?> topNote(TopNoteReqVO topNoteReqVO) {
        Long noteId = topNoteReqVO.getId();
        Boolean isTop = topNoteReqVO.getIsTop();
        Long currUsrId = LoginUserContextHolder.getUserId();
        NoteDO noteDO = NoteDO.builder()
                .id(noteId)
                .isTop(isTop)
                .updateTime(LocalDateTime.now())
                .creatorId(currUsrId)
                .build();
        int count = noteDOMapper.updateIsTop(noteDO);
        if (count == 0) {
            throw new BizException(ResponseCodeEnum.NOTE_CANT_OPERATE);
        }
        // 删除 Redis 缓存
        String noteDetailRedisKey = RedisKeyConstants.buildNoteDetailKey(noteId);
        redisTemplate.delete(noteDetailRedisKey);
        // 同步发送广播模式 MQ，将所有实例中的本地缓存都删除掉
        rocketMQTemplate.syncSend(MQConstants.TOPIC_DELETE_NOTE_LOCAL_CACHE, noteId);
        log.info("====> MQ：删除笔记本地缓存发送成功...");

        return Response.success();
    }

    /**
     * 用户点赞笔记
     * @param likeNoteReqVO
     * @return
     */
    @Override
    public Response<?> likeNote(LikeNoteReqVO likeNoteReqVO) {
        //当前用户id
        Long userId = LoginUserContextHolder.getUserId();
        Long noteId = likeNoteReqVO.getId();
        //校验被点赞的笔记是否存在
        Long creatorId = checkNoteIsExist(noteId);
        //-----在redis查找是否已点赞-----
        String bloomUserNoteLikeListKey = RedisKeyConstants.buildBloomUserNoteLikeListKey(userId);
        // 用户点赞列表 ZSet Key
        String userNoteLikeZSetKey = RedisKeyConstants.buildUserNoteLikeZSetKey(userId);
        //执行lua脚本
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/bloom_note_like_check.lua")));
        script.setResultType(Long.class);
        Long result = redisTemplate.execute(script, Collections.singletonList(bloomUserNoteLikeListKey), noteId);
        NoteLikeLuaResultEnum noteLikeLuaResultEnum = NoteLikeLuaResultEnum.valueOf(result);
        switch (noteLikeLuaResultEnum) {
            //-----redis中布隆过滤器不存在-----
            case NOT_EXIST -> {
                //从数据库中校验笔记是否被点赞，并异步初始化布隆过滤器，设置过期时间
                int count = noteLikeDOMapper.selectCountByUserIdAndNoteId(userId, noteId);
                long expireSeconds = 60*60*24 + RandomUtil.randomInt(60*60*24);
                if (count > 0) {
                    //已经点过赞，异步初始化布隆过滤器并将数据库中用户点赞信息存入redis
                    taskExecutor.submit(() -> {
                        batchAddNoteLike2BloomAndExpire(userId, expireSeconds, bloomUserNoteLikeListKey);
                    });
                    throw new BizException(ResponseCodeEnum.NOTE_ALREADY_LIKED);
                }
                //将数据库信息与当前笔记点赞信息添加到redis
                batchAddNoteLike2BloomAndExpire(userId, expireSeconds, bloomUserNoteLikeListKey);
                script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/bloom_add_note_like_and_expire.lua")));
                //返回值类型
                script.setResultType(Long.class);
                redisTemplate.execute(script, Collections.singletonList(bloomUserNoteLikeListKey), noteId, expireSeconds);
            }
            //-----redis查找结果为已点赞-----
            case NOTE_LIKED -> {
                //查询用户zset点赞列表
                Double score = redisTemplate.opsForZSet().score(userNoteLikeZSetKey, noteId);
                if(Objects.nonNull(score)){
                    throw new BizException(ResponseCodeEnum.NOTE_ALREADY_LIKED);
                }
                //从数据库查找是否已点赞
                int count = noteLikeDOMapper.selectNoteIsLiked(userId, noteId);
                if(count > 0){
                    // 数据库里面有点赞记录，而 Redis 中 ZSet 不存在，需要重新异步初始化 ZSet
                    asynInitUserNoteLikesZSet(userId, userNoteLikeZSetKey);
                    throw new BizException(ResponseCodeEnum.NOTE_ALREADY_LIKED);
                }
            }
        }
        //-----更新用户zset点赞列表（zset点赞列表为了解决布隆过滤器对已点赞的误判）-----
        LocalDateTime now = LocalDateTime.now();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/note_like_check_and_update_zset.lua")));
        script.setResultType(Long.class);
        result = redisTemplate.execute(script, Collections.singletonList(userNoteLikeZSetKey), noteId, DateUtils.localDateTime2Timestamp(now));
        //若ZSet列表不存在，需要重新初始化
        if (Objects.equals(result, NoteLikeLuaResultEnum.NOT_EXIST.getCode())) {
            // 查询当前用户最新点赞的 100 篇笔记
            List<NoteLikeDO> noteLikeDOS = noteLikeDOMapper.selectLikedByUserIdAndLimit(userId, 100);

            if (CollUtil.isNotEmpty(noteLikeDOS)) {
                // 保底1天+随机秒数
                long expireSeconds = 60*60*24 + RandomUtil.randomInt(60*60*24);
                // 构建 Lua 参数
                Object[] luaArgs = buildNoteLikeZSetLuaArgs(noteLikeDOS, expireSeconds);

                DefaultRedisScript<Long> script2 = new DefaultRedisScript<>();
                // Lua 脚本路径
                script2.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/batch_add_note_like_zset_and_expire.lua")));
                // 返回值类型
                script2.setResultType(Long.class);

                redisTemplate.execute(script2, Collections.singletonList(userNoteLikeZSetKey), luaArgs);

                // 再次调用 note_like_check_and_update_zset.lua 脚本，将点赞的笔记添加到 zset 中
                redisTemplate.execute(script, Collections.singletonList(userNoteLikeZSetKey), noteId, DateUtils.localDateTime2Timestamp(now));
            }
        }
        //-----发送MQ消息体，将点赞存入数据库-----
        LikeUnlikeNoteMqDTO likeUnlikeNoteMqDTO = LikeUnlikeNoteMqDTO.builder()
                .noteId(noteId)
                .type(LikeUnlikeNoteTypeEnum.LIKE.getCode())
                .userId(userId)
                .createTime(now)
                .noteCreatorId(creatorId)
                .build();
        Message<String> message = MessageBuilder.withPayload(JsonUtils.toJsonString(likeUnlikeNoteMqDTO)).build();
        String destination = MQConstants.TOPIC_LIKE_OR_UNLIKE + ":" + MQConstants.TAG_LIKE;
        String hashKey = String.valueOf(userId);
        rocketMQTemplate.asyncSendOrderly(destination, message, hashKey, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【笔记点赞】MQ 发送成功，SendResult: {}", sendResult);
            }
            @Override
            public void onException(Throwable throwable) {
                log.error("==> 【笔记点赞】MQ 发送异常: ", throwable);
            }
        });
        return Response.success();
    }

    /**
     * 取消点赞笔记
     * @param unlikeNoteReqVO
     * @return
     */
    @Override
    public Response<?> unlikeNote(UnlikeNoteReqVO unlikeNoteReqVO) {
        Long noteId = unlikeNoteReqVO.getId();
        //检验笔记是否真实存在
        Long creatorId = checkNoteIsExist(noteId);
        //获取当前用户id
        Long userId = LoginUserContextHolder.getUserId();
        //-----去redis确认是否已点赞-----
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/bloom_note_unlike_check.lua")));
        redisScript.setResultType(Long.class);
        String bloomUserNoteLikeListKey = RedisKeyConstants.buildBloomUserNoteLikeListKey(userId);
        Long result = redisTemplate.execute(redisScript, Collections.singletonList(bloomUserNoteLikeListKey), noteId);
        NoteUnlikeLuaResultEnum noteUnlikeLuaResultEnum = NoteUnlikeLuaResultEnum.valueOf(result);
        switch (noteUnlikeLuaResultEnum) {
            //-----布隆过滤器不存在-----
            case NOT_EXIST -> {
                //异步初始化布隆过滤器，从数据库检验笔记是否点赞
                int count = noteLikeDOMapper.selectNoteIsLiked(userId, noteId);
                long expireSeconds = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);
                taskExecutor.submit(() -> {
                    batchAddNoteLike2BloomAndExpire(userId, expireSeconds, bloomUserNoteLikeListKey);
                });
                if (count == 0) {
                    throw new BizException(ResponseCodeEnum.NOTE_NOT_LIKED);
                }
            }
            case NOTE_NOT_LIKED -> throw new BizException(ResponseCodeEnum.NOTE_NOT_LIKED);
        }
        //从zset查找并删除（允许误判，因为zset没有如果从数据库查找是否点赞影响性能且不必要）
        String userNoteLikeZSetKey = RedisKeyConstants.buildUserNoteLikeZSetKey(userId);
        redisTemplate.opsForZSet().remove(userNoteLikeZSetKey, noteId);
        //-----发送MQ消息体，数据更新落库-----
        LikeUnlikeNoteMqDTO likeUnlikeNoteMqDTO = LikeUnlikeNoteMqDTO.builder()
                .noteId(noteId)
                .type(LikeUnlikeNoteTypeEnum.UNLIKE.getCode())
                .userId(userId)
                .createTime(LocalDateTime.now())
                .noteCreatorId(creatorId)
                .build();
        Message<String> message = MessageBuilder.withPayload(JsonUtils.toJsonString(likeUnlikeNoteMqDTO)).build();
        String destination = MQConstants.TOPIC_LIKE_OR_UNLIKE + ":" + MQConstants.TAG_UNLIKE;
        String hashKey = String.valueOf(userId);
        rocketMQTemplate.asyncSendOrderly(destination, message, hashKey, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【笔记取消点赞】MQ 发送成功，SendResult: {}", sendResult);
            }
            @Override
            public void onException(Throwable throwable) {
                log.error("==> 【笔记取消点赞】MQ 发送异常: ", throwable);
            }
        });
        return Response.success();
    }

    /**
     * 收藏笔记
     * @param collectNoteReqVO
     * @return
     */
    @Override
    public Response<?> collectNote(CollectNoteReqVO collectNoteReqVO) {
        // 笔记ID
        Long noteId = collectNoteReqVO.getId();
        Long creatorId = checkNoteIsExist(noteId);
        //判断目标笔记，是否已经收藏过
        //当前登录用户ID
        Long userId = LoginUserContextHolder.getUserId();

        //布隆过滤器 Key
        String bloomUserNoteCollectListKey = RedisKeyConstants.buildBloomUserNoteCollectListKey(userId);

        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/bloom_note_collect_check.lua")));
        script.setResultType(Long.class);
        Long result = redisTemplate.execute(script, Collections.singletonList(bloomUserNoteCollectListKey), noteId);

        //用户收藏列表 ZSet Key
        String userNoteCollectZSetKey = RedisKeyConstants.buildUserNoteCollectZSetKey(userId);
        NoteCollectLuaResultEnum noteCollectLuaResultEnum = NoteCollectLuaResultEnum.valueOf(result);
        switch (noteCollectLuaResultEnum) {
            // Redis 中布隆过滤器不存在
            case NOT_EXIST -> {
                // 从数据库中校验笔记是否被收藏，并异步初始化布隆过滤器，设置过期时间
                int count = noteCollectionDOMapper.selectCountByUserIdAndNoteId(userId, noteId);
                // 保底1天+随机秒数
                long expireSeconds = 60*60*24 + RandomUtil.randomInt(60*60*24);
                // 目标笔记已经被收藏
                if (count > 0) {
                    // 异步初始化布隆过滤器
                    taskExecutor.submit(() ->
                            batchAddNoteCollect2BloomAndExpire(userId, expireSeconds, bloomUserNoteCollectListKey));
                    throw new BizException(ResponseCodeEnum.NOTE_ALREADY_COLLECTED);
                }
                // 若目标笔记未被收藏，查询当前用户是否有收藏其他笔记，有则同步初始化布隆过滤器
                batchAddNoteCollect2BloomAndExpire(userId, expireSeconds, bloomUserNoteCollectListKey);
                // 添加当前收藏笔记 ID 到布隆过滤器中
                // Lua 脚本路径
                script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/bloom_add_note_collect_and_expire.lua")));
                // 返回值类型
                script.setResultType(Long.class);
                redisTemplate.execute(script, Collections.singletonList(bloomUserNoteCollectListKey), noteId, expireSeconds);
            }
            // 目标笔记已经被收藏 (可能存在误判，需要进一步确认)
            case NOTE_COLLECTED -> {
                // 校验 ZSet 列表中是否包含被收藏的笔记ID
                Double score = redisTemplate.opsForZSet().score(userNoteCollectZSetKey, noteId);

                if (Objects.nonNull(score)) {
                    throw new BizException(ResponseCodeEnum.NOTE_ALREADY_COLLECTED);
                }

                // 若 Score 为空，则表示 ZSet 收藏列表中不存在，查询数据库校验
                int count = noteCollectionDOMapper.selectNoteIsCollected(userId, noteId);

                if (count > 0) {
                    // 数据库里面有收藏记录，而 Redis 中 ZSet 未初始化，需要重新异步初始化 ZSet
                    asynInitUserNoteCollectsZSet(userId, userNoteCollectZSetKey);

                    throw new BizException(ResponseCodeEnum.NOTE_ALREADY_COLLECTED);
                }
            }
        }

        // 3. 更新用户 ZSET 收藏列表
        LocalDateTime now = LocalDateTime.now();
        // Lua 脚本路径
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/note_collect_check_and_update_zset.lua")));
        // 返回值类型
        script.setResultType(Long.class);

        // 执行 Lua 脚本，拿到返回结果
        result = redisTemplate.execute(script, Collections.singletonList(userNoteCollectZSetKey), noteId, DateUtils.localDateTime2Timestamp(now));

        // 若 ZSet 列表不存在，需要重新初始化
        if (Objects.equals(result, NoteCollectLuaResultEnum.NOT_EXIST.getCode())) {
            // 查询当前用户最新收藏的 300 篇笔记
            List<NoteCollectionDO> noteCollectionDOS = noteCollectionDOMapper.selectCollectdByUserIdAndLimit(userId, 300);

            if (CollUtil.isNotEmpty(noteCollectionDOS)) {
                // 保底1天+随机秒数
                long expireSeconds = 60*60*24 + RandomUtil.randomInt(60*60*24);
                // 构建 Lua 参数
                Object[] luaArgs = buildNoteCollectZSetLuaArgs(noteCollectionDOS, expireSeconds);

                DefaultRedisScript<Long> script2 = new DefaultRedisScript<>();
                // Lua 脚本路径
                script2.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/batch_add_note_collect_zset_and_expire.lua")));
                // 返回值类型
                script2.setResultType(Long.class);

                redisTemplate.execute(script2, Collections.singletonList(userNoteCollectZSetKey), luaArgs);

                // 再次调用 note_collect_check_and_update_zset.lua 脚本，将当前收藏的笔记添加到 zset 中
                redisTemplate.execute(script, Collections.singletonList(userNoteCollectZSetKey), noteId, DateUtils.localDateTime2Timestamp(now));
            }
        }

        // 4. 发送 MQ, 将收藏数据落库
        // 构建消息体 DTO
        CollectUnCollectNoteMqDTO collectUnCollectNoteMqDTO = CollectUnCollectNoteMqDTO.builder()
                .userId(userId)
                .noteId(noteId)
                .type(CollectUnCollectNoteTypeEnum.COLLECT.getCode()) // 收藏笔记
                .createTime(now)
                .noteCreatorId(creatorId)
                .build();

        // 构建消息对象，并将 DTO 转成 Json 字符串设置到消息体中
        Message<String> message = MessageBuilder.withPayload(JsonUtils.toJsonString(collectUnCollectNoteMqDTO))
                .build();

        // 通过冒号连接, 可让 MQ 发送给主题 Topic 时，携带上标签 Tag
        String destination = MQConstants.TOPIC_COLLECT_OR_UNCOLLECT + ":" + MQConstants.TAG_COLLECT;

        String hashKey = String.valueOf(userId);

        // 异步发送顺序 MQ 消息，提升接口响应速度
        rocketMQTemplate.asyncSendOrderly(destination, message, hashKey, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【笔记收藏】MQ 发送成功，SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("==> 【笔记收藏】MQ 发送异常: ", throwable);
            }
        });

        return Response.success();
    }

    /**
     * 取消收藏笔记
     * @param unCollectNoteReqVO
     * @return
     */
    @Override
    public Response<?> unCollectNote(UnCollectNoteReqVO unCollectNoteReqVO) {
        // 笔记ID
        Long noteId = unCollectNoteReqVO.getId();
        // 1. 校验笔记是否真实存在
        Long creatorId = checkNoteIsExist(noteId);
        // 2. 校验笔记是否被收藏过
        // 当前登录用户ID
        Long userId = LoginUserContextHolder.getUserId();

        // 布隆过滤器 Key
        String bloomUserNoteCollectListKey = RedisKeyConstants.buildBloomUserNoteCollectListKey(userId);

        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        // Lua 脚本路径
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/bloom_note_uncollect_check.lua")));
        // 返回值类型
        script.setResultType(Long.class);

        // 执行 Lua 脚本，拿到返回结果
        Long result = redisTemplate.execute(script, Collections.singletonList(bloomUserNoteCollectListKey), noteId);

        NoteUncollectLuaResultEnum noteUnCollectLuaResultEnum = NoteUncollectLuaResultEnum.valueOf(result);

        switch (noteUnCollectLuaResultEnum) {
            // 布隆过滤器不存在
            case NOT_EXIST -> {
                // 异步初始化布隆过滤器
                taskExecutor.submit(() -> {
                    // 保底1天+随机秒数
                    long expireSeconds = 60*60*24 + RandomUtil.randomInt(60*60*24);
                    batchAddNoteCollect2BloomAndExpire(userId, expireSeconds, bloomUserNoteCollectListKey);
                });

                // 从数据库中校验笔记是否被收藏
                int count = noteCollectionDOMapper.selectCountByUserIdAndNoteId(userId, noteId);

                // 未收藏，无法取消收藏操作，抛出业务异常
                if (count == 0) throw new BizException(ResponseCodeEnum.NOTE_NOT_COLLECTED);
            }
            // 布隆过滤器校验目标笔记未被收藏（判断绝对正确）
            case NOTE_NOT_COLLECTED -> throw new BizException(ResponseCodeEnum.NOTE_NOT_COLLECTED);
        }

        // 3. 删除 ZSET 中已收藏的笔记 ID
        // 能走到这里，说明布隆过滤器判断已收藏，直接删除 ZSET 中已收藏的笔记 ID
        // 用户收藏列表 ZSet Key
        String userNoteCollectZSetKey = RedisKeyConstants.buildUserNoteCollectZSetKey(userId);

        redisTemplate.opsForZSet().remove(userNoteCollectZSetKey, noteId);

        // 4. 发送 MQ, 数据更新落库
        // 构建消息体 DTO
        CollectUnCollectNoteMqDTO unCollectNoteMqDTO = CollectUnCollectNoteMqDTO.builder()
                .userId(userId)
                .noteId(noteId)
                .type(CollectUnCollectNoteTypeEnum.UNCOLLECT.getCode()) // 取消收藏笔记
                .createTime(LocalDateTime.now())
                .noteCreatorId(creatorId)
                .build();

        // 构建消息对象，并将 DTO 转成 Json 字符串设置到消息体中
        Message<String> message = MessageBuilder.withPayload(JsonUtils.toJsonString(unCollectNoteMqDTO))
                .build();

        // 通过冒号连接, 可让 MQ 发送给主题 Topic 时，携带上标签 Tag
        String destination = MQConstants.TOPIC_COLLECT_OR_UNCOLLECT + ":" + MQConstants.TAG_UNCOLLECT;

        String hashKey = String.valueOf(userId);

        // 异步发送顺序 MQ 消息，提升接口响应速度
        rocketMQTemplate.asyncSendOrderly(destination, message, hashKey, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【笔记取消收藏】MQ 发送成功，SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("==> 【笔记取消收藏】MQ 发送异常: ", throwable);
            }
        });

        return Response.success();
    }

    /**
     * 异步初始化用户收藏笔记 ZSet
     * @param userId
     * @param userNoteCollectZSetKey
     */
    private void asynInitUserNoteCollectsZSet(Long userId, String userNoteCollectZSetKey) {
        taskExecutor.execute(() -> {
            // 判断用户笔记收藏 ZSET 是否存在
            boolean hasKey = redisTemplate.hasKey(userNoteCollectZSetKey);

            // 不存在，则重新初始化
            if (!hasKey) {
                // 查询当前用户最新收藏的 300 篇笔记
                List<NoteCollectionDO> noteCollectionDOS = noteCollectionDOMapper.selectCollectdByUserIdAndLimit(userId, 300);
                if (CollUtil.isNotEmpty(noteCollectionDOS)) {
                    // 保底1天+随机秒数
                    long expireSeconds = 60*60*24 + RandomUtil.randomInt(60*60*24);
                    // 构建 Lua 参数
                    Object[] luaArgs = buildNoteCollectZSetLuaArgs(noteCollectionDOS, expireSeconds);

                    DefaultRedisScript<Long> script2 = new DefaultRedisScript<>();
                    // Lua 脚本路径
                    script2.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/batch_add_note_collect_zset_and_expire.lua")));
                    // 返回值类型
                    script2.setResultType(Long.class);

                    redisTemplate.execute(script2, Collections.singletonList(userNoteCollectZSetKey), luaArgs);
                }
            }
        });
    }

    /**
     * 异步初始化用户点赞笔记 ZSet
     * @param userId
     * @param userNoteLikeZSetKey
     */
    private void asynInitUserNoteLikesZSet(Long userId, String userNoteLikeZSetKey) {
        taskExecutor.execute(() -> {
            // 判断用户笔记点赞 ZSET 是否存在
            boolean hasKey = redisTemplate.hasKey(userNoteLikeZSetKey);

            // 不存在，则重新初始化
            if (!hasKey) {
                // 查询当前用户最新点赞的 100 篇笔记
                List<NoteLikeDO> noteLikeDOS = noteLikeDOMapper.selectLikedByUserIdAndLimit(userId, 100);
                if (CollUtil.isNotEmpty(noteLikeDOS)) {
                    // 保底1天+随机秒数
                    long expireSeconds = 60*60*24 + RandomUtil.randomInt(60*60*24);
                    // 构建 Lua 参数
                    Object[] luaArgs = buildNoteLikeZSetLuaArgs(noteLikeDOS, expireSeconds);

                    DefaultRedisScript<Long> script2 = new DefaultRedisScript<>();
                    // Lua 脚本路径
                    script2.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/batch_add_note_like_zset_and_expire.lua")));
                    // 返回值类型
                    script2.setResultType(Long.class);

                    redisTemplate.execute(script2, Collections.singletonList(userNoteLikeZSetKey), luaArgs);
                }
            }
        });
    }

    /**
     * 构建笔记收藏 ZSET Lua 脚本参数
     *
     * @param noteCollectionDOS
     * @param expireSeconds
     * @return
     */
    private static Object[] buildNoteCollectZSetLuaArgs(List<NoteCollectionDO> noteCollectionDOS, long expireSeconds) {
        int argsLength = noteCollectionDOS.size() * 2 + 1; // 每个笔记收藏关系有 2 个参数（score 和 value），最后再跟一个过期时间
        Object[] luaArgs = new Object[argsLength];

        int i = 0;
        for (NoteCollectionDO noteCollectionDO : noteCollectionDOS) {
            luaArgs[i] = DateUtils.localDateTime2Timestamp(noteCollectionDO.getCreateTime()); // 收藏时间作为 score
            luaArgs[i + 1] = noteCollectionDO.getNoteId();          // 笔记ID 作为 ZSet value
            i += 2;
        }

        luaArgs[argsLength - 1] = expireSeconds; // 最后一个参数是 ZSet 的过期时间
        return luaArgs;
    }

    /**
     * 构建笔记点赞 ZSET Lua 脚本参数
     *
     * @param noteLikeDOS
     * @param expireSeconds
     * @return
     */
    private static Object[] buildNoteLikeZSetLuaArgs(List<NoteLikeDO> noteLikeDOS, long expireSeconds) {
        int argsLength = noteLikeDOS.size() * 2 + 1; // 每个笔记点赞关系有 2 个参数（score 和 value），最后再跟一个过期时间
        Object[] luaArgs = new Object[argsLength];

        int i = 0;
        for (NoteLikeDO noteLikeDO : noteLikeDOS) {
            luaArgs[i] = DateUtils.localDateTime2Timestamp(noteLikeDO.getCreateTime()); // 点赞时间作为 score
            luaArgs[i + 1] = noteLikeDO.getNoteId();          // 笔记ID 作为 ZSet value
            i += 2;
        }

        luaArgs[argsLength - 1] = expireSeconds; // 最后一个参数是 ZSet 的过期时间
        return luaArgs;
    }

    /**
     * 初始化笔记收藏布隆过滤器
     * @param userId
     * @param expireSeconds
     * @param bloomUserNoteCollectListKey
     */
    private void batchAddNoteCollect2BloomAndExpire(Long userId, long expireSeconds, String bloomUserNoteCollectListKey) {
        try {
            // 异步全量同步一下，并设置过期时间
            List<NoteCollectionDO> noteCollectionDOS = noteCollectionDOMapper.selectByUserId(userId);

            if (CollUtil.isNotEmpty(noteCollectionDOS)) {
                DefaultRedisScript<Long> script = new DefaultRedisScript<>();
                // Lua 脚本路径
                script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/bloom_batch_add_note_collect_and_expire.lua")));
                // 返回值类型
                script.setResultType(Long.class);

                // 构建 Lua 参数
                List<Object> luaArgs = Lists.newArrayList();
                noteCollectionDOS.forEach(noteCollectionDO -> luaArgs.add(noteCollectionDO.getNoteId())); // 将每个收藏的笔记 ID 传入
                luaArgs.add(expireSeconds);  // 最后一个参数是过期时间（秒）
                redisTemplate.execute(script, Collections.singletonList(bloomUserNoteCollectListKey), luaArgs.toArray());
            }
        } catch (Exception e) {
            log.error("## 异步初始化【笔记收藏】布隆过滤器异常: ", e);
        }
    }

    /**
     * 初始化笔记点赞布隆过滤器
     * @param userId
     * @param expireSeconds
     * @param bloomUserNoteLikeListKey
     */
    private void batchAddNoteLike2BloomAndExpire(Long userId, long expireSeconds, String bloomUserNoteLikeListKey) {
        try {
            // 异步全量同步一下，并设置过期时间
            List<NoteLikeDO> noteLikeDOS = noteLikeDOMapper.selectByUserId(userId);

            if (CollUtil.isNotEmpty(noteLikeDOS)) {
                DefaultRedisScript<Long> script = new DefaultRedisScript<>();
                // Lua 脚本路径
                script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/bloom_batch_add_note_like_and_expire.lua")));
                // 返回值类型
                script.setResultType(Long.class);

                // 构建 Lua 参数
                List<Object> luaArgs = Lists.newArrayList();
                noteLikeDOS.forEach(noteLikeDO -> luaArgs.add(noteLikeDO.getNoteId())); // 将每个点赞的笔记 ID 传入
                luaArgs.add(expireSeconds);  // 最后一个参数是过期时间（秒）
                redisTemplate.execute(script, Collections.singletonList(bloomUserNoteLikeListKey), luaArgs.toArray());
            }
        } catch (Exception e) {
            log.error("## 异步初始化【笔记点赞】布隆过滤器异常: ", e);
        }
    }

    /**
     * 检验笔记是否存在
     * @param noteId
     */
    private Long checkNoteIsExist(Long noteId) {
        //从本地缓存查找
        String findNoteDetailRspVOStrLocalCache = LOCAL_CACHE.getIfPresent(noteId);
        FindNoteDetailRspVO findNoteDetailRspVO = JsonUtils.parseObject(findNoteDetailRspVOStrLocalCache, FindNoteDetailRspVO.class);
        if (Objects.isNull(findNoteDetailRspVO)) {
            //从redis查找
            String noteDetailRedisKey = RedisKeyConstants.buildNoteDetailKey(noteId);
            String noteDetailRedis =  redisTemplate.opsForValue().get(noteDetailRedisKey);
            findNoteDetailRspVO = JsonUtils.parseObject(noteDetailRedis, FindNoteDetailRspVO.class);
            if(Objects.isNull(findNoteDetailRspVO)){
                // 笔记发布者用户 ID
                Long creatorId = noteDOMapper.selectCreatorIdByNoteId(noteId);
                if(Objects.isNull(creatorId)){
                    throw new BizException(ResponseCodeEnum.NOTE_NOT_FOUND);
                }
                //数据库中若存在，将数据异步同步到redis
                taskExecutor.submit(() -> {
                    FindNoteDetailReqVO findNoteDetailReqVO = FindNoteDetailReqVO.builder()
                            .id(noteId)
                            .build();
                    findNoteDetail(findNoteDetailReqVO);
                });
            }
        }
        return findNoteDetailRspVO.getCreatorId();
    }

    /**
     * 校验笔记是否可见
     * @param visible
     * @param currUserId
     * @param creatorId
     */
    private void checkNoteVisible(Integer visible, Long currUserId, Long creatorId){
        if (Objects.equals(visible, NoteVisibleEnum.PRIVATE.getCode())
                && !Objects.equals(currUserId, creatorId)) {
            // 仅自己可见, 并且访问用户为笔记创建者才能访问，非本人则抛出异常
            throw new BizException(ResponseCodeEnum.NOTE_PRIVATE);
        }
    }

    /**
     * 校验笔记的可见性（针对 VO 实体类）
     * @param userId
     * @param findNoteDetailRspVO
     */
    private void checkNoteVisibleFromVO(Long userId, FindNoteDetailRspVO findNoteDetailRspVO) {
        if (Objects.nonNull(findNoteDetailRspVO)) {
            Integer visible = findNoteDetailRspVO.getVisible();
            checkNoteVisible(visible, userId, findNoteDetailRspVO.getCreatorId());
        }
    }

}
