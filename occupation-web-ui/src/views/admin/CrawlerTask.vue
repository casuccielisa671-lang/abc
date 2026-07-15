<template>
  <div class="crawler-page">
    <div class="page-head with-actions">
      <div>
        <h2 class="page-title">采集任务管理</h2>
        <p class="page-sub">
          管理岗位与官方公开招聘公告采集任务，用于行业热度、城市分布等数据面板。行业资讯/新闻请到「资讯管理」中拉取和维护。
        </p>
      </div>
      <div class="page-actions">
        <el-button type="primary" @click="openDialog()">新增任务</el-button>
      </div>
    </div>

    <el-alert type="info" :closable="false" show-icon class="verify-tip">
      <template #title>如何判断采集是否真实有效？</template>
      <div>
        “官方公开招聘公告”会写入岗位采集链路，可在岗位数据中核对 sourceUrl。
        行业资讯是用户知识补充内容，属于独立资讯模块，请在「资讯管理」中拉取外部资讯。
        MOCK 只用于演示和本地初始化，不代表外部真实数据。
      </div>
    </el-alert>

    <el-card>
      <el-table :data="tasks" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="sourceName" label="任务名称" min-width="170" />
        <el-table-column label="采集源" width="150">
          <template #default="{ row }">
            <span class="chip">{{ sourceLabel(row.sourceType) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="cronExpr" label="Cron 表达式" width="160" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.statusText || (row.status === 1 ? '运行中' : '停止') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="210" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status !== 1" text type="primary" size="small" @click="handleStart(row)">
              启动
            </el-button>
            <el-button v-else text type="warning" size="small" @click="handleStop(row.id)">停止</el-button>
            <el-button text type="primary" size="small" @click="openDialog(row)">编辑</el-button>
            <el-button text type="danger" size="small" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && !tasks.length" description="暂无采集任务" />
      <el-pagination
        v-if="total > size"
        v-model:current-page="page"
        :page-size="size"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="loadTasks"
      />
    </el-card>

    <el-card style="margin-top:16px">
      <template #header>采集日志</template>
      <el-table :data="logs" v-loading="logLoading" stripe size="small">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="taskId" label="任务ID" width="90" />
        <el-table-column label="开始时间" width="170">
          <template #default="{ row }">{{ formatTime(row.startTime) }}</template>
        </el-table-column>
        <el-table-column label="结束时间" width="170">
          <template #default="{ row }">{{ formatTime(row.endTime) }}</template>
        </el-table-column>
        <el-table-column prop="recordCount" label="采集条数" width="100" />
        <el-table-column prop="duration" label="耗时" width="90" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="logStatusTag(row.status)" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="errorMsg" label="错误信息" min-width="220" show-overflow-tooltip />
      </el-table>

      <el-empty v-if="!logLoading && !logs.length" description="暂无采集日志" />
      <el-pagination
        v-if="logTotal > logSize"
        v-model:current-page="logPage"
        :page-size="logSize"
        :total="logTotal"
        layout="total, prev, pager, next"
        @current-change="loadLogs"
      />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑任务' : '新增任务'" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="采集源类型" prop="sourceType">
          <el-select v-model="form.sourceType" style="width:100%">
            <el-option label="MOCK（本地样例，不访问外网）" value="MOCK" />
            <el-option label="官方公开招聘公告（写入岗位库）" value="OFFICIAL_PUBLIC" />
            <el-option label="智联招聘（不推荐，易受限制）" value="ZHAOPIN" />
          </el-select>
        </el-form-item>

        <el-alert
          v-if="form.sourceType !== 'MOCK'"
          type="warning"
          :closable="false"
          show-icon
          class="form-alert"
        >
          外部岗位采集会先校验 robots.txt，并以单线程低频访问。招聘网站可能因反爬、改版或合规限制采集失败；
          行业资讯请使用「资讯管理」中的拉取外部资讯。
        </el-alert>

        <el-form-item label="任务名称" prop="sourceName">
          <el-input v-model="form.sourceName" placeholder="如：官方公开招聘公告示例" />
        </el-form-item>
        <el-form-item label="URL/数据文件" prop="urlPattern">
          <el-input v-model="form.urlPattern" :placeholder="urlPatternHint" />
          <span class="form-tip">{{ urlPatternHint }}</span>
        </el-form-item>
        <el-form-item label="Cron 表达式" prop="cronExpr">
          <el-input v-model="form.cronExpr" placeholder="如：0 0 6 * * ?（每天 6 点）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- MOCK 采集进度弹窗 -->
    <el-dialog
      v-model="crawlProgress.visible"
      :title="crawlProgress.done ? '采集完成' : '采集中'"
      width="380px"
      :show-close="crawlProgress.done"
      :close-on-click-modal="crawlProgress.done"
      :close-on-press-escape="crawlProgress.done"
    >
      <div class="crawl-progress">
        <el-progress
          :percentage="crawlProgress.pct"
          :status="crawlProgress.done ? 'success' : ''"
          :stroke-width="14"
          :duration="1"
        />
        <p class="cp-text">
          <template v-if="crawlProgress.done">
            ✓ 采集完成，本次采集 <b>{{ crawlProgress.count }}</b> 条职位，市场参考数据已刷新
          </template>
          <template v-else>正在读取并清洗职位数据…</template>
        </p>
      </div>
      <template #footer v-if="crawlProgress.done">
        <el-button type="primary" @click="crawlProgress.visible = false">知道了</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import {
  createCrawlerTask,
  deleteCrawlerTask,
  getCrawlerLogs,
  getCrawlerTask,
  getCrawlerTasks,
  startCrawlerTask,
  stopCrawlerTask,
  updateCrawlerTask
} from '@/api/admin'
import { toList, toTotal } from '@/utils/list'
import { formatTime } from '@/utils/format'
import { ElMessage, ElMessageBox } from 'element-plus'

const tasks = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)

// MOCK 采集进度弹窗（同步瞬时完成，用短动画把结果“铺开成过程”，结尾落到真实条数）
const crawlProgress = ref({ visible: false, pct: 0, done: false, count: 0 })

const logs = ref([])
const logLoading = ref(false)
const logPage = ref(1)
const logSize = ref(10)
const logTotal = ref(0)

const dialogVisible = ref(false)
const editingId = ref(null)
const saving = ref(false)
const formRef = ref(null)
const form = reactive({
  sourceType: 'MOCK',
  sourceName: '',
  urlPattern: '',
  cronExpr: ''
})

const rules = {
  sourceType: [{ required: true, message: '请选择采集源类型', trigger: 'change' }],
  sourceName: [{ required: true, message: '请输入任务名称', trigger: 'blur' }]
}

const urlPatternHint = computed(() => ({
  MOCK: '填写 mock 数据文件名，如 mock-jobs.json',
  OFFICIAL_PUBLIC: '填写官方公开招聘公告列表页，如 url=https://example.gov.cn/jobs/&maxItems=30',
  ZHAOPIN: '填写参数串，如 kw=Java&jl=653&maxPages=2'
}[form.sourceType] || ''))

async function loadTasks() {
  loading.value = true
  try {
    const data = await getCrawlerTasks({ page: page.value, size: size.value })
    tasks.value = toList(data)
    total.value = toTotal(data, tasks.value)
  } finally {
    loading.value = false
  }
}

async function loadLogs() {
  logLoading.value = true
  try {
    const data = await getCrawlerLogs({ page: logPage.value, size: logSize.value })
    logs.value = toList(data)
    logTotal.value = toTotal(data, logs.value)
  } finally {
    logLoading.value = false
  }
}

async function handleStart(row) {
  // 真实爬虫是异步的（点完还在后台跑），不能显示“完成 N 条”，保持原“已启动”提示
  if (row.sourceType !== 'MOCK') {
    try {
      await startCrawlerTask(row.id)
      ElMessage.success('任务已启动')
      await Promise.all([loadTasks(), loadLogs()])
    } catch { /* handled by interceptor */ }
    return
  }

  // MOCK：同步瞬时完成。用 ~1.2s 进度动画把瞬时结果铺开成有始有终的过程
  const p = crawlProgress.value
  p.visible = true; p.done = false; p.pct = 0; p.count = 0
  const timer = setInterval(() => { p.pct = Math.min(90, p.pct + 6) }, 80)
  const started = Date.now()
  try {
    const count = await startCrawlerTask(row.id)  // 接口返回本次采集条数
    const wait = 1200 - (Date.now() - started)    // 保证动画至少展示 1.2s
    if (wait > 0) await new Promise(r => setTimeout(r, wait))
    clearInterval(timer)
    p.pct = 100
    p.count = count ?? 0
    p.done = true
    await Promise.all([loadTasks(), loadLogs()])
  } catch {
    clearInterval(timer)
    p.visible = false   // 失败由拦截器提示
  }
}

async function handleStop(id) {
  try {
    await stopCrawlerTask(id)
    ElMessage.success('任务已停止')
    await Promise.all([loadTasks(), loadLogs()])
  } catch { /* handled by interceptor */ }
}

async function handleDelete(id) {
  await ElMessageBox.confirm('确定删除该任务吗？', '确认', { type: 'warning' })
  try {
    await deleteCrawlerTask(id)
    ElMessage.success('已删除')
    await loadTasks()
  } catch { /* handled by interceptor */ }
}

async function openDialog(row) {
  editingId.value = row ? row.id : null
  if (row) {
    const detail = await getCrawlerTask(row.id).catch(() => row)
    Object.assign(form, {
      sourceType: detail.sourceType || 'MOCK',
      sourceName: detail.sourceName || '',
      urlPattern: detail.urlPattern || '',
      cronExpr: detail.cronExpr || ''
    })
  } else {
    Object.assign(form, { sourceType: 'MOCK', sourceName: '', urlPattern: '', cronExpr: '' })
  }
  dialogVisible.value = true
}

async function handleSave() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    if (editingId.value) {
      await updateCrawlerTask(editingId.value, { ...form })
      ElMessage.success('更新成功')
    } else {
      await createCrawlerTask({ ...form })
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    await loadTasks()
  } finally {
    saving.value = false
  }
}

function sourceLabel(type) {
  return {
    MOCK: 'MOCK',
    OFFICIAL_PUBLIC: '官方招聘公告',
    ZHAOPIN: '智联招聘',
    NEWS_INFOQ: 'InfoQ 资讯',
    NEWS_OSCHINA: '开源中国资讯',
    BOSS_ZHIPIN: 'BOSS 直聘（已停用）',
    COMPANY_OFFICIAL: '企业官网（未实现）'
  }[type] || type
}

function logStatusTag(status) {
  return { SUCCESS: 'success', FAILED: 'danger', RUNNING: 'warning' }[status] || 'info'
}

onMounted(() => {
  loadTasks()
  loadLogs()
})
</script>

<style scoped>
.form-alert { margin: 0 0 18px 110px; width: calc(100% - 110px); }
.form-tip { color: var(--app-ink-3); font-size: 12px; }
.verify-tip { margin-bottom: 14px; }
.crawl-progress { padding: 6px 2px 2px; }
.cp-text { margin: 16px 0 0; font-size: 13.5px; color: var(--color-text-secondary); line-height: 1.6; text-align: center; }
.cp-text b { color: var(--color-primary); font-size: 16px; }
</style>
