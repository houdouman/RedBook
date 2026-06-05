package com.dobby.xiaohashu.user.dto.rsp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author 橡皮鸭
 * @version 1.0
 * @date 2026/4/11 19:50
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FindUserByIdRspDTO {

    /**
     * 用户id
     */
    private Long id;
    /**
     * 用户昵称
     */
    private String nickName;

    /**
     * 用户头像
     */
    private String avatar;

    /**
     * 简介
     */
    private String introduction;
}
