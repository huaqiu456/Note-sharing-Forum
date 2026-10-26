import { defineStore } from 'pinia'
import { ref } from 'vue'
import { loginByCode, loginByPassword, logout as apiLogout } from '../api/auth'

const TOKEN_KEY = "xiaohashu_token"

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string>(localStorage.getItem(TOKEN_KEY) || '')

  function setToken(value: string) {
    token.value = value
    localStorage.setItem(TOKEN_KEY, value)
  }

  async function loginWithCode(phone: string, code: string) {
    const t = await loginByCode(phone, code)
    setToken(t)
  }

  async function loginWithPassword(phone: string, password: string) {
    const t = await loginByPassword(phone, password)
    setToken(t)
  }

  async function logout() {
    try {
      await apiLogout()
    } catch (e) {
      // 忽略登出接口异常，本地仍要清理 token
    }
    token.value = ""
    localStorage.removeItem(TOKEN_KEY)
  }

  return { token, setToken, loginWithCode, loginWithPassword, logout }
})
