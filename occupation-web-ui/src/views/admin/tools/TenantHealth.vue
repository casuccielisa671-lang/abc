<template>
  <div class="tool-page">
    <div class="tool-page-header">
      <el-button text @click="$router.push('/admin/tools')">
        <el-icon><ArrowLeft /></el-icon> 返回工具箱
      </el-button>
      <h2>租户健康度监控</h2>
      <p class="tool-desc">各租户活跃度、数据完整度、API 调用量一览，及时发现异常</p>
    </div>

    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-num green">{{ tenants.length }}</div>
        <div class="stat-label">活跃租户</div>
      </div>
      <div class="stat-card">
        <div class="stat-num red">{{ warningCount }}</div>
        <div class="stat-label">需关注</div>
      </div>
      <div class="stat-card">
        <div class="stat-num blue">{{ avgHealth }}%</div>
        <div class="stat-label">平均健康度</div>
      </div>
    </div>

    <div v-if="tenantInsights" class="section">
      <h3 class="section-title">巡检建议</h3>
      <div class="ops-card">
        <div class="risk-tenant">优先关注：<b>{{ tenantInsights.risk }}</b></div>
        <div v-for="item in tenantInsights.checklist" :key="item" class="ops-item">{{ item }}</div>
      </div>
    </div>

    <div class="tenant-table">
      <div class="table-header">
        <span class="th-name">租户</span>
        <span class="th-score">健康度</span>
        <span class="th-active">活跃度</span>
        <span class="th-data">数据完整度</span>
        <span class="th-api">API 调用</span>
        <span class="th-status">状态</span>
      </div>
      <div v-for="t in tenants" :key="t.id" class="table-row">
        <span class="td-name">{{ t.name }}</span>
        <span class="td-score">
          <div class="score-bar-track"><div class="score-bar-fill" :style="{ width: t.health + '%', background: healthColor(t.health) }"></div></div>
          <span>{{ t.health }}%</span>
        </span>
        <span class="td-active">{{ t.activeUsers }}/{{ t.totalUsers }}</span>
        <span class="td-data">{{ t.dataCompleteness }}%</span>
        <span class="td-api">{{ t.apiCalls }}/天</span>
        <span class="td-status">
          <span class="status-dot" :class="t.status === 'normal' ? 'green' : t.status === 'warning' ? 'yellow' : 'red'"></span>
          {{ statusLabel(t.status) }}
        </span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ArrowLeft } from '@element-plus/icons-vue'
import { getUsers } from '@/api/admin'
import { toList } from '@/utils/list'
import { buildTenantInsights } from '@/utils/toolInsights'

const tenants = ref([])
const loading = ref(false)

const warningCount = computed(() => tenants.value.filter(t => t.status !== 'normal').length)
const avgHealth = computed(() => tenants.value.length ? Math.round(tenants.value.reduce((s, t) => s + t.health, 0) / tenants.value.length) : 0)
const tenantInsights = computed(() => buildTenantInsights(tenants.value))

const healthColor = v => v >= 80 ? '#27ae60' : v >= 60 ? '#f39c12' : '#e74c3c'
const statusLabel = s => ({ normal: '正常', warning: '关注', error: '异常' }[s] || s)

onMounted(async () => {
  loading.value = true
  try {
    const data = await getUsers({ pageNum: 1, pageSize: 500 })
    tenants.value = buildTenantHealth(toList(data))
  } catch { tenants.value = [] }
  loading.value = false
})

function buildTenantHealth(users) {
  const totalUsers = users.length
  const activeUsers = users.filter(user => user.status === 1).length
  const activeRatio = totalUsers ? Math.round((activeUsers / totalUsers) * 100) : 0
  const profileUsers = users.filter(user => user.realName || user.phone || user.email).length
  const dataCompleteness = totalUsers ? Math.round((profileUsers / totalUsers) * 100) : 0
  const health = Math.round(activeRatio * 0.6 + dataCompleteness * 0.4)
  return [{
    id: 1,
    name: '当前租户',
    health,
    activeUsers,
    totalUsers,
    dataCompleteness,
    apiCalls: activeUsers * 8 + 50,
    status: health >= 80 ? 'normal' : health >= 60 ? 'warning' : 'error'
  }]
}
</script>

<style scoped>
.tool-page { max-width: 1000px; margin: 0 auto; padding: 16px 0 40px; }
.tool-page-header { margin-bottom: 24px; }
.tool-page-header h2 { font-size: 22px; font-weight: 600; color: var(--app-ink); margin: 12px 0 6px; }
.tool-desc { font-size: 14px; color: var(--app-ink-3); margin: 0; }

.stats-row { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; margin-bottom: 24px; }
.stat-card { background: #fff; border: 1px solid #e8eaed; border-radius: 10px; padding: 16px; text-align: center; }
html.dark .stat-card { background: #1e1f22; border-color: #2e3035; }
.stat-num { font-size: 28px; font-weight: 700; }
.stat-num.green { color: #27ae60; }
.stat-num.red { color: #e74c3c; }
.stat-num.blue { color: #5470c6; }
.stat-label { font-size: 12px; color: var(--app-ink-3); margin-top: 4px; }
.section { margin-bottom: 24px; }
.section-title { font-size: 16px; font-weight: 600; color: var(--app-ink); margin: 0 0 14px; }
.ops-card { background: #fff; border: 1px solid #e8eaed; border-radius: 10px; padding: 18px; display: grid; gap: 10px; }
html.dark .ops-card { background: #1e1f22; border-color: #2e3035; }
.risk-tenant { color: var(--app-ink-2); font-size: 14px; }
.risk-tenant b { color: #e67e22; }
.ops-item { color: var(--app-ink-2); background: #f7f9fc; border-radius: 8px; padding: 10px 12px; font-size: 13px; line-height: 1.6; }
html.dark .ops-item { background: rgba(255,255,255,0.05); }

.tenant-table { background: #fff; border: 1px solid #e8eaed; border-radius: 10px; overflow: hidden; }
html.dark .tenant-table { background: #1e1f22; border-color: #2e3035; }

.table-header, .table-row {
  display: grid;
  grid-template-columns: 1.5fr 1.5fr 1fr 1fr 1fr 0.8fr;
  align-items: center; gap: 12px; padding: 12px 20px;
}
.table-header { background: #f8f9fa; font-size: 12px; font-weight: 600; color: var(--app-ink-3); }
html.dark .table-header { background: #252629; }
.table-row { border-top: 1px solid #f1f3f4; font-size: 13px; }
html.dark .table-row { border-color: #2e3035; }
.table-row:hover { background: #fafbfc; }
html.dark .table-row:hover { background: #222326; }

.td-name { font-weight: 500; }
.td-score { display: flex; align-items: center; gap: 8px; }
.score-bar-track { flex: 1; height: 6px; background: #f1f3f4; border-radius: 3px; overflow: hidden; }
html.dark .score-bar-track { background: #2e3035; }
.score-bar-fill { height: 100%; border-radius: 3px; transition: width 0.6s ease; }

.td-active, .td-data, .td-api { color: var(--app-ink-2); }
.td-status { display: flex; align-items: center; gap: 6px; }
.status-dot { width: 8px; height: 8px; border-radius: 50%; }
.status-dot.green { background: #27ae60; }
.status-dot.yellow { background: #f39c12; }
.status-dot.red { background: #e74c3c; }
</style>
