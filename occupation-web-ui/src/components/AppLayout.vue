<template>
  <div class="app-layout">
    <!-- 侧边栏 -->
    <aside class="sidebar" :class="{ collapsed: sidebarCollapsed }">
      <div class="brand">
        <div class="brand-mark">职</div>
        <div v-if="!sidebarCollapsed" class="brand-name">职业能力平台</div>
      </div>

      <nav class="nav">
        <template v-for="group in menuGroups" :key="group.label">
          <div v-if="!sidebarCollapsed" class="nav-group">{{ group.label }}</div>
          <div
            v-for="item in group.items" :key="item.index"
            class="nav-item"
            :class="{ active: item.index === activeIndex }"
            :title="sidebarCollapsed ? item.title : ''"
            @click="$router.push(item.index)"
          >
            <el-icon :size="16"><component :is="item.icon" /></el-icon>
            <span v-if="!sidebarCollapsed">{{ item.title }}</span>
          </div>
        </template>
      </nav>

      <div class="side-user">
        <div class="avatar">{{ avatarText }}</div>
        <div v-if="!sidebarCollapsed" class="who">
          <div class="who-name">{{ userStore.realName || userStore.username }}</div>
          <div class="who-sub">{{ userStore.tenantName || '—' }} · {{ roleLabel }}</div>
        </div>
      </div>
    </aside>

    <!-- 主区域 -->
    <div class="main">
      <header class="topbar">
        <el-button text @click="appStore.toggleSidebar()">
          <el-icon :size="18"><Fold v-if="!sidebarCollapsed" /><Expand v-else /></el-icon>
        </el-button>
        <span class="page-name">{{ pageTitle }}</span>

        <div class="topbar-right">
          <el-button text circle :title="appStore.dark ? '切换到浅色模式' : '切换到深色模式'" @click="appStore.toggleTheme()">
            <el-icon :size="16"><Sunny v-if="appStore.dark" /><Moon v-else /></el-icon>
          </el-button>
          <el-button text type="danger" @click="userStore.logout()">退出登录</el-button>
        </div>
      </header>

      <main class="content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/store/user'
import { useAppStore } from '@/store/app'
import {
  DataAnalysis, Setting, Document, User, House, Fold, Expand, Sunny, Moon,
  Reading, Star, Files, TrendCharts, Management, Tickets
} from '@element-plus/icons-vue'

const route = useRoute()
const userStore = useUserStore()
const appStore = useAppStore()

const sidebarCollapsed = computed(() => appStore.sidebarCollapsed)

const roleLabel = computed(() => {
  const map = { ADMIN: '管理员', STUDENT: '学生', TEACHER: '教师', HR: 'HR' }
  return map[userStore.role] || userStore.role
})

const avatarText = computed(() => {
  const name = userStore.realName || userStore.username || '?'
  return name.slice(0, 1)
})

/** 按角色分组导航 */
const menuGroups = computed(() => {
  const groups = {
    ADMIN: [
      {
        label: '工作台',
        items: [
          { index: '/admin', title: '数据看板', icon: DataAnalysis },
          { index: '/admin/crawler', title: '采集管理', icon: Setting }
        ]
      },
      {
        label: '报告中心',
        items: [
          { index: '/admin/report-template', title: '模板管理', icon: Files },
          { index: '/admin/report-list', title: '报告列表', icon: Document }
        ]
      },
      {
        label: '系统',
        items: [
          { index: '/admin/user', title: '用户管理', icon: User }
        ]
      }
    ],
    STUDENT: [
      {
        label: '求职服务',
        items: [
          { index: '/student', title: '职位推荐', icon: House },
          { index: '/student/profile', title: '个人画像', icon: User },
          { index: '/student/favorites', title: '我的收藏', icon: Star },
          { index: '/student/reports', title: '我的报告', icon: Document }
        ]
      }
    ],
    TEACHER: [
      {
        label: '教学管理',
        items: [
          { index: '/teacher', title: '班级概览', icon: DataAnalysis },
          { index: '/teacher/students', title: '学生管理', icon: Reading },
          { index: '/teacher/suggestions', title: '教学建议', icon: TrendCharts }
        ]
      }
    ],
    HR: [
      {
        label: '招聘管理',
        items: [
          { index: '/hr', title: '工作台', icon: DataAnalysis },
          { index: '/hr/jobs', title: '职位管理', icon: Management },
          { index: '/hr/applications', title: '收到的投递', icon: Tickets },
          { index: '/hr/talents', title: '人才浏览', icon: User }
        ]
      }
    ]
  }
  return groups[userStore.role] || []
})

/** 当前高亮项：优先精确匹配，其次最长前缀匹配（如职位详情归到职位推荐） */
const activeIndex = computed(() => {
  const items = menuGroups.value.flatMap(g => g.items)
  const exact = items.find(i => i.index === route.path)
  if (exact) return exact.index
  const prefix = items
    .filter(i => route.path.startsWith(i.index + '/'))
    .sort((a, b) => b.index.length - a.index.length)[0]
  return prefix ? prefix.index : ''
})

/** 顶栏页面标题 */
const pageTitle = computed(() => {
  const items = menuGroups.value.flatMap(g => g.items)
  const hit = items.find(i => i.index === activeIndex.value)
  if (route.name === 'JobDetail') return '职位详情'
  return hit ? hit.title : ''
})
</script>

<style scoped>
.app-layout { display: flex; height: 100vh; background: var(--app-canvas); }

/* ---- 侧边栏：与画布同色，靠一条内描边分隔，不用投影 ---- */
.sidebar {
  width: 216px;
  flex: none;
  display: flex;
  flex-direction: column;
  background: var(--app-canvas);
  border-right: 1px solid var(--app-stone);
  padding: 20px 12px 12px;
  transition: width .25s;
  overflow: hidden;
}
.sidebar.collapsed { width: 64px; }

.brand { display: flex; align-items: center; gap: 10px; padding: 2px 6px 20px; }
.brand-mark {
  width: 32px; height: 32px; border-radius: 8px; flex: none;
  background: var(--app-action); color: var(--app-action-ink);
  display: flex; align-items: center; justify-content: center;
  font-size: 15px; font-weight: 600;
}
.brand-name {
  font-size: 15px; font-weight: 600; white-space: nowrap;
  color: var(--app-ink); letter-spacing: -0.013em;
}

.nav { flex: 1; display: flex; flex-direction: column; gap: 2px; overflow-y: auto; }
.nav-group {
  font-size: 12px; color: var(--app-ink-3);
  letter-spacing: .08em; padding: 16px 10px 6px; white-space: nowrap;
}
.nav-item {
  display: flex; align-items: center; gap: 10px;
  padding: 9px 10px; border-radius: 8px;
  font-size: 14px; color: var(--app-ink-2);
  cursor: pointer; white-space: nowrap;
  transition: background .12s, color .12s;
}
.sidebar.collapsed .nav-item { justify-content: center; padding: 10px 0; }
.nav-item:hover { background: var(--app-surface-2); color: var(--app-ink); }
/* 选中项：墨黑实心，全站唯一的深色表面之一（深色模式下翻转为奶油） */
.nav-item.active {
  background: var(--app-action);
  color: var(--app-action-ink);
  font-weight: 600;
}

.side-user {
  display: flex; align-items: center; gap: 10px;
  padding: 14px 6px 4px;
  border-top: 1px solid var(--app-stone);
  margin-top: 8px;
}
.sidebar.collapsed .side-user { justify-content: center; }
.avatar {
  width: 30px; height: 30px; border-radius: 50%; flex: none;
  background: var(--app-surface-2); color: var(--app-ink-2);
  box-shadow: var(--app-hairline-strong);
  display: flex; align-items: center; justify-content: center;
  font-size: 12px; font-weight: 600;
}
.who { min-width: 0; }
.who-name { font-size: 13px; font-weight: 600; color: var(--app-ink); white-space: nowrap; }
.who-sub {
  font-size: 12px; color: var(--app-ink-3); white-space: nowrap;
  overflow: hidden; text-overflow: ellipsis;
}

/* ---- 主区域 ---- */
.main { flex: 1; display: flex; flex-direction: column; min-width: 0; }
.topbar {
  display: flex; align-items: center; gap: 8px;
  padding: 12px 24px;
  background: var(--app-canvas);
  border-bottom: 1px solid var(--app-stone);
}
.page-name { font-size: 15px; font-weight: 600; color: var(--app-ink); letter-spacing: -0.013em; }
.topbar-right { margin-left: auto; display: flex; align-items: center; gap: 4px; }

.content { flex: 1; overflow-y: auto; background: var(--app-canvas); padding: 32px; }
</style>
