<template>
  <el-dialog v-model="visible" :title="cur?.title" width="600px" destroy-on-close>
    <div v-if="cur" class="news-detail">
      <div class="nd-meta">
        <span class="tag info">{{ typeLabel(cur.type) }}</span>
        <span v-if="cur.source">{{ cur.source }}</span>
        <span>{{ formatTime(cur.publishTime) }}</span>
        <span>浏览 {{ cur.viewCount }}</span>
      </div>
      <p v-if="cur.summary" class="nd-sum">{{ cur.summary }}</p>
      <div v-if="cur.content" class="nd-content" v-html="cur.content"></div>
      <el-button v-if="cur.type === 'EXTERNAL' && cur.sourceUrl" type="primary"
                 @click="openExternal(cur.sourceUrl)">阅读原文 ↗</el-button>
      <el-alert v-else-if="cur.type === 'DATA_CAST'" type="info" :closable="false"
                title="本条为平台数据播报，数据来自站内分析结果。" style="margin-top:8px" />
    </div>
  </el-dialog>
</template>

<script setup>
import { ref } from 'vue'
import { getNewsDetail } from '@/api/student'
import { formatTime } from '@/utils/format'

const visible = ref(false)
const cur = ref(null)

async function open(item) {
  try {
    cur.value = await getNewsDetail(item.id)
    visible.value = true
  } catch { /* 拦截器已提示 */ }
}
function typeLabel(t) {
  return { DATA_CAST: '数据播报', ARTICLE: '精选文章', EXTERNAL: '外部资讯' }[t] || '资讯'
}
function openExternal(url) { window.open(url, '_blank', 'noopener') }

defineExpose({ open })
</script>

<style scoped>
.news-detail .nd-meta { display: flex; align-items: center; gap: 12px; font-size: 12.5px; color: var(--color-text-tertiary); margin-bottom: 12px; flex-wrap: wrap; }
.tag { display: inline-flex; align-items: center; padding: 2px 9px; border-radius: 6px; font-size: 11.5px; font-weight: 600; }
.tag.info { background: var(--color-primary-lighter); color: var(--color-primary); }
.nd-sum { color: var(--color-text-secondary); margin-bottom: 12px; }
.nd-content { line-height: 1.7; }
</style>
