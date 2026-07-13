<template>
  <el-container class="main-layout">
    <el-header class="main-header" height="56px">
      <div class="brand" @click="$router.push(homePath)">
        <span class="brand-mark">职</span>
        <span class="brand-name">职业能力平台</span>
      </div>

      <el-menu
        mode="horizontal"
        router
        :default-active="activeIndex"
        class="top-menu"
        :ellipsis="false"
      >
        <template v-for="item in flatMenuItems" :key="item.index">
          <el-sub-menu v-if="item.children" :index="item.index">
            <template #title>
              <el-icon v-if="item.icon"><component :is="item.icon" /></el-icon>
              <span>{{ item.title }}</span>
            </template>
            <el-menu-item
              v-for="child in item.children"
              :key="child.index"
              :index="child.index"
            >
              <el-icon v-if="child.icon"><component :is="child.icon" /></el-icon>
              <span>{{ child.title }}</span>
            </el-menu-item>
          </el-sub-menu>

          <el-menu-item v-else :index="item.index">
            <el-icon v-if="item.icon"><component :is="item.icon" /></el-icon>
            <span>{{ item.title }}</span>
          </el-menu-item>
        </template>
      </el-menu>

      <div class="header-right">
        <button class="theme-toggle-btn" @click="handleThemeToggle" :title="appStore.dark ? '切换到日光模式' : '切换到夜光模式'">
          <svg v-if="!appStore.dark" class="theme-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <circle cx="12" cy="12" r="5" stroke="currentColor" stroke-width="2"/>
            <path d="M12 1v6m0 6v6M4.22 4.22l4.24 4.24m5.08 5.08l4.24 4.24M1 12h6m6 0h6M4.22 19.78l4.24-4.24m5.08-5.08l4.24-4.24" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
          </svg>
          <svg v-else class="theme-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </button>
        <span class="user-name">{{ userStore.realName || userStore.username }}</span>
        <el-tag size="small" class="role-tag">{{ roleLabel }}</el-tag>
        <el-button text type="danger" @click="userStore.logout()">退出登录</el-button>
      </div>
    </el-header>

    <el-main class="main-content" :class="{ 'main-content--full': isFullLayout }">
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/store/user'
import { useAppStore } from '@/store/app'
import {
  DataAnalysis, Setting, Document, User, House,
  Reading, Star, TrendCharts, Management,
  Tickets, ChatDotRound, Promotion, Notebook
} from '@element-plus/icons-vue'

const route = useRoute()
const userStore = useUserStore()
const appStore = useAppStore()

function handleThemeToggle() {
  appStore.toggleTheme()
}

const ROLE_HOME = {
  ADMIN: '/admin',
  STUDENT: '/student',
  TEACHER: '/teacher',
  HR: '/hr'
}

const homePath = computed(() => ROLE_HOME[userStore.role] || '/admin')

const isFullLayout = computed(() => route.meta.layout === 'full')

const roleLabel = computed(() => {
  const map = { ADMIN: '管理员', STUDENT: '学生', TEACHER: '教师', HR: 'HR' }
  return map[userStore.role] || userStore.role
})

/** 顶部水平菜单（按角色） */
const flatMenuItems = computed(() => {
  const menus = {
    ADMIN: [
      { index: '/admin', title: '首页', icon: House },
      { index: '/admin/dashboard', title: '数据看板', icon: DataAnalysis },
      { index: '/admin/employment', title: '就业分析', icon: TrendCharts },
      { index: '/admin/crawler', title: '采集管理', icon: Setting },
      { index: '/admin/report-list', title: '报告中心', icon: Document },
      { index: '/admin/user', title: '用户管理', icon: User },
      { index: '/admin/class', title: '班级管理', icon: Reading },
      { index: '/admin/news-manage', title: '资讯管理', icon: Notebook }
    ],
    STUDENT: [
      { index: '/student', title: '首页', icon: House },
      { index: '/student/jobs', title: '职位推荐', icon: Promotion },
      { index: '/student/profile', title: '个人画像', icon: User },
      { index: '/student/resume', title: '我的简历', icon: Tickets },
      { index: '/student/applications', title: '我的投递', icon: Promotion },
      { index: '/student/favorites', title: '我的收藏', icon: Star },
      { index: '/student/reports', title: '我的报告', icon: Document },
      { index: '/student/advisor', title: '职业顾问', icon: ChatDotRound },
      { index: '/student/news', title: '资讯', icon: Notebook }
    ],
    TEACHER: [
      { index: '/teacher', title: '首页', icon: House },
      { index: '/teacher/students', title: '学生管理', icon: Reading },
      { index: '/teacher/suggestions', title: '教学建议', icon: TrendCharts },
      { index: '/teacher/news', title: '资讯', icon: Notebook }
    ],
    HR: [
      { index: '/hr', title: '首页', icon: House },
      { index: '/hr/jobs', title: '职位管理', icon: Management },
      { index: '/hr/applications', title: '收到的投递', icon: Tickets },
      { index: '/hr/talents', title: '人才浏览', icon: User },
      { index: '/hr/news', title: '资讯', icon: Notebook }
    ]
  }
  return menus[userStore.role] || []
})

/** 路由高亮：精确匹配优先，其次最长前缀（如职位详情归到职位推荐） */
const activeIndex = computed(() => {
  const leafItems = flatMenuItems.value.flatMap(item =>
    item.children ? item.children : [item]
  )
  const exact = leafItems.find(i => i.index === route.path)
  if (exact) return exact.index
  const prefix = leafItems
    .filter(i => route.path.startsWith(i.index + '/'))
    .sort((a, b) => b.index.length - a.index.length)[0]
  return prefix ? prefix.index : route.path
})
</script>

<style scoped>
.main-layout {
  height: 100vh;
  /* 日光模式：科技蓝渐变背景（专业数据平台风格） */
  background: linear-gradient(135deg, #F8FAFC 0%, #EFF6FF 50%, #F0F4FF 100%);
}

/* 深色模式：深蓝-深灰渐变背景 */
html.dark .main-layout {
  background: linear-gradient(135deg, #0F172A 0%, #1E293B 50%, #1F2937 100%);
}

.main-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 20px;
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border-bottom: 1px solid var(--app-stone);
}

/* 深色模式header背景 */
html.dark .main-header {
  background: rgba(30, 20, 60, 0.5);
  border-bottom-color: rgba(100, 100, 100, 0.3);
}

.brand {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: none;
  cursor: pointer;
  margin-right: 8px;
}

.brand-mark {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: var(--app-action);
  color: var(--app-action-ink);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
  font-weight: 600;
}

.brand-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--app-ink);
  white-space: nowrap;
  letter-spacing: -0.013em;
}

/* 日光模式header文字颜色 */
html:not(.dark) .brand-name {
  color: #000000;
}

.top-menu {
  flex: 1;
  min-width: 0;
  border-bottom: none;
  background: transparent;
}

.header-right {
  flex: none;
  display: flex;
  align-items: center;
  gap: 12px;
  margin-left: 12px;
}

.theme-toggle-btn {
  background: transparent;
  border: 1px solid var(--app-stone);
  border-radius: 6px;
  padding: 6px 8px;
  color: var(--app-ink-2);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

.theme-toggle-btn:hover {
  background: var(--app-surface-2);
  border-color: var(--app-ink-3);
  color: var(--app-ink);
}

.theme-icon {
  width: 18px;
  height: 18px;
  stroke: currentColor;
  fill: none;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.user-name {
  font-size: 13px;
  color: var(--app-ink-2);
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 日光模式用户名颜色 */
html:not(.dark) .user-name {
  color: #000000;
}

.role-tag {
  border: none;
  background: var(--app-surface-2);
  color: var(--app-ink-3);
}

.main-content {
  padding: 32px;
  background: transparent;
  overflow-y: auto;
}

.main-content--full {
  padding: 0;
  overflow: hidden;
}
</style>

<style>
/* 水平菜单与主题对齐 */
.main-header .el-menu--horizontal > .el-menu-item,
.main-header .el-menu--horizontal > .el-sub-menu .el-sub-menu__title {
  height: 56px;
  line-height: 56px;
  color: var(--app-ink-2);
  border-bottom: 2px solid transparent;
}

.main-header .el-menu--horizontal > .el-menu-item.is-active,
.main-header .el-menu--horizontal > .el-sub-menu.is-active .el-sub-menu__title {
  color: var(--app-ink);
  font-weight: 600;
  border-bottom-color: var(--app-action);
  background: transparent;
}

.main-header .el-menu--horizontal > .el-menu-item:hover,
.main-header .el-menu--horizontal > .el-sub-menu .el-sub-menu__title:hover {
  color: var(--app-ink);
  background: var(--app-surface-2);
}

.main-header .el-menu--horizontal .el-menu-item .el-icon,
.main-header .el-menu--horizontal .el-sub-menu__title .el-icon {
  margin-right: 4px;
}
</style>
