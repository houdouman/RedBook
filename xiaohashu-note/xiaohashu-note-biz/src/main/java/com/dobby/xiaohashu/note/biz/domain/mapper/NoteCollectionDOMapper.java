package com.dobby.xiaohashu.note.biz.domain.mapper;


import com.dobby.xiaohashu.note.biz.domain.dataobject.NoteCollectionDO;
import com.dobby.xiaohashu.note.biz.domain.dataobject.NoteLikeDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface NoteCollectionDOMapper {
    int deleteByPrimaryKey(Long id);

    int insert(NoteCollectionDO record);

    int insertSelective(NoteCollectionDO record);

    NoteCollectionDO selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(NoteCollectionDO record);

    int updateByPrimaryKey(NoteCollectionDO record);

    int selectCountByUserIdAndNoteId(@Param("userId") Long userId, @Param("noteId") Long noteId);

    List<NoteCollectionDO> selectByUserId(@Param("userId") Long userId);

    int selectNoteIsCollected(@Param("userId") Long userId, @Param("noteId") Long noteId);

    List<NoteCollectionDO> selectCollectdByUserIdAndLimit(@Param("userId") Long userId, @Param("limit")  int limit);

    /**
     * 新增笔记收藏记录，若已存在，则更新笔记收藏记录
     * @param noteCollectionDO
     * @return
     */
    int insertOrUpdate(NoteCollectionDO noteCollectionDO);

    /**
     * 取消收藏
     * @param noteCollectionDO
     * @return
     */
    int update2UnCollectByUserIdAndNoteId(NoteCollectionDO noteCollectionDO);
}