<template>
  <div class="favorites-page">
    <div class="page-head">
      <h2 class="page-title">我的收藏</h2>
      <p class="page-sub">收藏的职位会保留在这里，方便随时回顾对比</p>
    </div>

    <div v-loading="loading">
      <el-empty v-if="!loading && list.length === 0" description="暂无收藏，去推荐页看看吧" />

      <div class="job-grid">
        <div v-for="job in list" :key="job.id" class="job-card"
          @click="$router.push(`/student/job/${job.id}`)">
          <div class="job-head">
            <div>
              <h3 class="job-title">{{ job.title }}</h3>
              <p class="job-company">{{ job.company }}</p>
            </div>
            <div class="job-salary">{{ salaryRange(job.salaryMin, job.salaryMax) }}</div>
          </div>
          <div class="job-chips">
            <span class="chip">{{ job.city }}</span>
            <span class="chip">{{ job.education || '学历不限' }}</span>
            <span v-if="job.industry" class="chip">{{ job.industry }}</span>
          </div>
          <div class="job-foot">
            <span class="date">{{ job.publishDate }}</span>
            <el-button size="small" type="danger" text @click.stop="handleUnfavorite(job.id)">
              取消收藏
            </el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getFavorites, unfavoriteJob } from '@/api/student'
import { toList } from '@/utils/list'
import { salaryRange } from '@/utils/format'
import { ElMessage } from 'element-plus'

const list = ref([])
const loading = ref(false)

async function loadFavorites() {
  loading.value = true
  try {
    list.value = toList(await getFavorites())
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
