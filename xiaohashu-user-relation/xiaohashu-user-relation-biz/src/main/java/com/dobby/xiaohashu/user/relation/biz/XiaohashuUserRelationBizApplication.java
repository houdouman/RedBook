package com.dobby.xiaohashu.user.relation.biz;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * @author 橡皮鸭
 * @version 1.0
 * @date 2026/4/14 17:33
 */
@SpringBootApplication
@MapperScan("com.dobby.xiaohashu.user.relation.biz.domain.mapper")
@EnableFeignClients(basePackages = "com.dobby.xiaohashu")
public class XiaohashuUserRelationBizApplication {
    public static void main(String[] args) {
        SpringApplication.run(XiaohashuUserRelationBizApplication.class, args);
    }
}
