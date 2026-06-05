package com.dobby.xiaohashu.kv.dto.rsp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * @author 橡皮鸭
 * @version 1.0
 * @date 2026/4/11 13:31
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FindNoteContentRspDTO {

    /**
     * 笔记 ID
     */
    private UUID uuid;

    /**
     * 笔记内容
     */
    private String content;

}
