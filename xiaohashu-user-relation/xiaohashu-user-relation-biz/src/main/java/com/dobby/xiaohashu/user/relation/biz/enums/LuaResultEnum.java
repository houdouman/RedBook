package com.dobby.xiaohashu.user.relation.biz.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author 橡皮鸭
 * @version 1.0
 * @date 2026/4/15 11:12
 */
@Getter
@AllArgsConstructor
public enum LuaResultEnum {

    // ZSET 不存在
    ZSET_NOT_EXIST(-1L),
    // 关注已达到上限
    FOLLOW_LIMIT(-2L),
    // 已经关注了该用户
    ALREADY_FOLLOWED(-3L),
    // 关注成功
    FOLLOW_SUCCESS(0L),
    // 未关注该用户
    NOT_FOLLOWED(-4L),
    ;

    private final Long code;

    /**
     * 根据类型code获取对应枚举
     * @param code
     * @return
     */
    public static LuaResultEnum valueOf(Long code) {
        for(LuaResultEnum value : LuaResultEnum.values()){
            if(value.getCode().equals(code)){
                return value;
            }
        }
        return null;
    }
}
