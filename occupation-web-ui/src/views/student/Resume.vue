<template>
  <div class="resume-page">
    <div class="page-head with-actions">
      <div>
        <h2 class="page-title">我的简历</h2>
        <p class="page-sub">
          填写后 HR 才能在收到你的投递时看到简历。
          <span v-if="!form.exists && !loading">你还没有创建简历，先从「求职意向」写起。</span>
        </p>
      </div>
      <div class="page-actions">
        <el-button :loading="reviewing" @click="handleReview">AI 诊断简历</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存简历</el-button>
      </div>
    </div>

    <div v-loading="loading" class="resume-body">
      <!-- ============ 基本信息 ============ -->
      <el-card>
        <template #header>基本信息</template>
        <el-form :model="form" label-width="96px" style="max-width:720px">
          <el-form-item label="求职意向">
            <el-input v-model="form.jobIntention" placeholder="如：Java后端开发工程师" maxlength="100" />
          </el-form-item>
          <el-form-item label="求职手机">
            <el-input v-model="form.contactPhone" placeholder="留空则使用账号手机号" maxlength="11" />
          </el-form-item>
          <el-form-item label="求职邮箱">
            <el-input v-model="form.contactEmail" placeholder="留空则使用账号邮箱" maxlength="100" />
          </el-form-item>
          <el-form-item label="自我评价">
            <el-input
              v-model="form.selfIntro" type="textarea" :rows="4" maxlength="1000" show-word-limit
              placeholder="专业与学历、最擅长的 2-3 项技术、求职方向"
            />
            <el-button
              text type="primary" size="small" class="polish-btn"
              :loading="polishing === 'selfIntro'"
              @click="polish('自我评价', form.selfIntro, v => (form.selfIntro = v), 'selfIntro')"
            >AI 润色这段</el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <!-- ============ 教育经历 ============ -->
      <el-card class="section">
        <template #header>
          <div class="card-head">
            <span>教育经历</span>
            <el-button size="small" @click="form.educations.push({})">添加</el-button>
          </div>
        </template>
        <el-empty v-if="!form.educations.length" description="还没有教育经历" :image-size="60" />
        <div v-for="(edu, i) in form.educations" :key="i" class="entry">
          <div class="entry-head">
            <span class="entry-no">教育经历 {{ i + 1 }}</span>
            <el-button text type="danger" size="small" @click="form.educations.splice(i, 1)">删除</el-button>
          </div>
          <div class="grid-3">
            <el-input v-model="edu.school" placeholder="学校" maxlength="100" />
            <el-input v-model="edu.major" placeholder="专业" maxlength="100" />
            <el-select v-model="edu.degree" placeholder="学历">
              <el-option v-for="d in DEGREES" :key="d" :label="d" :value="d" />
            </el-select>
          </div>
          <div class="grid-3">
            <el-input v-model="edu.startDate" placeholder="入学 如 2022-09" maxlength="10" />
            <el-input v-model="edu.endDate" placeholder="毕业 如 2026-06" maxlength="10" />
            <el-input v-model="edu.gpa" placeholder="GPA 如 3.6/4.0" maxlength="20" />
          </div>
        </div>
      </el-card>

      <!-- ============ 项目经历 ============ -->
      <el-card class="section">
        <template #header>
          <div class="card-head">
            <span>项目经历<span class="hint">校招最看重这一段</span></span>
            <el-button size="small" @click="form.projects.push({ skills: [] })">添加</el-button>
          </div>
        </template>
        <el-empty v-if="!form.projects.length" description="还没有项目经历" :image-size="60" />
        <div v-for="(p, i) in form.projects" :key="i" class="entry">
          <div class="entry-head">
            <span class="entry-no">项目 {{ i + 1 }}</span>
            <el-button text type="danger" size="small" @click="form.projects.splice(i, 1)">删除</el-button>
          </div>
          <div class="grid-2">
            <el-input v-model="p.name" placeholder="项目名称" maxlength="100" />
            <el-input v-model="p.role" placeholder="担任角色 如 后端开发" maxlength="50" />
          </div>
          <div class="grid-2">
            <el-input v-model="p.startDate" placeholder="开始 如 2025-03" maxlength="10" />
            <el-input v-model="p.endDate" placeholder="结束 如 2025-07" maxlength="10" />
          </div>
          <SkillTags v-model="p.skills" placeholder="技术栈，回车添加" />
          <el-input
            v-model="p.description" type="textarea" :rows="3" maxlength="2000"
            placeholder="背景 → 你做了什么 → 用了什么技术 → 结果如何（尽量写出数据）"
          />
          <el-button
            text type="primary" size="small" class="polish-btn"
            :loading="polishing === 'project-' + i"
            @click="polish('项目经历', p.description, v => (p.description = v), 'project-' + i)"
          >AI 润色这段</el-button>
        </div>
      </el-card>

      <!-- ============ 实习经历 ============ -->
      <el-card class="section">
        <template #header>
          <div class="card-head">
            <span>实习经历</span>
            <el-button size="small" @click="form.internships.push({})">添加</el-button>
          </div>
        </template>
        <el-empty v-if="!form.internships.length" description="还没有实习经历" :image-size="60" />
        <div v-for="(it, i) in form.internships" :key="i" class="entry">
          <div class="entry-head">
            <span class="entry-no">实习 {{ i + 1 }}</span>
            <el-button text type="danger" size="small" @click="form.internships.splice(i, 1)">删除</el-button>
          </div>
          <div class="grid-2">
            <el-input v-model="it.company" placeholder="公司" maxlength="100" />
            <el-input v-model="it.position" placeholder="岗位" maxlength="100" />
          </div>
          <div class="grid-2">
            <el-input v-model="it.startDate" placeholder="开始 如 2025-07" maxlength="10" />
            <el-input v-model="it.endDate" placeholder="结束 如 2025-09" maxlength="10" />
          </div>
          <el-input
            v-model="it.description" type="textarea" :rows="3" maxlength="2000"
            placeholder="负责的模块、独立完成的需求、产出"
          />
          <el-button
            text type="primary" size="small" class="polish-btn"
            :loading="polishing === 'intern-' + i"
            @click="polish('实习经历', it.description, v => (it.description = v), 'intern-' + i)"
          >AI 润色这段</el-button>
        </div>
      </el-card>

      <!-- ============ 获奖与证书 ============ -->
      <el-card class="section">
        <template #header>获奖与证书</template>
        <SkillTags v-model="form.honors" placeholder="如 CET-6、蓝桥杯省一，回车添加" />
      </el-card>
    </div>

    <!-- ============ AI 诊断结果 ============ -->
    <el-drawer v-model="reviewVisible" title="AI 简历诊断" size="480px">
      <div v-if="review" class="review">
        <el-alert
          v-if="!review.aiGenerated" type="info" :closable="false" show-icon
          title="AI 未启用，以下为规则诊断"
          description="仅按简历完整度与技能覆盖度给出结论，不含语义分析。"
          style="margin-bottom:16px"
        />

        <div class="score-row">
          <div class="score-num">{{ review.score }}</div>
          <div>
            <div class="score-cap">综合评分</div>
            <div class="score-sum">{{ review.summary }}</div>
          </div>
        </div>

        <div v-if="review.targetJobTitle" class="target">对标岗位：{{ review.targetJobTitle }}</div>

        <template v-if="review.strengths?.length">
          <h4 class="rv-title">亮点</h4>
          <ul class="rv-list good"><li v-for="(s, i) in review.strengths" :key="i">{{ s }}</li></ul>
        </template>

        <template v-if="review.weaknesses?.length">
          <h4 class="rv-title">待改进</h4>
          <ul class="rv-list bad"><li v-for="(s, i) in review.weaknesses" :key="i">{{ s }}</li></ul>
        </template>

        <template v-if="review.suggestions?.length">
          <h4 class="rv-title">逐条改进建议</h4>
          <div v-for="(s, i) in review.suggestions" :key="i" class="sugg">
            <span class="chip">{{ s.section }}</span>
            <p class="sugg-issue">{{ s.issue }}</p>
            <p class="sugg-advice">{{ s.advice }}</p>
          </div>
        </template>

        <template v-if="review.missingSkills?.length">
          <h4 class="rv-title">建议补齐的技能</h4>
          <div class="chip-row">
            <span v-for="sk in review.missingSkills" :key="sk" class="chip learn">{{ sk }}</span>
          </div>
        </template>
      </div>
      <template #footer>
        <el-button :loading="reviewing" @click="handleReview(true)">重新诊断</el-button>
        <el-button type="primary" @click="reviewVisible = false">关闭</el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getResume, saveResume, aiReviewResume, aiPolishResume } from '@/api/student'
import SkillTags from '@/components/SkillTags.vue'
import { ElMessage } from 'element-plus'

const DEGREES = ['专科', '本科', '硕士', '博士']

const loading = ref(false)
const saving = ref(false)
const reviewing = ref(false)
const polishing = ref('')
const reviewVisible = ref(false)
const review = ref(null)

const form = reactive({
  exists: false,
  jobIntention: '',
  contactPhone: '',
  contactEmail: '',
  selfIntro: '',
  educations: [],
  projects: [],
  internships: [],
  honors: []
})

async function load() {
  loading.value = true
  try {
    const d = await getResume()
    form.exists = d.exists
    form.jobIntention = d.jobIntention || ''
    form.contactPhone = d.contactPhone || ''
    form.contactEmail = d.contactEmail || ''
    form.selfIntro = d.selfIntro || ''
    // 后端已经解析成数组了，这里不需要 JSON.parse
    form.educations = d.educations || []
    form.projects = (d.projects || []).map(p => ({ ...p, skills: p.skills || [] }))
    form.internships = d.internships || []
    form.honors = d.honors || []
    review.value = d.aiReview || null
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  saving.value = true
  try {
    await saveResume({
      jobIntention: form.jobIntention,
      contactPhone: form.contactPhone,
      contactEmail: form.contactEmail,
      selfIntro: form.selfIntro,
      educations: form.educations,
      projects: form.projects,
      internships: form.internships,
      honors: form.honors
    })
    form.exists = true
    ElMessage.success('简历已保存')
  } finally {
    saving.value = false
  }
}

/** 诊断前先落库：让大模型看到的是你刚写的内容，而不是上次保存的版本 */
async function handleReview(refresh = false) {
  reviewing.value = true
  try {
    await handleSave()
    review.value = await aiReviewResume(undefined, refresh === true)
    reviewVisible.value = true
  } catch {
    /* 拦截器已提示 */
  } finally {
    reviewing.value = false
  }
}

async function polish(section, text, apply, key) {
  if (!text || !text.trim()) {
    ElMessage.warning('请先填写内容再润色')
    return
  }
  polishing.value = key
  try {
    const { polished } = await aiPolishResume(section, text)
    apply(polished)
    ElMessage.success('已润色，请检查后再保存')
  } catch {
    /* 拦截器已提示（AI 未启用时后端返回明确原因） */
  } finally {
    polishing.value = ''
  }
}

onMounted(load)
</script>

<style scoped>
.resume-body { display: flex; flex-direction: column; gap: 16px; }
.card-head { display: flex; justify-content: space-between; align-items: center; }
.hint { color: var(--app-ink-3); font-size: 12px; font-weight: 400; margin-left: 8px; }

.entry {
  padding: 14px;
  border-radius: 10px;
  box-shadow: var(--app-hairline);
  margin-bottom: 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.entry:last-child { margin-bottom: 0; }
.entry-head { display: flex; justify-content: space-between; align-items: center; }
.entry-no { font-size: 13px; font-weight: 600; color: var(--app-ink-2); }

.grid-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
.grid-3 { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 10px; }
.polish-btn { align-self: flex-start; padding-left: 0; }

/* ---- 诊断抽屉 ---- */
.score-row { display: flex; align-items: center; gap: 16px; margin-bottom: 16px; }
.score-num {
  font-size: 40px; font-weight: 600; line-height: 1;
  color: var(--app-score); font-variant-numeric: tabular-nums;
}
.score-cap { font-size: 12px; color: var(--app-ink-3); }
.score-sum { font-size: 14px; color: var(--app-ink); margin-top: 2px; }
.target { font-size: 13px; color: var(--app-ink-3); margin-bottom: 12px; }

.rv-title { font-size: 14px; font-weight: 600; color: var(--app-ink); margin: 18px 0 8px; }
.rv-list { margin: 0; padding-left: 18px; }
.rv-list li { font-size: 13px; line-height: 1.9; }
.rv-list.good li { color: var(--app-ink-2); }
.rv-list.bad li { color: var(--app-ember); }

.sugg { padding: 10px 0; border-top: 1px solid var(--app-stone); }
.sugg:first-of-type { border-top: none; }
.sugg-issue { font-size: 13px; color: var(--app-ink); margin: 6px 0 2px; }
.sugg-advice { font-size: 13px; color: var(--app-ink-3); margin: 0; line-height: 1.7; }
.chip-row { display: flex; flex-wrap: wrap; gap: 6px; }

@media (max-width: 760px) {
  .grid-2, .grid-3 { grid-template-columns: 1fr; }
}
</style>
