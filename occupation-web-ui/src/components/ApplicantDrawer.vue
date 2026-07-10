<template>
  <el-drawer v-model="visible" :title="title" size="560px">
    <div v-loading="loading">
      <template v-if="detail">
        <!-- 身份与联系方式 -->
        <el-descriptions :column="2" border>
          <el-descriptions-item label="姓名">{{ detail.realName || '—' }}</el-descriptions-item>
          <el-descriptions-item label="学号">{{ detail.username || '—' }}</el-descriptions-item>
          <el-descriptions-item label="手机">{{ detail.phone || '—' }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ detail.email || '—' }}</el-descriptions-item>
        </el-descriptions>

        <!-- 投递记录：每条单独流转 —— 同一个学生可能投了你好几个职位，各自的进度不同 -->
        <h4 class="sec-title">投递了你的这些职位</h4>
        <div v-for="a in detail.appliedJobs" :key="a.applicationId" class="applied">
          <div class="applied-row">
            <span class="applied-title">{{ a.jobTitle || '职位已下架' }}</span>
            <el-tag :type="statusTag(a.status)" size="small">{{ a.statusLabel }}</el-tag>
          </div>
          <div class="applied-row">
            <span class="applied-time">投递于 {{ formatTime(a.applyTime) }}</span>
            <div v-if="!a.terminal" class="applied-actions">
              <el-button
                v-if="a.status !== 'INTERVIEW'" text size="small" type="primary"
                :loading="acting === a.applicationId"
                @click="act(a, 'INTERVIEW')"
              >邀请面试</el-button>
              <el-button
                text size="small" type="success"
                :loading="acting === a.applicationId"
                @click="act(a, 'OFFER')"
              >录用</el-button>
              <el-button
                text size="small" type="danger"
                :loading="acting === a.applicationId"
                @click="act(a, 'REJECTED')"
              >不合适</el-button>
            </div>
            <span v-else class="terminal-hint">已是终态，不可再变更</span>
          </div>
          <p v-if="a.hrNote" class="applied-note">备注：{{ a.hrNote }}</p>
        </div>

        <!-- 画像 -->
        <h4 class="sec-title">求职画像</h4>
        <el-empty v-if="!detail.profileCompleted" description="该学生尚未完善画像" :image-size="60" />
        <el-descriptions v-else :column="2" border>
          <el-descriptions-item label="专业">{{ detail.major || '—' }}</el-descriptions-item>
          <el-descriptions-item label="学历">{{ detail.educationLevel || '—' }}</el-descriptions-item>
          <el-descriptions-item label="期望城市">{{ detail.expectedCity || '—' }}</el-descriptions-item>
          <el-descriptions-item label="期望行业">{{ detail.expectedIndustry || '—' }}</el-descriptions-item>
          <el-descriptions-item label="期望薪资" :span="2">
            <span class="salary-text">
              {{ salaryRange(detail.expectedSalaryMin, detail.expectedSalaryMax, '未填写') }}
            </span>
          </el-descriptions-item>
          <el-descriptions-item label="技能" :span="2">
            <div class="chip-row">
              <span v-for="sk in parseSkills(detail.skills)" :key="sk" class="chip">{{ sk }}</span>
              <span v-if="!parseSkills(detail.skills).length">—</span>
            </div>
          </el-descriptions-item>
        </el-descriptions>

        <div class="stat-grid compact">
          <div class="stat-card">
            <div class="stat-label">浏览职位</div>
            <div class="stat-value">{{ detail.viewCount }}</div>
          </div>
          <div class="stat-card">
            <div class="stat-label">收藏职位</div>
            <div class="stat-value">{{ detail.favoriteCount }}</div>
          </div>
          <div class="stat-card">
            <div class="stat-label">总投递数</div>
            <div class="stat-value">{{ detail.applyCount }}</div>
          </div>
        </div>

        <!-- 简历 -->
        <h4 class="sec-title">简历</h4>
        <el-empty v-if="!detail.resume?.exists" description="该学生尚未填写简历" :image-size="60" />
        <div v-else class="resume">
          <p v-if="detail.resume.jobIntention" class="intention">
            求职意向：<strong>{{ detail.resume.jobIntention }}</strong>
          </p>

          <template v-if="detail.resume.selfIntro">
            <h5 class="rs-title">自我评价</h5>
            <p class="rs-text">{{ detail.resume.selfIntro }}</p>
          </template>

          <template v-if="detail.resume.educations?.length">
            <h5 class="rs-title">教育经历</h5>
            <div v-for="(e, i) in detail.resume.educations" :key="i" class="rs-item">
              <div class="rs-line">
                <span class="rs-main">{{ e.school }} · {{ e.major }}</span>
                <span class="rs-date">{{ e.startDate }} ~ {{ e.endDate }}</span>
              </div>
              <div class="rs-sub">
                <span class="chip">{{ e.degree }}</span>
                <span v-if="e.gpa" class="rs-gpa">GPA {{ e.gpa }}</span>
              </div>
            </div>
          </template>

          <template v-if="detail.resume.projects?.length">
            <h5 class="rs-title">项目经历</h5>
            <div v-for="(p, i) in detail.resume.projects" :key="i" class="rs-item">
              <div class="rs-line">
                <span class="rs-main">{{ p.name }}</span>
                <span class="rs-date">{{ p.startDate }} ~ {{ p.endDate }}</span>
              </div>
              <div class="rs-sub">
                <span v-if="p.role" class="rs-role">{{ p.role }}</span>
                <span v-for="sk in p.skills || []" :key="sk" class="chip">{{ sk }}</span>
              </div>
              <p class="rs-text">{{ p.description }}</p>
            </div>
          </template>

          <template v-if="detail.resume.internships?.length">
            <h5 class="rs-title">实习经历</h5>
            <div v-for="(it, i) in detail.resume.internships" :key="i" class="rs-item">
              <div class="rs-line">
                <span class="rs-main">{{ it.company }} · {{ it.position }}</span>
                <span class="rs-date">{{ it.startDate }} ~ {{ it.endDate }}</span>
              </div>
              <p class="rs-text">{{ it.description }}</p>
            </div>
          </template>

          <template v-if="detail.resume.honors?.length">
            <h5 class="rs-title">获奖与证书</h5>
            <div class="chip-row">
              <span v-for="h in detail.resume.honors" :key="h" class="chip">{{ h }}</span>
            </div>
          </template>
        </div>
      </template>
    </div>
  </el-drawer>
</template>

<script setup>
/**
 * 投递人详情抽屉（HR 端）
 *
 * 后端 GET /api/hr/applicants/{userId} 会校验该学生确实投递过当前 HR 的职位，
 * 否则返回 403 —— 前端不做任何权限判断，也判断不了。
 */
import { ref, computed, watch } from 'vue'
import { getHrApplicant, changeApplicationStatus } from '@/api/student'
import { parseSkills } from '@/utils/skills'
import { salaryRange, formatTime } from '@/utils/format'
import { ElMessage, ElMessageBox } from 'element-plus'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  userId: { type: Number, default: null }
})
const emit = defineEmits(['update:modelValue', 'changed'])

const detail = ref(null)
const loading = ref(false)
const acting = ref(null)

const STATUS_LABEL = {
  INTERVIEW: '邀请面试',
  OFFER: '录用',
  REJECTED: '标记为不合适'
}

function statusTag(status) {
  return {
    SUBMITTED: 'info',
    VIEWED: 'warning',
    INTERVIEW: 'primary',
    OFFER: 'success',
    REJECTED: 'danger'
  }[status] || 'info'
}

/**
 * 变更一条投递的状态。
 *
 * OFFER / REJECTED 是终态，学生立刻能看到且不可撤销，所以先让 HR 确认一次。
 * 备注是可选的，留空就不覆盖已有备注（后端只在传了非 null 时才写）。
 */
async function act(application, status) {
  let hrNote
  try {
    const { value } = await ElMessageBox.prompt(
      status === 'REJECTED' || status === 'OFFER'
        ? `确定要「${STATUS_LABEL[status]}」吗？此操作不可撤销，学生会看到这个结果。`
        : `确定要「${STATUS_LABEL[status]}」吗？`,
      application.jobTitle || '处理投递',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputPlaceholder: '备注（选填，仅你自己可见）',
        inputValue: application.hrNote || '',
        type: status === 'REJECTED' ? 'warning' : 'info'
      }
    )
    hrNote = value
  } catch {
    return // 用户取消
  }

  acting.value = application.applicationId
  try {
    await changeApplicationStatus(application.applicationId, status, hrNote)
    ElMessage.success('已更新')
    await load(props.userId)
    emit('changed')
  } catch {
    /* 拦截器已提示（越权 403 / 非法流转都会给出原因） */
  } finally {
    acting.value = null
  }
}

const visible = computed({
  get: () => props.modelValue,
  set: v => emit('update:modelValue', v)
})

const title = computed(() =>
  detail.value ? `投递人 · ${detail.value.realName || detail.value.username}` : '投递人详情'
)

async function load(userId) {
  loading.value = true
  detail.value = null
  try {
    detail.value = await getHrApplicant(userId)
  } catch {
    // 403 等错误已由拦截器提示，关闭抽屉避免留一个空壳
    visible.value = false
  } finally {
    loading.value = false
  }
}

watch(
  () => [props.modelValue, props.userId],
  ([open, uid]) => {
    if (open && uid != null) load(uid)
  },
  { immediate: true }
)
</script>

<style scoped>
.sec-title { font-size: 14px; font-weight: 600; color: var(--app-ink); margin: 22px 0 10px; }

.applied {
  padding: 10px 12px; border-radius: 8px; box-shadow: var(--app-hairline); margin-bottom: 8px;
}
.applied-row { display: flex; justify-content: space-between; align-items: center; gap: 10px; }
.applied-row + .applied-row { margin-top: 6px; }
.applied-title { font-size: 13px; font-weight: 600; color: var(--app-ink); }
.applied-time { font-size: 12px; color: var(--app-ink-3); }
.applied-actions { display: flex; gap: 2px; }
.terminal-hint { font-size: 11px; color: var(--app-ink-3); }
.applied-note {
  margin: 8px 0 0; font-size: 12px; color: var(--app-ink-3);
  padding-top: 8px; border-top: 1px solid var(--app-stone);
}

.stat-grid.compact { margin-top: 16px; grid-template-columns: repeat(3, 1fr); }
.chip-row { display: flex; flex-wrap: wrap; gap: 4px; }

.intention { font-size: 13px; color: var(--app-ink-2); margin: 0 0 4px; }
.rs-title { font-size: 13px; font-weight: 600; color: var(--app-ink-2); margin: 16px 0 6px; }
.rs-item { padding: 10px 0; border-top: 1px solid var(--app-stone); }
.rs-item:first-of-type { border-top: none; }
.rs-line { display: flex; justify-content: space-between; align-items: baseline; gap: 12px; }
.rs-main { font-size: 13px; font-weight: 600; color: var(--app-ink); }
.rs-date { font-size: 12px; color: var(--app-ink-3); white-space: nowrap; }
.rs-sub { display: flex; flex-wrap: wrap; align-items: center; gap: 6px; margin-top: 6px; }
.rs-role, .rs-gpa { font-size: 12px; color: var(--app-ink-3); }
.rs-text { font-size: 13px; line-height: 1.75; color: var(--app-ink-2); margin: 6px 0 0; white-space: pre-line; }
</style>
