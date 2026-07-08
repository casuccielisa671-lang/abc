<template>
  <div class="student-home">
    <h2>职位推荐</h2>
    <p class="subtitle">根据你的画像智能匹配，得分越高越适合你</p>

    <div v-loading="loading">
      <el-empty v-if="!loading && list.length === 0" description="暂无推荐职位，请先完善个人画像" />

      <div class="job-grid">
        <el-card
          v-for="item in list" :key="item.job?.id"
          class="job-card" shadow="hover"
          @click="goDetail(item.job?.id)"
        >
          <div class="card-header">
            <h3>{{ item.job?.title }}</h3>
            <el-tag type="warning" size="small">匹配 {{ item.score }} 分</el-tag>
          </div>
          <p class="company">{{ item.job?.company }}</p>
          <div class="tags">
            <el-tag size="small">{{ item.job?.city }}</el-tag>
            <el-tag size="small" type="success">{{ item.job?.salaryMin / 1000 }}k-{{ item.job?.salaryMax / 1000 }}k</el-tag>
            <el-tag size="small" type="info">{{ item.job?.education }}</el-tag>
          </div>
          <p class="reason" v-if="item.matchReason">{{ item.matchReason }}</p>
          <div class="missing" v-if="item.missingSkills?.length">
            <span class="missing-label">建议学习：</span>
            <el-tag v-for="sk in item.missingSkills" :key="sk" size="small" type="danger" effect="plain">{{ sk }}</el-tag>
          </div>
          <div class="card-footer">
            <span class="date">{{ item.job?.publishDate }}</span>
            <el-button type="primary" size="small" @click.stop="goDetail(item.job?.id)">查看详情</el-button>
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getRecommend } from '@/api/student'

const router = useRouter()
const list = ref([])
const loading = ref(false)

function goDetail(id) {
  router.push(`/student/job/${id}`)
}

onMounted(async () => {
  loading.value = true
  try {
    list.value = await getRecommend(20)
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.student-home h2 { margin-bottom: 4px; }
.subtitle { color: #909399; margin-bottom: 16px; }
.job-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(380px, 1fr)); gap: 16px; }
.job-card { cursor: pointer; transition: transform 0.2s; }
.job-card:hover { transform: translateY(-2px); }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.card-header h3 { margin: 0; font-size: 16px; }
.company { color: #606266; margin: 8px 0; }
.tags { display: flex; gap: 6px; flex-wrap: wrap; margin-bottom: 8px; }
.reason { color: #67C23A; font-size: 13px; margin: 6px 0; }
.missing { display: flex; align-items: center; gap: 4px; flex-wrap: wrap; margin: 6px 0; }
.missing-label { font-size: 12px; color: #F56C6C; }
.card-footer { display: flex; justify-content: space-between; align-items: center; margin-top: 10px; }
.date { color: #c0c4cc; font-size: 12px; }
</style>
