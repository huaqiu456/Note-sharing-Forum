<template>
  <div>
    <!-- 个人主页背景横幅：背景图取自 t_user.background_img，未设置时用默认渐变 -->
    <div class="profile-banner" :style="bannerStyle">
      <div class="banner-mask"></div>
      <div class="banner-content">
        <el-avatar :size="72" :src="user?.avatar || ''" />
        <div class="info">
          <div class="nickname">{{ user?.nickname || "未设置昵称" }}</div>
          <div class="meta">用户ID：{{ user?.userId }} · 笔记论坛号：{{ user?.xiaohashuId || "无" }}</div>
          <div class="meta">简介：{{ user?.introduction || "这个人很懒，什么都没写" }}</div>
        </div>
        <el-button type="primary" @click="openEdit">编辑资料</el-button>
        <el-button @click="pwdVisible = true">修改密码</el-button>
      </div>
    </div>

    <h3>我的笔记</h3>
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
      <el-empty v-if="!loading && list.length === 0" description="还没有发布过笔记" />
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

    <el-dialog v-model="editVisible" title="编辑资料" width="480px">
      <el-form label-width="80px">
        <el-form-item label="昵称">
          <el-input v-model="editForm.nickname" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="editForm.sex">
            <el-radio :value="0">女</el-radio>
            <el-radio :value="1">男</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="生日">
          <el-date-picker v-model="editForm.birthday" type="date" value-format="YYYY-MM-DD" placeholder="选择生日" />
        </el-form-item>
        <el-form-item label="简介">
          <el-input v-model="editForm.introduction" type="textarea" :rows="3" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="头像">
          <el-upload :auto-upload="false" :show-file-list="false" accept="image/*" :on-change="onAvatarChange">
            <el-button>选择头像</el-button>
          </el-upload>
        </el-form-item>
        <el-form-item label="背景图">
          <el-upload :auto-upload="false" :show-file-list="false" accept="image/*" :on-change="onBackgroundChange">
            <el-button>选择背景图</el-button>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSave">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="pwdVisible" title="修改密码" width="400px">
      <el-form label-width="80px">
        <el-form-item label="新密码">
          <el-input v-model="pwdForm.password" type="password" show-password placeholder="请输入新密码" />
        </el-form-item>
        <el-form-item label="确认密码">
          <el-input v-model="pwdForm.confirm" type="password" show-password placeholder="请再次输入新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdVisible = false">取消</el-button>
        <el-button type="primary" :loading="pwdSaving" @click="onSavePassword">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getCurrentUser, updateProfile } from '../api/user'
import { updatePassword } from '../api/auth'
import { listMyNotes } from '../api/note'
import type { UserInfo, NoteListItem } from '../types'

const router = useRouter()
const user = ref<UserInfo | null>(null)
const page = ref(1)
const size = 10
const total = ref(0)
const list = ref<NoteListItem[]>([])
const loading = ref(false)

const editVisible = ref(false)
const saving = ref(false)
const editForm = reactive({ nickname: "", sex: 0, birthday: "", introduction: "" })
const avatarFile = ref<File | null>(null)
const backgroundFile = ref<File | null>(null)
const pwdVisible = ref(false)
const pwdSaving = ref(false)
const pwdForm = reactive({ password: "", confirm: "" })

/**
 * 背景横幅样式：
 * - 有背景图时用 backgroundImage 铺满（cover + 居中裁切，避免拉伸变形）
 * - 无背景图时回退默认渐变底色，保证布局不塌陷
 */
const bannerStyle = computed(() => {
  const bg = user.value?.backgroundImg
  if (!bg) {
    return { background: 'linear-gradient(135deg, #ffdde1, #ee9ca7)' }
  }
  return {
    backgroundImage: `url(${bg})`,
    backgroundSize: 'cover',
    backgroundPosition: 'center'
  }
})

function firstImage(imgUris?: string) {
  if (!imgUris) return ""
  return imgUris.split(',')[0] || ''
}

async function loadUser() {
  try { user.value = await getCurrentUser() } catch (e) { user.value = null }
}

async function loadList() {
  loading.value = true
  try {
    const rsp = await listMyNotes(page.value, size)
    total.value = rsp.total
    list.value = rsp.list
  } catch (e) {
    list.value = []
  } finally {
    loading.value = false
  }
}

function openEdit() {
  editForm.nickname = user.value?.nickname || ""
  editForm.sex = user.value?.sex ?? 0
  editForm.birthday = user.value?.birthday || ""
  editForm.introduction = user.value?.introduction || ""
  avatarFile.value = null
  backgroundFile.value = null
  editVisible.value = true
}

function onAvatarChange(file: any) { avatarFile.value = file.raw as File }
function onBackgroundChange(file: any) { backgroundFile.value = file.raw as File }

async function onSave() {
  const fd = new FormData()
  if (editForm.nickname) fd.append("nickname", editForm.nickname)
  fd.append("sex", String(editForm.sex))
  if (editForm.birthday) fd.append("birthday", editForm.birthday)
  if (editForm.introduction) fd.append("introduction", editForm.introduction)
  if (avatarFile.value) fd.append("avatar", avatarFile.value)
  if (backgroundFile.value) fd.append("backgroundImg", backgroundFile.value)

  saving.value = true
  try {
    await updateProfile(fd)
    ElMessage.success('保存成功')
    editVisible.value = false
    await loadUser()
  } finally {
    saving.value = false
  }
}

async function onSavePassword() {
  if (!pwdForm.password) {
    ElMessage.warning("请输入新密码")
    return
  }
  if (pwdForm.password !== pwdForm.confirm) {
    ElMessage.warning("两次输入的密码不一致")
    return
  }
  pwdSaving.value = true
  try {
    await updatePassword(pwdForm.password)
    ElMessage.success("密码修改成功")
    pwdVisible.value = false
    pwdForm.password = ""
    pwdForm.confirm = ""
  } finally {
    pwdSaving.value = false
  }
}

function onPageChange(p: number) {
  page.value = p
  loadList()
}

function openDetail(noteId: number) {
  router.push({ name: 'note-detail', params: { id: String(noteId) } })
}

onMounted(() => { loadUser(); loadList() })
</script>

<style scoped>
/* 个人主页背景横幅 */
.profile-banner {
  position: relative;
  min-height: 180px;
  margin-bottom: 16px;
  border-radius: 8px;
  overflow: hidden;
  display: flex;
  align-items: flex-end;
  padding: 16px;
}
.banner-mask { position: absolute; inset: 0; background: rgba(0, 0, 0, 0.25); }
.banner-content { position: relative; display: flex; align-items: center; gap: 16px; width: 100%; }
.banner-content .nickname { color: #fff; }
.banner-content .meta { color: rgba(255, 255, 255, 0.9); }

.info { flex: 1; }
.nickname { font-size: 18px; font-weight: 600; }
.meta { color: #999; font-size: 13px; }
.note-card { margin-bottom: 10px; cursor: pointer; }
.note-body { display: flex; gap: 12px; align-items: center; }
.cover { width: 96px; height: 96px; object-fit: cover; border-radius: 6px; flex-shrink: 0; }
.title { font-size: 15px; font-weight: 600; }
.pager { margin-top: 16px; display: flex; justify-content: center; }
</style>
