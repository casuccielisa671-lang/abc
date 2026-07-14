<template>
  <div class="news-manage">
    <div class="page-head">
      <h2 class="page-title">资讯管理</h2>
      <p class="page-sub">管理精选文章、审核上下架，并可一键生成数据播报或拉取外部资讯</p>
    </div>

    <el-card>
      <div class="bar">
        <div class="filters">
          <el-select v-model="type" placeholder="类型" clearable style="width:130px" @change="reload">
            <el-option label="数据播报" value="DATA_CAST" />
            <el-option label="精选文章" value="ARTICLE" />
            <el-option label="外部资讯" value="EXTERNAL" />
          </el-select>
          <el-select v-model="status" placeholder="状态" clearable style="width:120px" @change="reload">
            <el-option label="上架" :value="1" />
            <el-option label="下架" :value="0" />
          </el-select>
        </div>
        <div class="actions">
          <el-button :loading="genLoading" @click="genDataCast">⚡ 生成数据播报</el-button>
          <el-button :loading="rssLoading" @click="pullRss">拉取外部资讯</el-button>
          <el-button type="primary" @click="openDialog()">新建文章</el-button>
        </div>
      </div>

      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column label="标题" min-width="240">
          <template #default="{ row }">
            <span class="ntitle">{{ row.title }}</span>
            <el-tag v-if="row.featured" size="small" type="warning" style="margin-left:6px">精选</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="100">
          <template #default="{ row }"><el-tag size="small" :type="typeTag(row.type)">{{ typeLabel(row.type) }}</el-tag></template>
        </el-table-column>
        <el-table-column label="方向" width="110">
          <template #default="{ row }"><span class="chip">{{ catLabel(row.category) }}</span></template>
        </el-table-column>
        <el-table-column prop="source" label="来源" width="130" show-overflow-tooltip />
        <el-table-column label="浏览" width="80">
          <template #default="{ row }">{{ row.viewCount }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '上架' : '下架' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="发布时间" width="150">
          <template #default="{ row }">{{ formatTime(row.publishTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button text size="small" @click="toggleStatus(row)">{{ row.status === 1 ? '下架' : '上架' }}</el-button>
            <el-button text type="primary" size="small" @click="openDialog(row)">编辑</el-button>
            <el-button text type="danger" size="small" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && !list.length" description="暂无资讯" />
      <el-pagination
        v-if="total > pageSize"
        v-model:current-page="pageNum" :page-size="pageSize" :total="total"
        layout="total, prev, pager, next" @current-change="load" style="margin-top:16px; justify-content:flex-end"
      />
    </el-card>

    <!-- 新建/编辑 -->
    <el-dialog v-model="dialog" :title="form.id ? '编辑资讯' : '新建文章'" width="620px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="标题" required>
          <el-input v-model="form.title" placeholder="资讯标题" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.type" style="width:100%">
            <el-option label="精选文章（有正文）" value="ARTICLE" />
            <el-option label="外部资讯（跳原文）" value="EXTERNAL" />
          </el-select>
        </el-form-item>
        <el-form-item label="技术方向">
          <el-select v-model="form.category" clearable placeholder="通用" style="width:100%">
            <el-option v-for="c in CATS" :key="c.id" :label="c.label" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="摘要">
          <el-input v-model="form.summary" type="textarea" :rows="2" placeholder="一句话摘要（列表与卡片展示）" />
        </el-form-item>
        <el-form-item v-if="form.type === 'ARTICLE'" label="正文">
          <el-input v-model="form.content" type="textarea" :rows="6" placeholder="支持 HTML，如 <p>段落</p>" />
        </el-form-item>
        <el-form-item v-if="form.type === 'EXTERNAL'" label="原文链接">
          <el-input v-model="form.sourceUrl" placeholder="https://..." />
        </el-form-item>
        <el-form-item label="来源">
          <el-input v-model="form.source" placeholder="如 就业指导中心 / 开源中国" />
        </el-form-item>
        <el-form-item label="封面色">
          <el-radio-group v-model="form.coverStyle">
            <el-radio-button v-for="c in COVERS" :key="c" :value="c">{{ c }}</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="精选置顶">
          <el-switch v-model="form.featured" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="上架" inactive-text="下架" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getAdminNews, saveNews, deleteNews, updateNewsStatus, generateDataCast, pullRssNews
} from '@/api/admin'
import { toList, toTotal } from '@/utils/list'
import { formatTime } from '@/utils/format'

const CATS = [
  { id: 'backend', label: '后端开发' }, { id: 'frontend', label: '前端开发' }, { id: 'test', label: '测试开发' },
  { id: 'devops', label: '运维开发' }, { id: 'bigdata', label: '大数据开发' }
]
const COVERS = ['blue', 'green', 'purple', 'amber']

const list = ref([])
const loading = ref(false)
const type = ref('')
const status = ref('')
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

function typeLabel(t) { return { DATA_CAST: '数据播报', ARTICLE: '精选文章', EXTERNAL: '外部资讯' }[t] || t }
function typeTag(t) { return { DATA_CAST: 'info', ARTICLE: 'success', EXTERNAL: 'warning' }[t] || '' }
function catLabel(c) { return CATS.find(x => x.id === c)?.label || '通用' }

async function load() {
  loading.value = true
  try {
    const params = { pageNum: pageNum.value, pageSize: pageSize.value }
    if (type.value) params.type = type.value
    if (status.value !== '') params.status = status.value
    const data = await getAdminNews(params)
    list.value = toList(data)
    total.value = toTotal(data, list.value)
  } finally {
    loading.value = false
  }
}
function reload() { pageNum.value = 1; load() }

const EMPTY = { id: null, title: '', type: 'ARTICLE', category: '', summary: '', content: '', sourceUrl: '', source: '', coverStyle: 'blue', featured: 0, status: 1 }
const dialog = ref(false)
const form = reactive({ ...EMPTY })
function openDialog(row) {
  Object.assign(form, row ? { ...EMPTY, ...row } : { ...EMPTY })
  dialog.value = true
}
async function submit() {
  if (!form.title) { ElMessage.warning('标题不能为空'); return }
  await saveNews({ ...form })
  ElMessage.success('已保存')
  dialog.value = false
  load()
}
async function remove(row) {
  try { await ElMessageBox.confirm(`确认删除「${row.title}」？`, '提示', { type: 'warning' }) } catch { return }
  await deleteNews(row.id)
  ElMessage.success('已删除')
  load()
}
async function toggleStatus(row) {
  await updateNewsStatus(row.id, row.status === 1 ? 0 : 1)
  ElMessage.success(row.status === 1 ? '已下架' : '已上架')
  load()
}

const genLoading = ref(false)
async function genDataCast() {
  try { await ElMessageBox.confirm('将根据当前站内分析数据重新生成"数据播报"（会覆盖旧的播报）。继续？', '生成数据播报') } catch { return }
  genLoading.value = true
  try {
    const n = await generateDataCast()
    ElMessage.success(`已生成 ${n} 条数据播报`)
    load()
  } finally { genLoading.value = false }
}

const rssLoading = ref(false)
async function pullRss() {
  let query
  try {
    const r = await ElMessageBox.prompt('输入检索关键词（默认从开源中国 RSS 拉取）', '拉取外部资讯', {
      inputValue: 'IT就业', confirmButtonText: '拉取', cancelButtonText: '取消'
    })
    query = r.value
  } catch { return }
  rssLoading.value = true
  try {
    const n = await pullRssNews(query, 8)
    ElMessage[n > 0 ? 'success' : 'warning'](n > 0 ? `已拉取 ${n} 条外部资讯` : '未拉取到内容（默认 RSS 源暂不可访问或已重复）')
    load()
  } finally { rssLoading.value = false }
}

onMounted(load)
</script>

<style scoped>
.news-manage { max-width: 1100px; }
.page-head { margin-bottom: 16px; }
.page-title { font-size: 22px; font-weight: 700; margin: 0 0 4px; }
.page-sub { color: var(--color-text-tertiary); font-size: 13.5px; margin: 0; }
.bar { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 16px; flex-wrap: wrap; }
.filters { display: flex; gap: 10px; }
.actions { display: flex; gap: 8px; flex-wrap: wrap; }
.ntitle { font-weight: 550; }
.chip { display: inline-flex; padding: 2px 9px; border-radius: 999px; background: var(--color-bg-secondary); border: 1px solid var(--color-border); color: var(--color-text-secondary); font-size: 12px; }
</style>
