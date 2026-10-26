package com.huaqiu.xiaohashu.note.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NoteListItemDto {

    private Long noteId;

    private String title;

    private String imgUris;

    private Long creatorId;

    private String createTime;

}
