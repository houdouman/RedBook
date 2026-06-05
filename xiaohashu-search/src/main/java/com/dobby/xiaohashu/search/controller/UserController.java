package com.dobby.xiaohashu.search.controller;

import com.dobby.framework.biz.operationlog.aspect.ApiOperationLog;
import com.dobby.framework.common.response.PageResponse;
import com.dobby.xiaohashu.search.model.vo.SearchUserReqVO;
import com.dobby.xiaohashu.search.model.vo.SearchUserRspVO;
import com.dobby.xiaohashu.search.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author 橡皮鸭
 * @version 1.0
 * @date 2026/4/26 17:13
 */
@RestController
@RequestMapping("/search")
@Slf4j
public class UserController {

    @Resource
    private UserService userService;

    @PostMapping("/user")
    @ApiOperationLog(description = "搜索用户")
    public PageResponse<SearchUserRspVO> searchUser(@RequestBody @Validated SearchUserReqVO searchUserReqVO) {
        return userService.searchUser(searchUserReqVO);
    }

}
