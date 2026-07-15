<template>
  <div class="class-manage" :class="{ embedded }">
    <div class="page-head" v-if="!embedded">
      <h2 class="page-title">班级管理</h2>
      <p class="page-sub">维护学院内「专业-入学年级-班级」结构，并配置教师的可见范围（班主任 / 专业老师 / 届老师）</p>
    </div>

    <el-tabs v-model="tab">
      <!-- ============ 班级 ============ -->
      <el-tab-pane label="班级" name="classes">
        <el-card>
          <div class="bar">
            <span class="hint">共 {{ classes.length }} 个班级</span>
            <el-button type="primary" @click="openClassDialog()">新建班级</el-button>
          </div>

          <el-table :data="classes" v-loading="loading" stripe>
            <el-table-column label="班级" min-width="200">
              <template #default="{ row }"><span class="chip">{{ row.code }}</span></template>
            </el-table-column>
            <el-table-column prop="major" label="专业" min-width="150" />
            <el-table-column label="入学年级" width="100">
              <template #default="{ row }">{{ row.enrollYear }} 级</template>
            </el-table-column>
            <el-table-column prop="className" label="班级名" width="90" />
            <el-table-column label="在册学生" width="90">
              <template #default="{ row }"><b>{{ row.studentCount }}</b></template>
            </el-table-column>
            <el-table-column label="状态" width="80">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
                  {{ row.status === 1 ? '启用' : '停用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="220">
              <template #default="{ row }">
                <el-button text type="primary" @click="openAssignDialog(row)">分配学生</el-button>
                <el-button text @click="openClassDialog(row)">编辑</el-button>
                <el-button text type="danger" @click="removeClass(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!loading && !classes.length" description="暂无班级，点击「新建班级」开始" />
        </el-card>
      </el-tab-pane>

      <!-- ============ 教师范围 ============ -->
      <el-tab-pane label="教师范围" name="scopes">
        <el-card>
          <div class="bar">
            <el-select v-model="scopeTeacherId" placeholder="选择教师" filterable style="width:240px"
                       @change="loadScopes">
              <el-option v-for="t in teachers" :key="t.id"
                         :label="(t.realName || t.username) + '（' + t.username + '）'" :value="t.id" />
            </el-select>
            <el-button type="primary" :disabled="!scopeTeacherId" @click="openScopeDialog()">添加范围</el-button>
          </div>

          <el-table v-if="scopeTeacherId" :data="scopes" v-loading="scopeLoading" stripe>
            <el-table-column label="范围类型" width="140">
              <template #default="{ row }">
                <el-tag size="small">{{ scopeTypeLabel(row.scopeType) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="范围" min-width="220">
              <template #default="{ row }"><span class="chip">{{ scopeValueLabel(row) }}</span></template>
            </el-table-column>
            <el-table-column label="操作" width="90">
              <template #default="{ row }">
                <el-button text type="danger" @click="removeScope(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="scopeTeacherId && !scopeLoading && !scopes.length"
                    description="该教师尚未配置范围（无范围 = 看不到任何学生）" />
          <el-empty v-else-if="!scopeTeacherId" description="请先选择一位教师" />
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- 班级新建/编辑 -->
    <el-dialog v-model="classDialog" :title="classForm.id ? '编辑班级' : '新建班级'" width="440px">
      <el-form :model="classForm" label-width="90px">
        <el-form-item label="专业" required>
          <el-input v-model="classForm.major" placeholder="如 软件工程" />
        </el-form-item>
        <el-form-item label="入学年级" required>
          <el-input-number v-model="classForm.enrollYear" :min="2000" :max="2100" controls-position="right" />
        </el-form-item>
        <el-form-item label="班级名" required>
          <el-input v-model="classForm.className" placeholder="如 1班" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="classForm.status" :active-value="1" :inactive-value="0" />
          <span class="hint" style="margin-left:8px">{{ classForm.status === 1 ? '启用' : '停用' }}</span>
        </el-form-item>
        <el-form-item label="统一命名">
          <span class="chip">{{ previewCode || '专业-入学年级-班级' }}</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="classDialog = false">取消</el-button>
        <el-button type="primary" @click="submitClass">保存</el-button>
      </template>
    </el-dialog>

    <!-- 分配学生 -->
    <el-dialog v-model="assignDialog" :title="'分配学生 → ' + (assignTarget?.code || '')" width="520px">
      <p class="hint" style="margin-bottom:12px">选择要归属到该班级的学生（会覆盖其原有班级归属）</p>
      <el-select v-model="assignUserIds" multiple filterable placeholder="选择学生" style="width:100%">
        <el-option v-for="s in allStudents" :key="s.id"
                   :label="(s.realName || s.username) + '（' + s.username + '）'" :value="s.id" />
      </el-select>
      <template #footer>
        <el-button @click="assignDialog = false">取消</el-button>
        <el-button type="primary" :disabled="!assignUserIds.length" @click="submitAssign">分配</el-button>
      </template>
    </el-dialog>

    <!-- 添加范围 -->
    <el-dialog v-model="scopeDialog" title="添加教师范围" width="440px">
      <el-form :model="scopeForm" label-width="90px">
        <el-form-item label="范围类型" required>
          <el-select v-model="scopeForm.scopeType" placeholder="选择类型" style="width:100%" @change="scopeForm.scopeValue = ''">
            <el-option label="班主任（本班）" value="CLASS" />
            <el-option label="专业老师（整个专业）" value="MAJOR" />
            <el-option label="届老师（某入学年级）" value="GRADE" />
          </el-select>
        </el-form-item>
        <el-form-item label="范围值" required>
          <el-select v-if="scopeForm.scopeType === 'CLASS'" v-model="scopeForm.scopeValue" placeholder="选择班级" filterable style="width:100%">
            <el-option v-for="c in classes" :key="c.id" :label="c.code" :value="String(c.id)" />
          </el-select>
          <el-select v-else-if="scopeForm.scopeType === 'MAJOR'" v-model="scopeForm.scopeValue" placeholder="选择专业" filterable style="width:100%">
            <el-option v-for="m in filters.majors" :key="m" :label="m" :value="m" />
          </el-select>
          <el-select v-else-if="scopeForm.scopeType === 'GRADE'" v-model="scopeForm.scopeValue" placeholder="选择入学年级" style="width:100%">
            <el-option v-for="y in filters.years" :key="y" :label="y + ' 级'" :value="String(y)" />
          </el-select>
          <span v-else class="hint">请先选择范围类型</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="scopeDialog = false">取消</el-button>
        <el-button type="primary" :disabled="!scopeForm.scopeType || !scopeForm.scopeValue" @click="submitScope">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getClasses, saveClass, deleteClass, assignStudentsToClass, getClassFilters,
  getTeacherScopes, saveTeacherScope, deleteTeacherScope, getUsers
} from '@/api/admin'
import { toList } from '@/utils/list'

// embedded=true 时隐藏页头与内部标签栏；section 由「组织管理」中心控制显示「班级」还是「教师范围」
const props = defineProps({
  embedded: { type: Boolean, default: false },
  section: { type: String, default: '' }   // 'classes' | 'scopes'
})

const tab = ref('classes')
// 中心切「班级/教师范围」标签时，同步内部 tab（embedded 下内部标签栏已隐藏，仅由此驱动）
watch(() => props.section, v => { if (v) tab.value = v }, { immediate: true })

// ---------- 班级 ----------
const classes = ref([])
const loading = ref(false)
const filters = ref({ majors: [], years: [] })

async function loadClasses() {
  loading.value = true
  try {
    classes.value = toList(await getClasses())
  } finally {
    loading.value = false
  }
}
async function loadFilters() {
  const data = await getClassFilters()
  filters.value = { majors: data?.majors || [], years: data?.years || [] }
}

// 班级新建/编辑
const classDialog = ref(false)
const classForm = ref({ id: null, major: '', enrollYear: new Date().getFullYear(), className: '', status: 1 })
const previewCode = computed(() => {
  const f = classForm.value
  return f.major && f.enrollYear && f.className ? `${f.major}-${f.enrollYear}-${f.className}` : ''
})
function openClassDialog(row) {
  classForm.value = row
    ? { id: row.id, major: row.major, enrollYear: row.enrollYear, className: row.className, status: row.status }
    : { id: null, major: '', enrollYear: new Date().getFullYear(), className: '', status: 1 }
  classDialog.value = true
}
async function submitClass() {
  const f = classForm.value
  if (!f.major || !f.enrollYear || !f.className) {
    ElMessage.warning('专业、入学年级、班级名不能为空')
    return
  }
  await saveClass(f)
  ElMessage.success('已保存')
  classDialog.value = false
  await Promise.all([loadClasses(), loadFilters()])
}
async function removeClass(row) {
  try {
    await ElMessageBox.confirm(`确认删除班级「${row.code}」？`, '提示', { type: 'warning' })
  } catch {
    return
  }
  await deleteClass(row.id)
  ElMessage.success('已删除')
  loadClasses()
}

// 分配学生
const assignDialog = ref(false)
const assignTarget = ref(null)
const assignUserIds = ref([])
const allStudents = ref([])
async function openAssignDialog(row) {
  assignTarget.value = row
  assignUserIds.value = []
  if (!allStudents.value.length) {
    allStudents.value = toList(await getUsers({ role: 'STUDENT', pageNum: 1, pageSize: 500 }))
  }
  assignDialog.value = true
}
async function submitAssign() {
  await assignStudentsToClass(assignTarget.value.id, assignUserIds.value)
  ElMessage.success('已分配')
  assignDialog.value = false
  loadClasses()
}

// ---------- 教师范围 ----------
const teachers = ref([])
const scopeTeacherId = ref(null)
const scopes = ref([])
const scopeLoading = ref(false)

async function loadTeachers() {
  teachers.value = toList(await getUsers({ role: 'TEACHER', pageNum: 1, pageSize: 500 }))
}
async function loadScopes() {
  if (!scopeTeacherId.value) return
  scopeLoading.value = true
  try {
    scopes.value = toList(await getTeacherScopes(scopeTeacherId.value))
  } finally {
    scopeLoading.value = false
  }
}
function scopeTypeLabel(t) {
  return { CLASS: '班主任', MAJOR: '专业老师', GRADE: '届老师' }[t] || t
}
function scopeValueLabel(row) {
  if (row.scopeType === 'CLASS') {
    const c = classes.value.find(c => String(c.id) === String(row.scopeValue))
    return c ? c.code : `班级#${row.scopeValue}`
  }
  if (row.scopeType === 'GRADE') return `${row.scopeValue} 级`
  return row.scopeValue
}

const scopeDialog = ref(false)
const scopeForm = ref({ scopeType: '', scopeValue: '' })
function openScopeDialog() {
  scopeForm.value = { scopeType: '', scopeValue: '' }
  scopeDialog.value = true
}
async function submitScope() {
  await saveTeacherScope({
    teacherId: scopeTeacherId.value,
    scopeType: scopeForm.value.scopeType,
    scopeValue: scopeForm.value.scopeValue
  })
  ElMessage.success('已添加')
  scopeDialog.value = false
  loadScopes()
}
async function removeScope(row) {
  try {
    await ElMessageBox.confirm('确认删除该范围？', '提示', { type: 'warning' })
  } catch {
    return
  }
  await deleteTeacherScope(row.id)
  ElMessage.success('已删除')
  loadScopes()
}

onMounted(async () => {
  await Promise.all([loadClasses(), loadFilters(), loadTeachers()])
})
</script>

<style scoped>
.bar { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 16px; flex-wrap: wrap; }
.hint { color: var(--color-text-tertiary); font-size: 13px; }
/* 嵌入「组织管理」中心时，隐藏内部 el-tabs 的标签栏（改由中心的顶层标签驱动） */
.class-manage.embedded :deep(.el-tabs__header) { display: none; }
</style>
