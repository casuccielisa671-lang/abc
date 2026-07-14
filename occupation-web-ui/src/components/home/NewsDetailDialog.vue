<template>
  <el-dialog v-model="visible" :title="cur?.title" width="640px" destroy-on-close class="news-dialog">
    <div v-if="cur" class="news-detail">
      <!-- 元信息 -->
      <div class="nd-meta">
        <div class="nd-meta-row">
          <span class="nd-meta-item">
            <span class="nd-meta-key">发布</span>
            {{ formatTime(cur.publishTime) }}
          </span>
          <span class="nd-meta-item" v-if="cur.source">
            <span class="nd-meta-key">来源</span>
            {{ cur.source }}
          </span>
          <span class="nd-meta-item" v-if="cur.category">
            <span class="nd-meta-key">分类</span>
            {{ catLabel(cur.category) }}
          </span>
          <span class="nd-meta-item">
            <span class="nd-meta-key">浏览</span>
            {{ cur.viewCount }} 次
          </span>
        </div>
      </div>

      <!-- 摘要 -->
      <div v-if="cur.summary" class="nd-summary">
        <div class="nd-summary-label">摘要</div>
        <p>{{ cur.summary }}</p>
      </div>

      <!-- 正文 -->
      <div v-if="cur.content" class="nd-content" v-html="cur.content"></div>

      <!-- 操作按钮 -->
      <div class="nd-actions">
        <el-button v-if="cur.sourceUrl" type="primary" round class="nd-link-btn"
                   @click="openExternal(cur.sourceUrl)">
          跳转相关资讯网页
        </el-button>
        <el-alert v-else-if="cur.type === 'DATA_CAST'" type="info" :closable="false"
                  title="本条为平台数据播报，数据来自站内分析结果。" />
      </div>
    </div>
  </el-dialog>
</template>

<script setup>
import { ref } from 'vue'
import { getNewsDetail } from '@/api/student'
import { formatTime } from '@/utils/format'

const visible = ref(false)
const cur = ref(null)

const CAT_MAP = { backend: '后端', frontend: '前端', test: '测试', devops: '运维', bigdata: '大数据' }

async function open(item) {
  try {
    cur.value = await getNewsDetail(item.id)
    visible.value = true
  } catch { /* 拦截器已提示 */ }
}
function catLabel(c) { return CAT_MAP[c] || c }
function openExternal(url) { window.open(url, '_blank', 'noopener,noreferrer') }

defineExpose({ open })
</script>

<style scoped>
/* 元信息 */
.nd-meta { margin-bottom: 14px; padding-bottom: 14px; border-bottom: 1px solid #f1f5f9; }
.nd-meta-row { display: flex; align-items: center; gap: 16px; flex-wrap: wrap; }
.nd-meta-item { font-size: 13px; color: #64748b; display: flex; align-items: center; gap: 5px; }
.nd-meta-key { font-size: 11px; color: #94a3b8; font-weight: 600; }

/* 摘要 */
.nd-summary {
  background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 10px;
  padding: 14px 16px; margin-bottom: 14px;
}
.nd-summary-label { font-size: 12px; font-weight: 650; color: #94a3b8; text-transform: uppercase; letter-spacing: .5px; margin-bottom: 6px; }
.nd-summary p { margin: 0; font-size: 13.5px; color: #475569; line-height: 1.65; }

/* 正文 */
.nd-content { line-height: 1.75; color: #334155; font-size: 14px; }

/* 操作区 */
.nd-actions { margin-top: 16px; display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.nd-link-btn { font-weight: 650; }
</style>
