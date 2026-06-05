package com.dobby.framework.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author 橡皮鸭
 * @version 1.0
 * @date 2026/3/31 15:21
 * 状态枚举
 */
@Getter
@AllArgsConstructor
public enum StatusEnum {

    //启用
    ENABLED(0),
    //禁用
    DISABLED(1);
    private final Integer value;
}
