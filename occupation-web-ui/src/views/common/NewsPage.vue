<template>
  <div class="news-page">
    <div class="page-head">
      <h2 class="page-title">行业资讯</h2>
      <p class="page-sub">平台数据播报 · 精选文章 · 外部资讯，助你了解就业动态</p>
    </div>

    <el-card>
      <div class="filters">
        <div class="cats">
          <span v-for="c in CATS" :key="c.id" class="nc" :class="{ on: category === c.id }"
                @click="setCat(c.id)">{{ c.label }}</span>
        </div>
        <el-select v-model="type" placeholder="内容类型" clearable style="width:150px" @change="reload">
          <el-option label="数据播报" value="DATA_CAST" />
          <el-option label="精选文章" value="ARTICLE" />
          <el-option label="外部资讯" value="EXTERNAL" />
        </el-select>
      </div>

      <div v-loading="loading" class="grid">
        <div v-for="n in list" :key="n.id" class="card" @click="detailRef?.open(n)">
          <div class="cover" :class="'cov-' + (n.coverStyle || 'blue')">
            <span class="k">{{ typeLabel(n.type) }}</span>
            <span v-if="n.featured" class="star">★ 精选</span>
          </div>
          <div class="body">
            <div class="ti">{{ n.title }}</div>
            <div class="sum">{{ n.summary }}</div>
            <div class="meta">
              <span>{{ n.source || '资讯' }}</span>
              <span>{{ formatTime(n.publishTime) }}</span>
              <span>浏览 {{ n.viewCount }}</span>
            </div>
          </div>
        </div>
      </div>

      <el-empty v-if="!loading && !list.length" description="暂无符合条件的资讯" />

      <el-pagination
        v-if="total > pageSize"
        v-model:current-page="pageNum" :page-size="pageSize" :total="total"
        layout="total, prev, pager, next" @current-change="load" style="margin-top:16px; justify-content:flex-end"
      />
    </el-card>

    <NewsDetailDialog ref="detailRef" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getNewsPage } from '@/api/student'
import { toList, toTotal } from '@/utils/list'
import { formatTime } from '@/utils/format'
import NewsDetailDialog from '@/components/home/NewsDetailDialog.vue'

const CATS = [
  { id: 'all', label: '全部' }, { id: 'backend', label: '后端开发' }, { id: 'frontend', label: '前端开发' },
  { id: 'test', label: '测试开发' }, { id: 'devops', label: '运维开发' }, { id: 'bigdata', label: '大数据开发' }
]

const detailRef = ref(null)
const list = ref([])
const loading = ref(false)
const category = ref('all')
const type = ref('')
const pageNum = ref(1)
const pageSize = ref(12)
const total = ref(0)

function typeLabel(t) {
  return { DATA_CAST: '数据播报', ARTICLE: '精选文章', EXTERNAL: '外部资讯' }[t] || '资讯'
}

async function load() {
  loading.value = true
  try {
    const params = { pageNum: pageNum.value, pageSize: pageSize.value }
    if (category.value !== 'all') params.category = category.value
    if (type.value) params.type = type.value
    const data = await getNewsPage(params)
    list.value = toList(data)
    total.value = toTotal(data, list.value)
  } finally {
    loading.value = false
  }
}
function reload() { pageNum.value = 1; load() }
function setCat(id) { category.value = id; reload() }

onMounted(load)
</script>

<style scoped>
.news-page { max-width: 1100px; margin: 0 auto; }
.page-head { margin-bottom: 16px; }
.page-title { font-size: 22px; font-weight: 700; margin: 0 0 4px; }
.page-sub { color: var(--color-text-tertiary); font-size: 13.5px; margin: 0; }

.filters { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 18px; flex-wrap: wrap; }
.cats { display: flex; gap: 8px; flex-wrap: wrap; }
.nc { font-size: 13px; padding: 5px 13px; border-radius: 999px; color: var(--color-text-secondary); background: var(--color-bg-secondary); border: 1px solid var(--color-border); cursor: pointer; }
.nc.on { color: #fff; background: var(--color-primary); border-color: transparent; font-weight: 600; }

.grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; }
.card { border: 1px solid var(--color-border); border-radius: 12px; overflow: hidden; cursor: pointer; background: var(--color-surface); transition: transform .15s, box-shadow .15s, border-color .15s; display: flex; flex-direction: column; }
.card:hover { transform: translateY(-3px); box-shadow: var(--shadow-md); border-color: color-mix(in srgb, var(--color-primary) 40%, var(--color-border)); }
.cover { height: 120px; position: relative; }
.cover.cov-blue { background: linear-gradient(135deg,#2563EB,#5B60F0); }
.cover.cov-green { background: linear-gradient(135deg,#0E9F6E,#15A34A); }
.cover.cov-purple { background: linear-gradient(135deg,#5B60F0,#8A5BF0); }
.cover.cov-amber { background: linear-gradient(135deg,#C97A00,#E08600); }
.cover .k { position: absolute; top: 10px; left: 10px; font-size: 11.5px; font-weight: 600; color: #fff; background: rgba(0,0,0,.32); padding: 3px 9px; border-radius: 7px; }
.cover .star { position: absolute; top: 10px; right: 10px; font-size: 11px; font-weight: 600; color: #fff; background: rgba(201,122,0,.85); padding: 3px 8px; border-radius: 7px; }
.body { padding: 14px; flex: 1; display: flex; flex-direction: column; }
.ti { font-size: 15px; font-weight: 650; line-height: 1.4; margin-bottom: 8px; }
.sum { font-size: 13px; color: var(--color-text-secondary); line-height: 1.55; flex: 1;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.meta { display: flex; gap: 12px; font-size: 12px; color: var(--color-text-tertiary); margin-top: 12px; flex-wrap: wrap; }

@media (max-width: 900px) { .grid { grid-template-columns: 1fr; } }
</style>
