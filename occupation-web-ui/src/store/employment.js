import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getEmploymentStatus } from '@/api/student'

/**
 * 当前登录学生的就业状态 —— 投递/联系按钮的禁用、首页徽章共享同一份，避免各页各查。
 * 接收录用/投递后调用 refresh() 更新。
 */
export const useEmploymentStore = defineStore('employment', () => {
  const status = ref('')          // EMPLOYED / OFFERED / SEEKING / IDLE / ''
  const loaded = ref(false)

  async function refresh() {
    try {
      status.value = await getEmploymentStatus()
      loaded.value = true
    } catch {
      /* 未登录或非学生时忽略 */
    }
  }

  const employed = computed(() => status.value === 'EMPLOYED')

  return { status, loaded, employed, refresh }
})
