package com.huaqiu.xiaohashu.note.service;

import com.huaqiu.framework.common.response.Response;
import com.huaqiu.xiaohashu.note.domain.dto.GetNoteDetailReqDto;
import com.huaqiu.xiaohashu.note.domain.dto.NoteListRspDto;
import com.huaqiu.xiaohashu.note.domain.dto.PublishNoteReqDto;
import com.huaqiu.xiaohashu.note.domain.dto.SearchNoteReqDto;
import com.huaqiu.xiaohashu.note.domain.dto.SearchNoteRspDto;
import com.huaqiu.xiaohashu.note.domain.dto.UpdateNoteReqDto;

import java.util.List;

public interface NoteService {

    Response<?> publishNote(PublishNoteReqDto publishNoteReqDto);

    Response<?> getNoteDetail(GetNoteDetailReqDto getNoteDetailReqDto);

    Response<?> updateNote(UpdateNoteReqDto updateNoteReqDto);

    Response<List<SearchNoteRspDto>> searchNote(SearchNoteReqDto searchNoteReqDto);

    Response<NoteListRspDto> listNotes(int page, int size);

    Response<NoteListRspDto> listMyNotes(int page, int size);

    void deleteNoteLocalCache(Long noteId);

}
