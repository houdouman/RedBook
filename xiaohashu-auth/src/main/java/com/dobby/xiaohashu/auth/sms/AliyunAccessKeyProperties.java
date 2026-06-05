package com.dobby.xiaohashu.auth.sms;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author 橡皮鸭
 * @version 1.0
 * @date 2026/3/31 11:41
 * 阿里云Access Key配置
 */
@ConfigurationProperties(prefix = "aliyun")
@Component
@Data
public class AliyunAccessKeyProperties {
    private String accessKeyId;
    private String accessKeySecret;
}
