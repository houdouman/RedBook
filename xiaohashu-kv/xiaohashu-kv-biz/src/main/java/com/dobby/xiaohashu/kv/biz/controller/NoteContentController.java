package com.dobby.xiaohashu.kv.biz.controller;

import com.dobby.framework.common.response.Response;
import com.dobby.xiaohashu.kv.biz.domain.dataobject.NoteContentDO;
import com.dobby.xiaohashu.kv.biz.service.NoteContentService;
import com.dobby.xiaohashu.kv.dto.req.AddNoteContentReqDTO;
import com.dobby.xiaohashu.kv.dto.req.DeleteNoteContentReqDTO;
import com.dobby.xiaohashu.kv.dto.req.FindNoteContentReqDTO;
import com.dobby.xiaohashu.kv.dto.rsp.FindNoteContentRspDTO;
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
 * @date 2026/4/11 13:13
 */
@RestController
@RequestMapping("/kv")
@Slf4j
public class NoteContentController {
    @Resource
    private NoteContentService noteContentService;

    @PostMapping("/note/content/add")
    public Response<?> addNoteContent(@RequestBody @Validated AddNoteContentReqDTO addNoteContentReqDTO) {
        return noteContentService.addNoteContent(addNoteContentReqDTO);
    }

    @PostMapping(value = "/note/content/find")
    public Response<FindNoteContentRspDTO> findNoteContent(@Validated @RequestBody FindNoteContentReqDTO findNoteContentReqDTO) {
        return noteContentService.findNoteContent(findNoteContentReqDTO);
    }

    @PostMapping(value = "/note/content/delete")
    public Response<?> deleteNoteContent(@Validated @RequestBody DeleteNoteContentReqDTO deleteNoteContentReqDTO) {
        return noteContentService.deleteNoteContent(deleteNoteContentReqDTO);
    }

}
