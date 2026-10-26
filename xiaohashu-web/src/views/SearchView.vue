<template>
  <div>
    <el-card>
      <div class="search-row">
        <el-input v-model="keyword" placeholder="搜索笔记关键词，如：成都美食" style="width: 360px" @keyup.enter="onSearch" />
        <el-button type="primary" :loading="loading" @click="onSearch">搜索</el-button>
      </div>

      <div v-if="searched && list.length === 0" class="empty">
        <el-empty description="没有搜到相关笔记" />
      </div>

      <div v-else class="result-list">
        <el-card v-for="item in list" :key="item.noteId" class="result-item" shadow="hover" @click="openDetail(item.noteId)">
          <div class="result-body">
            <img v-if="firstImage(item.imgUris)" :src="firstImage(item.imgUris)" class="cover" />
            <div class="info">
              <div class="title">{{ item.title || "无标题" }}</div>
              <div class="meta">{{ item.updateTime }}</div>
            </div>
          </div>
        </el-card>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { searchNote } from '../api/note'
import type { SearchNoteItem } from '../types'

const router = useRouter()
const keyword = ref('')
const loading = ref(false)
const searched = ref(false)
const list = ref<SearchNoteItem[]>([])

function firstImage(imgUris?: string) {
  if (!imgUris) return ""
  return imgUris.split(',')[0] || ''
}

async function onSearch() {
  if (!keyword.value.trim()) {
    ElMessage.warning('请输入关键词')
    return
  }
  loading.value = true
  searched.value = true
  try {
    list.value = await searchNote(keyword.value.trim())
  } catch (e) {
    list.value = []
  } finally {
    loading.value = false
  }
}

function openDetail(noteId: number) {
  router.push({ name: 'note-detail', params: { id: String(noteId) } })
}
</script>

<style scoped>
.search-row { display: flex; gap: 8px; margin-bottom: 12px; }
.result-list { display: flex; flex-direction: column; gap: 10px; }
.result-item { cursor: pointer; }
.result-body { display: flex; gap: 12px; align-items: center; }
.cover { width: 80px; height: 80px; object-fit: cover; border-radius: 6px; flex-shrink: 0; }
.info { flex: 1; }
.title { font-size: 15px; font-weight: 600; }
.meta { color: #999; font-size: 12px; margin-top: 6px; }
</style>
