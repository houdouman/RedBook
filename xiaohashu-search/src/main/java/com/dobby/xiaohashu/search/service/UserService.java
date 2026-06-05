package com.dobby.xiaohashu.search.service;

import com.dobby.framework.common.response.PageResponse;
import com.dobby.xiaohashu.search.model.vo.SearchUserReqVO;
import com.dobby.xiaohashu.search.model.vo.SearchUserRspVO;

/**
 * @author 橡皮鸭
 * @version 1.0
 * @date 2026/4/26 16:37
 * 用户搜索业务
 */
public interface UserService {

    /**
     * 搜索用户
     * @param searchUserReqVO
     * @return
     */
    PageResponse<SearchUserRspVO> searchUser(SearchUserReqVO searchUserReqVO);
}

