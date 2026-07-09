<template>
  <div class="teacher-suggestions">
    <div class="page-head">
      <h2 class="page-title">教学建议报告</h2>
      <p class="page-sub">
        对比市场热门技能与本校学生掌握率，找出需要补强的技能。
        <template v-if="studentsWithProfile">掌握率基于本校 {{ studentsWithProfile }} 名已填写画像的学生。</template>
      </p>
    </div>

    <!-- 技能缺口分析 -->
    <el-card>
      <template #header>技能缺口分析</template>

      <el-empty
        v-if="!loading && !skillGaps.length"
        description="暂无技能缺口数据。请先在管理端执行「手动重算分析数据」，并确保本校学生已填写画像。"
      />
      <el-table v-else :data="skillGaps" v-loading="loading" stripe>
        <el-table-column prop="skill" label="技能" min-width="130" />
        <el-table-column label="市场需求度" width="180">
          <template #default="{ row }">
            <div class="meter">
              <el-progress :percentage="row.marketDemand" :stroke-width="8" :show-text="false" color="#2a78d6" />
              <span class="meter-num">{{ row.marketDemand }}%</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="学生掌握率" width="180">
          <template #default="{ row }">
            <div class="meter">
              <el-progress :percentage="row.studentRate" :stroke-width="8" :show-text="false" :color="rateColor" />
              <span class="meter-num">{{ row.studentRate }}%</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="缺口" width="110">
          <template #default="{ row }">
            <el-tag :type="gapTag(row.gap)" size="small">{{ gapLabel(row.gap) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="证据" width="170">
          <template #default="{ row }">
            <span class="evidence">{{ row.jobCount }} 个岗位要求 · {{ row.masteredCount }} 人掌握</span>
          </template>
        </el-table-column>
        <el-table-column prop="suggestion" label="建议" min-width="260" show-overflow-tooltip />
      </el-table>
    </el-card>

    <!-- 课程改革建议 -->
    <el-card style="margin-top:16px">
      <template #header>课程改革建议</template>
      <el-empty v-if="!loading && !courseSuggestions.length" description="暂无课程改革建议" />
      <el-timeline v-else>
        <el-timeline-item
          v-for="item in courseSuggestions" :key="item.id"
          :timestamp="priorityLabel(item.priority)"
          :color="priorityColor(item.priority)"
        >
          <h4 class="sugg-title">{{ item.title }}</h4>
          <p class="sugg-desc">{{ item.description }}</p>
        </el-timeline-item>
      </el-timeline>
    </el-card>

    <!-- 市场趋势 -->
    <el-card style="margin-top:16px">
      <template #header>市场趋势（按月）</template>
      <div class="trend-pair">
        <div>
          <div class="trend-title">职位数量</div>
          <div ref="trendCountChart" class="chart-box"></div>
        </div>
        <div>
          <div class="trend-title">平均薪资</div>
          <div ref="trendSalaryChart" class="chart-box"></div>
        </div>
      </div>
      <el-empty v-if="!loading && !trendData.length" description="暂无趋势数据" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import { getDashboard } from '@/api/admin'
import { getTeacherSuggestions } from '@/api/student'
import * as echarts from 'echarts'
import { useAppStore } from '@/store/app'
import { chartThemeName, primarySeriesColor, moneySeriesColor } from '@/styles/chartTheme'

const appStore = useAppStore()
const loading = ref(false)

/** 全部来自后端 /teacher/suggestions —— 不再有任何随机数或写死文案 */
const skillGaps = ref([])
const courseSuggestions = ref([])
const studentsWithProfile = ref(0)
const trendData = ref([])

const trendCountChart = ref(null)
const trendSalaryChart = ref(null)
let chartInstances = []

const rateColor = computed(() => (appStore.dark ? '#35d98f' : '#00854f'))

function gapTag(gap) {
  if (gap >= 50) return 'danger'
  if (gap >= 20) return 'warning'
  return 'success'
}
function gapLabel(gap) {
  if (gap >= 50) return '严重缺口'
  if (gap >= 20) return '中等缺口'
  return '轻微缺口'
}
function priorityLabel(p) {
  return { HIGH: '高优先级', MEDIUM: '中优先级', LOW: '低优先级' }[p] || p
}
function priorityColor(p) {
  const dark = appStore.dark
  if (p === 'HIGH') return dark ? '#ff5c66' : '#de2532'
  if (p === 'MEDIUM') return dark ? '#e5a92b' : '#9b6800'
  return dark ? '#9d978e' : '#6f6e6c'
}

async function loadData() {
  loading.value = true
  try {
    const [suggestion, dashboard] = await Promise.all([
      getTeacherSuggestions(),
      getDashboard()
    ])
    skillGaps.value = suggestion.skillGaps || []
    courseSuggestions.value = suggestion.courseSuggestions || []
    studentsWithProfile.value = suggestion.studentsWithProfile || 0

    trendData.value = dashboard.trend || []
    await nextTick()
    buildTrendCharts()
  } finally {
    loading.value = false
  }
}

function buildTrendCharts() {
  disposeCharts()
  const list = trendData.value
  if (!list.length) return

  const theme = chartThemeName(appStore.dark)
  const grid = { left: '3%', right: '5%', bottom: '3%', top: 24, containLabel: true }
  const periods = list.map(i => i.period)

  if (trendCountChart.value) {
    const c = echarts.init(trendCountChart.value, theme)
    c.setOption({
      tooltip: { trigger: 'axis' },
      grid,
      xAxis: { type: 'category', data: periods, boundaryGap: false },
      yAxis: { type: 'value' },
      series: [{
        name: '职位数量', type: 'line', smooth: true,
        color: primarySeriesColor(appStore.dark),
        data: list.map(i => i.jobCount),
        areaStyle: { opacity: 0.09 }
      }]
    })
    chartInstances.push(c)
  }

  // 薪资单独一张图：与职位数量量纲不同，禁止塞进同一张图做双 Y 轴
  if (trendSalaryChart.value) {
    const c = echarts.init(trendSalaryChart.value, theme)
    c.setOption({
      tooltip: { trigger: 'axis', valueFormatter: v => `${v}k` },
      grid,
      xAxis: { type: 'category', data: periods, boundaryGap: false },
      yAxis: { type: 'value', axisLabel: { formatter: '{value}k' } },
      series: [{
        name: '平均薪资', type: 'line', smooth: true,
        color: moneySeriesColor(appStore.dark),
        data: list.map(i => +(i.avgSalary / 1000).toFixed(1)),
        areaStyle: { opacity: 0.09 }
      }]
    })
    chartInstances.push(c)
  }
}

function disposeCharts() {
  chartInstances.forEach(c => c.dispose())
  chartInstances = []
}

function handleResize() {
  chartInstances.forEach(c => c.resize())
}

// 深浅模式切换时重绘（ECharts 主题在 init 时固定，只能销毁重建）
watch(() => appStore.dark, () => buildTrendCharts())

onMounted(() => {
  loadData()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  disposeCharts()
})
</script>

<style scoped>
.sugg-title { font-size: 15px; font-weight: 600; color: var(--app-ink); margin: 0; }
.sugg-desc { color: var(--app-ink-3); font-size: 13px; margin: 4px 0 0; }
.trend-pair { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.trend-title { font-size: 13px; font-weight: 600; color: var(--app-ink-2); margin-bottom: 8px; }
.meter { display: flex; align-items: center; gap: 10px; }
.meter :deep(.el-progress) { flex: 1; }
.meter-num { font-size: 12px; color: var(--app-ink-3); font-variant-numeric: tabular-nums; width: 34px; text-align: right; }
.evidence { font-size: 12px; color: var(--app-ink-3); }

@media (max-width: 900px) {
  .trend-pair { grid-template-columns: 1fr; }
}
</style>
