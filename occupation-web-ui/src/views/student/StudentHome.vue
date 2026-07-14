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
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getRecommend, contactJob } from '@/api/student'
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
  { key: 'favorites', label: '我的收藏' }
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

/**
 * 当前标签由路由决定，保证 Dashboard 卡片、消息通知等老链接照常落到正确标签：
 *   /student/applications → 我的投递    /student/favorites → 我的收藏
 *   /student/jobs（?tab=market → 市场参考，默认 → 可投递）
 */
const activeTab = computed(() => {
  if (route.path === '/student/applications') return 'applications'
  if (route.path === '/student/favorites') return 'favorites'
  return route.query.tab === 'market' ? 'market' : 'applicable'
})

function switchTab(key) {
  if (key === activeTab.value) return
  const target = {
    applicable: { path: '/student/jobs' },
    market: { path: '/student/jobs', query: { tab: 'market' } },
    applications: { path: '/student/applications' },
    favorites: { path: '/student/favorites' }
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

onMounted(() => { empStore.refresh() })
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
</style>
