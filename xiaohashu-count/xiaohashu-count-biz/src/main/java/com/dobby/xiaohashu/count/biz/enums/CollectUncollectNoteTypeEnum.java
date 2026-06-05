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
public enum CollectUncollectNoteTypeEnum {
    // 点赞
    COLLECT(1),
    // 取消点赞
    UNCOLLECT(0),
    ;

    private final Integer code;

    public static CollectUncollectNoteTypeEnum valueOf(Integer code) {
        for (CollectUncollectNoteTypeEnum collectUncollectNoteTypeEnum : CollectUncollectNoteTypeEnum.values()) {
            if (Objects.equals(code, collectUncollectNoteTypeEnum.getCode())) {
                return collectUncollectNoteTypeEnum;
            }
        }
        return null;
    }

}
