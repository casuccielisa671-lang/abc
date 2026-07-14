<template>
  <div class="tool-page">
    <div class="tool-page-header">
      <el-button text @click="$router.push('/student/tools')">
        <el-icon><ArrowLeft /></el-icon> 返回工具箱
      </el-button>
      <h2>多岗位对比</h2>
      <p class="tool-desc">选择 2~4 个岗位，从薪资、技能、学历、城市等多维度并排对比</p>
    </div>

    <div class="select-bar">
      <el-select v-model="selected" multiple placeholder="选择岗位（2~4 个）" :multiple-limit="4" style="width: 100%" filterable>
        <el-option v-for="j in jobs" :key="j.id" :label="jobOptionLabel(j)" :value="j.id" />
      </el-select>
      <el-button type="primary" :disabled="selected.length < 2" @click="compare" :loading="loading">开始对比</el-button>
    </div>

    <el-alert v-if="!loading && !jobs.length" title="暂无可选岗位，请先在后台采集或导入岗位数据" type="warning" show-icon :closable="false" class="hint" />

    <el-table v-if="result && result.jobs" :data="dims" border stripe class="compare-table" highlight-current-row>
      <el-table-column label="维度" width="110" fixed>
        <template #default="{ row }">
          <div class="dim-cell">{{ row.label }}</div>
        </template>
      </el-table-column>
      <el-table-column v-for="job in result.jobs" :key="job.id" :label="job.title">
        <template #default="{ row }">
          <span v-if="row.key === 'base'">{{ job.company }} · {{ job.city }}</span>
          <span v-else-if="row.key === 'salary'" class="salary">{{ job.salaryRange }}</span>
          <span v-else-if="row.key === 'industry'" class="industry">{{ job.industry || '未分类' }}</span>
          <span v-else-if="row.key === 'education'">{{ job.education || '-' }}</span>
          <span v-else-if="row.key === 'experience'">{{ job.experience || '-' }}</span>
          <span v-else-if="row.key === 'skills'" class="skill-tags">
            <el-tag v-for="s in job.skills" :key="s" size="small">{{ s }}</el-tag>
          </span>
          <span v-else-if="row.key === 'publishDate'">{{ job.publishDate || '-' }}</span>
        </template>
      </el-table-column>
    </el-table>

    <template v-if="result && result.summary">
      <div v-if="decisionInsights.length" class="section">
        <h3 class="section-title">决策评分</h3>
        <div class="decision-grid">
          <div v-for="item in decisionInsights" :key="item.id" class="decision-card">
            <div class="decision-head">
              <span>{{ item.title }}</span>
              <b>{{ item.total }}</b>
            </div>
            <div class="mini-bars">
              <div><span>薪资</span><i :style="{ width: item.salaryScore + '%' }"></i></div>
              <div><span>成长</span><i :style="{ width: item.skillGrowth + '%' }"></i></div>
              <div><span>门槛</span><i class="warn" :style="{ width: item.barrier + '%' }"></i></div>
            </div>
            <p>{{ item.advice }}</p>
          </div>
        </div>
      </div>

      <div class="section">
        <h3 class="section-title">对比总结</h3>
        <div class="summary-box">
          <div class="sum-item">
            <el-icon><Top /></el-icon> 薪资最高：<b>{{ result.summary.highestSalary }}</b>
          </div>
          <div class="sum-item">
            <el-icon><Bottom /></el-icon> 薪资最低：<b>{{ result.summary.lowestSalary }}</b>
          </div>
          <div class="sum-item">
            <el-icon><Link /></el-icon> 共同技能：<template v-if="result.summary.commonSkills.length">{{ result.summary.commonSkills.join('、') }}</template><template v-else>无</template>
          </div>
        </div>
      </div>

      <div v-if="result.summary.uniqueSkills" class="section">
        <h3 class="section-title">各岗位独有技能</h3>
        <div v-for="job in result.jobs" :key="'u'+job.id" class="unique-block">
          <span class="unique-label">{{ job.title }}：</span>
          <template v-if="result.summary.uniqueSkills[job.title]?.length">
            <el-tag v-for="s in result.summary.uniqueSkills[job.title]" :key="s" size="small" type="warning">{{ s }}</el-tag>
          </template>
          <span v-else class="no-data">无独有技能</span>
        </div>
      </div>
    </template>

    <div v-if="!result && !loading" class="empty-state">
      <el-icon :size="48"><DataAnalysis /></el-icon>
      <p>选择 2 个以上岗位，点击"开始对比"查看结果</p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ArrowLeft, DataAnalysis, Top, Bottom, Link } from '@element-plus/icons-vue'
import { getRecommend, getToolJobs, compareJobs } from '@/api/student'
import { toList } from '@/utils/list'
import { normalizeJob, jobOptionLabel, buildCompareResult } from '@/utils/jobTools'
import { buildJobDecisionInsights } from '@/utils/toolInsights'

const jobs = ref([])
const selected = ref([])
const loading = ref(false)
const result = ref(null)
const decisionInsights = computed(() => buildJobDecisionInsights(result.value))

const dims = [
  { key: 'base', label: '基本信息' },
  { key: 'salary', label: '薪资范围' },
  { key: 'industry', label: '行业' },
  { key: 'education', label: '学历要求' },
  { key: 'experience', label: '经验要求' },
  { key: 'skills', label: '技能标签' },
  { key: 'publishDate', label: '发布日期' }
]

onMounted(async () => {
  await loadJobs()
})

const compare = async () => {
  if (selected.value.length < 2) return
  loading.value = true
  try {
    const data = await compareJobs(selected.value)
    result.value = data?.jobs?.length ? normalizeCompare(data) : buildCompareResult(selected.value, jobs.value)
  } catch {
    result.value = buildCompareResult(selected.value, jobs.value)
  }
  loading.value = false
}

async function loadJobs() {
  loading.value = true
  try {
    const recommended = toList(await getRecommend(30)).map(normalizeJob).filter(job => job.id)
    jobs.value = recommended.length ? recommended : toList(await getToolJobs()).map(normalizeJob).filter(job => job.id)
  } catch {
    jobs.value = []
  } finally {
    loading.value = false
  }
}

function normalizeCompare(data) {
  const normalizedJobs = toList(data.jobs).map(normalizeJob)
  const fallback = buildCompareResult(normalizedJobs.map(job => job.id), normalizedJobs)
  return {
    ...data,
    jobs: normalizedJobs,
    summary: {
      ...data.summary,
      commonSkills: data.summary?.commonSkills || fallback.summary.commonSkills,
      uniqueSkills: data.summary?.uniqueSkills || fallback.summary.uniqueSkills,
      highestSalary: fallback.summary.highestSalary,
      lowestSalary: fallback.summary.lowestSalary
    }
  }
}
</script>

<style scoped>
.tool-page { max-width: 1000px; margin: 0 auto; padding: 16px 0 40px; }
.tool-page-header { margin-bottom: 24px; }
.tool-page-header h2 { font-size: 22px; font-weight: 600; color: var(--app-ink); margin: 12px 0 6px; }
.tool-desc { font-size: 14px; color: var(--app-ink-3); margin: 0; }
.select-bar { display: flex; gap: 12px; margin-bottom: 28px; }
.hint { margin-bottom: 18px; }
.compare-table { margin-bottom: 28px; }
.dim-cell { font-weight: 600; font-size: 13px; color: var(--app-ink-2); }
.salary { color: #e74c3c; font-weight: 600; }
.skill-tags { display: flex; flex-wrap: wrap; gap: 4px; }
.section { margin-bottom: 28px; }
.section-title { font-size: 16px; font-weight: 600; color: var(--app-ink); margin: 0 0 14px; }
.summary-box { background: #fff; border: 1px solid #e8eaed; border-radius: 10px; padding: 18px; display: flex; flex-direction: column; gap: 10px; }
html.dark .summary-box { background: #1e1f22; border-color: #2e3035; }
.sum-item { display: flex; align-items: center; gap: 8px; font-size: 14px; color: var(--app-ink-2); }
.sum-item b { color: var(--app-ink); }
.decision-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 12px; }
.decision-card { background: #fff; border: 1px solid #e8eaed; border-radius: 12px; padding: 16px; }
html.dark .decision-card { background: #1e1f22; border-color: #2e3035; }
.decision-head { display: flex; align-items: center; justify-content: space-between; gap: 12px; font-size: 14px; font-weight: 600; }
.decision-head b { color: #5470c6; font-size: 24px; }
.mini-bars { display: grid; gap: 8px; margin: 12px 0; }
.mini-bars div { display: grid; grid-template-columns: 42px 1fr; align-items: center; gap: 8px; font-size: 12px; color: var(--app-ink-3); }
.mini-bars div::after { content: ''; height: 6px; background: #f1f3f4; border-radius: 4px; grid-column: 2; grid-row: 1; }
.mini-bars i { height: 6px; background: #27ae60; border-radius: 4px; grid-column: 2; grid-row: 1; z-index: 1; }
.mini-bars i.warn { background: #f59e0b; }
.decision-card p { margin: 0; color: var(--app-ink-2); font-size: 13px; line-height: 1.6; }
.unique-block { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; flex-wrap: wrap; }
.unique-label { font-size: 13px; font-weight: 500; white-space: nowrap; }
.no-data { font-size: 13px; color: var(--app-ink-4); }
.empty-state { text-align: center; padding: 80px 0; color: var(--app-ink-3); }
.empty-state p { margin-top: 16px; }
</style>
