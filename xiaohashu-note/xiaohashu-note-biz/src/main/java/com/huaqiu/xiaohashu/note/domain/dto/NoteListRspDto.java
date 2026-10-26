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
public class NoteListRspDto {

    private Long total;

    private List<NoteListItemDto> list;

}
