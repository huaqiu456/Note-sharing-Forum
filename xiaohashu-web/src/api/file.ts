import { http } from '../utils/request'

export function uploadFile(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return http.post<string>('/file/upload', formData)
}

export interface DirectUploadReq {
  fileName: string
  fileSize: number
  contentType: string
}

export interface DirectUploadRsp {
  objectName: string
  uploadUrl: string
  fileUrl: string
  expiresAt: string
}

export interface InitMultipartReq {
  fileName: string
  fileSize: number
  contentType: string
  fileFingerprint: string
  partSize: number
  totalParts: number
}

export interface InitMultipartRsp {
  uploadId: string
  objectName: string
  partSize: number
  totalParts: number
  uploadedParts: number[]
  expiresAt: string
}

export interface PartUrl {
  partNumber: number
  url: string
  expiresAt: string
}

export interface MultipartStatusRsp {
  uploadId: string
  status: string
  partSize: number
  totalParts: number
  uploadedParts: number[]
  uploadedBytes: number
  totalBytes: number
  expiresAt: string
}

export interface UploadPart {
  partNumber: number
  etag: string
}

export function presignDirectUpload(data: DirectUploadReq) {
  return http.post<DirectUploadRsp>('/file/direct/presign', data)
}

export function initMultipartUpload(data: InitMultipartReq) {
  return http.post<InitMultipartRsp>('/file/multipart/init', data)
}

export function getPartUrls(uploadId: string, partNumbers: number[]) {
  return http.post<{ urls: PartUrl[] }>('/file/multipart/part-urls', {
    uploadId,
    partNumbers
  })
}

export function getMultipartStatus(uploadId: string) {
  return http.get<MultipartStatusRsp>(`/file/multipart/${encodeURIComponent(uploadId)}/status`)
}

export function completeMultipartUpload(uploadId: string, parts: UploadPart[]) {
  return http.post<{ url: string; objectName: string; size: number }>('/file/multipart/complete', {
    uploadId,
    parts
  })
}

export function abortMultipartUpload(uploadId: string) {
  return http.post<null>('/file/multipart/abort', { uploadId })
}
