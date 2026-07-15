<template>
  <div class="msg-page">
    <div class="page-head with-actions">
      <div>
        <h2 class="page-title">消息中心</h2>
        <p class="page-sub">投递进度、面试邀请、报告下发与系统通知都在这里</p>
      </div>
      <div class="page-actions">
        <el-radio-group v-model="typeFilter" @change="reload">
          <el-radio-button label="">全部</el-radio-button>
          <el-radio-button label="interview">面试/投递</el-radio-button>
          <el-radio-button label="REPORT">报告</el-radio-button>
          <el-radio-button label="RECOMMEND">推荐</el-radio-button>
          <el-radio-button label="SYSTEM">系统</el-radio-button>
        </el-radio-group>
        <el-button :disabled="!unread" @click="readAll">全部已读</el-button>
      </div>
    </div>

    <el-card>
      <div v-loading="loading" class="mlist">
        <div
          v-for="m in list"
          :key="m.id"
          class="mitem"
          :class="{ unread: !m.isRead }"
          @click="open(m)"
        >
          <span class="dot" :class="tone(m.type)" />
          <div class="mbody">
            <div class="mhead">
              <span class="mbadge" :class="tone(m.type)">{{ label(m.type) }}</span>
              <span class="mtitle">{{ m.title }}</span>
              <span v-if="!m.isRead" class="new">未读</span>
              <span class="mtime">{{ formatTime(m.createTime) }}</span>
            </div>
            <div class="mcontent">{{ m.content }}</div>
            <div v-if="targetLabel(m)" class="mgo">{{ targetLabel(m) }} ›</div>
          </div>
        </div>
        <el-empty v-if="!loading && !list.length" description="暂无消息" />
      </div>

      <div v-if="total > pageSize" class="pager">
        <el-pagination
          layout="prev, pager, next"
          :total="total"
          :page-size="pageSize"
          :current-page="pageNum"
          @current-change="onPage"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useUserStore } from '@/store/user'
import { useMessageStore } from '@/store/message'
import { getMyMessages, markMessageRead, markAllMessagesRead } from '@/api/push'
import { toList, toTotal } from '@/utils/list'
import { formatTime } from '@/utils/format'
import { messageMeta, messageTarget, messageActionLabel } from '@/utils/message'

const router = useRouter()
const userStore = useUserStore()
const messageStore = useMessageStore()
const { unread } = storeToRefs(messageStore)

const list = ref([])
const total = ref(0)
const loading = ref(false)
const pageNum = ref(1)
const pageSize = 12
const typeFilter = ref('')

const rolePrefix = computed(() =>
  ({ STUDENT: 'student', TEACHER: 'teacher', HR: 'hr', ADMIN: 'admin' }[userStore.role] || 'student')
)
function label(t) { return messageMeta(t).label }
function tone(t) { return messageMeta(t).tone }
function targetLabel(m) {
  return messageActionLabel(m)
}

/** 「面试/投递」把 INTERVIEW/OFFER/REJECT 三类归到一起，其余按精确类型过滤 */
function matchType(m) {
  if (!typeFilter.value) return true
  if (typeFilter.value === 'interview') return ['INTERVIEW', 'OFFER', 'REJECT'].includes(m.type)
  return m.type === typeFilter.value
}

async function load() {
  loading.value = true
  try {
    // 后端未按类型过滤，前端按当前页数据本地筛。数据量小，够用。
    const d = await getMyMessages({ pageNum: pageNum.value, pageSize })
    const all = toList(d)
    list.value = all.filter(matchType)
    total.value = toTotal(d, all)
  } finally {
    loading.value = false
  }
}
function reload() { pageNum.value = 1; load() }
function onPage(p) { pageNum.value = p; load() }

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
    list.value.forEach(m => { m.isRead = 1 })
    messageStore.reset()
  } catch { /* 拦截器已提示 */ }
}

onMounted(() => {
  messageStore.refreshUnread()
  load()
})
</script>

<style scoped>
.mlist { display: flex; flex-direction: column; }
.mitem { display: flex; gap: 12px; padding: 14px 10px; border-bottom: 1px solid var(--color-border); cursor: pointer; }
.mitem:last-child { border-bottom: none; }
.mitem:hover { background: var(--color-bg-secondary); }
.mitem.unread { background: var(--color-primary-lighter); }
.dot { flex: none; width: 9px; height: 9px; border-radius: 50%; margin-top: 7px; background: var(--color-text-tertiary); }
.dot.primary { background: var(--color-primary); }
.dot.success { background: var(--color-success); }
.dot.muted { background: var(--color-text-tertiary); }
.mbody { flex: 1; min-width: 0; }
.mhead { display: flex; align-items: center; gap: 8px; }
.mbadge { font-size: 11px; font-weight: 600; padding: 1px 8px; border-radius: 6px; flex: none; }
.mbadge.primary { background: var(--color-primary-lighter); color: var(--color-primary); }
.mbadge.success { background: var(--color-success-lighter); color: var(--color-success); }
.mbadge.muted { background: var(--color-bg-secondary); color: var(--color-text-tertiary); }
.mtitle { font-size: 14px; font-weight: 650; color: var(--color-text-primary); }
.new { font-size: 10.5px; font-weight: 700; color: #fff; background: var(--color-danger, #E5484D); border-radius: 5px; padding: 0 6px; }
.mtime { font-size: 12px; color: var(--color-text-tertiary); margin-left: auto; flex: none; }
.mcontent { font-size: 13px; color: var(--color-text-secondary); line-height: 1.7; margin-top: 6px; white-space: pre-line; }
.mgo { font-size: 12.5px; color: var(--color-primary); margin-top: 8px; }
.pager { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
