package com.dobby.xiaohashu.search.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;

/**
 * @author 橡皮鸭
 * @version 1.0
 * @date 2026/4/26 17:48
 * 笔记排序类型
 */
@Getter
@AllArgsConstructor
public enum NoteSortTypeEnum {

    // 最新
    LATEST(0),
    // 最新点赞
    MOST_LIKE(1),
    // 最多评论
    MOST_COMMENT(2),
    // 最多收藏
    MOST_COLLECT(3),
    ;

    private final Integer code;

    /**
     * 根据类型 code 获取对应的枚举
     *
     * @param code
     * @return
     */
    public static NoteSortTypeEnum valueOf(Integer code) {
        for (NoteSortTypeEnum noteSortTypeEnum : NoteSortTypeEnum.values()) {
            if (Objects.equals(code, noteSortTypeEnum.getCode())) {
                return noteSortTypeEnum;
            }
        }
        return null;
    }

}
