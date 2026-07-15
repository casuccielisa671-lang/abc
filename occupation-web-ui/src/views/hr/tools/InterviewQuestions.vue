<template>
  <div class="interview-page">
    <div class="page-head">
      <h2 class="page-title">AI 面试问题生成</h2>
      <p class="page-desc">
        根据职位要求和候选人简历，AI 自动生成针对性面试问题，帮助面试官高效评估候选人。
      </p>
    </div>

    <el-card class="config-card">
      <div class="config-row">
        <div class="config-item">
          <label>选择职位</label>
          <el-select
            v-model="selectedJobId"
            placeholder="请选择职位"
            style="width: 100%"
            @change="handleJobChange"
          >
            <el-option
              v-for="job in jobs"
              :key="job.id"
              :label="job.title"
              :value="job.id"
            />
          </el-select>
        </div>

        <div class="config-item">
          <label>选择候选人（可选）</label>
          <el-select
            v-model="selectedApplicantId"
            placeholder="不选则生成通用问题"
            clearable
            style="width: 100%"
            :disabled="!selectedJobId"
          >
            <el-option
              v-for="applicant in jobApplicants"
              :key="applicant.userId"
              :label="applicant.realName || `候选人 ${applicant.userId}`"
              :value="applicant.userId"
            />
          </el-select>
        </div>

        <div class="config-item config-btn">
          <el-button
            type="primary"
            :loading="generating"
            :disabled="!selectedJobId"
            @click="generateQuestions"
          >
            生成面试问题
          </el-button>
        </div>
      </div>
    </el-card>

    <div v-if="result" class="questions-area">
      <el-card v-if="result.technical?.length" class="q-card">
        <template #header>
          <div class="q-header">
            <el-tag type="primary" size="large">技术能力</el-tag>
            <span class="q-count">{{ result.technical.length }} 题</span>
          </div>
        </template>
        <div class="q-list">
          <div
            v-for="(question, index) in result.technical"
            :key="`technical-${index}`"
            class="q-item"
          >
            <div class="q-num">{{ index + 1 }}</div>
            <div class="q-body">
              <p class="q-question">{{ question.question }}</p>
              <div class="q-meta">
                <span class="q-purpose">考察目的：{{ question.purpose }}</span>
              </div>
              <div class="q-answer">
                <span class="q-label">评分要点：</span>
                <p>{{ question.expectedAnswer }}</p>
              </div>
            </div>
          </div>
        </div>
      </el-card>

      <el-card v-if="result.project?.length" class="q-card">
        <template #header>
          <div class="q-header">
            <el-tag type="success" size="large">项目经验</el-tag>
            <span class="q-count">{{ result.project.length }} 题</span>
          </div>
        </template>
        <div class="q-list">
          <div
            v-for="(question, index) in result.project"
            :key="`project-${index}`"
            class="q-item"
          >
            <div class="q-num">{{ index + 1 }}</div>
            <div class="q-body">
              <p class="q-question">{{ question.question }}</p>
              <div class="q-meta">
                <span class="q-purpose">考察目的：{{ question.purpose }}</span>
              </div>
              <div class="q-answer">
                <span class="q-label">评分要点：</span>
                <p>{{ question.expectedAnswer }}</p>
              </div>
            </div>
          </div>
        </div>
      </el-card>

      <el-card v-if="result.behavioral?.length" class="q-card">
        <template #header>
          <div class="q-header">
            <el-tag type="warning" size="large">行为与软技能</el-tag>
            <span class="q-count">{{ result.behavioral.length }} 题</span>
          </div>
        </template>
        <div class="q-list">
          <div
            v-for="(question, index) in result.behavioral"
            :key="`behavioral-${index}`"
            class="q-item"
          >
            <div class="q-num">{{ index + 1 }}</div>
            <div class="q-body">
              <p class="q-question">{{ question.question }}</p>
              <div class="q-meta">
                <span class="q-purpose">考察目的：{{ question.purpose }}</span>
              </div>
              <div class="q-answer">
                <span class="q-label">评分要点：</span>
                <p>{{ question.expectedAnswer }}</p>
              </div>
            </div>
          </div>
        </div>
      </el-card>

      <div class="q-actions">
        <el-button type="primary" @click="copyAllQuestions">一键复制全部问题</el-button>
        <el-button plain @click="generateQuestions">重新生成</el-button>
      </div>
    </div>

    <el-empty
      v-else-if="!generating"
      description="选择职位后点击“生成面试问题”"
    />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { aiInterviewQuestions, getHrApplications, getHrJobs } from '@/api/student'
import { toList } from '@/utils/list'

const jobs = ref([])
const applications = ref([])
const jobApplicants = ref([])
const selectedJobId = ref(null)
const selectedApplicantId = ref(null)
const generating = ref(false)
const result = ref(null)

onMounted(loadPageData)

async function loadPageData() {
  try {
    const [jobData, applicationData] = await Promise.all([
      getHrJobs({ pageNum: 1, pageSize: 100 }),
      getHrApplications()
    ])

    jobs.value = toList(jobData)
    applications.value = toList(applicationData)
    syncApplicants()
  } catch {
    ElMessage.error('职位或候选人数据加载失败，请稍后重试')
  }
}

function handleJobChange() {
  result.value = null
  selectedApplicantId.value = null
  syncApplicants()
}

function syncApplicants() {
  if (!selectedJobId.value) {
    jobApplicants.value = []
    return
  }

  const seen = new Set()
  jobApplicants.value = applications.value
    .filter(item => item.jobId === selectedJobId.value)
    .filter(item => {
      if (seen.has(item.userId)) return false
      seen.add(item.userId)
      return true
    })
    .map(item => ({
      userId: item.userId,
      realName: item.realName,
      jobId: item.jobId
    }))
}

async function generateQuestions() {
  if (!selectedJobId.value) return

  generating.value = true
  result.value = null

  try {
    result.value = await aiInterviewQuestions(
      selectedJobId.value,
      selectedApplicantId.value || undefined
    )
  } catch {
    ElMessage.error('生成失败，请稍后重试')
  } finally {
    generating.value = false
  }
}

function copyAllQuestions() {
  if (!result.value) return

  const lines = []
  appendSection(lines, '技术能力', result.value.technical)
  appendSection(lines, '项目经验', result.value.project)
  appendSection(lines, '行为与软技能', result.value.behavioral)

  navigator.clipboard.writeText(lines.join('\n')).then(() => {
    ElMessage.success('已复制到剪贴板')
  }).catch(() => {
    ElMessage.error('复制失败，请手动复制')
  })
}

function appendSection(lines, title, items) {
  if (!items?.length) return

  lines.push(`【${title}】`)
  items.forEach((item, index) => {
    lines.push(`${index + 1}. ${item.question}`)
    lines.push(`   考察目的：${item.purpose}`)
    lines.push(`   评分要点：${item.expectedAnswer}`)
    lines.push('')
  })
}
</script>

<style scoped>
.interview-page { max-width: 900px; margin: 0 auto; padding: 16px 0 40px; }
.page-head { margin-bottom: 24px; }
.page-title { font-size: 22px; font-weight: 600; color: var(--app-ink); margin: 0 0 6px; letter-spacing: -0.3px; }
.page-desc { font-size: 14px; color: var(--app-ink-3); margin: 0; line-height: 1.6; }

.config-card { margin-bottom: 24px; }
.config-row { display: flex; align-items: flex-end; gap: 16px; flex-wrap: wrap; }
.config-item { flex: 1; min-width: 200px; }
.config-item label { display: block; font-size: 13px; font-weight: 600; color: var(--app-ink-2); margin-bottom: 6px; }
.config-btn { flex: 0 0 auto; }

.questions-area { display: flex; flex-direction: column; gap: 16px; }
.q-card { border-radius: 12px; }
.q-header { display: flex; align-items: center; gap: 10px; }
.q-count { font-size: 13px; color: var(--app-ink-3); }

.q-list { display: flex; flex-direction: column; gap: 20px; }
.q-item { display: flex; gap: 14px; }
.q-num {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: #f1f3f4;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 700;
  color: var(--app-ink-3);
  flex-shrink: 0;
  margin-top: 2px;
}
html.dark .q-num { background: #2e3035; }
.q-body { flex: 1; min-width: 0; }
.q-question { font-size: 15px; font-weight: 600; color: var(--app-ink); margin: 0 0 6px; line-height: 1.6; }
.q-meta { margin-bottom: 6px; }
.q-purpose { font-size: 13px; color: var(--app-ink-3); }
.q-answer {
  padding: 8px 12px;
  background: #fafafa;
  border-radius: 8px;
  border-left: 3px solid #e0e0e0;
}
html.dark .q-answer { background: #1e1f22; border-left-color: #3a3d42; }
.q-label { font-size: 12px; font-weight: 600; color: var(--app-ink-3); }
.q-answer p { font-size: 13px; color: var(--app-ink-2); margin: 4px 0 0; line-height: 1.6; }

.q-actions { display: flex; gap: 12px; justify-content: center; padding-top: 8px; }
</style>
