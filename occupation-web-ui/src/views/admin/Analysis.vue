<template>
  <div class="analysis-hub">
    <!-- 标题 + 双标签 + 共用重算按钮 -->
    <div class="hub-head">
      <h2 class="page-title">数据分析</h2>
      <div class="seg" role="tablist">
        <button
          v-for="t in TABS" :key="t.key" type="button" role="tab"
          class="seg-btn" :class="{ active: activeTab === t.key }"
          @click="switchTab(t.key)"
        >{{ t.label }}</button>
      </div>
      <el-button
        type="primary" class="rebuild-btn" :loading="rebuilding" @click="handleRebuild"
      >重算分析数据</el-button>
    </div>

    <!-- 市场看板：市场供给侧（行业/城市/技能/学历/趋势） -->
    <!-- v-if 只挂载当前标签：图表在可见容器里初始化，避免隐藏标签算出 0 宽度 -->
    <div v-if="activeTab === 'market'" class="tab-panel">
      <p class="panel-note">市场有什么岗位：行业 / 城市 / 技能热度 / 学历 / 趋势。数据每日凌晨 2:00 自动更新</p>
      <Dashboard ref="marketRef" embedded />
    </div>

    <!-- 就业分析：学生需求侧（投递漏斗/意向/供需错配） -->
    <div v-if="activeTab === 'employment'" class="tab-panel">
      <p class="panel-note">本校学生怎么样：投递转化 / 意向城市行业 / 供需错配。若为空，点右上「重算分析数据」</p>
      <Employment ref="empRef" embedded />
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { rebuildAnalysis } from '@/api/admin'
import Dashboard from '@/views/admin/Dashboard.vue'
import Employment from '@/views/admin/Employment.vue'

const TABS = [
  { key: 'market', label: '市场看板' },
  { key: 'employment', label: '就业分析' }
]

const router = useRouter()
const route = useRoute()
const rebuilding = ref(false)
const marketRef = ref(null)
const empRef = ref(null)

// 当前标签由路由决定：/admin/employment → 就业分析，其余（/admin/dashboard）→ 市场看板
const activeTab = computed(() => (route.path === '/admin/employment' ? 'employment' : 'market'))

function switchTab(key) {
  if (key === activeTab.value) return
  router.push(key === 'employment' ? '/admin/employment' : '/admin/dashboard')
}

// 两个看板的数据同源（analysis_result），重算一次两边都新。点后刷新当前可见标签。
async function handleRebuild() {
  rebuilding.value = true
  try {
    const count = await rebuildAnalysis()
    ElMessage.success(`重算完成，共更新 ${count} 条分析结果`)
    const active = activeTab.value === 'market' ? marketRef.value : empRef.value
    active?.reload?.()
  } catch {
    /* 拦截器已提示 */
  } finally {
    rebuilding.value = false
  }
}
</script>

<style scoped>
.hub-head {
  display: flex;
  align-items: center;
  gap: 18px;
  flex-wrap: wrap;
  margin-bottom: 18px;
}
.hub-head .page-title { margin: 0; }
.rebuild-btn { margin-left: auto; }

.seg {
  display: inline-flex;
  gap: 4px;
  padding: 4px;
  background: var(--color-fill-light, var(--color-surface));
  border: 1px solid var(--color-border);
  border-radius: 12px;
}
.seg-btn {
  padding: 6px 14px;
  border: none;
  background: transparent;
  color: var(--color-text-secondary);
  font-size: 13.5px;
  font-weight: 500;
  border-radius: 9px;
  cursor: pointer;
  transition: background .15s, color .15s;
  white-space: nowrap;
}
.seg-btn:hover { color: var(--color-text-primary); }
.seg-btn.active { background: var(--color-primary); color: #fff; font-weight: 600; }

.panel-note {
  font-size: 12px;
  color: var(--color-text-tertiary);
  margin: 0 0 14px;
}
</style>
