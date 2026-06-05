package com.dobby.xiaohashu.kv.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author 橡皮鸭
 * @version 1.0
 * @date 2026/4/11 13:06
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AddNoteContentReqDTO {

    @NotBlank(message = "笔记ID不能为空")
    private String uuid;

    @NotNull(message = "笔记内容不能为空")
    private String content;
}
