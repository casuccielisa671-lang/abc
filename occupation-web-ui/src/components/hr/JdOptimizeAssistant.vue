<template>
  <div class="jd-assistant">
    <div class="actions">
      <el-button type="primary" plain @click="analyze">分析当前描述</el-button>
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
      <div class="chat-list">
        <div v-for="(msg, index) in messages" :key="index" class="msg" :class="msg.role">
          <span class="role">{{ msg.role === 'user' ? 'HR' : 'AI' }}</span>
          <p>{{ msg.content }}</p>
        </div>
      </div>
      <div class="chat-input">
        <el-input
          v-model="instruction"
          placeholder="告诉 AI 你的主要意思，如：突出成长空间，语气更吸引应届生"
          @keyup.enter="polish"
        />
        <el-button type="primary" :loading="polishing" @click="polish">润色</el-button>
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
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { analyzeJd } from '@/utils/jdOptimizer'

const props = defineProps({
  modelValue: { type: String, default: '' },
  title: { type: String, default: '' },
  skills: { type: String, default: '' }
})

const emit = defineEmits(['update:modelValue'])

const analysis = ref(null)
const instruction = ref('')
const draft = ref('')
const polishing = ref(false)
const messages = ref([
  { role: 'assistant', content: '你可以先点“分析当前描述”，也可以直接告诉我想强化什么卖点，我会生成一版更适合发布的 JD。' }
])

watch(() => props.modelValue, () => {
  analysis.value = null
  draft.value = ''
})

function analyze() {
  const result = analyzeJd(props.modelValue)
  if (!result) {
    ElMessage.warning('请先填写职位描述')
    return
  }
  analysis.value = result
}

function polish() {
  const input = instruction.value.trim()
  if (!props.modelValue?.trim() && !input) {
    ElMessage.warning('请先填写职位描述或输入润色要求')
    return
  }
  polishing.value = true
  messages.value.push({ role: 'user', content: input || '请基于当前内容润色一版 JD' })
  draft.value = buildPolishedJd(input)
  messages.value.push({ role: 'assistant', content: '我已生成一版更清晰、更像正式招聘发布的 JD。你可以继续提要求，或直接应用到职位描述。' })
  instruction.value = ''
  polishing.value = false
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

function buildPolishedJd(input) {
  const title = props.title || '该岗位'
  const skillText = props.skills || '相关专业技能'
  const source = props.modelValue || input
  return [
    `${title}`,
    '',
    '我们希望找到一位对业务有好奇心、愿意把技术能力落到真实场景中的伙伴。你将参与核心业务模块建设，在清晰的目标和团队支持下持续成长。',
    '',
    '岗位职责：',
    `1. 围绕 ${title} 的业务目标，参与需求理解、方案设计、开发实现与持续优化。`,
    '2. 与产品、设计、测试及业务同学协作，保障功能按时、高质量交付。',
    '3. 关注系统稳定性、用户体验与数据反馈，持续推动流程和产品改进。',
    '',
    '任职要求：',
    `1. 熟悉 ${skillText}，具备良好的学习能力和问题拆解能力。`,
    '2. 有项目实践、实习经历或作品沉淀者优先。',
    '3. 沟通主动，责任心强，能够在团队协作中稳定推进任务。',
    '',
    '你将获得：',
    '1. 清晰的培养路径、真实业务项目和可见的成长反馈。',
    '2. 开放协作的团队氛围，以及对优秀表现的及时认可。',
    input ? `\nHR 特别强调：${input}` : '',
    source && !input ? `\n原始要点已保留：${source}` : ''
  ].filter(Boolean).join('\n')
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
