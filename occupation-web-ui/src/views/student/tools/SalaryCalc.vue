<template>
  <div class="tool-page">
    <div class="tool-page-header">
      <el-button text @click="$router.push('/student/tools')">
        <el-icon><ArrowLeft /></el-icon> 返回工具箱
      </el-button>
      <h2>期望薪资计算器</h2>
      <p class="tool-desc">输入城市、岗位、学历、经验，基于历史数据给出合理期望薪资范围</p>
    </div>

    <div class="form-box">
      <div class="form-row">
        <span class="form-label">意向城市</span>
        <el-input v-model="city" placeholder="如：北京、上海、深圳" clearable />
      </div>
      <div class="form-row">
        <span class="form-label">岗位关键词</span>
        <el-input v-model="keyword" placeholder="如：Java开发、前端工程师" clearable />
      </div>
      <div class="form-row">
        <span class="form-label">学历</span>
        <el-select v-model="education" placeholder="选择学历" clearable style="width: 100%">
          <el-option v-for="e in edus" :key="e" :label="e" :value="e" />
        </el-select>
      </div>
      <div class="form-row">
        <span class="form-label">经验</span>
        <el-select v-model="experience" placeholder="选择经验年限" clearable style="width: 100%">
          <el-option v-for="e in exps" :key="e" :label="e" :value="e" />
        </el-select>
      </div>
      <el-button type="primary" size="large" @click="calc" :loading="loading">计算期望薪资</el-button>
    </div>

    <template v-if="result">
      <div class="section">
        <h3 class="section-title">薪资建议</h3>
        <div class="salary-rec">
          <div class="rec-high">¥{{ fmt(result.suggestedMin) }}<span class="rec-label">建议下限</span></div>
          <div class="rec-divider">~</div>
          <div class="rec-high">¥{{ fmt(result.suggestedMax) }}<span class="rec-label">建议上限</span></div>
        </div>
        <div class="rec-meta">基于 {{ result.sampleCount }} 条市场数据计算</div>
      </div>

      <div class="section">
        <h3 class="section-title">市场分位</h3>
        <div class="percentiles">
          <div class="pct-item"><span class="pct-val">¥{{ fmt(result.marketP25) }}</span><span class="pct-label">P25</span></div>
          <div class="pct-item highlight"><span class="pct-val">¥{{ fmt(result.marketMedian) }}</span><span class="pct-label">P50（中位）</span></div>
          <div class="pct-item"><span class="pct-val">¥{{ fmt(result.marketP75) }}</span><span class="pct-label">P75</span></div>
        </div>
      </div>

      <div v-if="result.cityBreakdown?.length" class="section">
        <h3 class="section-title">城市薪资对比</h3>
        <div class="city-list">
          <div v-for="c in result.cityBreakdown" :key="c.city" class="city-item">
            <span class="city-name">{{ c.city }}</span>
            <div class="city-bar-track"><div class="city-bar-fill" :style="{ width: barWidth(c.avgSalary) + '%' }"></div></div>
            <span class="city-salary">¥{{ fmt(c.avgSalary) }}</span>
            <span class="city-count">{{ c.jobCount }}个</span>
          </div>
        </div>
      </div>
    </template>

    <div v-else class="empty-state">
      <el-icon :size="48"><Coin /></el-icon>
      <p>输入城市和岗位等信息，点击"计算期望薪资"查看结果</p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ArrowLeft, Coin } from '@element-plus/icons-vue'
import { salaryCalc } from '@/api/student'

const city = ref('')
const keyword = ref('')
const education = ref('')
const experience = ref('')
const loading = ref(false)
const result = ref(null)

const edus = ['博士', '硕士', '本科', '大专']
const exps = ['应届生', '1-3年', '3-5年', '5-10年', '10年以上']
const fmt = v => v ? (v / 1000).toFixed(1) + 'k' : '--'

const maxCity = computed(() => {
  if (!result.value?.cityBreakdown?.length) return 1
  return Math.max(...result.value.cityBreakdown.map(c => c.avgSalary))
})
const barWidth = v => maxCity.value > 0 ? Math.round(v * 100 / maxCity.value) : 0

const calc = async () => {
  loading.value = true
  const params = {}
  if (city.value.trim()) params.city = city.value.trim()
  if (keyword.value.trim()) params.keyword = keyword.value.trim()
  if (education.value) params.education = education.value
  if (experience.value) params.experience = experience.value
  try { result.value = await salaryCalc(params) } catch { result.value = null }
  loading.value = false
}
</script>

<style scoped>
.tool-page { max-width: 760px; margin: 0 auto; padding: 16px 0 40px; }
.tool-page-header { margin-bottom: 24px; }
.tool-page-header h2 { font-size: 22px; font-weight: 600; color: var(--app-ink); margin: 12px 0 6px; }
.tool-desc { font-size: 14px; color: var(--app-ink-3); margin: 0; }
.form-box { background: #fff; border: 1px solid #e8eaed; border-radius: 12px; padding: 24px; display: flex; flex-direction: column; gap: 16px; margin-bottom: 32px; }
html.dark .form-box { background: #1e1f22; border-color: #2e3035; }
.form-row { display: flex; align-items: center; gap: 14px; }
.form-label { width: 80px; font-size: 14px; font-weight: 500; color: var(--app-ink-2); flex-shrink: 0; }
.section { margin-bottom: 28px; }
.section-title { font-size: 16px; font-weight: 600; color: var(--app-ink); margin: 0 0 14px; }
.salary-rec { display: flex; align-items: center; justify-content: center; gap: 24px; padding: 24px; background: #f0f4ff; border-radius: 12px; }
html.dark .salary-rec { background: rgba(84,112,198,0.1); }
.rec-high { display: flex; flex-direction: column; align-items: center; font-size: 28px; font-weight: 700; color: #5470c6; }
.rec-label { font-size: 12px; font-weight: 400; color: var(--app-ink-3); margin-top: 4px; }
.rec-divider { font-size: 20px; color: var(--app-ink-3); }
.rec-meta { text-align: center; font-size: 12px; color: var(--app-ink-3); margin-top: 8px; }
.percentiles { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; }
.pct-item { background: #fff; border: 1px solid #e8eaed; border-radius: 10px; padding: 16px; text-align: center; }
html.dark .pct-item { background: #1e1f22; border-color: #2e3035; }
.pct-item.highlight { border-color: #5470c6; background: #f0f4ff; }
html.dark .pct-item.highlight { border-color: #5470c6; background: rgba(84,112,198,0.1); }
.pct-val { font-size: 20px; font-weight: 700; display: block; margin-bottom: 4px; }
.pct-label { font-size: 12px; color: var(--app-ink-3); }
.city-list { display: flex; flex-direction: column; gap: 10px; }
.city-item { display: flex; align-items: center; gap: 12px; }
.city-name { width: 60px; font-size: 13px; font-weight: 500; }
.city-bar-track { flex: 1; height: 8px; background: #f1f3f4; border-radius: 4px; overflow: hidden; }
html.dark .city-bar-track { background: #2e3035; }
.city-bar-fill { height: 100%; background: #5470c6; border-radius: 4px; transition: width 0.6s ease; }
.city-salary { font-size: 13px; font-weight: 600; min-width: 55px; text-align: right; }
.city-count { font-size: 12px; color: var(--app-ink-3); }
.empty-state { text-align: center; padding: 80px 0; color: var(--app-ink-3); }
.empty-state p { margin-top: 16px; }
</style>
