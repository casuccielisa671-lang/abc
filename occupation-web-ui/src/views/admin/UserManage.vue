<template>
  <div class="user-manage">
    <div class="page-head with-actions">
      <div>
        <h2 class="page-title">用户管理</h2>
        <p class="page-sub">管理本校账号、角色与启用状态。账号统一由管理员创建或批量导入，不开放注册。</p>
      </div>
      <div class="page-actions">
        <el-button @click="importVisible = true">批量导入</el-button>
        <el-button type="primary" @click="openDialog()">新增用户</el-button>
      </div>
    </div>

    <el-card>
      <div class="search-bar">
        <el-input
          v-model="searchKeyword" placeholder="搜索用户名 / 真实姓名" clearable
          style="width:220px" @keyup.enter="search" @clear="search"
        />
        <el-select v-model="searchRole" placeholder="角色筛选" clearable style="width:140px" @change="search">
          <el-option label="管理员" value="ADMIN" />
          <el-option label="教师" value="TEACHER" />
          <el-option label="学生" value="STUDENT" />
          <el-option label="HR" value="HR" />
        </el-select>
        <el-button type="primary" @click="search">查询</el-button>
      </div>

      <el-table :data="users" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column prop="realName" label="真实姓名" min-width="110">
          <template #default="{ row }">{{ row.realName || '—' }}</template>
        </el-table-column>
        <el-table-column label="角色" width="100">
          <template #default="{ row }">
            <span class="chip">{{ roleLabel(row.role) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" width="130">
          <template #default="{ row }">{{ row.phone || '—' }}</template>
        </el-table-column>
        <el-table-column prop="email" label="邮箱" min-width="180">
          <template #default="{ row }">{{ row.email || '—' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="150">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="openDialog(row)">编辑</el-button>
            <el-button
              text size="small" :type="row.status === 1 ? 'danger' : 'success'"
              @click="handleToggleStatus(row)"
            >{{ row.status === 1 ? '禁用' : '启用' }}</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && !users.length" description="没有符合条件的用户" />

      <el-pagination
        v-if="total > size"
        v-model:current-page="page" :page-size="size"
        :total="total" layout="total, prev, pager, next"
        @current-change="loadUsers"
      />
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑用户' : '新增用户'" width="480px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="登录用户名" :disabled="!!editingId" />
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="用户真实姓名" />
        </el-form-item>
        <el-form-item label="密码" :prop="editingId ? '' : 'password'">
          <el-input
            v-model="form.password" type="password" show-password
            :placeholder="editingId ? '留空则不修改密码' : '请输入密码'"
          />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="form.role" style="width:100%">
            <el-option label="管理员" value="ADMIN" />
            <el-option label="教师" value="TEACHER" />
            <el-option label="学生" value="STUDENT" />
            <el-option label="HR" value="HR" />
          </el-select>
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="选填" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="选填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 批量导入 -->
    <el-dialog v-model="importVisible" title="Excel 批量导入用户" width="560px" @closed="resetImport">
      <el-alert type="info" :closable="false" show-icon style="margin-bottom:16px">
        <template #title>整体校验，全部通过才写库</template>
        只要有一行不合法，本次导入不会写入任何账号，你可以改好 Excel 再传一次。
      </el-alert>

      <div class="import-actions">
        <el-button :loading="templateLoading" @click="handleDownloadTemplate">下载导入模板</el-button>
        <el-upload
          :auto-upload="false" :show-file-list="false" accept=".xlsx"
          :on-change="handleFileChange"
        >
          <el-button type="primary">选择 Excel 文件</el-button>
        </el-upload>
      </div>

      <p v-if="selectedFile" class="file-name">已选择：{{ selectedFile.name }}</p>

      <!-- 导入结果 -->
      <div v-if="report" class="report">
        <el-result
          v-if="!report.errors.length"
          icon="success" :title="`成功导入 ${report.imported} 个账号`"
          :sub-title="report.defaultPassword ? `未填写密码的账号初始密码为 ${report.defaultPassword}` : ''"
        />
        <template v-else>
          <p class="report-head">
            共 {{ report.total }} 行，{{ report.errors.length }} 行不合法，<strong>本次未写入任何账号</strong>。
          </p>
          <el-table :data="report.errors" size="small" max-height="240" stripe>
            <el-table-column prop="rowNum" label="Excel 行号" width="100" />
            <el-table-column prop="username" label="用户名" width="130">
              <template #default="{ row }">{{ row.username || '（空）' }}</template>
            </el-table-column>
            <el-table-column prop="reason" label="原因" min-width="220" />
          </el-table>
        </template>
      </div>

      <template #footer>
        <el-button @click="importVisible = false">关闭</el-button>
        <el-button type="primary" :disabled="!selectedFile" :loading="importing" @click="handleImport">
          开始导入
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import {
  getUsers, createUser, updateUser, updateUserStatus,
  batchImportUsers, downloadImportTemplate
} from '@/api/admin'
import { toList, toTotal } from '@/utils/list'
import { formatTime } from '@/utils/format'
import { saveBlob } from '@/utils/download'
import { ElMessage, ElMessageBox } from 'element-plus'

const users = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)
const searchKeyword = ref('')
const searchRole = ref('')

async function loadUsers() {
  loading.value = true
  try {
    // 后端读的是 pageNum/pageSize，传 page/size 会被忽略、永远返回第一页
    const params = { pageNum: page.value, pageSize: size.value }
    if (searchKeyword.value) params.keyword = searchKeyword.value
    if (searchRole.value) params.role = searchRole.value
    const data = await getUsers(params)
    users.value = toList(data)
    total.value = toTotal(data, users.value)
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 1
  loadUsers()
}

async function handleToggleStatus(row) {
  const action = row.status === 1 ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(`确定${action}用户「${row.username}」吗？`, '确认', { type: 'warning' })
  } catch {
    return // 用户取消
  }
  try {
    await updateUserStatus(row.id, row.status === 1 ? 0 : 1)
    ElMessage.success(`已${action}`)
    loadUsers()
  } catch { /* 拦截器已提示 */ }
}

// ---- 新增 / 编辑 ----
const dialogVisible = ref(false)
const editingId = ref(null)
const saving = ref(false)
const formRef = ref(null)
// 租户不在表单里：它由登录态的 JWT 决定，后端自动注入 tenant_id
const form = reactive({ username: '', realName: '', password: '', role: 'STUDENT', phone: '', email: '' })

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' }
  ],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

function openDialog(row) {
  editingId.value = row ? row.id : null
  Object.assign(form, {
    username: row?.username || '',
    realName: row?.realName || '',
    password: '',
    role: row?.role || 'STUDENT',
    phone: row?.phone || '',
    email: row?.email || ''
  })
  dialogVisible.value = true
}

async function handleSave() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const payload = { ...form }
    if (editingId.value && !payload.password) delete payload.password
    if (editingId.value) {
      await updateUser(editingId.value, payload)
      ElMessage.success('更新成功')
    } else {
      await createUser(payload)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadUsers()
  } catch {
    // 拦截器已提示
  } finally {
    saving.value = false
  }
}

// ---- 批量导入 ----
const importVisible = ref(false)
const importing = ref(false)
const templateLoading = ref(false)
const selectedFile = ref(null)
const report = ref(null)

function handleFileChange(file) {
  selectedFile.value = file.raw
  report.value = null
}

function resetImport() {
  selectedFile.value = null
  report.value = null
}

async function handleDownloadTemplate() {
  templateLoading.value = true
  try {
    await saveBlob(downloadImportTemplate(), '用户批量导入模板.xlsx')
  } catch {
    // 拦截器已提示
  } finally {
    templateLoading.value = false
  }
}

async function handleImport() {
  importing.value = true
  try {
    report.value = await batchImportUsers(selectedFile.value)
    if (!report.value.errors.length) {
      ElMessage.success(`成功导入 ${report.value.imported} 个账号`)
      loadUsers()
    }
  } catch {
    // 拦截器已提示
  } finally {
    importing.value = false
  }
}

function roleLabel(role) {
  return { ADMIN: '管理员', TEACHER: '教师', STUDENT: '学生', HR: 'HR' }[role] || role
}

onMounted(loadUsers)
</script>

<style scoped>
.search-bar { display: flex; align-items: center; gap: 12px; margin-bottom: 20px; flex-wrap: wrap; }
.import-actions { display: flex; gap: 12px; align-items: center; }
.file-name { font-size: 13px; color: var(--app-ink-3); margin: 12px 0 0; }
.report { margin-top: 20px; }
.report-head { font-size: 13px; color: var(--app-ink-2); margin: 0 0 12px; }
</style>
