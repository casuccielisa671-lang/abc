<template>
  <div class="profile-page">
    <h2>个人画像</h2>
    <p class="subtitle">完善画像后，系统将为你精准匹配职位</p>

    <el-card v-loading="loading">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px" style="max-width:600px">
        <el-form-item label="专业" prop="major">
          <el-input v-model="form.major" placeholder="如：计算机科学与技术" />
        </el-form-item>
        <el-form-item label="技能" prop="skills">
          <el-input v-model="skillsText" placeholder="多个技能用逗号分隔，如：Java,Spring,Vue" />
          <span class="form-tip">输入后按回车或逗号添加标签</span>
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
            <el-option label="大专" value="大专" />
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
      <el-row :gutter="20">
        <el-col :span="8">
          <el-statistic title="浏览职位" :value="stats.VIEW || 0" />
        </el-col>
        <el-col :span="8">
          <el-statistic title="收藏职位" :value="stats.FAVORITE || 0" />
        </el-col>
        <el-col :span="8">
          <el-statistic title="投递次数" :value="stats.APPLY || 0" />
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { getProfile, saveProfile, getProfileStats } from '@/api/student'
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

const rules = {
  major: [{ required: true, message: '请输入专业', trigger: 'blur' }]
}

// 技能文本 ↔ JSON 数组转换
function skillsToText(skillsJson) {
  if (!skillsJson) return ''
  try {
    return JSON.parse(skillsJson).join(',')
  } catch {
    return skillsJson
  }
}

function textToSkills(text) {
  if (!text) return '[]'
  const arr = text.split(/[,，]/).map(s => s.trim()).filter(Boolean)
  return JSON.stringify(arr)
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
      skillsText.value = skillsToText(data.skills)
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
.subtitle { color: #909399; margin-bottom: 20px; }
.form-tip { color: #C0C4CC; font-size: 12px; }
</style>
