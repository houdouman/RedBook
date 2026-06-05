package com.dobby.xiaohashu.kv.biz.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.cassandra.config.AbstractCassandraConfiguration;

/**
 * @author 橡皮鸭
 * @version 1.0
 * @date 2026/4/10 15:52
 * Cassandra配置类
 * AbstractCassandraConfiguration是Spring Data Cassandra提供的一个抽象基类
 * 包含了一些默认的方法实现，用于配置Cassandra连接
 */
@Configuration
public class CassandraConfig extends AbstractCassandraConfiguration {
    @Value("${spring.cassandra.keyspace-name}")
    private String keySpace;

    @Value("${spring.cassandra.contact-points}")
    private String contactPoints;

    @Value("${spring.cassandra.port}")
    private int port;


    @Override
    protected String getKeyspaceName() {
        return keySpace;
    }

    @Override
    public String getContactPoints() {
        return contactPoints;
    }

    @Override
    public int getPort() {
        return port;
    }
}
