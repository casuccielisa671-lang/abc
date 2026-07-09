<template>
  <div class="job-detail" v-loading="loading">
    <el-page-header @back="$router.back()" :content="job.title || '职位详情'" style="margin-bottom:20px" />

    <el-card v-if="job.id">
      <h2>{{ job.title }}</h2>
      <p class="company-name">{{ job.company }}</p>

      <el-descriptions :column="3" border style="margin:20px 0">
        <el-descriptions-item label="城市">{{ job.city }}</el-descriptions-item>
        <el-descriptions-item label="行业">{{ job.industry }}</el-descriptions-item>
        <el-descriptions-item label="学历要求">{{ job.education }}</el-descriptions-item>
        <el-descriptions-item label="经验要求">{{ job.experience || '不限' }}</el-descriptions-item>
        <el-descriptions-item label="薪资范围">
          <span class="salary-text">{{ salaryRange(job.salaryMin, job.salaryMax) }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="发布日期">{{ job.publishDate }}</el-descriptions-item>
      </el-descriptions>

      <div class="skills" v-if="skillList.length">
        <h4>技能要求</h4>
        <div class="skill-chips">
          <span v-for="sk in skillList" :key="sk" class="chip">{{ sk }}</span>
        </div>
      </div>

      <div class="desc" v-if="job.description">
        <h4>职位描述</h4>
        <p>{{ job.description }}</p>
      </div>

      <div class="actions">
        <el-button
          :type="favorited ? 'warning' : 'default'"
          @click="toggleFavorite"
          :loading="favLoading"
        >{{ favorited ? '取消收藏' : '收藏职位' }}</el-button>
        <el-button type="primary" @click="handleApply" :loading="applyLoading">投递简历</el-button>
      </div>
    </el-card>

    <el-empty v-else-if="!loading" description="职位不存在" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getJobDetail, favoriteJob, unfavoriteJob, applyJob, getFavorites } from '@/api/student'
import { toList } from '@/utils/list'
import { parseSkills } from '@/utils/skills'
import { salaryRange } from '@/utils/format'
import { ElMessage } from 'element-plus'

const route = useRoute()
const job = ref({})
const loading = ref(false)
const favorited = ref(false)
const favLoading = ref(false)
const applyLoading = ref(false)

const skillList = computed(() => parseSkills(job.value.skills))

async function loadDetail() {
  loading.value = true
  try {
    const id = route.params.id
    job.value = await getJobDetail(id)
    try {
      const favs = toList(await getFavorites())
      favorited.value = favs.some(f => f.id === job.value.id)
    } catch { /* 收藏状态取不到不影响详情展示 */ }
  } finally {
    loading.value = false
  }
}

async function toggleFavorite() {
  favLoading.value = true
  try {
    if (favorited.value) {
      await unfavoriteJob(job.value.id)
      ElMessage.success('已取消收藏')
    } else {
      await favoriteJob(job.value.id)
      ElMessage.success('已收藏')
    }
    favorited.value = !favorited.value
  } finally {
    favLoading.value = false
  }
}

async function handleApply() {
  applyLoading.value = true
  try {
    await applyJob(job.value.id)
    ElMessage.success('投递成功！')
  } finally {
    applyLoading.value = false
  }
}

onMounted(() => loadDetail())
</script>

<style scoped>
h2 { font-size: 23px; letter-spacing: -0.019em; font-weight: 600; color: var(--app-ink); margin: 0; }
.company-name { color: var(--app-ink-3); font-size: 14px; margin: 4px 0 0; }
.skills h4, .desc h4 { margin: 20px 0 10px; color: var(--app-ink); font-size: 15px; font-weight: 600; }
.skill-chips { display: flex; flex-wrap: wrap; gap: 6px; }
.desc p { color: var(--app-ink-2); line-height: 1.8; white-space: pre-line; }
.actions { margin-top: 28px; display: flex; gap: 12px; }
</style>
