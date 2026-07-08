<template>
  <div class="login-container">
    <el-card class="login-card">
      <h2>职业能力大数据服务平台</h2>
      <el-form ref="formRef" :model="form" :rules="rules" @submit.prevent="handleLogin">
        <el-form-item prop="tenantName">
          <el-input v-model="form.tenantName" placeholder="租户名称（如：测试学院）" />
        </el-form-item>
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" native-type="submit" :loading="loading" style="width:100%">
            登录
          </el-button>
        </el-form-item>
      </el-form>
      <p class="hint">种子账号：admin / admin123（租户：测试学院）</p>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { login } from '@/api/auth'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const formRef = ref(null)

const form = reactive({
  tenantName: '测试学院',
  username: '',
  password: ''
})

const rules = {
  tenantName: [{ required: true, message: '请输入租户名称', trigger: 'blur' }],
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

/** 角色 → 首页路由映射 */
const ROLE_HOME = {
  ADMIN: '/admin',
  STUDENT: '/student',
  TEACHER: '/teacher',
  HR: '/hr'
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
    ElMessage.success(`欢迎回来，${data.realName || data.username}`)
    router.push(ROLE_HOME[data.role] || '/admin')
  } catch {
    // 错误已在拦截器中提示
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  display: flex; justify-content: center; align-items: center; height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
.login-card { width: 420px; }
h2 { text-align: center; margin-bottom: 24px; color: #303133; }
.hint { text-align: center; color: #909399; font-size: 12px; margin-top: 8px; }
</style>
