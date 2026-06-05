package com.dobby.xiaohashu.note.biz.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;

/**
 * @author 橡皮鸭
 * @version 1.0
 * @date 2026/4/23 09:59
 */
@Getter
@AllArgsConstructor
public enum NoteUncollectLuaResultEnum {
    // 布隆过滤器或ZSET不存在
    NOT_EXIST(-1L),
    // 笔记已收藏
    NOTE_COLLECTED(1L),
    // 笔记未收藏
    NOTE_NOT_COLLECTED(0L),
    ;

    private final Long code;

    /**
     * 根据类型 code 获取对应的枚举
     *
     * @param code
     * @return
     */
    public static NoteUncollectLuaResultEnum valueOf(Long code) {
        for (NoteUncollectLuaResultEnum noteUnCollectLuaResultEnum : NoteUncollectLuaResultEnum.values()) {
            if (Objects.equals(code, noteUnCollectLuaResultEnum.getCode())) {
                return noteUnCollectLuaResultEnum;
            }
        }
        return null;
    }
}
