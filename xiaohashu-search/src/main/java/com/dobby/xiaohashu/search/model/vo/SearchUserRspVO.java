package com.dobby.xiaohashu.search.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author 橡皮鸭
 * @version 1.0
 * @date 2026/4/26 16:36
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SearchUserRspVO {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 小哈书ID
     */
    private String xiaohashuId;

    /**
     * 笔记发布总数
     */
    private Integer noteTotal;

    /**
     * 粉丝总数
     */
    private String fansTotal;

    /**
     * 昵称：关键词高亮
     */
    private String highlightNickname;

}
