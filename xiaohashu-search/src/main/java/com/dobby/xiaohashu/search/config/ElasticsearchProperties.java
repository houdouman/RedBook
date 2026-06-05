package com.dobby.xiaohashu.search.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author 橡皮鸭
 * @version 1.0
 * @date 2026/4/26 16:34
 */
@ConfigurationProperties(prefix = "elasticsearch")
@Component
@Data
public class ElasticsearchProperties {
    private String address;
}