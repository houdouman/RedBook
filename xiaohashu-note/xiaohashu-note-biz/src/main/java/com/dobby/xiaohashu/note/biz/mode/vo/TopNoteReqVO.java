package com.dobby.xiaohashu.note.biz.mode.vo;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author 橡皮鸭
 * @version 1.0
 * @date 2026/4/14 17:06
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TopNoteReqVO {

    @NotNull(message = "笔记 ID 不能为空")
    private Long id;

    @NotNull(message = "置顶状态不能为空")
    private Boolean isTop;

}
