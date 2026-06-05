package com.dobby.xiaohashu.user.biz.service;

import com.dobby.framework.common.response.Response;
import com.dobby.xiaohashu.user.biz.model.vo.UpdatePasswordReqVO;
import com.dobby.xiaohashu.user.biz.model.vo.UpdateUserInfoReqVO;
import com.dobby.xiaohashu.user.dto.req.FindUserByIdReqDTO;
import com.dobby.xiaohashu.user.dto.req.FindUserByPhoneReqDTO;
import com.dobby.xiaohashu.user.dto.req.FindUsersByIdsReqDTO;
import com.dobby.xiaohashu.user.dto.req.RegisterUserReqDTO;
import com.dobby.xiaohashu.user.dto.rsp.FindUserByIdRspDTO;
import com.dobby.xiaohashu.user.dto.rsp.FindUserByPhoneRspDTO;

import java.util.List;

/**
 * @author 橡皮鸭
 * @version 1.0
 * @date 2026/4/3 10:44
 */
public interface UserService {

    /**
     * 更新用户信息
     * @param updateUserInfoReqVO
     * @return
     */
    Response<?> updateUserInfo(UpdateUserInfoReqVO updateUserInfoReqVO);

    /**
     * 用户注册
     * @param registerUserReqDTO
     * @return
     */
    Response<Long> register(RegisterUserReqDTO registerUserReqDTO);

    /**
     * 更新密码
     * @param updatePasswordReqVO
     * @return
     */
    Response<?> updatePassword(UpdatePasswordReqVO updatePasswordReqVO);

    /**
     * 根据手机号查询用户信息
     * @param findUserByPhoneReqDTO
     * @return
     */
    Response<FindUserByPhoneRspDTO> findByPhone(FindUserByPhoneReqDTO findUserByPhoneReqDTO);

    /**
     * 根据id查询用户信息
     * @param findUserByIdReqDTO
     * @return
     */
    Response<FindUserByIdRspDTO> findById(FindUserByIdReqDTO findUserByIdReqDTO);

    /**
     * 删除本地用户缓存
     * @param userId
     */
    void deleteUserLocalCache(Long userId);

    /**
     * 批量根据用户 ID 查询用户信息
     *
     * @param findUsersByIdsReqDTO
     * @return
     */
    Response<List<FindUserByIdRspDTO>> findByIds(FindUsersByIdsReqDTO findUsersByIdsReqDTO);


}
