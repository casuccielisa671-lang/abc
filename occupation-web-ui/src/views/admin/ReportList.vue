<template>
  <div class="report-list">
    <div class="page-header">
      <h2>报告生成管理</h2>
      <el-button type="primary" @click="openGenerateDialog">生成新报告</el-button>
    </div>

    <el-card>
      <el-table :data="records" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="templateName" label="模板名称" min-width="150" />
        <el-table-column label="报告类型" width="120">
          <template #default="{ row }">
            <el-tag :type="typeTag(row.type)" size="small">{{ typeLabel(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="format" label="格式" width="90">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ row.format?.toUpperCase() || 'PDF' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'COMPLETED' ? 'success' : 'warning'" size="small">
              {{ row.status === 'COMPLETED' ? '已完成' : '生成中' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="生成时间" width="170" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'COMPLETED'" size="small" type="success" @click="handleDownload(row.id)">
              下载
            </el-button>
            <el-button size="small" type="danger" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="page" v-model:page-size="size"
        :total="total" layout="total, prev, pager, next"
        @current-change="loadRecords" style="margin-top:16px; justify-content:flex-end"
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
        <el-form-item label="导出格式" prop="format">
          <el-select v-model="genForm.format" style="width:100%">
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
import { ElMessage, ElMessageBox } from 'element-plus'

const records = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)

async function loadRecords() {
  loading.value = true
  try {
    const data = await getReportRecords({ page: page.value, size: size.value })
    records.value = data.records || data.list || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

async function handleDelete(id) {
  await ElMessageBox.confirm('确定删除该报告记录吗？', '确认', { type: 'warning' })
  try {
    await deleteReportRecord(id)
    ElMessage.success('已删除')
    loadRecords()
  } catch { /* handled */ }
}

function handleDownload(id) {
  window.open(downloadReport(id), '_blank')
}

// 生成报告
const generateVisible = ref(false)
const generating = ref(false)
const templates = ref([])
const genFormRef = ref(null)
const genForm = reactive({ templateId: null, format: 'PDF' })
const genRules = {
  templateId: [{ required: true, message: '请选择模板', trigger: 'change' }],
  format: [{ required: true, message: '请选择导出格式', trigger: 'change' }]
}

async function openGenerateDialog() {
  try {
    const data = await getReportTemplates({ page: 1, size: 100 })
    templates.value = data.records || data.list || []
    genForm.templateId = null
    genForm.format = 'PDF'
    generateVisible.value = true
  } catch { /* handled */ }
}

async function handleGenerate() {
  const valid = await genFormRef.value.validate().catch(() => false)
  if (!valid) return
  generating.value = true
  try {
    await generateReport({ ...genForm })
    ElMessage.success('报告生成任务已触发，请稍后刷新查看')
    generateVisible.value = false
    loadRecords()
  } finally {
    generating.value = false
  }
}

function typeTag(type) {
  return { STUDENT: 'success', CLASS: 'warning', INDUSTRY: 'info', SCHOOL: 'danger' }[type] || ''
}
function typeLabel(type) {
  return { STUDENT: '学生个人报告', CLASS: '班级汇总报告', INDUSTRY: '行业分析报告', SCHOOL: '学校年度报告' }[type] || type
}

onMounted(() => loadRecords())
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-header h2 { margin: 0; }
</style>
