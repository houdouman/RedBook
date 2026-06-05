package com.dobby.xiaohashu.count.biz.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;

/**
 * @author 橡皮鸭
 * @version 1.0
 * @date 2026/4/22 10:34
 */
@Getter
@AllArgsConstructor
public enum LikeUnlikeNoteTypeEnum {
    // 点赞
    LIKE(1),
    // 取消点赞
    UNLIKE(0),
    ;

    private final Integer code;

    public static LikeUnlikeNoteTypeEnum valueOf(Integer code) {
        for (LikeUnlikeNoteTypeEnum likeUnlikeNoteTypeEnum : LikeUnlikeNoteTypeEnum.values()) {
            if (Objects.equals(code, likeUnlikeNoteTypeEnum.getCode())) {
                return likeUnlikeNoteTypeEnum;
            }
        }
        return null;
    }

}
