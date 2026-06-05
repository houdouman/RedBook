package com.dobby.xiaohashu.note.biz.enums;

import com.dobby.framework.common.exception.BaseExceptionInterface;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author 橡皮鸭
 * @version 1.0
 * @date 2026/4/11 18:29
 */
@Getter
@AllArgsConstructor
public enum ResponseCodeEnum implements BaseExceptionInterface {
    // ----------- 通用异常状态码 -----------
    SYSTEM_ERROR("NOTE-10000", "出错啦，后台小哥正在努力修复中..."),
    PARAM_NOT_VALID("NOTE-10001", "参数错误"),

    // ----------- 业务异常状态码 -----------
    NOTE_TYPE_ERROR("NOTE-20000", "未知的笔记类型"),
    NOTE_VISIBLE_NonVALID("NOTE-20001", "笔记可见类型设置失败"),
    NOTE_PUBLISH_FAIL("NOTE-20002", "笔记发布失败"),
    NOTE_NOT_FOUND("NOTE-20003", "笔记不存在"),
    NOTE_PRIVATE("NOTE-20004", "作者已将该笔记设置为仅自己可见"),
    NOTE_UPDATE_FAIL("NOTE-20005", "笔记更新失败"),
    TOPIC_NOT_FOUND("NOTE-20006", "话题不存在"),
    NOTE_CANT_VISIBLE_ONLY_ME("NOTE-20007", "此笔记无法修改为仅自己可见"),
    NOTE_CANT_OPERATE("NOTE-20008", "您无法操作该笔记"),
    NOTE_ALREADY_LIKED("NOTE-20009", "您已经点赞过该笔记"),
    NOTE_NOT_LIKED("NOTE-20010", "您未点赞该篇笔记，无法取消点赞"),
    NOTE_ALREADY_COLLECTED("NOTE-20011", "您已经收藏过该笔记"),
    NOTE_NOT_COLLECTED("NOTE-20012", "您未收藏该篇笔记，无法取消收藏"),



    ;

    private final String errorCode;
    private final String errorMessage;
}
