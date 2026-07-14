<template>
  <el-popover
    placement="bottom-end"
    :width="340"
    trigger="click"
    popper-class="bell-popover"
    @show="loadRecent"
  >
    <template #reference>
      <button class="bell-btn" :title="'消息' + (unread ? `（${unread} 条未读）` : '')">
        <el-badge :value="unread" :hidden="!unread" :max="99">
          <el-icon :size="18"><Bell /></el-icon>
        </el-badge>
      </button>
    </template>

    <div class="bell-panel">
      <div class="bp-head">
        <span class="bp-title">消息</span>
        <span v-if="unread" class="bp-allread" @click="readAll">全部已读</span>
      </div>

      <div v-loading="loading" class="bp-list">
        <div
          v-for="m in recent"
          :key="m.id"
          class="bp-item"
          :class="{ unread: !m.isRead }"
          @click="open(m)"
        >
          <span class="dot" :class="tone(m.type)" />
          <div class="bp-main">
            <div class="bp-row">
              <span class="bp-badge" :class="tone(m.type)">{{ label(m.type) }}</span>
              <span class="bp-mtitle">{{ m.title }}</span>
            </div>
            <div class="bp-content">{{ m.content }}</div>
            <div class="bp-time">{{ formatTime(m.createTime) }}</div>
          </div>
        </div>
        <el-empty v-if="!loading && !recent.length" description="暂无消息" :image-size="48" />
      </div>

      <div class="bp-foot" @click="goAll">查看全部消息 ›</div>
    </div>
  </el-popover>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { Bell } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import { useMessageStore } from '@/store/message'
import { getMyMessages, markMessageRead, markAllMessagesRead } from '@/api/push'
import { toList } from '@/utils/list'
import { formatTime } from '@/utils/format'
import { messageMeta, messageTarget } from '@/utils/message'

const router = useRouter()
const userStore = useUserStore()
const messageStore = useMessageStore()
const { unread } = storeToRefs(messageStore)

const recent = ref([])
const loading = ref(false)

const rolePrefix = computed(() =>
  ({ STUDENT: 'student', TEACHER: 'teacher', HR: 'hr', ADMIN: 'admin' }[userStore.role] || 'student')
)

function label(t) { return messageMeta(t).label }
function tone(t) { return messageMeta(t).tone }

async function loadRecent() {
  loading.value = true
  try {
    recent.value = toList(await getMyMessages({ pageNum: 1, pageSize: 8 }))
  } finally {
    loading.value = false
  }
}

async function open(m) {
  if (!m.isRead) {
    try {
      await markMessageRead(m.id)
      m.isRead = 1
      messageStore.decrement()
    } catch { /* 拦截器已提示 */ }
  }
  const target = messageTarget(m, rolePrefix.value)
  if (target) router.push(target)
}

async function readAll() {
  try {
    await markAllMessagesRead()
    recent.value.forEach(m => { m.isRead = 1 })
    messageStore.reset()
  } catch { /* 拦截器已提示 */ }
}

function goAll() {
  router.push(`/${rolePrefix.value}/messages`)
}

let timer = null
onMounted(() => {
  messageStore.refreshUnread()
  timer = setInterval(() => messageStore.refreshUnread(), 60000)
})
onUnmounted(() => { if (timer) clearInterval(timer) })
</script>

<style scoped>
.bell-btn {
  background: transparent;
  border: 1px solid var(--app-stone, var(--color-border));
  border-radius: 6px;
  padding: 6px 8px;
  color: var(--color-text-secondary);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}
.bell-btn:hover { color: var(--color-primary); border-color: var(--color-primary); }

.bell-panel { display: flex; flex-direction: column; max-height: 460px; }
.bp-head { display: flex; align-items: center; justify-content: space-between; padding: 2px 2px 10px; border-bottom: 1px solid var(--color-border); }
.bp-title { font-size: 14px; font-weight: 650; color: var(--color-text-primary); }
.bp-allread { font-size: 12px; color: var(--color-primary); cursor: pointer; }
.bp-list { flex: 1; overflow-y: auto; padding: 4px 0; }
.bp-item { display: flex; gap: 9px; padding: 9px 6px; border-radius: 9px; cursor: pointer; }
.bp-item:hover { background: var(--color-bg-secondary); }
.bp-item.unread { background: var(--color-primary-lighter); }
.bp-item.unread:hover { background: color-mix(in srgb, var(--color-primary) 16%, var(--color-surface)); }
.dot { flex: none; width: 8px; height: 8px; border-radius: 50%; margin-top: 6px; background: var(--color-text-tertiary); }
.dot.primary { background: var(--color-primary); }
.dot.success { background: var(--color-success); }
.dot.muted { background: var(--color-text-tertiary); }
.bp-main { min-width: 0; flex: 1; }
.bp-row { display: flex; align-items: center; gap: 6px; }
.bp-badge { font-size: 10.5px; font-weight: 600; padding: 1px 7px; border-radius: 6px; flex: none; }
.bp-badge.primary { background: var(--color-primary-lighter); color: var(--color-primary); }
.bp-badge.success { background: var(--color-success-lighter); color: var(--color-success); }
.bp-badge.muted { background: var(--color-bg-secondary); color: var(--color-text-tertiary); }
.bp-mtitle { font-size: 13px; font-weight: 600; color: var(--color-text-primary); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.bp-content { font-size: 12px; color: var(--color-text-secondary); line-height: 1.5; margin-top: 3px;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; white-space: pre-line; }
.bp-time { font-size: 11px; color: var(--color-text-tertiary); margin-top: 4px; }
.bp-foot { text-align: center; padding: 9px 0 2px; border-top: 1px solid var(--color-border); font-size: 12.5px; color: var(--color-primary); cursor: pointer; }
</style>
