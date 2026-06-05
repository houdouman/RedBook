package com.dobby.xiaohashu.auth.service;

import com.dobby.framework.common.response.Response;
import com.dobby.xiaohashu.auth.model.vo.user.UserLoginReqVO;

/**
 * @author 橡皮鸭
 * @version 1.0
 * @date 2026/3/31 15:06
 */
public interface AuthService {


    /**
     * 登录与注册
     * @param userLoginReqVO
     * @return
     */
    Response<String> loginAndRegister(UserLoginReqVO userLoginReqVO);

    /**
     * 退出登录
     * @return
     */
    Response<?> logout();

}
