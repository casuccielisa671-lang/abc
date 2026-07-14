<template>
  <div class="my-data">
    <!-- 标题 + 双标签切换栏 -->
    <div class="hub-head">
      <h2 class="page-title">我的资料</h2>
      <div class="seg" role="tablist">
        <button
          v-for="t in TABS" :key="t.key" type="button" role="tab"
          class="seg-btn" :class="{ active: activeTab === t.key }"
          @click="switchTab(t.key)"
        >{{ t.label }}</button>
      </div>
    </div>

    <!-- 个人画像：喂推荐算法的结构化匹配依据 -->
    <div v-show="activeTab === 'profile'" class="tab-panel">
      <p class="panel-note">结构化匹配依据，填得越全，职位推荐越准</p>
      <Profile embedded />
    </div>

    <!-- 我的简历：给 HR 和 AI 读的自我陈述 -->
    <div v-show="activeTab === 'resume'" class="tab-panel">
      <p class="panel-note">给 HR 和 AI 看的自我陈述（教育 / 项目 / 实习经历），与画像用途不同</p>
      <Resume embedded />
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import Profile from '@/views/student/Profile.vue'
import Resume from '@/views/student/Resume.vue'

const TABS = [
  { key: 'profile', label: '个人画像' },
  { key: 'resume', label: '我的简历' }
]

const router = useRouter()
const route = useRoute()

// 当前标签由路由决定：/student/resume → 我的简历，其余（/student/profile）→ 个人画像
const activeTab = computed(() => (route.path === '/student/resume' ? 'resume' : 'profile'))

function switchTab(key) {
  if (key === activeTab.value) return
  router.push(key === 'resume' ? '/student/resume' : '/student/profile')
}
// 两个表单都用 v-show 常驻挂载：切换标签不重新加载、保留未保存的编辑
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
