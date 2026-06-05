package com.dobby.xiaohashu.user.biz.enums;

import com.dobby.framework.common.exception.BaseExceptionInterface;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author 橡皮鸭
 * @version 1.0
 * @date 2026/4/3 10:02
 */
@Getter
@AllArgsConstructor
public enum ResponseCodeEnum implements BaseExceptionInterface {
    SYSTEM_ERROR("USER-10000", "出错了，后台人员努力修复中..."),
    PARAM_NOT_VALID("USER-10001", "参数错误"),
    // ----------- 业务异常状态码 -----------
    NICK_NAME_VALID_FAIL("USER-20001", "昵称请设置2-24个字符，不能使用@《/等特殊字符"),
    XIAOHASHU_ID_VALID_FAIL("USER-20002", "小哈书号请设置6-15个字符，仅可使用英文（必须）、数字、下划线"),
    SEX_VALID_FAIL("USER-20003", "性别错误"),
    INTRODUCTION_VALID_FAIL("USER-20004", "个人简介请设置1-100个字符"),
    UPLOAD_AVATAR_FAIL("USER-20005", "头像上传失败"),
    UPLOAD_BACKGROUND_IMG_FAIL("USER-20006", "背景图上传失败"),
    USER_NOT_FOUND("USER-20007", "该用户不存在"),
    ;

    private final String errorCode;
    private final String errorMessage;
}
