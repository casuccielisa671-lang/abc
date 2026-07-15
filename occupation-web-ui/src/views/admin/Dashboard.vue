<template>
  <div class="dashboard">
    <div class="page-head with-actions" v-if="!embedded">
      <div>
        <h2 class="page-title">数据看板</h2>
        <p class="page-sub">数据每日凌晨 2:00 自动更新</p>
      </div>
      <div class="page-actions">
        <el-button type="primary" :loading="rebuilding" @click="handleRebuild">
          手动重算分析数据
        </el-button>
      </div>
    </div>

    <el-row :gutter="16">
      <!-- 行业分布 -->
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>行业分布 Top10</template>
          <div ref="industryChart" class="chart-box"></div>
        </el-card>
      </el-col>
      <!-- 城市分布 -->
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>城市热度分布</template>
          <div ref="cityChart" class="chart-box"></div>
        </el-card>
      </el-col>
    </el-row>
    <el-row :gutter="16" style="margin-top:16px">
      <!-- 技能热度 -->
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>技能热度 Top15</template>
          <div ref="skillChart" class="chart-box"></div>
        </el-card>
      </el-col>
      <!-- 学历分布 -->
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>学历要求分布</template>
          <div ref="eduChart" class="chart-box"></div>
        </el-card>
      </el-col>
    </el-row>
    <el-row :gutter="16" style="margin-top:16px">
      <!-- 趋势：数量与薪资量纲不同，拆成两张图并排 -->
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>职位数量趋势</template>
          <div ref="trendCountChart" class="chart-box"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>平均薪资趋势</template>
          <div ref="trendSalaryChart" class="chart-box"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import { getDashboard, rebuildAnalysis } from '@/api/admin'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { useAppStore } from '@/store/app'
import { chartThemeName, primarySeriesColor, moneySeriesColor } from '@/styles/chartTheme'

// embedded=true 时隐藏自身标题+重算按钮，供「数据分析」中心以标签页嵌入（重算由中心统一触发）
defineProps({ embedded: { type: Boolean, default: false } })

const appStore = useAppStore()
const loading = ref(false)
const rebuilding = ref(false)

// 图表 DOM 引用
const industryChart = ref(null)
const cityChart = ref(null)
const skillChart = ref(null)
const eduChart = ref(null)
const trendCountChart = ref(null)
const trendSalaryChart = ref(null)

let charts = []
let lastData = null

function disposeCharts() {
  charts.forEach(c => c.dispose())
  charts = []
}

function initChart(refEl, option) {
  if (!refEl.value) return
  const chart = echarts.init(refEl.value, chartThemeName(appStore.dark))
  chart.setOption(option)
  charts.push(chart)
  return chart
}

function buildIndustryChart(data) {
  const list = (data || []).slice(0, 10)
  initChart(industryChart, {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: '3%', right: '10%', bottom: '3%', containLabel: true },
    xAxis: { type: 'value' },
    yAxis: { type: 'category', data: list.map(i => i.name), inverse: true },
    series: [{
      type: 'bar',
      // 单系列：统一用槽位 1，不按数值给柱子上色（长度已经表达了大小）
      color: primarySeriesColor(appStore.dark),
      data: list.map(i => i.count || i.value),
      itemStyle: { borderRadius: [0, 4, 4, 0] }
    }]
  })
}

function buildCityChart(data) {
  // 前 6 个城市 + 其余合并为「其他」，避免饼图切片过碎
  const list = (data || []).slice(0, 10)
  const top = list.slice(0, 6).map(i => ({ name: i.name, value: i.count || i.value }))
  const rest = list.slice(6).reduce((sum, i) => sum + (i.count || i.value || 0), 0)
  if (rest > 0) top.push({ name: '其他', value: rest })
  initChart(cityChart, {
    tooltip: { trigger: 'item' },
    series: [{
      type: 'pie', radius: ['42%', '68%'], center: ['50%', '52%'],
      data: top,
      label: { formatter: '{b} {d}%' }
    }]
  })
}

function buildSkillChart(data) {
  const list = (data || []).slice(0, 15)
  initChart(skillChart, {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: '3%', right: '10%', bottom: '3%', containLabel: true },
    xAxis: { type: 'value' },
    yAxis: { type: 'category', data: list.map(i => i.name), inverse: true },
    series: [{
      type: 'bar',
      barMaxWidth: 14,
      color: primarySeriesColor(appStore.dark),
      data: list.map(i => i.count || i.value),
      itemStyle: { borderRadius: [0, 4, 4, 0] }
    }]
  })
}

function buildEduChart(data) {
  const list = data || []
  // 学历值由后端 normalizeEducation 归一化，只会是这五档（原来写的「大专」「高中及以下」永远匹配不上）
  const eduOrder = ['博士', '硕士', '本科', '专科', '不限']
  const rank = name => {
    const i = eduOrder.indexOf(name)
    return i === -1 ? eduOrder.length : i
  }
  const sorted = list
    .map(i => ({ name: i.name, value: i.count || i.value || 0 }))
    .filter(i => i.value > 0)
    .sort((a, b) => rank(a.name) - rank(b.name))

  initChart(eduChart, {
    tooltip: { trigger: 'item' },
    series: [{
      type: 'pie', radius: '62%', center: ['50%', '52%'],
      data: sorted,
      // 直接标签：饼图切片本身对比度不够时，标签是必需的二次编码
      label: { formatter: '{b} {c}个 ({d}%)' }
    }]
  })
}

function buildTrendCharts(data) {
  const list = data || []
  initChart(trendCountChart, {
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '5%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: list.map(i => i.period), boundaryGap: false },
    yAxis: { type: 'value' },
    series: [{
      name: '职位数量', type: 'line', smooth: true,
      color: primarySeriesColor(appStore.dark),
      data: list.map(i => i.jobCount),
      areaStyle: { opacity: 0.09 }
    }]
  })
  // 薪资与数量量纲不同，单独成图（禁止双 Y 轴）
  initChart(trendSalaryChart, {
    tooltip: { trigger: 'axis', valueFormatter: v => `${v}k` },
    grid: { left: '3%', right: '5%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: list.map(i => i.period), boundaryGap: false },
    yAxis: { type: 'value', axisLabel: { formatter: '{value}k' } },
    series: [{
      name: '平均薪资', type: 'line', smooth: true,
      color: moneySeriesColor(appStore.dark),
      data: list.map(i => +(i.avgSalary / 1000).toFixed(1)),
      areaStyle: { opacity: 0.09 }
    }]
  })
}

function renderAll() {
  if (!lastData) return
  disposeCharts()
  buildIndustryChart(lastData.industryTop)
  buildCityChart(lastData.cityDist)
  buildSkillChart(lastData.skillHot)
  buildEduChart(lastData.educationDist)
  buildTrendCharts(lastData.trend)
}

async function loadDashboard() {
  loading.value = true
  try {
    lastData = await getDashboard()
    await nextTick()
    renderAll()
  } catch {
    // 拦截器已提示
  } finally {
    loading.value = false
  }
}

async function handleRebuild() {
  rebuilding.value = true
  try {
    const count = await rebuildAnalysis()
    ElMessage.success(`重算完成，共更新 ${count} 条分析结果`)
    await loadDashboard()
  } catch {
    // 拦截器已提示
  } finally {
    rebuilding.value = false
  }
}

// 深浅模式切换时重绘图表（ECharts 主题在 init 时固定，只能销毁重建）
watch(() => appStore.dark, () => renderAll())

function handleResize() {
  charts.forEach(c => c.resize())
}

onMounted(() => {
  loadDashboard()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  disposeCharts()
})

// 供「数据分析」中心在点「重算」后刷新本标签
defineExpose({ reload: loadDashboard })
</script>

<style scoped>
.chart-card { margin-bottom: 0; }
</style>
