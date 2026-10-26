package com.huaqiu.xiaohashu.note.mapper;

import com.huaqiu.xiaohashu.note.domain.po.Note;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface NoteMapper {
    int deleteByPrimaryKey(Long id);

    int insert(Note record);

    int insertSelective(Note record);

    Note selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(Note record);

    int updateByPrimaryKey(Note record);

    List<Long> selectIdsByPage(@Param("lastId") Long lastId, @Param("pageSize") Integer pageSize);

    // 首页列表：公开正常笔记，按发布时间倒序
    List<Long> selectPublicIdsByPage(@Param("offset") Integer offset, @Param("size") Integer size);

    Long countPublic();

    // 我的笔记列表：按 creatorId 过滤，发布时间倒序
    List<Long> selectMyIdsByPage(@Param("creatorId") Long creatorId, @Param("offset") Integer offset, @Param("size") Integer size);

    Long countMy(@Param("creatorId") Long creatorId);
}
