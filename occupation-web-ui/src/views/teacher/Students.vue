<template>
  <div class="teacher-students">
    <h2>学生管理</h2>
    <p class="subtitle">查看本校学生画像、求职行为统计与明细</p>

    <el-card>
      <div class="search-bar">
        <el-input v-model="searchKeyword" placeholder="搜索学生姓名/专业" clearable style="width:240px"
          @keyup.enter="loadStudents" />
        <el-button type="primary" @click="loadStudents" style="margin-left:12px">查询</el-button>
      </div>

      <el-table :data="students" v-loading="loading" stripe @row-click="selectStudent" highlight-current-row>
        <el-table-column prop="userId" label="用户ID" width="80" />
        <el-table-column prop="realName" label="姓名" min-width="100" />
        <el-table-column prop="major" label="专业" min-width="130" />
        <el-table-column label="学历" width="90">
          <template #default="{ row }">
            <el-tag size="small">{{ row.education || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="skills" label="技能" min-width="180">
          <template #default="{ row }">
            <el-tag v-for="sk in parseSkills(row.skills)" :key="sk" size="small" style="margin:2px">{{ sk }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="期望城市" width="100">
          <template #default="{ row }">{{ row.intendedCity || '-' }}</template>
        </el-table-column>
        <el-table-column label="期望薪资" width="120">
          <template #default="{ row }">
            <template v-if="row.expectedSalaryMin">
              {{ (row.expectedSalaryMin / 1000).toFixed(0) }}k - {{ (row.expectedSalaryMax / 1000).toFixed(0) }}k
            </template>
            <span v-else>-</span>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="page" v-model:page-size="size"
        :total="total" layout="total, prev, pager, next"
        @current-change="loadStudents" style="margin-top:16px; justify-content:flex-end"
      />
    </el-card>

    <!-- 学生详情面板 -->
    <el-card v-if="selectedUser" style="margin-top:16px">
      <template #header>
        <span>学生详情 — {{ selectedUser.realName || selectedUser.username }}</span>
      </template>

      <!-- 行为统计 -->
      <el-row :gutter="16" v-if="userStats">
        <el-col :span="6">
          <div class="mini-stat">
            <div class="mini-value">{{ userStats.viewCount || 0 }}</div>
            <div class="mini-label">浏览职位</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="mini-stat">
            <div class="mini-value">{{ userStats.favoriteCount || 0 }}</div>
            <div class="mini-label">收藏职位</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="mini-stat">
            <div class="mini-value">{{ userStats.applyCount || 0 }}</div>
            <div class="mini-label">投递次数</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="mini-stat">
            <div class="mini-value">{{ userStats.lastActiveTime || '-' }}</div>
            <div class="mini-label">最近活跃</div>
          </div>
        </el-col>
      </el-row>

      <!-- 行为明细 -->
      <el-table :data="behaviors" v-loading="behaviorLoading" stripe size="small" style="margin-top:16px">
        <el-table-column label="行为" width="100">
          <template #default="{ row }">
            <el-tag :type="actionTag(row.action)" size="small">{{ actionLabel(row.action) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="jobTitle" label="职位名称" min-width="180" />
        <el-table-column prop="jobCompany" label="公司" min-width="140" />
        <el-table-column prop="createTime" label="时间" width="170" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getTeacherStudents, getStudentStats, getStudentBehaviors } from '@/api/student'

const students = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)
const searchKeyword = ref('')

const selectedUser = ref(null)
const userStats = ref(null)
const behaviors = ref([])
const behaviorLoading = ref(false)

function parseSkills(skills) {
  if (!skills) return []
  if (Array.isArray(skills)) return skills
  try { return JSON.parse(skills) } catch { return skills.split(',').map(s => s.trim()).filter(Boolean) }
}

async function loadStudents() {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value }
    if (searchKeyword.value) params.keyword = searchKeyword.value
    const data = await getTeacherStudents(params)
    students.value = data.records || data.list || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

async function selectStudent(row) {
  selectedUser.value = row
  const userId = row.userId
  try {
    userStats.value = await getStudentStats(userId)
  } catch {
    userStats.value = null
  }
  behaviorLoading.value = true
  try {
    const data = await getStudentBehaviors(userId, { page: 1, size: 20 })
    behaviors.value = data.records || data.list || []
  } catch {
    behaviors.value = []
  } finally {
    behaviorLoading.value = false
  }
}

function actionTag(action) {
  return { VIEW: 'info', FAVORITE: 'warning', APPLY: 'success' }[action] || ''
}
function actionLabel(action) {
  return { VIEW: '浏览', FAVORITE: '收藏', APPLY: '投递' }[action] || action
}

onMounted(() => loadStudents())
</script>

<style scoped>
.teacher-students h2 { margin-bottom: 4px; }
.subtitle { color: #909399; margin-bottom: 16px; }
.search-bar { display: flex; align-items: center; margin-bottom: 16px; }
.mini-stat { text-align: center; padding: 12px; background: #f5f7fa; border-radius: 8px; }
.mini-value { font-size: 22px; font-weight: bold; color: #409EFF; }
.mini-label { color: #909399; font-size: 13px; margin-top: 4px; }
</style>
