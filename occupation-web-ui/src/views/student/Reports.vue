<template>
  <div class="student-reports">
    <div class="page-head">
      <h2 class="page-title">我的报告</h2>
      <p class="page-sub">查看和下载本校已生成的就业分析报告</p>
    </div>

    <el-card>
      <el-table :data="records" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="templateName" label="报告名称" min-width="200">
          <template #default="{ row }">{{ row.templateName || '模板已删除' }}</template>
        </el-table-column>
        <el-table-column label="类型" width="120">
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
        <el-table-column label="操作" width="110">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'SUCCESS'" text type="primary" size="small"
              @click="handleDownload(row)"
            >下载报告</el-button>
            <span v-else class="muted">—</span>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && !records.length" description="暂无报告，请联系管理员生成" />

      <el-pagination
        v-if="total > size"
        v-model:current-page="page" :page-size="size"
        :total="total" layout="total, prev, pager, next"
        @current-change="loadRecords"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getReportRecords, downloadReport } from '@/api/admin'
import { toList, toTotal } from '@/utils/list'
import { formatTime } from '@/utils/format'
import { saveBlob } from '@/utils/download'

const records = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)

async function loadRecords() {
  loading.value = true
  try {
    const data = await getReportRecords({ pageNum: page.value, pageSize: size.value })
    records.value = toList(data)
    total.value = toTotal(data, records.value)
  } finally {
    loading.value = false
  }
}

async function handleDownload(row) {
  const ext = { PDF: 'pdf', WORD: 'docx', HTML: 'html' }[row.fileType] || 'bin'
  try {
    await saveBlob(downloadReport(row.id), `report-${row.id}.${ext}`)
  } catch { /* 拦截器已提示 */ }
}

// 后端状态是 SUCCESS，不是 COMPLETED —— 原来写错导致下载按钮永不出现
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
.muted { color: var(--app-ink-3); }
</style>
