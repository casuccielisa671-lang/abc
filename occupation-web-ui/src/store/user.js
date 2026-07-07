import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

/**
 * 用户状态管理
 * 存储 JWT Token、当前用户信息、租户ID
 */
export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const username = ref('')
  const role = ref('')
  const tenantId = ref(null)
  const realName = ref('')

  const isLoggedIn = computed(() => !!token.value)

  // TODO: POST /api/auth/login → 存储 token 并解析用户信息
  function setLogin(tokenVal, userInfo) {
    token.value = tokenVal
    localStorage.setItem('token', tokenVal)
    if (userInfo) {
      username.value = userInfo.username
      role.value = userInfo.role
      tenantId.value = userInfo.tenantId
      realName.value = userInfo.realName
    }
  }

  function logout() {
    token.value = ''
    localStorage.removeItem('token')
    username.value = ''
    role.value = ''
    tenantId.value = null
    realName.value = ''
    window.location.href = '/login'
  }

  return { token, username, role, tenantId, realName, isLoggedIn, setLogin, logout }
})
