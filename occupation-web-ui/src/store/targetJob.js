import { defineStore } from 'pinia'
import { ref } from 'vue'
import { useUserStore } from './user'

/**
 * 学生选定的「目标岗位」——职位信息「目标岗位」标签里选,首页「能力画像」卡读同一份。
 * 持久化在 localStorage、按账号隔离(username);不预设,新用户为 null=未选择。
 * 存整条职位对象(含 skills),这样首页无需再查库即可算技能对比。
 */
export const useTargetJobStore = defineStore('targetJob', () => {
  const target = ref(null)   // { id, title, company, city, skills, salaryMin, salaryMax } | null

  function storageKey() {
    const u = useUserStore()
    return `targetJob:${u.username || 'anon'}`
  }

  /** 从 localStorage 还原当前账号的目标岗位（组件挂载时调，切账号也能刷新到对的那份） */
  function load() {
    try {
      target.value = JSON.parse(localStorage.getItem(storageKey()) || 'null')
    } catch {
      target.value = null
    }
  }

  function setTarget(job) {
    target.value = job || null
    if (job) localStorage.setItem(storageKey(), JSON.stringify(job))
    else localStorage.removeItem(storageKey())
  }

  function clear() { setTarget(null) }

  return { target, load, setTarget, clear }
})
