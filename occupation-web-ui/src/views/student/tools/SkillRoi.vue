<template>
  <div class="tool-page">
    <div class="tool-page-header">
      <el-button text @click="$router.push('/student/tools')">
        <el-icon><ArrowLeft /></el-icon> 返回工具箱
      </el-button>
      <h2>技能 ROI 分析</h2>
      <p class="tool-desc">输入技能名称，量化学习该技能的薪资回报与市场需求</p>
    </div>

    <div class="search-bar">
      <el-input v-model="skill" placeholder="输入技能名称，如：Spring Boot、Python、React" size="large" @keyup.enter="analyze">
        <template #append><el-button type="primary" @click="analyze" :loading="loading">分析</el-button></template>
      </el-input>
    </div>

    <template v-if="result">
      <div class="section">
        <h3 class="section-title">市场概览</h3>
        <div class="stats-row">
          <div class="stat-item"><span class="stat-num">{{ result.marketShare }}%</span><span class="stat-label">市场覆盖率</span></div>
          <div class="stat-item"><span class="stat-num">{{ result.jobCount }}</span><span class="stat-label">相关岗位数</span></div>
          <div class="stat-item"><span class="stat-num">¥{{ fmt(result.avgSalary) }}</span><span class="stat-label">平均薪资</span></div>
          <div class="stat-item"><span class="stat-num">¥{{ fmt(result.medianSalary) }}</span><span class="stat-label">薪资中位</span></div>
        </div>
        <div v-if="result.salaryPremium" class="premium-badge" :class="result.salaryPremium > 3000 ? 'good' : result.salaryPremium > 0 ? 'fair' : 'weak'">
          薪资溢价：+¥{{ result.salaryPremium / 1000 }}k/月
        </div>
      </div>

      <div v-if="result.relatedJobs?.length" class="section">
        <h3 class="section-title">关联岗位 Top 5</h3>
        <div class="related-list">
          <div v-for="j in result.relatedJobs" :key="j.title" class="related-item">
            <span class="rj-name">{{ j.title }}</span>
            <span class="rj-count">{{ j.jobCount }} 个岗位</span>
            <span class="rj-salary">¥{{ fmt(j.avgSalary) }}/月</span>
          </div>
        </div>
      </div>

      <div v-if="result.suggestions?.length" class="section">
        <h3 class="section-title">学习建议</h3>
        <div class="suggestions">
          <div v-for="(s, i) in result.suggestions" :key="i" class="sg-item">
            <span class="sg-idx">{{ i + 1 }}</span>
            <span>{{ s }}</span>
          </div>
        </div>
      </div>
    </template>

    <div v-else class="empty-state">
      <el-icon :size="48"><TrendCharts /></el-icon>
      <p>输入技能名称，点击"分析"查看 ROI 数据</p>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ArrowLeft, TrendCharts } from '@element-plus/icons-vue'
import { skillRoi } from '@/api/student'

const skill = ref('')
const loading = ref(false)
const result = ref(null)
const fmt = v => v ? (v / 1000).toFixed(1) + 'k' : '--'

const analyze = async () => {
  if (!skill.value.trim()) return
  loading.value = true
  try { result.value = await skillRoi(skill.value.trim()) } catch { result.value = null }
  loading.value = false
}
</script>

<style scoped>
.tool-page { max-width: 800px; margin: 0 auto; padding: 16px 0 40px; }
.tool-page-header { margin-bottom: 24px; }
.tool-page-header h2 { font-size: 22px; font-weight: 600; color: var(--app-ink); margin: 12px 0 6px; }
.tool-desc { font-size: 14px; color: var(--app-ink-3); margin: 0; }
.search-bar { margin-bottom: 32px; max-width: 500px; }
.section { margin-bottom: 28px; }
.section-title { font-size: 16px; font-weight: 600; color: var(--app-ink); margin: 0 0 14px; }
.stats-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; }
.stat-item { background: #fff; border: 1px solid #e8eaed; border-radius: 10px; padding: 16px; text-align: center; }
html.dark .stat-item { background: #1e1f22; border-color: #2e3035; }
.stat-num { font-size: 22px; font-weight: 700; display: block; margin-bottom: 4px; }
.stat-label { font-size: 12px; color: var(--app-ink-3); }
.premium-badge { display: inline-block; margin-top: 12px; padding: 6px 14px; border-radius: 6px; font-size: 14px; font-weight: 600; }
.premium-badge.good { background: #ecfdf5; color: #27ae60; }
.premium-badge.fair { background: #fffbeb; color: #f39c12; }
.premium-badge.weak { background: #f1f3f4; color: var(--app-ink-3); }
.related-list { display: flex; flex-direction: column; gap: 8px; }
.related-item { display: flex; align-items: center; gap: 16px; background: #fff; border: 1px solid #e8eaed; border-radius: 8px; padding: 12px 16px; }
html.dark .related-item { background: #1e1f22; border-color: #2e3035; }
.rj-name { font-size: 14px; font-weight: 500; flex: 1; }
.rj-count { font-size: 12px; color: var(--app-ink-3); }
.rj-salary { font-size: 14px; font-weight: 600; color: #e74c3c; }
.suggestions { background: #fff; border: 1px solid #e8eaed; border-radius: 10px; padding: 20px; display: flex; flex-direction: column; gap: 12px; }
html.dark .suggestions { background: #1e1f22; border-color: #2e3035; }
.sg-item { display: flex; align-items: flex-start; gap: 10px; font-size: 14px; color: var(--app-ink-2); line-height: 1.6; }
.sg-idx { width: 22px; height: 22px; border-radius: 50%; background: #f1f3f4; font-size: 12px; font-weight: 600; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
html.dark .sg-idx { background: #2e3035; }
.empty-state { text-align: center; padding: 80px 0; color: var(--app-ink-3); }
.empty-state p { margin-top: 16px; }
</style>
