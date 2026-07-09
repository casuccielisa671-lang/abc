<template>
  <div class="crawler-page">
    <div class="page-head with-actions">
      <div>
        <h2 class="page-title">采集任务管理</h2>
        <p class="page-sub">管理职位数据采集任务与执行日志</p>
      </div>
      <div class="page-actions">
        <el-button @click="handleMockCrawl">Mock 模拟采集</el-button>
        <el-button type="primary" @click="openDialog()">新增任务</el-button>
      </div>
    </div>

    <!-- 任务表格 -->
    <el-card>
      <el-table :data="tasks" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="sourceName" label="任务名称" min-width="160" />
        <el-table-column label="采集源" width="120">
          <template #default="{ row }"><span class="chip">{{ sourceLabel(row.sourceType) }}</span></template>
        </el-table-column>
        <el-table-column prop="cronExpr" label="Cron 表达式" width="150" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.statusText }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="150">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status !== 1" text type="primary" size="small" @click="handleStart(row.id)">
              启动
            </el-button>
            <el-button v-else text type="warning" size="small" @click="handleStop(row.id)">停止</el-button>
            <el-button text type="primary" size="small" @click="openDialog(row)">编辑</el-button>
            <el-button text type="danger" size="small" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && !tasks.length" description="暂无采集任务" />

      <el-pagination
        v-if="total > size"
        v-model:current-page="page" :page-size="size"
        :total="total" layout="total, prev, pager, next"
        @current-change="loadTasks"
      />
    </el-card>

    <!-- 采集日志 -->
    <el-card style="margin-top:16px">
      <template #header>采集日志</template>
      <el-table :data="logs" v-loading="logLoading" stripe size="small">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="taskId" label="任务ID" width="80" />
        <el-table-column label="开始时间" width="150">
          <template #default="{ row }">{{ formatTime(row.startTime) }}</template>
        </el-table-column>
        <el-table-column label="结束时间" width="150">
          <template #default="{ row }">{{ formatTime(row.endTime) }}</template>
        </el-table-column>
        <el-table-column prop="recordCount" label="采集条数" width="90" />
        <el-table-column prop="duration" label="耗时" width="90" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="logStatusTag(row.status)" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="errorMsg" label="错误信息" min-width="180" show-overflow-tooltip />
      </el-table>

      <el-empty v-if="!logLoading && !logs.length" description="暂无采集日志" />

      <el-pagination
        v-if="logTotal > logSize"
        v-model:current-page="logPage" :page-size="logSize"
        :total="logTotal" layout="total, prev, pager, next"
        @current-change="loadLogs"
      />
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑任务' : '新增任务'" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="采集源类型" prop="sourceType">
          <el-select v-model="form.sourceType" style="width:100%">
            <el-option label="MOCK（模拟数据，推荐）" value="MOCK" />
            <el-option label="BOSS 直聘" value="BOSS_ZHIPIN" />
            <el-option label="智联招聘" value="ZHAOPIN" />
          </el-select>
        </el-form-item>
        <el-alert
          v-if="form.sourceType !== 'MOCK'" type="warning" :closable="false" show-icon
          style="margin:0 0 18px 100px; width:calc(100% - 100px)"
        >
          真实站点采集依赖目标网站的页面结构，改版后会失效，且高频抓取可能违反其服务条款。
          日常开发与演示请使用 MOCK 数据源。
        </el-alert>
        <el-form-item label="任务名称" prop="sourceName">
          <el-input v-model="form.sourceName" placeholder="如：BOSS直聘-Java开发" />
        </el-form-item>
        <el-form-item label="URL/数据文件" prop="urlPattern">
          <el-input v-model="form.urlPattern" :placeholder="urlPatternHint" />
          <span class="form-tip">{{ urlPatternHint }}</span>
        </el-form-item>
        <el-form-item label="Cron 表达式" prop="cronExpr">
          <el-input v-model="form.cronExpr" placeholder="如：0 0 6 * * ?（每天6点）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import {
  getCrawlerTasks, getCrawlerTask, createCrawlerTask, updateCrawlerTask,
  deleteCrawlerTask, startCrawlerTask, stopCrawlerTask, mockCrawl,
  getCrawlerLogs
} from '@/api/admin'
import { toList, toTotal } from '@/utils/list'
import { formatTime } from '@/utils/format'
import { ElMessage, ElMessageBox } from 'element-plus'

// ========== 任务列表 ==========
const tasks = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)

// 采集接口用的是 page/size（与其他控制器的 pageNum/pageSize 不同，勿改）
async function loadTasks() {
  loading.value = true
  try {
    const data = await getCrawlerTasks({ page: page.value, size: size.value })
    tasks.value = toList(data)
    total.value = toTotal(data, tasks.value)
  } finally {
    loading.value = false
  }
}

// ========== 日志列表 ==========
const logs = ref([])
const logLoading = ref(false)
const logPage = ref(1)
const logSize = ref(10)
const logTotal = ref(0)

async function loadLogs() {
  logLoading.value = true
  try {
    const data = await getCrawlerLogs({ page: logPage.value, size: logSize.value })
    logs.value = toList(data)
    logTotal.value = toTotal(data, logs.value)
  } finally {
    logLoading.value = false
  }
}

// ========== 启停 ==========
async function handleStart(id) {
  try {
    await startCrawlerTask(id)
    ElMessage.success('任务已启动')
    loadTasks()
  } catch { /* handled */ }
}

async function handleStop(id) {
  try {
    await stopCrawlerTask(id)
    ElMessage.success('任务已停止')
    loadTasks()
  } catch { /* handled */ }
}

// ========== 删除 ==========
async function handleDelete(id) {
  await ElMessageBox.confirm('确定删除该任务吗？', '确认', { type: 'warning' })
  try {
    await deleteCrawlerTask(id)
    ElMessage.success('已删除')
    loadTasks()
  } catch { /* handled */ }
}

// ========== Mock 采集 ==========
async function handleMockCrawl() {
  try {
    await mockCrawl('mock-jobs.json')
    ElMessage.success('Mock 采集已触发，数据将通过 Kafka 进入清洗链路')
    setTimeout(() => { loadLogs(); loadTasks() }, 3000)
  } catch { /* handled */ }
}

// ========== 新增/编辑对话框 ==========
const dialogVisible = ref(false)
const editingId = ref(null)
const saving = ref(false)
const formRef = ref(null)
const form = reactive({
  sourceType: 'MOCK',
  sourceName: '',
  urlPattern: '',
  cronExpr: ''
})

const rules = {
  sourceType: [{ required: true, message: '请选择采集源类型', trigger: 'change' }],
  sourceName: [{ required: true, message: '请输入任务名称', trigger: 'blur' }]
}

async function openDialog(row) {
  editingId.value = row ? row.id : null
  if (row) {
    try {
      const detail = await getCrawlerTask(row.id)
      Object.assign(form, {
        sourceType: detail.sourceType || 'MOCK',
        sourceName: detail.sourceName || '',
        urlPattern: detail.urlPattern || '',
        cronExpr: detail.cronExpr || ''
      })
    } catch { /* fallback to row data */ }
  } else {
    Object.assign(form, { sourceType: 'MOCK', sourceName: '', urlPattern: '', cronExpr: '' })
  }
  dialogVisible.value = true
}

async function handleSave() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    if (editingId.value) {
      await updateCrawlerTask(editingId.value, { ...form })
      ElMessage.success('更新成功')
    } else {
      await createCrawlerTask({ ...form })
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadTasks()
  } finally {
    saving.value = false
  }
}

const urlPatternHint = computed(() => ({
  MOCK: '填 mock 数据文件名，如 mock-jobs.json',
  BOSS_ZHIPIN: '填参数串，如 query=Java&city=101010100&maxPages=3',
  ZHAOPIN: '填参数串，如 kw=Java&jl=530&maxPages=3（jl 是智联的城市编码）'
}[form.sourceType] || ''))

function sourceLabel(type) {
  return { MOCK: 'MOCK', BOSS_ZHIPIN: 'BOSS 直聘', ZHAOPIN: '智联招聘' }[type] || type
}

// RUNNING 是进行中，不是失败
function logStatusTag(status) {
  return { SUCCESS: 'success', FAILED: 'danger', RUNNING: 'warning' }[status] || 'info'
}

onMounted(() => {
  loadTasks()
  loadLogs()
})
</script>

<style scoped>
.form-tip { color: var(--app-ink-3); font-size: 12px; }
</style>
