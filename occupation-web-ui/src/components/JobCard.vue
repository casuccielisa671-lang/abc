<template>
  <div class="job-card modern-job-card" :class="{ 'market-job': !job.applicable }" @click="$emit('open')">
    <div class="match-pill">{{ matchLabel }}</div>
    <div class="job-head">
      <div class="job-main">
        <h3 class="job-title">{{ job.title }}</h3>
        <p class="job-company">
          {{ job.company }}
          <span class="company-verified" />
        </p>
        <div class="job-salary-line">
          <span>{{ salaryRange(job.salaryMin, job.salaryMax) }}</span>
          <em v-if="job.city">{{ job.city }}</em>
        </div>
      </div>
      <div class="job-score-ring" :style="scoreStyle">
        <span>{{ score }}</span>
        <small>匹配度</small>
      </div>
    </div>

    <div class="job-meta">
      <span v-if="job.applicable" class="job-source applicable">站内可投</span>
      <span v-else class="job-source">{{ sourceLabel }}</span>
      <span v-for="meta in metaList" :key="meta" class="chip">{{ meta }}</span>
    </div>

    <p class="job-reason" v-if="item.matchReason">{{ item.matchReason }}</p>

    <div class="job-chips" v-if="jobSkills.length">
      <span v-for="sk in jobSkills" :key="sk" class="chip skill">{{ sk }}</span>
    </div>

    <div class="job-learn" v-if="item.missingSkills?.length">
      <span class="learn-label">推荐学习</span>
      <span v-for="sk in item.missingSkills" :key="sk" class="chip learn">{{ sk }}</span>
    </div>

    <div class="job-foot">
      <span class="date">{{ job.publishDate }}</span>
      <slot />
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { salaryRange } from '@/utils/format'

const props = defineProps({
  item: { type: Object, required: true }
})
defineEmits(['open'])

const job = computed(() => props.item.job || {})
const score = computed(() => Math.max(0, Math.min(100, Number(props.item.score) || 0)))
const scoreStyle = computed(() => ({ '--score': `${score.value}%` }))
const matchLabel = computed(() => {
  if (score.value >= 90) return '高度匹配'
  if (score.value >= 80) return '优质匹配'
  if (score.value >= 70) return '匹配良好'
  return '潜力推荐'
})
const metaList = computed(() => [
  job.value.education || '学历不限',
  job.value.experience || '经验不限',
  job.value.industry
].filter(Boolean))
const jobSkills = computed(() => {
  const raw = job.value.skills
  if (Array.isArray(raw)) return raw.filter(Boolean).slice(0, 4)
  if (!raw) return []
  return String(raw)
    .replace(/[\[\]"']/g, '')
    .split(/[,，、\s]+/)
    .map(s => s.trim())
    .filter(Boolean)
    .slice(0, 4)
})

const SOURCE_LABEL = { ZHAOPIN: '来自智联', BOSS: '来自 Boss', MOCK: '模拟数据' }
const sourceLabel = computed(() => SOURCE_LABEL[job.value.source] || '外部渠道')
</script>

<style src="./JobCard.css"></style>
