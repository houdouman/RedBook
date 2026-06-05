package com.dobby.xiaohashu.note.biz;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * @author 橡皮鸭
 * @version 1.0
 * @date 2026/4/11 18:18
 */
@SpringBootApplication
@MapperScan("com.dobby.xiaohashu.note.biz.domain.mapper")
@EnableFeignClients(basePackages = "com.dobby.xiaohashu")
public class XiaohashuNoteBizApplication {
    public static void main(String[] args) {
        SpringApplication.run(XiaohashuNoteBizApplication.class, args);
    }
}
