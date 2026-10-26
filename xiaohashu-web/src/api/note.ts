import { http } from '../utils/request'
import type { NoteDetail, NoteListRsp, SearchNoteItem } from '../types'

export interface PublishNoteReq {
  type: number
  imgUris?: string[]
  videoUris?: string
  title?: string
  content?: string
  topicId?: number
}

export interface UpdateNoteReq {
  id: number
  type: number
  imgUris?: string[]
  videoUri?: string
  title?: string
  content?: string
  topicId?: number
}

export function publishNote(data: PublishNoteReq) {
  return http.post<null>('/note/publish', data)
}

export function getNoteDetail(noteId: number) {
  return http.post<NoteDetail>('/note/detail', { noteId })
}

export function updateNote(data: UpdateNoteReq) {
  return http.post<null>('/note/update', data)
}

export function listMyNotes(page = 1, size = 10) {
  return http.get<NoteListRsp>('/note/mine', { params: { page, size } })
}

export function listNotes(page = 1, size = 10) {
  return http.get<NoteListRsp>('/note/list', { params: { page, size } })
}

export function searchNote(keyword: string, page = 1, size = 10, type?: number) {
  return http.post<SearchNoteItem[]>('/note/search', { keyword, page, size, type })
}
