<template>
  <div class="layout">
    <el-header class="header">
      <div class="logo" @click="$router.push('/home')">笔记论坛</div>
      <el-menu mode="horizontal" :default-active="activeMenu" router class="menu">
        <el-menu-item index="/home">首页</el-menu-item>
        <el-menu-item index="/publish">发布</el-menu-item>
        <el-menu-item index="/search">搜索</el-menu-item>
        <el-menu-item index="/profile">我的</el-menu-item>
      </el-menu>
      <el-button link type="primary" @click="onLogout">退出登录</el-button>
    </el-header>
    <el-main class="main">
      <router-view />
    </el-main>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const activeMenu = computed(() => route.path)

async function onLogout() {
  await auth.logout()
  router.push('/login')
}
</script>

<style scoped>
.header { display: flex; align-items: center; gap: 16px; border-bottom: 1px solid #eee; }
.logo { font-size: 20px; font-weight: 700; color: #ff2442; cursor: pointer; }
.menu { flex: 1; border-bottom: none; }
.main { max-width: 960px; margin: 0 auto; }
</style>
