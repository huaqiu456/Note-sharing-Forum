package com.huaqiu.xiaohashu.note.controller;

import com.huaqiu.framework.biz.operationlog.aspect.ApiOperationLog;
import com.huaqiu.framework.common.response.Response;
import com.huaqiu.xiaohashu.note.domain.dto.GetNoteDetailReqDto;
import com.huaqiu.xiaohashu.note.domain.dto.NoteListRspDto;
import com.huaqiu.xiaohashu.note.domain.dto.PublishNoteReqDto;
import com.huaqiu.xiaohashu.note.domain.dto.SearchNoteReqDto;
import com.huaqiu.xiaohashu.note.domain.dto.SearchNoteRspDto;
import com.huaqiu.xiaohashu.note.domain.dto.UpdateNoteReqDto;
import com.huaqiu.xiaohashu.note.service.NoteService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/note")
@Slf4j
public class NoteController {

    @Resource
    private NoteService noteService;

    @PostMapping(value = "/publish")
    @ApiOperationLog(description = "笔记发布")
    public Response<?> publishNote(@Validated @RequestBody PublishNoteReqDto publishNoteReqDto) {
        return noteService.publishNote(publishNoteReqDto);
    }

    @PostMapping(value = "/detail")
    @ApiOperationLog(description = "获取笔记详情")
    public Response<?> getNoteDetail(@RequestBody GetNoteDetailReqDto getNoteDetailReqDto) {
        return noteService.getNoteDetail(getNoteDetailReqDto);
    }

    @PostMapping(value = "/update")
    @ApiOperationLog(description = "笔记修改")
    public Response<?> updateNote(@Validated @RequestBody UpdateNoteReqDto updateNoteReqDto) {
        return noteService.updateNote(updateNoteReqDto);
    }

    @GetMapping(value = "/mine")
    @ApiOperationLog(description = "我的笔记列表")
    public Response<NoteListRspDto> listMyNotes(@RequestParam(defaultValue = "1") int page,
                                                @RequestParam(defaultValue = "10") int size) {
        return noteService.listMyNotes(page, size);
    }

    @GetMapping(value = "/list")
    @ApiOperationLog(description = "首页笔记列表")
    public Response<NoteListRspDto> listNotes(@RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "10") int size) {
        return noteService.listNotes(page, size);
    }

    @PostMapping(value = "/search")
    @ApiOperationLog(description = "搜索笔记")
    public Response<List<SearchNoteRspDto>> searchNote(@Validated @RequestBody SearchNoteReqDto searchNoteReqDto) {
        return noteService.searchNote(searchNoteReqDto);
    }

}
