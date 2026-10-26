package com.huaqiu.xiaohashu.kv.Service.impl;

import com.huaqiu.framework.common.exception.BizException;
import com.huaqiu.framework.common.response.Response;
import com.huaqiu.xiaohashu.kv.Service.NoteContentService;
import com.huaqiu.xiaohashu.kv.domain.dto.AddNoteContentDto;
import com.huaqiu.xiaohashu.kv.domain.dto.DeleteNoteContentReqDto;
import com.huaqiu.xiaohashu.kv.domain.dto.FindNoteContentReqDto;
import com.huaqiu.xiaohashu.kv.domain.dto.FindNoteContentRspDto;
import com.huaqiu.xiaohashu.kv.domain.po.NoteContent;
import com.huaqiu.xiaohashu.kv.enums.ResponseCodeEnum;
import com.huaqiu.xiaohashu.kv.mapper.NoteContentMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class NoteContentServiceImpl implements NoteContentService {

    @Resource
    private NoteContentMapper noteContentMapper;

    @Override
    public Response<?> addNoteContent(AddNoteContentDto addNoteContentDto) {
        // 笔记 ID
        String noteId = addNoteContentDto.getNoteId();
        // 笔记内容
        String content = addNoteContentDto.getContent();

        // 构建数据库 DO 实体类
        NoteContent nodeContent = NoteContent.builder()
                .id(UUID.fromString(noteId))
                .content(content)
                .build();

        // 插入数据
        noteContentMapper.save(nodeContent);

        return Response.success();
    }

    @Override
    public Response<FindNoteContentRspDto> findNoteContent(FindNoteContentReqDto findNoteContentReqDTO) {
        // 笔记 ID
        String noteId = findNoteContentReqDTO.getNoteId();
        // 根据笔记 ID 查询笔记内容
        Optional<NoteContent> optional = noteContentMapper.findById(UUID.fromString(noteId));

        // 若笔记内容不存在
        if (!optional.isPresent()) {
            throw new BizException(ResponseCodeEnum.NOTE_CONTENT_NOT_FOUND);
        }

        NoteContent noteContentDO = optional.get();
        // 构建返参 DTO
        FindNoteContentRspDto findNoteContentRspDTO = FindNoteContentRspDto.builder()
                .noteId(noteContentDO.getId())
                .content(noteContentDO.getContent())
                .build();

        return Response.success(findNoteContentRspDTO);
    }

    @Override
    public Response<?> deleteNoteContent(DeleteNoteContentReqDto deleteNoteContentReqDto) {
        // 笔记 ID
        String noteId = deleteNoteContentReqDto.getNoteId();
        // 删除笔记内容
        noteContentMapper.deleteById(UUID.fromString(noteId));

        return Response.success();
    }
}
