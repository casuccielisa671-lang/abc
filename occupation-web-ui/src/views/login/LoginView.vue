<template>
  <div class="login-page">
    <!-- 星星背景 -->
    <div class="stars-container">
      <div v-for="i in 60" :key="`star-${i}`" class="star" :style="{ '--star-delay': `${i * 0.1}s`, '--star-duration': `${2 + Math.random() * 2}s` }"></div>
    </div>

    <!-- 极光流体背景 -->
    <div class="aurora-background">
      <div class="aurora-blob aurora-blob-1"></div>
      <div class="aurora-blob aurora-blob-2"></div>
      <div class="aurora-blob aurora-blob-3"></div>
      <div class="aurora-blob aurora-blob-4"></div>
    </div>

    <!-- 主题切换按钮 (右上角) -->
    <button class="theme-toggle" @click="handleThemeToggle" :title="appStore.dark ? '切换到日光模式' : '切换到夜光模式'">
      <svg v-if="!appStore.dark" class="theme-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
        <circle cx="12" cy="12" r="5" stroke="currentColor" stroke-width="2"/>
        <path d="M12 1v6m0 6v6M4.22 4.22l4.24 4.24m5.08 5.08l4.24 4.24M1 12h6m6 0h6M4.22 19.78l4.24-4.24m5.08-5.08l4.24-4.24" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
      </svg>
      <svg v-else class="theme-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
        <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
      </svg>
    </button>

    <!-- 主容器：左操作台 + 右介绍 -->
    <div class="login-container">
      <!-- 左侧：玻璃态登录卡片 -->
      <div class="login-card-section">
        <div class="login-card">
          <h2 class="card-title">登录</h2>
          <p class="card-subtitle">选择身份进入平台</p>

          <!-- 角色选择 -->
          <div class="role-tabs" role="tablist" aria-label="登录身份">
            <button
              v-for="r in ROLES" :key="r.value"
              type="button" class="role-tab" role="tab"
              :aria-selected="selectedRole === r.value"
              :class="{ active: selectedRole === r.value }"
              @click="selectedRole = r.value"
            >{{ r.label }}</button>
          </div>

          <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="handleLogin">
            <el-form-item prop="tenantName" label="学校 / 企业节点">
              <el-select
                v-model="form.tenantName"
                filterable
                :loading="tenantsLoading"
                placeholder="搜索或选择学校 / 企业"
                style="width:100%"
              >
                <el-option v-for="t in tenants" :key="t.id" :label="t.name" :value="t.name" />
              </el-select>
            </el-form-item>

            <el-form-item prop="username" label="账号">
              <el-input v-model="form.username" :placeholder="currentRole.placeholder" />
            </el-form-item>

            <el-form-item prop="password" label="密码">
              <el-input
                v-model="form.password"
                :type="showPassword ? 'text' : 'password'"
                placeholder="请输入密码"
                class="password-input"
              >
                <template #suffix>
                  <button
                    type="button"
                    class="password-toggle"
                    @click="showPassword = !showPassword"
                  >
                    <svg v-if="!showPassword" class="eye-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                      <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                      <circle cx="12" cy="12" r="3" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                    </svg>
                    <svg v-else class="eye-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                      <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                      <line x1="1" y1="1" x2="23" y2="23" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                    </svg>
                  </button>
                </template>
              </el-input>
            </el-form-item>

            <el-button
              type="primary" native-type="submit" :loading="loading"
              class="submit-btn" @click="handleLogin"
            >登录{{ currentRole.label }}端</el-button>
          </el-form>

          <p class="hint">演示账号：admin / student / teacher / hr，密码均为 admin123</p>
        </div>
      </div>

      <!-- 右侧：网站信息区域 -->
      <div class="intro-section">
        <div class="intro-content">
          <div class="decoration-line"></div>
          <div class="site-name-block">
            <h1 class="site-name">
              职业能力大数据<br>
              服务平台
            </h1>
          </div>
        </div>
        <p class="bottom-hint">登录以解锁职位推荐、能力画像与就业分析</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { useAppStore } from '@/store/app'
import { login, getTenants } from '@/api/auth'
import { toList } from '@/utils/list'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const appStore = useAppStore()
const loading = ref(false)
const formRef = ref(null)
const showPassword = ref(false)

/** 可选角色（仅作登录入口提示；实际角色由账号决定） */
const ROLES = [
  { value: 'STUDENT', label: '学生', placeholder: '学号 / 用户名' },
  { value: 'TEACHER', label: '教师', placeholder: '工号 / 用户名' },
  { value: 'HR', label: '企业 HR', placeholder: '用户名' },
  { value: 'ADMIN', label: '管理员', placeholder: '管理员用户名' }
]

const selectedRole = ref('STUDENT')
const currentRole = computed(() => ROLES.find(r => r.value === selectedRole.value))

const tenants = ref([])
const tenantsLoading = ref(false)

const form = reactive({
  tenantName: '',
  username: '',
  password: ''
})

const rules = {
  tenantName: [{ required: true, message: '请选择学校 / 企业', trigger: 'change' }],
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

/** 介绍区域的标题和特性 */
const titleWords = ['职业能力', '大数据', '服务平台']
const features = [
  { icon: '📊', title: '职位推荐', desc: '基于能力匹配的智能推荐系统' },
  { icon: '🎯', title: '能力画像', desc: '多维度展示个人职业竞争力' },
  { icon: '📈', title: '就业分析', desc: '实时掌握市场动态和趋势洞察' }
]

/** 角色 → 首页路由映射 */
const ROLE_HOME = { ADMIN: '/admin', STUDENT: '/student', TEACHER: '/teacher', HR: '/hr' }
const ROLE_LABEL = { ADMIN: '管理员', STUDENT: '学生', TEACHER: '教师', HR: '企业 HR' }

async function loadTenants() {
  tenantsLoading.value = true
  try {
    tenants.value = toList(await getTenants())
    if (tenants.value.length === 1) {
      form.tenantName = tenants.value[0].name
    }
  } catch {
    // 接口不可用时不阻塞登录
  } finally {
    tenantsLoading.value = false
  }
}

function handleThemeToggle() {
  appStore.toggleTheme()
}

async function handleLogin() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const data = await login({
      tenantName: form.tenantName,
      username: form.username,
      password: form.password
    })
    userStore.setLogin(data.token, {
      username: data.username,
      role: data.role,
      realName: data.realName,
      tenantName: data.tenantName
    })
    if (data.role !== selectedRole.value) {
      ElMessage.info(`该账号是${ROLE_LABEL[data.role] || data.role}账号，已为你进入${ROLE_LABEL[data.role] || ''}端`)
    } else {
      ElMessage.success(`欢迎回来，${data.realName || data.username}`)
    }
    router.push(ROLE_HOME[data.role] || '/admin')
  } catch {
    // 错误已在拦截器中提示
  } finally {
    loading.value = false
  }
}

onMounted(loadTenants)
</script>

<style scoped>
/* ========== 主容器：全屏背景 ========== */
.login-page {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100vh;
  /* 浅色模式：蓝紫渐变背景（蓝色比重更大） */
  background: linear-gradient(135deg, #f0f8ff 0%, #dce9ff 50%, #e8dff5 100%);
  display: flex;
  justify-content: center;
  align-items: center;
  overflow: hidden;
}

/* 深色模式：暗黑背景 */
html.dark .login-page {
  background: linear-gradient(135deg, #0f0a1f 0%, #1a0a3a 50%, #0d061a 100%);
}

/* ========== 极光背景层 ========== */
.aurora-background {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  overflow: hidden;
  z-index: 0;
  opacity: 0;
  /* 极光已删除 - 强化动态光圈效果 */
}

.aurora-blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(60px);
  opacity: 0;
  mix-blend-mode: screen;
  display: none;
}

.aurora-blob-1 {
  width: 600px;
  height: 600px;
  top: -150px;
  left: 5%;
  background: radial-gradient(circle, #8b5cf6 0%, transparent 70%);
  animation: aurora-float-1 12s ease-in-out infinite;
}

/* 浅色模式：亮紫 */
html:not(.dark) .aurora-blob-1 {
  background: radial-gradient(circle, #a78bfa 0%, transparent 70%);
}

/* 深色模式：深紫 */
html.dark .aurora-blob-1 {
  background: radial-gradient(circle, #6d28d9 0%, transparent 70%);
}

.aurora-blob-2 {
  width: 700px;
  height: 700px;
  top: 10%;
  right: -100px;
  background: radial-gradient(circle, #6366f1 0%, transparent 70%);
  animation: aurora-float-2 15s ease-in-out infinite;
  animation-delay: 1s;
}

/* 浅色模式：亮蓝 */
html:not(.dark) .aurora-blob-2 {
  background: radial-gradient(circle, #818cf8 0%, transparent 70%);
}

/* 深色模式：深蓝 */
html.dark .aurora-blob-2 {
  background: radial-gradient(circle, #4f46e5 0%, transparent 70%);
}

.aurora-blob-3 {
  width: 550px;
  height: 550px;
  bottom: 20%;
  left: 20%;
  background: radial-gradient(circle, #a78bfa 0%, transparent 70%);
  animation: aurora-float-3 14s ease-in-out infinite;
  animation-delay: 2s;
}

/* 浅色模式 */
html:not(.dark) .aurora-blob-3 {
  background: radial-gradient(circle, #c084fc 0%, transparent 70%);
}

/* 深色模式 */
html.dark .aurora-blob-3 {
  background: radial-gradient(circle, #5b21b6 0%, transparent 70%);
}

.aurora-blob-4 {
  width: 450px;
  height: 450px;
  bottom: -50px;
  right: 10%;
  background: radial-gradient(circle, #818cf8 0%, transparent 70%);
  animation: aurora-float-1 16s ease-in-out infinite;
  animation-delay: 3s;
}

/* 浅色模式：亮蓝紫 */
html:not(.dark) .aurora-blob-4 {
  background: radial-gradient(circle, #c084fc 0%, transparent 70%);
}

/* 深色模式：深蓝紫 */
html.dark .aurora-blob-4 {
  background: radial-gradient(circle, #4338ca 0%, transparent 70%);
}

@keyframes aurora-float-1 {
  0% {
    transform: translate(0, 0) scale(1);
  }
  50% {
    transform: translate(100px, -80px) scale(1.1);
  }
  100% {
    transform: translate(0, 0) scale(1);
  }
}

@keyframes aurora-float-2 {
  0% {
    transform: translate(0, 0) scale(1);
  }
  50% {
    transform: translate(-100px, 60px) scale(1.05);
  }
  100% {
    transform: translate(0, 0) scale(1);
  }
}

@keyframes aurora-float-3 {
  0% {
    transform: translateY(0) scale(1);
  }
  50% {
    transform: translateY(-100px) scale(1.1);
  }
  100% {
    transform: translateY(0) scale(1);
  }
}

/* ========== 主容器：左右布局 ========== */
.login-container {
  position: relative;
  z-index: 10;
  width: 100%;
  max-width: 1200px;
  height: 100vh;
  display: flex;
  gap: 60px;
  padding: 40px 60px;
  box-sizing: border-box;
}

/* ========== 左侧：玻璃态登录卡片 ========== */
.login-card-section {
  flex: 0 0 420px;
  display: flex;
  align-items: center;
  justify-content: flex-end;
}

.login-card {
  width: 100%;
  padding: 40px 32px;
  background: rgba(255, 255, 255, 0.12);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1.5px solid rgba(255, 255, 255, 0.25);
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2), inset 0 1px 0 rgba(255, 255, 255, 0.2);
  animation: cardSlideIn 0.8s cubic-bezier(0.16, 1, 0.3, 1);
}

/* 深色模式：增加不透明度 */
html.dark .login-card {
  background: rgba(30, 20, 60, 0.25);
  border-color: rgba(200, 150, 255, 0.3);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.4), inset 0 1px 0 rgba(150, 100, 255, 0.2);
}

.card-title {
  font-size: 28px;
  font-weight: 700;
  color: #ffffff;
  margin: 0 0 8px;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

/* 日光模式字体为黑色 */
html:not(.dark) .card-title {
  color: #000000;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

.card-subtitle {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
  margin: 0 0 24px;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.15);
}

/* 日光模式字体为黑色 */
html:not(.dark) .card-subtitle {
  color: rgba(0, 0, 0, 0.7);
  text-shadow: none;
}

/* ========== 角色选择选项卡 ========== */
.role-tabs {
  display: flex;
  gap: 8px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 10px;
  padding: 4px;
  margin-bottom: 24px;
}

.role-tab {
  flex: 1;
  padding: 10px;
  border: none;
  background: transparent;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.5);
  cursor: pointer;
  transition: all 0.15s ease;
}

.role-tab:hover {
  color: rgba(255, 255, 255, 0.7);
}

.role-tab.active {
  background: rgba(255, 255, 255, 0.1);
  color: #ffffff;
  font-weight: 600;
}

/* 日光模式角色选择 */
html:not(.dark) .role-tab {
  color: rgba(0, 0, 0, 0.5);
}

html:not(.dark) .role-tab:hover {
  color: rgba(0, 0, 0, 0.8);
}

html:not(.dark) .role-tab.active {
  background: rgba(0, 0, 0, 0.08);
  color: #000000;
}

/* ========== 表单元素 ========== */
.login-card :deep(.el-form-item) {
  margin-bottom: 16px;
}

.login-card :deep(.el-form-item__label) {
  color: rgba(255, 255, 255, 0.8);
  font-size: 12px;
  font-weight: 500;
  padding-bottom: 8px;
}

/* 日光模式表单标签 */
html:not(.dark) .login-card :deep(.el-form-item__label) {
  color: rgba(0, 0, 0, 0.8);
}

.login-card :deep(.el-input__wrapper) {
  background-color: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: none !important;
}

.login-card :deep(.el-input__wrapper:hover) {
  border-color: rgba(255, 255, 255, 0.2);
}

.login-card :deep(.el-input__wrapper.is-focus) {
  border-color: rgba(100, 180, 255, 0.6);
  background-color: rgba(255, 255, 255, 0.08);
  box-shadow: 0 0 0 3px rgba(100, 180, 255, 0.15) !important;
}

.login-card :deep(.el-input__inner) {
  color: #ffffff;
  font-size: 14px;
}

.login-card :deep(.el-input__inner::placeholder) {
  color: rgba(255, 255, 255, 0.3);
}

/* 日光模式输入框 */
html:not(.dark) .login-card :deep(.el-input__inner) {
  color: #000000;
}

html:not(.dark) .login-card :deep(.el-input__inner::placeholder) {
  color: rgba(0, 0, 0, 0.4);
}

html:not(.dark) .login-card :deep(.el-input__wrapper) {
  background-color: rgba(255, 255, 255, 0.8);
  border-color: rgba(0, 0, 0, 0.15);
}

html:not(.dark) .login-card :deep(.el-input__wrapper:hover) {
  border-color: rgba(0, 0, 0, 0.3);
}

html:not(.dark) .login-card :deep(.el-input__wrapper.is-focus) {
  border-color: #000000;
  background-color: rgba(255, 255, 255, 0.95);
}

.login-card :deep(.el-select__wrapper) {
  background-color: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: none !important;
}

.login-card :deep(.el-select__wrapper:hover) {
  border-color: rgba(255, 255, 255, 0.2);
}

.login-card :deep(.el-select__wrapper.is-focused) {
  border-color: rgba(100, 180, 255, 0.6);
  background-color: rgba(255, 255, 255, 0.08);
  box-shadow: 0 0 0 3px rgba(100, 180, 255, 0.15) !important;
}

/* 日光模式下拉框 */
html:not(.dark) .login-card :deep(.el-select__wrapper) {
  background-color: rgba(255, 255, 255, 0.8);
  border-color: rgba(0, 0, 0, 0.15);
}

html:not(.dark) .login-card :deep(.el-select__wrapper:hover) {
  border-color: rgba(0, 0, 0, 0.3);
}

html:not(.dark) .login-card :deep(.el-select__wrapper.is-focused) {
  border-color: #000000;
  background-color: rgba(255, 255, 255, 0.95);
}

/* ========== 登录按钮 ========== */
.submit-btn {
  width: 100%;
  margin-top: 8px;
}

.login-card :deep(.el-button--primary) {
  background: rgba(255, 255, 255, 0.9);
  border: none;
  color: #080810;
  font-weight: 600;
  transition: all 0.2s ease;
}

.login-card :deep(.el-button--primary:hover) {
  background: #ffffff;
  transform: translateY(-1px);
  box-shadow: 0 4px 16px rgba(255, 255, 255, 0.2);
}

/* ========== 提示文字 ========== */
.hint {
  text-align: center;
  color: rgba(255, 255, 255, 0.4);
  font-size: 11px;
  margin: 20px 0 0;
}

/* 日光模式字体为深灰色 */
html:not(.dark) .hint {
  color: rgba(0, 0, 0, 0.5);
}

/* ========== 右侧：网站信息区域 ========== */
.intro-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  justify-content: center;
  padding-left: 48px;
  min-width: 420px;
  position: relative;
}

.intro-content {
  display: flex;
  align-items: center;
  gap: 24px;
  animation: contentFadeIn 1s cubic-bezier(0.16, 1, 0.3, 1) 0.4s forwards;
  opacity: 0;
}

@keyframes contentFadeIn {
  from {
    opacity: 0;
    transform: translateX(-30px);
  }
  to {
    opacity: 1;
    transform: none;
  }
}

/* 装饰竖线 */
.decoration-line {
  width: 4px;
  height: 160px;
  background: linear-gradient(180deg, transparent 0%, #a78bfa 30%, #818cf8 70%, transparent 100%);
  border-radius: 2px;
  flex-shrink: 0;
}

html.dark .decoration-line {
  background: linear-gradient(180deg, transparent 0%, #a78bfa 30%, #818cf8 70%, transparent 100%);
}

/* 网站名文字块 */
.site-name-block {
  display: flex;
  align-items: center;
}

.site-name {
  font-size: 64px;
  font-weight: 700;
  font-family: 'Noto Serif SC', 'Georgia', 'Times New Roman', serif;
  color: #ffffff;
  line-height: 1.05;
  letter-spacing: 2px;
  margin: 0;
  white-space: pre-line;
}

html:not(.dark) .site-name {
  color: #000000;
}

/* 底部弱化提示 */
.bottom-hint {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.4);
  margin-top: 20px;
  margin-left: 0;
  line-height: 1.6;
  max-width: 480px;
}

html:not(.dark) .bottom-hint {
  color: rgba(0, 0, 0, 0.5);
}

/* ========== 响应式处理 ========== */
@media (max-width: 1200px) {
  .login-container {
    max-width: 100%;
    padding: 40px 40px;
    gap: 40px;
  }

  .site-name {
    font-size: 56px;
  }
}

@media (max-width: 1024px) {
  .login-container {
    flex-direction: column;
    justify-content: center;
    align-items: center;
    gap: 60px;
  }

  .login-card-section {
    flex: 0 0 auto;
    justify-content: center;
  }

  .intro-section {
    padding-left: 24px;
    align-items: flex-start;
  }

  .intro-content {
    gap: 20px;
  }

  .decoration-line {
    height: 140px;
  }

  .site-name {
    font-size: 48px;
  }
}

@media (max-width: 768px) {
  .intro-section {
    display: none;
  }

  .login-card-section {
    justify-content: center;
    width: 100%;
  }

  .login-card {
    max-width: 100%;
  }

  .site-name {
    font-size: 36px;
  }
}

@media (max-width: 768px) {
  .login-page {
    align-items: flex-start;
    padding-top: 40px;
  }

  .login-container {
    flex-direction: column;
    padding: 20px;
    gap: 40px;
    max-width: 100%;
    justify-content: center;
    align-items: center;
  }

  .login-card-section {
    flex: 0 0 auto;
    width: 100%;
    justify-content: center;
  }

  .login-card {
    max-width: 100%;
  }

  .intro-section {
    display: none;
  }

  .site-name {
    font-size: 36px;
  }

  .aurora-blob {
    filter: blur(50px);
    opacity: 0.5;
  }

  .aurora-blob-1 {
    width: 350px;
    height: 350px;
  }

  .aurora-blob-2 {
    width: 400px;
    height: 400px;
  }

  .aurora-blob-3 {
    width: 300px;
    height: 300px;
  }

  .aurora-blob-4 {
    width: 280px;
    height: 280px;
  }
}

@media (max-width: 640px) {
  .login-container {
    padding: 20px;
  }

  .login-card {
    padding: 32px 24px;
  }

  .card-title {
    font-size: 24px;
  }

  .site-name {
    font-size: 28px;
  }
}

/* ========== 星星闪烁背景 ========== */
.stars-container {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 0;
  overflow: hidden;
}

/* ========== 主题切换按钮（右上角） ========== */
.theme-toggle {
  position: fixed;
  top: 24px;
  right: 24px;
  z-index: 100;
  width: 44px;
  height: 44px;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.3);
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  color: #ffffff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
  padding: 0;
  box-sizing: border-box;
}

.theme-toggle:hover {
  background: rgba(255, 255, 255, 0.15);
  border-color: rgba(255, 255, 255, 0.5);
  transform: scale(1.05);
}

.theme-toggle:active {
  transform: scale(0.98);
}

.theme-icon {
  width: 20px;
  height: 20px;
  stroke: currentColor;
  fill: none;
  stroke-linecap: round;
  stroke-linejoin: round;
}

/* ========== 密码输入字段增强 ========== */
.password-input {
  position: relative;
}

.password-toggle {
  background: none;
  border: none;
  color: rgba(255, 255, 255, 0.5);
  cursor: pointer;
  padding: 4px 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: color 0.2s ease;
}

.password-toggle:hover {
  color: rgba(255, 255, 255, 0.8);
}

.eye-icon {
  width: 18px;
  height: 18px;
  stroke: currentColor;
  fill: none;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.login-card :deep(.el-input__suffix) {
  color: rgba(255, 255, 255, 0.5);
}

/* ========== 深色模式密码字段增强 ========== */
html.dark .password-toggle {
  color: rgba(200, 150, 255, 0.5);
}

html.dark .password-toggle:hover {
  color: rgba(200, 150, 255, 0.9);
}

/* ========== 禁用动画 ========== */
@media (prefers-reduced-motion: reduce) {
  .login-card,
  .word,
  .intro-description,
  .feature,
  .intro-footer,
  .aurora-blob {
    animation: none;
  }

  .role-tab,
  .login-card :deep(.el-input__wrapper),
  .login-card :deep(.el-select__wrapper),
  .login-card :deep(.el-button--primary) {
    transition: none;
  }
}
</style>
