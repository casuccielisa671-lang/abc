<template>
  <div class="report-template">
    <div class="page-header">
      <h2>报告模板管理</h2>
      <el-button type="primary" @click="openDialog()">新增模板</el-button>
    </div>

    <el-card>
      <el-table :data="templates" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" label="模板名称" min-width="150" />
        <el-table-column prop="type" label="报告类型" width="120">
          <template #default="{ row }">
            <el-tag :type="typeTag(row.type)" size="small">{{ typeLabel(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="industry" label="适用行业" width="120" />
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openDialog(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="page" v-model:page-size="size"
        :total="total" layout="total, prev, pager, next"
        @current-change="loadTemplates" style="margin-top:16px; justify-content:flex-end"
      />
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑模板' : '新增模板'" width="600px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="模板名称" prop="name">
          <el-input v-model="form.name" placeholder="如：Java开发就业报告" />
        </el-form-item>
        <el-form-item label="报告类型" prop="type">
          <el-select v-model="form.type" style="width:100%">
            <el-option label="学生个人报告" value="STUDENT" />
            <el-option label="班级汇总报告" value="CLASS" />
            <el-option label="行业分析报告" value="INDUSTRY" />
            <el-option label="学校年度报告" value="SCHOOL" />
          </el-select>
        </el-form-item>
        <el-form-item label="适用行业" prop="industry">
          <el-input v-model="form.industry" placeholder="如：互联网/IT、金融、教育" />
        </el-form-item>
        <el-form-item label="内容模板" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="8"
            placeholder="请输入报告内容模板，支持 Freemarker 语法变量" />
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
import { getReportTemplates, createReportTemplate, updateReportTemplate, deleteReportTemplate } from '@/api/admin'
import { ElMessage, ElMessageBox } from 'element-plus'

const templates = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)

async function loadTemplates() {
  loading.value = true
  try {
    const data = await getReportTemplates({ page: page.value, size: size.value })
    templates.value = data.records || data.list || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

async function handleDelete(id) {
  await ElMessageBox.confirm('确定删除该模板吗？', '确认', { type: 'warning' })
  try {
    await deleteReportTemplate(id)
    ElMessage.success('已删除')
    loadTemplates()
  } catch { /* handled */ }
}

const dialogVisible = ref(false)
const editingId = ref(null)
const saving = ref(false)
const formRef = ref(null)
const form = reactive({
  name: '', type: 'STUDENT', industry: '', content: ''
})

const rules = {
  name: [{ required: true, message: '请输入模板名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择报告类型', trigger: 'change' }],
  industry: [{ required: true, message: '请输入适用行业', trigger: 'blur' }],
  content: [{ required: true, message: '请输入内容模板', trigger: 'blur' }]
}

function openDialog(row) {
  editingId.value = row ? row.id : null
  if (row) {
    Object.assign(form, {
      name: row.name || '',
      type: row.type || 'STUDENT',
      industry: row.industry || '',
      content: row.content || ''
    })
  } else {
    Object.assign(form, { name: '', type: 'STUDENT', industry: '', content: '' })
  }
  dialogVisible.value = true
}

async function handleSave() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    if (editingId.value) {
      await updateReportTemplate(editingId.value, { ...form })
      ElMessage.success('更新成功')
    } else {
      await createReportTemplate({ ...form })
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadTemplates()
  } finally {
    saving.value = false
  }
}

function typeTag(type) {
  return { STUDENT: 'success', CLASS: 'warning', INDUSTRY: 'info', SCHOOL: 'danger' }[type] || ''
}
function typeLabel(type) {
  return { STUDENT: '学生个人报告', CLASS: '班级汇总报告', INDUSTRY: '行业分析报告', SCHOOL: '学校年度报告' }[type] || type
}

onMounted(() => loadTemplates())
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-header h2 { margin: 0; }
</style>
