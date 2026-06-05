package com.dobby.xiaohashu.auth.alarm;

import com.dobby.xiaohashu.auth.alarm.impl.MailAlarmHelper;
import com.dobby.xiaohashu.auth.alarm.impl.SmsAlarmHelper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author 橡皮鸭
 * @version 1.0
 * @date 2026/3/31 19:11
 */
@Configuration
@RefreshScope //实现配置动态刷新功能。当配置中心的配置发生变化时，标注了@RefreshScope的Bean会重新加载最新的配置，而无需重启应用
public class AlarmConfig {

    @Value("${alarm.type}")
    private String alarmType;

    @Bean
    @RefreshScope
    public AlarmInterface alarmHelper(){
        // 根据配置文件中的告警类型，初始化选择不同的告警实现类
        if (StringUtils.equals("sms", alarmType)) {
            return new SmsAlarmHelper();
        } else if (StringUtils.equals("mail", alarmType)) {
            return new MailAlarmHelper();
        } else {
            throw new IllegalArgumentException("错误的告警类型...");
        }
    }
}
