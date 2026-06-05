package com.dobby.framework.biz.operationlog.aspect;

import java.lang.annotation.*;

/**
 * @author 橡皮鸭
 * @version 1.0
 * @date 2026/3/30 15:23
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@Documented
public @interface ApiOperationLog {
    /**
     * API功能描述
     * @return
     */
    String description() default "";
}
