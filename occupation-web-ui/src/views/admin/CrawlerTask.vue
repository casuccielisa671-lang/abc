<template>
  <div class="crawler-page">
    <div class="page-header">
      <h2>采集任务管理</h2>
      <div class="actions">
        <el-button type="success" @click="handleMockCrawl">Mock 模拟采集</el-button>
        <el-button type="primary" @click="openDialog()">新增任务</el-button>
      </div>
    </div>

    <!-- 任务表格 -->
    <el-card>
      <el-table :data="tasks" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="sourceName" label="任务名称" min-width="160" />
        <el-table-column prop="sourceType" label="采集源" width="130">
          <template #default="{ row }">
            <el-tag :type="sourceTag(row.sourceType)" size="small">{{ row.sourceType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="cronExpr" label="Cron 表达式" width="150" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.statusText }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status !== 1"
              size="small" type="success"
              @click="handleStart(row.id)"
            >启动</el-button>
            <el-button
              v-else
              size="small" type="warning"
              @click="handleStop(row.id)"
            >停止</el-button>
            <el-button size="small" @click="openDialog(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="page" v-model:page-size="size"
        :total="total" layout="total, prev, pager, next"
        @current-change="loadTasks" style="margin-top:16px; justify-content:flex-end"
      />
    </el-card>

    <!-- 采集日志 -->
    <el-card style="margin-top:16px">
      <template #header>采集日志</template>
      <el-table :data="logs" v-loading="logLoading" stripe size="small">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="taskId" label="任务ID" width="80" />
        <el-table-column prop="startTime" label="开始时间" width="170" />
        <el-table-column prop="endTime" label="结束时间" width="170" />
        <el-table-column prop="recordCount" label="采集条数" width="90" />
        <el-table-column prop="duration" label="耗时" width="80" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'SUCCESS' ? 'success' : 'danger'" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="errorMsg" label="错误信息" min-width="150" show-overflow-tooltip />
      </el-table>
      <el-pagination
        v-model:current-page="logPage" v-model:page-size="logSize"
        :total="logTotal" layout="total, prev, pager, next"
        @current-change="loadLogs" style="margin-top:12px; justify-content:flex-end"
      />
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑任务' : '新增任务'" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="采集源类型" prop="sourceType">
          <el-select v-model="form.sourceType" style="width:100%">
            <el-option label="MOCK（模拟数据）" value="MOCK" />
            <el-option label="BOSS直聘" value="BOSS_ZHIPIN" />
            <el-option label="前程无忧" value="ZHAOPIN" />
          </el-select>
        </el-form-item>
        <el-form-item label="任务名称" prop="sourceName">
          <el-input v-model="form.sourceName" placeholder="如：BOSS直聘-Java开发" />
        </el-form-item>
        <el-form-item label="URL/数据文件" prop="urlPattern">
          <el-input v-model="form.urlPattern" placeholder="MOCK 填 mock-jobs.json，真实采集填 URL" />
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
import { ref, reactive, onMounted } from 'vue'
import {
  getCrawlerTasks, getCrawlerTask, createCrawlerTask, updateCrawlerTask,
  deleteCrawlerTask, startCrawlerTask, stopCrawlerTask, mockCrawl,
  getCrawlerLogs
} from '@/api/admin'
import { ElMessage, ElMessageBox } from 'element-plus'

// ========== 任务列表 ==========
const tasks = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)

async function loadTasks() {
  loading.value = true
  try {
    const data = await getCrawlerTasks({ page: page.value, size: size.value })
    tasks.value = data.records || data.list || []
    total.value = data.total || 0
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
    logs.value = data.records || data.list || []
    logTotal.value = data.total || 0
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

function sourceTag(type) {
  return type === 'MOCK' ? 'info' : type === 'BOSS_ZHIPIN' ? 'warning' : ''
}

onMounted(() => {
  loadTasks()
  loadLogs()
})
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-header h2 { margin: 0; }
.actions { display: flex; gap: 8px; }
</style>
