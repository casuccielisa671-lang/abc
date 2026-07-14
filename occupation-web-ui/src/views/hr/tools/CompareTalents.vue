<template>
  <div class="tool-page">
    <div class="tool-page-header">
      <el-button text @click="$router.push('/hr/tools')">
        <el-icon><ArrowLeft /></el-icon> 返回工具箱
      </el-button>
      <h2>人才对比</h2>
      <p class="tool-desc">选择 2~4 个候选人，从技能、学历、经验、薪资期望等维度并排对比</p>
    </div>

    <div class="select-bar">
      <el-select v-model="selected" multiple placeholder="选择候选人（2~4 人）" :multiple-limit="4" style="width: 100%">
        <el-option v-for="t in talentList" :key="t.id" :label="t.name" :value="t.id" />
      </el-select>
      <el-button type="primary" :disabled="selected.length < 2" @click="compare" :loading="loading">开始对比</el-button>
    </div>

    <template v-if="result">
      <div v-if="talentInsights" class="section">
        <h3 class="section-title">录用决策建议</h3>
        <div class="insight-grid">
          <div class="insight-card"><span>综合优先</span><b>{{ talentInsights.best }}</b></div>
          <div class="insight-card"><span>薪资效率</span><b>{{ talentInsights.efficient }}</b></div>
        </div>
      </div>

      <div class="compare-table">
        <div class="ct-header">
          <div class="ct-dim"></div>
          <div v-for="t in result.talents" :key="t.id" class="ct-col">{{ t.name }}</div>
        </div>
        <div v-for="row in rows" :key="row.label" class="ct-row">
          <div class="ct-dim">{{ row.label }}</div>
          <div v-for="t in result.talents" :key="t.id" class="ct-col">
            <span v-if="row.key === 'score'" class="ct-score" :style="{ color: scoreColor(t[row.key]) }">{{ t[row.key] }}</span>
            <span v-else-if="row.key === 'skills'" class="ct-skills">
              <span v-for="s in t[row.key]" :key="s" class="ct-skill-tag">{{ s }}</span>
            </span>
            <span v-else>{{ t[row.key] }}</span>
          </div>
        </div>
      </div>

      <div class="section">
        <h3 class="section-title">对比总结</h3>
        <div class="summary">
          <div class="sum-item"><el-icon><Top /></el-icon>综合评分最高：<b>{{ result.summary.best }}</b></div>
          <div class="sum-item"><el-icon><Coin /></el-icon>薪资期望最低：<b>{{ result.summary.lowestSalary }}</b></div>
          <div v-if="result.summary.salaryNote" class="sum-note">{{ result.summary.salaryNote }}</div>
          <div class="sum-item"><el-icon><Link /></el-icon>共同技能：<b>{{ result.summary.commonSkills.length ? result.summary.commonSkills.join('、') : '暂无共同技能' }}</b></div>
        </div>
      </div>

      <div v-if="talentInsights" class="section">
        <h3 class="section-title">面试追问方向</h3>
        <div class="action-list">
          <div v-for="item in talentInsights.actions" :key="item.name" class="action-item">
            <b>{{ item.name }}：{{ item.level }}</b>
            <span>{{ item.note }}</span>
          </div>
        </div>
      </div>
    </template>

    <div v-else class="empty">
      <el-icon :size="48"><Switch /></el-icon>
      <p>选择 2 个以上候选人，点击"开始对比"查看结果</p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ArrowLeft, Switch, Top, Coin, Link } from '@element-plus/icons-vue'
import { getHrApplications } from '@/api/student'
import { toList } from '@/utils/list'
import { parseSkills } from '@/utils/skills'
import { salaryRange } from '@/utils/format'
import { buildTalentInsights } from '@/utils/toolInsights'

const talentList = ref([])

const rows = [
  { label: '综合评分', key: 'score' },
  { label: '学历', key: 'education' },
  { label: '工作经验', key: 'experience' },
  { label: '期望薪资', key: 'salary' },
  { label: '技能标签', key: 'skills' }
]

const selected = ref([])
const loading = ref(false)
const result = ref(null)
const talentInsights = computed(() => buildTalentInsights(result.value))
const scoreColor = s => s >= 85 ? '#27ae60' : s >= 70 ? '#f39c12' : '#e74c3c'

onMounted(async () => {
  try {
    const data = await getHrApplications()
    talentList.value = uniqueApplicants(toList(data))
  } catch { talentList.value = [] }
})

const compare = async () => {
  if (selected.value.length < 2) return
  loading.value = true
  result.value = buildCompareResult()
  loading.value = false
}

function buildCompareResult() {
  const talents = selected.value
    .map(id => talentList.value.find(t => t.id === id))
    .filter(Boolean)
    .map(toCompareItem)
  const best = [...talents].sort((a, b) => b.score - a.score)[0]
  const salaryItems = [...talents].filter(t => t.salaryMin > 0).sort((a, b) => a.salaryMin - b.salaryMin)
  const lowestSalary = salaryItems[0]
  const commonSkills = commonSkillList(talents)
  return {
    talents,
    summary: {
      best: best?.name || '暂无',
      lowestSalary: lowestSalary?.name || '暂无有效薪资',
      salaryNote: buildSalaryNote(best, lowestSalary, salaryItems.length),
      commonSkills
    }
  }
}

function buildSalaryNote(best, lowestSalary, validSalaryCount) {
  if (!validSalaryCount) return '候选人未填写期望薪资，当前无法判断薪资最低者。'
  if (best?.id === lowestSalary?.id) return '该候选人同时满足“综合评分最高”和“薪资期望最低”，不是计算重复。'
  return ''
}

function toCompareItem(talent) {
  const skills = parseSkills(talent.skills)
  return {
    id: talent.id,
    name: talent.name,
    score: scoreTalent(talent, skills),
    education: talent.educationLevel || '未知',
    experience: talent.applyCount > 0 ? `已投递 ${talent.applyCount} 次` : '暂无投递记录',
    salary: salaryRange(talent.expectedSalaryMin, talent.expectedSalaryMax, '未填写'),
    salaryMin: talent.expectedSalaryMin || 0,
    skills
  }
}

function uniqueApplicants(applications) {
  const seen = new Map()
  for (const app of applications) {
    if (app.userId == null || seen.has(app.userId)) continue
    seen.set(app.userId, {
      ...app,
      id: app.userId,
      name: app.realName || app.username || `学生 ${app.userId}`,
      applyCount: applications.filter(item => item.userId === app.userId).length
    })
  }
  return [...seen.values()]
}

function scoreTalent(talent, skills) {
  let score = 50
  score += Math.min(25, skills.length * 5)
  if ((talent.educationLevel || '').includes('硕士')) score += 10
  else if ((talent.educationLevel || '').includes('本科')) score += 6
  score += Math.min(15, (talent.applyCount || 0) * 3 + (talent.contactCount || 0) * 2)
  return Math.min(100, score)
}

function commonSkillList(talents) {
  if (!talents.length) return []
  const sets = talents.map(t => new Set(t.skills.map(s => s.toLowerCase())))
  return talents[0].skills.filter(skill => sets.every(set => set.has(skill.toLowerCase())))
}
</script>

<style scoped>
.tool-page { max-width: 1000px; margin: 0 auto; padding: 16px 0 40px; }
.tool-page-header { margin-bottom: 24px; }
.tool-page-header h2 { font-size: 22px; font-weight: 600; color: var(--app-ink); margin: 12px 0 6px; }
.tool-desc { font-size: 14px; color: var(--app-ink-3); margin: 0; }

.select-bar { display: flex; gap: 12px; margin-bottom: 32px; }
.select-bar .el-button { flex-shrink: 0; }

.compare-table { background: #fff; border: 1px solid #e8eaed; border-radius: 10px; overflow: hidden; margin-bottom: 32px; }
html.dark .compare-table { background: #1e1f22; border-color: #2e3035; }

.ct-header, .ct-row { display: grid; grid-template-columns: 120px repeat(auto-fit, minmax(0, 1fr)); align-items: center; }
.ct-header { background: #f8f9fa; font-size: 13px; font-weight: 600; }
html.dark .ct-header { background: #252629; }
.ct-row { border-top: 1px solid #f1f3f4; }
html.dark .ct-row { border-color: #2e3035; }

.ct-dim { padding: 12px 16px; font-size: 13px; color: var(--app-ink-2); font-weight: 500; }
.ct-col { padding: 12px 16px; font-size: 13px; }
.ct-score { font-size: 18px; font-weight: 700; }
.ct-skills { display: flex; flex-wrap: wrap; gap: 4px; }
.ct-skill-tag { font-size: 11px; background: #f1f3f4; padding: 2px 8px; border-radius: 4px; }
html.dark .ct-skill-tag { background: #2e3035; }

.section { margin-bottom: 32px; }
.section-title { font-size: 16px; font-weight: 600; color: var(--app-ink); margin: 0 0 16px; }
.insight-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; }
.insight-card { background: #fff; border: 1px solid #e8eaed; border-radius: 10px; padding: 16px; display: flex; flex-direction: column; gap: 8px; }
html.dark .insight-card { background: #1e1f22; border-color: #2e3035; }
.insight-card span { font-size: 12px; color: var(--app-ink-3); }
.insight-card b { font-size: 18px; color: var(--app-ink); }

.summary { background: #fff; border: 1px solid #e8eaed; border-radius: 10px; padding: 20px; display: flex; flex-direction: column; gap: 12px; }
html.dark .summary { background: #1e1f22; border-color: #2e3035; }
.sum-item { display: flex; align-items: center; gap: 8px; font-size: 14px; color: var(--app-ink-2); }
.sum-item b { color: var(--app-ink); }
.sum-note { margin-left: 24px; font-size: 12px; color: #e67e22; line-height: 1.5; }
.action-list { display: grid; gap: 10px; }
.action-item { background: #fff; border: 1px solid #e8eaed; border-radius: 10px; padding: 14px 16px; display: flex; flex-direction: column; gap: 6px; font-size: 13px; color: var(--app-ink-2); line-height: 1.6; }
html.dark .action-item { background: #1e1f22; border-color: #2e3035; }
.action-item b { color: var(--app-ink); }

.empty { text-align: center; padding: 80px 0; color: var(--app-ink-3); }
.empty p { margin-top: 16px; }
@media (max-width: 768px) { .insight-grid { grid-template-columns: 1fr; } }
</style>
