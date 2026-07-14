<template>
  <div class="toolbox-page">
    <div class="page-head">
      <h2 class="page-title">求职工具箱</h2>
      <p class="page-desc">选择工具，辅助职业决策与求职准备</p>
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
import { ArrowRight, TrendCharts, Coin, DataAnalysis, List } from '@element-plus/icons-vue'

const tools = [
  {
    title: '多岗位对比',
    desc: '选择 2~4 个岗位，从薪资、技能、学历、城市等多维度并排对比',
    icon: DataAnalysis,
    route: '/student/tools/compare-jobs'
  },
  {
    title: '技能 ROI 分析',
    desc: '量化学习某项技能的薪资回报与市场需求，判断值不值得学',
    icon: TrendCharts,
    route: '/student/tools/skill-roi'
  },
  {
    title: '期望薪资计算器',
    desc: '输入城市、岗位、学历、经验，基于历史数据给出合理期望薪资',
    icon: Coin,
    route: '/student/tools/salary-calc'
  },
  {
    title: '求职清单生成器',
    desc: '对比岗位要求与个人技能，生成差距分析与学习路径建议',
    icon: List,
    route: '/student/tools/job-checklist'
  }
]
</script>

<style scoped>
.toolbox-page {
  max-width: 900px;
  margin: 0 auto;
  padding: 16px 0 40px;
}

/* ===== 页头 ===== */
.page-head {
  margin-bottom: 32px;
}

.page-title {
  font-size: 22px;
  font-weight: 600;
  color: var(--app-ink);
  margin: 0 0 6px;
  letter-spacing: -0.3px;
}

.page-desc {
  font-size: 14px;
  color: var(--app-ink-3);
  margin: 0;
}

/* ===== 卡片网格 ===== */
.tool-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

/* ===== 卡片 ===== */
.tool-card {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 24px;
  background: #fff;
  border: 1px solid #e8eaed;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}

html.dark .tool-card {
  background: #1e1f22;
  border-color: #2e3035;
}

.tool-card:hover {
  border-color: #c4c7cc;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

html.dark .tool-card:hover {
  border-color: #4a4d52;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.3);
}

.card-icon {
  width: 42px;
  height: 42px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  background: #f1f3f4;
  color: #5f6368;
  transition: all 0.2s ease;
}

html.dark .card-icon {
  background: #2e3035;
  color: #9aa0a6;
}

.tool-card:hover .card-icon {
  background: #1a1a1a;
  color: #fff;
}

html.dark .tool-card:hover .card-icon {
  background: #fff;
  color: #1a1a1a;
}

.card-body {
  flex: 1;
  min-width: 0;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--app-ink);
  margin: 0 0 6px;
}

.card-desc {
  font-size: 13px;
  color: var(--app-ink-3);
  line-height: 1.6;
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-arrow {
  flex-shrink: 0;
  margin-top: 12px;
  color: var(--app-ink-4);
  transition: all 0.2s ease;
}

.tool-card:hover .card-arrow {
  color: var(--app-ink);
  transform: translateX(3px);
}

/* ===== 响应式 ===== */
@media (max-width: 640px) {
  .tool-grid {
    grid-template-columns: 1fr;
  }
}
</style>
