package com.huaqiu.xiaohashu.note.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PublishNoteReqDto {

    private Integer type;

    private List<String> imgUris ;

    private String videoUris;

    private String title;

    private String content;

    private Long topicId;
}
