<template>
  <div class="my-applications">
    <div class="page-head">
      <h2 class="page-title">我的投递</h2>
      <p class="page-sub">只有企业在本平台发布的职位可以投递；采集来的职位仅供参考</p>
    </div>

    <!-- 进度概览 -->
    <div class="stat-grid" v-if="list.length">
      <div v-for="s in STATUSES" :key="s.key" class="stat-card">
        <div class="stat-label">{{ s.label }}</div>
        <div class="stat-value">{{ counts[s.key] || 0 }}</div>
      </div>
    </div>

    <el-card :style="list.length ? 'margin-top:16px' : ''">
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column label="职位" min-width="180">
          <template #default="{ row }">
            <span v-if="row.jobTitle">{{ row.jobTitle }}</span>
            <span v-else class="removed">职位已下架</span>
          </template>
        </el-table-column>
        <el-table-column prop="company" label="公司" min-width="170">
          <template #default="{ row }">{{ row.company || '—' }}</template>
        </el-table-column>
        <el-table-column label="城市" width="90">
          <template #default="{ row }">{{ row.jobCity || '—' }}</template>
        </el-table-column>
        <el-table-column label="薪资" width="140">
          <template #default="{ row }">
            <span class="salary-text">{{ salaryRange(row.salaryMin, row.salaryMax) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="投递时间" width="150">
          <template #default="{ row }">{{ formatTime(row.appliedAt) }}</template>
        </el-table-column>
        <el-table-column label="进度" width="200">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" size="small">{{ row.statusLabel }}</el-tag>
            <span v-if="row.statusChangedAt" class="changed">{{ formatTime(row.statusChangedAt) }}</span>
            <span v-else-if="!row.terminal" class="waiting">等待企业查看</span>
          </template>
        </el-table-column>
      </el-table>

      <el-empty
        v-if="!loading && !list.length"
        description="你还没有投递过职位。到「职位推荐」里找标着「可投递」的岗位试试"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getMyApplications } from '@/api/student'
import { toList } from '@/utils/list'
import { salaryRange, formatTime } from '@/utils/format'

/** 与后端 ApplicationStatus 枚举一一对应 */
const STATUSES = [
  { key: 'SUBMITTED', label: '已投递' },
  { key: 'VIEWED', label: '已查看' },
  { key: 'INTERVIEW', label: '邀请面试' },
  { key: 'OFFER', label: '已录用' },
  { key: 'REJECTED', label: '不合适' }
]

const list = ref([])
const loading = ref(false)

const counts = computed(() =>
  list.value.reduce((acc, a) => {
    acc[a.status] = (acc[a.status] || 0) + 1
    return acc
  }, {})
)

function statusTag(status) {
  return {
    SUBMITTED: 'info',
    VIEWED: 'warning',
    INTERVIEW: 'primary',
    OFFER: 'success',
    REJECTED: 'danger'
  }[status] || 'info'
}

async function load() {
  loading.value = true
  try {
    list.value = toList(await getMyApplications())
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.removed { color: var(--app-ink-3); font-style: italic; }
.changed, .waiting { display: block; font-size: 11px; color: var(--app-ink-3); margin-top: 3px; }
</style>
