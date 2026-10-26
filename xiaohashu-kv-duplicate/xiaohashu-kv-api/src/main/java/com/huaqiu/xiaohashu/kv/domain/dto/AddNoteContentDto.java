package com.huaqiu.xiaohashu.kv.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AddNoteContentDto {

    @NotNull(message = "笔记 ID 不能为空")
    private String noteId;

    @NotBlank(message = "笔记内容不能为空")
    private String content;

}
