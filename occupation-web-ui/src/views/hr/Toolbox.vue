<template>
  <div class="toolbox-page">
    <div class="page-head">
      <h2 class="page-title">HR 工具箱</h2>
      <p class="page-desc">招聘效率工具，辅助人才筛选与薪资决策。JD 优化已合并到职位管理的发布/编辑弹窗中。</p>
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
import { ArrowRight, Coin, Switch } from '@element-plus/icons-vue'

const tools = [
  {
    title: '人才对比',
    desc: '选择 2~4 个候选人，从技能、学历、经验、薪资期望等维度并排对比。',
    icon: Switch,
    route: '/hr/tools/compare-talents'
  },
  {
    title: '薪资竞争力分析',
    desc: '输入岗位，对比市场薪资分位，评估自家 offer 竞争力。',
    icon: Coin,
    route: '/hr/tools/salary-benchmark'
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
