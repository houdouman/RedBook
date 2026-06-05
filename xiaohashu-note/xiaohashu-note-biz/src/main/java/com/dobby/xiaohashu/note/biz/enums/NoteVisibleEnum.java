package com.dobby.xiaohashu.note.biz.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;

/**
 * @author 橡皮鸭
 * @version 1.0
 * @date 2026/4/11 18:32
 */
@Getter
@AllArgsConstructor
public enum NoteVisibleEnum {

    PUBLIC(0), // 公开，所有人可见
    PRIVATE(1); // 仅自己可见

    private final Integer code;

    /**
     * 类型是否有效
     * @param code
     * @return
     */
    public static boolean isValid(Integer code) {
        for (NoteVisibleEnum noteVisibleEnum : NoteVisibleEnum.values()) {
            if (Objects.equals(code, noteVisibleEnum.getCode())) {
                return true;
            }
        }
        return false;
    }

}
