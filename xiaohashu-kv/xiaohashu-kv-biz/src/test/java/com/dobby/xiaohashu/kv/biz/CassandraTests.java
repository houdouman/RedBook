package com.dobby.xiaohashu.kv.biz;

import com.dobby.xiaohashu.kv.biz.domain.dataobject.NoteContentDO;
import com.dobby.xiaohashu.kv.biz.domain.repository.NoteContentRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.UUID;

/**
 * @author 橡皮鸭
 * @version 1.0
 * @date 2026/4/10 15:56
 */
@SpringBootTest
@Slf4j
public class CassandraTests {

    @Resource
    private NoteContentRepository noteContentRepository;

    /**
     * 测试插入数据
     */
    @Test
    void testInsert(){
        NoteContentDO noteContent = NoteContentDO.builder()
                .id(UUID.randomUUID())
                .content("测试插入内容")
                .build();

        noteContentRepository.insert(noteContent);
    }

    /**
     * 测试查看所有数据
     */
    @Test
    void searchAll(){
        List<NoteContentDO> noteList = noteContentRepository.findAll();
        noteList.forEach(System.out::println);
    }
}
