import { defineStore } from 'pinia'
import { ref } from 'vue'

/**
 * 全局应用状态
 */
export const useAppStore = defineStore('app', () => {
  const sidebarCollapsed = ref(false)
  const loading = ref(false)

  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  return { sidebarCollapsed, loading, toggleSidebar }
})
