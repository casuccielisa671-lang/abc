import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getUnreadCount } from '@/api/push'

/**
 * 站内消息未读数 —— 铃铛红点与首页消息格子共享同一份计数，
 * 任一处标记已读后调用 refresh() 或 decrement() 保持同步。
 */
export const useMessageStore = defineStore('message', () => {
  const unread = ref(0)

  async function refreshUnread() {
    try {
      unread.value = await getUnreadCount()
    } catch {
      /* 未登录或网络异常时忽略，不打扰用户 */
    }
  }

  function decrement(n = 1) {
    unread.value = Math.max(0, unread.value - n)
  }

  function reset() {
    unread.value = 0
  }

  return { unread, refreshUnread, decrement, reset }
})
