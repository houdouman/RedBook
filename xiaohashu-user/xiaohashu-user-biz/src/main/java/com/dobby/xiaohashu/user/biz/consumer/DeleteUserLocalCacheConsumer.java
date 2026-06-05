package com.dobby.xiaohashu.user.biz.consumer;

import com.dobby.xiaohashu.user.biz.constant.MQConstants;
import com.dobby.xiaohashu.user.biz.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * @author 橡皮鸭
 * @version 1.0
 * @date 2026/4/12 20:34
 */
@Component
@Slf4j
@RocketMQMessageListener(consumerGroup = "xiaohashu_group_" + MQConstants.TOPIC_DELETE_USER_LOCAL_CACHE,
topic = MQConstants.TOPIC_DELETE_USER_LOCAL_CACHE,
messageModel = MessageModel.BROADCASTING)
public class DeleteUserLocalCacheConsumer implements RocketMQListener<String> {

    @Resource
    private UserService userService;

    @Override
    public void onMessage(String s) {
        Long userId = Long.valueOf(s);
        log.info("## 消费者消费成功, userId: {}", userId);
        userService.deleteUserLocalCache(userId);
    }
}
