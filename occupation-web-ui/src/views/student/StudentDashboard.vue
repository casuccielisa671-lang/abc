<template>
  <div class="stu-home">
    <!-- 欢迎条 -->
    <section class="welcome">
      <div class="hi">
        你好，{{ displayName }} 👋
        <span class="emp-badge" :class="empStatus.toLowerCase()">{{ empLabel }}</span>
        <small>{{ profileSub }}</small>
      </div>
      <div class="prog">
        <div class="ring" :style="{ '--p': completeness }">
          <span>{{ completeness }}%</span>
        </div>
        <div class="ptxt">
          <b>画像完善度 {{ completeness }}%</b>
          <span>补齐后推荐更准</span>
        </div>
        <el-button type="primary" @click="go('/student/profile')">去完善</el-button>
      </div>
    </section>

    <!-- Bento 网格 -->
    <section class="bento">
      <MapHeroTile class="t-map" />

      <!-- KPI: 最高匹配 -->
      <div class="tile kpi clickable" @click="topMatch && go(`/student/job/${topMatch.job.id}`)">
        <div class="eyebrow">最高匹配</div>
        <div v-if="topMatch" class="gauge">
          <svg width="54" height="54" viewBox="0 0 54 54">
            <circle cx="27" cy="27" r="22" fill="none" stroke="var(--color-bg-tertiary)" stroke-width="6" />
            <circle cx="27" cy="27" r="22" fill="none" stroke="var(--color-primary)" stroke-width="6" stroke-linecap="round"
                    :stroke-dasharray="138.2" :stroke-dashoffset="138.2 * (1 - (topMatch.score || 0) / 100)"
                    transform="rotate(-90 27 27)" />
            <text x="27" y="31" text-anchor="middle" font-size="15" font-weight="750" fill="var(--color-text-primary)">
              {{ topMatch.score }}
            </text>
          </svg>
          <div class="gtxt">
            <div class="gt-title">{{ topMatch.job.title }}</div>
            <div class="gt-sub">{{ topMatch.job.company }}</div>
          </div>
        </div>
        <div v-else class="empty-mini">完善画像后生成推荐</div>
      </div>

      <!-- KPI: 我的投递 -->
      <div class="tile kpi clickable" @click="go('/student/applications')">
        <div class="eyebrow">我的投递</div>
        <div class="big">{{ applyStats.total }}</div>
        <div class="mini-tags">
          <span class="tag ok">OFFER {{ applyStats.offer }}</span>
          <span class="tag info">面试 {{ applyStats.interview }}</span>
          <span class="tag mut">待处理 {{ applyStats.pending }}</span>
        </div>
      </div>

      <!-- KPI: 我的收藏 -->
      <div class="tile kpi clickable" @click="go('/student/favorites')">
        <div class="eyebrow">我的收藏</div>
        <div class="big">{{ favCount }}</div>
        <div class="mini-sub">收藏的心仪职位</div>
      </div>

      <!-- KPI: 待办 -->
      <div class="tile kpi">
        <div class="eyebrow">待办</div>
        <div v-if="todos.length" class="todo-list">
          <div v-for="t in todos" :key="t.to" class="todo" @click="go(t.to)">
            <span class="tag warn">{{ t.label }}</span>
            <span class="arrow">→</span>
          </div>
        </div>
        <div v-else class="empty-mini done">✓ 资料已完善，保持关注推荐</div>
      </div>

      <!-- 为你推荐 -->
      <div class="tile t-reco">
        <div class="tile-h">
          <span class="t">为你推荐</span>
          <span class="a" @click="go('/student/jobs')">查看全部 ›</span>
        </div>
        <div v-loading="loading" class="reco-list">
          <div v-for="item in recoTop" :key="item.job.id" class="job" @click="go(`/student/job/${item.job.id}`)">
            <div class="co" :style="{ background: avatarBg(item.job.company) }">{{ (item.job.company || '公')[0] }}</div>
            <div class="mid">
              <div class="ti">{{ item.job.title }}</div>
              <div class="meta">{{ item.job.company }} · {{ item.job.city || '—' }}</div>
            </div>
            <div class="right">
              <div class="sal">{{ salaryRange(item.job.salaryMin, item.job.salaryMax, '面议') }}</div>
              <span class="match">匹配 {{ item.score }}</span>
            </div>
          </div>
          <el-empty v-if="!loading && !recoTop.length" description="完善画像后为你生成推荐" :image-size="52" />
        </div>
      </div>

      <!-- 能力画像 -->
      <div class="tile t-skill">
        <div class="tile-h">
          <span class="t">能力画像</span>
          <span class="a" @click="go('/student/profile')">我的技能 vs 目标岗位</span>
        </div>
        <div v-if="skillMatch.req.length" class="skill-body">
          <div class="skill-cover">
            <div class="cov-num">{{ skillCoverage }}<small>%</small></div>
            <div class="cov-lab">技能覆盖<br>「{{ topMatch.job.title }}」</div>
          </div>
          <div class="skill-cols">
            <div class="scol">
              <div class="scol-h have">已掌握 {{ skillMatch.have.length }}</div>
              <div class="chips">
                <span v-for="s in skillMatch.have" :key="s" class="chip have">{{ s }}</span>
                <span v-if="!skillMatch.have.length" class="dash">—</span>
              </div>
            </div>
            <div class="scol">
              <div class="scol-h miss">待补强 {{ skillMatch.miss.length }}</div>
              <div class="chips">
                <span v-for="s in skillMatch.miss" :key="s" class="chip miss">{{ s }}</span>
                <span v-if="!skillMatch.miss.length" class="dash">全部掌握 🎉</span>
              </div>
            </div>
          </div>
        </div>
        <div v-else class="empty-mini">完善画像与推荐后展示能力对比</div>
      </div>

      <!-- 最新消息 -->
      <div class="tile t-msg"><MessageTile /></div>

      <!-- 行业资讯 -->
      <NewsTile class="t-news" />
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { getProfile, getRecommend, getMyApplications, getFavorites, getResume } from '@/api/student'
import { toList } from '@/utils/list'
import { parseSkills } from '@/utils/skills'
import { salaryRange } from '@/utils/format'
import { employmentLabel } from '@/utils/employment'
import MapHeroTile from '@/components/home/MapHeroTile.vue'
import NewsTile from '@/components/home/NewsTile.vue'
import MessageTile from '@/components/home/MessageTile.vue'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const profile = ref(null)
const resumeExists = ref(false)
const recommend = ref([])
const applications = ref([])
const favorites = ref([])

function go(path) { router.push(path) }

const displayName = computed(() => profile.value?.realName || userStore.realName || userStore.username || '同学')
const profileSub = computed(() => {
  const p = profile.value
  const parts = []
  if (p?.major) parts.push(p.major)
  if (p?.educationLevel) parts.push(p.educationLevel)
  return parts.length ? parts.join(' · ') : '完善画像，开启精准推荐'
})
const completeness = computed(() => {
  const p = profile.value
  if (!p) return 0
  let filled = 0
  const total = 6
  if (p.major) filled++
  if (parseSkills(p.skills).length) filled++
  if (p.expectedCity) filled++
  if (p.expectedIndustry) filled++
  if (p.educationLevel) filled++
  if (p.expectedSalaryMin || p.expectedSalaryMax) filled++
  return Math.round((filled / total) * 100)
})

const topMatch = computed(() => recommend.value[0] || null)
const applyStats = computed(() => {
  const by = {}
  applications.value.forEach(a => { by[a.status] = (by[a.status] || 0) + 1 })
  return {
    total: applications.value.length,
    offer: by.OFFER || 0,
    interview: by.INTERVIEW || 0,
    pending: (by.SUBMITTED || 0) + (by.VIEWED || 0)
  }
})
const favCount = computed(() => favorites.value.length)

/** 就业状态徽章（从投递派生，与后端口径一致） */
const empStatus = computed(() => {
  const apps = applications.value
  if (!apps.length) return 'IDLE'
  if (apps.some(a => a.status === 'ACCEPTED')) return 'EMPLOYED'
  if (apps.some(a => a.status === 'OFFER')) return 'OFFERED'
  return 'SEEKING'
})
const empLabel = computed(() => employmentLabel(empStatus.value))
const todos = computed(() => {
  const t = []
  if (completeness.value < 100) t.push({ label: '完善个人画像', to: '/student/profile' })
  if (!resumeExists.value) t.push({ label: '填写我的简历', to: '/student/resume' })
  return t
})
const recoTop = computed(() => recommend.value.slice(0, 4))

const skillMatch = computed(() => {
  const top = topMatch.value
  if (!top) return { req: [], have: [], miss: [] }
  const req = parseSkills(top.job?.skills)
  const missSet = new Set((top.missingSkills || []).map(s => s.toLowerCase()))
  const miss = req.filter(s => missSet.has(s.toLowerCase()))
  const have = req.filter(s => !missSet.has(s.toLowerCase()))
  return { req, have, miss }
})
const skillCoverage = computed(() => {
  const m = skillMatch.value
  return m.req.length ? Math.round((m.have.length / m.req.length) * 100) : 0
})

const AV_COLORS = [
  'linear-gradient(135deg,#2563EB,#5B60F0)', 'linear-gradient(135deg,#0E9F6E,#15A34A)',
  'linear-gradient(135deg,#5B60F0,#8A5BF0)', 'linear-gradient(135deg,#C97A00,#E08600)'
]
function avatarBg(name) {
  let h = 0
  for (const ch of (name || '')) h = (h + ch.charCodeAt(0)) % AV_COLORS.length
  return AV_COLORS[h]
}

onMounted(() => {
  loading.value = true
  getProfile().then(d => { profile.value = d }).catch(() => {})
  getResume().then(d => { resumeExists.value = !!d?.exists }).catch(() => {})
  getMyApplications().then(d => { applications.value = toList(d) }).catch(() => {})
  getFavorites().then(d => { favorites.value = toList(d) }).catch(() => {})
  getRecommend(8).then(d => { recommend.value = toList(d) }).catch(() => {}).finally(() => { loading.value = false })
})
</script>

<style scoped>
.stu-home { max-width: 1200px; margin: 0 auto; }

/* 欢迎条 */
.welcome {
  display: flex; align-items: center; gap: 18px; flex-wrap: wrap;
  padding: 16px 20px; border-radius: 14px; margin-bottom: 16px;
  background: linear-gradient(100deg, color-mix(in srgb, var(--color-primary) 12%, var(--color-surface)), var(--color-surface));
  border: 1px solid var(--color-border);
}
.welcome .hi { font-size: 19px; font-weight: 700; }
.welcome .hi small { display: block; font-size: 12.5px; font-weight: 500; color: var(--color-text-tertiary); margin-top: 3px; }
.emp-badge { font-size: 12px; font-weight: 600; padding: 2px 10px; border-radius: 999px; margin-left: 8px; vertical-align: middle; }
.emp-badge.employed { background: var(--color-success-lighter); color: var(--color-success); }
.emp-badge.offered { background: var(--color-warning-lighter); color: var(--color-warning); }
.emp-badge.seeking { background: var(--color-primary-lighter); color: var(--color-primary); }
.emp-badge.idle { background: var(--color-bg-secondary); color: var(--color-text-tertiary); }
.prog { display: flex; align-items: center; gap: 12px; margin-left: auto; }
.ring { width: 52px; height: 52px; border-radius: 50%; display: grid; place-items: center;
  background: conic-gradient(var(--color-primary) calc(var(--p) * 1%), var(--color-bg-tertiary) 0); }
.ring span { width: 40px; height: 40px; border-radius: 50%; background: var(--color-surface);
  display: grid; place-items: center; font-size: 12px; font-weight: 700; color: var(--color-primary); }
.ptxt { font-size: 13px; color: var(--color-text-secondary); line-height: 1.4; }
.ptxt b { display: block; color: var(--color-text-primary); }

/* 网格 */
.bento { display: grid; grid-template-columns: repeat(4, 1fr); grid-auto-rows: 158px; gap: 14px; }
.tile { background: var(--color-surface); border: 1px solid var(--color-border); border-radius: 14px;
  padding: 16px; overflow: hidden; position: relative; display: flex; flex-direction: column; box-shadow: var(--shadow-sm); }
.clickable { cursor: pointer; transition: transform .15s, box-shadow .15s, border-color .15s; }
.clickable:hover { transform: translateY(-2px); box-shadow: var(--shadow-md); border-color: color-mix(in srgb, var(--color-primary) 40%, var(--color-border)); }
.tile-h { display: flex; align-items: center; justify-content: space-between; margin-bottom: 10px; }
.tile-h .t { font-size: 14px; font-weight: 650; }
.tile-h .a { font-size: 12px; color: var(--color-primary); cursor: pointer; }
.eyebrow { font-size: 11px; letter-spacing: .4px; text-transform: uppercase; color: var(--color-text-tertiary); margin-bottom: 6px; }

.t-map { grid-column: span 2; grid-row: span 2; }
.t-reco { grid-column: span 2; grid-row: span 2; }
.t-skill { grid-column: span 2; grid-row: span 2; }
.t-msg { grid-column: span 4; grid-row: span 2; }
.t-news { grid-column: span 4; grid-row: span 2;
  background: var(--color-surface); border: 1px solid var(--color-border); border-radius: 14px;
  padding: 16px; box-shadow: var(--shadow-sm); }

/* KPI */
.kpi .big { font-size: 30px; font-weight: 750; letter-spacing: -.5px; margin-top: auto; }
.mini-sub { font-size: 12px; color: var(--color-text-tertiary); margin-top: 4px; }
.mini-tags { display: flex; flex-wrap: wrap; gap: 5px; margin-top: auto; }
.tag { display: inline-flex; align-items: center; padding: 2px 9px; border-radius: 6px; font-size: 11.5px; font-weight: 600; }
.tag.ok { background: var(--color-success-lighter); color: var(--color-success); }
.tag.info { background: var(--color-primary-lighter); color: var(--color-primary); }
.tag.warn { background: var(--color-warning-lighter); color: var(--color-warning); }
.tag.mut { background: var(--color-bg-secondary); color: var(--color-text-tertiary); }
.gauge { display: flex; align-items: center; gap: 12px; margin-top: auto; }
.gt-title { font-weight: 650; font-size: 13.5px; }
.gt-sub { font-size: 12px; color: var(--color-text-tertiary); }
.empty-mini { margin-top: auto; font-size: 12.5px; color: var(--color-text-tertiary); }
.empty-mini.done { color: var(--color-success); }
.todo-list { margin-top: auto; display: flex; flex-direction: column; gap: 7px; }
.todo { display: flex; align-items: center; justify-content: space-between; cursor: pointer; }
.todo .arrow { color: var(--color-text-tertiary); }
.todo:hover .arrow { color: var(--color-primary); }

/* 推荐 */
.reco-list { flex: 1; overflow: hidden; }
.job { display: flex; align-items: center; gap: 12px; padding: 9px 8px; border-radius: 10px; cursor: pointer; }
.job:hover { background: var(--color-bg-secondary); }
.job + .job { border-top: 1px solid var(--color-border); }
.job .co { width: 36px; height: 36px; border-radius: 9px; display: grid; place-items: center; font-weight: 700; color: #fff; font-size: 14px; flex: none; }
.job .mid { flex: 1; min-width: 0; }
.job .ti { font-weight: 600; font-size: 13.5px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.job .meta { font-size: 12px; color: var(--color-text-tertiary); }
.job .right { text-align: right; }
.job .sal { color: var(--color-salary); font-weight: 650; font-size: 13px; }
.match { font-size: 11.5px; font-weight: 700; color: var(--color-primary); background: var(--color-primary-lighter); padding: 1px 8px; border-radius: 999px; }

/* 能力画像 */
.skill-body { flex: 1; display: flex; gap: 14px; }
.skill-cover { flex: none; width: 96px; display: flex; flex-direction: column; align-items: center; justify-content: center;
  background: var(--color-primary-lighter); border-radius: 12px; }
.cov-num { font-size: 30px; font-weight: 800; color: var(--color-primary); line-height: 1; }
.cov-num small { font-size: 14px; }
.cov-lab { font-size: 11.5px; color: var(--color-text-secondary); text-align: center; margin-top: 6px; }
.skill-cols { flex: 1; display: flex; flex-direction: column; gap: 10px; min-width: 0; }
.scol-h { font-size: 12px; font-weight: 650; margin-bottom: 5px; }
.scol-h.have { color: var(--color-success); }
.scol-h.miss { color: var(--color-warning); }
.chips { display: flex; flex-wrap: wrap; gap: 4px; }
.chip { display: inline-flex; padding: 2px 9px; border-radius: 999px; font-size: 11.5px; border: 1px solid var(--color-border); background: var(--color-bg-secondary); color: var(--color-text-secondary); }
.chip.have { background: var(--color-success-lighter); color: var(--color-success); border-color: transparent; }
.chip.miss { background: var(--color-warning-lighter); color: var(--color-warning); border-color: transparent; }
.dash { color: var(--color-text-tertiary); font-size: 12px; }

@media (max-width: 900px) {
  .bento { grid-template-columns: repeat(2, 1fr); }
  .t-map, .t-reco, .t-skill { grid-column: span 2; }
}
</style>
