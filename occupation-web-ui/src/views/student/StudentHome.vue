<template>
  <div class="student-home">
    <!-- 标题 + 四标签切换栏 -->
    <div class="hub-head">
      <h2 class="page-title">职位信息</h2>
      <div class="seg" role="tablist">
        <button
          v-for="t in TABS" :key="t.key" type="button" role="tab"
          class="seg-btn" :class="{ active: activeTab === t.key }"
          @click="switchTab(t.key)"
        >
          {{ t.label }}
          <span v-if="t.key === 'applicable' && applicable.length" class="seg-badge">{{ applicable.length }}</span>
          <span v-else-if="t.key === 'market' && reference.length" class="seg-badge">{{ reference.length }}</span>
        </button>
      </div>
      <el-input
        v-model="query" clearable class="hub-search"
        placeholder="搜索职位名 / 公司 / 城市"
        :prefix-icon="Search"
      />
    </div>

    <!-- 可投递岗位 -->
    <div v-show="activeTab === 'applicable'" class="tab-panel">
      <p class="panel-note">企业在本平台发布，投递后可在「我的投递」看到处理进度</p>
      <div v-loading="loading">
        <el-empty v-if="!loading && !applicable.length" description="暂无可投递岗位，请先完善个人画像" />
        <div class="job-grid">
          <JobCard
            v-for="item in applicable" :key="item.job.id" :item="item"
            @open="goDetail(item.job.id)"
          >
            <el-button type="primary" text size="small" @click.stop="goDetail(item.job.id)">
              查看详情 →
            </el-button>
          </JobCard>
        </div>
      </div>
    </div>

    <!-- 市场参考 -->
    <div v-show="activeTab === 'market'" class="tab-panel">
      <p class="panel-note">
        采集自外部招聘渠道，本平台没有对应的招聘方。可自主联系，也用于技能热度与薪资趋势统计
      </p>
      <div v-loading="loading">
        <el-empty v-if="!loading && !reference.length" description="暂无市场参考职位" />
        <div class="job-grid">
          <JobCard
            v-for="item in reference" :key="item.job.id" :item="item"
            @open="goDetail(item.job.id)"
          >
            <el-button
              text size="small" :loading="contacting === item.job.id" :disabled="empStore.employed"
              @click.stop="handleContact(item.job)"
            >{{ empStore.employed ? '已入职' : '自主联系' }}</el-button>
          </JobCard>
        </div>
      </div>
    </div>

    <!-- 我的投递（复用原页面，隐藏其大标题） -->
    <div v-if="activeTab === 'applications'" class="tab-panel">
      <MyApplications embedded :filter="query" />
    </div>

    <!-- 我的收藏 -->
    <div v-if="activeTab === 'favorites'" class="tab-panel">
      <MyFavorites embedded :filter="query" />
    </div>

    <!-- 目标岗位：搜索选一个想去的岗位，看技能覆盖（首页「能力画像」卡同步显示） -->
    <div v-show="activeTab === 'target'" class="tab-panel">
      <p class="panel-note">从你收藏的岗位里选一个设为目标岗位，看技能覆盖。首页「能力画像」会同步显示这个目标。（用上方搜索框可在收藏里筛选）</p>

      <!-- 当前目标的技能对比卡 -->
      <div v-if="targetStore.target && targetGap" class="target-card">
        <div class="tc-head">
          <div>
            <div class="tc-title">{{ targetStore.target.title }}</div>
            <div class="tc-sub">{{ targetStore.target.company }}<span v-if="targetStore.target.city"> · {{ targetStore.target.city }}</span></div>
          </div>
          <div class="tc-cov" :class="{ full: targetGap.coverage === 100 }">
            <span class="tc-cov-num">{{ targetGap.coverage }}<small>%</small></span>
            <span class="tc-cov-lab">技能覆盖</span>
          </div>
        </div>
        <div class="tc-cols">
          <div class="tc-col">
            <div class="tc-col-h have">已掌握 {{ targetGap.have.length }}</div>
            <div class="tc-chips">
              <span v-for="s in targetGap.have" :key="s" class="chip have">{{ s }}</span>
              <span v-if="!targetGap.have.length" class="tc-dash">—</span>
            </div>
          </div>
          <div class="tc-col">
            <div class="tc-col-h miss">待补强 {{ targetGap.miss.length }}</div>
            <div class="tc-chips">
              <span v-for="s in targetGap.miss" :key="s" class="chip miss">{{ s }}</span>
              <span v-if="!targetGap.miss.length" class="tc-dash">全部掌握 🎉</span>
            </div>
          </div>
        </div>
        <div class="tc-foot">
          <el-button text type="primary" @click="goDetail(targetStore.target.id)">查看职位详情 →</el-button>
          <el-button text @click="clearTarget">清除目标</el-button>
        </div>
      </div>
      <el-empty v-else description="还没设目标岗位，从下面收藏里选一个「设为目标」" :image-size="52" />

      <!-- 从收藏中选目标（用顶部共享搜索框筛，不再单独放搜索） -->
      <div class="tgt-favs" v-loading="favLoading">
        <div class="tgt-favs-h">我的收藏 · 点「设为目标」选一个</div>
        <div class="job-grid">
          <div
            v-for="job in favFiltered" :key="job.id"
            class="job-card tgt-fav" :class="{ current: targetStore.target && targetStore.target.id === job.id }"
            @click="goDetail(job.id)"
          >
            <div class="job-head">
              <div>
                <h3 class="job-title">{{ job.title }}</h3>
                <p class="job-company">{{ job.company }}</p>
              </div>
              <div class="job-salary">{{ salaryRange(job.salaryMin, job.salaryMax) }}</div>
            </div>
            <div class="job-chips">
              <span class="chip">{{ job.city }}</span>
              <span v-if="job.industry" class="chip">{{ job.industry }}</span>
            </div>
            <div class="job-foot">
              <el-button
                v-if="targetStore.target && targetStore.target.id === job.id"
                type="success" size="small" plain disabled
              >✓ 当前目标</el-button>
              <el-button v-else type="primary" size="small" text @click.stop="setTargetFromFav(job)">设为目标岗位</el-button>
            </div>
          </div>
        </div>
        <el-empty
          v-if="!favLoading && !favFiltered.length"
          :description="favJobs.length ? '没有匹配的收藏' : '还没有收藏岗位，先去可投递/市场参考收藏一些'"
          :image-size="50"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getRecommend, contactJob, getProfile, getFavorites } from '@/api/student'
import { useTargetJobStore } from '@/store/targetJob'
import { skillGap } from '@/utils/skillGap'
import { salaryRange } from '@/utils/format'
import JobCard from '@/components/JobCard.vue'
import MyApplications from '@/views/student/Applications.vue'
import MyFavorites from '@/views/student/Favorites.vue'
import { toList } from '@/utils/list'
import { Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useEmploymentStore } from '@/store/employment'

const TABS = [
  { key: 'applicable', label: '可投递岗位' },
  { key: 'market', label: '市场参考' },
  { key: 'applications', label: '我的投递' },
  { key: 'favorites', label: '我的收藏' },
  { key: 'target', label: '目标岗位' }
]

/** 可投递 / 市场参考 两栏各自独立取前 25 名（后端 matchGrouped 按栏分别截取，互不抢名额） */
const TOP_N = 25

const router = useRouter()
const route = useRoute()
const empStore = useEmploymentStore()
const list = ref([])
const loading = ref(false)
const loaded = ref(false)
const contacting = ref(null)
const query = ref('')   // 搜索关键词，即时过滤当前标签（职位名/公司/城市）

/** 关键词命中：空则全放行；否则职位名/公司/城市任一含关键词（不区分大小写） */
function jobMatch(job) {
  const q = query.value.trim().toLowerCase()
  if (!q) return true
  return [job?.title, job?.company, job?.city].some(v => (v || '').toLowerCase().includes(q))
}

const applicable = computed(() => list.value.filter(i => i.job?.applicable && jobMatch(i.job)))
const reference = computed(() => list.value.filter(i => !i.job?.applicable && jobMatch(i.job)))

// ===== 目标岗位：从「收藏」里选，持久化 store，首页能力画像卡读同一份 =====
const targetStore = useTargetJobStore()
const mySkills = ref('')       // 画像技能，用于技能对比
const favJobs = ref([])        // 收藏列表（目标岗位从这里选）
const favLoading = ref(false)
const targetGap = computed(() => targetStore.target ? skillGap(targetStore.target.skills, mySkills.value) : null)
// 收藏用顶部共享搜索框 query 过滤（复用 jobMatch，与其它标签一致，不再单独放搜索框）
const favFiltered = computed(() => favJobs.value.filter(j => jobMatch(j)))

async function loadFavTargets() {
  favLoading.value = true
  try { favJobs.value = toList(await getFavorites()) }
  catch { favJobs.value = [] }
  finally { favLoading.value = false }
}
function setTargetFromFav(job) {
  targetStore.setTarget({
    id: job.id, title: job.title, company: job.company, city: job.city,
    skills: job.skills, salaryMin: job.salaryMin, salaryMax: job.salaryMax
  })
}
function clearTarget() { targetStore.clear() }

/**
 * 当前标签由路由决定，保证 Dashboard 卡片、消息通知等老链接照常落到正确标签：
 *   /student/applications → 我的投递    /student/favorites → 我的收藏
 *   /student/jobs（?tab=market → 市场参考，默认 → 可投递）
 */
const activeTab = computed(() => {
  if (route.path === '/student/applications') return 'applications'
  if (route.path === '/student/favorites') return 'favorites'
  const t = route.query.tab
  if (t === 'market') return 'market'
  if (t === 'target') return 'target'
  return 'applicable'
})

function switchTab(key) {
  if (key === activeTab.value) return
  const target = {
    applicable: { path: '/student/jobs' },
    market: { path: '/student/jobs', query: { tab: 'market' } },
    applications: { path: '/student/applications' },
    favorites: { path: '/student/favorites' },
    target: { path: '/student/jobs', query: { tab: 'target' } }
  }[key]
  router.push(target)
}

function goDetail(id) {
  router.push(`/student/job/${id}`)
}

/**
 * 自主联系：记录求职意向，并在有真实链接时打开原招聘页面。
 * 平台只能记录「你表达过意向」，之后你在原网站上做了什么无从得知，所以没有投递那样的状态跟踪。
 */
async function handleContact(job) {
  if (empStore.employed) return
  contacting.value = job.id
  try {
    await ElMessageBox.confirm(
      `将记录你对「${job.company} · ${job.title}」的求职意向，并为你打开原招聘页面（如果有）。`,
      '自主联系'
    )
    const { sourceUrl } = await contactJob(job.id)
    if (sourceUrl) {
      window.open(sourceUrl, '_blank', 'noopener,noreferrer')
      ElMessage.success('已记录求职意向，正在为你打开原招聘页面')
    } else {
      ElMessage.success('已记录求职意向。该职位为模拟数据，没有可跳转的外部页面')
    }
    // 已联系过的职位不再出现在推荐里，重新拉一次列表
    await load()
  } catch {
    /* 取消或拦截器已提示 */
  } finally {
    contacting.value = null
  }
}

async function load() {
  loading.value = true
  try {
    // 未填画像时后端抛业务异常，拦截器已提示，这里保持空列表并展示引导文案
    list.value = toList(await getRecommend(TOP_N))
  } catch {
    list.value = []
  } finally {
    loading.value = false
    loaded.value = true
  }
}

// 仅在「可投递/市场参考」标签才拉推荐：落在「我的投递/我的收藏」标签不必请求，
// 也避免未填画像时在这两个标签误弹「请先完善画像」。
watch(activeTab, tab => {
  if ((tab === 'applicable' || tab === 'market') && !loaded.value && !loading.value) {
    load()
  }
}, { immediate: true })

onMounted(() => {
  empStore.refresh()
  targetStore.load()
  loadFavTargets()   // 目标岗位从收藏里选
  // 载入画像技能供技能对比
  getProfile().then(p => { mySkills.value = p?.skills || '' }).catch(() => {})
})
</script>

<style scoped>
.hub-head {
  display: flex;
  align-items: center;
  gap: 18px;
  flex-wrap: wrap;
  margin-bottom: 18px;
}
.hub-head .page-title { margin: 0; }
.hub-search { margin-left: auto; max-width: 260px; }
@media (max-width: 700px) {
  .hub-search { margin-left: 0; max-width: none; width: 100%; }
}

/* 分段标签栏 */
.seg {
  display: inline-flex;
  gap: 4px;
  padding: 4px;
  background: var(--color-fill-light, var(--color-surface));
  border: 1px solid var(--color-border);
  border-radius: 12px;
}
.seg-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  border: none;
  background: transparent;
  color: var(--color-text-secondary);
  font-size: 13.5px;
  font-weight: 500;
  border-radius: 9px;
  cursor: pointer;
  transition: background .15s, color .15s;
  white-space: nowrap;
}
.seg-btn:hover { color: var(--color-text-primary); }
.seg-btn.active {
  background: var(--color-primary);
  color: #fff;
  font-weight: 600;
}
.seg-badge {
  font-size: 11px;
  line-height: 1;
  padding: 2px 6px;
  border-radius: 999px;
  background: color-mix(in srgb, currentColor 18%, transparent);
}
.seg-btn.active .seg-badge { background: rgba(255, 255, 255, .28); }

.panel-note {
  font-size: 12px;
  color: var(--color-text-tertiary);
  margin: 0 0 14px;
}

/* ---- 目标岗位 ---- */
.tgt-favs { margin-top: 20px; }
.tgt-favs-h { font-size: 14px; font-weight: 650; margin-bottom: 12px; color: var(--color-text-secondary); }
.tgt-fav.current { border-color: var(--color-success); box-shadow: 0 0 0 1px var(--color-success) inset; }
.target-card {
  border: 1px solid var(--color-border); border-radius: 14px; padding: 18px 20px;
  background: var(--color-surface); box-shadow: var(--shadow-sm); max-width: 720px;
}
.tc-head { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.tc-title { font-size: 17px; font-weight: 700; color: var(--color-text-primary); }
.tc-sub { font-size: 13px; color: var(--color-text-tertiary); margin-top: 2px; }
.tc-cov { text-align: center; flex: none; color: var(--color-danger); }
.tc-cov.full { color: var(--color-success); }
.tc-cov-num { font-size: 30px; font-weight: 800; line-height: 1; }
.tc-cov-num small { font-size: 15px; font-weight: 700; }
.tc-cov-lab { display: block; font-size: 11px; color: var(--color-text-tertiary); margin-top: 3px; }
.tc-cols { display: grid; grid-template-columns: 1fr 1fr; gap: 18px; margin-top: 18px; }
.tc-col-h { font-size: 13px; font-weight: 650; margin-bottom: 10px; }
.tc-col-h.have { color: var(--color-success); }
.tc-col-h.miss { color: var(--color-danger); }
.tc-chips { display: flex; flex-wrap: wrap; gap: 6px; }
.chip { font-size: 12px; padding: 3px 9px; border-radius: 999px; background: var(--color-bg-secondary); color: var(--color-text-secondary); }
.chip.have { background: var(--color-success-lighter); color: var(--color-success); }
.chip.miss { background: var(--color-danger-lighter); color: var(--color-danger); }
.tc-dash { font-size: 12px; color: var(--color-text-tertiary); }
.tc-foot { margin-top: 16px; display: flex; gap: 6px; }
@media (max-width: 640px) { .tc-cols { grid-template-columns: 1fr; } }
</style>
