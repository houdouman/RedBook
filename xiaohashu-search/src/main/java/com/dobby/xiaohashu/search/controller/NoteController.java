package com.dobby.xiaohashu.search.controller;

import com.dobby.framework.biz.operationlog.aspect.ApiOperationLog;
import com.dobby.framework.common.response.PageResponse;
import com.dobby.xiaohashu.search.model.vo.SearchNoteReqVO;
import com.dobby.xiaohashu.search.model.vo.SearchNoteRspVO;
import com.dobby.xiaohashu.search.service.NoteService;
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
 * @date 2026/4/26 17:31
 */
@RestController
@RequestMapping("/search")
@Slf4j
public class NoteController {

    @Resource
    private NoteService noteService;

    @PostMapping("/note")
    @ApiOperationLog(description = "搜索笔记")
    public PageResponse<SearchNoteRspVO> searchNote(@RequestBody @Validated SearchNoteReqVO searchNoteReqVO) {
        return noteService.searchNote(searchNoteReqVO);
    }

}
