<template>
  <div class="hr-applications">
    <div class="page-head">
      <h2 class="page-title">收到的投递</h2>
      <p class="page-sub">学生投递到你所发布职位的记录。点任意一行可查看他的联系方式与简历全文</p>
    </div>

    <el-card>
      <div class="search-bar">
        <el-select v-model="jobFilter" placeholder="按职位筛选" clearable style="width:240px">
          <el-option v-for="j in jobOptions" :key="j.jobId" :label="j.jobTitle" :value="j.jobId" />
        </el-select>
        <el-select v-model="statusFilter" placeholder="按状态筛选" clearable style="width:160px">
          <el-option v-for="s in STATUSES" :key="s.key" :label="s.label" :value="s.key" />
        </el-select>
        <span class="count">共 {{ filtered.length }} 条投递</span>
        <span v-if="pending" class="pending">{{ pending }} 条待处理</span>
        <el-button
          v-if="filtered.length > 1"
          type="primary"
          plain
          size="small"
          :loading="ranking"
          @click="rankApplicants"
        >
          AI 智能排序
        </el-button>
      </div>

      <el-table :data="sorted" v-loading="loading" stripe class="clickable" @row-click="openDetail">
        <el-table-column prop="jobTitle" label="投递职位" min-width="170" />
        <el-table-column label="投递人" width="100">
          <template #default="{ row }">{{ row.realName || '—' }}</template>
        </el-table-column>
        <el-table-column label="专业" min-width="140">
          <template #default="{ row }">
            <span v-if="row.profileCompleted">{{ row.major || '—' }}</span>
            <el-tag v-else type="info" size="small">未完善画像</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="学历" width="80">
          <template #default="{ row }"><span class="chip">{{ row.educationLevel || '—' }}</span></template>
        </el-table-column>
        <el-table-column label="技能" min-width="200">
          <template #default="{ row }">
            <div class="chip-row">
              <span v-for="sk in parseSkills(row.skills)" :key="sk" class="chip">{{ sk }}</span>
              <span v-if="!parseSkills(row.skills).length">—</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="期望薪资" width="130">
          <template #default="{ row }">
            <span class="salary-text">{{ salaryRange(row.expectedSalaryMin, row.expectedSalaryMax, '未填写') }}</span>
          </template>
        </el-table-column>
        <el-table-column label="简历" width="90">
          <template #default="{ row }">
            <el-tag :type="row.hasResume ? 'success' : 'info'" size="small">
              {{ row.hasResume ? '已填写' : '未填写' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="AI 匹配" width="90">
          <template #default="{ row }">
            <template v-if="matchScores[row.userId] != null">
              <el-tag :type="matchTag(matchScores[row.userId])" size="small">
                {{ matchScores[row.userId] }}分
              </el-tag>
            </template>
            <el-button
              v-else
              text
              size="small"
              type="primary"
              :loading="screening[row.userId]"
              @click.stop="screenOne(row)"
            >
              AI 分析
            </el-button>
          </template>
        </el-table-column>
        <el-table-column label="处理状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" size="small">{{ row.statusLabel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="投递时间" width="150">
          <template #default="{ row }">{{ formatTime(row.applyTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click.stop="openDetail(row)">处理</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty
        v-if="!loading && !filtered.length"
        :description="applications.length ? '该职位暂无投递' : '暂无投递记录'"
      />
    </el-card>

    <!-- AI 简历分析弹窗 -->
    <el-dialog v-model="screenVisible" title="AI 简历分析" width="600px" destroy-on-close>
      <div v-if="screenResult" class="screen-result">
        <div class="screen-header">
          <span class="screen-label">AI 分析</span>
          <el-tag v-if="screenResult.matchScore != null" :type="matchTag(screenResult.matchScore)" size="large">
            匹配度 {{ screenResult.matchScore }}分
          </el-tag>
        </div>
        <div v-if="screenResult.summary" class="screen-section">
          <h4>候选人概述</h4>
          <p>{{ screenResult.summary }}</p>
        </div>
        <div v-if="screenResult.highlights?.length" class="screen-section">
          <h4>亮点</h4>
          <ul>
            <li v-for="(h, i) in screenResult.highlights" :key="i">{{ h }}</li>
          </ul>
        </div>
        <div v-if="screenResult.risks?.length" class="screen-section">
          <h4>风险点</h4>
          <ul class="risks">
            <li v-for="(r, i) in screenResult.risks" :key="i">{{ r }}</li>
          </ul>
        </div>
        <div v-if="screenResult.matchAnalysis" class="screen-section">
          <h4>匹配分析</h4>
          <div v-if="screenResult.matchAnalysis.strengths?.length">
            <p class="sub-label">优势</p>
            <ul>
              <li v-for="(s, i) in screenResult.matchAnalysis.strengths" :key="i">{{ s }}</li>
            </ul>
          </div>
          <div v-if="screenResult.matchAnalysis.gaps?.length">
            <p class="sub-label">差距</p>
            <ul class="risks">
              <li v-for="(g, i) in screenResult.matchAnalysis.gaps" :key="i">{{ g }}</li>
            </ul>
          </div>
          <div v-if="screenResult.matchAnalysis.suggestion" class="suggestion-box">
            <p>{{ screenResult.matchAnalysis.suggestion }}</p>
          </div>
        </div>
      </div>
      <div v-else v-loading="true" style="min-height: 120px;" />
    </el-dialog>

    <!-- 抽屉里改了状态，列表要跟着刷新，否则「处理状态」列会显示旧值 -->
    <ApplicantDrawer v-model="detailVisible" :user-id="currentUserId" @changed="load" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getHrApplications, aiScreenResume, aiRankResumes } from '@/api/student'
import ApplicantDrawer from '@/components/ApplicantDrawer.vue'
import { toList } from '@/utils/list'
import { parseSkills } from '@/utils/skills'
import { salaryRange, formatTime } from '@/utils/format'

const STATUSES = [
  { key: 'SUBMITTED', label: '已投递' },
  { key: 'VIEWED', label: '已查看' },
  { key: 'INTERVIEW', label: '邀请面试' },
  { key: 'OFFER', label: '已录用' },
  { key: 'ACCEPTED', label: '已入职' },
  { key: 'REJECTED', label: '不合适' }
]

const applications = ref([])
const loading = ref(false)
const jobFilter = ref(null)
const statusFilter = ref(null)

const detailVisible = ref(false)
const currentUserId = ref(null)

// AI 相关
const matchScores = ref({})
const screening = ref({})
const ranking = ref(false)
const rankOrder = ref([]) // AI 排序后的 userId 顺序

// 弹窗
const screenVisible = ref(false)
const screenResult = ref(null)

/** 还没看过的投递，HR 一眼知道有多少活要干 */
const pending = computed(() => applications.value.filter(a => a.status === 'SUBMITTED').length)

function statusTag(status) {
  return {
    SUBMITTED: 'info',
    VIEWED: 'warning',
    INTERVIEW: 'primary',
    OFFER: 'success',
    ACCEPTED: 'success',
    REJECTED: 'danger'
  }[status] || 'info'
}

function matchTag(score) {
  if (score >= 80) return 'success'
  if (score >= 60) return 'warning'
  return 'danger'
}

function openDetail(row) {
  if (row.userId == null) return
  // 强制转 Number：抽屉 prop 类型是 Number，传字符串会触发 vue prop 类型警告
  currentUserId.value = Number(row.userId)
  detailVisible.value = true
}

/** 筛选下拉只列出真正收到过投递的职位 */
const jobOptions = computed(() => {
  const seen = new Map()
  for (const a of applications.value) {
    if (!seen.has(a.jobId)) seen.set(a.jobId, { jobId: a.jobId, jobTitle: a.jobTitle })
  }
  return [...seen.values()]
})

const filtered = computed(() =>
  applications.value.filter(a =>
    (jobFilter.value == null || a.jobId === jobFilter.value) &&
    (statusFilter.value == null || a.status === statusFilter.value)
  )
)

/** 排序：AI 排序优先，否则按投递时间倒序 */
const sorted = computed(() => {
  const list = [...filtered.value]
  if (rankOrder.value.length > 0) {
    const orderMap = new Map(rankOrder.value.map((id, i) => [id, i]))
    list.sort((a, b) => {
      const oa = orderMap.get(a.userId)
      const ob = orderMap.get(b.userId)
      if (oa != null && ob != null) return oa - ob
      if (oa != null) return -1
      if (ob != null) return 1
      return 0
    })
  }
  return list
})

/** 单份 AI 分析 */
async function screenOne(row) {
  screening.value = { ...screening.value, [row.userId]: true }
  try {
    // 拦截器已解包 Result.data —— 拿到的是 ResumeScreenVO 本身
    const data = await aiScreenResume(Number(row.userId), row.jobId ? Number(row.jobId) : undefined)
    if (data && data.matchScore != null) {
      matchScores.value = { ...matchScores.value, [row.userId]: data.matchScore }
    }
    screenResult.value = data
    if (data) screenVisible.value = true
  } catch (e) {
    console.error('AI 分析失败:', e)
  } finally {
    screening.value = { ...screening.value, [row.userId]: false }
  }
}

/** 批量 AI 排序 */
async function rankApplicants() {
  if (filtered.value.length < 2) return
  // 取当前筛选的第一个职位作为排序基准
  const jobId = jobFilter.value || filtered.value[0]?.jobId
  if (!jobId) {
    ElMessage.warning('请先选择要排序的职位')
    return
  }

  ranking.value = true
  try {
    const ids = filtered.value.map(a => Number(a.userId))
    // 拦截器已解包 Result.data —— 拿到的是 ResumeScreenVO[] 本身
    const list = await aiRankResumes(Number(jobId), ids)
    const arr = Array.isArray(list) ? list : []
    rankOrder.value = arr.map(r => r.userId)
    // 同步匹配分数
    const scores = {}
    arr.forEach(r => { if (r && r.userId != null) scores[r.userId] = r.matchScore })
    matchScores.value = { ...matchScores.value, ...scores }
    if (arr.length === 0) {
      ElMessage.warning('排序结果为空')
    }
  } catch (e) {
    console.error('AI 排序失败:', e)
  } finally {
    ranking.value = false
  }
}

async function load() {
  loading.value = true
  try {
    applications.value = toList(await getHrApplications())
    // 重置排序和分数
    rankOrder.value = []
    matchScores.value = {}
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.search-bar { display: flex; align-items: center; gap: 14px; margin-bottom: 20px; flex-wrap: wrap; }
.count { font-size: 13px; color: var(--app-ink-3); }
.pending { font-size: 13px; color: var(--app-ember); font-weight: 600; }
.chip-row { display: flex; flex-wrap: wrap; gap: 4px; }
.clickable :deep(.el-table__row) { cursor: pointer; }

/* AI 分析弹窗 */
.screen-result { display: flex; flex-direction: column; gap: 16px; }
.screen-header { display: flex; align-items: center; gap: 12px; }
.screen-label { font-size: 13px; color: var(--app-ink-3); }
.screen-section h4 { font-size: 14px; font-weight: 600; color: var(--app-ink); margin: 0 0 8px; }
.screen-section p { font-size: 14px; color: var(--app-ink-2); line-height: 1.7; margin: 0; }
.screen-section ul { margin: 0; padding-left: 18px; }
.screen-section li { font-size: 14px; color: var(--app-ink-2); line-height: 1.7; }
.screen-section ul.risks li { color: var(--app-ember); }
.sub-label { font-size: 13px; font-weight: 600; color: var(--app-ink-3); margin: 8px 0 4px; }
.suggestion-box { margin-top: 8px; padding: 10px 14px; background: #f0f7ff; border-radius: 8px; border-left: 3px solid var(--color-primary); }
html.dark .suggestion-box { background: #1a2332; }
.suggestion-box p { font-size: 14px; color: var(--app-ink-2); line-height: 1.7; margin: 0; }
</style>
