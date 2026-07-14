<template>
  <div class="tool-page">
    <div class="tool-page-header">
      <el-button text @click="$router.push('/admin/tools')">
        <el-icon><ArrowLeft /></el-icon> 返回工具箱
      </el-button>
      <h2>数据导出中心</h2>
      <p class="tool-desc">基于现有平台接口导出 CSV 文件，不再使用模拟下载记录。</p>
    </div>

    <div class="export-form">
      <div class="form-row">
        <span class="form-label">数据类型</span>
        <el-radio-group v-model="form.dataType" @change="resetFields">
          <el-radio value="dashboard">数据看板</el-radio>
          <el-radio value="employment">就业分析</el-radio>
          <el-radio value="user">用户数据</el-radio>
          <el-radio value="report">报告记录</el-radio>
        </el-radio-group>
      </div>

      <div class="form-row template-row">
        <span class="form-label">可用模板</span>
        <div class="template-grid">
          <button
            v-for="tpl in currentTemplates"
            :key="tpl.name"
            type="button"
            class="template-card"
            @click="applyTemplate(tpl)"
          >
            <b>{{ tpl.name }}</b>
            <span>{{ tpl.desc }}</span>
          </button>
        </div>
      </div>

      <div class="form-row">
        <span class="form-label">导出格式</span>
        <el-radio-group v-model="form.format">
          <el-radio value="csv">CSV (.csv)</el-radio>
        </el-radio-group>
      </div>

      <div class="form-row">
        <span class="form-label">包含字段</span>
        <div class="field-checks">
          <el-checkbox
            v-for="field in currentFields"
            :key="field.key"
            v-model="field.checked"
          >
            {{ field.label }}
          </el-checkbox>
        </div>
      </div>

      <div class="tip-card">
        CSV 可直接用 Excel 打开。若后续需要 PDF/Excel 原生格式，可继续接入报告模块的后端下载能力。
      </div>

      <el-button type="primary" size="large" @click="exportData" :loading="exporting">
        <el-icon><Download /></el-icon> 导出数据
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { ArrowLeft, Download } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getDashboard, getEmployment, getReportRecords, getUsers } from '@/api/admin'

const form = reactive({
  dataType: 'dashboard',
  format: 'csv'
})

const exporting = ref(false)

const fieldSets = reactive({
  dashboard: [
    { key: 'category', label: '分类', checked: true },
    { key: 'name', label: '名称', checked: true },
    { key: 'value', label: '数值', checked: true }
  ],
  employment: [
    { key: 'category', label: '分类', checked: true },
    { key: 'name', label: '名称', checked: true },
    { key: 'value', label: '数值', checked: true }
  ],
  user: [
    { key: 'id', label: 'ID', checked: true },
    { key: 'username', label: '账号', checked: true },
    { key: 'realName', label: '姓名', checked: true },
    { key: 'role', label: '角色', checked: true },
    { key: 'status', label: '状态', checked: true }
  ],
  report: [
    { key: 'id', label: 'ID', checked: true },
    { key: 'name', label: '报告名称', checked: true },
    { key: 'category', label: '报告大类', checked: true },
    { key: 'fileType', label: '格式', checked: true },
    { key: 'status', label: '状态', checked: true },
    { key: 'createTime', label: '创建时间', checked: true }
  ]
})

const templateSets = {
  dashboard: [
    { name: '看板概览模板', desc: '适合汇总行业、城市、技能等核心指标', fields: ['category', 'name', 'value'] },
    { name: '指标核查模板', desc: '保留最小字段，便于快速检查分析结果', fields: ['category', 'value'] }
  ],
  employment: [
    { name: '就业漏斗模板', desc: '导出投递、响应、offer 等就业分析指标', fields: ['category', 'name', 'value'] },
    { name: '供需错配模板', desc: '用于观察城市、薪资、行业方向差异', fields: ['category', 'name', 'value'] }
  ],
  user: [
    { name: '账号盘点模板', desc: '导出账号、姓名、角色、启用状态', fields: ['id', 'username', 'realName', 'role', 'status'] },
    { name: '权限核查模板', desc: '聚焦账号与角色，适合管理员自查', fields: ['username', 'role', 'status'] }
  ],
  report: [
    { name: '报告归档模板', desc: '导出名称、大类、格式、状态和创建时间', fields: ['id', 'name', 'category', 'fileType', 'status', 'createTime'] },
    { name: '失败排查模板', desc: '聚焦报告状态，便于筛查生成异常', fields: ['name', 'status', 'createTime'] }
  ]
}

const currentFields = computed(() => fieldSets[form.dataType] || [])
const currentTemplates = computed(() => templateSets[form.dataType] || [])

function resetFields() {
  currentFields.value.forEach((field) => {
    field.checked = true
  })
}

function applyTemplate(template) {
  const selected = new Set(template.fields)
  currentFields.value.forEach((field) => {
    field.checked = selected.has(field.key)
  })
  ElMessage.success(`已套用「${template.name}」`)
}

async function exportData() {
  const fields = currentFields.value.filter((field) => field.checked)
  if (!fields.length) {
    ElMessage.warning('请至少选择一个导出字段')
    return
  }

  exporting.value = true
  try {
    const rows = await loadRows()
    if (!rows.length) {
      ElMessage.warning('当前没有可导出的数据')
      return
    }
    downloadCsv(toCsv(rows, fields), `${form.dataType}-${formatDate()}.csv`)
    ElMessage.success(`已导出 ${rows.length} 条数据`)
  } finally {
    exporting.value = false
  }
}

async function loadRows() {
  if (form.dataType === 'dashboard') {
    return flattenAnalysis(await getDashboard())
  }
  if (form.dataType === 'employment') {
    return flattenAnalysis(await getEmployment())
  }
  if (form.dataType === 'user') {
    return toList(await getUsers({ pageNum: 1, pageSize: 1000 }))
  }
  return toList(await getReportRecords({ pageNum: 1, pageSize: 1000 }))
}

function toList(data) {
  if (Array.isArray(data)) return data
  return data?.records || data?.list || data?.items || []
}

function flattenAnalysis(data) {
  if (!data || typeof data !== 'object') return []
  return Object.entries(data).flatMap(([category, value]) => normalizeRows(category, value))
}

function normalizeRows(category, value) {
  if (Array.isArray(value)) {
    return value.map((item, index) => ({
      category,
      name: item.name || item.label || item.dimensionValue || `第${index + 1}项`,
      value: item.value ?? item.count ?? item.metricValue ?? JSON.stringify(item)
    }))
  }
  if (value && typeof value === 'object') {
    return Object.entries(value).map(([name, item]) => ({
      category,
      name,
      value: typeof item === 'object' ? JSON.stringify(item) : item
    }))
  }
  return [{ category, name: category, value }]
}

function toCsv(rows, fields) {
  const header = fields.map((field) => escapeCell(field.label)).join(',')
  const body = rows.map((row) =>
    fields.map((field) => escapeCell(row[field.key] ?? '')).join(',')
  )
  return `\uFEFF${[header, ...body].join('\n')}`
}

function escapeCell(value) {
  const text = String(value).replaceAll('"', '""')
  return /[",\n]/.test(text) ? `"${text}"` : text
}

function downloadCsv(content, filename) {
  const blob = new Blob([content], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  link.click()
  URL.revokeObjectURL(url)
}

function formatDate() {
  const date = new Date()
  const pad = (num) => String(num).padStart(2, '0')
  return `${date.getFullYear()}${pad(date.getMonth() + 1)}${pad(date.getDate())}-${pad(date.getHours())}${pad(date.getMinutes())}`
}
</script>

<style scoped>
.tool-page { max-width: 800px; margin: 0 auto; padding: 16px 0 40px; }
.tool-page-header { margin-bottom: 24px; }
.tool-page-header h2 { font-size: 22px; font-weight: 600; color: var(--app-ink); margin: 12px 0 6px; }
.tool-desc { font-size: 14px; color: var(--app-ink-3); margin: 0; }

.export-form {
  background: #fff; border: 1px solid #e8eaed; border-radius: 12px; padding: 28px;
  display: flex; flex-direction: column; gap: 20px;
}
html.dark .export-form { background: #1e1f22; border-color: #2e3035; }

.form-row { display: flex; align-items: center; gap: 16px; }
.form-label { width: 80px; font-size: 14px; font-weight: 500; color: var(--app-ink-2); flex-shrink: 0; }
.field-checks { display: flex; flex-wrap: wrap; gap: 12px; }
.template-row { align-items: flex-start; }
.template-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px; flex: 1; }
.template-card { text-align: left; border: 1px solid #e8eaed; background: #f7f9fc; border-radius: 10px; padding: 12px; cursor: pointer; transition: all .2s ease; }
.template-card:hover { border-color: #5470c6; background: #f0f4ff; transform: translateY(-1px); }
html.dark .template-card { background: rgba(255,255,255,0.05); border-color: #2e3035; color: var(--app-ink); }
.template-card b { display: block; font-size: 13px; margin-bottom: 4px; }
.template-card span { display: block; font-size: 12px; line-height: 1.5; color: var(--app-ink-3); }
.tip-card { color: var(--app-ink-3); background: #f7f9fc; border-radius: 10px; padding: 12px 14px; font-size: 13px; line-height: 1.7; }
html.dark .tip-card { background: rgba(255,255,255,0.05); }
@media (max-width: 768px) { .form-row { align-items: flex-start; flex-direction: column; } .template-grid { grid-template-columns: 1fr; width: 100%; } }
</style>
