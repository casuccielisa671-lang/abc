<template>
  <div class="jd-assistant">
    <div class="actions">
      <el-button type="primary" plain :loading="analyzing" @click="analyze">AI 分析当前描述</el-button>
      <el-button plain :disabled="!draft" @click="applyDraft">应用润色稿</el-button>
    </div>

    <div v-if="analysis" class="result">
      <div class="score">
        <span>综合评分</span>
        <strong :style="{ color: scoreColor(analysis.score) }">{{ analysis.score }}</strong>
        <span>/ 100</span>
      </div>
      <div class="dims">
        <span v-for="item in analysis.dimensions" :key="item.name">{{ item.name }} {{ item.score }}</span>
      </div>
      <ul class="suggestions">
        <li v-for="(item, index) in analysis.suggestions" :key="index">{{ item }}</li>
      </ul>
    </div>

    <div class="chat-box">
      <div class="chat-list" ref="chatList">
        <div v-for="(msg, index) in messages" :key="index" class="msg" :class="msg.role">
          <span class="role">{{ msg.role === 'user' ? 'HR' : 'AI' }}</span>
          <p v-if="msg.role === 'assistant'" style="white-space: pre-wrap;">{{ msg.content }}</p>
          <p v-else>{{ msg.content }}</p>
        </div>
      </div>
      <div class="chat-input">
        <el-input
          v-model="instruction"
          placeholder="告诉 AI 你的优化要求，如：突出成长空间、语气更吸引应届生"
          :disabled="optimizing"
          @keyup.enter="optimize"
        />
        <el-button type="primary" :loading="optimizing" @click="optimize">优化</el-button>
      </div>
      <el-input
        v-if="draft"
        v-model="draft"
        type="textarea"
        :rows="6"
        placeholder="AI 润色稿"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, watch, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { aiAnalyzeJd, aiOptimizeJd } from '@/api/student'

const props = defineProps({
  modelValue: { type: String, default: '' },
  title: { type: String, default: '' },
  skills: { type: String, default: '' }
})

const emit = defineEmits(['update:modelValue'])

const analysis = ref(null)
const instruction = ref('')
const draft = ref('')
const analyzing = ref(false)
const optimizing = ref(false)
const chatList = ref(null)
const messages = ref([
  { role: 'assistant', content: '你可以先点"AI 分析当前描述"查看 JD 质量评分，也可以直接告诉我优化要求，我会帮你生成更专业的 JD。' }
])

watch(() => props.modelValue, () => {
  analysis.value = null
  draft.value = ''
})

async function analyze() {
  if (!props.modelValue?.trim()) {
    ElMessage.warning('请先填写职位描述')
    return
  }
  analyzing.value = true
  try {
    const res = await aiAnalyzeJd(props.modelValue)
    analysis.value = res.data
    messages.value.push({
      role: 'assistant',
      content: `分析完成！综合评分 ${res.data.score}/100。${res.data.suggestions?.length ? '建议：' + res.data.suggestions.join('；') : ''}`
    })
  } catch {
    ElMessage.error('AI 分析失败，请稍后重试')
  } finally {
    analyzing.value = false
  }
}

async function optimize() {
  const input = instruction.value.trim()
  if (!props.modelValue?.trim() && !input) {
    ElMessage.warning('请先填写职位描述或输入优化要求')
    return
  }
  optimizing.value = true
  const userMsg = input || '请基于当前内容优化一版 JD'
  messages.value.push({ role: 'user', content: userMsg })
  instruction.value = ''

  // 构建对话历史（不含 system prompt）
  const history = messages.value
    .filter(m => m.role === 'user' || m.role === 'assistant')
    .slice(0, -1) // 排除刚加的这条 user 消息，后端会自己处理
    .map(m => ({ role: m.role, content: m.content }))

  try {
    const res = await aiOptimizeJd(props.modelValue, history)
    draft.value = res.data
    messages.value.push({
      role: 'assistant',
      content: '已生成优化版 JD，你可以查看下方润色稿，满意后点击"应用润色稿"替换原文。也可以继续提要求。'
    })
    await nextTick()
    scrollToBottom()
  } catch {
    ElMessage.error('AI 优化失败，请稍后重试')
    messages.value.push({ role: 'assistant', content: '抱歉，优化请求失败，请稍后重试。' })
  } finally {
    optimizing.value = false
  }
}

function applyDraft() {
  emit('update:modelValue', draft.value)
  ElMessage.success('已应用润色稿')
}

function scoreColor(score) {
  if (score >= 80) return '#27ae60'
  if (score >= 60) return '#f39c12'
  return '#e74c3c'
}

function scrollToBottom() {
  if (chatList.value) {
    chatList.value.scrollTop = chatList.value.scrollHeight
  }
}
</script>

<style scoped>
.jd-assistant { width: 100%; display: flex; flex-direction: column; gap: 12px; }
.actions { display: flex; gap: 8px; flex-wrap: wrap; }
.result { border: 1px solid #e8eaed; border-radius: 10px; padding: 12px; background: #fafafa; }
html.dark .result { background: #1e1f22; border-color: #2e3035; }
.score { display: flex; align-items: baseline; gap: 6px; margin-bottom: 8px; color: var(--app-ink-2); }
.score strong { font-size: 24px; }
.dims { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 8px; }
.dims span { font-size: 12px; color: var(--app-ink-3); background: #f1f3f4; border-radius: 999px; padding: 2px 8px; }
html.dark .dims span { background: #2e3035; }
.suggestions { margin: 0; padding-left: 18px; color: var(--app-ink-2); line-height: 1.7; }
.chat-box { display: flex; flex-direction: column; gap: 10px; }
.chat-list { display: flex; flex-direction: column; gap: 8px; max-height: 170px; overflow-y: auto; }
.msg { border-radius: 10px; padding: 10px 12px; background: var(--color-bg-secondary); }
.msg.user { align-self: flex-end; max-width: 86%; background: var(--color-primary-light-9); }
.msg.assistant { align-self: flex-start; max-width: 92%; }
.role { display: block; font-size: 12px; font-weight: 700; color: var(--app-ink-3); margin-bottom: 4px; }
.msg p { margin: 0; line-height: 1.6; color: var(--app-ink-2); }
.chat-input { display: flex; gap: 8px; }
.chat-input .el-button { flex-shrink: 0; }
</style>
