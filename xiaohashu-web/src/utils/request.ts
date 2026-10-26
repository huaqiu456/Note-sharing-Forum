import axios, { type AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResponse } from '../types'

const TOKEN_KEY = "xiaohashu_token"

const instance = axios.create({
  baseURL: '',
  timeout: 15000
})

instance.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers.Authorization = token
  }
  return config
})

instance.interceptors.response.use(
  (response) => {
    const body = response.data as ApiResponse
    if (body && typeof body === "object" && "success" in body) {
      if (!body.success) {
        ElMessage.error(body.message || "请求失败")
        return Promise.reject(new Error(body.message || "请求失败"))
      }
      return body.data
    }
    return response.data
  },
  (error) => {
    const status = error && error.response ? error.response.status : 0
    if (status === 401) {
      ElMessage.error("请先登录")
      localStorage.removeItem(TOKEN_KEY)
      window.location.href = '/login'
    } else if (status === 403) {
      // 已登录但权限不足：只提示，不清 token、不跳登录页
      const msg = error && error.response && error.response.data && error.response.data.message
      ElMessage.error(msg || "权限不足")
    } else {
      const msg = error && error.response && error.response.data && error.response.data.message
      ElMessage.error(msg || error.message || "网络错误")
    }
    return Promise.reject(error)
  }
)

export const http = {
  get<T>(url: string, config?: AxiosRequestConfig) {
    return instance.get(url, config) as unknown as Promise<T>
  },
  post<T>(url: string, data?: unknown, config?: AxiosRequestConfig) {
    return instance.post(url, data, config) as unknown as Promise<T>
  }
}
