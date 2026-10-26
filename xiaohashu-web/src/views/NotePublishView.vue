<template>
  <el-card>
    <h3>{{ isEdit ? "编辑笔记" : "发布笔记" }}</h3>
    <el-form label-width="80px">
      <el-form-item label="类型">
        <el-radio-group v-model="form.type">
          <el-radio :value="0">图文</el-radio>
          <el-radio :value="1">视频</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="标题">
        <el-input v-model="form.title" placeholder="请输入标题" />
      </el-form-item>
      <el-form-item label="正文">
        <el-input v-model="form.content" type="textarea" :rows="6" placeholder="请输入正文" />
      </el-form-item>
      <el-form-item label="话题ID">
        <el-input-number v-model="form.topicId" :min="0" placeholder="可选" />
      </el-form-item>

      <el-form-item v-if="form.type === 0" label="图片">
        <div>
          <el-upload :show-file-list="false" accept="image/*" :http-request="handleImageUpload">
            <el-button>上传图片</el-button>
          </el-upload>
          <div class="thumb-list">
            <img v-for="(url, i) in form.imgUris" :key="i" :src="url" class="thumb" />
          </div>
        </div>
      </el-form-item>

      <el-form-item v-if="form.type === 1" label="视频">
        <el-upload :show-file-list="false" accept="video/*" :http-request="handleVideoUpload">
          <el-button>上传视频</el-button>
        </el-upload>
        <video v-if="form.videoUri" :src="form.videoUri" controls class="video" />
      </el-form-item>

      <el-form-item v-if="uploadItems.length" label="上传任务">
        <div class="upload-task-list">
          <div v-for="item in uploadItems" :key="item.id" class="upload-task">
            <div class="upload-task__header">
              <span class="upload-task__name" :title="item.file.name">{{ item.file.name }}</span>
              <span class="upload-task__status" :class="`is-${item.status}`">
                {{ uploadStatusText(item.status) }}
              </span>
            </div>
            <el-progress
              :percentage="item.progress"
              :status="uploadProgressStatus(item.status)"
              :stroke-width="8"
              :show-text="false"
            />
            <div class="upload-task__meta">
              <span>{{ formatBytes(item.loaded) }} / {{ formatBytes(item.total) }}</span>
              <span>{{ item.progress }}%</span>
            </div>
            <div v-if="item.errorMessage" class="upload-task__error">{{ item.errorMessage }}</div>
            <div class="upload-task__actions">
              <!-- 小文件没有分片和 uploadId，因此不展示暂停/继续。 -->
              <el-button
                v-if="item.status === 'uploading' && item.file.size > DIRECT_UPLOAD_LIMIT"
                size="small"
                @click="pauseUpload(item)"
              >
                暂停
              </el-button>
              <el-button
                v-if="item.status === 'paused'"
                size="small"
                type="primary"
                @click="resumeUpload(item)"
              >
                继续
              </el-button>
              <el-button
                v-if="item.status === 'uploading' || item.status === 'paused'"
                size="small"
                type="danger"
                plain
                @click="cancelUpload(item)"
              >
                取消
              </el-button>
              <el-button
                v-if="item.status === 'success' || item.status === 'error' || item.status === 'cancelled'"
                size="small"
                text
                @click="removeUpload(item)"
              >
                移除
              </el-button>
            </div>
          </div>
        </div>
      </el-form-item>

      <el-form-item>
        <el-button
          type="primary"
          :loading="loading"
          :disabled="hasActiveUploads"
          @click="onSubmit"
        >
          {{ isEdit ? "保存" : "发布" }}
        </el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>

<script setup lang="ts">
import { computed, markRaw, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { publishNote, updateNote, getNoteDetail } from '../api/note'
import {
  createUploadTask,
  isUploadCancelledError,
  type UploadTask
} from '../api/uploadTask'

const DIRECT_UPLOAD_LIMIT = 10 * 1024 * 1024

type UploadKind = 'image' | 'video'
type UploadItemStatus = 'uploading' | 'paused' | 'success' | 'error' | 'cancelled'

interface UploadItem {
  id: number
  file: File
  kind: UploadKind
  progress: number
  loaded: number
  total: number
  status: UploadItemStatus
  errorMessage: string
  task: UploadTask | null
}

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const editId = ref<string>("")
const isEdit = computed(() => editId.value.length > 0)
const form = reactive({
  type: 0,
  title: "",
  content: "",
  topicId: 0,
  imgUris: [] as string[],
  videoUri: ""
})
const uploadItems = ref<UploadItem[]>([])
let nextUploadId = 1

const hasActiveUploads = computed(() =>
  uploadItems.value.some(item => item.status === 'uploading' || item.status === 'paused')
)

onMounted(async () => {
  const id = route.query.id
  if (id) {
    editId.value = String(id)
    const detail = await getNoteDetail(Number(id))
    form.type = detail.type || 0
    form.title = detail.title || ""
    form.content = detail.content || ""
    form.topicId = detail.topicId || 0
    form.imgUris = detail.imgUris ? detail.imgUris.split(",") : []
    form.videoUri = detail.videoUri || ""
  }
})

onBeforeUnmount(() => {
  // 离开发布页时，取消仍在进行的上传，避免后台残留 Multipart 会话。
  uploadItems.value.forEach(item => {
    if (item.status === 'uploading' || item.status === 'paused') {
      void item.task?.cancel()
    }
  })
})

function handleImageUpload(options: any) {
  return startUpload(options.file, 'image')
}

function handleVideoUpload(options: any) {
  return startUpload(options.file, 'video')
}

/**
 * 创建上传任务并同步到 UI。
 *
 * <p>小文件只展示进度和取消；
 * 大文件额外展示暂停和继续。
 */
async function startUpload(file: File, kind: UploadKind): Promise<string | void> {
  const item = reactive<UploadItem>({
    id: nextUploadId++,
    file,
    kind,
    progress: 0,
    loaded: 0,
    total: file.size,
    status: 'uploading',
    errorMessage: '',
    task: null
  })

  let task: UploadTask
  try {
    task = createUploadTask(file, progress => {
      item.loaded = progress.loaded
      item.total = progress.total
      item.progress = progress.percent
    })
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '文件不合法')
    return
  }

  item.task = markRaw(task)
  uploadItems.value.push(item)

  try {
    const url = await task.start()
    item.status = 'success'
    item.progress = 100
    item.loaded = item.total
    if (kind === 'image') {
      form.imgUris.push(url)
      ElMessage.success('图片上传成功')
    } else {
      form.videoUri = url
      ElMessage.success('视频上传成功')
    }
    return url
  } catch (error) {
    if (isUploadCancelledError(error)) {
      item.status = 'cancelled'
      item.errorMessage = '已取消'
      return
    }
    item.status = 'error'
    item.errorMessage = error instanceof Error ? error.message : '上传失败'
    ElMessage.error(item.errorMessage)
  }
}

function pauseUpload(item: UploadItem) {
  if (item.status !== 'uploading' || !item.task) {
    return
  }
  item.task.pause()
  item.status = 'paused'
}

function resumeUpload(item: UploadItem) {
  if (item.status !== 'paused' || !item.task) {
    return
  }
  item.status = 'uploading'
  item.errorMessage = ''
  // 最终状态由 startUpload 的 catch 统一处理。
  item.task.resume().catch(() => undefined)
}

async function cancelUpload(item: UploadItem) {
  if (!item.task || item.status === 'success' || item.status === 'cancelled' || item.status === 'error') {
    return
  }
  item.status = 'cancelled'
  item.errorMessage = '已取消'
  await item.task.cancel()
}

function removeUpload(item: UploadItem) {
  if (item.status === 'uploading' || item.status === 'paused') {
    void cancelUpload(item)
  }
  uploadItems.value = uploadItems.value.filter(upload => upload.id !== item.id)
}

function uploadStatusText(status: UploadItemStatus): string {
  const statusMap: Record<UploadItemStatus, string> = {
    uploading: '上传中',
    paused: '已暂停',
    success: '已完成',
    error: '上传失败',
    cancelled: '已取消'
  }
  return statusMap[status]
}

function uploadProgressStatus(status: UploadItemStatus): '' | 'success' | 'exception' | 'warning' {
  if (status === 'success') {
    return 'success'
  }
  if (status === 'error' || status === 'cancelled') {
    return 'exception'
  }
  if (status === 'paused') {
    return 'warning'
  }
  return ''
}

function formatBytes(bytes: number): string {
  if (bytes <= 0) {
    return '0 B'
  }
  const units = ['B', 'KB', 'MB', 'GB']
  let value = bytes
  let index = 0
  while (value >= 1024 && index < units.length - 1) {
    value /= 1024
    index++
  }
  return `${value.toFixed(index === 0 ? 0 : 1)} ${units[index]}`
}

async function onSubmit() {
  if (hasActiveUploads.value) {
    ElMessage.warning('请先完成或取消上传任务')
    return
  }
  loading.value = true
  try {
    if (isEdit.value) {
      await updateNote({
        id: Number(editId.value),
        type: form.type,
        title: form.title,
        content: form.content,
        topicId: form.topicId || undefined,
        imgUris: form.type === 0 ? form.imgUris : undefined,
        videoUri: form.type === 1 ? form.videoUri : undefined
      })
      ElMessage.success('更新成功')
      router.push({ name: 'note-detail', params: { id: editId.value } })
    } else {
      await publishNote({
        type: form.type,
        title: form.title,
        content: form.content,
        topicId: form.topicId || undefined,
        imgUris: form.type === 0 ? form.imgUris : undefined,
        videoUris: form.type === 1 ? form.videoUri : undefined
      })
      ElMessage.success('发布成功')
      router.push('/home')
    }
  } catch (e) {
    // 权限不足(403)等错误已由 axios 拦截器统一提示，这里避免未捕获的 Promise 异常
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.thumb-list { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 8px; }
.thumb { width: 96px; height: 96px; object-fit: cover; border-radius: 6px; }
.video { width: 320px; margin-top: 8px; }

.upload-task-list {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.upload-task {
  padding: 10px 12px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  background: #fafafa;
}

.upload-task__header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}

.upload-task__name {
  max-width: 360px;
  overflow: hidden;
  color: #303133;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.upload-task__status {
  flex-shrink: 0;
  color: #909399;
  font-size: 12px;
}

.upload-task__status.is-uploading { color: #409eff; }
.upload-task__status.is-paused { color: #e6a23c; }
.upload-task__status.is-success { color: #67c23a; }
.upload-task__status.is-error,
.upload-task__status.is-cancelled { color: #f56c6c; }

.upload-task__meta {
  display: flex;
  justify-content: space-between;
  margin-top: 4px;
  color: #909399;
  font-size: 12px;
}

.upload-task__error {
  margin-top: 4px;
  color: #f56c6c;
  font-size: 12px;
}

.upload-task__actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 8px;
}

.upload-task__actions :deep(.el-button + .el-button) {
  margin-left: 0;
}
</style>
