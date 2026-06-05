package com.dobby.framework.biz.operationlog.config;

import com.dobby.framework.biz.operationlog.aspect.ApiOperationLogAspect;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * @author 橡皮鸭
 * @version 1.0
 * @date 2026/3/30 15:48
 * 日志切面自动配置类
 */
@AutoConfiguration
public class ApiOperationLogAutoConfiguration {
    @Bean
    public ApiOperationLogAspect apiOperationLogAspect() {
        return new ApiOperationLogAspect();
    }
}
