<template>
  <div class="profile-page">
    <div class="page-head" v-if="!embedded">
      <h2 class="page-title">个人画像</h2>
      <p class="page-sub">完善画像后，系统将为你精准匹配职位</p>
    </div>

    <el-card class="profile-card" v-loading="loading">
      <div class="profile-layout">
        <!-- 左侧：基本信息表单 -->
        <div class="profile-form-area">
          <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
            <el-form-item label="专业" prop="major">
              <el-input v-model="form.major" placeholder="如：计算机科学与技术" />
            </el-form-item>
            <el-form-item label="技能" prop="skills">
              <el-input v-model="skillsText" placeholder="多个技能用逗号分隔，如：Java,Spring Boot,MySQL" />
              <div class="skill-preview">
                <span v-for="sk in skillPreview" :key="sk" class="chip">{{ sk }}</span>
                <span v-if="!skillPreview.length" class="form-tip">技能会参与职位匹配打分，填得越准推荐越贴合</span>
              </div>
            </el-form-item>
            <el-form-item label="意向城市">
              <el-input v-model="form.expectedCity" placeholder="如：深圳" />
            </el-form-item>
            <el-form-item label="意向行业">
              <el-input v-model="form.expectedIndustry" placeholder="如：互联网/IT" />
            </el-form-item>
            <el-form-item label="期望薪资">
              <el-col :span="11">
                <el-input-number v-model="form.expectedSalaryMin" :min="0" :step="1000" placeholder="最低" style="width:100%" />
              </el-col>
              <el-col :span="2" style="text-align:center">—</el-col>
              <el-col :span="11">
                <el-input-number v-model="form.expectedSalaryMax" :min="0" :step="1000" placeholder="最高" style="width:100%" />
              </el-col>
            </el-form-item>
            <el-form-item label="学历">
              <el-select v-model="form.educationLevel" style="width:100%">
                <el-option label="专科" value="专科" />
                <el-option label="本科" value="本科" />
                <el-option label="硕士" value="硕士" />
                <el-option label="博士" value="博士" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="saving" @click="handleSave">保存画像</el-button>
            </el-form-item>
          </el-form>
        </div>

        <!-- 右侧：证件照 -->
        <div class="profile-avatar-area">
          <div class="avatar-label">证件照</div>
          <div class="avatar-upload-area">
            <div class="avatar-preview" v-if="avatarUrl">
              <img :src="avatarUrl" alt="证件照" class="avatar-img" />
              <div class="avatar-actions">
                <el-button size="small" type="danger" plain @click="handleRemoveAvatar">删除</el-button>
              </div>
            </div>
            <div class="avatar-upload-box" v-else>
              <el-upload
                class="avatar-uploader"
                action="/api/student/profile/avatar"
                :headers="uploadHeaders"
                :show-file-list="false"
                :before-upload="beforeAvatarUpload"
                :on-success="handleAvatarSuccess"
                :on-error="handleAvatarError"
                accept="image/*"
                name="file"
              >
                <div class="upload-placeholder">
                  <el-icon class="upload-icon"><Plus /></el-icon>
                  <span class="upload-text">点击上传证件照</span>
                  <span class="upload-hint">支持 JPG/PNG，不超过 5MB</span>
                </div>
              </el-upload>
            </div>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 个人统计 -->
    <el-card style="margin-top:16px" v-if="stats">
      <template #header>求职统计</template>
      <div class="stat-grid">
        <div class="stat-card">
          <div class="stat-label">浏览职位</div>
          <div class="stat-value">{{ stats.VIEW || 0 }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">收藏职位</div>
          <div class="stat-value">{{ stats.FAVORITE || 0 }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">投递次数</div>
          <div class="stat-value">{{ stats.APPLY || 0 }}</div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { getProfile, saveProfile, getProfileStats } from '@/api/student'
import { parseSkills } from '@/utils/skills'
import { ElMessage, ElMessageBox } from 'element-plus'

// embedded=true 时隐藏自身大标题，供「我的资料」中心以标签页嵌入
defineProps({ embedded: { type: Boolean, default: false } })

const loading = ref(false)
const saving = ref(false)
const formRef = ref(null)
const stats = ref(null)
const avatarUrl = ref('')

const uploadHeaders = computed(() => ({
  Authorization: `Bearer ${localStorage.getItem('token')}`
}))

const form = reactive({
  major: '',
  skills: '',
  expectedCity: '',
  expectedIndustry: '',
  expectedSalaryMin: null,
  expectedSalaryMax: null,
  educationLevel: '',
  avatarUrl: ''
})

const skillsText = ref('')

/** 实时预览会被解析成哪些技能标签，避免用户以为分号、空格也能分隔 */
const skillPreview = computed(() => parseSkills(skillsText.value))

const rules = {
  major: [{ required: true, message: '请输入专业', trigger: 'blur' }]
}

function textToSkills(text) {
  return JSON.stringify(parseSkills(text))
}

async function loadProfile() {
  loading.value = true
  try {
    const data = await getProfile()
    if (data) {
      form.major = data.major || ''
      form.expectedCity = data.expectedCity || ''
      form.expectedIndustry = data.expectedIndustry || ''
      form.expectedSalaryMin = data.expectedSalaryMin
      form.expectedSalaryMax = data.expectedSalaryMax
      form.educationLevel = data.educationLevel || ''
      form.avatarUrl = data.avatarUrl || ''
      avatarUrl.value = data.avatarUrl || ''
      skillsText.value = parseSkills(data.skills).join(',')
      form.skills = data.skills || '[]'
    }
    stats.value = await getProfileStats()
  } finally {
    loading.value = false
  }
}

function beforeAvatarUpload(file) {
  const isImage = file.type.startsWith('image/')
  const isLt5M = file.size / 1024 / 1024 < 5
  if (!isImage) {
    ElMessage.error('只能上传图片文件')
    return false
  }
  if (!isLt5M) {
    ElMessage.error('图片大小不能超过 5MB')
    return false
  }
  return true
}

function handleAvatarSuccess(response) {
  if (response.avatarUrl) {
    avatarUrl.value = response.avatarUrl
    form.avatarUrl = response.avatarUrl
    ElMessage.success('证件照上传成功')
  }
}

function handleAvatarError() {
  ElMessage.error('证件照上传失败，请重试')
}

async function handleRemoveAvatar() {
  try {
    await ElMessageBox.confirm('确定要删除证件照吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    avatarUrl.value = ''
    form.avatarUrl = ''
    await saveProfile({ ...form })
    ElMessage.success('证件照已删除')
  } catch {
    // 用户取消
  }
}

async function handleSave() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    form.skills = textToSkills(skillsText.value)
    await saveProfile({ ...form })
    ElMessage.success('画像保存成功，推荐结果已更新')
  } finally {
    saving.value = false
  }
}

onMounted(() => loadProfile())
</script>

<style scoped>
.form-tip { color: var(--app-ink-3); font-size: 12px; }
.skill-preview { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 8px; }

/* 左右两栏布局（同一个卡片内） */
.profile-card { margin-bottom: 16px; }
.profile-layout {
  display: flex; gap: 32px; align-items: flex-start;
}
.profile-form-area { flex: 1; min-width: 0; }
.profile-avatar-area { width: 220px; flex-shrink: 0; }

.avatar-label {
  font-weight: 600; font-size: 14px; color: #374151;
  margin-bottom: 12px; text-align: center;
}

/* 证件照上传区域 */
.avatar-upload-area { display: flex; flex-direction: column; align-items: center; }

.avatar-preview {
  display: flex; flex-direction: column; align-items: center; gap: 12px;
}
.avatar-img {
  width: 160px; height: 210px; object-fit: cover;
  border-radius: 8px; border: 1px solid #e5e7eb;
  box-shadow: 0 2px 8px rgba(0,0,0,.08);
}
.avatar-actions { display: flex; gap: 8px; }

.avatar-upload-box { width: 100%; }
.avatar-uploader { width: 100%; }
.avatar-uploader :deep(.el-upload) {
  width: 100%; display: block;
}

.upload-placeholder {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  height: 180px; border: 2px dashed #d1d5db; border-radius: 8px;
  cursor: pointer; transition: border-color .2s, background .2s;
}
.upload-placeholder:hover {
  border-color: #409eff; background: #f0f7ff;
}
.upload-icon { font-size: 32px; color: #9ca3af; margin-bottom: 8px; }
.upload-text { font-size: 13px; color: #374151; margin-bottom: 4px; }
.upload-hint { font-size: 12px; color: #9ca3af; }

/* 响应式 */
@media (max-width: 768px) {
  .profile-layout { flex-direction: column; }
  .profile-avatar-area { width: 100%; }
}
</style>
