<template>
  <div>
    <el-card class="toolbar">
      <div class="toolbar-row">
        <h3>首页</h3>
        <el-button type="primary" @click="goPublish">发布笔记</el-button>
      </div>
    </el-card>

    <div v-loading="loading">
      <el-card v-for="item in list" :key="item.noteId" class="note-card" shadow="hover" @click="openDetail(item.noteId)">
        <div class="note-body">
          <img v-if="firstImage(item.imgUris)" :src="firstImage(item.imgUris)" class="cover" />
          <div class="info">
            <div class="title">{{ item.title || "无标题" }}</div>
            <div class="meta">{{ item.createTime }}</div>
          </div>
        </div>
      </el-card>

      <el-empty v-if="!loading && list.length === 0" description="暂无笔记" />
    </div>

    <div class="pager">
      <el-pagination
        v-model:current-page="page"
        :page-size="size"
        :total="total"
        layout="prev, pager, next, jumper"
        @current-change="onPageChange"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { listNotes } from '../api/note'
import type { NoteListItem } from '../types'

const router = useRouter()
const page = ref(1)
const size = 10
const total = ref(0)
const list = ref<NoteListItem[]>([])
const loading = ref(false)

function firstImage(imgUris?: string) {
  if (!imgUris) return ""
  return imgUris.split(',')[0] || ''
}

async function loadList() {
  loading.value = true
  try {
    const rsp = await listNotes(page.value, size)
    total.value = rsp.total
    list.value = rsp.list
  } catch (e) {
    list.value = []
  } finally {
    loading.value = false
  }
}

function onPageChange(p: number) {
  page.value = p
  loadList()
}

function openDetail(noteId: number) {
  router.push({ name: 'note-detail', params: { id: String(noteId) } })
}

function goPublish() { router.push('/publish') }

onMounted(loadList)
</script>

<style scoped>
.toolbar { margin-bottom: 12px; }
.toolbar-row { display: flex; justify-content: space-between; align-items: center; }
.note-card { margin-bottom: 10px; cursor: pointer; }
.note-body { display: flex; gap: 12px; align-items: center; }
.cover { width: 96px; height: 96px; object-fit: cover; border-radius: 6px; flex-shrink: 0; }
.info { flex: 1; }
.title { font-size: 16px; font-weight: 600; }
.meta { color: #999; font-size: 12px; margin-top: 8px; }
.pager { margin-top: 16px; display: flex; justify-content: center; }
</style>
