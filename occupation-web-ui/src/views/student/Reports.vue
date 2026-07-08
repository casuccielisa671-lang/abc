<template>
  <div class="student-reports">
    <h2>我的报告</h2>
    <p class="subtitle">查看和下载已生成的个人就业分析报告</p>

    <el-card>
      <el-table :data="records" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="templateName" label="报告名称" min-width="180" />
        <el-table-column label="格式" width="90">
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
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button v-if="row.status === 'COMPLETED'" size="small" type="success" @click="handleDownload(row.id)">
              下载报告
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="page" v-model:page-size="size"
        :total="total" layout="total, prev, pager, next"
        @current-change="loadRecords" style="margin-top:16px; justify-content:flex-end"
      />

      <el-empty v-if="!loading && records.length === 0" description="暂无报告，请联系管理员生成" style="margin-top:40px" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getReportRecords, downloadReport } from '@/api/admin'

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

function handleDownload(id) {
  window.open(downloadReport(id), '_blank')
}

onMounted(() => loadRecords())
</script>

<style scoped>
.student-reports h2 { margin-bottom: 4px; }
.subtitle { color: #909399; margin-bottom: 16px; }
</style>
