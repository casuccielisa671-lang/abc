import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

/**
 * 用户状态管理
 * 存储 JWT Token、当前用户信息
 */
export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const username = ref(localStorage.getItem('username') || '')
  const role = ref(localStorage.getItem('role') || '')
  const realName = ref(localStorage.getItem('realName') || '')
  const tenantName = ref(localStorage.getItem('tenantName') || '')

  const isLoggedIn = computed(() => !!token.value)

  function setLogin(tokenVal, userInfo) {
    token.value = tokenVal
    localStorage.setItem('token', tokenVal)
    if (userInfo) {
      username.value = userInfo.username || ''
      role.value = userInfo.role || ''
      realName.value = userInfo.realName || ''
      tenantName.value = userInfo.tenantName || ''
      localStorage.setItem('username', userInfo.username || '')
      localStorage.setItem('role', userInfo.role || '')
      localStorage.setItem('realName', userInfo.realName || '')
      localStorage.setItem('tenantName', userInfo.tenantName || '')
    }
  }

  function logout() {
    token.value = ''
    username.value = ''
    role.value = ''
    realName.value = ''
    tenantName.value = ''
    localStorage.clear()
    window.location.href = '/login'
  }

  return { token, username, role, realName, tenantName, isLoggedIn, setLogin, logout }
})
