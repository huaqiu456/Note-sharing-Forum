export interface ApiResponse<T = unknown> {
  success: boolean
  message?: string | null
  errorCode?: string | null
  data: T
}

export interface UserInfo {
  userId?: number
  nickname?: string
  avatar?: string
  xiaohashuId?: string
  introduction?: string
  sex?: number
  birthday?: string
  backgroundImg?: string
}

export interface NoteDetail {
  noteId: number
  title?: string
  content?: string
  updateTime?: string
  visible?: number
  type?: number
  imgUris?: string
  videoUri?: string
  topicId?: number
  topicName?: string
  creatorId?: number
  creatorNicakname?: string
  creatorAvatar?: string
}

export interface SearchNoteItem {
  noteId: number
  title?: string
  imgUris?: string
  creatorId?: number
  creatorNickname?: string
  creatorAvatar?: string
  updateTime?: string
}

export interface NoteListItem {
  noteId: number
  title?: string
  imgUris?: string
  creatorId?: number
  createTime?: string
}

export interface NoteListRsp {
  total: number
  list: NoteListItem[]
}
