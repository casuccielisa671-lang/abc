<template>
  <div class="favorites-page">
    <h2>我的收藏</h2>

    <div v-loading="loading">
      <el-empty v-if="!loading && list.length === 0" description="暂无收藏，去推荐页看看吧" />

      <div class="job-grid">
        <el-card v-for="job in list" :key="job.id" class="job-card" shadow="hover"
          @click="$router.push(`/student/job/${job.id}`)">
          <div class="card-header">
            <h3>{{ job.title }}</h3>
            <span class="salary">{{ (job.salaryMin / 1000).toFixed(0) }}k-{{ (job.salaryMax / 1000).toFixed(0) }}k</span>
          </div>
          <p class="company">{{ job.company }}</p>
          <div class="tags">
            <el-tag size="small">{{ job.city }}</el-tag>
            <el-tag size="small" type="success">{{ job.education }}</el-tag>
            <el-tag size="small" type="info">{{ job.industry }}</el-tag>
          </div>
          <div class="footer-actions">
            <span class="date">{{ job.publishDate }}</span>
            <el-button size="small" type="danger" text @click.stop="handleUnfavorite(job.id)">
              取消收藏
            </el-button>
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getFavorites, unfavoriteJob } from '@/api/student'
import { ElMessage } from 'element-plus'

const list = ref([])
const loading = ref(false)

async function loadFavorites() {
  loading.value = true
  try {
    list.value = await getFavorites()
  } finally {
    loading.value = false
  }
}

async function handleUnfavorite(id) {
  try {
    await unfavoriteJob(id)
    ElMessage.success('已取消收藏')
    list.value = list.value.filter(j => j.id !== id)
  } catch { /* handled */ }
}

onMounted(() => loadFavorites())
</script>

<style scoped>
.job-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(360px, 1fr)); gap: 16px; }
.job-card { cursor: pointer; transition: transform 0.2s; }
.job-card:hover { transform: translateY(-2px); }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.card-header h3 { margin: 0; font-size: 16px; }
.salary { color: #E6A23C; font-weight: bold; }
.company { color: #606266; margin: 8px 0; }
.tags { display: flex; gap: 6px; margin: 8px 0; }
.footer-actions { display: flex; justify-content: space-between; align-items: center; margin-top: 12px; }
.date { color: #C0C4CC; font-size: 12px; }
</style>
