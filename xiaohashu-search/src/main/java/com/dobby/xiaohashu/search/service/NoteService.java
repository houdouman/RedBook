package com.dobby.xiaohashu.search.service;

import com.dobby.framework.common.response.PageResponse;
import com.dobby.xiaohashu.search.model.vo.SearchNoteReqVO;
import com.dobby.xiaohashu.search.model.vo.SearchNoteRspVO;

/**
 * @author 橡皮鸭
 * @version 1.0
 * @date 2026/4/26 17:26
 * 笔记搜索业务
 */
public interface NoteService {

    /**
     * 搜索笔记
     * @param searchNoteReqVO
     * @return
     */
    PageResponse<SearchNoteRspVO> searchNote(SearchNoteReqVO searchNoteReqVO);
}
