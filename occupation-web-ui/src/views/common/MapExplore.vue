<template>
  <div class="map-explore">
    <div class="me-head">
      <div>
        <h2 class="page-title">全国{{ activeLayer.label }}分布 · 3D</h2>
        <p class="page-sub">{{ activeLayer.desc }} · 悬浮城市查看详情，可拖拽旋转</p>
      </div>
      <div class="me-controls">
        <el-radio-group v-model="metric" size="small" @change="onMetric">
          <el-radio-button v-for="l in layers" :key="l.key" :label="l.key">{{ l.label }}</el-radio-button>
        </el-radio-group>
        <el-radio-group v-model="mode" size="small" @change="render">
          <el-radio-button label="bar">柱状</el-radio-button>
          <el-radio-button label="scatter">光点</el-radio-button>
        </el-radio-group>
      </div>
    </div>

    <div class="me-body">
      <div class="me-map-wrap">
        <div ref="chartRef" class="me-map" v-loading="loading" element-loading-background="rgba(12,21,38,.6)"></div>
        <div v-if="error" class="me-error">{{ error }}</div>
      </div>

      <aside class="me-side">
        <div class="side-h">城市排行 · {{ activeLayer.label }}</div>
        <div class="rank-list">
          <div v-for="(p, i) in ranking" :key="p.name" class="rank">
            <span class="rk" :class="{ top: i < 3 }">{{ i + 1 }}</span>
            <span class="rk-city">{{ p.name }}</span>
            <span class="rk-bar"><span class="rk-fill" :style="{ width: pct(p) + '%' }"></span></span>
            <span class="rk-val">{{ fmtVal(p.val) }}</span>
          </div>
          <el-empty v-if="!loading && !ranking.length" description="该图层暂无数据" :image-size="50" />
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as echarts from 'echarts'
import 'echarts-gl'
import { useUserStore } from '@/store/user'
import { getCityDistribution, } from '@/api/map'
import { getIntentCities, getApplicationCities } from '@/api/student'
import { toList } from '@/utils/list'

const CHINA_GEOJSON_URL = 'https://geo.datav.aliyun.com/areas_v3/bound/100000_full.json'
const HEAT_COLORS = ['#1B4DB3', '#2E86FF', '#28C0D6', '#8BE04E', '#F7C948', '#F97316']

const userStore = useUserStore()
const chartRef = ref(null)
const loading = ref(false)
const error = ref('')
const metric = ref('jobCount')
const mode = ref('bar')
let chart = null

// 数据源
const cities = ref([])            // 市场：jobCount / avgSalary
const intentData = ref(null)      // 学生意向（懒加载）
const applicationData = ref(null) // 投递去向（懒加载）

// 图层定义（intent/application 仅教师/管理员可见）
const ALL_LAYERS = [
  { key: 'jobCount', label: '岗位数', unit: '', desc: '各城市站内岗位数量', kind: 'market' },
  { key: 'avgSalary', label: '平均薪资', unit: '元/月', desc: '各城市岗位平均薪资', kind: 'market' },
  { key: 'intent', label: '学生意向', unit: '人', desc: '范围内学生的求职意向城市', kind: 'intent', roles: ['TEACHER', 'ADMIN'] },
  { key: 'application', label: '投递去向', unit: '人', desc: '范围内学生的实际投递城市', kind: 'application', roles: ['TEACHER', 'ADMIN'] }
]
const layers = computed(() => ALL_LAYERS.filter(l => !l.roles || l.roles.includes(userStore.role)))
const activeLayer = computed(() => ALL_LAYERS.find(l => l.key === metric.value) || ALL_LAYERS[0])

// 当前图层的点集：[{ name, coord:[lng,lat], val, raw }]
const points = computed(() => {
  const l = activeLayer.value
  if (l.kind === 'market') {
    return cities.value.map(c => ({ name: c.cityName, coord: [c.longitude, c.latitude], val: c[l.key] || 0, raw: c }))
  }
  const arr = l.kind === 'intent' ? intentData.value : applicationData.value
  return (arr || []).map(c => ({ name: c.cityName, coord: [c.longitude, c.latitude], val: c.count || 0, raw: c }))
})
const ranking = computed(() => [...points.value].sort((a, b) => b.val - a.val).slice(0, 12))
const maxVal = computed(() => Math.max(1, ...points.value.map(p => p.val)))
function pct(p) { return Math.round((p.val / maxVal.value) * 100) }
function fmtVal(v) { return metric.value === 'avgSalary' ? (v / 1000).toFixed(1) + 'k' : v }

async function ensureMap() {
  if (echarts.getMap('china')) return
  const geo = await fetch(CHINA_GEOJSON_URL).then(r => { if (!r.ok) throw new Error('地图底图加载失败'); return r.json() })
  echarts.registerMap('china', geo)
}

function tipHtml(p) {
  const c = p.raw
  const l = activeLayer.value
  if (l.kind === 'market') {
    return `<b style="font-size:14px">${p.name}</b><br/>岗位数：<b>${c.jobCount}</b><br/>平均薪资：<b>${c.avgSalary ? c.avgSalary + ' 元/月' : '—'}</b>`
  }
  const unit = l.kind === 'intent' ? '人' : '次'
  return `<b style="font-size:14px">${p.name}</b><br/>${l.label}：<b>${c.count} ${unit}</b>`
}

function buildOption() {
  const max = maxVal.value
  const data = points.value.map(p => ({ name: p.name, value: [p.coord[0], p.coord[1], p.val], p }))
  const labelStyle = {
    show: true, distance: 2,
    formatter: (o) => (o.data.p.val >= max * 0.28 ? o.name : ''),
    textStyle: { color: '#eef4ff', fontSize: 11.5, fontWeight: 600, backgroundColor: 'rgba(8,16,34,.7)', padding: [2, 5], borderRadius: 4 }
  }
  const emphasisLabel = { show: true, textStyle: { color: '#fff', fontSize: 13, backgroundColor: 'rgba(8,16,34,.85)', padding: [3, 6], borderRadius: 4 } }

  const series = mode.value === 'scatter'
    ? [{ type: 'scatter3D', coordinateSystem: 'geo3D', symbolSize: (v) => 10 + (v[2] / max) * 42, itemStyle: { opacity: 0.62 }, data, label: labelStyle, emphasis: { label: emphasisLabel } }]
    : [{ type: 'bar3D', coordinateSystem: 'geo3D', barSize: 5, minHeight: 0.5, bevelSize: 0.2, shading: 'lambert', data, itemStyle: { opacity: 0.95 }, label: labelStyle, emphasis: { label: emphasisLabel } }]

  return {
    backgroundColor: 'transparent',
    tooltip: {
      backgroundColor: 'rgba(16,28,52,.95)', borderColor: '#4d86e6', borderWidth: 1, textStyle: { color: '#eaf1ff', fontSize: 13 },
      formatter: (o) => (o.data && o.data.p ? tipHtml(o.data.p) : '')
    },
    visualMap: {
      show: true, calculable: true, min: 0, max, dimension: 2,
      inRange: { color: HEAT_COLORS }, textStyle: { color: '#c3d3ee' }, left: 16, bottom: 16, itemWidth: 12, itemHeight: 110
    },
    geo3D: {
      map: 'china', roam: true, boxWidth: 100, regionHeight: 1.2, shading: 'lambert',
      itemStyle: { color: '#12305e', borderWidth: 0.8, borderColor: '#4d86e6', opacity: 0.95 },
      emphasis: { itemStyle: { color: '#1f4f96' } }, label: { show: false },
      light: { main: { intensity: 1.1, shadow: false }, ambient: { intensity: 0.5 } },
      viewControl: { autoRotate: true, autoRotateSpeed: 7, autoRotateAfterStill: 3, distance: 128, alpha: 45, beta: 0, minDistance: 60, maxDistance: 220 },
      postEffect: { enable: true, bloom: { enable: true, bloomIntensity: 0.1 } }
    },
    series
  }
}

async function ensureLayerData() {
  const l = activeLayer.value
  if (l.kind === 'intent' && intentData.value === null) {
    intentData.value = toList(await getIntentCities().catch(() => []))
  } else if (l.kind === 'application' && applicationData.value === null) {
    applicationData.value = toList(await getApplicationCities().catch(() => []))
  }
}

async function render() {
  if (!chartRef.value) return
  try { await ensureMap(); error.value = '' }
  catch (e) { error.value = e.message || '地图底图加载失败（请检查网络）'; return }
  if (!chart) chart = echarts.init(chartRef.value)
  chart.setOption(buildOption(), true)
}

async function onMetric() {
  loading.value = true
  try { await ensureLayerData() } finally { loading.value = false }
  render()
}

function onResize() { chart && chart.resize() }

onMounted(async () => {
  loading.value = true
  try { cities.value = toList(await getCityDistribution()) } catch { /* 拦截器已提示 */ } finally { loading.value = false }
  await nextTick()
  render()
  window.addEventListener('resize', onResize)
})
onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  if (chart) { chart.dispose(); chart = null }
})
</script>

<style scoped>
.map-explore { max-width: 1240px; margin: 0 auto; }
.me-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; margin-bottom: 14px; flex-wrap: wrap; }
.page-title { font-size: 22px; font-weight: 700; margin: 0 0 4px; }
.page-sub { color: var(--color-text-tertiary); font-size: 13.5px; margin: 0; }
.me-controls { display: flex; gap: 10px; flex-wrap: wrap; }
.me-body { display: grid; grid-template-columns: 1fr 280px; gap: 16px; }
.me-map-wrap { position: relative; border-radius: 14px; overflow: hidden;
  background: radial-gradient(720px 360px at 62% 20%, rgba(43,107,251,.22), transparent 62%), linear-gradient(160deg, #0c1d3c, #0a1730 60%, #081124);
  border: 1px solid var(--color-border); box-shadow: var(--shadow-md); }
.me-map { width: 100%; height: 580px; }
.me-error { position: absolute; inset: 0; display: grid; place-items: center; color: #ffb4b4; font-size: 14px; padding: 20px; text-align: center; }
.me-side { background: var(--color-surface); border: 1px solid var(--color-border); border-radius: 14px; padding: 16px; box-shadow: var(--shadow-sm); }
.side-h { font-size: 14px; font-weight: 650; margin-bottom: 14px; }
.rank-list { display: flex; flex-direction: column; gap: 10px; }
.rank { display: grid; grid-template-columns: 22px 56px 1fr 44px; align-items: center; gap: 8px; }
.rk { width: 22px; height: 22px; border-radius: 6px; display: grid; place-items: center; font-size: 12px; font-weight: 700; background: var(--color-bg-secondary); color: var(--color-text-tertiary); }
.rk.top { background: var(--color-primary); color: #fff; }
.rk-city { font-size: 13px; font-weight: 600; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.rk-bar { height: 6px; border-radius: 4px; background: var(--color-bg-tertiary); overflow: hidden; }
.rk-fill { display: block; height: 100%; border-radius: 4px; background: linear-gradient(90deg, #2563EB, #22D3EE); }
.rk-val { font-size: 12px; font-weight: 650; text-align: right; font-variant-numeric: tabular-nums; color: var(--color-text-secondary); }
@media (max-width: 900px) { .me-body { grid-template-columns: 1fr; } .me-map { height: 440px; } }
</style>
