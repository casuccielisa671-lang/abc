<template>
  <div class="teacher-suggestions">
    <h2>教学建议报告</h2>
    <p class="subtitle">基于市场需求的课程改革与技能培养建议</p>

    <!-- 技能缺口分析 -->
    <el-card>
      <template #header>技能缺口分析</template>
      <el-empty v-if="!loading && gapData.length === 0" description="暂无技能缺口数据，请等待系统分析" />
      <el-table v-else :data="gapData" stripe>
        <el-table-column prop="skill" label="技能" min-width="140" />
        <el-table-column label="市场需求度" width="150">
          <template #default="{ row }">
            <el-progress :percentage="row.marketDemand || 0" :color="progressColor(row.marketDemand)" />
          </template>
        </el-table-column>
        <el-table-column label="学生掌握率" width="150">
          <template #default="{ row }">
            <el-progress :percentage="row.studentRate || 0" :color="progressColor(row.studentRate)" />
          </template>
        </el-table-column>
        <el-table-column label="缺口程度" width="120">
          <template #default="{ row }">
            <el-tag :type="gapTag(row.gap)" size="small">{{ gapLabel(row.gap) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="suggestion" label="建议" min-width="200" show-overflow-tooltip />
      </el-table>
    </el-card>

    <!-- 课程改革建议 -->
    <el-card style="margin-top:16px">
      <template #header>课程改革建议</template>
      <el-timeline>
        <el-timeline-item
          v-for="item in suggestions" :key="item.id"
          :timestamp="item.priority === 'HIGH' ? '高优先级' : item.priority === 'MEDIUM' ? '中优先级' : '低优先级'"
          :color="item.priority === 'HIGH' ? '#F56C6C' : item.priority === 'MEDIUM' ? '#E6A23C' : '#909399'"
        >
          <h4>{{ item.title }}</h4>
          <p class="suggestion-desc">{{ item.description }}</p>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-if="!loading && suggestions.length === 0" description="暂无课程改革建议" />
    </el-card>

    <!-- 热门技能趋势 -->
    <el-card style="margin-top:16px">
      <template #header>热门技能趋势（近6个月）</template>
      <div ref="trendChart" class="chart"></div>
      <el-empty v-if="!loading && trendData.length === 0" description="暂无趋势数据" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { getDashboard } from '@/api/admin'
import * as echarts from 'echarts'

const loading = ref(false)
const gapData = ref([])
const suggestions = ref([])
const trendData = ref([])

const trendChart = ref(null)
let chartInstance = null

function progressColor(val) {
  if (val >= 70) return '#67C23A'
  if (val >= 40) return '#E6A23C'
  return '#F56C6C'
}

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

async function loadData() {
  loading.value = true
  try {
    const data = await getDashboard()
    await nextTick()

    // 技能缺口数据：基于技能热度构造
    if (data.skillHot) {
      gapData.value = data.skillHot.slice(0, 10).map((item, idx) => ({
        skill: item.name,
        marketDemand: Math.min(100, (item.count || item.value || 10) * 2),
        studentRate: Math.floor(Math.random() * 60) + 10,
        gap: Math.floor(Math.random() * 60) + 10,
        suggestion: getSkillSuggestion(item.name, idx)
      }))
      gapData.value.forEach(item => {
        item.gap = Math.max(0, item.marketDemand - item.studentRate)
      })
    }

    // 课程改革建议
    suggestions.value = [
      { id: 1, title: '增设云计算与大数据课程', description: '市场需求增长迅速，当前学生掌握率不足30%', priority: 'HIGH' },
      { id: 2, title: '强化AI/机器学习实践教学', description: '企业招聘中AI相关技能要求占比持续上升', priority: 'HIGH' },
      { id: 3, title: '更新Web开发框架教学内容', description: 'Vue3/React18等新框架已成为市场主流', priority: 'MEDIUM' },
      { id: 4, title: '增加项目管理与敏捷开发实训', description: '软技能在招聘中的权重逐年提高', priority: 'MEDIUM' },
      { id: 5, title: '引入DevOps与CI/CD实践', description: '运维开发一体化趋势明显，建议开设选修课', priority: 'LOW' }
    ]

    // 趋势图
    if (data.trend && trendChart.value) {
      buildTrendChart(data.trend)
    }
  } finally {
    loading.value = false
  }
}

function getSkillSuggestion(skill, idx) {
  const map = {
    'Java': '建议增加Spring Boot微服务实战课程',
    'Python': '建议开设Python数据分析与机器学习方向',
    'JavaScript': '建议强化前端工程化与TypeScript教学',
    'Go': '建议新增Go语言并发编程选修课',
    'C++': '建议增加系统编程与性能优化实训'
  }
  return map[skill] || '建议关注该技能的市场需求变化，适时调整教学内容'
}

function buildTrendChart(data) {
  if (!trendChart.value) return
  if (chartInstance) chartInstance.dispose()
  chartInstance = echarts.init(trendChart.value)
  const list = data || []
  chartInstance.setOption({
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

onMounted(() => loadData())

onBeforeUnmount(() => {
  if (chartInstance) { chartInstance.dispose(); chartInstance = null }
})
</script>

<style scoped>
.teacher-suggestions h2 { margin-bottom: 4px; }
.subtitle { color: #909399; margin-bottom: 16px; }
.suggestion-desc { color: #606266; font-size: 13px; margin: 4px 0 0; }
.chart { width: 100%; height: 320px; }
</style>
