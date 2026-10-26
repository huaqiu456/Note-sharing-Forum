package com.huaqiu.xiaohashu.note.service.impl;

import com.huaqiu.framework.common.util.JsonUtils;
import com.huaqiu.xiaohashu.note.domain.po.Note;
import com.huaqiu.xiaohashu.note.service.NoteSearchService;
import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.SearchRequest;
import com.meilisearch.sdk.model.Searchable;
import com.meilisearch.sdk.model.Settings;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 笔记搜索服务实现
 * 设计说明：
 * - MySQL 是正文的唯一事实来源；Meilisearch 只做搜索索引，最终一致。
 * - 发布/更新笔记时同步索引；搜索时先查 Meilisearch 拿 noteId，再回 MySQL 取元数据。
 */
@SuppressWarnings("SpellCheckingInspection")
@Service
@Slf4j
public class NoteSearchServiceImpl implements NoteSearchService {

    private static final String INDEX_NAME = "note";

    @Resource
    private Client meiliClient;

    /**
     * 启动时确保索引存在（主键为 noteId），并配置可搜索/过滤/排序字段
     */
    @PostConstruct
    public void init() {
        try {
            meiliClient.createIndex(INDEX_NAME, "noteId");
            log.info("Meilisearch 索引 {} 创建成功", INDEX_NAME);
        } catch (Exception e) {
            log.warn("Meilisearch 索引 {} 已存在或创建失败: {}", INDEX_NAME, e.getMessage());
        }

        // 配置可搜索、可过滤、可排序字段（过滤字段不配置则 filter 无法使用）
        try {
            Settings settings = new Settings();
            settings.setSearchableAttributes(new String[]{"title", "content", "topicName"});
            settings.setFilterableAttributes(new String[]{"type", "visible", "status"});
            settings.setSortableAttributes(new String[]{"updateTime"});
            meiliClient.index(INDEX_NAME).updateSettings(settings);
            log.info("Meilisearch 索引 {} 设置完成", INDEX_NAME);
        } catch (Exception e) {
            log.error("Meilisearch 索引 {} 设置失败", INDEX_NAME, e);
        }
    }

    /**
     * 新增或更新笔记索引（Meilisearch 按主键 noteId 覆盖）
     */
    @Override
    public void indexNote(Note note) {
        try {
            Map<String, Object> doc = new HashMap<>();
            doc.put("noteId", note.getId());
            doc.put("title", note.getTitle());
            doc.put("content", note.getContent());
            doc.put("topicName", note.getTopicName());
            doc.put("type", note.getType());
            doc.put("visible", note.getVisible());
            doc.put("status", note.getStatus());
            doc.put("creatorId", note.getCreatorId());
            doc.put("updateTime", note.getUpdateTime() == null ? null : note.getUpdateTime().toString());

            meiliClient.index(INDEX_NAME)
                    .addDocuments(JsonUtils.toJsonString(Collections.singletonList(doc)));
        } catch (Exception e) {
            log.error("同步 Meilisearch 索引失败, noteId: {}", note.getId(), e);
        }
    }

    /**
     * 搜索，返回命中的 noteId 列表
     */
    @Override
    public List<Long> searchNoteIds(String keyword, int page, int size, Integer type) {
        try {
            SearchRequest request = new SearchRequest(keyword);
            request.setLimit(size);
            request.setOffset((page - 1) * size);
            String filter = "visible = 0 AND status = 1";
            if (type != null) {
                filter += " AND type = " + type;
            }
            request.setFilter(new String[]{filter});

            Searchable result = meiliClient.index(INDEX_NAME).search(request);
            List<Long> ids = new ArrayList<>();
            for (var hit : result.getHits()) {
                Object id = hit.get("noteId");
                if (id instanceof Number) {
                    // Meilisearch 返回的数字可能是 Double（如 1.0），先转 long 再装箱为 Long
                    ids.add(((Number) id).longValue());
                } else if (id != null) {
                    ids.add(Long.valueOf(id.toString()));
                }
            }
            return ids;
        } catch (Exception e) {
            log.error("Meilisearch 搜索失败, keyword: {}", keyword, e);
            return Collections.emptyList();
        }
    }

}
