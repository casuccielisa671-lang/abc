<template>
  <div class="news-tile">
    <div class="tile-h">
      <span class="t">行业资讯</span>
      <div class="news-cats">
        <span v-for="c in NEWS_CATS" :key="c.id" class="nc" :class="{ on: newsCat === c.id }"
              @click="newsCat = c.id">{{ c.label }}</span>
        <span class="view-all" @click="viewAll">查看全部 ›</span>
      </div>
    </div>
    <div v-loading="loading" class="news-grid">
      <div v-for="n in filteredNews" :key="n.id" class="ncard" @click="detailRef?.open(n)">
        <div class="thumb" :class="'cov-' + (n.coverStyle || 'blue')">
          <span class="k">{{ typeLabel(n.type) }}</span>
        </div>
        <div class="nbody">
          <div class="nti">{{ n.title }}</div>
          <div class="ntm">{{ formatTime(n.publishTime) }} · {{ n.source || '资讯' }}</div>
        </div>
      </div>
      <el-empty v-if="!loading && !filteredNews.length" description="该方向暂无资讯" :image-size="52" />
    </div>

    <NewsDetailDialog ref="detailRef" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
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
const filteredNews = computed(() =>
  (newsCat.value === 'all' ? news.value : news.value.filter(n => n.category === newsCat.value)).slice(0, 6)
)
function typeLabel(t) {
  return { DATA_CAST: '数据播报', ARTICLE: '精选文章', EXTERNAL: '外部资讯' }[t] || '资讯'
}
function viewAll() {
  const rp = { STUDENT: 'student', TEACHER: 'teacher', HR: 'hr', ADMIN: 'admin' }[userStore.role] || 'student'
  router.push(`/${rp}/news`)
}

onMounted(() => {
  loading.value = true
  getLatestNews(12).then(d => { news.value = toList(d) }).catch(() => {}).finally(() => { loading.value = false })
})
</script>

<style scoped>
.news-tile { display: flex; flex-direction: column; height: 100%; }
.tile-h { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; gap: 10px; flex-wrap: wrap; }
.tile-h .t { font-size: 14px; font-weight: 650; }
.news-cats { display: flex; gap: 6px; flex-wrap: wrap; align-items: center; }
.nc { font-size: 12px; padding: 3px 10px; border-radius: 999px; color: var(--color-text-secondary); background: var(--color-bg-secondary); border: 1px solid var(--color-border); cursor: pointer; }
.nc.on { color: #fff; background: var(--color-primary); border-color: transparent; font-weight: 600; }
.view-all { font-size: 12px; color: var(--color-primary); cursor: pointer; margin-left: 4px; }
.news-grid { flex: 1; display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; overflow: hidden; }
.ncard { border: 1px solid var(--color-border); border-radius: 11px; overflow: hidden; cursor: pointer; display: flex; flex-direction: column; }
.ncard:hover { border-color: var(--color-primary); }
.thumb { height: 74px; position: relative; }
.thumb.cov-blue { background: linear-gradient(135deg,#2563EB,#5B60F0); }
.thumb.cov-green { background: linear-gradient(135deg,#0E9F6E,#15A34A); }
.thumb.cov-purple { background: linear-gradient(135deg,#5B60F0,#8A5BF0); }
.thumb.cov-amber { background: linear-gradient(135deg,#C97A00,#E08600); }
.thumb .k { position: absolute; top: 8px; left: 8px; font-size: 10.5px; font-weight: 600; color: #fff; background: rgba(0,0,0,.32); padding: 2px 7px; border-radius: 6px; }
.nbody { padding: 8px 10px; }
.nti { font-size: 12.5px; font-weight: 600; line-height: 1.35; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.ntm { font-size: 11px; color: var(--color-text-tertiary); margin-top: 5px; }
</style>
