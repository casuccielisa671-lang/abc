<template>
  <div class="job-card" @click="$emit('open')">
    <div class="job-head">
      <div>
        <h3 class="job-title">{{ job.title }}</h3>
        <p class="job-company">{{ job.company }}</p>
      </div>
      <div class="job-score">
        <div class="num">{{ item.score }}</div>
        <div class="cap">匹配分</div>
      </div>
    </div>

    <div class="job-salary">{{ salaryRange(job.salaryMin, job.salaryMax) }}</div>

    <div class="job-chips">
      <!-- 能不能站内投递，在列表上就说清楚，别让学生点进去才发现没有投递按钮 -->
      <el-tag v-if="job.applicable" type="success" size="small" effect="plain">可投递</el-tag>
      <span v-else class="chip">{{ sourceLabel }}</span>
      <span class="chip">{{ job.city }}</span>
      <span class="chip">{{ job.education || '学历不限' }}</span>
      <span v-if="job.industry" class="chip">{{ job.industry }}</span>
    </div>

    <p class="job-reason" v-if="item.matchReason">{{ item.matchReason }}</p>

    <div class="job-learn" v-if="item.missingSkills?.length">
      建议学习
      <span v-for="sk in item.missingSkills" :key="sk" class="chip learn">{{ sk }}</span>
    </div>

    <div class="job-foot">
      <span class="date">{{ job.publishDate }}</span>
      <slot />
    </div>
  </div>
</template>

<script setup>
/**
 * 推荐职位卡片。可投递岗位与市场参考两栏共用，差别只在 foot 里的按钮（由 slot 决定）。
 */
import { computed } from 'vue'
import { salaryRange } from '@/utils/format'

const props = defineProps({
  /** MatchJobVO：{ job, score, matchReason, missingSkills } */
  item: { type: Object, required: true }
})
defineEmits(['open'])

const job = computed(() => props.item.job || {})

const SOURCE_LABEL = { ZHAOPIN: '来自智联', BOSS: '来自 Boss', MOCK: '模拟数据' }
const sourceLabel = computed(() => SOURCE_LABEL[job.value.source] || '外部渠道')
</script>
