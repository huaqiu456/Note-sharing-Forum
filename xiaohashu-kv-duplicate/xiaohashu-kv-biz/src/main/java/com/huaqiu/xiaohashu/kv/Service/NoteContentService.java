package com.huaqiu.xiaohashu.kv.Service;

import com.huaqiu.framework.common.response.Response;
import com.huaqiu.xiaohashu.kv.domain.dto.AddNoteContentDto;
import com.huaqiu.xiaohashu.kv.domain.dto.DeleteNoteContentReqDto;
import com.huaqiu.xiaohashu.kv.domain.dto.FindNoteContentReqDto;
import com.huaqiu.xiaohashu.kv.domain.dto.FindNoteContentRspDto;

public interface NoteContentService {

    Response<?> addNoteContent(AddNoteContentDto addNoteContentDto);

    Response<FindNoteContentRspDto> findNoteContent(FindNoteContentReqDto findNoteContentReqDTO);

    Response<?> deleteNoteContent(DeleteNoteContentReqDto deleteNoteContentReqDto);

}
