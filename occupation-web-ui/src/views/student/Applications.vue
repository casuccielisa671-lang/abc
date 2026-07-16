<template>
  <div class="my-applications">
    <div class="page-head" v-if="!embedded">
      <span class="page-badge">投递追踪</span>
      <h2 class="page-title">我的投递</h2>
      <p class="page-sub">只有企业在本平台发布的职位可以投递；采集来的职位仅供参考</p>
    </div>

    <!-- 就业状态横幅 -->
    <div v-if="acceptedApp" class="emp-banner employed">
      🎉 你已入职：<b>{{ acceptedApp.jobTitle }} · {{ acceptedApp.company }}</b>
      <span class="sub">已就业，暂不能再投递或联系其他职位</span>
    </div>
    <div v-else-if="offerCount" class="emp-banner offered">
      你已收到 <b>{{ offerCount }}</b> 个录用，在下方选择一个「接收录用」即可正式入职（只能接收一个）
    </div>

    <!-- 进度概览 -->
    <div class="stat-grid" v-if="list.length">
      <div v-for="s in STATUSES" :key="s.key" class="stat-card">
        <span class="stat-dot" :class="s.key.toLowerCase()" />
        <div class="stat-label">{{ s.label }}</div>
        <div class="stat-value">{{ counts[s.key] || 0 }}</div>
      </div>
    </div>

    <!-- 面试通知卡：HR 邀请面试后在这里醒目展示时间地点 -->
    <div v-if="interviews.length" class="iv-cards">
      <div v-for="a in interviews" :key="a.applicationId" class="iv-card">
        <div class="iv-top">
          <span class="iv-badge">面试邀请</span>
          <span class="iv-title">{{ a.jobTitle || '职位' }}<em>· {{ a.company || '—' }}</em></span>
        </div>
        <div class="iv-grid">
          <div class="iv-cell"><span class="k">面试时间</span><span class="v strong">{{ formatTime(a.interviewTime) }}</span></div>
          <div class="iv-cell"><span class="k">地点/方式</span><span class="v">{{ a.interviewPlace || '—' }}</span></div>
          <div v-if="a.interviewContact" class="iv-cell"><span class="k">联系人</span><span class="v">{{ a.interviewContact }}</span></div>
          <div v-if="a.interviewContent" class="iv-cell full"><span class="k">面试内容</span><span class="v">{{ a.interviewContent }}</span></div>
        </div>
      </div>
    </div>

    <el-card class="applications-panel" :style="list.length ? 'margin-top:16px' : ''">
      <div class="panel-title">
        <span>投递记录</span>
        <em>{{ shownList.length }} 条</em>
      </div>
      <el-table class="applications-table" :data="shownList" v-loading="loading" stripe>
        <el-table-column label="职位" min-width="180">
          <template #default="{ row }">
            <span v-if="row.jobTitle">{{ row.jobTitle }}</span>
            <span v-else class="removed">职位已下架</span>
          </template>
        </el-table-column>
        <el-table-column prop="company" label="公司" min-width="170">
          <template #default="{ row }">{{ row.company || '—' }}</template>
        </el-table-column>
        <el-table-column label="城市" width="90">
          <template #default="{ row }">{{ row.jobCity || '—' }}</template>
        </el-table-column>
        <el-table-column label="薪资" width="140">
          <template #default="{ row }">
            <span class="salary-text">{{ salaryRange(row.salaryMin, row.salaryMax) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="投递时间" width="150">
          <template #default="{ row }">{{ formatTime(row.appliedAt) }}</template>
        </el-table-column>
        <el-table-column label="进度" width="180">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" size="small">{{ row.statusLabel }}</el-tag>
            <span v-if="row.statusChangedAt" class="changed">{{ formatTime(row.statusChangedAt) }}</span>
            <span v-else-if="!row.terminal" class="waiting">等待企业查看</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'OFFER'" type="success" size="small" plain
              :loading="accepting === row.applicationId" :disabled="!!acceptedApp"
              @click="accept(row)"
            >接收录用</el-button>
            <span v-else-if="row.status === 'ACCEPTED'" class="accepted-mark">✓ 已入职</span>
            <span v-else class="muted">—</span>
          </template>
        </el-table-column>
      </el-table>

      <el-empty
        v-if="!loading && !shownList.length"
        :description="list.length ? '没有匹配的投递' : '你还没有投递过职位。到「可投递岗位」里找喜欢的岗位试试'"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMyApplications, acceptOffer } from '@/api/student'
import { useEmploymentStore } from '@/store/employment'
import { toList } from '@/utils/list'
import { salaryRange, formatTime } from '@/utils/format'

// embedded=true 时隐藏自身大标题，供「职位信息」中心以标签页嵌入
const props = defineProps({
  embedded: { type: Boolean, default: false },
  filter: { type: String, default: '' }   // 「职位信息」中心传入的搜索关键词，过滤表格
})

/** 与后端 ApplicationStatus 枚举一一对应 */
const STATUSES = [
  { key: 'SUBMITTED', label: '已投递' },
  { key: 'VIEWED', label: '已查看' },
  { key: 'INTERVIEW', label: '邀请面试' },
  { key: 'OFFER', label: '已录用' },
  { key: 'ACCEPTED', label: '已入职' },
  { key: 'REJECTED', label: '不合适' }
]

const empStore = useEmploymentStore()
const list = ref([])

/** 表格按关键词过滤（职位/公司/城市）；统计卡、面试卡、就业横幅仍用全量 list */
const shownList = computed(() => {
  const q = (props.filter || '').trim().toLowerCase()
  if (!q) return list.value
  return list.value.filter(a =>
    [a.jobTitle, a.company, a.jobCity].some(v => (v || '').toLowerCase().includes(q)))
})
const loading = ref(false)
const accepting = ref(null)

const counts = computed(() =>
  list.value.reduce((acc, a) => {
    acc[a.status] = (acc[a.status] || 0) + 1
    return acc
  }, {})
)

/** 已被邀请面试且填了时间的投递，置顶成面试卡 */
const interviews = computed(() =>
  list.value.filter(a => a.status === 'INTERVIEW' && a.interviewTime)
)
/** 已接收的录用（已入职）；有则整页进入「已就业」态 */
const acceptedApp = computed(() => list.value.find(a => a.status === 'ACCEPTED') || null)
/** 收到但尚未接收的录用数 */
const offerCount = computed(() => list.value.filter(a => a.status === 'OFFER').length)

function statusTag(status) {
  return {
    SUBMITTED: 'info',
    VIEWED: 'warning',
    INTERVIEW: 'primary',
    OFFER: 'warning',
    ACCEPTED: 'success',
    REJECTED: 'danger'
  }[status] || 'info'
}

async function accept(row) {
  try {
    await ElMessageBox.confirm(
      `确定接收「${row.company || ''} · ${row.jobTitle || '该职位'}」的录用吗？` +
      '接收后即视为入职，将无法再投递或联系其他职位，也不能再接收别的录用。',
      '接收录用', { type: 'warning', confirmButtonText: '确定接收', cancelButtonText: '再想想' }
    )
  } catch { return }
  accepting.value = row.applicationId
  try {
    await acceptOffer(row.applicationId)
    ElMessage.success('已接收录用，恭喜入职！')
    await load()
    empStore.refresh()
  } catch { /* 拦截器已提示 */ } finally {
    accepting.value = null
  }
}

async function load() {
  loading.value = true
  try {
    list.value = toList(await getMyApplications())
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style src="./Applications.css"></style>
