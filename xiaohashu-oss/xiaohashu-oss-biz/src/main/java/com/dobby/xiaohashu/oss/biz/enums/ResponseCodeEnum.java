package com.dobby.xiaohashu.oss.biz.enums;

import com.dobby.framework.common.exception.BaseExceptionInterface;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author 橡皮鸭
 * @version 1.0
 * @date 2026/4/2 18:18
 */
@Getter
@AllArgsConstructor
public enum ResponseCodeEnum implements BaseExceptionInterface {
    // ----------- 通用异常状态码 -----------
    SYSTEM_ERROR("OSS-10000", "出错了，后台人员努力修复中..."),
    PARAM_NOT_VALID("OSS-10001", "参数错误"),
    ;
    private final String errorCode;
    private final String errorMessage;


}
