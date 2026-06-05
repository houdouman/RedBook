package com.dobby.framework.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author 橡皮鸭
 * @version 1.0
 * @date 2026/3/31 15:21
 * 逻辑删除
 */
@Getter
@AllArgsConstructor
public enum DeletedEnum {

    YES(true),
    NO(false),
    ;
    private final Boolean value;
}
