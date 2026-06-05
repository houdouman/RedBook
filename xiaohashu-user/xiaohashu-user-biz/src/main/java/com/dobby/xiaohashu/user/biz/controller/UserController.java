package com.dobby.xiaohashu.user.biz.controller;

import com.dobby.framework.biz.operationlog.aspect.ApiOperationLog;
import com.dobby.framework.common.response.Response;
import com.dobby.xiaohashu.user.biz.model.vo.UpdatePasswordReqVO;
import com.dobby.xiaohashu.user.biz.model.vo.UpdateUserInfoReqVO;
import com.dobby.xiaohashu.user.biz.service.UserService;
import com.dobby.xiaohashu.user.dto.req.FindUserByIdReqDTO;
import com.dobby.xiaohashu.user.dto.req.FindUserByPhoneReqDTO;
import com.dobby.xiaohashu.user.dto.req.FindUsersByIdsReqDTO;
import com.dobby.xiaohashu.user.dto.req.RegisterUserReqDTO;
import com.dobby.xiaohashu.user.dto.rsp.FindUserByIdRspDTO;
import com.dobby.xiaohashu.user.dto.rsp.FindUserByPhoneRspDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author 橡皮鸭
 * @version 1.0
 * @date 2026/4/3 11:33
 */
@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {
    @Resource
    private UserService userService;

    /**
     * 用户信息修改
     * @param updateUserInfoReqVO
     * @return
     */
    @PostMapping(value = "/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Response<?> updateUserInfo(@Validated UpdateUserInfoReqVO updateUserInfoReqVO) {
        return userService.updateUserInfo(updateUserInfoReqVO);
    }

    /**
     * 更新用户密码
     * @param updatePasswordReqVO
     * @return
     */
    @PostMapping("/password/update")
    @ApiOperationLog(description = "更新密码")
    public Response<?> updatePassword(@RequestBody @Validated UpdatePasswordReqVO updatePasswordReqVO) {
        return userService.updatePassword(updatePasswordReqVO);
    }


    // ===================================== 对其他服务提供的接口 =====================================
    @PostMapping("/register")
    @ApiOperationLog(description = "用户注册")
    public Response<Long> register(@Validated @RequestBody RegisterUserReqDTO registerUserReqDTO) {
        return userService.register(registerUserReqDTO);
    }

    @PostMapping("/findByPhone")
    @ApiOperationLog(description = "手机号查询用户信息")
    public Response<FindUserByPhoneRspDTO> findByPhone(@Validated @RequestBody FindUserByPhoneReqDTO findUserByPhoneReqDTO) {
        return userService.findByPhone(findUserByPhoneReqDTO);
    }

    @PostMapping("/findById")
    @ApiOperationLog(description = "查询用户信息")
    public Response<FindUserByIdRspDTO> findById(@Validated @RequestBody FindUserByIdReqDTO findUserByIdReqDTO) {
        return userService.findById(findUserByIdReqDTO);
    }

    @PostMapping("/findByIds")
    @ApiOperationLog(description = "批量查询用户信息")
    public Response<List<FindUserByIdRspDTO>> findByIds(@Validated @RequestBody FindUsersByIdsReqDTO findUsersByIdsReqDTO) {
        return userService.findByIds(findUsersByIdsReqDTO);
    }

}
