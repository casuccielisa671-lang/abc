<template>
  <div class="login-page">
    <LoginBackground />
    <AppThemeToggle class="login-theme-toggle" />

    <main class="login-container">
      <LoginIntro />

      <div class="login-card-section">
        <LoginFormCard
          v-model:selected-role="selectedRole"
          :roles="LOGIN_ROLES"
          :current-role="currentRole"
          :form="form"
          :rules="rules"
          :tenants="tenants"
          :tenants-loading="tenantsLoading"
          :loading="loading"
          @submit="handleLogin"
        />
      </div>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AppThemeToggle from '@/components/common/AppThemeToggle.vue'
import LoginBackground from '@/components/login/LoginBackground.vue'
import LoginFormCard from '@/components/login/LoginFormCard.vue'
import LoginIntro from '@/components/login/LoginIntro.vue'
import { getTenants, login } from '@/api/auth'
import { LOGIN_ROLES, getRoleHome, getRoleLabel } from '@/config/roles'
import { useUserStore } from '@/store/user'
import { toList } from '@/utils/list'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const tenants = ref([])
const tenantsLoading = ref(false)
const selectedRole = ref('STUDENT')

const currentRole = computed(() =>
  LOGIN_ROLES.find(role => role.value === selectedRole.value) || LOGIN_ROLES[0]
)

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

async function loadTenants() {
  tenantsLoading.value = true
  try {
    tenants.value = toList(await getTenants())
    if (tenants.value.length === 1) {
      form.tenantName = tenants.value[0].name
    }
  } catch {
    // 租户列表仅用于登录辅助，接口暂不可用时不阻塞账号密码登录。
  } finally {
    tenantsLoading.value = false
  }
}

async function handleLogin() {
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

    const actualRoleLabel = getRoleLabel(data.role)
    if (data.role !== selectedRole.value) {
      ElMessage.info(`该账号是${actualRoleLabel}账号，已为你进入${actualRoleLabel}端`)
    } else {
      ElMessage.success(`欢迎回来，${data.realName || data.username}`)
    }

    router.push(getRoleHome(data.role))
  } catch {
    // 请求错误已由 axios 拦截器统一提示。
  } finally {
    loading.value = false
  }
}

onMounted(loadTenants)
</script>

<style scoped>
.login-page {
  position: fixed;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  background: #f8fafc;
}

html.dark .login-page {
  background: #0f172a;
}

.login-theme-toggle {
  position: fixed;
  top: 24px;
  right: 24px;
  z-index: 20;
}

.login-container {
  position: relative;
  z-index: 10;
  width: min(1200px, 100%);
  height: 100vh;
  display: flex;
  gap: 60px;
  padding: 40px 60px;
}

.login-card-section {
  flex: 0 0 420px;
  display: flex;
  align-items: center;
  justify-content: flex-end;
}

@media (max-width: 1200px) {
  .login-container {
    gap: 40px;
    padding: 40px;
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
}

@media (max-width: 768px) {
  .login-page {
    align-items: flex-start;
    padding-top: 40px;
  }

  .login-container {
    padding: 20px;
  }

  .login-card-section {
    width: 100%;
  }
}
</style>
