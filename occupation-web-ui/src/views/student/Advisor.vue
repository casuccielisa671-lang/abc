<template>
  <div class="advisor-page">
    <div class="page-head">
      <h2 class="page-title">AI 职业顾问</h2>
      <p class="page-sub">结合你的画像与本平台的真实岗位数据回答，不做凭空推测</p>
    </div>

    <el-card class="chat-card">
      <div ref="scrollRef" class="messages">
        <!-- 开场白 + 快捷问题 -->
        <div v-if="!messages.length" class="welcome">
          <p class="welcome-title">你好，我是你的 AI 职业顾问</p>
          <p class="welcome-sub">
            我能看到你的专业、技能、求职意向，以及平台上 {{ '' }}真实的岗位分布与技能热度。
            问得越具体，答得越有用。
          </p>
          <div class="presets">
            <span v-for="q in PRESETS" :key="q" class="chip preset" @click="send(q)">{{ q }}</span>
          </div>
        </div>

        <div v-for="(m, i) in messages" :key="i" class="msg" :class="m.role">
          <div class="bubble">
            <p v-for="(line, j) in m.content.split('\n')" :key="j" class="line">{{ line }}</p>
            <span v-if="m.role === 'assistant' && m.aiGenerated === false" class="degraded">
              AI 未启用，这是规则化回复
            </span>
          </div>
        </div>

        <div v-if="thinking" class="msg assistant">
          <div class="bubble thinking">顾问正在思考…</div>
        </div>
      </div>

      <div class="composer">
        <el-input
          v-model="draft"
          type="textarea"
          :rows="2"
          resize="none"
          maxlength="2000"
          placeholder="问点具体的，比如：我想去杭州做后端，还差哪些技能？（Enter 发送，Shift+Enter 换行）"
          @keydown.enter.exact.prevent="send()"
        />
        <div class="composer-actions">
          <el-button text size="small" :disabled="!messages.length || thinking" @click="reset">清空对话</el-button>
          <el-button type="primary" :loading="thinking" @click="send()">发送</el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import { advisorChat } from '@/api/student'
import { ElMessage } from 'element-plus'

const PRESETS = [
  '我该补哪些技能才能进大厂做后端？',
  '我的画像适合投哪些城市的岗位？',
  '简历里的项目经历怎么写才有说服力？',
  '现在市场上最热门的技能是什么？'
]

/** 每条：{ role: 'user'|'assistant', content, aiGenerated? } */
const messages = ref([])
const draft = ref('')
const thinking = ref(false)
const scrollRef = ref(null)

async function scrollToBottom() {
  await nextTick()
  const el = scrollRef.value
  if (el) el.scrollTop = el.scrollHeight
}

async function send(preset) {
  const text = (preset ?? draft.value).trim()
  if (!text || thinking.value) return

  messages.value.push({ role: 'user', content: text })
  draft.value = ''
  thinking.value = true
  await scrollToBottom()

  try {
    // 服务端无状态：每次把完整历史传回去。只传 role/content，
    // 服务端会丢弃非 user/assistant 的角色（防 prompt 注入）
    const history = messages.value.map(m => ({ role: m.role, content: m.content }))
    const { reply, aiGenerated } = await advisorChat(history)
    messages.value.push({ role: 'assistant', content: reply, aiGenerated })
  } catch {
    // 失败时把刚发出的那条收回来，避免历史里留下一条没有回复的用户消息
    messages.value.pop()
    ElMessage.error('顾问暂时无法回复，请稍后再试')
  } finally {
    thinking.value = false
    await scrollToBottom()
  }
}

function reset() {
  messages.value = []
}
</script>

<style scoped>
.chat-card { display: flex; flex-direction: column; }
.chat-card :deep(.el-card__body) { display: flex; flex-direction: column; height: calc(100vh - 230px); }

.messages { flex: 1; overflow-y: auto; padding-right: 6px; }

.welcome { padding: 32px 8px; }
.welcome-title { font-size: 17px; font-weight: 600; color: var(--app-ink); margin: 0; }
.welcome-sub { font-size: 13px; color: var(--app-ink-3); margin: 6px 0 20px; line-height: 1.7; }
.presets { display: flex; flex-wrap: wrap; gap: 8px; }
.chip.preset { cursor: pointer; }
.chip.preset:hover { color: var(--app-ink); box-shadow: var(--app-hairline); }

.msg { display: flex; margin-bottom: 14px; }
.msg.user { justify-content: flex-end; }

.bubble {
  max-width: 76%;
  padding: 10px 14px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.75;
}
.msg.assistant .bubble { box-shadow: var(--app-hairline); color: var(--app-ink); }
.msg.user .bubble { background: var(--app-action); color: var(--app-action-ink); }

.line { margin: 0; }
.line:not(:last-child) { margin-bottom: 6px; }

.thinking { color: var(--app-ink-3); }
.degraded {
  display: block;
  margin-top: 8px;
  font-size: 11px;
  color: var(--app-ember);
}

.composer { padding-top: 12px; border-top: 1px solid var(--app-stone); }
.composer-actions { display: flex; justify-content: flex-end; gap: 8px; margin-top: 8px; }
</style>
