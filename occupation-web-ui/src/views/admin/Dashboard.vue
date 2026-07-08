<template>
  <div class="dashboard">
    <h2>数据看板</h2>
    <el-row :gutter="16">
      <!-- 行业分布 -->
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>行业分布 Top10</template>
          <div ref="industryChart" class="chart"></div>
        </el-card>
      </el-col>
      <!-- 城市分布 -->
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>城市热度分布</template>
          <div ref="cityChart" class="chart"></div>
        </el-card>
      </el-col>
    </el-row>
    <el-row :gutter="16" style="margin-top:16px">
      <!-- 技能热度 -->
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>技能热度 Top15</template>
          <div ref="skillChart" class="chart"></div>
        </el-card>
      </el-col>
      <!-- 学历分布 -->
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>学历要求分布</template>
          <div ref="eduChart" class="chart"></div>
        </el-card>
      </el-col>
    </el-row>
    <el-row :gutter="16" style="margin-top:16px">
      <!-- 趋势 -->
      <el-col :span="24">
        <el-card class="chart-card">
          <template #header>职位数量 & 平均薪资趋势</template>
          <div ref="trendChart" class="chart chart-tall"></div>
        </el-card>
      </el-col>
    </el-row>
    <div class="toolbar">
      <el-button type="primary" :loading="rebuilding" @click="handleRebuild">
        手动重算分析数据
      </el-button>
      <span class="tip">数据每日凌晨 2:00 自动更新</span>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { getDashboard, rebuildAnalysis } from '@/api/admin'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'

const loading = ref(false)
const rebuilding = ref(false)

// 图表 DOM 引用
const industryChart = ref(null)
const cityChart = ref(null)
const skillChart = ref(null)
const eduChart = ref(null)
const trendChart = ref(null)

let charts = []

function initChart(refEl, option) {
  if (!refEl.value) return
  const chart = echarts.init(refEl.value)
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
    series: [{ type: 'bar', data: list.map(i => i.count || i.value), itemStyle: { color: '#409EFF' }, barMaxWidth: 30 }]
  })
}

function buildCityChart(data) {
  const list = (data || []).slice(0, 10)
  initChart(cityChart, {
    tooltip: { trigger: 'item' },
    series: [{
      type: 'pie', radius: ['40%', '70%'], center: ['50%', '55%'],
      data: list.map(i => ({ name: i.name, value: i.count || i.value })),
      label: { formatter: '{b}: {d}%' },
      emphasis: { itemStyle: { shadowBlur: 10, shadowOffsetX: 0, shadowColor: 'rgba(0,0,0,0.5)' } }
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
    series: [{ type: 'bar', data: list.map(i => i.count || i.value), itemStyle: { color: '#67C23A' }, barMaxWidth: 20 }]
  })
}

function buildEduChart(data) {
  const list = data || []
  const eduOrder = ['博士', '硕士', '本科', '大专', '高中及以下']
  const sorted = eduOrder.map(name => {
    const item = list.find(i => i.name === name)
    return item ? { name, value: item.count || item.value } : { name, value: 0 }
  }).filter(i => i.value > 0)

  initChart(eduChart, {
    tooltip: { trigger: 'item' },
    series: [{
      type: 'pie', radius: '65%', center: ['50%', '55%'],
      data: sorted,
      label: { formatter: '{b}: {c}个 ({d}%)' }
    }]
  })
}

function buildTrendChart(data) {
  const list = data || []
  initChart(trendChart, {
    tooltip: { trigger: 'axis' },
    legend: { data: ['职位数量', '平均薪资(千)'] },
    grid: { left: '3%', right: '5%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: list.map(i => i.period), boundaryGap: false },
    yAxis: [
      { type: 'value', name: '职位数量' },
      { type: 'value', name: '薪资(千)', axisLabel: { formatter: '{value}k' } }
    ],
    series: [
      { name: '职位数量', type: 'line', data: list.map(i => i.jobCount), smooth: true, itemStyle: { color: '#409EFF' } },
      { name: '平均薪资(千)', type: 'line', yAxisIndex: 1, data: list.map(i => (i.avgSalary / 1000).toFixed(1)), smooth: true, itemStyle: { color: '#E6A23C' } }
    ]
  })
}

async function loadDashboard() {
  loading.value = true
  try {
    const data = await getDashboard()
    await nextTick()
    buildIndustryChart(data.industryTop)
    buildCityChart(data.cityDist)
    buildSkillChart(data.skillHot)
    buildEduChart(data.educationDist)
    buildTrendChart(data.trend)
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

onMounted(() => {
  loadDashboard()
})

onBeforeUnmount(() => {
  charts.forEach(c => c.dispose())
  charts = []
})
</script>

<style scoped>
.dashboard h2 { margin-bottom: 16px; }
.chart-card { margin-bottom: 0; }
.chart { width: 100%; height: 320px; }
.chart-tall { height: 360px; }
.toolbar { margin-top: 20px; display: flex; align-items: center; gap: 12px; }
.tip { color: #909399; font-size: 13px; }
</style>
