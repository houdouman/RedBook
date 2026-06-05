package com.dobby.xiaohashu.note.biz.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author 橡皮鸭
 * @version 1.0
 * @date 2026/4/23 09:56
 */
@Getter
@AllArgsConstructor
public enum CollectUnCollectNoteTypeEnum {
    //收藏
    COLLECT(1),
    //取消收藏
    UNCOLLECT(0),
    ;

    private final Integer code;
}
