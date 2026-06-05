package com.dobby.xiaohashu.note.biz.rpc;

import com.dobby.framework.common.response.Response;
import com.dobby.xiaohashu.user.api.UserFeignApi;
import com.dobby.xiaohashu.user.dto.req.FindUserByIdReqDTO;
import com.dobby.xiaohashu.user.dto.rsp.FindUserByIdRspDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * @author 橡皮鸭
 * @version 1.0
 * @date 2026/4/12 17:47
 */
@Component
public class UserRpcService {
    @Resource
    private UserFeignApi userFeignApi;

    /**
     * 查询用户信息
     * @param userId
     * @return
     */
    public FindUserByIdRspDTO findById(Long userId){
        FindUserByIdReqDTO reqDTO = new FindUserByIdReqDTO();
        reqDTO.setId(userId);
        Response<FindUserByIdRspDTO> response = userFeignApi.findById(reqDTO);
        if(Objects.isNull(response) || !response.isSuccess()){
            return null;
        }
        return response.getData();
    }
}
