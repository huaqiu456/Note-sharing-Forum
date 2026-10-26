<template>
  <div class="login-page">
    <el-card class="login-card">
      <h2 class="title">笔记论坛 · 登录 / 注册</h2>
      <el-tabs v-model="tab">
        <el-tab-pane label="验证码登录" name="code">
          <el-form label-width="80px">
            <el-form-item label="手机号">
              <el-input v-model="form.phone" placeholder="请输入手机号" />
            </el-form-item>
            <el-form-item label="验证码">
              <div class="code-row">
                <el-input v-model="form.code" placeholder="请输入验证码" />
                <el-button :disabled="countdown > 0" @click="onSendCode">
                  {{ countdown > 0 ? (countdown + "s") : "发送验证码" }}
                </el-button>
              </div>
            </el-form-item>
            <el-button type="primary" class="submit" :loading="loading" @click="onCodeLogin">登录</el-button>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="密码登录" name="password">
          <el-form label-width="80px">
            <el-form-item label="手机号">
              <el-input v-model="form.phone" placeholder="请输入手机号" />
            </el-form-item>
            <el-form-item label="密码">
              <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password />
            </el-form-item>
            <el-button type="primary" class="submit" :loading="loading" @click="onPasswordLogin">登录</el-button>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { sendVerificationCode } from '../api/auth'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()
const tab = ref('code')
const loading = ref(false)
const countdown = ref(0)
const form = reactive({ phone: "", code: "", password: "" })

async function onSendCode() {
  if (!form.phone) {
    ElMessage.warning("请输入手机号")
    return
  }
  await sendVerificationCode(form.phone)
  ElMessage.success("验证码已发送")
  countdown.value = 60
  const timer = setInterval(() => {
    countdown.value = countdown.value - 1
    if (countdown.value <= 0) clearInterval(timer)
  }, 1000)
}

async function onCodeLogin() {
  if (!form.phone || !form.code) {
    ElMessage.warning("请输入手机号和验证码")
    return
  }
  loading.value = true
  try {
    await auth.loginWithCode(form.phone, form.code)
    ElMessage.success('登录成功')
    router.push('/home')
  } finally {
    loading.value = false
  }
}

async function onPasswordLogin() {
  if (!form.phone || !form.password) {
    ElMessage.warning("请输入手机号和密码")
    return
  }
  loading.value = true
  try {
    await auth.loginWithPassword(form.phone, form.password)
    ElMessage.success('登录成功')
    router.push('/home')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page { display: flex; justify-content: center; align-items: center; min-height: 100vh; background: #f5f5f5; }
.login-card { width: 420px; }
.title { text-align: center; }
.code-row { display: flex; gap: 8px; width: 100%; }
.submit { width: 100%; }
</style>
