package com.dobby.xiaohashu.user.relation.biz.service;

import com.dobby.framework.common.response.PageResponse;
import com.dobby.framework.common.response.Response;
import com.dobby.xiaohashu.user.relation.biz.model.vo.*;

/**
 * @author 橡皮鸭
 * @version 1.0
 * @date 2026/4/14 17:40
 */
public interface RelationService {

    /**
     * 关注用户
     * @param followUserReqVO
     * @return
     */
    Response<?> follow(FollowUserReqVO followUserReqVO);

    /**
     * 取关用户
     * @param unfollowUserReqVO
     * @return
     */
    Response<?> unfollow(UnfollowUserReqVO unfollowUserReqVO);

    /**
     * 查询关注列表
     * @param findFollowingListReqVO
     * @return
     */
    PageResponse<FindFollowingUserRspVO> findFollowingList(FindFollowingListReqVO findFollowingListReqVO);

    /**
     * 查询粉丝列表
     * @param findFansListReqVO
     * @return
     */
    PageResponse<FindFansUserRspVO> findFansList(FindFansListReqVO findFansListReqVO);


}
