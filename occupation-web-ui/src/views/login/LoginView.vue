<template>
  <div class="login-page">
    <div class="login-card">
      <div class="brand">
        <div class="brand-mark">职</div>
        <h1 class="brand-title">职业能力大数据服务平台</h1>
        <p class="brand-sub">职位推荐 · 能力画像 · 就业分析</p>
      </div>

      <!-- 角色选择：仅作登录入口提示，实际进入哪个端由账号自身的 role 决定 -->
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
          <!-- 下拉联想：从后端拉取启用中的租户，杜绝手输错别字导致「租户不存在」 -->
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
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>

        <el-button
          type="primary" native-type="submit" :loading="loading"
          class="submit-btn" @click="handleLogin"
        >登录{{ currentRole.label }}端</el-button>
      </el-form>

      <p class="hint">演示账号：admin / student / teacher / hr，密码均为 admin123</p>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { login, getTenants } from '@/api/auth'
import { toList } from '@/utils/list'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const formRef = ref(null)

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

/** 角色 → 首页路由映射 */
const ROLE_HOME = { ADMIN: '/admin', STUDENT: '/student', TEACHER: '/teacher', HR: '/hr' }
const ROLE_LABEL = { ADMIN: '管理员', STUDENT: '学生', TEACHER: '教师', HR: '企业 HR' }

async function loadTenants() {
  tenantsLoading.value = true
  try {
    tenants.value = toList(await getTenants())
    // 只有一个租户时直接选中，省掉一次点击
    if (tenants.value.length === 1) {
      form.tenantName = tenants.value[0].name
    }
  } catch {
    // 接口不可用时不阻塞登录：错误已由拦截器提示
  } finally {
    tenantsLoading.value = false
  }
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
    // 账号实际角色与所选入口不一致时给出提示，并按实际角色进入
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
/* 登录页固定浅色：奶油画布，不随深色模式翻转（品牌第一印象保持一致） */
.login-page {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  padding: 24px;
  background: #fbfaf9;
}

/* 内描边而非投影，与全站卡片保持同一手法 */
.login-card {
  width: 420px;
  max-width: 100%;
  background: #ffffff;
  border-radius: 10px;
  box-shadow: inset 0 0 0 1px #f2f0ed;
  padding: 40px 32px 28px;
  animation: rise .4s cubic-bezier(0.16, 1, 0.3, 1);
}
@keyframes rise {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: none; }
}

.brand { text-align: center; margin-bottom: 28px; }
.brand-mark {
  width: 52px; height: 52px; border-radius: 14px;
  margin: 0 auto 16px;
  background: #121212; color: #fbfaf9;
  display: flex; align-items: center; justify-content: center;
  font-size: 24px; font-weight: 600;
}
.brand-title {
  font-size: 23px; line-height: 1.1; letter-spacing: -0.019em;
  font-weight: 600; color: #343433; margin: 0 0 6px;
}
.brand-sub { font-size: 14px; color: #6f6e6c; margin: 0; }

/* iOS 分段控制器：沙色槽 + 白色滑块 */
.role-tabs {
  display: flex; gap: 4px;
  background: #f6f4ef;
  border-radius: 10px;
  padding: 4px;
  margin-bottom: 24px;
}
.role-tab {
  flex: 1;
  border: none;
  background: transparent;
  padding: 8px 0;
  border-radius: 8px;
  font: inherit;
  font-size: 13px;
  font-weight: 500;
  color: #6f6e6c;
  cursor: pointer;
  transition: background .15s, color .15s;
}
.role-tab:hover { color: #343433; }
.role-tab.active {
  background: #ffffff;
  color: #121212;
  font-weight: 600;
  box-shadow: inset 0 0 0 1px #f2f0ed;
}

.submit-btn { width: 100%; margin-top: 8px; }

.hint { text-align: center; color: #6f6e6c; font-size: 12px; margin: 20px 0 0; }

/* 登录页锁定浅色，覆盖深色模式下 Element 的表单令牌 */
.login-card :deep(.el-form-item__label) { color: #474645; font-size: 13px; font-weight: 500; padding-bottom: 6px; }
.login-card :deep(.el-input__wrapper),
.login-card :deep(.el-select__wrapper) {
  background-color: #f6f4ef;
  box-shadow: inset 0 0 0 1px #e5e1da !important;
}
.login-card :deep(.el-input__wrapper.is-focus),
.login-card :deep(.el-select__wrapper.is-focused) {
  background-color: #ffffff;
  box-shadow: inset 0 0 0 1px #121212 !important;
}
.login-card :deep(.el-input__inner) { color: #343433; }
</style>
