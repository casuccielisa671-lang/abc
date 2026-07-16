<template>
  <section class="login-card" aria-label="登录表单">
    <h2 class="card-title">登录</h2>
    <p class="card-subtitle">选择身份进入平台</p>

    <div class="role-tabs" role="tablist" aria-label="登录身份">
      <button
        v-for="role in roles"
        :key="role.value"
        type="button"
        class="role-tab"
        role="tab"
        :aria-selected="selectedRole === role.value"
        :class="{ active: selectedRole === role.value }"
        @click="$emit('update:selectedRole', role.value)"
      >
        {{ role.label }}
      </button>
    </div>

    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="submit">
      <el-form-item prop="tenantName" label="学校 / 企业节点">
        <el-select
          v-model="form.tenantName"
          filterable
          :loading="tenantsLoading"
          placeholder="搜索或选择学校 / 企业"
          class="tenant-select"
        >
          <el-option v-for="tenant in tenants" :key="tenant.id" :label="tenant.name" :value="tenant.name" />
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
        >
          <template #suffix>
            <button type="button" class="password-toggle" @click="showPassword = !showPassword">
              <el-icon>
                <Hide v-if="showPassword" />
                <View v-else />
              </el-icon>
            </button>
          </template>
        </el-input>
      </el-form-item>

      <el-button type="primary" native-type="submit" :loading="loading" class="submit-btn">
        登录{{ currentRole.label }}端
      </el-button>
    </el-form>

    <p class="hint">演示账号：admin / student / teacher / hr，密码均为 admin123</p>
  </section>
</template>

<script setup>
import { ref } from 'vue'
import { Hide, View } from '@element-plus/icons-vue'

defineProps({
  roles: { type: Array, required: true },
  selectedRole: { type: String, required: true },
  currentRole: { type: Object, required: true },
  form: { type: Object, required: true },
  rules: { type: Object, required: true },
  tenants: { type: Array, required: true },
  tenantsLoading: { type: Boolean, default: false },
  loading: { type: Boolean, default: false }
})

const emit = defineEmits(['update:selectedRole', 'submit'])
const formRef = ref(null)
const showPassword = ref(false)

async function submit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (valid) emit('submit')
}
</script>

<style scoped>
.login-card {
  width: 100%;
  padding: 40px 32px;
  border: 1px solid rgba(255, 255, 255, 0.24);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.15);
  box-shadow: 0 24px 70px rgba(15, 23, 42, 0.18);
  backdrop-filter: blur(18px);
  animation: cardSlideIn 0.72s cubic-bezier(0.16, 1, 0.3, 1);
}

html.dark .login-card {
  border-color: rgba(148, 163, 184, 0.26);
  background: rgba(15, 23, 42, 0.56);
  box-shadow: 0 24px 80px rgba(0, 0, 0, 0.34);
}

.card-title {
  margin: 0 0 8px;
  color: var(--color-text-primary);
  font-size: 28px;
  font-weight: 700;
}

.card-subtitle,
.hint {
  color: var(--color-text-secondary);
}

.card-subtitle {
  margin: 0 0 24px;
  font-size: 14px;
}

.role-tabs {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 6px;
  padding: 4px;
  margin-bottom: 24px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.18);
}

html.dark .role-tabs {
  background: rgba(148, 163, 184, 0.12);
}

.role-tab {
  min-height: 36px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: var(--color-text-secondary);
  cursor: pointer;
  font-size: 12px;
  font-weight: 600;
  transition: background var(--transition-fast), color var(--transition-fast);
}

.role-tab:hover,
.role-tab.active {
  background: var(--color-surface);
  color: var(--color-text-primary);
}

.tenant-select,
.submit-btn {
  width: 100%;
}

.login-card :deep(.el-form-item) {
  margin-bottom: 16px;
}

.login-card :deep(.el-form-item__label) {
  color: var(--color-text-secondary);
  font-size: 12px;
  font-weight: 600;
  padding-bottom: 8px;
}

.login-card :deep(.el-input__wrapper),
.login-card :deep(.el-select__wrapper) {
  min-height: 40px;
  border: 1px solid var(--color-border-light);
  background: color-mix(in srgb, var(--color-surface) 82%, transparent);
  box-shadow: none;
}

.login-card :deep(.el-input__wrapper:hover),
.login-card :deep(.el-select__wrapper:hover),
.login-card :deep(.el-input__wrapper.is-focus),
.login-card :deep(.el-select__wrapper.is-focused) {
  border-color: var(--color-primary);
  box-shadow: var(--shadow-focus);
}

.submit-btn {
  margin-top: 8px;
  font-weight: 700;
}

.password-toggle {
  border: 0;
  background: transparent;
  color: var(--color-text-tertiary);
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 4px;
}

.password-toggle:hover {
  color: var(--color-text-primary);
}

.hint {
  margin: 20px 0 0;
  text-align: center;
  font-size: 11px;
}

@keyframes cardSlideIn {
  from {
    opacity: 0;
    transform: translateY(18px) scale(0.98);
  }
  to {
    opacity: 1;
    transform: none;
  }
}

@media (max-width: 640px) {
  .login-card {
    padding: 32px 24px;
  }

  .role-tabs {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (prefers-reduced-motion: reduce) {
  .login-card,
  .role-tab,
  .login-card :deep(.el-input__wrapper),
  .login-card :deep(.el-select__wrapper) {
    animation: none;
    transition: none;
  }
}
</style>
