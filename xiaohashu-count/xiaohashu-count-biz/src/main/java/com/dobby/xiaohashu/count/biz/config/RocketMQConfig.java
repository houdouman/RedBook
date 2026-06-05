package com.dobby.xiaohashu.count.biz.config;


import org.apache.rocketmq.spring.autoconfigure.RocketMQAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @author 橡皮鸭
 * @version 1.0
 * @date 2026/4/12 20:29
 */
@Configuration
@Import(RocketMQAutoConfiguration.class)
public class RocketMQConfig {
}
