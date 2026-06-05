package com.dobby.xiaohashu.comment.biz.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author 橡皮鸭
 * @version 1.0
 * @date 2026/6/2 20:43
 */
@ConfigurationProperties(prefix = RetryProperties.PREFIX)
@Component
@Data
public class RetryProperties {

    public static final String PREFIX = "retry";

    /**
     * 最大重试次数
     */
    private Integer maxAttempts = 3;

    /**
     * 初始间隔时间，单位 ms
     */
    private Integer initInterval = 1000;

    /**
     * 乘积（每次乘以 2）
     */
    private Double multiplier = 2.0;

}
