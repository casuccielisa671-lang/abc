<template>
  <div class="org-manage">
    <!-- 标题 + 三标签 -->
    <div class="hub-head">
      <h2 class="page-title">组织管理</h2>
      <div class="seg" role="tablist">
        <button
          v-for="t in TABS" :key="t.key" type="button" role="tab"
          class="seg-btn" :class="{ active: activeTab === t.key }"
          @click="switchTab(t.key)"
        >{{ t.label }}</button>
      </div>
    </div>

    <!-- 用户：账号/角色/启用状态，批量导入 -->
    <div v-show="activeTab === 'users'" class="tab-panel">
      <UserManage embedded />
    </div>

    <!-- 班级 / 教师范围：同一个 ClassManage，section 控制显示哪块 -->
    <div v-show="activeTab === 'classes' || activeTab === 'scopes'" class="tab-panel">
      <ClassManage embedded :section="activeTab === 'scopes' ? 'scopes' : 'classes'" />
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import UserManage from '@/views/admin/UserManage.vue'
import ClassManage from '@/views/admin/ClassManage.vue'

const TABS = [
  { key: 'users', label: '用户' },
  { key: 'classes', label: '班级' },
  { key: 'scopes', label: '教师范围' }
]

const router = useRouter()
const route = useRoute()

// 当前标签由路由决定：/admin/user → 用户；/admin/class(?tab=scopes → 教师范围，否则 → 班级)
const activeTab = computed(() => {
  if (route.path === '/admin/user') return 'users'
  return route.query.tab === 'scopes' ? 'scopes' : 'classes'
})

function switchTab(key) {
  if (key === activeTab.value) return
  const target = {
    users: { path: '/admin/user' },
    classes: { path: '/admin/class' },
    scopes: { path: '/admin/class', query: { tab: 'scopes' } }
  }[key]
  router.push(target)
}
// UserManage / ClassManage 都用 v-show 常驻挂载：切换标签不重载、保留列表分页与筛选状态
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
</style>
