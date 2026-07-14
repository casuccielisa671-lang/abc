const toNumber = (value) => Number(value) || 0

export function buildClassInsights(result) {
  const classes = result?.classes || []
  if (!classes.length) return null
  const byEmployment = [...classes].sort((a, b) => toNumber(b.employmentRate) - toNumber(a.employmentRate))
  const bySalary = [...classes].sort((a, b) => toNumber(b.avgSalary) - toNumber(a.avgSalary))
  const risk = [...classes].sort((a, b) => classScore(a) - classScore(b))[0]
  const employmentGap = toNumber(byEmployment[0]?.employmentRate) - toNumber(byEmployment.at(-1)?.employmentRate)
  const salaryGap = toNumber(bySalary[0]?.avgSalary) - toNumber(bySalary.at(-1)?.avgSalary)
  return {
    best: byEmployment[0]?.name || '-',
    salaryLeader: bySalary[0]?.name || '-',
    risk: risk?.name || '-',
    employmentGap: Math.round(employmentGap),
    salaryGap,
    actions: [
      `${risk?.name || '低分班级'}优先做一轮未就业学生访谈，拆分为技能缺口、简历缺口和求职意愿三类。`,
      `把 ${byEmployment[0]?.name || '高就业班级'} 的投递节奏、项目作品和面试复盘方法沉淀为班级模板。`,
      salaryGap > 2000 ? '薪资差距较大，建议对低薪班级补充高薪岗位方向训练和谈薪案例。' : '薪资差距可控，重点保持岗位匹配质量和投递跟进频率。'
    ]
  }
}

export function classScore(item) {
  const employment = toNumber(item.employmentRate)
  const salary = Math.min(100, toNumber(item.avgSalary) / 250)
  const destinations = item.topDestinations || []
  const concentration = destinations[0]?.ratio ? Math.max(0, 100 - toNumber(destinations[0].ratio)) : 55
  return Math.round(employment * 0.5 + salary * 0.3 + concentration * 0.2)
}

export function buildJobDecisionInsights(result) {
  const jobs = result?.jobs || []
  if (!jobs.length) return []
  return jobs.map((job) => {
    const skills = job.skills || []
    const salaryScore = Math.min(100, Math.round(midSalary(job) / 250))
    const skillGrowth = Math.min(100, 45 + skills.length * 8)
    const barrier = requirementBarrier(job)
    const total = Math.round(salaryScore * 0.35 + skillGrowth * 0.35 + (100 - barrier) * 0.3)
    return {
      id: job.id,
      title: job.title,
      total,
      salaryScore,
      skillGrowth,
      barrier,
      advice: total >= 75 ? '优先投递，可作为主攻岗位' : total >= 60 ? '值得保留，先补齐关键技能' : '谨慎选择，投入产出不够稳定'
    }
  }).sort((a, b) => b.total - a.total)
}

export function buildChecklistInsights(result) {
  if (!result) return null
  const gaps = result.skillGaps || []
  const missing = gaps.filter((gap) => !gap.possessed)
  const owned = gaps.length - missing.length
  return {
    readiness: missing.length <= 1 ? '冲刺投递' : missing.length <= 3 ? '补强后投递' : '先集中训练',
    owned,
    missing: missing.length,
    priorities: missing.slice(0, 3).map((gap, index) => ({
      skill: gap.skill,
      level: index === 0 ? '高优先级' : '中优先级',
      action: `用 1 个小项目或作品片段证明 ${gap.skill}，避免只写“了解”。`
    })),
    interview: [
      '准备 1 分钟岗位匹配自述：为什么选这个岗位、已有能力、正在补齐什么。',
      '把简历里的项目按“背景-动作-结果-指标”重写，至少准备 2 个可追问案例。',
      '投递后 48 小时内复盘 JD 关键词，更新下一版简历。'
    ]
  }
}

export function buildTalentInsights(result) {
  const talents = result?.talents || []
  if (!talents.length) return null
  const best = [...talents].sort((a, b) => toNumber(b.score) - toNumber(a.score))[0]
  const efficient = [...talents].filter(t => t.salaryMin > 0).sort((a, b) => valueIndex(b) - valueIndex(a))[0]
  return {
    best: best?.name || '-',
    efficient: efficient?.name || '暂无有效薪资',
    actions: talents.map((talent) => ({
      name: talent.name,
      level: talent.score >= 80 ? '建议优先约面' : talent.score >= 65 ? '建议补充沟通' : '暂不优先',
      note: talent.skills?.length ? `可围绕 ${talent.skills.slice(0, 3).join('、')} 追问项目深度。` : '技能信息较少，建议先补充简历或作品。'
    }))
  }
}

export function buildSalaryAdvice(result, offerSalary) {
  if (!result) return null
  const conservative = Math.round(toNumber(result.p50) * 0.95)
  const target = Math.round(toNumber(result.p75) * 1.02)
  const stretch = Math.round(toNumber(result.p90) * 0.98)
  const offer = toNumber(offerSalary)
  return {
    bands: [
      { label: '保守成交价', value: conservative },
      { label: '竞争报价', value: target },
      { label: '抢人报价', value: stretch }
    ],
    message: offer >= target ? '当前报价已经有竞争力，建议重点强化成长空间、团队资源和晋升路径。' : '当前报价吸引力不足，建议至少贴近竞争报价，或用签约奖/培养资源补足。'
  }
}

export function buildTenantInsights(tenants) {
  const list = tenants || []
  const risk = [...list].sort((a, b) => toNumber(a.health) - toNumber(b.health))[0]
  return {
    risk: risk?.name || '-',
    checklist: [
      '健康度低于 80% 的租户优先检查账号启用率和基础资料完整度。',
      'API 调用量异常升高时，建议联动访问日志排查批量导出或脚本调用。',
      '每周导出租户健康快照，方便观察持续下滑而不是只看单日异常。'
    ]
  }
}

function midSalary(job) {
  const min = toNumber(job.salaryMin)
  const max = toNumber(job.salaryMax)
  if (min && max) return (min + max) / 2
  return min || max || 0
}

function requirementBarrier(job) {
  let score = 30
  const education = job.education || ''
  const experience = job.experience || ''
  if (education.includes('硕士') || education.includes('博士')) score += 25
  if (experience.includes('3-5') || experience.includes('5-10')) score += 25
  if ((job.skills || []).length >= 6) score += 15
  return Math.min(100, score)
}

function valueIndex(talent) {
  const salary = Math.max(1, toNumber(talent.salaryMin))
  return toNumber(talent.score) / salary
}
