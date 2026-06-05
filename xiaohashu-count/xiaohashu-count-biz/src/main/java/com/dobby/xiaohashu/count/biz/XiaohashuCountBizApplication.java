package com.dobby.xiaohashu.count.biz;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author 橡皮鸭
 * @version 1.0
 * @date 2026/4/19 19:16
 */
@SpringBootApplication
@MapperScan("com.dobby.xiaohashu.count.biz.domain.mapper")
public class XiaohashuCountBizApplication {

    public static void main(String[] args) {
        SpringApplication.run(XiaohashuCountBizApplication.class, args);
    }

}
