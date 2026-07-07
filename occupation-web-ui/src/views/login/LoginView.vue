<template>
  <div class="login-container">
    <el-card class="login-card">
      <h2>职业能力大数据服务平台</h2>
      <el-form @submit.prevent="handleLogin">
        <el-form-item>
          <el-input v-model="form.username" placeholder="用户名" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" type="password" placeholder="密码" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" native-type="submit" :loading="loading" style="width:100%">
            登录
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
// TODO: import { login } from '@/api/auth'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const form = reactive({ username: '', password: '' })

function handleLogin() {
  loading.value = true
  // TODO: 调用 POST /api/auth/login，成功后 setLogin + 按角色跳转
  userStore.setLogin('mock_token', { username: form.username, role: 'ADMIN', tenantId: 1, realName: '管理员' })
  loading.value = false
  router.push('/admin')
}
</script>

<style scoped>
.login-container {
  display: flex; justify-content: center; align-items: center; height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
.login-card { width: 400px; }
h2 { text-align: center; margin-bottom: 24px; color: #303133; }
</style>
