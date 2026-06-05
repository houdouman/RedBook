package com.dobby.xiaohashu.count.biz.consumer;

import cn.hutool.core.collection.CollUtil;
import com.dobby.framework.common.util.JsonUtils;
import com.dobby.xiaohashu.count.biz.constant.MQConstants;
import com.dobby.xiaohashu.count.biz.domain.mapper.UserCountDOMapper;
import com.google.common.util.concurrent.RateLimiter;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author 橡皮鸭
 * @version 1.0
 * @date 2026/4/19 20:18
 */
@Component
@RocketMQMessageListener(consumerGroup = "xiaohashu_group_" + MQConstants.TOPIC_COUNT_FANS_2_DB, // Group 组
        topic = MQConstants.TOPIC_COUNT_FANS_2_DB // 主题 Topic
)
@Slf4j
public class CountFans2DBConsumer implements RocketMQListener<String> {
    @Resource
    private UserCountDOMapper userCountDOMapper;
    // 每秒创建 5000 个令牌
    private RateLimiter rateLimiter = RateLimiter.create(5000);

    @Override
    public void onMessage(String body) {
        // 流量削峰：通过获取令牌，如果没有令牌可用，将阻塞，直到获得
        rateLimiter.acquire();
        log.info("## 消费到了 MQ 【计数: 粉丝数入库】, {}...", body);
        if(StringUtils.isBlank(body)) return;
        Map<Long, Integer> countMap = null;
        try {
            countMap = JsonUtils.parseMap(body, Long.class, Integer.class);
        } catch (Exception e) {
            log.error("## 解析 JSON 字符串异常", e);
        }
        if(CollUtil.isNotEmpty(countMap)){
            countMap.forEach((k,v)->userCountDOMapper.insertOrUpdateFansTotalByUserId(v, k));
        }
    }

}
