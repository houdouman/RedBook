package com.dobby.xiaohashu.kv.biz.domain.repository;
import com.dobby.xiaohashu.kv.biz.domain.dataobject.NoteContentDO;
import org.springframework.data.cassandra.repository.CassandraRepository;

import java.util.UUID;

/**
 * @author 橡皮鸭
 * @version 1.0
 * @date 2026/4/10 15:49
 * CassandraRepository: 是Spring Data Cassandra提供的一个泛型接口
 * 它为Cassandra数据库提供了CRUD和其他一些基本的操作方法
 */
public interface NoteContentRepository extends CassandraRepository<NoteContentDO, UUID> {

}
