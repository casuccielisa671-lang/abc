<template>
  <div class="skill-tags">
    <span v-for="(tag, i) in list" :key="tag + i" class="chip closable">
      {{ tag }}
      <el-icon class="close" @click="remove(i)"><Close /></el-icon>
    </span>

    <el-input
      v-if="inputVisible"
      ref="inputRef"
      v-model="draft"
      size="small"
      class="tag-input"
      @keyup.enter="confirm"
      @blur="confirm"
    />
    <el-button v-else size="small" text class="add-btn" @click="showInput">+ 添加</el-button>

    <span v-if="!list.length && !inputVisible" class="ph">{{ placeholder }}</span>
  </div>
</template>

<script setup>
/**
 * 标签数组输入框 —— 项目技术栈、获奖证书共用。
 *
 * v-model 绑的是字符串数组，父组件直接把它交给后端；
 * 不做 JSON.stringify —— 简历接口收发的都是结构化数组。
 */
import { ref, computed, nextTick } from 'vue'
import { Close } from '@element-plus/icons-vue'

const props = defineProps({
  modelValue: { type: Array, default: () => [] },
  placeholder: { type: String, default: '回车添加' }
})
const emit = defineEmits(['update:modelValue'])

const list = computed(() => props.modelValue || [])
const inputVisible = ref(false)
const draft = ref('')
const inputRef = ref(null)

async function showInput() {
  inputVisible.value = true
  await nextTick()
  inputRef.value?.focus()
}

function confirm() {
  const value = draft.value.trim()
  // 重复标签直接忽略，不弹提示 —— 用户意图很明确，报错反而烦
  if (value && !list.value.includes(value)) {
    emit('update:modelValue', [...list.value, value])
  }
  draft.value = ''
  inputVisible.value = false
}

function remove(i) {
  const next = [...list.value]
  next.splice(i, 1)
  emit('update:modelValue', next)
}
</script>

<style scoped>
.skill-tags { display: flex; flex-wrap: wrap; align-items: center; gap: 6px; }

.chip.closable { display: inline-flex; align-items: center; gap: 4px; padding-right: 6px; }
.close { cursor: pointer; font-size: 12px; opacity: 0.55; }
.close:hover { opacity: 1; color: var(--app-danger); }

.tag-input { width: 130px; }
.add-btn { padding: 0 6px; height: 24px; }
.ph { font-size: 12px; color: var(--app-ink-3); }
</style>
