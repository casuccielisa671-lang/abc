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
import {
  DataAnalysis, Setting, Document, User, House,
  Reading, Star, Files, TrendCharts, Management,
  Tickets, ChatDotRound, Promotion
} from '@element-plus/icons-vue'

const route = useRoute()
const userStore = useUserStore()

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
      {
        index: 'admin-report',
        title: '报告中心',
        icon: Document,
        children: [
          { index: '/admin/report-template', title: '模板管理', icon: Files },
          { index: '/admin/report-list', title: '报告列表', icon: Document }
        ]
      },
      { index: '/admin/user', title: '用户管理', icon: User }
    ],
    STUDENT: [
      { index: '/student', title: '首页', icon: House },
      { index: '/student/jobs', title: '职位推荐', icon: Promotion },
      { index: '/student/profile', title: '个人画像', icon: User },
      { index: '/student/resume', title: '我的简历', icon: Tickets },
      { index: '/student/applications', title: '我的投递', icon: Promotion },
      { index: '/student/favorites', title: '我的收藏', icon: Star },
      { index: '/student/reports', title: '我的报告', icon: Document },
      { index: '/student/advisor', title: '职业顾问', icon: ChatDotRound }
    ],
    TEACHER: [
      { index: '/teacher', title: '首页', icon: House },
      { index: '/teacher/students', title: '学生管理', icon: Reading },
      { index: '/teacher/suggestions', title: '教学建议', icon: TrendCharts }
    ],
    HR: [
      { index: '/hr', title: '首页', icon: House },
      { index: '/hr/jobs', title: '职位管理', icon: Management },
      { index: '/hr/applications', title: '收到的投递', icon: Tickets },
      { index: '/hr/talents', title: '人才浏览', icon: User }
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
  background: var(--app-canvas);
}

.main-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 20px;
  background: var(--app-canvas);
  border-bottom: 1px solid var(--app-stone);
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
  gap: 8px;
  margin-left: 12px;
}

.user-name {
  font-size: 13px;
  color: var(--app-ink-2);
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.role-tag {
  border: none;
  background: var(--app-surface-2);
  color: var(--app-ink-3);
}

.main-content {
  padding: 32px;
  background: var(--app-canvas);
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
