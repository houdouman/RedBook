package com.dobby.xiaohashu.note.biz.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;

/**
 * @author 橡皮鸭
 * @version 1.0
 * @date 2026/4/20 14:10
 */
@Getter
@AllArgsConstructor
public enum NoteLikeLuaResultEnum {
    // 布隆过滤器或ZSET不存在
    NOT_EXIST(-1L),
    // 笔记已点赞
    NOTE_LIKED(1L),
    // 笔记点赞成功
    NOTE_LIKE_SUCCESS(0L),
    ;

    private final Long code;

    /**
     * 根据类型 code 获取对应的枚举
     *
     * @param code
     * @return
     */
    public static NoteLikeLuaResultEnum valueOf(Long code) {
        for (NoteLikeLuaResultEnum noteLikeLuaResultEnum : NoteLikeLuaResultEnum.values()) {
            if (Objects.equals(code, noteLikeLuaResultEnum.getCode())) {
                return noteLikeLuaResultEnum;
            }
        }
        return null;
    }
}
