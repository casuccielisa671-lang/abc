<template>
  <div class="student-reports">
    <div class="page-head with-actions">
      <div>
        <h2 class="page-title">我的报告</h2>
        <p class="page-sub">用 AI 生成你的个性化求职分析报告，可多轮修改、保存与下载</p>
      </div>
      <div class="page-actions">
        <el-button type="primary" @click="openGen">✨ 生成 AI 分析报告</el-button>
      </div>
    </div>

    <el-card>
      <el-tabs v-model="activeTab">
        <!-- ① 我的 AI 报告 -->
        <el-tab-pane name="mine">
          <template #label>我的 AI 报告</template>
          <el-table :data="records" v-loading="loading" stripe>
            <el-table-column label="ID" width="70">
              <template #default="{ $index }">{{ reportNo($index) }}</template>
            </el-table-column>
            <el-table-column prop="name" label="报告名称" min-width="220">
              <template #default="{ row }">{{ row.name || '—' }}</template>
            </el-table-column>
            <el-table-column label="格式" width="90">
              <template #default="{ row }"><span class="chip">{{ row.fileType || '—' }}</span></template>
            </el-table-column>
            <el-table-column label="状态" width="110">
              <template #default="{ row }"><el-tag :type="statusTag(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag></template>
            </el-table-column>
            <el-table-column label="生成时间" width="150">
              <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="110">
              <template #default="{ row }">
                <el-button v-if="row.status === 'SUCCESS'" text type="primary" size="small" @click="handleDownload(row)">下载</el-button>
                <span v-else class="muted">—</span>
              </template>
            </el-table-column>
          </el-table>

          <el-empty v-if="!loading && !records.length" description="还没有报告，点右上角生成一份 AI 分析报告" />

          <el-pagination
            v-if="total > size"
            v-model:current-page="page" :page-size="size" :total="total"
            layout="total, prev, pager, next" @current-change="loadRecords"
          />
        </el-tab-pane>

        <!-- ② 收到的报告（管理员下发） -->
        <el-tab-pane name="received">
          <template #label>
            <el-badge :value="unreadCount" :hidden="!unreadCount" class="tab-badge">收到的报告</el-badge>
          </template>
          <el-table :data="received" v-loading="rLoading" stripe>
            <el-table-column label="ID" width="70">
              <template #default="{ $index }">{{ receivedNo($index) }}</template>
            </el-table-column>
            <el-table-column label="报告名称" min-width="220">
              <template #default="{ row }">
                <el-badge is-dot :hidden="row.read" class="dot">{{ row.name || '—' }}</el-badge>
              </template>
            </el-table-column>
            <el-table-column label="类型" width="120">
              <template #default="{ row }">
                <el-tag size="small" :type="row.category === 'EMPLOYMENT' ? 'warning' : 'primary'">
                  {{ receivedCategoryLabel(row.category) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="格式" width="90">
              <template #default="{ row }"><span class="chip">{{ row.fileType || '—' }}</span></template>
            </el-table-column>
            <el-table-column label="收到时间" width="150">
              <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="110">
              <template #default="{ row }">
                <el-button text type="primary" size="small" @click="handleReceivedDownload(row)">下载</el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-empty v-if="!rLoading && !received.length" description="暂时没有收到管理员下发的报告" />

          <el-pagination
            v-if="rTotal > rSize"
            v-model:current-page="rPage" :page-size="rSize" :total="rTotal"
            layout="total, prev, pager, next" @current-change="loadReceived"
          />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 生成 / 多轮改 / 保存 -->
    <el-dialog v-model="genVisible" title="AI 求职分析报告" width="720px" :close-on-click-modal="false">
      <div v-loading="genLoading" class="gen-body">
        <div v-if="!content" class="gen-empty">
          <p>AI 会综合你的画像、简历、技能缺口与市场匹配，生成一份个性化求职分析。</p>
          <el-button type="primary" :loading="genLoading" @click="generate()">开始生成</el-button>
        </div>

        <template v-else>
          <div class="gen-tip">
            <el-tag :type="aiGenerated ? 'success' : 'info'" size="small">
              {{ aiGenerated ? 'AI 生成' : '规则化生成（AI 未启用）' }}
            </el-tag>
            <span class="muted">可直接编辑，或用下方指令让 AI 再改一版</span>
            <el-radio-group v-model="viewMode" size="small" class="view-toggle">
              <el-radio-button label="preview">预览</el-radio-button>
              <el-radio-button label="edit">编辑</el-radio-button>
            </el-radio-group>
          </div>

          <!-- 预览模式：渲染 Markdown，没有 ** / #### 残留 -->
          <div v-if="viewMode === 'preview'" class="md-preview" v-html="renderedContent" />

          <!-- 编辑模式：保留原始 Markdown 文本，便于手动改写 -->
          <el-input v-else v-model="content" type="textarea" :rows="14" placeholder="报告正文（Markdown）" />

          <div class="refine">
            <el-input v-model="instruction" placeholder="让 AI 修改，如：把技能提升建议写得更具体" @keyup.enter="refine" />
            <el-button :loading="genLoading" @click="refine">让 AI 改</el-button>
          </div>
        </template>
      </div>

      <template #footer>
        <div class="foot">
          <div class="foot-left">
            <el-input v-model="reportName" placeholder="报告名称" style="width:200px" />
            <el-select v-model="fileType" style="width:110px">
              <el-option label="PDF" value="PDF" />
              <el-option label="HTML" value="HTML" />
            </el-select>
          </div>
          <div>
            <el-button @click="genVisible = false">取消</el-button>
            <el-button type="primary" :disabled="!content" :loading="saving" @click="save">保存到我的报告</el-button>
          </div>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { marked } from 'marked'
import { ElMessage } from 'element-plus'
import {
  previewAiReport, saveAiReport, getMyReports, downloadMyReport,
  getReceivedReports, markReportRead
} from '@/api/student'
import { toList, toTotal } from '@/utils/list'
import { formatTime } from '@/utils/format'
import { saveBlob } from '@/utils/download'

// Markdown → HTML（前端预览用）；后端导出 PDF 时也会再渲染一次
marked.setOptions({ breaks: true, gfm: true })

const activeTab = ref('mine')
const viewMode = ref('preview')   // 'preview' 渲染 Markdown，'edit' 显示原文便于手动改
const renderedContent = computed(() => marked.parse(content.value || ''))

// ---- ① 我的 AI 报告 ----
const records = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)

async function loadRecords() {
  loading.value = true
  try {
    const data = await getMyReports({ pageNum: page.value, pageSize: size.value })
    records.value = toList(data)
    total.value = toTotal(data, records.value)
  } finally {
    loading.value = false
  }
}

async function handleDownload(row) {
  const ext = { PDF: 'pdf', WORD: 'docx', HTML: 'html' }[row.fileType] || 'bin'
  try { await saveBlob(downloadMyReport(row.id), `${row.name || 'report'}.${ext}`) } catch { /* 拦截器已提示 */ }
}

// ---- ② 收到的报告（管理员下发） ----
const received = ref([])
const rLoading = ref(false)
const rPage = ref(1)
const rSize = ref(10)
const rTotal = ref(0)
const unreadCount = computed(() => received.value.filter(r => !r.read).length)
const reportNo = index => (page.value - 1) * size.value + index + 1
const receivedNo = index => (rPage.value - 1) * rSize.value + index + 1

async function loadReceived() {
  rLoading.value = true
  try {
    const data = await getReceivedReports({ pageNum: rPage.value, pageSize: rSize.value })
    received.value = toList(data)
    rTotal.value = toTotal(data, received.value)
  } finally {
    rLoading.value = false
  }
}

async function handleReceivedDownload(row) {
  const ext = { PDF: 'pdf', WORD: 'docx', HTML: 'html' }[row.fileType] || 'bin'
  try {
    await saveBlob(downloadMyReport(row.id), `${row.name || 'report'}.${ext}`)
    if (!row.read) {
      row.read = true
      markReportRead(row.id).catch(() => {})   // 标记已读失败不影响下载体验
    }
  } catch { /* 拦截器已提示 */ }
}

function receivedCategoryLabel(c) {
  return { EMPLOYMENT: '学生就业', MARKET: '市场行业' }[c] || '报告'
}

// ---- 生成 / 多轮改 / 保存 ----
const genVisible = ref(false)
const genLoading = ref(false)
const saving = ref(false)
const content = ref('')
const aiGenerated = ref(false)
const instruction = ref('')
const history = ref([])          // 多轮对话历史（前端持有）
const reportName = ref('我的求职分析报告')
const fileType = ref('PDF')

function openGen() {
  content.value = ''
  aiGenerated.value = false
  instruction.value = ''
  history.value = []
  reportName.value = '我的求职分析报告'
  fileType.value = 'PDF'
  viewMode.value = 'preview'
  genVisible.value = true
}

async function generate(instr) {
  genLoading.value = true
  try {
    const data = await previewAiReport({ instruction: instr || '', history: history.value })
    content.value = data.content
    aiGenerated.value = data.aiGenerated
    history.value.push({ role: 'user', content: instr || '生成我的求职分析报告' })
    history.value.push({ role: 'assistant', content: data.content })
  } catch { /* 拦截器已提示 */ } finally { genLoading.value = false }
}

async function refine() {
  const instr = instruction.value.trim()
  if (!instr) { ElMessage.warning('请输入修改指令'); return }
  instruction.value = ''
  await generate(instr)
}

async function save() {
  saving.value = true
  try {
    await saveAiReport({ name: reportName.value, content: content.value, fileType: fileType.value })
    ElMessage.success('已保存到我的报告')
    genVisible.value = false
    loadRecords()
  } catch { /* 拦截器已提示 */ } finally { saving.value = false }
}

function statusTag(status) {
  return { SUCCESS: 'success', FAILED: 'danger', GENERATING: 'warning', PENDING: 'info' }[status] || 'info'
}
function statusLabel(status) {
  return { SUCCESS: '已完成', FAILED: '失败', GENERATING: '生成中', PENDING: '排队中' }[status] || status
}
onMounted(() => {
  loadRecords()
  loadReceived()   // 进页即拉一次，用于「收到的报告」未读角标
})
</script>

<style scoped>
.muted { color: var(--color-text-tertiary); }
.gen-body { min-height: 120px; }
.gen-empty { text-align: center; padding: 24px 0; color: var(--color-text-secondary); }
.gen-empty p { margin-bottom: 16px; }
.gen-tip { display: flex; align-items: center; gap: 10px; margin-bottom: 10px; font-size: 12.5px; }
.refine { display: flex; gap: 10px; margin-top: 12px; }
.foot { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.foot-left { display: flex; gap: 10px; }
.tab-badge :deep(.el-badge__content) { top: 6px; }
.dot :deep(.el-badge__content.is-dot) { top: 6px; right: -2px; }

/* Markdown 预览样式：与后端 PDF CSS 对齐，避免浏览器与导出 PDF 视觉差异 */
.view-toggle { margin-left: auto; }
.md-preview {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  padding: 18px 22px;
  min-height: 280px;
  max-height: 480px;
  overflow-y: auto;
  background: #fff;
  line-height: 1.85;
  color: #1f2937;
  font-size: 13.5px;
}
.md-preview :deep(h1),
.md-preview :deep(h2),
.md-preview :deep(h3),
.md-preview :deep(h4) { font-weight: 600; line-height: 1.4; }
.md-preview :deep(h2) {
  font-size: 17px; margin: 18px 0 10px; padding-left: 10px;
  border-left: 4px solid #2563eb; color: #1e3a8a;
}
.md-preview :deep(h3) { font-size: 15px; margin: 14px 0 8px; color: #1e40af; }
.md-preview :deep(h4) { font-size: 14px; margin: 10px 0 6px; color: #374151; }
.md-preview :deep(p) { margin: 8px 0; }
.md-preview :deep(ul),
.md-preview :deep(ol) { margin: 6px 0 6px 22px; padding: 0; }
.md-preview :deep(li) { margin: 3px 0; }
.md-preview :deep(strong) { color: #0f172a; font-weight: 700; }
.md-preview :deep(code) {
  background: #f3f4f6; padding: 1px 6px; border-radius: 3px;
  font-family: Consolas, monospace; color: #be185d;
}
.md-preview :deep(pre) {
  background: #1e293b; color: #e2e8f0; padding: 12px;
  border-radius: 6px; overflow: auto;
}
.md-preview :deep(table) {
  border-collapse: collapse; width: 100%; margin: 10px 0; font-size: 12.5px;
}
.md-preview :deep(th),
.md-preview :deep(td) {
  border: 1px solid #d1d5db; padding: 6px 10px; text-align: left;
}
.md-preview :deep(th) { background: #f3f4f6; font-weight: 600; }
.md-preview :deep(blockquote) {
  border-left: 4px solid #cbd5e1; color: #475569;
  margin: 8px 0; padding: 6px 14px; background: #f8fafc;
}
</style>
