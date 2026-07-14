<template>
  <div class="news-tile">
    <div class="tile-h">
      <span class="t">行业资讯</span>
      <div class="news-cats">
        <span v-for="c in NEWS_CATS" :key="c.id" class="nc" :class="{ on: newsCat === c.id }"
              @click="switchCat(c.id)">{{ c.label }}</span>
        <span class="view-all" @click="viewAll">查看全部 ›</span>
      </div>
    </div>
    <div v-loading="loading" class="news-list" element-loading-text="加载中...">
      <div v-for="n in news" :key="n.id" class="nitem" @click="detailRef?.open(n)">
        <div class="ni-body">
          <div class="ni-head">
            <span class="ni-source">{{ n.source || '资讯' }}</span>
            <span class="ni-time">{{ formatTime(n.publishTime) }}</span>
            <span class="ni-type-tag" :class="'t-' + (n.type || '')">{{ typeLabel(n.type) }}</span>
          </div>
          <div class="ni-title">{{ n.title }}</div>
        </div>
      </div>
      <el-empty v-if="!loading && !news.length" description="该方向暂无资讯" :image-size="48" />
    </div>

    <NewsDetailDialog ref="detailRef" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { getLatestNews } from '@/api/student'
import { toList } from '@/utils/list'
import { formatTime } from '@/utils/format'
import NewsDetailDialog from '@/components/home/NewsDetailDialog.vue'

const router = useRouter()
const userStore = useUserStore()
const detailRef = ref(null)

const NEWS_CATS = [
  { id: 'all', label: '全部' }, { id: 'backend', label: '后端' }, { id: 'frontend', label: '前端' },
  { id: 'test', label: '测试' }, { id: 'devops', label: '运维' }, { id: 'bigdata', label: '大数据' }
]
const news = ref([])
const loading = ref(false)
const newsCat = ref('all')

function typeLabel(t) {
  return { DATA_CAST: '数据播报', ARTICLE: '精选文章', EXTERNAL: '外部资讯' }[t] || '资讯'
}
function viewAll() {
  const rp = { STUDENT: 'student', TEACHER: 'teacher', HR: 'hr', ADMIN: 'admin' }[userStore.role] || 'student'
  router.push(`/${rp}/news`)
}

async function load(category) {
  loading.value = true
  try {
    const cat = category && category !== 'all' ? category : undefined
    const d = await getLatestNews(12, cat)
    let rows = toList(d)
    if (!rows.length && cat) {
      const fallback = await getLatestNews(12)
      rows = toList(fallback)
    }
    news.value = rows.slice(0, 6)
  } catch {
    news.value = []
  }
  finally { loading.value = false }
}

function switchCat(id) {
  newsCat.value = id
  load(id)
}

onMounted(() => load('all'))
</script>

<style scoped>
.news-tile { display: flex; flex-direction: column; height: 100%; }
.tile-h { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; gap: 10px; flex-wrap: wrap; }
.tile-h .t { font-size: 15px; font-weight: 700; color: #1e293b; }
.news-cats { display: flex; gap: 5px; flex-wrap: wrap; align-items: center; }
.nc { font-size: 11.5px; padding: 3px 10px; border-radius: 8px; color: #64748b; background: #f1f5f9; border: 1px solid #e2e8f0; cursor: pointer; transition: all .15s; user-select: none; }
.nc:hover { background: #e2e8f0; color: #334155; }
.nc.on { color: #fff; background: var(--color-primary); border-color: transparent; font-weight: 600; }
.view-all { font-size: 12px; color: var(--color-primary); cursor: pointer; margin-left: 2px; font-weight: 500; white-space: nowrap; }
.view-all:hover { text-decoration: underline; }

/* 横条列表 */
.news-list {
  flex: 1; display: flex; flex-direction: column; gap: 2px;
  overflow-y: auto; overflow-x: hidden;
  max-height: 380px;
  scrollbar-width: thin;
  scrollbar-color: #cbd5e1 transparent;
}
.news-list::-webkit-scrollbar { width: 6px; }
.news-list::-webkit-scrollbar-thumb { background: #cbd5e1; border-radius: 3px; }
.news-list::-webkit-scrollbar-track { background: transparent; }
.nitem {
  padding: 10px 8px; border-radius: 8px; cursor: pointer;
  transition: background .15s;
}
.nitem:hover { background: #f8fafc; }
.nitem + .nitem { border-top: 1px solid #f1f5f9; }

/* 文本 */
.ni-body { flex: 1; min-width: 0; }
.ni-head { display: flex; align-items: center; gap: 6px; margin-bottom: 2px; }
.ni-source { font-size: 12px; font-weight: 600; color: #1e293b; }
.ni-time { font-size: 11px; color: #94a3b8; }
.ni-type-tag {
  font-size: 10px; font-weight: 600; padding: 0 6px; border-radius: 4px;
  border: 1px solid;
}
.t-DATA_CAST { color: #047857; background: #ecfdf5; border-color: #a7f3d0; }
.t-ARTICLE   { color: #6d28d9; background: #f5f3ff; border-color: #ddd6fe; }
.t-EXTERNAL  { color: #1d4ed8; background: #eff6ff; border-color: #bfdbfe; }

.ni-title {
  font-size: 13px; font-weight: 500; color: #1e293b; line-height: 1.45;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;
}
.nitem:hover .ni-title { color: #1d4ed8; }
</style>
