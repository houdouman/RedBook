package com.dobby.xiaohashu.note.biz.mode.vo;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author 橡皮鸭
 * @version 1.0
 * @date 2026/4/12 19:07
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UpdateNoteReqVO {

    @NotNull(message = "笔记 ID 不能为空")
    private Long id;

    @NotNull(message = "笔记类型不能为空")
    private Integer type;

    @NotNull(message = "可见类型不能为空")
    private Integer visible;

    private List<String> imgUris;

    private String videoUri;

    private String title;

    private String content;

    private Long topicId;
}
