<template>
  <div class="map-explore">
    <div class="me-head">
      <div>
        <h2 class="page-title">全国{{ activeLayer.label }}分布 · 3D</h2>
        <p class="page-sub">{{ activeLayer.desc }} · 卫星热力图，可拖拽旋转、滚轮缩放</p>
      </div>
      <div class="me-controls">
        <!-- 4 图层：岗位数/平均薪资（全角色）+ 学生意向/投递去向（教师·管理员，按范围过滤） -->
        <el-radio-group v-model="metric" size="small" @change="onMetric">
          <el-radio-button v-for="l in layers" :key="l.key" :value="l.key">{{ l.label }}</el-radio-button>
        </el-radio-group>
      </div>
    </div>

    <div class="me-body">
      <div class="me-map-wrap">
        <!-- 队友的自研卫星 3D 热力图；切图层→heatPoints 变→内部 updateHeatPoints 重绘 -->
        <China3DMap :heat-points="heatPoints" />
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
import { ref, computed, onMounted } from 'vue'
import China3DMap from '@/components/visualization/China3DMap.vue'
import { useUserStore } from '@/store/user'
import { getCityDistribution } from '@/api/map'
import { getIntentCities, getApplicationCities } from '@/api/student'
import { toList } from '@/utils/list'

const userStore = useUserStore()
const loading = ref(false)
const metric = ref('jobCount')

// 数据源
const cities = ref([])            // 市场：jobCount / avgSalary
const intentData = ref(null)      // 学生意向（懒加载）
const applicationData = ref(null) // 投递去向（懒加载）

// 图层定义（intent/application 仅教师/管理员可见，后端按 TeacherScope 范围过滤）
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

// 喂给 China3DMap 的热力点；传原始 val，heatmapLayer 内部按各图层 max 自适应归一化
const heatPoints = computed(() =>
  points.value.map(p => ({
    cityName: p.name,
    longitude: p.coord[0],
    latitude: p.coord[1],
    gatherValue: p.val
  }))
)

async function ensureLayerData() {
  const l = activeLayer.value
  if (l.kind === 'intent' && intentData.value === null) {
    intentData.value = toList(await getIntentCities().catch(() => []))
  } else if (l.kind === 'application' && applicationData.value === null) {
    applicationData.value = toList(await getApplicationCities().catch(() => []))
  }
}

async function onMetric() {
  loading.value = true
  try { await ensureLayerData() } finally { loading.value = false }
}

onMounted(async () => {
  loading.value = true
  try { cities.value = toList(await getCityDistribution()) }
  catch { /* 拦截器已提示 */ }
  finally { loading.value = false }
})
</script>

<style scoped>
.map-explore { max-width: 1240px; margin: 0 auto; }
.me-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; margin-bottom: 14px; flex-wrap: wrap; }
.page-title { font-size: 22px; font-weight: 700; margin: 0 0 4px; }
.page-sub { color: var(--color-text-tertiary); font-size: 13.5px; margin: 0; }
.me-controls { display: flex; gap: 10px; flex-wrap: wrap; }
.me-body { display: grid; grid-template-columns: 1fr 280px; gap: 16px; }
.me-map-wrap { position: relative; height: 580px; border-radius: 14px; overflow: hidden;
  border: 1px solid var(--color-border); box-shadow: var(--shadow-md); }
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
@media (max-width: 900px) { .me-body { grid-template-columns: 1fr; } .me-map-wrap { height: 440px; } }
</style>
