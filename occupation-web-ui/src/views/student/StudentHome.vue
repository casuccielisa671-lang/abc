<template>
  <div class="student-home" :class="`is-${activeTab}`">
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

    <StudentJobMetrics
      v-if="activeTab === 'applicable'"
      class="job-metrics-block"
      :items="applicable"
      :total-items="list"
      active-tab="applicable"
      :applicable-count="applicableAll.length"
    />

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

    <div v-if="activeTab === 'applications'" class="tab-panel">
      <MyApplications embedded :filter="query" />
    </div>

    <div v-if="activeTab === 'favorites'" class="tab-panel">
      <MyFavorites embedded :filter="query" />
    </div>

    <div v-show="activeTab === 'target'" class="tab-panel">
      <p class="panel-note">从你收藏的岗位里选一个设为目标岗位，看技能覆盖。首页「能力画像」会同步显示这个目标。（用上方搜索框可在收藏里筛选）</p>

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
import StudentJobMetrics from '@/components/home/StudentJobMetrics.vue'
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

const TOP_N = 25

const router = useRouter()
const route = useRoute()
const empStore = useEmploymentStore()
const list = ref([])
const loading = ref(false)
const loaded = ref(false)
const contacting = ref(null)
const query = ref('')   // 搜索关键词，即时过滤当前标签（职位名/公司/城市）

function jobMatch(job) {
  const q = query.value.trim().toLowerCase()
  if (!q) return true
  return [job?.title, job?.company, job?.city].some(v => (v || '').toLowerCase().includes(q))
}

const applicableAll = computed(() => list.value.filter(i => i.job?.applicable))
const referenceAll = computed(() => list.value.filter(i => !i.job?.applicable))
const applicable = computed(() => applicableAll.value.filter(i => jobMatch(i.job)))
const reference = computed(() => referenceAll.value.filter(i => jobMatch(i.job)))

const targetStore = useTargetJobStore()
const mySkills = ref('')
const favJobs = ref([])
const favLoading = ref(false)
const targetGap = computed(() => targetStore.target ? skillGap(targetStore.target.skills, mySkills.value) : null)
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
    list.value = toList(await getRecommend(TOP_N))
  } catch {
    list.value = []
  } finally {
    loading.value = false
    loaded.value = true
  }
}

watch(activeTab, tab => {
  if ((tab === 'applicable' || tab === 'market') && !loaded.value && !loading.value) {
    load()
  }
}, { immediate: true })

onMounted(() => {
  empStore.refresh()
  targetStore.load()
  loadFavTargets()
  getProfile().then(p => { mySkills.value = p?.skills || '' }).catch(() => {})
})
</script>

<style src="./StudentHome.css"></style>
