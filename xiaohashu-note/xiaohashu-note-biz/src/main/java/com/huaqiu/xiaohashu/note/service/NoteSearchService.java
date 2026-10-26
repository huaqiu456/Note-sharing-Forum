package com.huaqiu.xiaohashu.note.service;

import com.huaqiu.xiaohashu.note.domain.po.Note;

import java.util.List;

/**
 * 笔记搜索服务接口
 * 职责：
 * 1. 新增/更新笔记时同步 Meilisearch 索引。
 * 2. 删除笔记时同步删除索引。
 * 3. 根据关键词搜索，返回命中的 noteId 列表。
 */
@SuppressWarnings("SpellCheckingInspection")
public interface NoteSearchService {

    /**
     * 新增或更新笔记索引（Meilisearch 按主键 noteId 覆盖）
     */
    void indexNote(Note note);

    /**
     * 搜索，返回命中的 noteId 列表
     */
    List<Long> searchNoteIds(String keyword, int page, int size, Integer type);

}
