import { http } from '../utils/request'
import type { UserInfo } from '../types'

export function findUserById(userId: number) {
  return http.post<UserInfo>('/user/findById', { userId })
}

export function getCurrentUser() {
  return http.post<UserInfo>('/user/me')
}

export function updateProfile(formData: FormData) {
  return http.post<null>('/user/update', formData)
}
