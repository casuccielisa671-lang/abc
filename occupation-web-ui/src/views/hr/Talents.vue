<template>
  <div class="hr-talents">
    <h2>人才浏览</h2>
    <p class="subtitle">浏览本校学生画像（脱敏展示，不含真实姓名等隐私信息）</p>

    <el-card>
      <div class="search-bar">
        <el-input v-model="searchKeyword" placeholder="搜索专业/技能" clearable style="width:240px"
          @keyup.enter="loadTalents" />
        <el-select v-model="searchEducation" placeholder="学历筛选" clearable style="width:140px; margin-left:12px"
          @change="loadTalents">
          <el-option label="大专" value="大专" />
          <el-option label="本科" value="本科" />
          <el-option label="硕士" value="硕士" />
          <el-option label="博士" value="博士" />
        </el-select>
        <el-button type="primary" @click="loadTalents" style="margin-left:12px">查询</el-button>
      </div>

      <el-table :data="talents" v-loading="loading" stripe>
        <el-table-column type="index" label="#" width="50" />
        <el-table-column label="学历" width="90">
          <template #default="{ row }">
            <el-tag size="small">{{ row.education || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="major" label="专业" min-width="130" />
        <el-table-column label="技能" min-width="220">
          <template #default="{ row }">
            <el-tag v-for="sk in parseSkills(row.skills)" :key="sk" size="small" style="margin:2px">{{ sk }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="期望城市" width="110">
          <template #default="{ row }">{{ row.intendedCity || '-' }}</template>
        </el-table-column>
        <el-table-column label="期望薪资" width="140">
          <template #default="{ row }">
            <template v-if="row.expectedSalaryMin">
              {{ (row.expectedSalaryMin / 1000).toFixed(0) }}k - {{ (row.expectedSalaryMax / 1000).toFixed(0) }}k
            </template>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="求职状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.applyCount > 0 ? 'success' : 'info'" size="small">
              {{ row.applyCount > 0 ? '活跃' : '观望' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="showDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="page" v-model:page-size="size"
        :total="total" layout="total, prev, pager, next"
        @current-change="loadTalents" style="margin-top:16px; justify-content:flex-end"
      />
    </el-card>

    <!-- 人才详情对话框 -->
    <el-dialog v-model="detailVisible" title="人才详情（脱敏）" width="500px">
      <el-descriptions v-if="currentTalent" :column="2" border>
        <el-descriptions-item label="学历">{{ currentTalent.education || '-' }}</el-descriptions-item>
        <el-descriptions-item label="专业">{{ currentTalent.major || '-' }}</el-descriptions-item>
        <el-descriptions-item label="期望城市">{{ currentTalent.intendedCity || '-' }}</el-descriptions-item>
        <el-descriptions-item label="期望薪资">
          <template v-if="currentTalent.expectedSalaryMin">
            {{ (currentTalent.expectedSalaryMin / 1000).toFixed(0) }}k - {{ (currentTalent.expectedSalaryMax / 1000).toFixed(0) }}k
          </template>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="技能" :span="2">
          <el-tag v-for="sk in parseSkills(currentTalent.skills)" :key="sk" size="small" style="margin:2px">{{ sk }}</el-tag>
          <span v-if="!currentTalent.skills">-</span>
        </el-descriptions-item>
        <el-descriptions-item label="浏览职位数">{{ currentTalent.viewCount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="投递次数">{{ currentTalent.applyCount || 0 }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getTalents } from '@/api/student'

const talents = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)
const searchKeyword = ref('')
const searchEducation = ref('')

const detailVisible = ref(false)
const currentTalent = ref(null)

function parseSkills(skills) {
  if (!skills) return []
  if (Array.isArray(skills)) return skills
  try { return JSON.parse(skills) } catch { return skills.split(',').map(s => s.trim()).filter(Boolean) }
}

async function loadTalents() {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value }
    if (searchKeyword.value) params.keyword = searchKeyword.value
    if (searchEducation.value) params.education = searchEducation.value
    const data = await getTalents(params)
    talents.value = data.records || data.list || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function showDetail(row) {
  currentTalent.value = row
  detailVisible.value = true
}

onMounted(() => loadTalents())
</script>

<style scoped>
.hr-talents h2 { margin-bottom: 4px; }
.subtitle { color: #909399; margin-bottom: 16px; }
.search-bar { display: flex; align-items: center; margin-bottom: 16px; }
</style>
