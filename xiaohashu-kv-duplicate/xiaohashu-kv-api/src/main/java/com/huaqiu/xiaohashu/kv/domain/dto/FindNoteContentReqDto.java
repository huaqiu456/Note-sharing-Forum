package com.huaqiu.xiaohashu.kv.domain.dto;


import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @version: v1.0.0
 * @description: 查询笔记内容
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FindNoteContentReqDto {

    @NotBlank(message = "笔记 ID 不能为空")
    private String noteId;

}
