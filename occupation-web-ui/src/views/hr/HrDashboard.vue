<template>
  <div class="role-home">
    <section class="welcome">
      <div class="hi">
        你好，{{ displayName }} 👋
        <small>招聘工作台 · 仅统计你发布的职位与收到的投递</small>
      </div>
      <el-button type="primary" @click="go('/hr/jobs')">职位管理</el-button>
    </section>

    <section class="bento">
      <MapHeroTile class="t-map" />

      <div class="tile kpi clickable" @click="go('/hr/jobs')">
        <div class="eyebrow">我发布职位</div>
        <div class="big">{{ jobCount }}</div>
        <div class="mini-sub">在招职位</div>
      </div>
      <div class="tile kpi clickable" @click="go('/hr/applications')">
        <div class="eyebrow">收到投递</div>
        <div class="big">{{ apps.length }}</div>
        <div class="mini-sub">累计投递</div>
      </div>
      <div class="tile kpi clickable" @click="go('/hr/applications')">
        <div class="eyebrow">待处理</div>
        <div class="big">{{ statusCount.SUBMITTED }}</div>
        <div class="mini-tags"><span class="tag warn">需查看</span></div>
      </div>
      <div class="tile kpi">
        <div class="eyebrow">面试 / 录用</div>
        <div class="big">{{ statusCount.INTERVIEW }}<small> / {{ statusCount.OFFER }}</small></div>
        <div class="mini-sub">推进中 / 已录用</div>
      </div>

      <!-- 投递漏斗 -->
      <div class="tile t-funnel">
        <div class="tile-h">
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
        <div class="tile-h"><span class="t">快捷入口</span></div>
        <div class="quick-grid">
          <div class="qa" @click="go('/hr/jobs')"><span class="qi">📋</span>职位管理</div>
          <div class="qa" @click="go('/hr/applications')"><span class="qi">📨</span>收到投递</div>
          <div class="qa" @click="go('/hr/talents')"><span class="qi">🧑‍💼</span>人才浏览</div>
          <div class="qa" @click="go('/hr/jobs')"><span class="qi">➕</span>发布职位</div>
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

<style scoped>
.role-home { max-width: 1200px; margin: 0 auto; }
.welcome { display: flex; align-items: center; gap: 18px; flex-wrap: wrap; padding: 16px 20px; border-radius: 14px; margin-bottom: 16px;
  background: linear-gradient(100deg, color-mix(in srgb, var(--color-primary) 12%, var(--color-surface)), var(--color-surface)); border: 1px solid var(--color-border); }
.welcome .hi { font-size: 19px; font-weight: 700; }
.welcome .hi small { display: block; font-size: 12.5px; font-weight: 500; color: var(--color-text-tertiary); margin-top: 3px; }
.welcome > .el-button { margin-left: auto; }

.bento { display: grid; grid-template-columns: repeat(4, 1fr); grid-auto-rows: 158px; gap: 14px; }
.tile { background: var(--color-surface); border: 1px solid var(--color-border); border-radius: 14px; padding: 16px; overflow: hidden; display: flex; flex-direction: column; box-shadow: var(--shadow-sm); }
.clickable { cursor: pointer; transition: transform .15s, box-shadow .15s, border-color .15s; }
.clickable:hover { transform: translateY(-2px); box-shadow: var(--shadow-md); border-color: color-mix(in srgb, var(--color-primary) 40%, var(--color-border)); }
.tile-h { display: flex; align-items: center; justify-content: space-between; margin-bottom: 10px; }
.tile-h .t { font-size: 14px; font-weight: 650; }
.tile-h .a { font-size: 12px; color: var(--color-primary); cursor: pointer; }
.eyebrow { font-size: 11px; letter-spacing: .4px; text-transform: uppercase; color: var(--color-text-tertiary); margin-bottom: 6px; }

.t-map { grid-column: span 2; grid-row: span 2; }
.t-funnel { grid-column: span 2; grid-row: span 2; }
.t-quick { grid-column: span 2; grid-row: span 2; }
.t-msg { grid-column: span 4; grid-row: span 2; }
.t-news { grid-column: span 4; grid-row: span 2; background: var(--color-surface); border: 1px solid var(--color-border); border-radius: 14px; padding: 16px; box-shadow: var(--shadow-sm); }

.kpi .big { font-size: 30px; font-weight: 750; letter-spacing: -.5px; margin-top: auto; }
.kpi .big small { font-size: 14px; font-weight: 500; color: var(--color-text-tertiary); }
.mini-sub { font-size: 12px; color: var(--color-text-tertiary); margin-top: 4px; }
.mini-tags { display: flex; gap: 5px; margin-top: auto; }
.tag { display: inline-flex; padding: 2px 9px; border-radius: 6px; font-size: 11.5px; font-weight: 600; }
.tag.warn { background: var(--color-warning-lighter); color: var(--color-warning); }

/* 漏斗 */
.funnel { flex: 1; display: flex; flex-direction: column; justify-content: center; gap: 11px; }
.fn-row { display: grid; grid-template-columns: 54px 1fr 34px; align-items: center; gap: 10px; }
.fn-label { font-size: 12.5px; color: var(--color-text-secondary); }
.fn-bar { height: 12px; border-radius: 6px; background: var(--color-bg-tertiary); overflow: hidden; }
.fn-fill { display: block; height: 100%; border-radius: 6px; transition: width .3s; }
.fn-fill.SUBMITTED { background: var(--color-info); }
.fn-fill.VIEWED { background: var(--color-primary); }
.fn-fill.INTERVIEW { background: var(--color-secondary); }
.fn-fill.OFFER { background: var(--color-success); }
.fn-fill.REJECTED { background: var(--color-danger); }
.fn-num { font-size: 13px; font-weight: 650; text-align: right; font-variant-numeric: tabular-nums; }

/* 快捷入口 */
.quick-grid { flex: 1; display: grid; grid-template-columns: repeat(2, 1fr); gap: 10px; }
.qa { display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 6px; border-radius: 11px;
  background: var(--color-bg-secondary); border: 1px solid var(--color-border); cursor: pointer; font-size: 13px; font-weight: 600; transition: all .15s; }
.qa:hover { border-color: var(--color-primary); color: var(--color-primary); transform: translateY(-1px); }
.qi { font-size: 20px; }

@media (max-width: 900px) {
  .bento { grid-template-columns: repeat(2, 1fr); }
  .t-map, .t-funnel, .t-quick { grid-column: span 2; }
}
</style>
