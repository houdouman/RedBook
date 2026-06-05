package com.dobby.xiaohashu.user.biz;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * @author 橡皮鸭
 * @version 1.0
 * @date 2026/4/3 09:22
 */
@SpringBootApplication
@MapperScan("com.dobby.xiaohashu.user.biz.domain.mapper")
@EnableFeignClients(basePackages = "com.dobby.xiaohashu")
public class XiaohashuUserBizApplication {

    public  static void main(String[] args) {
        SpringApplication.run(XiaohashuUserBizApplication.class, args);
    }
}
