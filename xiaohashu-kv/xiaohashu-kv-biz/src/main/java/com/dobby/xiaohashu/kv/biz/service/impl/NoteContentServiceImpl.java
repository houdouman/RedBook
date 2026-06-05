package com.dobby.xiaohashu.kv.biz.service.impl;

import com.dobby.framework.common.exception.BizException;
import com.dobby.framework.common.response.Response;
import com.dobby.xiaohashu.kv.biz.domain.dataobject.NoteContentDO;
import com.dobby.xiaohashu.kv.biz.domain.repository.NoteContentRepository;
import com.dobby.xiaohashu.kv.biz.enums.ResponseCodeEnum;
import com.dobby.xiaohashu.kv.biz.service.NoteContentService;
import com.dobby.xiaohashu.kv.dto.req.AddNoteContentReqDTO;
import com.dobby.xiaohashu.kv.dto.req.DeleteNoteContentReqDTO;
import com.dobby.xiaohashu.kv.dto.req.FindNoteContentReqDTO;
import com.dobby.xiaohashu.kv.dto.rsp.FindNoteContentRspDTO;
import jakarta.annotation.Resource;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * @author 橡皮鸭
 * @version 1.0
 * @date 2026/4/11 13:09
 */
@Service
public class NoteContentServiceImpl implements NoteContentService {

    @Resource
    private NoteContentRepository noteContentRepository;

    /**
     * 添加笔记内容
     * @param addNoteContentReqDTO
     * @return
     */
    @Override
    public Response<?> addNoteContent(AddNoteContentReqDTO addNoteContentReqDTO) {
        String noteId = addNoteContentReqDTO.getUuid();
        String content = addNoteContentReqDTO.getContent();

        NoteContentDO noteContent = NoteContentDO.builder()
                .id(UUID.fromString(noteId))
                .content(content)
                .build();
        noteContentRepository.save(noteContent);
        return Response.success();
    }

    /**
     * 查找笔记内容
     * @param findNoteContentReqDTO
     * @return
     */
    @Override
    public Response<FindNoteContentRspDTO> findNoteContent(FindNoteContentReqDTO findNoteContentReqDTO) {
        String noteId = findNoteContentReqDTO.getUuid();
        //Optional<T>是一个容器对象，它可能包含也可能不包含非null的值,主要为了解决NPE（空指针异常）问题
        Optional<NoteContentDO> optional = noteContentRepository.findById(UUID.fromString(noteId));
        // 若笔记内容不存在
        if (!optional.isPresent()) {
            throw new BizException(ResponseCodeEnum.NOTE_CONTENT_NOT_FOUND);
        }

        NoteContentDO noteContentDO = optional.get();
        // 构建返参 DTO
        FindNoteContentRspDTO findNoteContentRspDTO = FindNoteContentRspDTO.builder()
                .uuid(noteContentDO.getId())
                .content(noteContentDO.getContent())
                .build();

        return Response.success(findNoteContentRspDTO);
    }

    @Override
    public Response<?> deleteNoteContent(DeleteNoteContentReqDTO deleteNoteContentReqDTO) {
        String noteId = deleteNoteContentReqDTO.getUuid();
        Optional<NoteContentDO> optional = noteContentRepository.findById(UUID.fromString(noteId));
        if (!optional.isPresent()) {
            throw new BizException(ResponseCodeEnum.NOTE_CONTENT_NOT_FOUND);
        }
        noteContentRepository.deleteById(UUID.fromString(noteId));

        return Response.success("删除成功~");
    }
}
