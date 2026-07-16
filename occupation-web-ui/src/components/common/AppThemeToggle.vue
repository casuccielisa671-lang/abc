<template>
  <button class="theme-toggle-button" type="button" :title="title" @click="appStore.toggleTheme()">
    <el-icon>
      <Moon v-if="appStore.dark" />
      <Sunny v-else />
    </el-icon>
  </button>
</template>

<script setup>
import { computed } from 'vue'
import { Moon, Sunny } from '@element-plus/icons-vue'
import { useAppStore } from '@/store/app'

const appStore = useAppStore()
const title = computed(() => (appStore.dark ? '切换到日光模式' : '切换到夜光模式'))
</script>

<style scoped>
.theme-toggle-button {
  width: 40px;
  height: 40px;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  background: color-mix(in srgb, var(--color-surface) 72%, transparent);
  color: var(--color-text-secondary);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: background var(--transition-fast), color var(--transition-fast), transform var(--transition-fast);
}

.theme-toggle-button:hover {
  background: var(--color-surface);
  color: var(--color-text-primary);
  transform: translateY(-1px);
}

.theme-toggle-button :deep(.el-icon) {
  font-size: 18px;
}
</style>
