<template>
  <div class="hr-applications">
    <div class="page-head">
      <h2 class="page-title">收到的投递</h2>
      <p class="page-sub">学生投递到你所发布职位的记录（投递人脱敏展示，不含姓名与联系方式）</p>
    </div>

    <el-card>
      <div class="search-bar">
        <el-select v-model="jobFilter" placeholder="按职位筛选" clearable style="width:260px">
          <el-option v-for="j in jobOptions" :key="j.jobId" :label="j.jobTitle" :value="j.jobId" />
        </el-select>
        <span class="count">共 {{ filtered.length }} 条投递</span>
      </div>

      <el-table :data="filtered" v-loading="loading" stripe>
        <el-table-column prop="jobTitle" label="投递职位" min-width="180" />
        <el-table-column label="城市" width="90">
          <template #default="{ row }">{{ row.jobCity || '—' }}</template>
        </el-table-column>
        <el-table-column label="专业" min-width="150">
          <template #default="{ row }">
            <span v-if="row.profileCompleted">{{ row.major || '—' }}</span>
            <el-tag v-else type="info" size="small">未完善画像</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="学历" width="90">
          <template #default="{ row }"><span class="chip">{{ row.educationLevel || '—' }}</span></template>
        </el-table-column>
        <el-table-column label="技能" min-width="220">
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
        <el-table-column label="投递时间" width="150">
          <template #default="{ row }">{{ formatTime(row.applyTime) }}</template>
        </el-table-column>
      </el-table>

      <el-empty
        v-if="!loading && !filtered.length"
        :description="applications.length ? '该职位暂无投递' : '暂无投递记录'"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getHrApplications } from '@/api/student'
import { toList } from '@/utils/list'
import { parseSkills } from '@/utils/skills'
import { salaryRange, formatTime } from '@/utils/format'

const applications = ref([])
const loading = ref(false)
const jobFilter = ref(null)

/** 筛选下拉只列出真正收到过投递的职位 */
const jobOptions = computed(() => {
  const seen = new Map()
  for (const a of applications.value) {
    if (!seen.has(a.jobId)) seen.set(a.jobId, { jobId: a.jobId, jobTitle: a.jobTitle })
  }
  return [...seen.values()]
})

const filtered = computed(() =>
  jobFilter.value == null
    ? applications.value
    : applications.value.filter(a => a.jobId === jobFilter.value)
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
.search-bar { display: flex; align-items: center; gap: 16px; margin-bottom: 20px; }
.count { font-size: 13px; color: var(--app-ink-3); }
.chip-row { display: flex; flex-wrap: wrap; gap: 4px; }
</style>
