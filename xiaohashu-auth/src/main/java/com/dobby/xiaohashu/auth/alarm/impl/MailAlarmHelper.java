package com.dobby.xiaohashu.auth.alarm.impl;

import com.dobby.xiaohashu.auth.alarm.AlarmInterface;
import lombok.extern.slf4j.Slf4j;

/**
 * @author 橡皮鸭
 * @version 1.0
 * @date 2026/3/31 19:12
 */
@Slf4j
public class MailAlarmHelper implements AlarmInterface {

    /**
     * 发送告警信息
     *
     * @param message
     * @return
     */
    @Override
    public boolean send(String message) {
        log.info("==> 【邮件告警】：{}", message);

        // 业务逻辑...

        return true;
    }
}
