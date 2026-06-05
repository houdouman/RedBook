package com.dobby.xiaohashu.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author 橡皮鸭
 * @version 1.0
 * @date 2026/3/31 09:48
 * 自定义线程池
 */
@Configuration
public class ThreadPoolConfig {

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        //核心线程数
        executor.setCorePoolSize(10);
        //最大线程数
        executor.setMaxPoolSize(50);
        //队列容量
        executor.setQueueCapacity(200);
        //线程活跃时间
        executor.setKeepAliveSeconds(30);
        //线程名前缀
        executor.setThreadNamePrefix("AuthExecutor-");

        //拒绝策略：由调用线程处理（一般为主线程）
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        //等待所有任务结束再关闭线程
        executor.setWaitForTasksToCompleteOnShutdown(true);
        //设置等待时间，超过时间强制销毁
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;

    }
}
