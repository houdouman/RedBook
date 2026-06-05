package com.dobby.xiaohashu.auth.service;

import com.dobby.framework.common.response.Response;
import com.dobby.xiaohashu.auth.model.vo.verificationcode.SendVerificationCodeReqVO;

/**
 * @author 橡皮鸭
 * @version 1.0
 * @date 2026/3/30 20:06
 */
public interface VerificationCodeService {

    /**
     * 发送短信验证码
     * @param sendVerificationCodeReqVO
     * @return
     */
    Response<?> send(SendVerificationCodeReqVO sendVerificationCodeReqVO);
}
