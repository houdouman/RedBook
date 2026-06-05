package com.dobby.xiaohashu.kv.biz.controller;

import com.dobby.framework.biz.operationlog.aspect.ApiOperationLog;
import com.dobby.framework.common.response.Response;
import com.dobby.xiaohashu.kv.biz.service.CommentContentService;
import com.dobby.xiaohashu.kv.dto.req.BatchAddCommentContentReqDTO;
import com.dobby.xiaohashu.kv.dto.req.BatchFindCommentContentReqDTO;
import com.dobby.xiaohashu.kv.dto.req.DeleteCommentContentReqDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author: 犬小哈
 * @date: 2024/4/4 13:22
 * @version: v1.0.0
 * @description: 评论内容
 **/
@RestController
@RequestMapping("/kv")
@Slf4j
public class CommentContentController {

    @Resource
    private CommentContentService commentContentService;

    @PostMapping(value = "/comment/content/batchAdd")
    @ApiOperationLog(description = "批量存储评论内容")
    public Response<?> batchAddCommentContent(@Validated @RequestBody BatchAddCommentContentReqDTO batchAddCommentContentReqDTO) {
        return commentContentService.batchAddCommentContent(batchAddCommentContentReqDTO);
    }

    @PostMapping(value = "/comment/content/batchFind")
    @ApiOperationLog(description = "批量查询评论内容")
    public Response<?> batchFindCommentContent(@Validated @RequestBody BatchFindCommentContentReqDTO batchFindCommentContentReqDTO) {
        return commentContentService.batchFindCommentContent(batchFindCommentContentReqDTO);
    }

    @PostMapping(value = "/comment/content/delete")
    @ApiOperationLog(description = "删除评论内容")
    public Response<?> deleteCommentContent(@Validated @RequestBody DeleteCommentContentReqDTO deleteCommentContentReqDTO) {
        return commentContentService.deleteCommentContent(deleteCommentContentReqDTO);
    }
}
