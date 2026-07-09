<template>
  <div class="student-home">
    <div class="page-head">
      <h2 class="page-title">职位推荐</h2>
      <p class="page-sub">根据你的画像智能匹配，得分越高越适合你</p>
    </div>

    <div v-loading="loading">
      <el-empty v-if="!loading && list.length === 0" description="暂无推荐职位，请先完善个人画像" />

      <div class="job-grid">
        <div
          v-for="item in list" :key="item.job?.id"
          class="job-card"
          @click="goDetail(item.job?.id)"
        >
          <div class="job-head">
            <div>
              <h3 class="job-title">{{ item.job?.title }}</h3>
              <p class="job-company">{{ item.job?.company }}</p>
            </div>
            <div class="job-score">
              <div class="num">{{ item.score }}</div>
              <div class="cap">匹配分</div>
            </div>
          </div>

          <div class="job-salary">{{ salaryRange(item.job?.salaryMin, item.job?.salaryMax) }}</div>

          <div class="job-chips">
            <span class="chip">{{ item.job?.city }}</span>
            <span class="chip">{{ item.job?.education || '学历不限' }}</span>
            <span v-if="item.job?.industry" class="chip">{{ item.job.industry }}</span>
          </div>

          <p class="job-reason" v-if="item.matchReason">{{ item.matchReason }}</p>

          <div class="job-learn" v-if="item.missingSkills?.length">
            建议学习
            <span v-for="sk in item.missingSkills" :key="sk" class="chip learn">{{ sk }}</span>
          </div>

          <div class="job-foot">
            <span class="date">{{ item.job?.publishDate }}</span>
            <el-button type="primary" text size="small" @click.stop="goDetail(item.job?.id)">查看详情 →</el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getRecommend } from '@/api/student'
import { toList } from '@/utils/list'
import { salaryRange } from '@/utils/format'

const router = useRouter()
const list = ref([])
const loading = ref(false)

function goDetail(id) {
  router.push(`/student/job/${id}`)
}

onMounted(async () => {
  loading.value = true
  try {
    // 未填画像时后端抛业务异常，拦截器已提示，这里保持空列表并展示引导文案
    list.value = toList(await getRecommend(20))
  } catch {
    list.value = []
  } finally {
    loading.value = false
  }
})
</script>
