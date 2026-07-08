<template>
  <div class="hr-job-manage">
    <div class="page-header">
      <h2>职位管理</h2>
      <el-button type="primary" @click="openDialog()">发布新职位</el-button>
    </div>

    <el-card>
      <el-table :data="jobs" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="title" label="职位名称" min-width="160" />
        <el-table-column prop="city" label="城市" width="100" />
        <el-table-column label="薪资范围" width="150">
          <template #default="{ row }">
            {{ (row.salaryMin / 1000).toFixed(0) }}k - {{ (row.salaryMax / 1000).toFixed(0) }}k
          </template>
        </el-table-column>
        <el-table-column prop="education" label="学历要求" width="100" />
        <el-table-column prop="experience" label="经验要求" width="100" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '在招' : '已下架' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="发布时间" width="170" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openDialog(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row.id)">下架</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="page" v-model:page-size="size"
        :total="total" layout="total, prev, pager, next"
        @current-change="loadJobs" style="margin-top:16px; justify-content:flex-end"
      />
    </el-card>

    <!-- 发布/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑职位' : '发布新职位'" width="600px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="职位名称" prop="title">
          <el-input v-model="form.title" placeholder="如：高级Java开发工程师" />
        </el-form-item>
        <el-form-item label="公司名称" prop="company">
          <el-input v-model="form.company" placeholder="公司名称" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="城市" prop="city">
              <el-input v-model="form.city" placeholder="如：北京" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="行业" prop="industry">
              <el-input v-model="form.industry" placeholder="如：互联网/IT" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="最低薪资" prop="salaryMin">
              <el-input-number v-model="form.salaryMin" :min="0" :step="1000" style="width:100%" placeholder="月薪下限" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="最高薪资" prop="salaryMax">
              <el-input-number v-model="form.salaryMax" :min="0" :step="1000" style="width:100%" placeholder="月薪上限" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="学历要求" prop="education">
              <el-select v-model="form.education" style="width:100%">
                <el-option label="不限" value="不限" />
                <el-option label="大专" value="大专" />
                <el-option label="本科" value="本科" />
                <el-option label="硕士" value="硕士" />
                <el-option label="博士" value="博士" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="经验要求" prop="experience">
              <el-select v-model="form.experience" style="width:100%">
                <el-option label="不限" value="不限" />
                <el-option label="应届生" value="应届生" />
                <el-option label="1-3年" value="1-3年" />
                <el-option label="3-5年" value="3-5年" />
                <el-option label="5-10年" value="5-10年" />
                <el-option label="10年以上" value="10年以上" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="技能要求" prop="skills">
          <el-input v-model="form.skills" placeholder="多个技能用逗号分隔，如：Java,Spring Boot,MySQL" />
        </el-form-item>
        <el-form-item label="职位描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="5" placeholder="请输入职位描述" />
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
import { getHrJobs, createHrJob, updateHrJob, deleteHrJob } from '@/api/student'
import { ElMessage, ElMessageBox } from 'element-plus'

const jobs = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)

async function loadJobs() {
  loading.value = true
  try {
    const data = await getHrJobs({ page: page.value, size: size.value })
    jobs.value = data.records || data.list || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

async function handleDelete(id) {
  await ElMessageBox.confirm('确定下架该职位吗？', '确认', { type: 'warning' })
  try {
    await deleteHrJob(id)
    ElMessage.success('已下架')
    loadJobs()
  } catch { /* handled */ }
}

const dialogVisible = ref(false)
const editingId = ref(null)
const saving = ref(false)
const formRef = ref(null)
const form = reactive({
  title: '', company: '', city: '', industry: '',
  salaryMin: 10000, salaryMax: 20000,
  education: '本科', experience: '1-3年',
  skills: '', description: ''
})

const rules = {
  title: [{ required: true, message: '请输入职位名称', trigger: 'blur' }],
  company: [{ required: true, message: '请输入公司名称', trigger: 'blur' }],
  city: [{ required: true, message: '请输入城市', trigger: 'blur' }],
  industry: [{ required: true, message: '请输入行业', trigger: 'blur' }],
  salaryMin: [{ required: true, message: '请输入最低薪资', trigger: 'blur' }],
  salaryMax: [{ required: true, message: '请输入最高薪资', trigger: 'blur' }],
  education: [{ required: true, message: '请选择学历要求', trigger: 'change' }],
  experience: [{ required: true, message: '请选择经验要求', trigger: 'change' }],
  description: [{ required: true, message: '请输入职位描述', trigger: 'blur' }]
}

function openDialog(row) {
  editingId.value = row ? row.id : null
  if (row) {
    Object.assign(form, {
      title: row.title || '',
      company: row.company || '',
      city: row.city || '',
      industry: row.industry || '',
      salaryMin: row.salaryMin || 10000,
      salaryMax: row.salaryMax || 20000,
      education: row.education || '本科',
      experience: row.experience || '1-3年',
      skills: Array.isArray(row.skills) ? row.skills.join(',') : (row.skills || ''),
      description: row.description || ''
    })
  } else {
    Object.assign(form, {
      title: '', company: '', city: '', industry: '',
      salaryMin: 10000, salaryMax: 20000,
      education: '本科', experience: '1-3年',
      skills: '', description: ''
    })
  }
  dialogVisible.value = true
}

async function handleSave() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const payload = {
      ...form,
      skills: form.skills ? form.skills.split(',').map(s => s.trim()).filter(Boolean) : []
    }
    if (editingId.value) {
      await updateHrJob(editingId.value, payload)
      ElMessage.success('更新成功')
    } else {
      await createHrJob(payload)
      ElMessage.success('发布成功')
    }
    dialogVisible.value = false
    loadJobs()
  } finally {
    saving.value = false
  }
}

onMounted(() => loadJobs())
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-header h2 { margin: 0; }
</style>
