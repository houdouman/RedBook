package com.dobby.xiaohashu.count.biz.consumer;

import com.dobby.framework.common.util.JsonUtils;
import com.dobby.xiaohashu.count.biz.constant.MQConstants;
import com.dobby.xiaohashu.count.biz.constant.RedisKeyConstants;
import com.dobby.xiaohashu.count.biz.enums.FollowUnfollowTypeEnum;
import com.dobby.xiaohashu.count.biz.model.dto.CountFollowUnfollowMqDTO;
import com.github.phantomthief.collection.BufferTrigger;
import com.google.common.collect.Maps;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author 橡皮鸭
 * @version 1.0
 * @date 2026/4/19 19:47
 * 计数：关注数
 */
@Component
@RocketMQMessageListener(consumerGroup = "xiaohashu_group_" + MQConstants.TOPIC_COUNT_FOLLOWING, // Group 组
        topic = MQConstants.TOPIC_COUNT_FOLLOWING // 主题 Topic
)
@Slf4j
public class CountFollowingConsumer implements RocketMQListener<String> {
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private RocketMQTemplate rocketMQTemplate;

    @Override
    public void onMessage(String body) {
        log.info("## 消费到了 MQ 【计数: 关注数】, {}...", body);
        //关注计数无需聚合
        CountFollowUnfollowMqDTO countFollowUnfollowMqDTO = JsonUtils.parseObject(body, CountFollowUnfollowMqDTO.class);
        if (Objects.isNull(countFollowUnfollowMqDTO)) return;
        Integer type = countFollowUnfollowMqDTO.getType();
        FollowUnfollowTypeEnum followUnfollowTypeEnum = FollowUnfollowTypeEnum.valueOf(type);
        //原用户id
        Long userId = countFollowUnfollowMqDTO.getUserId();
        //更新redis
        String redisKey = RedisKeyConstants.buildCountUserKey(userId);
        //判断hash是否存在
        boolean isExist = redisTemplate.hasKey(redisKey);
        if(isExist){
            long count = Objects.equals(type, FollowUnfollowTypeEnum.FOLLOW.getCode()) ? 1 : -1;
            //更新redis
            redisTemplate.opsForHash().increment(redisKey,RedisKeyConstants.FIELD_FOLLOWING_TOTAL,count);
        }
        //发送MQ消息体，关注数写入数据库
        Message<String> message = MessageBuilder.withPayload(body).build();
        // 异步发送 MQ 消息
        rocketMQTemplate.asyncSend(MQConstants.TOPIC_COUNT_FOLLOWING_2_DB, message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【计数服务：关注数入库】MQ 发送成功，SendResult: {}", sendResult);
            }
            @Override
            public void onException(Throwable throwable) {
                log.error("==> 【计数服务：关注数入库】MQ 发送异常: ", throwable);
            }
        });
    }
}
