package com.dobby.xiaohashu.user.relation.biz.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @author 橡皮鸭
 * @version 1.0
 * @date 2026/4/17 10:29
 * 关注用户，将用户关注的相关数据发送给消费方
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FollowUserMqDTO {
    private Long userId;

    private Long followUserId;

    private LocalDateTime createTime;
}
