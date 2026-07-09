<template>
  <div class="report-template">
    <div class="page-head with-actions">
      <div>
        <h2 class="page-title">报告模板管理</h2>
        <p class="page-sub">维护各类报告的内容模板（Freemarker HTML，留空则使用系统内置默认模板）</p>
      </div>
      <div class="page-actions">
        <el-button type="primary" @click="openDialog()">新增模板</el-button>
      </div>
    </div>

    <el-card>
      <el-table :data="templates" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" label="模板名称" min-width="200" />
        <el-table-column label="报告类型" width="120">
          <template #default="{ row }"><span class="chip">{{ typeLabel(row.type) }}</span></template>
        </el-table-column>
        <el-table-column label="适用行业" width="140">
          <template #default="{ row }">{{ row.industry || '通用' }}</template>
        </el-table-column>
        <el-table-column label="模板内容" width="120">
          <template #default="{ row }">
            <span class="chip">{{ row.templateContent ? '自定义' : '内置默认' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="150">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="openDialog(row)">编辑</el-button>
            <el-button text type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && !templates.length" description="暂无模板" />

      <el-pagination
        v-if="total > size"
        v-model:current-page="page" :page-size="size"
        :total="total" layout="total, prev, pager, next"
        @current-change="loadTemplates"
      />
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑模板' : '新增模板'" width="640px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="模板名称" prop="name">
          <el-input v-model="form.name" placeholder="如：就业市场月度分析报告" />
        </el-form-item>
        <el-form-item label="报告类型" prop="type">
          <!-- 后端 @Pattern 只接受 MONTHLY/QUARTERLY/YEARLY -->
          <el-select v-model="form.type" style="width:100%">
            <el-option label="月度报告" value="MONTHLY" />
            <el-option label="季度报告" value="QUARTERLY" />
            <el-option label="年度报告" value="YEARLY" />
          </el-select>
        </el-form-item>
        <el-form-item label="适用行业" prop="industry">
          <el-input v-model="form.industry" placeholder="留空表示通用，如：互联网/IT" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" />
        </el-form-item>
        <el-form-item label="内容模板" prop="templateContent">
          <el-input
            v-model="form.templateContent" type="textarea" :rows="8"
            placeholder="Freemarker HTML，可用变量：${title} ${generateTime} ${aiSummary}，列表 skillHot / industryTop / cityDist。留空使用内置默认模板。"
          />
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
import { toList, toTotal } from '@/utils/list'
import { formatTime } from '@/utils/format'
import { ElMessage, ElMessageBox } from 'element-plus'

const templates = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)

async function loadTemplates() {
  loading.value = true
  try {
    const data = await getReportTemplates({ pageNum: page.value, pageSize: size.value })
    templates.value = toList(data)
    total.value = toTotal(data, templates.value)
  } finally {
    loading.value = false
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除模板「${row.name}」吗？`, '确认', { type: 'warning' })
  } catch {
    return
  }
  try {
    await deleteReportTemplate(row.id)
    ElMessage.success('已删除')
    loadTemplates()
  } catch { /* 拦截器已提示 */ }
}

const dialogVisible = ref(false)
const editingId = ref(null)
const saving = ref(false)
const formRef = ref(null)
// 字段名必须与 TemplateSaveDTO 一致：templateContent（不是 content），type 取 MONTHLY/QUARTERLY/YEARLY
const EMPTY = { name: '', type: 'MONTHLY', industry: '', templateContent: '', status: 1 }
const form = reactive({ ...EMPTY })

const rules = {
  name: [{ required: true, message: '请输入模板名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择报告类型', trigger: 'change' }]
}

function openDialog(row) {
  editingId.value = row ? row.id : null
  Object.assign(form, row
    ? {
        name: row.name || '',
        type: row.type || 'MONTHLY',
        industry: row.industry || '',
        templateContent: row.templateContent || '',
        status: row.status ?? 1
      }
    : { ...EMPTY })
  dialogVisible.value = true
}

async function handleSave() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    // 行业留空 = 通用，传空串会被当成一个叫「」的行业
    const payload = { ...form, industry: form.industry || null }
    if (editingId.value) {
      await updateReportTemplate(editingId.value, payload)
      ElMessage.success('更新成功')
    } else {
      await createReportTemplate(payload)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadTemplates()
  } catch {
    // 拦截器已提示
  } finally {
    saving.value = false
  }
}

function typeLabel(type) {
  return { MONTHLY: '月度报告', QUARTERLY: '季度报告', YEARLY: '年度报告' }[type] || type
}

onMounted(loadTemplates)
</script>
