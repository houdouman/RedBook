package com.dobby.xiaohashu.user.dto.req;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author 橡皮鸭
 * @version 1.0
 * @date 2026/4/11 19:49
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FindUserByIdReqDTO {

    /**
     * 用户id
     */
    @NotNull(message = "用户id不能为空")
    private Long id;
}
