<template>
  <div class="tool-page">
    <div class="tool-page-header">
      <el-button text @click="$router.push('/hr/tools')">
        <el-icon><ArrowLeft /></el-icon> 返回工具箱
      </el-button>
      <h2>薪资竞争力分析</h2>
      <p class="tool-desc">输入岗位，对比市场薪资分位，评估自家 offer 竞争力</p>
    </div>

    <div class="search-bar">
      <el-input v-model="jobTitle" placeholder="输入岗位名称，如：Java开发工程师" size="large" clearable @keyup.enter="search">
        <template #append><el-button type="primary" @click="search" :loading="loading">分析</el-button></template>
      </el-input>
    </div>

    <template v-if="result">
      <div class="section">
        <h3 class="section-title">市场薪资分位</h3>
        <div class="percentiles">
          <div class="pct-item">
            <div class="pct-label">P25（低位）</div>
            <div class="pct-val">¥{{ fmt(result.p25) }}</div>
          </div>
          <div class="pct-item highlight">
            <div class="pct-label">P50（中位）</div>
            <div class="pct-val">¥{{ fmt(result.p50) }}</div>
          </div>
          <div class="pct-item">
            <div class="pct-label">P75（高位）</div>
            <div class="pct-val">¥{{ fmt(result.p75) }}</div>
          </div>
          <div class="pct-item">
            <div class="pct-label">P90（顶位）</div>
            <div class="pct-val">¥{{ fmt(result.p90) }}</div>
          </div>
        </div>
      </div>

      <div class="section">
        <h3 class="section-title">Offer 竞争力评估</h3>
        <div class="offer-input">
          <span class="oi-label">你的 Offer 薪资：</span>
          <el-input-number v-model="offerSalary" :min="0" :step="1000" />
          <span class="oi-unit">元/月</span>
          <el-button type="primary" @click="evaluate">评估</el-button>
        </div>

        <div v-if="salaryAdvice" class="band-grid">
          <div v-for="band in salaryAdvice.bands" :key="band.label" class="band-card">
            <span>{{ band.label }}</span>
            <b>¥{{ fmt(band.value) }}</b>
          </div>
        </div>

        <div v-if="evaluation" class="eval-result" :class="evaluation.level">
          <div class="eval-icon">
            <el-icon :size="32"><component :is="evalIcon" /></el-icon>
          </div>
          <div class="eval-text">
            <div class="eval-title">{{ evaluation.title }}</div>
            <div class="eval-desc">{{ evaluation.desc }}</div>
          </div>
        </div>
        <div v-else-if="salaryAdvice" class="advice-note">{{ salaryAdvice.message }}</div>
      </div>

      <div class="section">
        <h3 class="section-title">城市薪资对比</h3>
        <div class="city-compare">
          <div v-for="c in result.cityData" :key="c.city" class="city-item">
            <div class="city-name">{{ c.city }}</div>
            <div class="city-bar-track"><div class="city-bar-fill" :style="{ width: (c.salary / maxCity) * 100 + '%' }"></div></div>
            <div class="city-val">¥{{ fmt(c.salary) }}</div>
          </div>
        </div>
      </div>
    </template>

    <div v-else class="empty">
      <el-icon :size="48"><Coin /></el-icon>
      <p>输入岗位名称，点击"分析"查看薪资竞争力</p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ArrowLeft, Coin, Top, Bottom, Warning } from '@element-plus/icons-vue'
import { getToolJobs } from '@/api/student'
import { toList } from '@/utils/list'
import { buildSalaryAdvice } from '@/utils/toolInsights'

const jobTitle = ref('')
const loading = ref(false)
const result = ref(null)
const offerSalary = ref(18000)
const evaluation = ref(null)

const fmt = v => `${((v || 0) / 1000).toFixed(1)}k`
const maxCity = computed(() => Math.max(...(result.value?.cityData || []).map(c => c.salary || 0), 1))
const salaryAdvice = computed(() => buildSalaryAdvice(result.value, offerSalary.value))

const evalIcon = computed(() => {
  if (!evaluation.value) return Coin
  return evaluation.value.level === 'good' ? Top : evaluation.value.level === 'fair' ? Warning : Bottom
})

const search = async () => {
  if (!jobTitle.value.trim()) return
  loading.value = true
  try {
    const data = await getToolJobs({ keyword: jobTitle.value.trim(), pageSize: 100 })
    result.value = buildSalaryBenchmark(toList(data))
    evaluation.value = null
  } catch { result.value = null }
  loading.value = false
}

const evaluate = () => {
  if (!result.value) return
  const s = offerSalary.value
  const p50 = result.value.p50
  const p75 = result.value.p75
  if (s >= p75) {
    evaluation.value = { level: 'good', title: '竞争力强', desc: `你的 offer 高于市场 P75（¥${fmt(p75)}），在市场中处于前 25%，对候选人吸引力较强。` }
  } else if (s >= p50) {
    evaluation.value = { level: 'fair', title: '竞争力中等', desc: `你的 offer 处于市场中位附近（P50=¥${fmt(p50)}），建议适当上浮 10%~15% 提升吸引力。` }
  } else {
    evaluation.value = { level: 'weak', title: '竞争力偏弱', desc: `你的 offer 低于市场中位（P50=¥${fmt(p50)}），建议上调至 ¥${fmt(Math.round(p50 * 1.1))} 以上以提升竞争力。` }
  }
}

function buildSalaryBenchmark(jobs) {
  const salaries = jobs.map(midSalary).filter(Boolean).sort((a, b) => a - b)
  const fallback = salaries.length ? salaries : [12000, 16000, 20000, 26000]
  const cityMap = new Map()
  jobs.forEach(job => {
    const salary = midSalary(job)
    if (!salary || !job.city) return
    const list = cityMap.get(job.city) || []
    list.push(salary)
    cityMap.set(job.city, list)
  })
  const cityData = [...cityMap.entries()]
    .map(([city, list]) => ({ city, salary: Math.round(list.reduce((sum, value) => sum + value, 0) / list.length) }))
    .sort((a, b) => b.salary - a.salary)
    .slice(0, 8)
  return {
    p25: percentile(fallback, 0.25),
    p50: percentile(fallback, 0.5),
    p75: percentile(fallback, 0.75),
    p90: percentile(fallback, 0.9),
    cityData
  }
}

function midSalary(job) {
  const min = Number(job.salaryMin)
  const max = Number(job.salaryMax)
  if (min > 0 && max > 0) return Math.round((min + max) / 2)
  if (min > 0) return min
  if (max > 0) return max
  return 0
}

function percentile(values, ratio) {
  return values[Math.min(values.length - 1, Math.floor((values.length - 1) * ratio))] || 0
}
</script>

<style scoped>
.tool-page { max-width: 800px; margin: 0 auto; padding: 16px 0 40px; }
.tool-page-header { margin-bottom: 24px; }
.tool-page-header h2 { font-size: 22px; font-weight: 600; color: var(--app-ink); margin: 12px 0 6px; }
.tool-desc { font-size: 14px; color: var(--app-ink-3); margin: 0; }

.search-bar { margin-bottom: 32px; max-width: 500px; }

.section { margin-bottom: 32px; }
.section-title { font-size: 16px; font-weight: 600; color: var(--app-ink); margin: 0 0 16px; }

.percentiles { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; }
.pct-item { background: #fff; border: 1px solid #e8eaed; border-radius: 10px; padding: 16px; text-align: center; }
html.dark .pct-item { background: #1e1f22; border-color: #2e3035; }
.pct-item.highlight { border-color: #5470c6; background: #f0f4ff; }
html.dark .pct-item.highlight { border-color: #5470c6; background: rgba(84,112,198,0.1); }
.pct-label { font-size: 12px; color: var(--app-ink-3); margin-bottom: 6px; }
.pct-val { font-size: 20px; font-weight: 700; }

.offer-input { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; }
.oi-label { font-size: 14px; color: var(--app-ink-2); }
.oi-unit { font-size: 13px; color: var(--app-ink-3); }
.band-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; margin-bottom: 16px; }
.band-card { background: #fff; border: 1px solid #e8eaed; border-radius: 10px; padding: 14px; display: flex; flex-direction: column; gap: 6px; text-align: center; }
html.dark .band-card { background: #1e1f22; border-color: #2e3035; }
.band-card span { font-size: 12px; color: var(--app-ink-3); }
.band-card b { font-size: 20px; color: #5470c6; }
.advice-note { background: #f7f9fc; border-radius: 10px; padding: 12px 14px; font-size: 13px; color: var(--app-ink-2); line-height: 1.7; }
html.dark .advice-note { background: rgba(255,255,255,0.05); }

.eval-result { display: flex; align-items: center; gap: 16px; padding: 20px; border-radius: 10px; }
.eval-result.good { background: #ecfdf5; border: 1px solid #a7f3d0; }
.eval-result.fair { background: #fffbeb; border: 1px solid #fde68a; }
.eval-result.weak { background: #fef2f2; border: 1px solid #fecaca; }
html.dark .eval-result.good { background: rgba(39,174,96,0.1); border-color: rgba(39,174,96,0.3); }
html.dark .eval-result.fair { background: rgba(243,156,18,0.1); border-color: rgba(243,156,18,0.3); }
html.dark .eval-result.weak { background: rgba(231,76,60,0.1); border-color: rgba(231,76,60,0.3); }
.eval-icon { color: var(--app-ink-2); }
.eval-title { font-size: 16px; font-weight: 600; margin-bottom: 4px; }
.eval-desc { font-size: 13px; color: var(--app-ink-2); line-height: 1.6; }

.city-compare { display: flex; flex-direction: column; gap: 10px; }
.city-item { display: flex; align-items: center; gap: 12px; }
.city-name { width: 60px; font-size: 13px; font-weight: 500; }
.city-bar-track { flex: 1; height: 8px; background: #f1f3f4; border-radius: 4px; overflow: hidden; }
html.dark .city-bar-track { background: #2e3035; }
.city-bar-fill { height: 100%; background: #5470c6; border-radius: 4px; transition: width 0.6s ease; }
.city-val { font-size: 13px; font-weight: 600; width: 50px; }

.empty { text-align: center; padding: 80px 0; color: var(--app-ink-3); }
.empty p { margin-top: 16px; }
@media (max-width: 768px) { .percentiles, .band-grid { grid-template-columns: 1fr; } .offer-input { align-items: flex-start; flex-direction: column; } }
</style>
