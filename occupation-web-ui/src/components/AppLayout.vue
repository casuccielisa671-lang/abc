<template>
  <el-container class="app-layout">
    <el-aside :width="sidebarCollapsed ? '64px' : '220px'">
      <div class="logo">{{ sidebarCollapsed ? '职业' : '职业能力平台' }}</div>
      <el-menu
        :collapse="sidebarCollapsed"
        :default-active="activeMenu"
        router
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
      >
        <template v-for="item in menuItems" :key="item.index">
          <el-sub-menu v-if="item.children" :index="item.index">
            <template #title>
              <el-icon><component :is="item.icon" /></el-icon>
              <span>{{ item.title }}</span>
            </template>
            <el-menu-item v-for="child in item.children" :key="child.index" :index="child.index">
              <el-icon><component :is="child.icon" /></el-icon>
              <span>{{ child.title }}</span>
            </el-menu-item>
          </el-sub-menu>
          <el-menu-item v-else :index="item.index">
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.title }}</span>
          </el-menu-item>
        </template>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header>
        <el-button text @click="appStore.toggleSidebar()">
          <el-icon :size="20"><Fold v-if="!sidebarCollapsed" /><Expand v-else /></el-icon>
        </el-button>
        <span class="username">{{ userStore.realName || userStore.username }}</span>
        <el-tag size="small" type="info" style="margin-right:12px">{{ roleLabel }}</el-tag>
        <el-button text type="danger" @click="userStore.logout()">退出登录</el-button>
      </el-header>

      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/store/user'
import { useAppStore } from '@/store/app'
import {
  DataAnalysis, Setting, Document, User, House,
  Reading, Star, Files, School, TrendCharts, Management
} from '@element-plus/icons-vue'

const route = useRoute()
const userStore = useUserStore()
const appStore = useAppStore()

const sidebarCollapsed = computed(() => appStore.sidebarCollapsed)
const activeMenu = computed(() => route.path)

const roleLabel = computed(() => {
  const map = { ADMIN: '管理员', STUDENT: '学生', TEACHER: '教师', HR: 'HR' }
  return map[userStore.role] || userStore.role
})

/** 按角色动态菜单 */
const menuItems = computed(() => {
  const role = userStore.role

  const menus = {
    ADMIN: [
      { index: '/admin', title: '数据看板', icon: 'DataAnalysis' },
      { index: '/admin/crawler', title: '采集管理', icon: 'Setting' },
      {
        index: 'report-group',
        title: '报告中心',
        icon: 'Document',
        children: [
          { index: '/admin/report-template', title: '模板管理', icon: 'Files' },
          { index: '/admin/report-list', title: '报告列表', icon: 'Document' }
        ]
      },
      { index: '/admin/user', title: '用户管理', icon: 'User' }
    ],
    STUDENT: [
      { index: '/student', title: '职位推荐', icon: 'House' },
      { index: '/student/profile', title: '个人画像', icon: 'User' },
      { index: '/student/favorites', title: '我的收藏', icon: 'Star' },
      { index: '/student/reports', title: '我的报告', icon: 'Document' }
    ],
    TEACHER: [
      { index: '/teacher', title: '班级概览', icon: 'DataAnalysis' },
      { index: '/teacher/students', title: '学生管理', icon: 'Reading' },
      { index: '/teacher/suggestions', title: '教学建议', icon: 'TrendCharts' }
    ],
    HR: [
      { index: '/hr', title: '工作台', icon: 'DataAnalysis' },
      { index: '/hr/jobs', title: '职位管理', icon: 'Management' },
      { index: '/hr/talents', title: '人才浏览', icon: 'User' }
    ]
  }

  return menus[role] || []
})
</script>

<style scoped>
.app-layout { height: 100vh; }
.el-aside { background: #304156; overflow-x: hidden; transition: width 0.3s; }
.logo { color: #fff; text-align: center; padding: 16px 0; font-size: 18px; white-space: nowrap; font-weight: bold; }
.el-header {
  display: flex; align-items: center; border-bottom: 1px solid #eee;
  background: #fff; padding: 0 16px;
}
.username { margin-left: auto; margin-right: 8px; color: #303133; }
.el-main { background: #f0f2f5; padding: 20px; }
</style>
