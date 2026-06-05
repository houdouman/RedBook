package com.dobby.framework.common.exception;

import lombok.Getter;
import lombok.Setter;

/**
 * @author 橡皮鸭
 * @version 1.0
 * @date 2026/3/30 14:53
 * 业务异常类
 */
@Getter
@Setter
public class BizException extends RuntimeException{

    //异常码
    private String errorCode;
    //错误信息
    private String errorMessage;

    public BizException(BaseExceptionInterface baseExceptionInterface) {
        this.errorCode = baseExceptionInterface.getErrorCode();
        this.errorMessage = baseExceptionInterface.getErrorMessage();
    }
}
