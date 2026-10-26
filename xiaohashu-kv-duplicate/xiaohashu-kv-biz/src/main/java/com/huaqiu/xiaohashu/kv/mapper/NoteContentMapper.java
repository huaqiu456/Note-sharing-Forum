package com.huaqiu.xiaohashu.kv.mapper;

import com.huaqiu.xiaohashu.kv.domain.po.NoteContent;
import org.springframework.data.cassandra.repository.CassandraRepository;

import java.util.UUID;

public interface NoteContentMapper extends CassandraRepository<NoteContent, UUID> {

}
