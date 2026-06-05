package com.dobby.xiaohashu.auth.rpc;

import com.dobby.framework.common.response.Response;
import com.dobby.xiaohashu.user.api.UserFeignApi;
import com.dobby.xiaohashu.user.dto.req.FindUserByPhoneReqDTO;
import com.dobby.xiaohashu.user.dto.req.RegisterUserReqDTO;
import com.dobby.xiaohashu.user.dto.rsp.FindUserByPhoneRspDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * @author 橡皮鸭
 * @version 1.0
 * @date 2026/4/7 21:07
 */
@Component
public class UserRpcService {

    @Resource
    private UserFeignApi userFeignApi;

    /**
     * 用户注册
     * @param phone
     * @return
     */
    public Long registerUser(String phone){
        RegisterUserReqDTO registerUserReqDTO = new RegisterUserReqDTO();
        registerUserReqDTO.setPhone(phone);
        Response<Long> response = userFeignApi.registerUser(registerUserReqDTO);
        if(!response.isSuccess()){
            return null;
        }
        return response.getData();
    }

    /**
     * 根据手机号查询用户信息
     * @param phone
     * @return
     */
    public FindUserByPhoneRspDTO findUserByPhone(String phone) {
        FindUserByPhoneReqDTO findUserByPhoneReqDTO = new FindUserByPhoneReqDTO();
        findUserByPhoneReqDTO.setPhone(phone);

        Response<FindUserByPhoneRspDTO> response = userFeignApi.findByPhone(findUserByPhoneReqDTO);

        if (!response.isSuccess()) {
            return null;
        }

        return response.getData();
    }
}
