package com.dobby.framework.common.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author 橡皮鸭
 * @version 1.0
 * @date 2026/3/31 14:00
 * 自定义手机号校验注解
 */
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME) // 指定注解保留策略，RetentionPolicy.RUNTIME表示该注解在运行时仍然可用
@Constraint(validatedBy = PhoneNumberValidator.class) //指定关联的验证器类
public @interface PhoneNumber {

    String message() default "手机号格式不正确, 需为 11 位数字";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
