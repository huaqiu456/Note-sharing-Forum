package com.huaqiu.xiaohashu.note.domain.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder

public class Note {
    // 笔记ID，使用 Leaf 号段模式生成
    private Long id;

    private Long creatorId;

    private String title;

    // 话题相关信息
    private Long topicId;

    private String topicName;

    // 笔记类型：0 图文，1 视频
    private Integer type;

    private String imgUris;

    private String videoUri;

    // 可见性与置顶
    private Integer visible;
    private Boolean isTop;

    // 三个系统字段
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer status;

    /**
     * 笔记正文：直接存储在 MySQL t_note.content 中，
     * 已废弃原 Cassandra/KV 方案（不再使用 contentUuid）
     */
    private String content;

    private Boolean isContentEmpty;

}
