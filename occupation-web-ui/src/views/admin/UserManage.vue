<template>
  <div class="user-manage">
    <div class="page-header">
      <h2>用户管理</h2>
      <el-button type="primary" @click="openDialog()">新增用户</el-button>
    </div>

    <el-card>
      <div class="search-bar">
        <el-input v-model="searchKeyword" placeholder="搜索用户名/真实姓名" clearable style="width:220px"
          @keyup.enter="loadUsers" />
        <el-select v-model="searchRole" placeholder="角色筛选" clearable style="width:140px; margin-left:12px"
          @change="loadUsers">
          <el-option label="管理员" value="ADMIN" />
          <el-option label="教师" value="TEACHER" />
          <el-option label="学生" value="STUDENT" />
          <el-option label="HR" value="HR" />
        </el-select>
        <el-button type="primary" @click="loadUsers" style="margin-left:12px">查询</el-button>
      </div>

      <el-table :data="users" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column prop="realName" label="真实姓名" min-width="100" />
        <el-table-column label="角色" width="100">
          <template #default="{ row }">
            <el-tag :type="roleTag(row.role)" size="small">{{ roleLabel(row.role) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="tenantName" label="租户" width="120" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openDialog(row)">编辑</el-button>
            <el-button size="small" :type="row.status === 1 ? 'warning' : 'success'"
              @click="handleToggleStatus(row)">
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="page" v-model:page-size="size"
        :total="total" layout="total, prev, pager, next"
        @current-change="loadUsers" style="margin-top:16px; justify-content:flex-end"
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
          <el-input v-model="form.password" type="password" show-password
            :placeholder="editingId ? '留空则不修改密码' : '请输入密码'" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="form.role" style="width:100%">
            <el-option label="管理员" value="ADMIN" />
            <el-option label="教师" value="TEACHER" />
            <el-option label="学生" value="STUDENT" />
            <el-option label="HR" value="HR" />
          </el-select>
        </el-form-item>
        <el-form-item label="租户" prop="tenantName">
          <el-input v-model="form.tenantName" placeholder="所属租户名称" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getUsers, createUser, updateUser, updateUserStatus } from '@/api/admin'
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
    const params = { page: page.value, size: size.value }
    if (searchKeyword.value) params.keyword = searchKeyword.value
    if (searchRole.value) params.role = searchRole.value
    const data = await getUsers(params)
    users.value = data.records || data.list || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

async function handleToggleStatus(row) {
  const action = row.status === 1 ? '禁用' : '启用'
  await ElMessageBox.confirm(`确定${action}用户「${row.username}」吗？`, '确认', { type: 'warning' })
  try {
    await updateUserStatus(row.id, row.status === 1 ? 0 : 1)
    ElMessage.success(`已${action}`)
    loadUsers()
  } catch { /* handled */ }
}

const dialogVisible = ref(false)
const editingId = ref(null)
const saving = ref(false)
const formRef = ref(null)
const form = reactive({
  username: '', realName: '', password: '', role: 'STUDENT', tenantName: '测试学院'
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }, { min: 6, message: '密码至少6位', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
  tenantName: [{ required: true, message: '请输入租户名称', trigger: 'blur' }]
}

function openDialog(row) {
  editingId.value = row ? row.id : null
  if (row) {
    Object.assign(form, {
      username: row.username || '',
      realName: row.realName || '',
      password: '',
      role: row.role || 'STUDENT',
      tenantName: row.tenantName || '测试学院'
    })
  } else {
    Object.assign(form, { username: '', realName: '', password: '', role: 'STUDENT', tenantName: '测试学院' })
  }
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
  } finally {
    saving.value = false
  }
}

function roleTag(role) {
  return role === 'ADMIN' ? 'danger' : role === 'TEACHER' ? 'warning' : role === 'STUDENT' ? 'success' : 'info'
}
function roleLabel(role) {
  return { ADMIN: '管理员', TEACHER: '教师', STUDENT: '学生', HR: 'HR' }[role] || role
}

onMounted(() => loadUsers())
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-header h2 { margin: 0; }
.search-bar { display: flex; align-items: center; margin-bottom: 16px; }
</style>
