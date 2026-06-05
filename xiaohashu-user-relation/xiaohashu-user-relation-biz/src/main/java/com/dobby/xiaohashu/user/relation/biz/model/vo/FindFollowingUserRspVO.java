package com.dobby.xiaohashu.user.relation.biz.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author 橡皮鸭
 * @version 1.0
 * @date 2026/4/19 16:36
 * 查询关注列表
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FindFollowingUserRspVO {

    private Long userId;

    private String avatar;

    private String nickname;

    private String introduction;

}

