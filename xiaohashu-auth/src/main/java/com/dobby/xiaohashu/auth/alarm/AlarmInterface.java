package com.dobby.xiaohashu.auth.alarm;

/**
 * @author 橡皮鸭
 * @version 1.0
 * @date 2026/3/31 19:11
 */
public interface AlarmInterface {

    /**
     * 发送告警信息
     * @param message
     * @return
     */
    boolean send(String message);
}
