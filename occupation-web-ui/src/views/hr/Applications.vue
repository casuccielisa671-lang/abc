<template>
  <div class="hr-applications">
    <div class="page-head">
      <h2 class="page-title">收到的投递</h2>
      <p class="page-sub">学生投递到你所发布职位的记录。点任意一行可查看他的联系方式与简历全文</p>
    </div>

    <el-card>
      <div class="search-bar">
        <el-select v-model="jobFilter" placeholder="按职位筛选" clearable style="width:240px">
          <el-option v-for="j in jobOptions" :key="j.jobId" :label="j.jobTitle" :value="j.jobId" />
        </el-select>
        <el-select v-model="statusFilter" placeholder="按状态筛选" clearable style="width:160px">
          <el-option v-for="s in STATUSES" :key="s.key" :label="s.label" :value="s.key" />
        </el-select>
        <span class="count">共 {{ filtered.length }} 条投递</span>
        <span v-if="pending" class="pending">{{ pending }} 条待处理</span>
      </div>

      <el-table :data="filtered" v-loading="loading" stripe class="clickable" @row-click="openDetail">
        <el-table-column prop="jobTitle" label="投递职位" min-width="170" />
        <el-table-column label="投递人" width="100">
          <template #default="{ row }">{{ row.realName || '—' }}</template>
        </el-table-column>
        <el-table-column label="专业" min-width="140">
          <template #default="{ row }">
            <span v-if="row.profileCompleted">{{ row.major || '—' }}</span>
            <el-tag v-else type="info" size="small">未完善画像</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="学历" width="80">
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
        <el-table-column label="期望薪资" width="130">
          <template #default="{ row }">
            <span class="salary-text">{{ salaryRange(row.expectedSalaryMin, row.expectedSalaryMax, '未填写') }}</span>
          </template>
        </el-table-column>
        <el-table-column label="简历" width="90">
          <template #default="{ row }">
            <el-tag :type="row.hasResume ? 'success' : 'info'" size="small">
              {{ row.hasResume ? '已填写' : '未填写' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="处理状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" size="small">{{ row.statusLabel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="投递时间" width="150">
          <template #default="{ row }">{{ formatTime(row.applyTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click.stop="openDetail(row)">处理</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty
        v-if="!loading && !filtered.length"
        :description="applications.length ? '该职位暂无投递' : '暂无投递记录'"
      />
    </el-card>

    <!-- 抽屉里改了状态，列表要跟着刷新，否则「处理状态」列会显示旧值 -->
    <ApplicantDrawer v-model="detailVisible" :user-id="currentUserId" @changed="load" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getHrApplications } from '@/api/student'
import ApplicantDrawer from '@/components/ApplicantDrawer.vue'
import { toList } from '@/utils/list'
import { parseSkills } from '@/utils/skills'
import { salaryRange, formatTime } from '@/utils/format'

const STATUSES = [
  { key: 'SUBMITTED', label: '已投递' },
  { key: 'VIEWED', label: '已查看' },
  { key: 'INTERVIEW', label: '邀请面试' },
  { key: 'OFFER', label: '已录用' },
  { key: 'REJECTED', label: '不合适' }
]

const applications = ref([])
const loading = ref(false)
const jobFilter = ref(null)
const statusFilter = ref(null)

const detailVisible = ref(false)
const currentUserId = ref(null)

/** 还没看过的投递，HR 一眼知道有多少活要干 */
const pending = computed(() => applications.value.filter(a => a.status === 'SUBMITTED').length)

function statusTag(status) {
  return {
    SUBMITTED: 'info',
    VIEWED: 'warning',
    INTERVIEW: 'primary',
    OFFER: 'success',
    REJECTED: 'danger'
  }[status] || 'info'
}

function openDetail(row) {
  if (row.userId == null) return
  currentUserId.value = row.userId
  detailVisible.value = true
}

/** 筛选下拉只列出真正收到过投递的职位 */
const jobOptions = computed(() => {
  const seen = new Map()
  for (const a of applications.value) {
    if (!seen.has(a.jobId)) seen.set(a.jobId, { jobId: a.jobId, jobTitle: a.jobTitle })
  }
  return [...seen.values()]
})

const filtered = computed(() =>
  applications.value.filter(a =>
    (jobFilter.value == null || a.jobId === jobFilter.value) &&
    (statusFilter.value == null || a.status === statusFilter.value)
  )
)

async function load() {
  loading.value = true
  try {
    applications.value = toList(await getHrApplications())
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.search-bar { display: flex; align-items: center; gap: 14px; margin-bottom: 20px; flex-wrap: wrap; }
.count { font-size: 13px; color: var(--app-ink-3); }
.pending { font-size: 13px; color: var(--app-ember); font-weight: 600; }
.chip-row { display: flex; flex-wrap: wrap; gap: 4px; }
.clickable :deep(.el-table__row) { cursor: pointer; }
</style>
