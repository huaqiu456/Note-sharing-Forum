package com.huaqiu.xiaohashu.kv.api;

import com.huaqiu.framework.common.response.Response;
import com.huaqiu.xiaohashu.kv.domain.dto.AddNoteContentDto;
import com.huaqiu.xiaohashu.kv.domain.dto.DeleteNoteContentReqDto;
import com.huaqiu.xiaohashu.kv.domain.dto.FindNoteContentReqDto;
import com.huaqiu.xiaohashu.kv.domain.dto.FindNoteContentRspDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@FeignClient(name = "xiaohashu-kv")
public interface NoteContentFeignClient {

    @PostMapping("/kv/note/content/add")
    Response<?> addNoteContent(@RequestBody AddNoteContentDto addNoteContentDto);

    @PostMapping("/kv/note/content/find")
    Response<FindNoteContentRspDto> searchNoteContent(@RequestBody FindNoteContentReqDto findNoteContentReqDto);

    @PostMapping(value = "/kv/note/content/delete")
    public Response<?> deleteNoteContent(@Validated @RequestBody DeleteNoteContentReqDto deleteNoteContentReqDTO);
}
