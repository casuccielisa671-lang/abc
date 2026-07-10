<template>
  <div class="hr-home">
    <div class="page-head">
      <h2 class="page-title">HR 工作台</h2>
      <p class="page-sub">职位管理与人才浏览概览</p>
    </div>

    <!-- 统计卡片 -->
    <div class="stat-grid">
      <div class="stat-card">
        <div class="stat-label">我发布的职位</div>
        <div class="stat-value">{{ stats.totalJobs }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">人才库</div>
        <div class="stat-value">{{ stats.totalTalents }}</div>
        <div class="stat-hint">本校已填写画像的学生</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">收到投递</div>
        <div class="stat-value">{{ stats.totalApplies }}</div>
        <div class="stat-hint">来自 {{ stats.applicantCount }} 位学生</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">收到投递的职位</div>
        <div class="stat-value">{{ stats.jobsWithApplies }}</div>
        <div class="stat-hint">共 {{ stats.totalJobs }} 个职位中</div>
      </div>
    </div>

    <!-- 最近职位 -->
    <el-card style="margin-top:16px">
      <template #header>
        <div class="card-header-row">
          <span>我最近发布的职位</span>
          <el-button size="small" @click="$router.push('/hr/jobs')">管理职位</el-button>
        </div>
      </template>
      <el-table :data="recentJobs" v-loading="loading" stripe>
        <el-table-column prop="title" label="职位名称" min-width="180" />
        <el-table-column prop="city" label="城市" width="90" />
        <el-table-column label="薪资范围" width="150">
          <template #default="{ row }">
            <span class="salary-text">{{ salaryRange(row.salaryMin, row.salaryMax) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="学历要求" width="100">
          <template #default="{ row }"><span class="chip">{{ row.education || '不限' }}</span></template>
        </el-table-column>
        <el-table-column prop="publishDate" label="发布日期" width="120" />
      </el-table>
      <el-empty v-if="!loading && !recentJobs.length" description="你还没有发布过职位" />
    </el-card>

    <!-- 收到的投递 -->
    <el-card style="margin-top:16px">
      <template #header>
        <div class="card-header-row">
          <span>最近收到的投递</span>
          <el-button size="small" @click="$router.push('/hr/applications')">查看全部</el-button>
        </div>
      </template>
      <el-table :data="recentApplications" v-loading="loading" stripe class="clickable" @row-click="openDetail">
        <el-table-column prop="jobTitle" label="投递职位" min-width="170" />
        <el-table-column label="投递人" width="100">
          <template #default="{ row }">{{ row.realName || '—' }}</template>
        </el-table-column>
        <el-table-column label="专业" min-width="140">
          <template #default="{ row }">{{ row.major || '未填写画像' }}</template>
        </el-table-column>
        <el-table-column label="学历" width="90">
          <template #default="{ row }"><span class="chip">{{ row.educationLevel || '—' }}</span></template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" size="small">{{ row.statusLabel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="投递时间" width="160">
          <template #default="{ row }">{{ formatTime(row.applyTime) }}</template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && !recentApplications.length" description="暂无投递记录" />
    </el-card>

    <ApplicantDrawer v-model="detailVisible" :user-id="currentUserId" @changed="loadData" />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getHrJobs, getTalents, getHrApplications } from '@/api/student'
import ApplicantDrawer from '@/components/ApplicantDrawer.vue'
import { toList, toTotal } from '@/utils/list'
import { salaryRange, formatTime } from '@/utils/format'

const recentJobs = ref([])
const recentApplications = ref([])
const loading = ref(false)

const stats = reactive({
  totalJobs: 0,
  totalTalents: 0,
  totalApplies: 0,
  jobsWithApplies: 0,
  applicantCount: 0
})

const detailVisible = ref(false)
const currentUserId = ref(null)

function openDetail(row) {
  if (row.userId == null) return
  currentUserId.value = row.userId
  detailVisible.value = true
}

function statusTag(status) {
  return {
    SUBMITTED: 'info',
    VIEWED: 'warning',
    INTERVIEW: 'primary',
    OFFER: 'success',
    REJECTED: 'danger'
  }[status] || 'info'
}

async function loadData() {
  loading.value = true
  try {
    const [jobData, talentData, applications] = await Promise.all([
      getHrJobs({ pageNum: 1, pageSize: 5 }),
      getTalents({ page: 1, size: 1 }),
      getHrApplications()
    ])

    recentJobs.value = toList(jobData)
    stats.totalJobs = toTotal(jobData, recentJobs.value)

    stats.totalTalents = toTotal(talentData)

    const all = toList(applications)
    recentApplications.value = all.slice(0, 5)
    stats.totalApplies = all.length
    stats.jobsWithApplies = new Set(all.map(a => a.jobId)).size
    // 投递列表现在带 userId（投递即授权），可以去重成真实的「投递人数」了
    stats.applicantCount = new Set(all.map(a => a.userId)).size
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.card-header-row { display: flex; justify-content: space-between; align-items: center; }
.clickable :deep(.el-table__row) { cursor: pointer; }
</style>
