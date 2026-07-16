<template>
  <div class="recommend-metrics">
    <div
      v-for="metric in metrics"
      :key="metric.key"
      class="recommend-metric"
      :class="`theme-${metric.theme}`"
    >
      <div class="metric-icon">
        <component :is="metric.icon" />
      </div>
      <div class="metric-body">
        <div class="metric-label">{{ metric.label }}</div>
        <div class="metric-value">
          {{ metric.value }}<span v-if="metric.unit">{{ metric.unit }}</span>
        </div>
        <div class="metric-trend">{{ metric.trend }}</div>
        <div class="metric-desc">{{ metric.desc }}</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { Aim, Promotion, Star, TrendCharts } from '@element-plus/icons-vue'
import { getMyApplications } from '@/api/student'
import { useEmploymentStore } from '@/store/employment'
import { toList } from '@/utils/list'

const props = defineProps({
  items: { type: Array, default: () => [] },
  totalItems: { type: Array, default: () => [] },
  activeTab: { type: String, default: 'applicable' },
  applicableCount: { type: Number, default: 0 }
})

const empStore = useEmploymentStore()
const applications = ref([])

const totalApplicableCount = computed(() => props.totalItems.filter(i => i.job?.applicable).length)
const totalReferenceCount = computed(() => props.totalItems.filter(i => !i.job?.applicable).length)
const highMatchCount = computed(() => props.items.filter(i => Number(i.score) >= 80).length)
const salarySamples = computed(() => props.items
  .map(i => {
    const min = Number(i.job?.salaryMin)
    const max = Number(i.job?.salaryMax)
    if (!Number.isFinite(min) && !Number.isFinite(max)) return 0
    if (!Number.isFinite(min)) return max
    if (!Number.isFinite(max)) return min
    return (min + max) / 2
  })
  .filter(Boolean))
const averageSalaryText = computed(() => {
  if (!salarySamples.value.length) return '--'
  const avg = salarySamples.value.reduce((sum, n) => sum + n, 0) / salarySamples.value.length
  return `${(avg / 1000).toFixed(1)}K`
})
const employmentStatusLabel = computed(() => ({
  EMPLOYED: '已就业',
  OFFERED: '收到录用',
  SEEKING: '求职中',
  IDLE: '待投递'
}[empStore.status] || '状态同步中'))
const scopeLabel = computed(() => ({
  applicable: '当前可投递岗位',
  market: '当前市场参考',
  applications: '推荐概览',
  favorites: '推荐概览',
  target: '推荐概览'
}[props.activeTab] || '推荐概览'))
const progressValue = computed(() => {
  const total = props.applicableCount || totalApplicableCount.value
  if (!total) return String(applications.value.length)
  return `${applications.value.length}/${total}`
})

const metrics = computed(() => [
  {
    key: 'recommend',
    label: '推荐职位',
    value: props.items.length,
    unit: '个',
    trend: scopeLabel.value,
    desc: `全部 ${props.totalItems.length} 个，可投 ${totalApplicableCount.value} / 市场 ${totalReferenceCount.value}`,
    icon: Promotion,
    theme: 'blue'
  },
  {
    key: 'match',
    label: '高度匹配',
    value: highMatchCount.value,
    unit: '个',
    trend: '匹配度 >= 80%',
    desc: '按当前列表真实匹配分统计',
    icon: Aim,
    theme: 'violet'
  },
  {
    key: 'salary',
    label: '平均薪资',
    value: averageSalaryText.value,
    trend: `样本 ${salarySamples.value.length} 个`,
    desc: '按当前列表薪资均值统计',
    icon: TrendCharts,
    theme: 'cyan'
  },
  {
    key: 'progress',
    label: '投递进度',
    value: progressValue.value,
    trend: '已投递 / 可投递',
    desc: employmentStatusLabel.value,
    icon: Star,
    theme: 'purple'
  }
])

onMounted(async () => {
  try { applications.value = toList(await getMyApplications()) }
  catch { applications.value = [] }
})
</script>

<style src="./StudentJobMetrics.css"></style>
