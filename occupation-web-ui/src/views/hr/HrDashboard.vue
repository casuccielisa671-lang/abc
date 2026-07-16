<template>
  <div class="role-home hr-home">
    <section class="welcome">
      <div class="hi">
        <span class="welcome-badge">招聘驾驶舱</span>
        你好，{{ displayName }}
        <small>招聘工作台 · 仅统计你发布的职位与收到的投递</small>
      </div>
      <el-button type="primary" @click="go('/hr/jobs')">职位管理</el-button>
    </section>

    <section class="bento">
      <MapHeroTile class="t-map" />

      <div class="tile kpi clickable" @click="go('/hr/jobs')">
        <span class="metric-icon jobs">职</span>
        <div>
          <div class="eyebrow">我发布职位</div>
          <div class="big">{{ jobCount }}</div>
          <div class="mini-sub">在招职位</div>
        </div>
      </div>
      <div class="tile kpi clickable" @click="go('/hr/applications')">
        <span class="metric-icon applies">投</span>
        <div>
          <div class="eyebrow">收到投递</div>
          <div class="big">{{ apps.length }}</div>
          <div class="mini-sub">累计投递</div>
        </div>
      </div>
      <div class="tile kpi clickable" @click="go('/hr/applications')">
        <span class="metric-icon pending">待</span>
        <div>
          <div class="eyebrow">待处理</div>
          <div class="big">{{ statusCount.SUBMITTED }}</div>
          <div class="mini-tags"><span class="tag warn">需查看</span></div>
        </div>
      </div>
      <div class="tile kpi">
        <span class="metric-icon hired">录</span>
        <div>
          <div class="eyebrow">面试 / 录用</div>
          <div class="big">{{ statusCount.INTERVIEW }}<small> / {{ statusCount.OFFER }}</small></div>
          <div class="mini-sub">推进中 / 已录用</div>
        </div>
      </div>

      <!-- 投递漏斗 -->
      <div class="tile t-funnel">
        <div class="tile-h section-title">
          <span class="t">投递漏斗</span>
          <span class="a" @click="go('/hr/applications')">收到的投递 ›</span>
        </div>
        <div v-loading="loading" class="funnel">
          <div v-for="s in FUNNEL" :key="s.key" class="fn-row">
            <div class="fn-label">{{ s.label }}</div>
            <div class="fn-bar"><span class="fn-fill" :class="s.key" :style="{ width: barW(statusCount[s.key]) + '%' }"></span></div>
            <div class="fn-num">{{ statusCount[s.key] }}</div>
          </div>
        </div>
      </div>

      <!-- 快捷入口 -->
      <div class="tile t-quick">
        <div class="tile-h section-title"><span class="t">快捷入口</span></div>
        <div class="quick-grid">
          <div class="qa" @click="go('/hr/jobs')">职位管理</div>
          <div class="qa" @click="go('/hr/applications')">收到投递</div>
          <div class="qa" @click="go('/hr/talents')">人才浏览</div>
          <div class="qa" @click="go('/hr/jobs')">发布职位</div>
        </div>
      </div>

      <div class="tile t-msg"><MessageTile /></div>

      <NewsTile class="t-news" />
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { getHrJobs, getHrApplications } from '@/api/student'
import { toList, toTotal } from '@/utils/list'
import MapHeroTile from '@/components/home/MapHeroTile.vue'
import NewsTile from '@/components/home/NewsTile.vue'
import MessageTile from '@/components/home/MessageTile.vue'

const router = useRouter()
const userStore = useUserStore()
function go(p) { router.push(p) }

const FUNNEL = [
  { key: 'SUBMITTED', label: '已投递' }, { key: 'VIEWED', label: '已查看' },
  { key: 'INTERVIEW', label: '面试' }, { key: 'OFFER', label: '录用' }, { key: 'REJECTED', label: '不合适' }
]

const jobCount = ref(0)
const apps = ref([])
const loading = ref(false)

const displayName = computed(() => userStore.realName || userStore.username || 'HR')
const statusCount = computed(() => {
  const c = { SUBMITTED: 0, VIEWED: 0, INTERVIEW: 0, OFFER: 0, REJECTED: 0 }
  apps.value.forEach(a => { if (c[a.status] !== undefined) c[a.status]++ })
  return c
})
const maxCount = computed(() => Math.max(1, ...Object.values(statusCount.value)))
function barW(n) { return Math.round((n / maxCount.value) * 100) }

onMounted(() => {
  loading.value = true
  getHrJobs({ pageNum: 1, pageSize: 200 }).then(d => { jobCount.value = toTotal(d, toList(d)) }).catch(() => {})
  getHrApplications().then(d => { apps.value = toList(d) }).catch(() => {}).finally(() => { loading.value = false })
})
</script>

<style src="@/views/hr/HrDashboard.css"></style>
