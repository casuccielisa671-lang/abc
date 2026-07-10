<template>
  <div class="teacher-students">
    <div class="page-head">
      <h2 class="page-title">学生管理</h2>
      <p class="page-sub">查看本校学生画像、求职行为统计与明细（点击一行展开详情）</p>
    </div>

    <el-card>
      <div class="search-bar">
        <el-input
          v-model="searchKeyword" placeholder="搜索专业 / 技能" clearable
          style="width:240px" @keyup.enter="search" @clear="search"
        />
        <el-select v-model="searchEducation" placeholder="学历筛选" clearable style="width:140px" @change="search">
          <el-option label="专科" value="专科" />
          <el-option label="本科" value="本科" />
          <el-option label="硕士" value="硕士" />
          <el-option label="博士" value="博士" />
        </el-select>
        <el-button type="primary" @click="search">查询</el-button>
      </div>

      <el-table
        :data="students" v-loading="loading" stripe
        highlight-current-row @row-click="selectStudent"
      >
        <el-table-column prop="username" label="学号" width="110" />
        <el-table-column prop="realName" label="姓名" min-width="100">
          <template #default="{ row }">{{ row.realName || '—' }}</template>
        </el-table-column>
        <el-table-column prop="major" label="专业" min-width="140" />
        <el-table-column label="学历" width="90">
          <template #default="{ row }"><span class="chip">{{ row.educationLevel || '—' }}</span></template>
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
        <el-table-column label="期望薪资" width="130">
          <template #default="{ row }">
            <span class="salary-text">{{ salaryRange(row.expectedSalaryMin, row.expectedSalaryMax, '未填写') }}</span>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && !students.length" description="没有符合条件的学生" />

      <el-pagination
        v-if="total > size"
        v-model:current-page="page" :page-size="size"
        :total="total" layout="total, prev, pager, next"
        @current-change="loadStudents"
      />
    </el-card>

    <!-- 学生详情面板 -->
    <el-card v-if="selectedUser" style="margin-top:16px">
      <template #header>
        <div class="card-header-row">
          <span>学生详情 — {{ selectedUser.realName || selectedUser.username }}</span>
          <el-button text @click="selectedUser = null">收起</el-button>
        </div>
      </template>

      <!-- 行为统计 -->
      <div class="stat-grid" v-if="userStats">
        <div class="stat-card">
          <div class="stat-label">浏览职位</div>
          <div class="stat-value">{{ userStats.viewCount }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">收藏职位</div>
          <div class="stat-value">{{ userStats.favoriteCount }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">投递次数</div>
          <div class="stat-value">{{ userStats.applyCount }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">最近活跃</div>
          <div class="stat-value time">{{ formatTime(userStats.lastActiveTime) }}</div>
        </div>
      </div>

      <!-- 行为明细 -->
      <el-table :data="behaviors" v-loading="behaviorLoading" stripe size="small" style="margin-top:16px">
        <el-table-column label="行为" width="90">
          <template #default="{ row }">
            <el-tag :type="actionTag(row.action)" size="small">{{ actionLabel(row.action) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="职位名称" min-width="200">
          <template #default="{ row }">{{ row.jobTitle || '职位已下架' }}</template>
        </el-table-column>
        <el-table-column label="公司" min-width="160">
          <template #default="{ row }">{{ row.jobCompany || '—' }}</template>
        </el-table-column>
        <el-table-column label="城市" width="90">
          <template #default="{ row }">{{ row.jobCity || '—' }}</template>
        </el-table-column>
        <el-table-column label="时间" width="150">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!behaviorLoading && !behaviors.length" description="该学生尚无求职行为记录" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getTeacherStudents, getStudentStats, getStudentBehaviors } from '@/api/student'
import { toList, toTotal } from '@/utils/list'
import { parseSkills } from '@/utils/skills'
import { salaryRange, formatTime } from '@/utils/format'

const route = useRoute()

const students = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)
const searchKeyword = ref('')
const searchEducation = ref('')

const selectedUser = ref(null)
const userStats = ref(null)
const behaviors = ref([])
const behaviorLoading = ref(false)

async function loadStudents() {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value }
    if (searchKeyword.value) params.keyword = searchKeyword.value
    if (searchEducation.value) params.education = searchEducation.value
    const data = await getTeacherStudents(params)
    students.value = toList(data)
    total.value = toTotal(data, students.value)
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 1
  loadStudents()
}

async function selectStudent(row) {
  selectedUser.value = row
  userStats.value = null
  behaviors.value = []
  behaviorLoading.value = true
  try {
    const [stats, detail] = await Promise.all([
      getStudentStats(row.userId),
      getStudentBehaviors(row.userId)
    ])
    userStats.value = stats
    behaviors.value = toList(detail)
  } catch {
    // 错误已在拦截器中提示
  } finally {
    behaviorLoading.value = false
  }
}

function actionTag(action) {
  return { VIEW: 'info', FAVORITE: 'warning', APPLY: 'success', IGNORE: 'info', CONTACT: 'primary' }[action] || 'info'
}
function actionLabel(action) {
  return { VIEW: '浏览', FAVORITE: '收藏', APPLY: '投递', IGNORE: '忽略', CONTACT: '自主联系' }[action] || action
}

onMounted(async () => {
  await loadStudents()
  // 从班级概览「查看详情」跳转过来时，自动展开对应学生
  const userId = Number(route.query.userId)
  if (userId) {
    const hit = students.value.find(s => s.userId === userId)
    if (hit) selectStudent(hit)
  }
})
</script>

<style scoped>
.search-bar { display: flex; align-items: center; gap: 12px; margin-bottom: 20px; flex-wrap: wrap; }
.card-header-row { display: flex; justify-content: space-between; align-items: center; }
.chip-row { display: flex; flex-wrap: wrap; gap: 4px; }
.stat-value.time { font-size: 18px; }
</style>
