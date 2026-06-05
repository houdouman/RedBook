package com.dobby.xiaohashu.note.biz.consumer;

import com.alibaba.nacos.shaded.com.google.common.util.concurrent.RateLimiter;
import com.dobby.framework.common.util.JsonUtils;
import com.dobby.xiaohashu.note.biz.constant.MQConstants;
import com.dobby.xiaohashu.note.biz.domain.dataobject.NoteLikeDO;
import com.dobby.xiaohashu.note.biz.domain.mapper.NoteLikeDOMapper;
import com.dobby.xiaohashu.note.biz.mode.dto.LikeUnlikeNoteMqDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * @author 橡皮鸭
 * @version 1.0
 * @date 2026/4/21 21:15
 */
@Component
@RocketMQMessageListener(consumerGroup = "xiaohashu_group_" + MQConstants.TOPIC_LIKE_OR_UNLIKE,
topic = MQConstants.TOPIC_LIKE_OR_UNLIKE,
consumeMode = ConsumeMode.ORDERLY)
@Slf4j
public class LikeUnlikeNoteConsumer implements RocketMQListener<Message> {

    private RateLimiter rateLimiter = RateLimiter.create(5000);

    @Resource
    private NoteLikeDOMapper noteLikeDOMapper;
    @Resource
    private RocketMQTemplate rocketMQTemplate;


    @Override
    public void onMessage(Message message) {
        rateLimiter.acquire();
        String bodyJsonStr = new String(message.getBody());
        String tags = message.getTags();
        log.info("==> LikeUnlikeNoteConsumer 消费了消息 {}, tags: {}", bodyJsonStr, tags);
        if(Objects.equals(tags, MQConstants.TAG_LIKE)){
            handleLikeTagMessage(bodyJsonStr);
        }else if(Objects.equals(tags, MQConstants.TAG_UNLIKE)){
            handleUnLikeTagMessage(bodyJsonStr);
        }
    }

    /**
     * 点赞笔记数据库存储数据
     * @param bodyJsonStr
     */
    private void handleLikeTagMessage(String bodyJsonStr){
        LikeUnlikeNoteMqDTO likeUnlikeNoteMqDTO = JsonUtils.parseObject(bodyJsonStr, LikeUnlikeNoteMqDTO.class);
        if(Objects.isNull(likeUnlikeNoteMqDTO)) return;
        //幂等性，由用户id和笔记id的共同唯一索引满足
        Long userId = likeUnlikeNoteMqDTO.getUserId();
        Long noteId = likeUnlikeNoteMqDTO.getNoteId();
        LocalDateTime createTime = likeUnlikeNoteMqDTO.getCreateTime();
        //操作类型
        Integer type = likeUnlikeNoteMqDTO.getType();
        //数据库中用户点赞笔记表更新
        NoteLikeDO noteLikeDO = NoteLikeDO.builder()
                .userId(userId)
                .noteId(noteId)
                .createTime(createTime)
                .status(type)
                .build();
        int count = noteLikeDOMapper.insertOrUpdate(noteLikeDO);
        if (count == 0) return;
        // 更新数据库成功后，发送计数 MQ
        org.springframework.messaging.Message<String> message = MessageBuilder.withPayload(bodyJsonStr)
                .build();
        // 异步发送 MQ 消息
        rocketMQTemplate.asyncSend(MQConstants.TOPIC_COUNT_NOTE_LIKE, message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【计数: 笔记点赞】MQ 发送成功，SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("==> 【计数: 笔记点赞】MQ 发送异常: ", throwable);
            }
        });

    }

    /**
     * 取消点赞笔记数据库删除数据
     * @param bodyJsonStr
     */
    private void handleUnLikeTagMessage(String bodyJsonStr){
        LikeUnlikeNoteMqDTO likeUnlikeNoteMqDTO = JsonUtils.parseObject(bodyJsonStr, LikeUnlikeNoteMqDTO.class);
        if(Objects.isNull(likeUnlikeNoteMqDTO)) return;
        //幂等性，由用户id和笔记id的共同唯一索引满足
        Long userId = likeUnlikeNoteMqDTO.getUserId();
        Long noteId = likeUnlikeNoteMqDTO.getNoteId();
        LocalDateTime createTime = likeUnlikeNoteMqDTO.getCreateTime();
        //操作类型
        Integer type = likeUnlikeNoteMqDTO.getType();
        //数据库中用户点赞笔记表更新
        NoteLikeDO noteLikeDO = NoteLikeDO.builder()
                .userId(userId)
                .noteId(noteId)
                .createTime(createTime)
                .status(type)
                .build();
        int count = noteLikeDOMapper.update2UnlikeByUserIdAndNoteId(noteLikeDO);
        if (count == 0) return;
        // 更新数据库成功后，发送计数 MQ
        org.springframework.messaging.Message<String> message = MessageBuilder.withPayload(bodyJsonStr)
                .build();
        // 异步发送 MQ 消息
        rocketMQTemplate.asyncSend(MQConstants.TOPIC_COUNT_NOTE_LIKE, message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【计数: 笔记取消点赞】MQ 发送成功，SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("==> 【计数: 笔记取消点赞】MQ 发送异常: ", throwable);
            }
        });
    }
}
