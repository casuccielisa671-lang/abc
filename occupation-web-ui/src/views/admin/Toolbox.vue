<template>
  <div class="toolbox-page">
    <div class="page-head">
      <h2 class="page-title">管理工具箱</h2>
      <p class="page-desc">数据导出、运营监控与效率工具</p>
    </div>

    <div class="tool-grid">
      <div
        v-for="tool in tools"
        :key="tool.route"
        class="tool-card"
        @click="$router.push(tool.route)"
      >
        <div class="card-icon">
          <el-icon :size="24"><component :is="tool.icon" /></el-icon>
        </div>
        <div class="card-body">
          <h3 class="card-title">{{ tool.title }}</h3>
          <p class="card-desc">{{ tool.desc }}</p>
        </div>
        <div class="card-arrow">
          <el-icon :size="16"><ArrowRight /></el-icon>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ArrowRight, Download, Monitor } from '@element-plus/icons-vue'

const tools = [
  {
    title: '数据导出中心',
    desc: '批量导出看板数据、就业分析结果为 Excel/PDF，支持自定义字段与时间范围',
    icon: Download,
    route: '/admin/tools/export-center'
  },
  {
    title: '租户健康度监控',
    desc: '各租户活跃度、数据完整度、API 调用量一览，及时发现异常',
    icon: Monitor,
    route: '/admin/tools/tenant-health'
  }
]
</script>

<style scoped>
.toolbox-page { max-width: 900px; margin: 0 auto; padding: 16px 0 40px; }
.page-head { margin-bottom: 32px; }
.page-title { font-size: 22px; font-weight: 600; color: var(--app-ink); margin: 0 0 6px; letter-spacing: -0.3px; }
.page-desc { font-size: 14px; color: var(--app-ink-3); margin: 0; }

.tool-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px; }

.tool-card {
  display: flex; align-items: flex-start; gap: 14px; padding: 24px;
  background: #fff; border: 1px solid #e8eaed; border-radius: 12px;
  cursor: pointer; transition: all 0.2s ease;
}
html.dark .tool-card { background: #1e1f22; border-color: #2e3035; }
.tool-card:hover { border-color: #c4c7cc; box-shadow: 0 2px 12px rgba(0,0,0,0.06); }
html.dark .tool-card:hover { border-color: #4a4d52; box-shadow: 0 2px 12px rgba(0,0,0,0.3); }

.card-icon {
  width: 42px; height: 42px; border-radius: 10px;
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
  background: #f1f3f4; color: #5f6368; transition: all 0.2s ease;
}
html.dark .card-icon { background: #2e3035; color: #9aa0a6; }
.tool-card:hover .card-icon { background: #1a1a1a; color: #fff; }
html.dark .tool-card:hover .card-icon { background: #fff; color: #1a1a1a; }

.card-body { flex: 1; min-width: 0; }
.card-title { font-size: 15px; font-weight: 600; color: var(--app-ink); margin: 0 0 6px; }
.card-desc { font-size: 13px; color: var(--app-ink-3); line-height: 1.6; margin: 0; }

.card-arrow { flex-shrink: 0; margin-top: 12px; color: var(--app-ink-4); transition: all 0.2s ease; }
.tool-card:hover .card-arrow { color: var(--app-ink); transform: translateX(3px); }

@media (max-width: 640px) { .tool-grid { grid-template-columns: 1fr; } }
</style>
