<template>
  <div class="hr-home">
    <h2>HR 工作台</h2>
    <p class="subtitle">职位管理与人才浏览概览</p>

    <!-- 统计卡片 -->
    <el-row :gutter="16">
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-value">{{ stats.totalJobs }}</div>
          <div class="stat-label">发布职位</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-value">{{ stats.activeJobs }}</div>
          <div class="stat-label">在招职位</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-value">{{ stats.totalTalents }}</div>
          <div class="stat-label">人才库</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-value">{{ stats.recentApplies }}</div>
          <div class="stat-label">近期投递</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 最近职位 -->
    <el-card style="margin-top:16px">
      <template #header>
        <div class="card-header-row">
          <span>最近发布的职位</span>
          <el-button type="primary" size="small" @click="$router.push('/hr/jobs')">管理职位</el-button>
        </div>
      </template>
      <el-table :data="recentJobs" v-loading="jobLoading" stripe>
        <el-table-column prop="title" label="职位名称" min-width="160" />
        <el-table-column prop="city" label="城市" width="100" />
        <el-table-column label="薪资范围" width="140">
          <template #default="{ row }">
            {{ (row.salaryMin / 1000).toFixed(0) }}k - {{ (row.salaryMax / 1000).toFixed(0) }}k
          </template>
        </el-table-column>
        <el-table-column prop="education" label="学历要求" width="100" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '在招' : '已下架' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="发布时间" width="170" />
      </el-table>
    </el-card>

    <!-- 人才推荐 -->
    <el-card style="margin-top:16px">
      <template #header>
        <div class="card-header-row">
          <span>人才推荐（脱敏展示）</span>
          <el-button type="primary" size="small" @click="$router.push('/hr/talents')">浏览人才库</el-button>
        </div>
      </template>
      <el-table :data="recentTalents" v-loading="talentLoading" stripe>
        <el-table-column label="学历" width="90">
          <template #default="{ row }">
            <el-tag size="small">{{ row.education || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="major" label="专业" min-width="130" />
        <el-table-column label="技能" min-width="200">
          <template #default="{ row }">
            <el-tag v-for="sk in parseSkills(row.skills)" :key="sk" size="small" style="margin:2px">{{ sk }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="期望城市" width="100">
          <template #default="{ row }">{{ row.intendedCity || '-' }}</template>
        </el-table-column>
        <el-table-column label="期望薪资" width="140">
          <template #default="{ row }">
            <template v-if="row.expectedSalaryMin">
              {{ (row.expectedSalaryMin / 1000).toFixed(0) }}k - {{ (row.expectedSalaryMax / 1000).toFixed(0) }}k
            </template>
            <span v-else>-</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getHrJobs, getTalents } from '@/api/student'

const recentJobs = ref([])
const jobLoading = ref(false)
const recentTalents = ref([])
const talentLoading = ref(false)

const stats = reactive({
  totalJobs: 0,
  activeJobs: 0,
  totalTalents: 0,
  recentApplies: 0
})

function parseSkills(skills) {
  if (!skills) return []
  if (Array.isArray(skills)) return skills
  try { return JSON.parse(skills) } catch { return skills.split(',').map(s => s.trim()).filter(Boolean) }
}

async function loadData() {
  jobLoading.value = true
  talentLoading.value = true
  try {
    const [jobData, talentData] = await Promise.all([
      getHrJobs({ page: 1, size: 5 }),
      getTalents({ page: 1, size: 5 })
    ])
    recentJobs.value = jobData.records || jobData.list || []
    stats.totalJobs = jobData.total || 0
    stats.activeJobs = (jobData.records || jobData.list || []).filter(j => j.status === 1).length

    recentTalents.value = talentData.records || talentData.list || []
    stats.totalTalents = talentData.total || 0
  } finally {
    jobLoading.value = false
    talentLoading.value = false
  }
}

onMounted(() => loadData())
</script>

<style scoped>
.hr-home h2 { margin-bottom: 4px; }
.subtitle { color: #909399; margin-bottom: 16px; }
.stat-card { text-align: center; }
.stat-value { font-size: 28px; font-weight: bold; color: #409EFF; }
.stat-label { color: #909399; margin-top: 8px; font-size: 14px; }
.card-header-row { display: flex; justify-content: space-between; align-items: center; }
</style>
