<template>
  <div ref="scrollRootRef" class="home-scroll-root">
    <section ref="mapSectionRef" class="home-map-section">
  <div class="home-index" :class="{ 'home-index--immersive': !panelsVisible }">
    <!-- 左侧：搜索 + 推荐职业 -->
    <aside v-show="panelsVisible" class="home-left">
      <div class="search-row">
        <el-input
          v-model="searchKeyword"
          placeholder="输入职业名称搜索"
          clearable
          @keyup.enter="handleSearch"
        >
          <template #append>
            <el-button :icon="Search" @click="handleSearch" />
          </template>
        </el-input>
      </div>

      <div class="job-panel">
        <div class="panel-head">
          <span>推荐职业</span>
          <span class="panel-hint">点击切换地图热力</span>
        </div>
        <div v-loading="jobsLoading" class="job-scroll">
          <button
            v-for="job in recommendJobs"
            :key="job.jobName"
            type="button"
            class="job-card"
            :class="{ active: job.jobName === activeJob }"
            @click="selectJob(job.jobName)"
          >
            <span class="job-name">{{ job.jobName }}</span>
            <span class="job-count">{{ job.jobCount }} 个岗位</span>
          </button>
          <el-empty v-if="!jobsLoading && !recommendJobs.length" description="暂无职业数据" :image-size="60" />
        </div>
      </div>

      <div v-if="rolePanelVisible" class="role-panel">
        <div class="panel-head">{{ rolePanelTitle }}</div>
        <div v-if="userStore.role === 'TEACHER'" class="role-body" v-loading="teacherLoading">
          <p>本校投递总数：<strong>{{ teacherStats.total }}</strong></p>
          <p>HR 已查看：<strong>{{ teacherStats.viewed }}</strong></p>
          <p>进入录用：<strong>{{ teacherStats.offer }}</strong></p>
        </div>
        <div v-else-if="userStore.role === 'HR'" class="role-body">
          <p>查看各城市岗位聚集，了解人才竞争分布。</p>
          <p>当前职业：<strong>{{ activeJob || '—' }}</strong></p>
        </div>
        <div v-else-if="userStore.role === 'ADMIN'" class="role-body">
          <p>管理员可重算 Spark 分析任务，刷新职业聚集度数据源。</p>
          <el-button type="primary" size="small" :loading="rebuilding" @click="handleRebuild">
            重算职业聚集度
          </el-button>
        </div>
      </div>
    </aside>

    <!-- 右侧主体：地图占满，图表浮层右上角 -->
    <section class="home-main">
      <div class="map-stage">
        <China3DMap :heat-points="heatPoints" />
      </div>

      <aside v-show="panelsVisible" class="chart-float">
        <div class="chart-panel">
          <div class="panel-head">
            <span>{{ activeJob ? `${activeJob} · 城市热度` : '城市热度' }}</span>
          </div>
          <div ref="chartRef" class="chart-box" v-loading="heatLoading" />
        </div>
      </aside>
      <div class="home-fab-group">
        <button
          type="button"
          class="fab-btn fab-btn--scroll"
          title="向下翻页 · 就业资讯"
          @click="scrollToNews"
        >
          <el-icon :size="20"><ArrowDown /></el-icon>
        </button>
        <button
          type="button"
          class="fab-btn fab-btn--panel"
          :title="panelsVisible ? '隐藏面板，全屏地图' : '显示面板'"
          @click="togglePanels"
        >
          <el-icon :size="20">
            <component :is="panelsVisible ? FullScreen : Menu" />
          </el-icon>
        </button>
      </div>
    </section>
  </div>
    </section>

    <section ref="newsSectionRef" class="home-news-section">
      <JobNews :visible="newsVisible" @back-top="scrollToMap" />
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import * as echarts from 'echarts'
import { Search, FullScreen, Menu, ArrowDown } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import China3DMap from '@/components/visualization/China3DMap.vue'
import JobNews from './JobNews.vue'
import { getRecommendJobs, getJobCityHeat, rebuildJobGather } from '@/api/map'
import { getEmployment } from '@/api/admin'
import { useUserStore } from '@/store/user'
import { chartThemeName, primarySeriesColor } from '@/styles/chartTheme'

const userStore = useUserStore()

const searchKeyword = ref('')
const recommendJobs = ref([])
const activeJob = ref('')
const heatPoints = ref([])
const jobsLoading = ref(false)
const heatLoading = ref(false)
const rebuilding = ref(false)
const teacherLoading = ref(false)
const teacherStats = ref({ total: 0, viewed: 0, offer: 0 })

const chartRef = ref(null)
const scrollRootRef = ref(null)
const mapSectionRef = ref(null)
const newsSectionRef = ref(null)
const panelsVisible = ref(true)
const newsVisible = ref(false)
let chart = null
let newsObserver = null

const rolePanelVisible = computed(() =>
  ['TEACHER', 'HR', 'ADMIN'].includes(userStore.role)
)

const rolePanelTitle = computed(() => {
  const map = { TEACHER: '本校投递概况', HR: 'HR 视角', ADMIN: '数据管理' }
  return map[userStore.role] || ''
})

function renderChart(points) {
  if (!chartRef.value) return
  if (!chart) {
    chart = echarts.init(chartRef.value, chartThemeName(), { renderer: 'canvas' })
  }
  const sorted = [...points].sort((a, b) => Number(b.gatherValue) - Number(a.gatherValue)).slice(0, 12)
  chart.setOption({
    backgroundColor: 'transparent',
    grid: { left: 40, right: 8, top: 20, bottom: 48 },
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      data: sorted.map(p => p.cityName),
      axisLabel: { rotate: 30, interval: 0, fontSize: 10 }
    },
    yAxis: { type: 'value', name: '聚集度', nameTextStyle: { fontSize: 10 } },
    series: [{
      type: 'bar',
      data: sorted.map(p => Number(p.gatherValue)),
      itemStyle: { color: primarySeriesColor() },
      barMaxWidth: 22
    }]
  })
}

async function loadJobHeat(jobName) {
  if (!jobName) return
  heatLoading.value = true
  try {
    const data = await getJobCityHeat(jobName)
    heatPoints.value = data || []
    activeJob.value = jobName
    await nextTick()
    renderChart(heatPoints.value)
  } catch {
    heatPoints.value = []
  } finally {
    heatLoading.value = false
  }
}

function selectJob(jobName) {
  searchKeyword.value = jobName
  loadJobHeat(jobName)
}

function handleSearch() {
  const kw = searchKeyword.value.trim()
  if (!kw) {
    ElMessage.warning('请输入职业名称')
    return
  }
  loadJobHeat(kw)
}

async function loadRecommendJobs() {
  jobsLoading.value = true
  try {
    recommendJobs.value = await getRecommendJobs()
    if (recommendJobs.value.length && !activeJob.value) {
      await loadJobHeat(recommendJobs.value[0].jobName)
    }
  } finally {
    jobsLoading.value = false
  }
}

async function loadTeacherStats() {
  if (userStore.role !== 'TEACHER') return
  teacherLoading.value = true
  try {
    const data = await getEmployment()
    teacherStats.value = {
      total: data?.funnel?.total ?? 0,
      viewed: data?.funnel?.responded ?? 0,
      offer: data?.funnel?.offer ?? 0
    }
  } finally {
    teacherLoading.value = false
  }
}

async function handleRebuild() {
  rebuilding.value = true
  try {
    const count = await rebuildJobGather()
    ElMessage.success(`重算完成，更新 ${count} 条分析记录`)
    await loadRecommendJobs()
    if (activeJob.value) await loadJobHeat(activeJob.value)
  } finally {
    rebuilding.value = false
  }
}

function handleResize() {
  chart?.resize()
}

function togglePanels() {
  panelsVisible.value = !panelsVisible.value
  nextTick(() => {
    handleResize()
    window.dispatchEvent(new Event('resize'))
  })
}

function scrollToNews() {
  newsSectionRef.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function scrollToMap() {
  mapSectionRef.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function setupNewsObserver() {
  if (!newsSectionRef.value) return
  newsObserver = new IntersectionObserver(
    ([entry]) => {
      if (entry.isIntersecting) newsVisible.value = true
    },
    { root: scrollRootRef.value, threshold: 0.12 }
  )
  newsObserver.observe(newsSectionRef.value)
}

watch(activeJob, () => nextTick(() => chart?.resize()))

onMounted(() => {
  loadRecommendJobs()
  loadTeacherStats()
  window.addEventListener('resize', handleResize)
  nextTick(() => setupNewsObserver())
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  newsObserver?.disconnect()
  newsObserver = null
  chart?.dispose()
  chart = null
})
</script>

<style scoped>
.home-scroll-root {
  height: calc(100vh - 56px);
  overflow-y: auto;
  overflow-x: hidden;
  scroll-behavior: smooth;
  scrollbar-width: thin;
  scrollbar-color: rgba(180, 140, 80, 0.45) transparent;
}

.home-scroll-root::-webkit-scrollbar {
  width: 8px;
}

.home-scroll-root::-webkit-scrollbar-thumb {
  background: rgba(180, 140, 80, 0.45);
  border-radius: 4px;
}

.home-map-section {
  height: calc(100vh - 56px);
  min-height: calc(100vh - 56px);
}

.home-news-section {
  min-height: calc(100vh - 56px);
}

.home-index {
  display: grid;
  grid-template-columns: 260px minmax(0, 1fr);
  gap: 12px;
  height: calc(100vh - 56px);
  padding: 12px;
  background: linear-gradient(180deg, #F8FAFC 0%, #EFF6FF 50%, #F0F4FF 100%);
  overflow: hidden;
  transition: grid-template-columns 0.28s ease;
}

.home-index--immersive {
  grid-template-columns: minmax(0, 1fr);
  gap: 0;
  padding: 0;
}

.home-left {
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-height: 0;
  overflow: hidden;
  padding: 14px 12px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  border: 1px solid rgba(37, 99, 235, 0.1);
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.08);
}

.search-row :deep(.el-input-group__append) {
  padding: 0;
}

.search-row :deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.45);
  box-shadow: none;
}

.job-panel,
.role-panel {
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(4px);
  border-radius: 10px;
  box-shadow: 0 2px 8px rgba(37, 99, 235, 0.05);
  border: 1px solid rgba(37, 99, 235, 0.08);
}

.panel-head {
  padding: 10px 14px 6px;
  font-size: 13px;
  font-weight: 600;
  color: var(--app-ink);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.panel-hint {
  font-weight: 400;
  font-size: 11px;
  color: var(--app-ink-3);
}

.job-panel {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.job-scroll {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 0 10px 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  scrollbar-width: thin;
  scrollbar-color: rgba(37, 99, 235, 0.3) transparent;
}

.job-scroll::-webkit-scrollbar {
  width: 6px;
}

.job-scroll::-webkit-scrollbar-thumb {
  background: rgba(37, 99, 235, 0.3);
  border-radius: 3px;
}

.job-card {
  text-align: left;
  padding: 10px 12px;
  border-radius: 8px;
  border: 1px solid rgba(200, 180, 150, 0.45);
  background: rgba(255, 255, 255, 0.45);
  cursor: pointer;
  transition: background 0.15s, border-color 0.15s, transform 0.12s;
}

.job-card:hover {
  background: rgba(255, 255, 255, 0.72);
  transform: translateX(2px);
}

.job-card.active {
  background: linear-gradient(135deg, #2563EB 0%, #1D4ED8 100%);
  border-color: rgba(37, 99, 235, 0.4);
  color: #fff;
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.3);
}

.job-card.active .job-count {
  color: rgba(255, 255, 255, 0.82);
}

.job-name {
  display: block;
  font-size: 13px;
  font-weight: 600;
  line-height: 1.35;
}

.job-count {
  display: block;
  margin-top: 4px;
  font-size: 11px;
  color: var(--app-ink-3);
}

.role-panel {
  flex: none;
  max-height: 160px;
}

.role-body {
  padding: 0 14px 14px;
  font-size: 13px;
  color: var(--app-ink-2);
  line-height: 1.7;
}

.home-main {
  position: relative;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
  border-radius: var(--app-radius-card, 10px);
}

.home-index--immersive .home-main {
  border-radius: 0;
}

.home-fab-group {
  position: absolute;
  right: 18px;
  bottom: 72px;
  z-index: 20;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.fab-btn {
  width: 46px;
  height: 46px;
  border: 1px solid rgba(255, 255, 255, 0.65);
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(80, 55, 30, 0.88);
  background: linear-gradient(
    145deg,
    rgba(255, 255, 255, 0.72) 0%,
    rgba(255, 245, 230, 0.55) 100%
  );
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  box-shadow: 0 4px 18px rgba(100, 70, 40, 0.15);
  transition: transform 0.15s, box-shadow 0.15s, background 0.15s;
}

.fab-btn:hover {
  transform: scale(1.06);
  box-shadow: 0 6px 22px rgba(100, 70, 40, 0.22);
  background: linear-gradient(
    145deg,
    rgba(255, 255, 255, 0.88) 0%,
    rgba(255, 248, 235, 0.7) 100%
  );
}

.fab-btn--scroll:hover {
  color: #2563eb;
  border-color: rgba(59, 130, 246, 0.35);
}

.fab-btn:active {
  transform: scale(0.96);
}

.map-stage {
  position: absolute;
  inset: 0;
  border-radius: var(--app-radius-card, 10px);
  overflow: hidden;
}

.chart-float {
  position: absolute;
  top: 10px;
  right: 10px;
  width: min(300px, 38%);
  height: 220px;
  z-index: 5;
  pointer-events: auto;
}

.chart-panel {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: linear-gradient(
    165deg,
    rgba(255, 255, 255, 0.35) 0%,
    rgba(255, 250, 240, 0.22) 100%
  );
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  border-radius: 10px;
  border: 1px solid rgba(255, 255, 255, 0.4);
  box-shadow: 0 2px 16px rgba(100, 80, 50, 0.06);
}

.chart-box {
  flex: 1;
  min-height: 0;
  padding: 0 6px 8px;
}

@media (max-width: 960px) {
  .home-index {
    grid-template-columns: 1fr;
    grid-template-rows: auto 1fr;
    height: auto;
    min-height: calc(100vh - 56px);
  }

  .home-main {
    min-height: 480px;
  }

  .chart-float {
    width: min(280px, 85%);
    height: 200px;
  }
}
</style>
