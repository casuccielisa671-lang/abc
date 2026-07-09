<template>
  <div class="profile-page">
    <div class="page-head">
      <h2 class="page-title">个人画像</h2>
      <p class="page-sub">完善画像后，系统将为你精准匹配职位</p>
    </div>

    <el-card v-loading="loading">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px" style="max-width:600px">
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
          <!-- 必须与后端 JobMatchServiceImpl.EDU_LEVEL 的键一致（是「专科」不是「大专」），
               否则学历维度拿不到分，匹配分会莫名偏低 -->
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
import { getProfile, saveProfile, getProfileStats } from '@/api/student'
import { parseSkills } from '@/utils/skills'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const saving = ref(false)
const formRef = ref(null)
const stats = ref(null)

const form = reactive({
  major: '',
  skills: '',
  expectedCity: '',
  expectedIndustry: '',
  expectedSalaryMin: null,
  expectedSalaryMax: null,
  educationLevel: ''
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
      skillsText.value = parseSkills(data.skills).join(',')
      form.skills = data.skills || '[]'
    }
    stats.value = await getProfileStats()
  } finally {
    loading.value = false
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
</style>
