package com.dobby.xiaohashu.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(basePackages = "com.dobby.xiaohashu")
public class XiaohashuAuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(XiaohashuAuthApplication.class, args);
    }

}
