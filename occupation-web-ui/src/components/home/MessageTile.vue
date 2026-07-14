<template>
  <div class="msg-tile">
    <div class="tile-h">
      <span class="t">最新消息<em v-if="unread" class="badge">{{ unread > 99 ? '99+' : unread }}</em></span>
      <span class="view-all" @click="goAll">查看全部 ›</span>
    </div>
    <div v-loading="loading" class="msg-list">
      <div
        v-for="m in items"
        :key="m.id"
        class="mrow"
        :class="{ unread: !m.isRead }"
        @click="open(m)"
      >
        <span class="dot" :class="tone(m.type)" />
        <div class="mmain">
          <div class="mtop">
            <span class="mbadge" :class="tone(m.type)">{{ label(m.type) }}</span>
            <span class="mtitle">{{ m.title }}</span>
            <span class="mtime">{{ formatTime(m.createTime) }}</span>
          </div>
          <div class="mcontent">{{ m.content }}</div>
        </div>
      </div>
      <el-empty v-if="!loading && !items.length" description="暂无新消息" :image-size="52" />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useUserStore } from '@/store/user'
import { useMessageStore } from '@/store/message'
import { getMyMessages, markMessageRead } from '@/api/push'
import { toList } from '@/utils/list'
import { formatTime } from '@/utils/format'
import { messageMeta, messageTarget } from '@/utils/message'

const router = useRouter()
const userStore = useUserStore()
const messageStore = useMessageStore()
const { unread } = storeToRefs(messageStore)

const items = ref([])
const loading = ref(false)

const rolePrefix = computed(() =>
  ({ STUDENT: 'student', TEACHER: 'teacher', HR: 'hr', ADMIN: 'admin' }[userStore.role] || 'student')
)
function label(t) { return messageMeta(t).label }
function tone(t) { return messageMeta(t).tone }

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

function goAll() { router.push(`/${rolePrefix.value}/messages`) }

onMounted(() => {
  loading.value = true
  getMyMessages({ pageNum: 1, pageSize: 5 })
    .then(d => { items.value = toList(d) })
    .catch(() => {})
    .finally(() => { loading.value = false })
  messageStore.refreshUnread()
})
</script>

<style scoped>
.msg-tile { display: flex; flex-direction: column; height: 100%; }
.tile-h { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.tile-h .t { font-size: 14px; font-weight: 650; display: inline-flex; align-items: center; gap: 7px; }
.badge { font-style: normal; font-size: 11px; font-weight: 700; color: #fff; background: var(--color-danger, #E5484D);
  border-radius: 999px; padding: 0 7px; line-height: 17px; }
.view-all { font-size: 12px; color: var(--color-primary); cursor: pointer; }
.msg-list { flex: 1; overflow: hidden; display: flex; flex-direction: column; gap: 2px; }
.mrow { display: flex; gap: 9px; padding: 8px; border-radius: 10px; cursor: pointer; }
.mrow:hover { background: var(--color-bg-secondary); }
.mrow.unread { background: var(--color-primary-lighter); }
.dot { flex: none; width: 8px; height: 8px; border-radius: 50%; margin-top: 6px; background: var(--color-text-tertiary); }
.dot.primary { background: var(--color-primary); }
.dot.success { background: var(--color-success); }
.dot.muted { background: var(--color-text-tertiary); }
.mmain { min-width: 0; flex: 1; }
.mtop { display: flex; align-items: center; gap: 7px; }
.mbadge { font-size: 10.5px; font-weight: 600; padding: 1px 7px; border-radius: 6px; flex: none; }
.mbadge.primary { background: var(--color-primary-lighter); color: var(--color-primary); }
.mbadge.success { background: var(--color-success-lighter); color: var(--color-success); }
.mbadge.muted { background: var(--color-bg-secondary); color: var(--color-text-tertiary); }
.mtitle { font-size: 13px; font-weight: 600; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; flex: 1; min-width: 0; }
.mtime { font-size: 11px; color: var(--color-text-tertiary); flex: none; }
.mcontent { font-size: 12px; color: var(--color-text-secondary); line-height: 1.5; margin-top: 3px;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
</style>
