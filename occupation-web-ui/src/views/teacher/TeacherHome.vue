<template>
  <div class="teacher-home">
    <div class="page-head with-actions">
      <div>
        <h2 class="page-title">班级概览</h2>
        <p class="page-sub">本校学生就业情况总览</p>
      </div>
      <div class="page-actions">
        <el-button :loading="exporting" @click="handleExport">导出 Excel</el-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stat-grid">
      <div class="stat-card">
        <div class="stat-label">学生总数</div>
        <div class="stat-value">{{ stats.totalStudents }}</div>
        <div class="stat-hint">本校 STUDENT 角色账号</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">已填写画像</div>
        <div class="stat-value">{{ stats.withProfile }}</div>
        <div class="stat-hint">{{ pendingProfile }} 人尚未完善</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">总浏览数</div>
        <div class="stat-value">{{ stats.totalViews }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">总投递数</div>
        <div class="stat-value">{{ stats.totalApplies }}</div>
        <div class="stat-hint">投给平台内企业</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">自主求职</div>
        <div class="stat-value">{{ stats.totalContacts }}</div>
        <div class="stat-hint">学生自行联系的外部岗位</div>
      </div>
    </div>

    <!-- 学生列表 -->
    <el-card style="margin-top:16px">
      <template #header>学生列表</template>
      <el-table :data="students" v-loading="loading" stripe>
        <el-table-column prop="username" label="学号" width="110" />
        <el-table-column prop="realName" label="姓名" min-width="100">
          <template #default="{ row }">{{ row.realName || '—' }}</template>
        </el-table-column>
        <el-table-column prop="major" label="专业" min-width="140" />
        <el-table-column label="学历" width="90">
          <template #default="{ row }">
            <span class="chip">{{ row.educationLevel || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="技能" min-width="200">
          <template #default="{ row }">
            <div class="chip-row">
              <span v-for="sk in parseSkills(row.skills)" :key="sk" class="chip">{{ sk }}</span>
              <span v-if="!parseSkills(row.skills).length">—</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="期望城市" width="100">
          <template #default="{ row }">{{ row.expectedCity || '—' }}</template>
        </el-table-column>
        <el-table-column label="投递" width="80">
          <template #default="{ row }">{{ row.applyCount }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="goDetail(row.userId)">查看详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && !students.length" description="本校暂无已填写画像的学生" />

      <el-pagination
        v-if="total > size"
        v-model:current-page="page" :page-size="size"
        :total="total" layout="total, prev, pager, next"
        @current-change="loadStudents"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getTeacherStudents, getTeacherOverview, exportTeacherStudents } from '@/api/student'
import { toList, toTotal } from '@/utils/list'
import { saveBlob } from '@/utils/download'
import { parseSkills } from '@/utils/skills'
import { ElMessage } from 'element-plus'

const router = useRouter()
const students = ref([])
const loading = ref(false)
const exporting = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)

const stats = reactive({
  totalStudents: 0,
  withProfile: 0,
  totalViews: 0,
  totalApplies: 0,
  totalContacts: 0
})

const pendingProfile = computed(() => Math.max(0, stats.totalStudents - stats.withProfile))

async function loadStudents() {
  loading.value = true
  try {
    const data = await getTeacherStudents({ page: page.value, size: size.value })
    students.value = toList(data)
    total.value = toTotal(data, students.value)
  } finally {
    loading.value = false
  }
}

async function loadOverview() {
  // 统计口径来自 /teacher/overview，不再从学生列表里凑
  const data = await getTeacherOverview()
  Object.assign(stats, data)
}

async function handleExport() {
  exporting.value = true
  try {
    await saveBlob(exportTeacherStudents(), '学生就业数据.xlsx')
    ElMessage.success('导出成功')
  } catch {
    // 错误已在拦截器中提示
  } finally {
    exporting.value = false
  }
}

function goDetail(userId) {
  router.push({ path: '/teacher/students', query: { userId } })
}

onMounted(() => {
  loadStudents()
  loadOverview()
})
</script>

<style scoped>
.chip-row { display: flex; flex-wrap: wrap; gap: 4px; }
</style>
