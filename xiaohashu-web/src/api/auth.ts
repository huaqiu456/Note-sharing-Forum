import { http } from '../utils/request'

export function sendVerificationCode(phone: string) {
  return http.post<null>('/auth/verification/code/send', { phone })
}

export function loginByCode(phone: string, code: string) {
  return http.post<string>('/auth/login', { phone, code, type: 1 })
}

export function loginByPassword(phone: string, password: string) {
  return http.post<string>('/auth/login', { phone, password, type: 2 })
}

export function logout() {
  return http.post<null>('/auth/logout')
}

export function updatePassword(password: string) {
  return http.post<null>('/auth/password/update', { password })
}
