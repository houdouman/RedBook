package com.dobby.xiaohashu.kv.biz.service;

import com.dobby.framework.common.response.Response;
import com.dobby.xiaohashu.kv.dto.req.AddNoteContentReqDTO;
import com.dobby.xiaohashu.kv.dto.req.DeleteNoteContentReqDTO;
import com.dobby.xiaohashu.kv.dto.req.FindNoteContentReqDTO;
import com.dobby.xiaohashu.kv.dto.rsp.FindNoteContentRspDTO;

/**
 * @author 橡皮鸭
 * @version 1.0
 * @date 2026/4/11 13:09
 * 笔记内容
 */
public interface NoteContentService {

    /**
     * 添加笔记内容
     * @param addNoteContentReqDTO
     * @return
     */
    Response<?> addNoteContent(AddNoteContentReqDTO addNoteContentReqDTO);

    /**
     * 查找笔记内容
     * @param findNoteContentReqDTO
     * @return
     */
    Response<FindNoteContentRspDTO> findNoteContent(FindNoteContentReqDTO findNoteContentReqDTO);

    /**
     * 删除笔记内容
     * @param deleteNoteContentReqDTO
     * @return
     */
    Response<?> deleteNoteContent(DeleteNoteContentReqDTO deleteNoteContentReqDTO);

}
