<template>
  <div class="tool-page">
    <div class="tool-page-header">
      <el-button text @click="$router.push('/student/tools')">
        <el-icon><ArrowLeft /></el-icon> 返回工具箱
      </el-button>
      <h2>求职清单生成器</h2>
      <p class="tool-desc">选择目标岗位，对比岗位要求与个人技能，生成差距分析与学习路径建议</p>
    </div>

    <div class="select-bar">
      <el-select v-model="selectedJobId" placeholder="选择目标岗位" filterable style="width: 100%">
        <el-option v-for="j in jobs" :key="j.id" :label="jobOptionLabel(j)" :value="j.id" />
      </el-select>
      <el-button type="primary" :disabled="!selectedJobId" @click="generate" :loading="loading">生成清单</el-button>
    </div>

    <el-alert v-if="!loading && !jobs.length" title="暂无可选岗位，请先在后台采集或导入岗位数据" type="warning" show-icon :closable="false" class="hint" />

    <template v-if="result">
      <div class="match-header">
        <h3 class="match-title">{{ result.jobTitle }}</h3>
        <div class="match-score-ring">
          <span class="match-num" :style="{ color: scoreColor }">{{ result.matchScore }}%</span>
          <span class="match-label">匹配度</span>
        </div>
      </div>

      <div v-if="checklistInsights" class="section">
        <h3 class="section-title">投递准备度</h3>
        <div class="ready-card">
          <div>
            <span>当前阶段</span>
            <b>{{ checklistInsights.readiness }}</b>
          </div>
          <div>
            <span>已具备技能</span>
            <b>{{ checklistInsights.owned }}</b>
          </div>
          <div>
            <span>待补齐技能</span>
            <b>{{ checklistInsights.missing }}</b>
          </div>
        </div>
        <div v-if="checklistInsights.priorities.length" class="priority-list">
          <div v-for="item in checklistInsights.priorities" :key="item.skill" class="priority-item">
            <el-tag type="warning" size="small">{{ item.level }}</el-tag>
            <b>{{ item.skill }}</b>
            <span>{{ item.action }}</span>
          </div>
        </div>
      </div>

      <div v-if="result.skillGaps?.length" class="section">
        <h3 class="section-title">技能差距分析</h3>
        <div class="gap-list">
          <div v-for="g in result.skillGaps" :key="g.skill" class="gap-item" :class="{ possessed: g.possessed }">
            <span class="gap-skill">{{ g.skill }}</span>
            <el-tag :type="g.possessed ? 'success' : 'warning'" size="small">{{ g.description }}</el-tag>
          </div>
        </div>
      </div>

      <div v-if="result.learningPath?.length" class="section">
        <h3 class="section-title">学习路径</h3>
        <div class="path-list">
          <div v-for="s in result.learningPath" :key="s.order" class="path-item">
            <div class="path-num">{{ s.order }}</div>
            <div class="path-body">
              <div class="path-title">{{ s.title }}</div>
              <div class="path-desc">{{ s.description }}</div>
              <span class="path-time">{{ s.estimatedTime }}</span>
            </div>
          </div>
        </div>
      </div>

      <div v-if="result.resumeTips?.length" class="section">
        <h3 class="section-title">简历优化建议</h3>
        <div class="tips">
          <div v-for="(t, i) in result.resumeTips" :key="i" class="tip-item">
            <el-icon><Check /></el-icon>{{ t }}
          </div>
        </div>
      </div>

      <div v-if="result.resources?.length" class="section">
        <h3 class="section-title">推荐学习资源</h3>
        <div class="tips">
          <div v-for="(r, i) in result.resources" :key="i" class="tip-item">
            <el-icon><Link /></el-icon>{{ r }}
          </div>
        </div>
      </div>

      <div v-if="checklistInsights" class="section">
        <h3 class="section-title">面试前动作</h3>
        <div class="tips">
          <div v-for="(t, i) in checklistInsights.interview" :key="i" class="tip-item">
            <el-icon><Check /></el-icon>{{ t }}
          </div>
        </div>
      </div>
    </template>

    <div v-else class="empty-state">
      <el-icon :size="48"><List /></el-icon>
      <p>选择一个目标岗位，点击"生成清单"查看差距分析</p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ArrowLeft, List, Check, Link } from '@element-plus/icons-vue'
import { getProfile, getRecommend, getToolJobs, jobChecklist } from '@/api/student'
import { toList } from '@/utils/list'
import { normalizeJob, jobOptionLabel, buildChecklistResult } from '@/utils/jobTools'
import { buildChecklistInsights } from '@/utils/toolInsights'

const jobs = ref([])
const selectedJobId = ref(null)
const loading = ref(false)
const result = ref(null)
const profile = ref(null)
const checklistInsights = computed(() => buildChecklistInsights(result.value))

const scoreColor = computed(() => {
  if (!result.value) return '#999'
  const s = result.value.matchScore
  return s >= 80 ? '#27ae60' : s >= 50 ? '#f39c12' : '#e74c3c'
})

onMounted(async () => {
  await Promise.all([loadJobs(), loadProfile()])
})

const generate = async () => {
  if (!selectedJobId.value) return
  loading.value = true
  const selectedJob = jobs.value.find(job => job.id === selectedJobId.value)
  try {
    const data = await jobChecklist(selectedJobId.value)
    result.value = data?.jobTitle ? data : buildChecklistResult(selectedJob, profile.value)
  } catch {
    result.value = buildChecklistResult(selectedJob, profile.value)
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

async function loadProfile() {
  try { profile.value = await getProfile() } catch { profile.value = null }
}
</script>

<style scoped>
.tool-page { max-width: 800px; margin: 0 auto; padding: 16px 0 40px; }
.tool-page-header { margin-bottom: 24px; }
.tool-page-header h2 { font-size: 22px; font-weight: 600; color: var(--app-ink); margin: 12px 0 6px; }
.tool-desc { font-size: 14px; color: var(--app-ink-3); margin: 0; }
.select-bar { display: flex; gap: 12px; margin-bottom: 28px; }
.hint { margin-bottom: 18px; }
.match-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 28px; padding: 20px; background: #fff; border: 1px solid #e8eaed; border-radius: 12px; }
html.dark .match-header { background: #1e1f22; border-color: #2e3035; }
.match-title { font-size: 18px; font-weight: 600; color: var(--app-ink); margin: 0; }
.match-score-ring { text-align: center; }
.match-num { font-size: 32px; font-weight: 700; }
.match-label { font-size: 12px; color: var(--app-ink-3); }
.ready-card { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; margin-bottom: 12px; }
.ready-card div { background: #fff; border: 1px solid #e8eaed; border-radius: 10px; padding: 14px; display: flex; flex-direction: column; gap: 6px; }
html.dark .ready-card div { background: #1e1f22; border-color: #2e3035; }
.ready-card span { color: var(--app-ink-3); font-size: 12px; }
.ready-card b { color: var(--app-ink); font-size: 18px; }
.priority-list { display: grid; gap: 10px; }
.priority-item { display: flex; align-items: center; gap: 10px; background: #fffbeb; border: 1px solid #fde68a; border-radius: 10px; padding: 12px 14px; font-size: 13px; color: var(--app-ink-2); }
html.dark .priority-item { background: rgba(245,158,11,0.1); border-color: rgba(245,158,11,0.25); }
.priority-item span:last-child { line-height: 1.6; }
.section { margin-bottom: 28px; }
.section-title { font-size: 16px; font-weight: 600; color: var(--app-ink); margin: 0 0 14px; }
.gap-list { display: flex; flex-direction: column; gap: 8px; }
.gap-item { display: flex; align-items: center; justify-content: space-between; padding: 10px 16px; background: #fff; border: 1px solid #e8eaed; border-radius: 8px; }
html.dark .gap-item { background: #1e1f22; border-color: #2e3035; }
.gap-item.possessed { background: #f0fdf4; border-color: #bbf7d0; }
html.dark .gap-item.possessed { background: rgba(39,174,96,0.08); border-color: rgba(39,174,96,0.2); }
.gap-skill { font-size: 14px; font-weight: 500; }
.path-list { display: flex; flex-direction: column; gap: 12px; }
.path-item { display: flex; gap: 14px; }
.path-num { width: 32px; height: 32px; border-radius: 50%; background: #5470c6; color: #fff; display: flex; align-items: center; justify-content: center; font-size: 14px; font-weight: 600; flex-shrink: 0; }
.path-body { flex: 1; }
.path-title { font-size: 15px; font-weight: 600; margin-bottom: 4px; }
.path-desc { font-size: 13px; color: var(--app-ink-2); line-height: 1.5; margin-bottom: 4px; }
.path-time { font-size: 12px; color: #5470c6; background: #f0f4ff; padding: 2px 8px; border-radius: 4px; }
html.dark .path-time { background: rgba(84,112,198,0.15); }
.tips { background: #fff; border: 1px solid #e8eaed; border-radius: 10px; padding: 20px; display: flex; flex-direction: column; gap: 10px; }
html.dark .tips { background: #1e1f22; border-color: #2e3035; }
.tip-item { display: flex; align-items: flex-start; gap: 8px; font-size: 14px; color: var(--app-ink-2); line-height: 1.6; }
.empty-state { text-align: center; padding: 80px 0; color: var(--app-ink-3); }
.empty-state p { margin-top: 16px; }
@media (max-width: 768px) { .ready-card { grid-template-columns: 1fr; } .priority-item { align-items: flex-start; flex-direction: column; } }
</style>
