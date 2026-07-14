<template>
  <div class="favorites-page">
    <div class="page-head" v-if="!embedded">
      <h2 class="page-title">我的收藏</h2>
      <p class="page-sub">收藏的职位会保留在这里，方便随时回顾对比</p>
    </div>

    <div v-loading="loading">
      <el-empty
        v-if="!loading && shownList.length === 0"
        :description="list.length ? '没有匹配的收藏' : '暂无收藏，去「可投递岗位」或「市场参考」看看吧'"
      />

      <div class="job-grid">
        <div v-for="job in shownList" :key="job.id" class="job-card"
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
import { ref, computed, onMounted } from 'vue'
import { getFavorites, unfavoriteJob } from '@/api/student'
import { toList } from '@/utils/list'
import { salaryRange } from '@/utils/format'
import { ElMessage } from 'element-plus'

// embedded=true 时隐藏自身大标题，供「职位信息」中心以标签页嵌入
const props = defineProps({
  embedded: { type: Boolean, default: false },
  filter: { type: String, default: '' }   // 「职位信息」中心传入的搜索关键词
})

const list = ref([])

/** 按关键词过滤收藏（职位/公司/城市） */
const shownList = computed(() => {
  const q = (props.filter || '').trim().toLowerCase()
  if (!q) return list.value
  return list.value.filter(j =>
    [j.title, j.company, j.city].some(v => (v || '').toLowerCase().includes(q)))
})
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
