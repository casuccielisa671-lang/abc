<template>
  <div class="hr-talents">
    <div class="page-head">
      <h2 class="page-title">人才浏览</h2>
      <p class="page-sub">浏览本校学生画像（脱敏展示，不含姓名与联系方式）</p>
    </div>

    <el-card>
      <div class="search-bar">
        <el-input
          v-model="searchKeyword" placeholder="搜索专业 / 技能" clearable
          style="width:240px" @keyup.enter="search" @clear="search"
        />
        <el-select v-model="searchEducation" placeholder="学历筛选" clearable style="width:140px" @change="search">
          <el-option label="专科" value="专科" />
          <el-option label="本科" value="本科" />
          <el-option label="硕士" value="硕士" />
          <el-option label="博士" value="博士" />
        </el-select>
        <el-button type="primary" @click="search">查询</el-button>
      </div>

      <el-table :data="talents" v-loading="loading" stripe>
        <el-table-column type="index" label="#" width="50" />
        <el-table-column label="学历" width="90">
          <template #default="{ row }"><span class="chip">{{ row.educationLevel || '—' }}</span></template>
        </el-table-column>
        <el-table-column prop="major" label="专业" min-width="140" />
        <el-table-column label="技能" min-width="220">
          <template #default="{ row }">
            <div class="chip-row">
              <span v-for="sk in parseSkills(row.skills)" :key="sk" class="chip">{{ sk }}</span>
              <span v-if="!parseSkills(row.skills).length">—</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="期望城市" width="110">
          <template #default="{ row }">{{ row.expectedCity || '—' }}</template>
        </el-table-column>
        <el-table-column label="期望薪资" width="140">
          <template #default="{ row }">
            <span class="salary-text">{{ salaryRange(row.expectedSalaryMin, row.expectedSalaryMax, '未填写') }}</span>
          </template>
        </el-table-column>
        <el-table-column label="求职状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.applyCount > 0 ? 'success' : 'info'" size="small">
              {{ row.applyCount > 0 ? '活跃' : '观望' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="showDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && !talents.length" description="没有符合条件的人才" />

      <el-pagination
        v-if="total > size"
        v-model:current-page="page" :page-size="size"
        :total="total" layout="total, prev, pager, next"
        @current-change="loadTalents"
      />
    </el-card>

    <!-- 人才详情对话框 -->
    <el-dialog v-model="detailVisible" title="人才详情（脱敏）" width="520px">
      <el-descriptions v-if="currentTalent" :column="2" border>
        <el-descriptions-item label="学历">{{ currentTalent.educationLevel || '—' }}</el-descriptions-item>
        <el-descriptions-item label="专业">{{ currentTalent.major || '—' }}</el-descriptions-item>
        <el-descriptions-item label="期望城市">{{ currentTalent.expectedCity || '—' }}</el-descriptions-item>
        <el-descriptions-item label="期望行业">{{ currentTalent.expectedIndustry || '—' }}</el-descriptions-item>
        <el-descriptions-item label="期望薪资" :span="2">
          <span class="salary-text">
            {{ salaryRange(currentTalent.expectedSalaryMin, currentTalent.expectedSalaryMax, '未填写') }}
          </span>
        </el-descriptions-item>
        <el-descriptions-item label="技能" :span="2">
          <div class="chip-row">
            <span v-for="sk in parseSkills(currentTalent.skills)" :key="sk" class="chip">{{ sk }}</span>
            <span v-if="!parseSkills(currentTalent.skills).length">—</span>
          </div>
        </el-descriptions-item>
        <el-descriptions-item label="浏览职位数">{{ currentTalent.viewCount }}</el-descriptions-item>
        <el-descriptions-item label="投递次数">{{ currentTalent.applyCount }}</el-descriptions-item>
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
import { toList, toTotal } from '@/utils/list'
import { parseSkills } from '@/utils/skills'
import { salaryRange } from '@/utils/format'

const talents = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)
const searchKeyword = ref('')
const searchEducation = ref('')

const detailVisible = ref(false)
const currentTalent = ref(null)

async function loadTalents() {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value }
    if (searchKeyword.value) params.keyword = searchKeyword.value
    if (searchEducation.value) params.education = searchEducation.value
    const data = await getTalents(params)
    talents.value = toList(data)
    total.value = toTotal(data, talents.value)
  } finally {
    loading.value = false
  }
}

/** 改变筛选条件后回到第一页，否则会停在一个可能已不存在的页码上 */
function search() {
  page.value = 1
  loadTalents()
}

function showDetail(row) {
  currentTalent.value = row
  detailVisible.value = true
}

onMounted(loadTalents)
</script>

<style scoped>
.search-bar { display: flex; align-items: center; gap: 12px; margin-bottom: 20px; flex-wrap: wrap; }
.chip-row { display: flex; flex-wrap: wrap; gap: 4px; }
</style>
