package com.dobby.xiaohashu.kv.biz.domain.dataobject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.util.UUID;

/**
 * @author 橡皮鸭
 * @version 1.0
 * @date 2026/4/10 15:49
 * 对应数据库中note_content表
 */
@Table("note_content")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NoteContentDO {
    @PrimaryKey("id")
    private UUID id;
    private String content;
}
