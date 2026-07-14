<template>
  <div class="hr-job-manage">
    <div class="page-head with-actions">
      <div>
        <h2 class="page-title">职位管理</h2>
        <p class="page-sub">只显示由你发布的职位；采集来的职位不可编辑</p>
      </div>
      <div class="page-actions">
        <el-button type="primary" @click="openDialog()">发布新职位</el-button>
      </div>
    </div>

    <el-card>
      <el-table :data="jobs" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="title" label="职位名称" min-width="180" />
        <el-table-column prop="company" label="公司" min-width="160" />
        <el-table-column prop="city" label="城市" width="90" />
        <el-table-column label="薪资范围" width="150">
          <template #default="{ row }">
            <span class="salary-text">{{ salaryRange(row.salaryMin, row.salaryMax) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="学历要求" width="100">
          <template #default="{ row }"><span class="chip">{{ row.education || '不限' }}</span></template>
        </el-table-column>
        <el-table-column label="经验要求" width="100">
          <template #default="{ row }"><span class="chip">{{ row.experience || '不限' }}</span></template>
        </el-table-column>
        <el-table-column prop="publishDate" label="发布日期" width="120" />
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="openDialog(row)">编辑</el-button>
            <el-button text type="danger" size="small" @click="handleDelete(row)">下架</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && !jobs.length" description="你还没有发布过职位" />

      <el-pagination
        v-if="total > size"
        v-model:current-page="page" :page-size="size"
        :total="total" layout="total, prev, pager, next"
        @current-change="loadJobs"
      />
    </el-card>

    <!-- 发布/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑职位' : '发布新职位'" width="600px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="职位名称" prop="title">
          <el-input v-model="form.title" placeholder="如：高级 Java 开发工程师" />
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
              <el-input-number v-model="form.salaryMin" :min="0" :step="1000" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="最高薪资" prop="salaryMax">
              <el-input-number v-model="form.salaryMax" :min="0" :step="1000" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="学历要求" prop="education">
              <!-- 与后端 normalizeEducation 的五档保持一致（是「专科」不是「大专」） -->
              <el-select v-model="form.education" style="width:100%">
                <el-option label="不限" value="不限" />
                <el-option label="专科" value="专科" />
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
        <el-form-item label="JD 优化">
          <JdOptimizeAssistant
            v-model="form.description"
            :title="form.title"
            :skills="form.skills"
          />
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
import { toList, toTotal } from '@/utils/list'
import { parseSkills } from '@/utils/skills'
import { salaryRange } from '@/utils/format'
import JdOptimizeAssistant from '@/components/hr/JdOptimizeAssistant.vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const jobs = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)

async function loadJobs() {
  loading.value = true
  try {
    // 后端 JobQueryDTO 读的是 pageNum/pageSize；publisherId 由服务端强制注入
    const data = await getHrJobs({ pageNum: page.value, pageSize: size.value })
    jobs.value = toList(data)
    total.value = toTotal(data, jobs.value)
  } finally {
    loading.value = false
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定下架职位「${row.title}」吗？该操作不可恢复。`, '确认', { type: 'warning' })
  } catch {
    return
  }
  try {
    await deleteHrJob(row.id)
    ElMessage.success('已下架')
    loadJobs()
  } catch { /* 拦截器已提示 */ }
}

const dialogVisible = ref(false)
const editingId = ref(null)
const saving = ref(false)
const formRef = ref(null)
const EMPTY = {
  title: '', company: '', city: '', industry: '',
  salaryMin: 10000, salaryMax: 20000,
  education: '本科', experience: '1-3年',
  skills: '', description: ''
}
const form = reactive({ ...EMPTY })

const rules = {
  title: [{ required: true, message: '请输入职位名称', trigger: 'blur' }],
  company: [{ required: true, message: '请输入公司名称', trigger: 'blur' }],
  city: [{ required: true, message: '请输入城市', trigger: 'blur' }],
  salaryMin: [{ required: true, message: '请输入最低薪资', trigger: 'blur' }],
  salaryMax: [
    { required: true, message: '请输入最高薪资', trigger: 'blur' },
    {
      validator: (_rule, value, callback) =>
        value < form.salaryMin ? callback(new Error('最高薪资不能低于最低薪资')) : callback(),
      trigger: 'blur'
    }
  ],
  description: [{ required: true, message: '请输入职位描述', trigger: 'blur' }]
}

function openDialog(row) {
  editingId.value = row ? row.id : null
  Object.assign(form, row
    ? {
        title: row.title || '',
        company: row.company || '',
        city: row.city || '',
        industry: row.industry || '',
        salaryMin: row.salaryMin ?? 10000,
        salaryMax: row.salaryMax ?? 20000,
        education: row.education || '本科',
        experience: row.experience || '1-3年',
        skills: parseSkills(row.skills).join(','),
        description: row.description || ''
      }
    : { ...EMPTY })
  dialogVisible.value = true
}

async function handleSave() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    // JobSaveDTO.skills 是 String（JSON 数组字符串），直接传数组会绑定失败
    const skills = form.skills.split(',').map(s => s.trim()).filter(Boolean)
    const payload = { ...form, skills: JSON.stringify(skills) }

    if (editingId.value) {
      await updateHrJob(editingId.value, payload)
      ElMessage.success('更新成功')
    } else {
      await createHrJob(payload)
      ElMessage.success('发布成功')
    }
    dialogVisible.value = false
    loadJobs()
  } catch {
    // 拦截器已提示（例如越权编辑他人职位会返回 403）
  } finally {
    saving.value = false
  }
}

onMounted(loadJobs)
</script>
