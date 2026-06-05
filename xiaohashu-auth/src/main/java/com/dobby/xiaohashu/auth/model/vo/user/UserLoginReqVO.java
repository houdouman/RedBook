package com.dobby.xiaohashu.auth.model.vo.user;

import com.dobby.framework.common.validator.PhoneNumber;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author 橡皮鸭
 * @version 1.0
 * @date 2026/3/31 14:54
 * 用户登录，支持验证码和密码两种
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserLoginReqVO {

    /**
     * 手机号
     */
    @NotBlank(message = "手机号不能为空")
    @PhoneNumber
    private String phone;

    /**
     * 验证码
     */
    private String code;

    /**
     * 密码
     */
    private String password;

    /**
     * 登录类型：手机号验证码(1)，或者是账号密码(2)
     */
    @NotNull(message = "登录类型不能为空")
    private Integer type;
}
