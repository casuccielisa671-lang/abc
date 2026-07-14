<template>
  <div class="tool-page">
    <div class="tool-page-header">
      <el-button text @click="$router.push('/teacher/tools')">
        <el-icon><ArrowLeft /></el-icon> 返回工具箱
      </el-button>
      <h2>学生预警看板</h2>
      <p class="tool-desc">自动标记画像不完整、长期未投递、技能缺口大的学生，辅助精准辅导</p>
    </div>

    <div class="filter-bar">
      <el-select v-model="alertType" placeholder="预警类型" clearable style="width: 180px">
        <el-option label="全部" value="" />
        <el-option label="画像不完整" value="profile" />
        <el-option label="长期未投递" value="inactive" />
        <el-option label="技能缺口大" value="skill_gap" />
      </el-select>
      <el-select v-model="severity" placeholder="严重程度" clearable style="width: 140px">
        <el-option label="全部" value="" />
        <el-option label="高" value="high" />
        <el-option label="中" value="medium" />
        <el-option label="低" value="low" />
      </el-select>
      <el-input v-model="search" placeholder="搜索学生姓名" clearable style="width: 200px" />
    </div>

    <div class="stats-row">
      <div class="stat-card red">
        <div class="stat-num">{{ highCount }}</div>
        <div class="stat-label">高风险</div>
      </div>
      <div class="stat-card yellow">
        <div class="stat-num">{{ mediumCount }}</div>
        <div class="stat-label">中风险</div>
      </div>
      <div class="stat-card blue">
        <div class="stat-num">{{ lowCount }}</div>
        <div class="stat-label">低风险</div>
      </div>
      <div class="stat-card green">
        <div class="stat-num">{{ totalStudents }}</div>
        <div class="stat-label">覆盖学生</div>
      </div>
    </div>

    <div class="alert-list">
      <div v-for="item in filteredAlerts" :key="item.id" class="alert-item">
        <div class="alert-left">
          <div class="alert-sev" :class="'sev-' + item.severity">
            {{ item.severity === 'high' ? '高' : item.severity === 'medium' ? '中' : '低' }}
          </div>
          <div class="alert-info">
            <div class="alert-student">{{ item.studentName }}</div>
            <div class="alert-reason">{{ item.reason }}</div>
          </div>
        </div>
        <div class="alert-right">
          <span class="alert-date">{{ item.date }}</span>
          <el-button size="small" type="primary" plain @click="viewDetail(item)">查看</el-button>
        </div>
      </div>
    </div>

    <el-dialog v-model="dialogVisible" title="学生详情" width="560px">
      <div v-if="currentStudent" class="detail">
        <div class="detail-row"><span class="dl">姓名</span><span class="dv">{{ currentStudent.studentName }}</span></div>
        <div class="detail-row"><span class="dl">班级</span><span class="dv">{{ currentStudent.className }}</span></div>
        <div class="detail-row"><span class="dl">预警类型</span><span class="dv">{{ typeLabel(currentStudent.type) }}</span></div>
        <div class="detail-row"><span class="dl">严重程度</span><span class="dv">{{ sevLabel(currentStudent.severity) }}</span></div>
        <div class="detail-row"><span class="dl">详情</span><span class="dv">{{ currentStudent.detail }}</span></div>
        <div class="detail-row"><span class="dl">建议</span><span class="dv">{{ currentStudent.suggestion }}</span></div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ArrowLeft } from '@element-plus/icons-vue'
import { studentAlerts } from '@/api/student'

const alertType = ref('')
const severity = ref('')
const search = ref('')
const dialogVisible = ref(false)
const currentStudent = ref(null)
const alerts = ref([])
const summary = ref({ highCount: 0, mediumCount: 0, lowCount: 0, totalStudents: 0 })
const loading = ref(false)

const filteredAlerts = computed(() => {
  return alerts.value.filter(a => {
    if (alertType.value && a.type !== alertType.value) return false
    if (severity.value && a.severity !== severity.value) return false
    if (search.value && !a.studentName.includes(search.value)) return false
    return true
  })
})

const highCount = computed(() => summary.value.highCount)
const mediumCount = computed(() => summary.value.mediumCount)
const lowCount = computed(() => summary.value.lowCount)
const totalStudents = computed(() => summary.value.totalStudents)

const typeLabel = t => ({ profile: '画像不完整', inactive: '长期未投递', skill_gap: '技能缺口大' }[t] || t)
const sevLabel = s => ({ high: '高', medium: '中', low: '低' }[s] || s)

const fetchAlerts = async () => {
  loading.value = true
  try {
    const data = await studentAlerts({ alertType: alertType.value, severity: severity.value, search: search.value || undefined })
    alerts.value = data.alerts || []
    summary.value = data.summary || { highCount: 0, mediumCount: 0, lowCount: 0, totalStudents: 0 }
  } catch { alerts.value = [] }
  loading.value = false
}

onMounted(() => fetchAlerts())

const viewDetail = item => {
  currentStudent.value = item
  dialogVisible.value = true
}
</script>

<style scoped>
.tool-page { max-width: 1000px; margin: 0 auto; padding: 16px 0 40px; }
.tool-page-header { margin-bottom: 24px; }
.tool-page-header h2 { font-size: 22px; font-weight: 600; color: var(--app-ink); margin: 12px 0 6px; }
.tool-desc { font-size: 14px; color: var(--app-ink-3); margin: 0; }

.filter-bar { display: flex; gap: 12px; margin-bottom: 24px; }

.stats-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; margin-bottom: 24px; }
.stat-card { background: #fff; border: 1px solid #e8eaed; border-radius: 10px; padding: 16px; text-align: center; }
html.dark .stat-card { background: #1e1f22; border-color: #2e3035; }
.stat-num { font-size: 28px; font-weight: 700; }
.stat-label { font-size: 12px; color: var(--app-ink-3); margin-top: 4px; }
.stat-card.red .stat-num { color: #e74c3c; }
.stat-card.yellow .stat-num { color: #f39c12; }
.stat-card.blue .stat-num { color: #5470c6; }
.stat-card.green .stat-num { color: #27ae60; }

.alert-list { display: flex; flex-direction: column; gap: 8px; }
.alert-item {
  display: flex; align-items: center; justify-content: space-between;
  background: #fff; border: 1px solid #e8eaed; border-radius: 10px; padding: 16px 20px;
  transition: all 0.2s ease;
}
html.dark .alert-item { background: #1e1f22; border-color: #2e3035; }
.alert-item:hover { border-color: #c4c7cc; box-shadow: 0 2px 8px rgba(0,0,0,0.04); }
html.dark .alert-item:hover { border-color: #4a4d52; }

.alert-left { display: flex; align-items: center; gap: 14px; flex: 1; min-width: 0; }
.alert-sev {
  width: 28px; height: 28px; border-radius: 6px; font-size: 12px; font-weight: 600;
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}
.sev-high { background: #fde8e8; color: #e74c3c; }
.sev-medium { background: #fef3c7; color: #f39c12; }
.sev-low { background: #e0f2fe; color: #3b82f6; }
html.dark .sev-high { background: rgba(231,76,60,0.15); }
html.dark .sev-medium { background: rgba(243,156,18,0.15); }
html.dark .sev-low { background: rgba(59,130,246,0.15); }

.alert-info { min-width: 0; }
.alert-student { font-size: 14px; font-weight: 600; color: var(--app-ink); }
.alert-reason { font-size: 13px; color: var(--app-ink-3); margin-top: 2px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

.alert-right { display: flex; align-items: center; gap: 16px; flex-shrink: 0; }
.alert-date { font-size: 12px; color: var(--app-ink-4); }

.detail { display: flex; flex-direction: column; gap: 14px; }
.detail-row { display: flex; gap: 12px; }
.dl { width: 80px; font-size: 13px; color: var(--app-ink-3); flex-shrink: 0; }
.dv { font-size: 14px; color: var(--app-ink); }

@media (max-width: 768px) { .stats-row { grid-template-columns: repeat(2, 1fr); } .alert-right { flex-direction: column; gap: 8px; align-items: flex-end; } }
</style>
