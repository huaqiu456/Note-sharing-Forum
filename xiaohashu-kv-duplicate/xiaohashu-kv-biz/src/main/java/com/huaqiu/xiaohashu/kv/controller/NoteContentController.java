package com.huaqiu.xiaohashu.kv.controller;

import com.huaqiu.framework.common.response.Response;
import com.huaqiu.xiaohashu.kv.Service.NoteContentService;
import com.huaqiu.xiaohashu.kv.domain.dto.AddNoteContentDto;
import com.huaqiu.xiaohashu.kv.domain.dto.DeleteNoteContentReqDto;
import com.huaqiu.xiaohashu.kv.domain.dto.FindNoteContentReqDto;
import com.huaqiu.xiaohashu.kv.domain.dto.FindNoteContentRspDto;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/kv")
public class NoteContentController {

    @Resource
    private  NoteContentService noteContentService;

    @PostMapping("/note/content/add")
    public Response<?> addNoteContent(@RequestBody AddNoteContentDto addNoteContentDto){
        System.out.println("tianjiabiji");
        return noteContentService.addNoteContent(addNoteContentDto);
    }
    @PostMapping(value = "/note/content/find")
    public Response<FindNoteContentRspDto> findNoteContent(@Validated @RequestBody FindNoteContentReqDto findNoteContentReqDTO) {
        return noteContentService.findNoteContent(findNoteContentReqDTO);
    }

    @PostMapping(value = "/note/content/delete")
    public Response<?> deleteNoteContent(@Validated @RequestBody DeleteNoteContentReqDto deleteNoteContentReqDTO) {
        return noteContentService.deleteNoteContent(deleteNoteContentReqDTO);
    }
}
