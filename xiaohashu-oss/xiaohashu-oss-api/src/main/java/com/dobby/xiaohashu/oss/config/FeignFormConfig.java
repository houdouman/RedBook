package com.dobby.xiaohashu.oss.config;

import feign.codec.Encoder;
import feign.form.spring.SpringFormEncoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author 橡皮鸭
 * @version 1.0
 * @date 2026/4/3 16:18
 * FeignForm表单配置类
 */
@Configuration
public class FeignFormConfig {

    /**
     * SpringFormEncoder是Feign提供的一个编码器，用于处理表单提交
     * @return
     */
    @Bean
    public Encoder feignFormEncoder() {
        return new SpringFormEncoder();
    }
}
