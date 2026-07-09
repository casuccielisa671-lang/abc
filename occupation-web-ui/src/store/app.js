import { defineStore } from 'pinia'
import { ref } from 'vue'

/**
 * 全局应用状态
 */
export const useAppStore = defineStore('app', () => {
  const sidebarCollapsed = ref(false)
  const loading = ref(false)
  // 深色模式：跟随上次选择，默认浅色
  const dark = ref(localStorage.getItem('theme') === 'dark')

  function applyTheme() {
    document.documentElement.classList.toggle('dark', dark.value)
  }

  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  function toggleTheme() {
    dark.value = !dark.value
    localStorage.setItem('theme', dark.value ? 'dark' : 'light')
    applyTheme()
  }

  // 初始化时应用一次
  applyTheme()

  return { sidebarCollapsed, loading, dark, toggleSidebar, toggleTheme }
})
