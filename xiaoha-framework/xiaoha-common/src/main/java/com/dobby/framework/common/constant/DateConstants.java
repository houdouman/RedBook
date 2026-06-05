package com.dobby.framework.common.constant;

import java.time.format.DateTimeFormatter;

/**
 * @author 橡皮鸭
 * @version 1.0
 * @date 2026/3/30 16:11
 * 日期常量接口
 */
public interface DateConstants {

    /**
     * DateTimeFormatter：年-月-日 时：分：秒
     */
    DateTimeFormatter DATE_FORMAT_Y_M_D_H_M_S = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * DateTimeFormatter：年-月-日
     */
    DateTimeFormatter DATE_FORMAT_Y_M_D = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * DateTimeFormatter：时：分：秒
     */
    DateTimeFormatter DATE_FORMAT_H_M_S = DateTimeFormatter.ofPattern("HH:mm:ss");

    /**
     * DateTimeFormatter：年-月
     */
    DateTimeFormatter DATE_FORMAT_Y_M =  DateTimeFormatter.ofPattern("yyyy-MM");

    /**
     * DateTimeFormatter：月-日
     */
    DateTimeFormatter DATE_FORMAT_M_D = DateTimeFormatter.ofPattern("MM-dd");


    /**
     * DateTimeFormatter：时：分
     */
    DateTimeFormatter DATE_FORMAT_H_M = DateTimeFormatter.ofPattern("HH:mm");
}

