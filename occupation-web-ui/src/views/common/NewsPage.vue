<template>
  <div class="news-page">
    <!-- 头部区域 -->
    <div class="page-hero">
      <div class="hero-left">
        <h2 class="page-title">行业资讯</h2>
        <p class="page-sub">平台数据播报 · 精选文章 · 外部资讯，助你了解就业动态</p>
      </div>
      <div class="hero-stats">
        <div class="stat-item">
          <span class="stat-num">{{ total }}</span>
          <span class="stat-label">条资讯</span>
        </div>
      </div>
    </div>

    <!-- 筛选栏 -->
    <div class="filter-bar">
      <div class="cats">
        <span v-for="c in CATS" :key="c.id" class="nc" :class="{ on: category === c.id }"
              @click="setCat(c.id)">
          {{ c.label }}
        </span>
      </div>
      <el-select v-model="type" placeholder="内容类型" clearable size="default" style="width:160px" @change="reload">
        <el-option label="数据播报" value="DATA_CAST" />
        <el-option label="精选文章" value="ARTICLE" />
        <el-option label="外部资讯" value="EXTERNAL" />
      </el-select>
    </div>

    <!-- 横条式列表（搜索引擎结果风格） -->
    <div v-loading="loading" class="result-list" element-loading-text="加载中...">
      <div v-for="n in list" :key="n.id" class="result-item" @click="detailRef?.open(n)">
        <div class="ri-body">
          <!-- 头部：来源名 + 面包屑域名 -->
          <div class="ri-head">
            <span class="ri-source">{{ n.source || '资讯' }}</span>
            <span class="ri-breadcrumb">{{ breadcrumb(n) }}</span>
            <span v-if="n.featured" class="ri-featured">精选</span>
            <span class="ri-type-tag" :class="'t-' + (n.type || '')">{{ typeLabel(n.type) }}</span>
          </div>
          <!-- 标题 -->
          <h3 class="ri-title">{{ n.title }}</h3>
          <!-- 摘要 -->
          <p class="ri-summary">{{ n.summary || '暂无摘要' }}</p>
          <!-- 底部：分类、时间、阅读数 -->
          <div class="ri-footer">
            <span v-if="n.category" class="ri-cat">
              <span class="ri-cat-dot"></span>
              {{ catLabel(n.category) }}
            </span>
            <span class="ri-time">{{ formatTime(n.publishTime) }}</span>
            <span class="ri-dot">·</span>
            <span class="ri-views">{{ n.viewCount }} 阅读</span>
          </div>
        </div>
      </div>
    </div>

    <el-empty v-if="!loading && !list.length" description="暂无符合条件的资讯" :image-size="80" />

    <el-pagination
      v-if="total > pageSize"
      v-model:current-page="pageNum" :page-size="pageSize" :total="total"
      layout="total, prev, pager, next" @current-change="load" class="pagination-wrap"
    />

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
  { id: 'all', label: '全部' },
  { id: 'backend', label: '后端开发' },
  { id: 'frontend', label: '前端开发' },
  { id: 'test', label: '测试开发' },
  { id: 'devops', label: '运维开发' },
  { id: 'bigdata', label: '大数据开发' }
]
const CAT_MAP = { backend: '后端', frontend: '前端', test: '测试', devops: '运维', bigdata: '大数据' }

// 来源映射：域名显示（面包屑）
const SOURCE_DOMAIN = {
  'CSDN 博客':       'csdn.net › article › details',
  '博客园':          'cnblogs.com › archive',
  '掘金':            'juejin.cn › post',
  '思否':            'segmentfault.com › a',
  'InfoQ':           'infoq.cn › article',
  'OSCHINA':         'oschina.net › news',
  'OSChina':         'oschina.net › news',
  '开源中国':         'oschina.net › news',
  '新华网':          'news.cn › tech',
  '人民网':          'people.com.cn › tech',
  '央视网':          'cctv.com › tech',
  '36 氪':          '36kr.com › information',
  '钛媒体':          'tmtpost.com',
  '虎嗅':            'huxiu.com › article',
  '极客公园':         'geekpark.net',
  '雷锋网':          'leiphone.com',
  'IT 之家':        'ithome.com › news',
  '中国新闻网':       'chinanews.com › tech',
  '亿欧网':          'iyiou.com',
  '新浪科技':         'tech.sina.com.cn',
  '腾讯科技':         'new.qq.com › tech',
  '网易科技':         'tech.163.com',
  '百度百科':         'baike.baidu.com › item',
  '中关村在线':        'zol.com.cn › news',
  '太平洋电脑网':      'pconline.com.cn',
  '快科技':          'mydrivers.com',
  '量子位':          'qbitai.com',
  'AI 前线':        'ai-front.com',
  'AI 科技评论':     'leiphone.com › category',
  'GitHub':          'github.com › trending',
  'Stack Overflow':  'stackoverflow.com › questions',
  'Medium':          'medium.com',
  'Dev.to':          'dev.to',
  'FreeCodeCamp':    'freecodecamp.org › news',
  'Hacker News':     'news.ycombinator.com',
  'IBM Developer':   'developer.ibm.com › articles',
  'AWS Blog':        'aws.amazon.com › blogs',
  'Microsoft Learn': 'learn.microsoft.com',
  'Google Developers':'developers.googleblog.com',
  'Apple Developer': 'developer.apple.com › news',
  'Mozilla Hacks':   'hacks.mozilla.org',
  'Android Developers':'developer.android.com',
  'Red Hat Blog':    'redhat.com › blog',
  'CNCF Blog':       'cncf.io › blog',
  'IEEE Spectrum':   'spectrum.ieee.org',
  'ACM TechNews':    'technews.acm.org',
  'Wired':           'wired.com › story',
  'TechCrunch':      'techcrunch.com',
  'The Verge':       'theverge.com',
  'Ars Technica':    'arstechnica.com',
  'VentureBeat':     'venturebeat.com',
  'ZDNet':           'zdnet.com › article',
  'Engadget':        'engadget.com',
  'Slashdot':        'slashdot.org',
  '企业内训平台':      'platform.occupation.edu',
  '数据播报中心':      'analytics.occupation.edu',
  '就业指导中心':      'career.occupation.edu',
  '教研组':          'research.occupation.edu',
  '行业研究中心':      'research.occupation.edu',
  '职业评估中心':      'assessment.occupation.edu',
  '教学中心':         'teach.occupation.edu',
  '职业规划组':       'planning.occupation.edu',
  '技能提升小组':      'skill.occupation.edu',
  '平台数据播报':      'data.occupation.edu › cast'
}

function breadcrumb(n) {
  if (n.sourceUrl) {
    // 截取域名
    try {
      const u = new URL(n.sourceUrl)
      return u.hostname.replace(/^www\./, '')
    } catch { /* ignore */ }
  }
  if (n.source && SOURCE_DOMAIN[n.source]) return SOURCE_DOMAIN[n.source]
  return 'occupation.edu › news'
}

const detailRef = ref(null)
const list = ref([])
const loading = ref(false)
const category = ref('all')
const type = ref('')
const pageNum = ref(1)
const pageSize = ref(15)
const total = ref(0)

function typeLabel(t) {
  return { DATA_CAST: '数据播报', ARTICLE: '精选文章', EXTERNAL: '外部资讯' }[t] || '资讯'
}
function catLabel(c) { return CAT_MAP[c] || c }

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
/* ========== 页面容器 ========== */
.news-page { max-width: 1160px; margin: 0 auto; padding: 0 4px; }

/* ========== 头部 Hero ========== */
.page-hero {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 20px; padding: 24px 28px;
  background: linear-gradient(135deg, #f8faff 0%, #eef2ff 100%);
  border-radius: 16px; border: 1px solid #e0e7ff;
}
.hero-left { flex: 1; }
.page-title { font-size: 24px; font-weight: 750; margin: 0 0 4px; color: #1e293b; letter-spacing: -.3px; }
.page-sub { color: #64748b; font-size: 13.5px; margin: 0; }
.hero-stats { display: flex; gap: 24px; }
.stat-item { text-align: center; }
.stat-num { display: block; font-size: 28px; font-weight: 700; color: var(--color-primary); line-height: 1; }
.stat-label { font-size: 12px; color: #94a3b8; margin-top: 4px; display: block; }

/* ========== 筛选栏 ========== */
.filter-bar {
  display: flex; align-items: center; justify-content: space-between;
  gap: 12px; margin-bottom: 16px; flex-wrap: wrap;
}
.cats { display: flex; gap: 6px; flex-wrap: wrap; }
.nc {
  font-size: 13px; padding: 6px 14px; border-radius: 10px;
  color: #475569; background: #f1f5f9; border: 1px solid #e2e8f0;
  cursor: pointer; transition: all .2s;
  user-select: none;
}
.nc:hover { background: #e2e8f0; color: #1e293b; }
.nc.on {
  color: #fff; background: var(--color-primary); border-color: transparent;
  font-weight: 600; box-shadow: 0 2px 8px rgba(37,99,235,.25);
}

/* ========== 横条列表 ========== */
.result-list { display: flex; flex-direction: column; gap: 4px; min-height: 200px; }
.result-item {
  padding: 16px 12px; border-radius: 10px; cursor: pointer;
  transition: background .18s;
}
.result-item:hover { background: #f8fafc; }
.result-item + .result-item { border-top: 1px solid #f1f5f9; }

/* 文本容器 */
.ri-body { flex: 1; min-width: 0; }

/* 头部：来源 + 面包屑 + 类型标签 */
.ri-head {
  display: flex; align-items: center; gap: 8px; flex-wrap: wrap;
  margin-bottom: 4px;
}
.ri-source {
  font-size: 13px; font-weight: 600; color: #1e293b;
}
.ri-breadcrumb {
  font-size: 12px; color: #64748b;
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 60%;
}
.ri-featured {
  font-size: 10.5px; font-weight: 700; color: #fff;
  background: linear-gradient(135deg, #f59e0b, #ef4444);
  padding: 1px 8px; border-radius: 5px;
}
.ri-type-tag {
  font-size: 11px; font-weight: 600; padding: 1px 8px; border-radius: 5px;
  border: 1px solid;
}
.t-DATA_CAST { color: #047857; background: #ecfdf5; border-color: #a7f3d0; }
.t-ARTICLE   { color: #6d28d9; background: #f5f3ff; border-color: #ddd6fe; }
.t-EXTERNAL  { color: #1d4ed8; background: #eff6ff; border-color: #bfdbfe; }

/* 标题 */
.ri-title {
  font-size: 17px; font-weight: 600; color: #1d4ed8;
  margin: 0 0 4px; line-height: 1.4;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;
}
.result-item:hover .ri-title { text-decoration: underline; color: #1e40af; }

/* 摘要 */
.ri-summary {
  font-size: 13px; color: #475569; line-height: 1.65;
  margin: 0 0 6px;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;
}

/* 底部元信息 */
.ri-footer { display: flex; align-items: center; gap: 8px; font-size: 12px; color: #94a3b8; }
.ri-cat { display: flex; align-items: center; gap: 4px; color: #475569; font-weight: 500; }
.ri-cat-dot { width: 6px; height: 6px; border-radius: 50%; background: var(--color-primary); }
.ri-dot { color: #cbd5e1; }
.ri-time, .ri-views { white-space: nowrap; }

/* ========== 分页 ========== */
.pagination-wrap { margin-top: 20px; display: flex; justify-content: center; }

/* ========== 响应式 ========== */
@media (max-width: 640px) {
  .page-hero { flex-direction: column; align-items: flex-start; gap: 12px; padding: 18px 20px; }
  .hero-stats { gap: 16px; }
  .ri-breadcrumb { max-width: 100%; }
  .ri-title { font-size: 15.5px; }
  .result-item { padding: 14px 8px; }
}
</style>
