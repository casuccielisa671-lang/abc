<template>
  <el-container class="app-layout">
    <el-aside :width="sidebarCollapsed ? '64px' : '220px'">
      <div class="logo">{{ sidebarCollapsed ? '职业' : '职业能力平台' }}</div>
      <!-- TODO: 根据 role 动态渲染侧边栏菜单 -->
      <el-menu :collapse="sidebarCollapsed" :default-active="activeMenu" router>
        <el-menu-item index="/admin">
          <el-icon><DataAnalysis /></el-icon>
          <span>数据看板</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header>
        <el-button @click="appStore.toggleSidebar()">
          <el-icon><Fold /></el-icon>
        </el-button>
        <span class="username">{{ userStore.realName || userStore.username }}</span>
        <el-button @click="userStore.logout()">退出登录</el-button>
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

const route = useRoute()
const userStore = useUserStore()
const appStore = useAppStore()

const sidebarCollapsed = computed(() => appStore.sidebarCollapsed)
const activeMenu = computed(() => route.path)
</script>

<style scoped>
.app-layout { height: 100vh; }
.el-aside { background: #304156; overflow-x: hidden; }
.logo { color: #fff; text-align: center; padding: 16px 0; font-size: 18px; white-space: nowrap; }
.el-header { display: flex; align-items: center; border-bottom: 1px solid #eee; }
.username { margin-left: auto; margin-right: 12px; }
.el-main { background: #f0f2f5; padding: 20px; }
</style>
