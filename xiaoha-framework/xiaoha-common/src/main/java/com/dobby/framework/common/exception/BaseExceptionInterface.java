package com.dobby.framework.common.exception;

/**
 * @author 橡皮鸭
 * @version 1.0
 * @date 2026/3/30 14:50
 * 基础异常接口
 */
public interface BaseExceptionInterface {
    //获取异常码
    String getErrorCode();
    //获取异常信息
    String getErrorMessage();
}
