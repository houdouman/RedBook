package com.dobby.xiaohashu.user.relation.biz.domain.mapper;

import com.dobby.xiaohashu.user.relation.biz.domain.dataobject.FollowDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface FollowDOMapper {
    int deleteByPrimaryKey(Long id);

    int insert(FollowDO record);

    int insertSelective(FollowDO record);

    FollowDO selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(FollowDO record);

    int updateByPrimaryKey(FollowDO record);

    List<FollowDO> selectByUserId(Long userId);

    /**
     * 查询关注用户列表
     * @param userId
     * @return
     */
    List<FollowDO> selectAllByUserId(Long userId);

    int deleteByUserIdAndFollowingUserId(@Param("userId") Long userId,
                                         @Param("unfollowUserId") Long unfollowUserId);

    /**
     * 查询记录总数
     *
     * @param userId
     * @return
     */
    long selectCountByUserId(Long userId);

    /**
     * 分页查询
     * @param userId
     * @param offset
     * @param limit
     * @return
     */
    List<FollowDO> selectPageListByUserId(@Param("userId") Long userId,
                                             @Param("offset") long offset,
                                             @Param("limit") long limit);

}