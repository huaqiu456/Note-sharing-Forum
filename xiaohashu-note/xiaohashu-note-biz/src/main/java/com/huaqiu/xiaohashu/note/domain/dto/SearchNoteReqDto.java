package com.huaqiu.xiaohashu.note.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SearchNoteReqDto {

    @NotBlank(message = "关键词不能为空")
    private String keyword;

    private Integer page = 1;

    private Integer size = 10;

    private Integer type;

}
