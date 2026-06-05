package com.dobby.xiaohashu.oss.biz.config;

import io.minio.MinioClient;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author 橡皮鸭
 * @version 1.0
 * @date 2026/4/2 18:10
 */
@Configuration
@Slf4j
public class MinioConfig {

    @Resource
    private MinioProperties minioProperties;

    @Bean
    public MinioClient minioClient(){
        //构建Minio客户端
        return MinioClient.builder()
                .endpoint(minioProperties.getEndpoint())
                .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
                .build();
    }
}
