<template>
  <div class="report-list">
    <div class="page-head with-actions">
      <div>
        <h2 class="page-title">报告中心</h2>
        <p class="page-sub">直接选大类与范围生成报告并下载（市场行业 / 学生就业两类）</p>
      </div>
      <div class="page-actions">
        <el-button type="primary" @click="openGenerateDialog">生成新报告</el-button>
      </div>
    </div>

    <!-- #3 两类报告的生成依据说明 -->
    <el-alert type="info" :closable="false" show-icon class="basis">
      <template #title>两类报告的数据来源</template>
      <div class="basis-body">
        <p><b>市场行业报告</b>：汇总全校 <code>职位库</code>（采集 + 站内发布）的
          <b>行业 / 技能 / 城市 / 学历 / 薪资</b> 分布，反映就业市场供给。
          <el-tag size="small" type="success" effect="plain">发布即全体学生可见</el-tag></p>
        <p><b>学生就业数据报告</b>：按选定的 <b>专业 / 入学年级 / 班级</b> 范围，
          汇总本院学生的 <b>画像 / 投递 / 求职意向</b>，反映学生就业动向（纯聚合，不含个人姓名）。
          <el-tag size="small" type="warning" effect="plain">需按范围「发送」给学生</el-tag></p>
      </div>
    </el-alert>

    <el-card>
      <el-table :data="records" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" label="报告名称" min-width="240">
          <template #default="{ row }">{{ row.name || '—' }}</template>
        </el-table-column>
        <el-table-column label="大类" width="110">
          <template #default="{ row }">
            <el-tag :type="row.category === 'EMPLOYMENT' ? 'warning' : 'primary'" size="small">
              {{ categoryLabel(row.category) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="格式" width="90">
          <template #default="{ row }"><span class="chip">{{ row.fileType || '—' }}</span></template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="生成时间" width="150">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="失败原因" min-width="180">
          <template #default="{ row }">
            <span v-if="row.status === 'FAILED'" class="err" :title="row.errorMsg">{{ row.errorMsg }}</span>
            <span v-else>—</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'SUCCESS'" text type="primary" size="small" @click="handleDownload(row)">下载</el-button>
            <el-button v-if="row.status === 'SUCCESS' && row.category === 'EMPLOYMENT'"
                       text type="success" size="small" @click="openDeliver(row)">发送</el-button>
            <el-tag v-else-if="row.status === 'SUCCESS' && row.category === 'MARKET'"
                    size="small" type="info" effect="plain" class="broadcast-tag">全体可见</el-tag>
            <el-button text type="danger" size="small" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && !records.length" description="暂无报告，点击右上角生成一份" />

      <el-pagination
        v-if="total > size"
        v-model:current-page="page" :page-size="size"
        :total="total" layout="total, prev, pager, next"
        @current-change="loadRecords"
      />
    </el-card>

    <!-- 生成报告对话框 -->
    <el-dialog v-model="generateVisible" title="生成新报告" width="500px">
      <el-form ref="genFormRef" :model="genForm" label-width="100px">
        <el-form-item label="报告大类" prop="category">
          <el-select v-model="genForm.category" style="width:100%" @change="onCategoryChange">
            <el-option label="市场行业报告（全平台就业市场数据）" value="MARKET" />
            <el-option label="学生就业数据报告（按专业/年级/班级）" value="EMPLOYMENT" />
          </el-select>
        </el-form-item>

        <!-- 学生就业报告：选择范围（不选=全校；班级优先，否则按专业/年级） -->
        <template v-if="genForm.category === 'EMPLOYMENT'">
          <el-form-item label="报告范围">
            <span class="hint">不选任何项 = 全校；可选具体班级，或按专业 / 入学年级</span>
          </el-form-item>
          <el-form-item label="按班级">
            <el-select v-model="genForm.classId" clearable filterable placeholder="选择班级（可选）"
                       style="width:100%" @change="onClassPick">
              <el-option v-for="c in scopeClasses" :key="c.id" :label="c.code" :value="c.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="按专业" v-if="!genForm.classId">
            <el-select v-model="genForm.major" clearable filterable placeholder="选择专业（可选）" style="width:100%">
              <el-option v-for="m in scopeMajors" :key="m" :label="m" :value="m" />
            </el-select>
          </el-form-item>
          <el-form-item label="按入学年级" v-if="!genForm.classId">
            <el-select v-model="genForm.enrollYear" clearable placeholder="选择年级（可选）" style="width:100%">
              <el-option v-for="y in scopeYears" :key="y" :label="y + ' 级'" :value="y" />
            </el-select>
          </el-form-item>
        </template>

        <el-form-item label="导出格式" prop="fileType">
          <el-select v-model="genForm.fileType" style="width:100%">
            <el-option label="PDF" value="PDF" />
            <el-option label="Word" value="WORD" />
            <el-option label="HTML" value="HTML" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="generateVisible = false">取消</el-button>
        <el-button type="primary" :loading="generating" @click="handleGenerate">生成</el-button>
      </template>
    </el-dialog>

    <!-- 发送就业报告给某范围学生 -->
    <el-dialog v-model="deliverVisible" title="发送报告给学生" width="480px">
      <p class="deliver-name">《{{ deliverRow?.name }}》</p>
      <el-form label-width="90px">
        <el-form-item label="发送范围">
          <el-select v-model="deliverForm.targetType" style="width:100%" @change="deliverForm.targetValue = null">
            <el-option label="全体学生" value="ALL" />
            <el-option label="按专业" value="MAJOR" />
            <el-option label="按入学年级" value="GRADE" />
            <el-option label="按班级" value="CLASS" />
          </el-select>
        </el-form-item>
        <el-form-item label="专业" v-if="deliverForm.targetType === 'MAJOR'">
          <el-select v-model="deliverForm.targetValue" filterable placeholder="选择专业" style="width:100%">
            <el-option v-for="m in scopeMajors" :key="m" :label="m" :value="m" />
          </el-select>
        </el-form-item>
        <el-form-item label="入学年级" v-if="deliverForm.targetType === 'GRADE'">
          <el-select v-model="deliverForm.targetValue" placeholder="选择年级" style="width:100%">
            <el-option v-for="y in scopeYears" :key="y" :label="y + ' 级'" :value="String(y)" />
          </el-select>
        </el-form-item>
        <el-form-item label="班级" v-if="deliverForm.targetType === 'CLASS'">
          <el-select v-model="deliverForm.targetValue" filterable placeholder="选择班级" style="width:100%">
            <el-option v-for="c in scopeClasses" :key="c.id" :label="c.code" :value="String(c.id)" />
          </el-select>
        </el-form-item>
      </el-form>
      <p class="hint">学生将在「我的报告 → 收到的报告」中看到并可下载；已发过的学生不会重复接收。</p>
      <template #footer>
        <el-button @click="deliverVisible = false">取消</el-button>
        <el-button type="primary" :loading="delivering" @click="handleDeliver">发送</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import {
  getReportRecords, deleteReportRecord, downloadReport, generateReport,
  getClasses, getClassFilters, deliverReport
} from '@/api/admin'
import { toList, toTotal } from '@/utils/list'
import { formatTime } from '@/utils/format'
import { saveBlob } from '@/utils/download'
import { ElMessage, ElMessageBox } from 'element-plus'

const records = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)

async function loadRecords() {
  loading.value = true
  try {
    const data = await getReportRecords({ pageNum: page.value, pageSize: size.value })
    records.value = toList(data)
    total.value = toTotal(data, records.value)
  } finally {
    loading.value = false
  }
}

async function handleDelete(id) {
  try { await ElMessageBox.confirm('确定删除该报告记录及其文件吗？', '确认', { type: 'warning' }) } catch { return }
  try {
    await deleteReportRecord(id)
    ElMessage.success('已删除')
    loadRecords()
  } catch { /* 拦截器已提示 */ }
}

async function handleDownload(row) {
  const ext = { PDF: 'pdf', WORD: 'docx', HTML: 'html' }[row.fileType] || 'bin'
  try { await saveBlob(downloadReport(row.id), `${row.name || 'report'}.${ext}`) } catch { /* 拦截器已提示 */ }
}

// ---- 生成报告 ----
const generateVisible = ref(false)
const generating = ref(false)
const genFormRef = ref(null)
const genForm = reactive({ category: 'MARKET', fileType: 'PDF', major: null, enrollYear: null, classId: null })

// 就业类报告的范围候选
const scopeClasses = ref([])
const scopeMajors = ref([])
const scopeYears = ref([])
let scopeLoaded = false

function resetScope() {
  genForm.major = null
  genForm.enrollYear = null
  genForm.classId = null
}
function onClassPick() {
  // 班级优先：选了班级就清掉专业/年级（后端也是班级优先）
  if (genForm.classId) { genForm.major = null; genForm.enrollYear = null }
}
async function loadScopeOptions() {
  if (scopeLoaded) return
  try {
    const [cls, filters] = await Promise.all([getClasses(), getClassFilters()])
    scopeClasses.value = toList(cls)
    scopeMajors.value = filters?.majors || []
    scopeYears.value = filters?.years || []
    scopeLoaded = true
  } catch { /* 拦截器已提示 */ }
}
async function onCategoryChange() {
  resetScope()
  if (genForm.category === 'EMPLOYMENT') await loadScopeOptions()
}

function openGenerateDialog() {
  genForm.category = 'MARKET'
  genForm.fileType = 'PDF'
  resetScope()
  generateVisible.value = true
}

async function handleGenerate() {
  generating.value = true
  try {
    const payload = genForm.category === 'EMPLOYMENT'
      ? { category: 'EMPLOYMENT', fileType: genForm.fileType, major: genForm.major, enrollYear: genForm.enrollYear, classId: genForm.classId }
      : { category: 'MARKET', fileType: genForm.fileType }
    await generateReport(payload)
    ElMessage.success('报告已生成')
    generateVisible.value = false
    loadRecords()
  } catch {
    // 生成是同步的，失败原因已由拦截器提示，同时会落一条 FAILED 记录
    loadRecords()
  } finally {
    generating.value = false
  }
}

// ---- 发送报告给学生 ----
const deliverVisible = ref(false)
const delivering = ref(false)
const deliverRow = ref(null)
const deliverForm = reactive({ targetType: 'ALL', targetValue: null })

async function openDeliver(row) {
  deliverRow.value = row
  deliverForm.targetType = 'ALL'
  deliverForm.targetValue = null
  await loadScopeOptions()   // 复用生成对话框的范围候选
  deliverVisible.value = true
}

async function handleDeliver() {
  if (deliverForm.targetType !== 'ALL' && !deliverForm.targetValue) {
    ElMessage.warning('请选择发送范围的具体值')
    return
  }
  delivering.value = true
  try {
    const n = await deliverReport(deliverRow.value.id, {
      targetType: deliverForm.targetType,
      targetValue: deliverForm.targetValue
    })
    ElMessage.success(n > 0 ? `已发送给 ${n} 名学生` : '该范围内的学生此前均已收到')
    deliverVisible.value = false
  } catch { /* 拦截器已提示 */ } finally {
    delivering.value = false
  }
}

function statusTag(status) {
  return { SUCCESS: 'success', FAILED: 'danger', GENERATING: 'warning', PENDING: 'info' }[status] || 'info'
}
function statusLabel(status) {
  return { SUCCESS: '已完成', FAILED: '失败', GENERATING: '生成中', PENDING: '排队中' }[status] || status
}
function categoryLabel(category) {
  return { MARKET: '市场行业', EMPLOYMENT: '学生就业' }[category] || '市场行业'
}

onMounted(loadRecords)
</script>

<style scoped>
.err { color: var(--color-danger); font-size: 12px; display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.hint { color: var(--color-text-tertiary); font-size: 12px; }
.basis { margin-bottom: 14px; }
.basis-body p { margin: 4px 0; line-height: 1.7; font-size: 12.5px; }
.basis-body code { background: var(--color-fill-light, rgba(0,0,0,.05)); padding: 0 4px; border-radius: 3px; }
.deliver-name { font-weight: 600; margin-bottom: 12px; }
.broadcast-tag { margin: 0 6px; }
</style>
