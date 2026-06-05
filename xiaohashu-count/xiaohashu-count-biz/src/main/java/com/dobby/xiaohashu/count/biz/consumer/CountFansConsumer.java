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
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
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
 */
@Component
@RocketMQMessageListener(consumerGroup = "xiaohashu_group_" + MQConstants.TOPIC_COUNT_FANS, // Group 组
        topic = MQConstants.TOPIC_COUNT_FANS // 主题 Topic
)
@Slf4j
public class CountFansConsumer implements RocketMQListener<String> {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    private BufferTrigger<String> bufferTrigger = BufferTrigger.<String>batchBlocking()
            .bufferSize(50000) // 缓存队列的最大容量
            .batchSize(1000)   // 一批次最多聚合 1000 条
            .linger(Duration.ofSeconds(1)) // 多久聚合一次
            .setConsumerEx(this::consumeMessage) // 设置消费者方法
            .build();
    @Resource
    private RocketMQTemplate rocketMQTemplate;

    @Override
    public void onMessage(String body) {
        // 往 bufferTrigger 中添加元素
        bufferTrigger.enqueue(body);
    }

    private void consumeMessage(List<String> bodys) {
        log.info("==> 聚合消息, size: {}", bodys.size());
        log.info("==> 聚合消息, {}", JsonUtils.toJsonString(bodys));
        // List<String> 转 List<CountFollowUnfollowMqDTO>
        List<CountFollowUnfollowMqDTO> countFollowUnfollowMqDTOS = bodys.stream()
                .map(body -> JsonUtils.parseObject(body, CountFollowUnfollowMqDTO.class))
                .toList();
        //按目标用户id分组
        Map<Long, List<CountFollowUnfollowMqDTO>> groupMap = countFollowUnfollowMqDTOS.stream()
                .collect(Collectors.groupingBy(CountFollowUnfollowMqDTO::getTargetUserId));
        //统计计数
        Map<Long, Integer> countMap = Maps.newHashMap();
        for (Map.Entry<Long, List<CountFollowUnfollowMqDTO>> entry : groupMap.entrySet()) {
            List<CountFollowUnfollowMqDTO> list = entry.getValue();
            //最终的计数值，默认为0
            int finalCount = 0;
            for (CountFollowUnfollowMqDTO countFollowUnfollowMqDTO : list) {
                Integer type = countFollowUnfollowMqDTO.getType();
                FollowUnfollowTypeEnum followUnfollowTypeEnum = FollowUnfollowTypeEnum.valueOf(type);
                // 若枚举为空，跳到下一次循环
                if (Objects.isNull(followUnfollowTypeEnum)) continue;

                switch (followUnfollowTypeEnum) {
                    case FOLLOW -> finalCount += 1; // 如果为关注操作，粉丝数 +1
                    case UNFOLLOW -> finalCount -= 1; // 如果为取关操作，粉丝数 -1
                }
            }
            countMap.put(entry.getKey(), finalCount);
        }
        //更新redis
        countMap.forEach((userId, count) -> {
            String redisKey = RedisKeyConstants.buildCountUserKey(userId);
            //判断redis中hash是否存在
            boolean isExist = redisTemplate.hasKey(redisKey);
            if (isExist) {
                redisTemplate.opsForHash().increment(redisKey, RedisKeyConstants.FIELD_FANS_TOTAL, count);
            }
        });
        //发送MQ，计数数据存入数据库
        Message<String> message = MessageBuilder.withPayload(JsonUtils.toJsonString(countMap)).build();
        //异步发送MQ消息
        rocketMQTemplate.asyncSend(MQConstants.TOPIC_COUNT_FANS_2_DB, message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【计数服务：粉丝数入库】MQ 发送成功，SendResult: {}", sendResult);
            }
            @Override
            public void onException(Throwable throwable) {
                log.error("==> 【计数服务：粉丝数入库】MQ 发送异常: ", throwable);
            }
        });
    }

}
