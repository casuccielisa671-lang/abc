<template>
  <div class="report-list">
    <div class="page-head with-actions">
      <div>
        <h2 class="page-title">报告生成管理</h2>
        <p class="page-sub">按模板生成就业分析报告并下载</p>
      </div>
      <div class="page-actions">
        <el-button type="primary" @click="openGenerateDialog">生成新报告</el-button>
      </div>
    </div>

    <el-card>
      <el-table :data="records" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="templateName" label="模板名称" min-width="180">
          <template #default="{ row }">{{ row.templateName || '模板已删除' }}</template>
        </el-table-column>
        <el-table-column label="报告类型" width="120">
          <template #default="{ row }"><span class="chip">{{ typeLabel(row.type) }}</span></template>
        </el-table-column>
        <el-table-column label="格式" width="90">
          <template #default="{ row }"><span class="chip">{{ row.fileType || '—' }}</span></template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="生成时间" width="150">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="失败原因" min-width="180">
          <template #default="{ row }">
            <span v-if="row.status === 'FAILED'" class="err" :title="row.errorMsg">{{ row.errorMsg }}</span>
            <span v-else>—</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'SUCCESS'" text type="primary" size="small"
              @click="handleDownload(row)"
            >下载</el-button>
            <el-button text type="danger" size="small" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && !records.length" description="暂无报告，点击右上角生成一份" />

      <el-pagination
        v-if="total > size"
        v-model:current-page="page" :page-size="size"
        :total="total" layout="total, prev, pager, next"
        @current-change="loadRecords"
      />
    </el-card>

    <!-- 生成报告对话框 -->
    <el-dialog v-model="generateVisible" title="生成新报告" width="500px">
      <el-form ref="genFormRef" :model="genForm" :rules="genRules" label-width="100px">
        <el-form-item label="选择模板" prop="templateId">
          <el-select v-model="genForm.templateId" placeholder="请选择报告模板" style="width:100%">
            <el-option v-for="t in templates" :key="t.id" :label="t.name" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="导出格式" prop="fileType">
          <el-select v-model="genForm.fileType" style="width:100%">
            <el-option label="PDF" value="PDF" />
            <el-option label="Word" value="WORD" />
            <el-option label="HTML" value="HTML" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="generateVisible = false">取消</el-button>
        <el-button type="primary" :loading="generating" @click="handleGenerate">生成</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getReportRecords, deleteReportRecord, downloadReport, generateReport, getReportTemplates } from '@/api/admin'
import { toList, toTotal } from '@/utils/list'
import { formatTime } from '@/utils/format'
import { saveBlob } from '@/utils/download'
import { ElMessage, ElMessageBox } from 'element-plus'

const records = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)

async function loadRecords() {
  loading.value = true
  try {
    // 后端读的是 pageNum/pageSize
    const data = await getReportRecords({ pageNum: page.value, pageSize: size.value })
    records.value = toList(data)
    total.value = toTotal(data, records.value)
  } finally {
    loading.value = false
  }
}

async function handleDelete(id) {
  try {
    await ElMessageBox.confirm('确定删除该报告记录及其文件吗？', '确认', { type: 'warning' })
  } catch {
    return
  }
  try {
    await deleteReportRecord(id)
    ElMessage.success('已删除')
    loadRecords()
  } catch { /* 拦截器已提示 */ }
}

async function handleDownload(row) {
  const ext = { PDF: 'pdf', WORD: 'docx', HTML: 'html' }[row.fileType] || 'bin'
  try {
    await saveBlob(downloadReport(row.id), `report-${row.id}.${ext}`)
  } catch { /* 拦截器已提示 */ }
}

// ---- 生成报告 ----
const generateVisible = ref(false)
const generating = ref(false)
const templates = ref([])
const genFormRef = ref(null)
// 字段名必须是 fileType：后端 GenerateReportDTO 读的是它，传 format 会被忽略并默认导出 HTML
const genForm = reactive({ templateId: null, fileType: 'PDF' })
const genRules = {
  templateId: [{ required: true, message: '请选择模板', trigger: 'change' }],
  fileType: [{ required: true, message: '请选择导出格式', trigger: 'change' }]
}

async function openGenerateDialog() {
  try {
    const data = await getReportTemplates({ pageNum: 1, pageSize: 100 })
    templates.value = toList(data)
    genForm.templateId = null
    genForm.fileType = 'PDF'
    generateVisible.value = true
  } catch { /* 拦截器已提示 */ }
}

async function handleGenerate() {
  const valid = await genFormRef.value.validate().catch(() => false)
  if (!valid) return
  generating.value = true
  try {
    await generateReport({ ...genForm })
    ElMessage.success('报告已生成')
    generateVisible.value = false
    loadRecords()
  } catch {
    // 生成是同步的，失败原因已由拦截器提示，同时会落一条 FAILED 记录
    loadRecords()
  } finally {
    generating.value = false
  }
}

function statusTag(status) {
  return { SUCCESS: 'success', FAILED: 'danger', GENERATING: 'warning', PENDING: 'info' }[status] || 'info'
}
function statusLabel(status) {
  return { SUCCESS: '已完成', FAILED: '失败', GENERATING: '生成中', PENDING: '排队中' }[status] || status
}
function typeLabel(type) {
  return { MONTHLY: '月度报告', QUARTERLY: '季度报告', YEARLY: '年度报告' }[type] || type || '—'
}

onMounted(loadRecords)
</script>

<style scoped>
.err {
  color: var(--app-danger);
  font-size: 12px;
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
