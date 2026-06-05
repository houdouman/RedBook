package com.dobby.framework.common.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * @author 橡皮鸭
 * @version 1.0
 * @date 2026/3/31 11:52
 */
public class PhoneNumberValidator implements ConstraintValidator<PhoneNumber,String> {
    @Override
    public void initialize(PhoneNumber constraintAnnotation) {
        //进行初始化操作
    }

    @Override
    public boolean isValid(String phoneNumber, ConstraintValidatorContext context) {
        // 校验逻辑：正则表达式判断手机号是否为 11 位数字
        return phoneNumber != null && phoneNumber.matches("\\d{11}");
    }
}
