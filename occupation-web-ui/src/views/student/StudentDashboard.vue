<template>
  <div class="stu-home">
    <!-- 欢迎条 -->
    <section class="welcome">
      <span class="card-badge welcome-badge">职业驾驶舱</span>
      <div class="hi">
        你好，{{ displayName }} 👋
        <span class="emp-badge" :class="empStatus.toLowerCase()">{{ empLabel }}</span>
        <small>{{ profileSub }}</small>
      </div>
      <div class="prog">
        <div class="ring" :class="{ full: completeness === 100 }" :style="{ '--p': completeness }">
          <span>{{ completeness }}%</span>
        </div>
        <div class="ptxt">
          <b>画像完善度 {{ completeness }}%</b>
          <span>{{ completeness === 100 ? '已全部完善' : '补齐后推荐更准' }}</span>
        </div>
        <el-button
          :type="completeness === 100 ? 'success' : 'danger'"
          @click="go('/student/profile')"
        >{{ completeness === 100 ? '已完善' : '去完善' }}</el-button>
      </div>
    </section>

    <!-- Bento 网格 -->
    <section class="bento">
      <MapHeroTile class="t-map" />

      <!-- KPI: 最高匹配 -->
      <div class="tile kpi clickable" @click="topMatch && go(`/student/job/${topMatch.job.id}`)">
        <span class="card-badge">画像驱动</span>
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
        <span class="card-badge">投递追踪</span>
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
        <span class="card-badge">收藏池</span>
        <div class="eyebrow">我的收藏</div>
        <div class="big">{{ favCount }}</div>
        <div class="mini-sub">收藏的心仪职位</div>
      </div>

      <!-- KPI: 待办 -->
      <div class="tile kpi">
        <span class="card-badge">待办提醒</span>
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
          <span class="t">为你推荐 <em class="section-tag">智能推荐</em></span>
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

      <!-- 能力画像（我的技能 vs 目标岗位；目标在职位信息「目标岗位」标签里选，此处是缩影） -->
      <div class="tile t-skill">
        <div class="tile-h">
          <span class="t">能力画像 <em class="section-tag">能力诊断</em></span>
          <span class="a" @click="go('/student/jobs?tab=target')">我的技能 vs 目标岗位</span>
        </div>
        <div v-if="targetStore.target && skillMatch.req.length" class="skill-body">
          <div class="skill-cover">
            <div class="cov-num">{{ skillCoverage }}<small>%</small></div>
            <div class="cov-lab">技能覆盖<br>「{{ targetStore.target.title }}」</div>
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
        <div v-else class="empty-mini clickable" @click="go('/student/jobs?tab=target')">
          未选择目标岗位 · 点这里去选一个 →
        </div>
      </div>

      <!-- 最新消息 -->
      <div class="tile t-msg">
        <span class="card-badge">站内消息</span>
        <MessageTile />
      </div>

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
import { useTargetJobStore } from '@/store/targetJob'
import { skillGap } from '@/utils/skillGap'
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
  const total = 7
  if (p.major) filled++
  if (parseSkills(p.skills).length) filled++
  if (p.expectedCity) filled++
  if (p.expectedIndustry) filled++
  if (p.educationLevel) filled++
  if (p.expectedSalaryMin || p.expectedSalaryMax) filled++
  if (p.avatarUrl) filled++   // 证件照也计入完整度
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

// 能力画像卡改用「目标岗位」store（学生在职位信息「目标岗位」标签里选，两处呼应）；不预设
const targetStore = useTargetJobStore()
const skillMatch = computed(() => {
  const t = targetStore.target
  if (!t) return { req: [], have: [], miss: [] }
  return skillGap(t.skills, profile.value?.skills)
})
const skillCoverage = computed(() => skillMatch.value.coverage || 0)

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
  targetStore.load()   // 载入当前账号选定的目标岗位（能力画像卡据此显示）
  getProfile().then(d => { profile.value = d }).catch(() => {})
  getResume().then(d => { resumeExists.value = !!d?.exists }).catch(() => {})
  getMyApplications().then(d => { applications.value = toList(d) }).catch(() => {})
  getFavorites().then(d => { favorites.value = toList(d) }).catch(() => {})
  getRecommend(8).then(d => { recommend.value = toList(d) }).catch(() => {}).finally(() => { loading.value = false })
})
</script>

<style src="./StudentDashboard.css"></style>
