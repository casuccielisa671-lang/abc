<template>
  <div class="employment-page">
    <div class="page-head with-actions" v-if="!embedded">
      <div>
        <h2 class="page-title">就业分析</h2>
        <p class="page-sub">
          看板讲「市场有什么岗位」，这里讲「本校学生怎么样」。数据需先执行「重算分析数据」
        </p>
      </div>
      <div class="page-actions">
        <el-button :loading="rebuilding" @click="handleRebuild">重算分析数据</el-button>
      </div>
    </div>

    <div v-loading="loading">
      <!-- ============ 投递转化 ============ -->
      <el-card>
        <template #header>
          <div class="card-head">
            <span>投递转化</span>
            <span class="head-note">
              只统计站内投递（{{ funnel.total }} 条）。学生投给外部岗位的记录没有招聘方处理，不计入
            </span>
          </div>
        </template>

        <el-empty v-if="!funnel.total" description="还没有站内投递记录" :image-size="70" />
        <template v-else>
          <div class="stat-grid">
            <div class="stat-card">
              <div class="stat-label">收到投递</div>
              <div class="stat-value">{{ funnel.total }}</div>
            </div>
            <div class="stat-card">
              <div class="stat-label">被 HR 查看</div>
              <div class="stat-value">{{ funnel.viewRate }}<span class="unit">%</span></div>
              <div class="stat-hint">{{ funnel.responded }} 条已处理</div>
            </div>
            <div class="stat-card">
              <div class="stat-label">进入面试</div>
              <div class="stat-value">{{ funnel.interviewRate }}<span class="unit">%</span></div>
            </div>
            <div class="stat-card">
              <div class="stat-label">最终录用</div>
              <div class="stat-value">{{ funnel.offerRate }}<span class="unit">%</span></div>
            </div>
          </div>

          <div class="funnel-bar">
            <div v-for="s in funnelStages" :key="s.key" class="stage">
              <div class="stage-head">
                <span class="stage-name">{{ s.label }}</span>
                <span class="stage-num">{{ s.count }}</span>
              </div>
              <div class="stage-track">
                <div class="stage-fill" :class="s.key" :style="{ width: pct(s.count) }"></div>
              </div>
            </div>
          </div>

          <p class="note">
            <template v-if="funnel.unresponded">
              有 <strong>{{ funnel.unresponded }}</strong> 条投递还没有任何 HR 查看过。
            </template>
            <template v-if="Number(funnel.medianResponseHours) > 0">
              已处理的投递，HR 平均在 <strong>{{ roundHours(funnel.medianResponseHours) }}</strong> 后给出第一次反馈。
            </template>
          </p>
        </template>
      </el-card>

      <!-- ============ 供需错配 ============ -->
      <el-card class="section">
        <template #header>
          <div class="card-head">
            <span>城市供需错配</span>
            <span class="head-note">错配比 = 想去的学生占比 ÷ 岗位供给占比。大于 1 表示学生扎堆</span>
          </div>
        </template>
        <el-empty v-if="!cityGap.length" description="暂无数据，请先让学生填写意向城市" :image-size="70" />
        <template v-else>
          <div ref="gapChart" class="chart-box tall"></div>
          <el-table :data="cityGap" stripe size="small" class="gap-table">
            <el-table-column prop="city" label="城市" width="90" />
            <el-table-column label="想去的学生占比" width="140">
              <template #default="{ row }">{{ row.studentRatio }}%</template>
            </el-table-column>
            <el-table-column label="岗位供给占比" width="130">
              <template #default="{ row }">{{ row.jobRatio }}%</template>
            </el-table-column>
            <el-table-column label="错配比" width="110">
              <template #default="{ row }">
                <el-tag :type="gapTag(row.gapRatio)" size="small">{{ row.gapRatio }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="解读" min-width="220">
              <template #default="{ row }">{{ gapText(row) }}</template>
            </el-table-column>
          </el-table>
        </template>
      </el-card>

      <!-- ============ 薪资期望偏差 ============ -->
      <el-card class="section">
        <template #header>薪资期望偏差</template>
        <div class="salary-gap">
          <div class="stat-card">
            <div class="stat-label">学生期望中位数</div>
            <div class="stat-value salary-text">{{ money(salaryGap.studentMedian) }}</div>
          </div>
          <div class="stat-card">
            <div class="stat-label">市场薪资中位数</div>
            <div class="stat-value salary-text">{{ money(salaryGap.marketMedian) }}</div>
          </div>
          <div class="stat-card">
            <div class="stat-label">偏差</div>
            <div class="stat-value" :class="devClass">
              {{ Number(salaryGap.deviationPercent) > 0 ? '+' : '' }}{{ salaryGap.deviationPercent }}<span class="unit">%</span>
            </div>
            <div class="stat-hint">{{ devText }}</div>
          </div>
        </div>
        <div ref="salaryChart" class="chart-box"></div>
      </el-card>

      <!-- ============ 自主求职流向 ============ -->
      <el-card class="section">
        <template #header>
          <div class="card-head">
            <span>自主求职流向</span>
            <span class="head-note">学生对采集来的外部岗位表达的求职意向，反映他们真实的求职方向</span>
          </div>
        </template>
        <el-empty v-if="!contactCity.length" description="还没有学生使用「自主联系」" :image-size="70" />
        <div v-else class="pair">
          <div>
            <div class="pair-title">按城市</div>
            <div ref="contactCityChart" class="chart-box"></div>
          </div>
          <div>
            <div class="pair-title">按行业</div>
            <div ref="contactIndustryChart" class="chart-box"></div>
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import * as echarts from 'echarts'
import { getEmployment, rebuildAnalysis } from '@/api/admin'
import { useAppStore } from '@/store/app'
import { chartThemeName, primarySeriesColor, moneySeriesColor, CHART_COLORS } from '@/styles/chartTheme'
import { ElMessage } from 'element-plus'

// embedded=true 时隐藏自身标题+重算按钮，供「数据分析」中心以标签页嵌入（重算由中心统一触发）
defineProps({ embedded: { type: Boolean, default: false } })

const appStore = useAppStore()
const loading = ref(false)
const rebuilding = ref(false)

const funnel = ref({ total: 0 })
const cityGap = ref([])
const salaryGap = ref({ studentMedian: 0, marketMedian: 0, deviationPercent: 0 })
const studentSalary = ref([])
const contactCity = ref([])
const contactIndustry = ref([])

const gapChart = ref(null)
const salaryChart = ref(null)
const contactCityChart = ref(null)
const contactIndustryChart = ref(null)
let charts = []

const funnelStages = computed(() => [
  { key: 'submitted', label: '已投递（待查看）', count: funnel.value.submitted },
  { key: 'viewed', label: '已查看', count: funnel.value.viewed },
  { key: 'interview', label: '邀请面试', count: funnel.value.interview },
  { key: 'offer', label: '已录用', count: funnel.value.offer },
  { key: 'rejected', label: '不合适', count: funnel.value.rejected }
])

const devClass = computed(() =>
  Number(salaryGap.value.deviationPercent) > 0 ? 'over' : 'under'
)
const devText = computed(() => {
  const d = Number(salaryGap.value.deviationPercent)
  if (Math.abs(d) < 5) return '期望与市场基本吻合'
  return d > 0 ? '学生期望高于市场，需引导' : '学生期望低于市场，可鼓励议价'
})

function pct(n) {
  return funnel.value.total ? `${(n / funnel.value.total) * 100}%` : '0%'
}
function roundHours(h) {
  const n = Number(h)
  return n >= 24 ? `${(n / 24).toFixed(1)} 天` : `${n.toFixed(0)} 小时`
}
function money(v) {
  return `${Math.round(Number(v)).toLocaleString()} 元`
}
function gapTag(r) {
  const n = Number(r)
  if (n >= 1.5) return 'danger'
  if (n > 1.05) return 'warning'
  return 'success'
}
function gapText(row) {
  const n = Number(row.gapRatio)
  if (n >= 999) return '本平台几乎没有该城市的岗位'
  if (n >= 1.5) return '学生严重扎堆，岗位供给远跟不上意向'
  if (n > 1.05) return '想去的人略多于岗位'
  if (n < 0.8) return '岗位充足，学生关注度偏低'
  return '供需基本平衡'
}

async function load() {
  loading.value = true
  try {
    const d = await getEmployment()
    funnel.value = d.funnel || { total: 0 }
    cityGap.value = d.cityGap || []
    salaryGap.value = d.salaryGap || { studentMedian: 0, marketMedian: 0, deviationPercent: 0 }
    studentSalary.value = d.studentSalary || []
    contactCity.value = d.contactCity || []
    contactIndustry.value = d.contactIndustry || []
    await nextTick()
    buildCharts()
  } finally {
    loading.value = false
  }
}

async function handleRebuild() {
  rebuilding.value = true
  try {
    await rebuildAnalysis()
    ElMessage.success('已重算')
    await load()
  } catch {
    /* 拦截器已提示 */
  } finally {
    rebuilding.value = false
  }
}

function buildCharts() {
  disposeCharts()
  const dark = appStore.dark
  const theme = chartThemeName(dark)
  const palette = dark ? CHART_COLORS.dark : CHART_COLORS.light
  const grid = { left: '3%', right: '4%', bottom: '3%', top: 40, containLabel: true }

  // 城市错配：两条系列都是百分比，同量纲，可以同图 —— 不违反「禁止双 Y 轴」
  if (gapChart.value && cityGap.value.length) {
    const c = echarts.init(gapChart.value, theme)
    c.setOption({
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, valueFormatter: v => `${v}%` },
      legend: { top: 4 },
      grid,
      xAxis: { type: 'category', data: cityGap.value.map(g => g.city) },
      yAxis: { type: 'value', axisLabel: { formatter: '{value}%' } },
      series: [
        { name: '想去的学生占比', type: 'bar', color: palette[0], data: cityGap.value.map(g => +g.studentRatio) },
        { name: '岗位供给占比', type: 'bar', color: palette[1], data: cityGap.value.map(g => +g.jobRatio) }
      ]
    })
    charts.push(c)
  }

  // 期望薪资分桶：单系列，用槽位 1
  if (salaryChart.value && studentSalary.value.length) {
    const c = echarts.init(salaryChart.value, theme)
    c.setOption({
      title: { text: '学生期望薪资分布', left: 'center', top: 0, textStyle: { fontSize: 13 } },
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      grid,
      xAxis: { type: 'category', data: studentSalary.value.map(x => x.name) },
      yAxis: { type: 'value', minInterval: 1 },
      series: [{ name: '人数', type: 'bar', color: primarySeriesColor(dark), data: studentSalary.value.map(x => x.count) }]
    })
    charts.push(c)
  }

  buildPie(contactCityChart.value, contactCity.value, theme, palette)
  buildPie(contactIndustryChart.value, contactIndustry.value, theme, palette)
}

function buildPie(el, data, theme, palette) {
  if (!el || !data.length) return
  const c = echarts.init(el, theme)
  c.setOption({
    tooltip: { trigger: 'item' },
    color: palette,
    series: [{
      type: 'pie',
      radius: ['42%', '68%'],
      itemStyle: { borderRadius: 4, borderWidth: 2 },
      label: { formatter: '{b}\n{c}' },
      data: data.map(x => ({ name: x.name, value: x.count }))
    }]
  })
  charts.push(c)
}

function disposeCharts() {
  charts.forEach(c => c.dispose())
  charts = []
}
function handleResize() {
  charts.forEach(c => c.resize())
}

// 深浅模式切换时重绘：ECharts 主题在 init 时固定，只能销毁重建
watch(() => appStore.dark, () => buildCharts())

onMounted(() => {
  load()
  window.addEventListener('resize', handleResize)
})
onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  disposeCharts()
})

// 供「数据分析」中心在点「重算」后刷新本标签
defineExpose({ reload: load })
</script>

<style scoped>
.section { margin-top: 16px; }
.card-head { display: flex; justify-content: space-between; align-items: baseline; gap: 12px; flex-wrap: wrap; }
.head-note { font-size: 12px; color: var(--app-ink-3); font-weight: 400; }
.unit { font-size: 14px; margin-left: 2px; color: var(--app-ink-3); }

.chart-box { height: 280px; }
.chart-box.tall { height: 320px; }

.funnel-bar { margin-top: 20px; display: flex; flex-direction: column; gap: 12px; }
.stage-head { display: flex; justify-content: space-between; font-size: 12px; margin-bottom: 4px; }
.stage-name { color: var(--app-ink-2); }
.stage-num { color: var(--app-ink-3); font-variant-numeric: tabular-nums; }
.stage-track { height: 8px; border-radius: 4px; background: var(--app-stone); overflow: hidden; }
.stage-fill { height: 100%; border-radius: 4px; transition: width 0.3s; }
.stage-fill.submitted { background: var(--app-ink-3); }
.stage-fill.viewed { background: var(--app-ember); }
.stage-fill.interview { background: var(--app-link); }
.stage-fill.offer { background: var(--app-money); }
.stage-fill.rejected { background: var(--app-danger); }

.note { margin: 16px 0 0; font-size: 13px; color: var(--app-ink-3); line-height: 1.8; }
.gap-table { margin-top: 12px; }

.salary-gap { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; margin-bottom: 8px; }
.over { color: var(--app-ember); }
.under { color: var(--app-money); }

.pair { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.pair-title { font-size: 13px; font-weight: 600; color: var(--app-ink-2); margin-bottom: 8px; }

@media (max-width: 900px) {
  .pair, .salary-gap { grid-template-columns: 1fr; }
}
</style>
