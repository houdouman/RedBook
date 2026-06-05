package com.dobby.xiaohashu.gateway.enums;

import com.dobby.framework.common.exception.BaseExceptionInterface;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author 橡皮鸭
 * @version 1.0
 * @date 2026/4/1 16:08
 */
@Getter
@AllArgsConstructor
public enum ResponseCodeEnum implements BaseExceptionInterface {
    // ----------- 通用异常状态码 -----------
    SYSTEM_ERROR("500", "系统繁忙，请稍后再试"),
    UNAUTHORIZED("401", "权限不足"),


    // ----------- 业务异常状态码 -----------


    ;

    // 异常码
    private final String errorCode;
    // 错误信息
    private final String errorMessage;

}
