package com.dobby.xiaohashu.note.biz.rpc;

import com.dobby.framework.common.response.Response;
import com.dobby.xiaohashu.kv.api.KVFeignApi;
import com.dobby.xiaohashu.kv.dto.req.AddNoteContentReqDTO;
import com.dobby.xiaohashu.kv.dto.req.DeleteNoteContentReqDTO;
import com.dobby.xiaohashu.kv.dto.req.FindNoteContentReqDTO;
import com.dobby.xiaohashu.kv.dto.rsp.FindNoteContentRspDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * @author 橡皮鸭
 * @version 1.0
 * @date 2026/4/11 18:52
 */
@Component
public class KeyValueRpcService {

    @Resource
    private KVFeignApi keyValueFeignApi;

    /**
     * 保存笔记内容
     *
     * @param uuid
     * @param content
     * @return
     */
    public boolean saveNoteContent(String uuid, String content) {
        AddNoteContentReqDTO addNoteContentReqDTO = new AddNoteContentReqDTO();
        addNoteContentReqDTO.setUuid(uuid);
        addNoteContentReqDTO.setContent(content);

        Response<?> response = keyValueFeignApi.addNoteContent(addNoteContentReqDTO);

        if (Objects.isNull(response) || !response.isSuccess()) {
            return false;
        }

        return true;
    }

    /**
     * 删除笔记内容
     *
     * @param uuid
     * @return
     */
    public boolean deleteNoteContent(String uuid) {
        DeleteNoteContentReqDTO deleteNoteContentReqDTO = new DeleteNoteContentReqDTO();
        deleteNoteContentReqDTO.setUuid(uuid);

        Response<?> response = keyValueFeignApi.deleteNoteContent(deleteNoteContentReqDTO);

        if (Objects.isNull(response) || !response.isSuccess()) {
            return false;
        }

        return true;
    }

    /**
     * 获取笔记内容
     * @param contentUuid
     * @return
     */
    public String findNoteContent(String contentUuid) {
        FindNoteContentReqDTO findNoteContentReqDTO = new FindNoteContentReqDTO();
        findNoteContentReqDTO.setUuid(contentUuid);
        Response<FindNoteContentRspDTO> response = keyValueFeignApi.findNoteContent(findNoteContentReqDTO);
        if (Objects.isNull(response) || !response.isSuccess() || Objects.isNull(response.getData())) {
            return null;
        }
        return response.getData().getContent();
    }

}