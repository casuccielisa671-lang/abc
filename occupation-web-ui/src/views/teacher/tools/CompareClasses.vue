<template>
  <div class="tool-page">
    <div class="tool-page-header">
      <el-button text @click="$router.push('/teacher/tools')">
        <el-icon><ArrowLeft /></el-icon> 返回工具箱
      </el-button>
      <h2>班级就业对比</h2>
      <p class="tool-desc">选择多个班级，从就业率、薪资、去向、技能等维度并排对比</p>
    </div>

    <div class="select-bar">
      <el-select v-model="selected" multiple placeholder="选择班级（最多 5 个）" :multiple-limit="5" style="width: 100%">
        <el-option v-for="c in classList" :key="c.id" :label="c.name" :value="c.id" />
      </el-select>
      <el-button type="primary" :disabled="selected.length < 2" @click="compare" :loading="loading">开始对比</el-button>
    </div>

    <template v-if="result">
      <div v-if="classInsights" class="section">
        <h3 class="section-title">综合诊断</h3>
        <div class="insight-grid">
          <div class="insight-card"><span>就业领先</span><b>{{ classInsights.best }}</b></div>
          <div class="insight-card"><span>薪资领先</span><b>{{ classInsights.salaryLeader }}</b></div>
          <div class="insight-card warn"><span>优先关注</span><b>{{ classInsights.risk }}</b></div>
          <div class="insight-card"><span>就业差距</span><b>{{ classInsights.employmentGap }}%</b></div>
        </div>
      </div>

      <div class="section">
        <h3 class="section-title">概览</h3>
        <div class="overview-grid">
          <div v-for="c in result.classes" :key="c.id" class="ov-card">
            <div class="ov-name">{{ c.name }}</div>
            <div class="ov-row">
              <div class="ov-item"><span class="ov-val">{{ c.studentCount }}</span><span class="ov-label">学生数</span></div>
              <div class="ov-item"><span class="ov-val blue">{{ c.employmentRate }}%</span><span class="ov-label">就业率</span></div>
              <div class="ov-item"><span class="ov-val">¥{{ fmt(c.avgSalary) }}</span><span class="ov-label">平均薪资</span></div>
              <div class="ov-item"><span class="ov-val green">{{ classScore(c) }}</span><span class="ov-label">综合分</span></div>
            </div>
          </div>
        </div>
      </div>

      <div class="section">
        <h3 class="section-title">就业率 & 薪资对比</h3>
        <div class="chart-row">
          <div class="chart-box">
            <div class="chart-hd">就业率</div>
            <div v-for="c in result.classes" :key="c.id" class="bar-item">
              <span class="bar-label">{{ c.name }}</span>
              <div class="bar-track"><div class="bar-fill" :style="{ width: c.employmentRate + '%', background: color(c.id) }"></div></div>
              <span class="bar-val">{{ c.employmentRate }}%</span>
            </div>
          </div>
          <div class="chart-box">
            <div class="chart-hd">平均薪资</div>
            <div v-for="c in result.classes" :key="c.id" class="bar-item">
              <span class="bar-label">{{ c.name }}</span>
              <div class="bar-track"><div class="bar-fill" :style="{ width: pct(c.avgSalary) + '%', background: color(c.id) }"></div></div>
              <span class="bar-val">¥{{ fmt(c.avgSalary) }}</span>
            </div>
          </div>
        </div>
      </div>

      <div class="section">
        <h3 class="section-title">去向分布</h3>
        <div class="dest-grid">
          <div v-for="c in result.classes" :key="c.id" class="dest-card">
            <div class="dest-name">{{ c.name }}</div>
            <div v-for="(d, i) in c.topDestinations" :key="i" class="dest-item">
              <span class="dest-idx">{{ i + 1 }}</span>
              <span class="dest-job">{{ d.jobCategory }}</span>
              <span class="dest-pct">{{ d.ratio }}%</span>
            </div>
          </div>
        </div>
      </div>

      <div class="section">
        <h3 class="section-title">对比总结</h3>
        <div class="summary">
          <div class="sum-item"><el-icon><Top /></el-icon>就业率最高：<b>{{ result.comparison.highestEmployment }}</b></div>
          <div class="sum-item"><el-icon><Bottom /></el-icon>就业率最低：<b>{{ result.comparison.lowestEmployment }}</b></div>
          <div class="sum-item"><el-icon><Link /></el-icon>共同去向：<b>{{ result.comparison.commonDestinations.join('、') }}</b></div>
        </div>
      </div>

      <div v-if="classInsights" class="section">
        <h3 class="section-title">辅导动作建议</h3>
        <div class="action-list">
          <div v-for="item in classInsights.actions" :key="item" class="action-item">{{ item }}</div>
        </div>
      </div>
    </template>

    <div v-else class="empty">
      <el-icon :size="48"><DataAnalysis /></el-icon>
      <p>选择 2 个以上班级，点击"开始对比"查看结果</p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ArrowLeft, DataAnalysis, Top, Bottom, Link } from '@element-plus/icons-vue'
import { getTeacherStudents, compareClasses } from '@/api/student'
import { toList } from '@/utils/list'
import { buildClassInsights, classScore } from '@/utils/toolInsights'

const classList = ref([])
const selected = ref([])
const loading = ref(false)
const result = ref(null)
const classInsights = computed(() => buildClassInsights(result.value))
const fmt = v => (v / 1000).toFixed(1) + 'k'
const pct = v => {
  const max = Math.max(...(result.value?.classes || []).map(c => c.avgSalary || 0), 1)
  return Math.round(((v || 0) / max) * 100)
}

const COLORS = ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de']
const color = (id) => COLORS[selected.value.indexOf(id) % COLORS.length]

onMounted(loadClasses)

const compare = async () => {
  if (selected.value.length < 2) return
  loading.value = true
  try { result.value = await compareClasses(selected.value) } catch { result.value = null }
  loading.value = false
}

async function loadClasses() {
  loading.value = true
  try {
    const data = await getTeacherStudents({ page: 1, size: 500 })
    const byId = new Map()
    toList(data).forEach(student => {
      if (!student.classId || byId.has(student.classId)) return
      byId.set(student.classId, {
        id: student.classId,
        name: student.classCode || `班级 ${student.classId}`
      })
    })
    classList.value = [...byId.values()]
  } catch {
    classList.value = []
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.tool-page { max-width: 1000px; margin: 0 auto; padding: 16px 0 40px; }
.tool-page-header { margin-bottom: 24px; }
.tool-page-header h2 { font-size: 22px; font-weight: 600; color: var(--app-ink); margin: 12px 0 6px; }
.tool-desc { font-size: 14px; color: var(--app-ink-3); margin: 0; }

.select-bar { display: flex; gap: 12px; margin-bottom: 32px; }
.select-bar .el-button { flex-shrink: 0; }

.section { margin-bottom: 32px; }
.section-title { font-size: 16px; font-weight: 600; color: var(--app-ink); margin: 0 0 16px; }

.overview-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 12px; }
.ov-card { background: #fff; border: 1px solid #e8eaed; border-radius: 10px; padding: 16px; }
html.dark .ov-card { background: #1e1f22; border-color: #2e3035; }
.ov-name { font-size: 13px; font-weight: 600; margin-bottom: 12px; }
.ov-row { display: flex; gap: 16px; }
.ov-item { display: flex; flex-direction: column; gap: 2px; }
.ov-val { font-size: 18px; font-weight: 700; }
.ov-val.blue { color: #5470c6; }
.ov-val.green { color: #27ae60; }
.ov-label { font-size: 11px; color: var(--app-ink-3); }

.insight-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; }
.insight-card { background: #fff; border: 1px solid #e8eaed; border-radius: 10px; padding: 16px; display: flex; flex-direction: column; gap: 8px; }
html.dark .insight-card { background: #1e1f22; border-color: #2e3035; }
.insight-card.warn { border-color: #f59e0b; background: #fffbeb; }
html.dark .insight-card.warn { background: rgba(245,158,11,0.1); }
.insight-card span { font-size: 12px; color: var(--app-ink-3); }
.insight-card b { font-size: 18px; color: var(--app-ink); }

.chart-row { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.chart-box { background: #fff; border: 1px solid #e8eaed; border-radius: 10px; padding: 20px; }
html.dark .chart-box { background: #1e1f22; border-color: #2e3035; }
.chart-hd { font-size: 14px; font-weight: 600; margin-bottom: 16px; }
.bar-item { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; }
.bar-label { width: 120px; font-size: 12px; color: var(--app-ink-2); text-align: right; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.bar-track { flex: 1; height: 10px; background: #f1f3f4; border-radius: 5px; overflow: hidden; }
html.dark .bar-track { background: #2e3035; }
.bar-fill { height: 100%; border-radius: 5px; transition: width 0.6s ease; }
.bar-val { font-size: 13px; font-weight: 600; width: 50px; }

.dest-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 12px; }
.dest-card { background: #fff; border: 1px solid #e8eaed; border-radius: 10px; padding: 16px; }
html.dark .dest-card { background: #1e1f22; border-color: #2e3035; }
.dest-name { font-size: 13px; font-weight: 600; margin-bottom: 12px; }
.dest-item { display: flex; align-items: center; gap: 8px; padding: 6px 0; }
.dest-idx { width: 20px; height: 20px; border-radius: 50%; background: #f1f3f4; font-size: 11px; font-weight: 600; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
html.dark .dest-idx { background: #2e3035; }
.dest-job { flex: 1; font-size: 13px; }
.dest-pct { font-size: 13px; font-weight: 600; color: var(--app-ink-2); }

.summary { background: #fff; border: 1px solid #e8eaed; border-radius: 10px; padding: 20px; display: flex; flex-direction: column; gap: 12px; }
html.dark .summary { background: #1e1f22; border-color: #2e3035; }
.sum-item { display: flex; align-items: center; gap: 8px; font-size: 14px; color: var(--app-ink-2); }
.sum-item b { color: var(--app-ink); }
.action-list { display: grid; gap: 10px; }
.action-item { background: #fff; border: 1px solid #e8eaed; border-radius: 10px; padding: 14px 16px; font-size: 14px; color: var(--app-ink-2); line-height: 1.7; }
html.dark .action-item { background: #1e1f22; border-color: #2e3035; }

.empty { text-align: center; padding: 80px 0; color: var(--app-ink-3); }
.empty p { margin-top: 16px; }

@media (max-width: 768px) { .chart-row, .insight-grid { grid-template-columns: 1fr; } }
</style>
