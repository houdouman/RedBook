package com.dobby.xiaohashu.user.relation.biz.rpc;

import cn.hutool.core.collection.CollUtil;
import com.dobby.framework.common.response.Response;
import com.dobby.xiaohashu.user.api.UserFeignApi;
import com.dobby.xiaohashu.user.dto.req.FindUserByIdReqDTO;
import com.dobby.xiaohashu.user.dto.req.FindUsersByIdsReqDTO;
import com.dobby.xiaohashu.user.dto.rsp.FindUserByIdRspDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * @author 橡皮鸭
 * @version 1.0
 * @date 2026/4/15 10:35
 */
@Component
public class UserRpcService {

    @Resource
    private UserFeignApi userFeignApi;

    /**
     * 根据用户id查询
     * @param userId
     * @return
     */
    public FindUserByIdRspDTO findById(Long userId) {
        FindUserByIdReqDTO findUserByIdReqDTO = FindUserByIdReqDTO.builder().id(userId).build();
        Response<FindUserByIdRspDTO> response = userFeignApi.findById(findUserByIdReqDTO);
        if(!response.isSuccess() || Objects.isNull(response.getData())) {
            return null;
        }

        return response.getData();
    }

    /**
     * 根据用户id批量查询
     * @param userIds
     * @return
     */
    public List<FindUserByIdRspDTO> findByIds(List<Long> userIds) {
        FindUsersByIdsReqDTO findUsersByIdsReqDTO = new FindUsersByIdsReqDTO();
        findUsersByIdsReqDTO.setIds(userIds);

        Response<List<FindUserByIdRspDTO>> response = userFeignApi.findByIds(findUsersByIdsReqDTO);

        if (!response.isSuccess() || Objects.isNull(response.getData()) || CollUtil.isEmpty(response.getData())) {
            return null;
        }

        return response.getData();
    }
}
