package com.huaqiu.xiaohashu.note.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder

public class GetNoteDetailRspDto {

    // noteId 是号段模式下 Long 类型
    private Long noteId;

    private String title;

    private String content ;

    private LocalDateTime updateTime;

    private Integer visible;

    // 笔记类型 是图片笔记还是 视频笔记
    private Integer type;

    private String imgUris ;

    private String videoUri;

    // topic属于什么呢
    private Long topicId;

    private String topicName;

    // 创造者的相关 - user 微服务获得的
    private Long creatorId;

    private String creatorNicakname ;

    private String creatorAvatar;



}
