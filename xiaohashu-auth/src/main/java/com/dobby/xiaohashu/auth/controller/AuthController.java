package com.dobby.xiaohashu.auth.controller;

import com.dobby.framework.biz.operationlog.aspect.ApiOperationLog;
import com.dobby.framework.common.response.Response;
import com.dobby.xiaohashu.auth.model.vo.user.UserLoginReqVO;
import com.dobby.xiaohashu.auth.service.AuthService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * @author 橡皮鸭
 * @version 1.0
 * @date 2026/3/31 15:18
 */
@RestController
@Slf4j
public class AuthController {

    @Resource
    private AuthService userService;

    @PostMapping("/login")
    @ApiOperationLog(description = "用户登录/注册")
    public Response<String> loginAndRegister(@RequestBody @Validated UserLoginReqVO userLoginReqVO){
        return userService.loginAndRegister(userLoginReqVO);
    }

    @PostMapping("/logout")
    @ApiOperationLog(description = "账号登出")
    public Response<?> logout() {
        return userService.logout();
    }


}
