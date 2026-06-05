package com.dobby.xiaohashu.user.biz.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author 橡皮鸭
 * @version 1.0
 * @date 2026/4/3 10:48
 */
@Getter
@AllArgsConstructor
public enum SexEnum {
    WOMAN(0),
    MAN(1);
    private final Integer value;

    public static boolean isValid(Integer value){
        for(SexEnum loginTypeEnum : SexEnum.values()){
            if(loginTypeEnum.getValue().equals(value)){
                return true;
            }
        }
        return false;
    }

}
