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

      <!-- AI 解读：为什么把这个职位推给我 -->
      <div class="explain" v-if="explanation">
        <div class="explain-head">
          <span class="explain-title">AI 解读</span>
          <span v-if="!explanation.aiGenerated" class="explain-degraded">AI 未启用，以下为规则说明</span>
        </div>
        <p class="explain-body">{{ explanation.reply }}</p>
      </div>

      <!-- 来源说明：采集来的职位在本平台没有主人，不能站内投递 -->
      <el-alert
        v-if="!job.applicable" :closable="false" show-icon type="info"
        :title="originTitle" class="origin"
      >
        <template #default>
          <p class="origin-desc">{{ originDesc }}</p>
        </template>
      </el-alert>

      <div class="actions">
        <el-button
          :type="favorited ? 'warning' : 'default'"
          @click="toggleFavorite"
          :loading="favLoading"
        >{{ favorited ? '取消收藏' : '收藏职位' }}</el-button>
        <el-button :loading="explainLoading" @click="handleExplain">
          {{ explanation ? '重新解读' : 'AI 解读匹配度' }}
        </el-button>
        <el-button
          v-if="job.applicable"
          type="primary" @click="handleApply" :loading="applyLoading" :disabled="empStore.employed"
        >{{ empStore.employed ? '你已入职' : '投递简历' }}</el-button>
        <el-button
          v-else
          type="primary" @click="handleContact" :loading="contactLoading" :disabled="empStore.employed"
        >{{ empStore.employed ? '你已入职' : '自主联系' }}</el-button>
      </div>
    </el-card>

    <el-empty v-else-if="!loading" description="职位不存在" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import {
  getJobDetail, favoriteJob, unfavoriteJob, applyJob, getFavorites, explainMatch, contactJob
} from '@/api/student'
import { toList } from '@/utils/list'
import { parseSkills } from '@/utils/skills'
import { salaryRange } from '@/utils/format'
import { ElMessage } from 'element-plus'
import { useEmploymentStore } from '@/store/employment'

const empStore = useEmploymentStore()

const route = useRoute()
const job = ref({})
const loading = ref(false)
const favorited = ref(false)
const favLoading = ref(false)
const applyLoading = ref(false)
const explainLoading = ref(false)
const contactLoading = ref(false)
const explanation = ref(null)

const skillList = computed(() => parseSkills(job.value.skills))

/**
 * 职位来源三分法。后端的 applicable = (publisherId != null)，是唯一的可投递判据；
 * source 只用来决定「怎么向用户解释不能投」。
 */
const SOURCE_LABEL = { ZHAOPIN: '智联招聘', BOSS: 'Boss 直聘', MOCK: '模拟采集' }

const originTitle = computed(() =>
  `该职位来自${SOURCE_LABEL[job.value.source] || '外部渠道'}，非本平台企业发布`
)

const originDesc = computed(() =>
  job.value.source === 'MOCK'
    ? '这是用于演示与市场分析的模拟数据，没有真实的招聘方，因此不支持站内投递。'
      + '点「自主联系」会记录你的求职意向（教师可见），但没有可跳转的外部页面。'
    : '本平台只是采集并分析它，招聘方并不在这里收简历。'
      + '点「自主联系」会记录你的求职意向并打开原招聘页面，之后请在原网站上完成投递。'
)

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

/**
 * 自主联系：记录求职意向 + 打开原招聘页面（若有真实链接）。
 * 平台无法跟踪之后发生了什么，所以没有像投递那样的状态流转。
 */
async function handleContact() {
  contactLoading.value = true
  try {
    const { sourceUrl } = await contactJob(job.value.id)
    if (sourceUrl) {
      window.open(sourceUrl, '_blank', 'noopener,noreferrer')
      ElMessage.success('已记录求职意向，正在为你打开原招聘页面')
    } else {
      ElMessage.success('已记录求职意向。该职位为模拟数据，没有可跳转的外部页面')
    }
  } catch {
    /* 拦截器已提示 */
  } finally {
    contactLoading.value = false
  }
}

/** 按需生成：推荐列表一次 20 条，逐条调大模型太慢太贵，点开才生成 */
async function handleExplain() {
  explainLoading.value = true
  try {
    explanation.value = await explainMatch(job.value.id)
  } catch {
    /* 拦截器已提示 */
  } finally {
    explainLoading.value = false
  }
}

onMounted(() => { loadDetail(); empStore.refresh() })
</script>

<style scoped>
h2 { font-size: 23px; letter-spacing: -0.019em; font-weight: 600; color: var(--app-ink); margin: 0; }
.company-name { color: var(--app-ink-3); font-size: 14px; margin: 4px 0 0; }
.skills h4, .desc h4 { margin: 20px 0 10px; color: var(--app-ink); font-size: 15px; font-weight: 600; }
.skill-chips { display: flex; flex-wrap: wrap; gap: 6px; }
.desc p { color: var(--app-ink-2); line-height: 1.8; white-space: pre-line; }
.actions { margin-top: 28px; display: flex; gap: 12px; }

.origin { margin-top: 24px; }
.origin-desc { margin: 0; font-size: 13px; line-height: 1.7; }
.origin-link { display: inline-block; margin-top: 6px; font-size: 13px; color: var(--app-link); }

.explain { margin-top: 24px; padding: 14px 16px; border-radius: 10px; box-shadow: var(--app-hairline); }
.explain-head { display: flex; align-items: baseline; gap: 10px; }
.explain-title { font-size: 13px; font-weight: 600; color: var(--app-ink); }
.explain-degraded { font-size: 11px; color: var(--app-ember); }
.explain-body { margin: 8px 0 0; font-size: 13px; line-height: 1.8; color: var(--app-ink-2); }
</style>
