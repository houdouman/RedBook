package com.dobby.xiaohashu.comment.biz;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.retry.annotation.EnableRetry;

/**
 * @author 橡皮鸭
 * @version 1.0
 * @date 2026/6/2 19:45
 */
@SpringBootApplication
@MapperScan("com.dobby.xiaohashu.comment.biz.domain.mapper")
@EnableRetry // 启用 Spring Retry
@EnableFeignClients(basePackages = "com.dobby.xiaohashu")
public class XiaohashuCommentBizApplication {

    public static void main(String[] args) {
        SpringApplication.run(XiaohashuCommentBizApplication.class, args);
    }

}

