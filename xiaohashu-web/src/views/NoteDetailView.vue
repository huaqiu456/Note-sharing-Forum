<template>
  <div v-if="detail">
    <el-card>
      <div class="head">
        <h3>{{ detail.title }}</h3>
        <el-button type="primary" link @click="goEdit">编辑</el-button>
      </div>
      <div v-if="images.length" class="img-list">
        <img v-for="(url, i) in images" :key="i" :src="url" class="img" />
      </div>
      <video v-if="detail.videoUri" :src="detail.videoUri" controls class="video" />
      <p class="content">{{ detail.content }}</p>
      <el-divider />
      <div class="meta">
        <span>话题：{{ detail.topicName || "无" }}</span>
        <span>更新时间：{{ detail.updateTime }}</span>
      </div>
    </el-card>
  </div>
  <el-empty v-else description="加载中或笔记不存在" />
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getNoteDetail } from '../api/note'
import type { NoteDetail } from '../types'

const route = useRoute()
const router = useRouter()
const detail = ref<NoteDetail | null>(null)
const images = computed(() => (detail.value && detail.value.imgUris ? detail.value.imgUris.split(",") : []))

onMounted(async () => {
  const id = Number(route.params.id)
  if (id) {
    detail.value = await getNoteDetail(id)
  }
})

function goEdit() {
  router.push({ name: 'publish', query: { id: String(detail.value && detail.value.noteId) } })
}
</script>

<style scoped>
.head { display: flex; justify-content: space-between; align-items: center; }
.img-list { display: flex; flex-wrap: wrap; gap: 8px; }
.img { width: 180px; height: 180px; object-fit: cover; border-radius: 6px; }
.video { width: 420px; max-width: 100%; }
.content { white-space: pre-wrap; color: #333; }
.meta { display: flex; gap: 16px; color: #888; font-size: 13px; }
</style>
