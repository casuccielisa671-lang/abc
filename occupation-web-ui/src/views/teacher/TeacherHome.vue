<template>
  <div class="teacher-home">
    <h2>班级概览</h2>
    <p class="subtitle">本校学生就业情况总览</p>

    <!-- 统计卡片 -->
    <el-row :gutter="16">
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-value">{{ stats.totalStudents }}</div>
          <div class="stat-label">学生总数</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-value">{{ stats.withProfile }}</div>
          <div class="stat-label">已填写画像</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-value">{{ stats.totalViews }}</div>
          <div class="stat-label">总浏览数</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-value">{{ stats.totalApplies }}</div>
          <div class="stat-label">总投递数</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 学生列表 -->
    <el-card style="margin-top:16px">
      <template #header>学生列表</template>
      <el-table :data="students" v-loading="loading" stripe>
        <el-table-column prop="userId" label="用户ID" width="80" />
        <el-table-column prop="realName" label="姓名" min-width="100" />
        <el-table-column prop="major" label="专业" min-width="120" />
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
          <template #default="{ row }">
            {{ row.intendedCity || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="goDetail(row.userId)">查看详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="page" v-model:page-size="size"
        :total="total" layout="total, prev, pager, next"
        @current-change="loadStudents" style="margin-top:16px; justify-content:flex-end"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getTeacherStudents } from '@/api/student'

const router = useRouter()
const students = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)

const stats = reactive({
  totalStudents: 0,
  withProfile: 0,
  totalViews: 0,
  totalApplies: 0
})

function parseSkills(skills) {
  if (!skills) return []
  if (Array.isArray(skills)) return skills
  try { return JSON.parse(skills) } catch { return skills.split(',').map(s => s.trim()).filter(Boolean) }
}

async function loadStudents() {
  loading.value = true
  try {
    const data = await getTeacherStudents({ page: page.value, size: size.value })
    students.value = data.records || data.list || []
    total.value = data.total || 0
    // 从返回数据中提取统计
    if (data.stats) {
      stats.totalStudents = data.stats.totalStudents || total.value
      stats.withProfile = data.stats.withProfile || 0
      stats.totalViews = data.stats.totalViews || 0
      stats.totalApplies = data.stats.totalApplies || 0
    }
  } finally {
    loading.value = false
  }
}

function goDetail(userId) {
  router.push({ path: '/teacher/students', query: { userId } })
}

onMounted(() => loadStudents())
</script>

<style scoped>
.teacher-home h2 { margin-bottom: 4px; }
.subtitle { color: #909399; margin-bottom: 16px; }
.stat-card { text-align: center; }
.stat-value { font-size: 28px; font-weight: bold; color: #409EFF; }
.stat-label { color: #909399; margin-top: 8px; font-size: 14px; }
</style>
