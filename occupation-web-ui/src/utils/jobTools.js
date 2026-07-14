import { salaryRange } from './format'
import { parseSkills } from './skills'

export function normalizeJob(raw) {
  const job = raw?.job ?? raw ?? {}
  const skills = parseSkills(job.skills)
  const range = job.salaryRange || salaryRange(job.salaryMin, job.salaryMax)

  return {
    ...job,
    id: job.id ?? job.jobId,
    title: job.title || job.jobTitle || '未命名岗位',
    company: job.company || '未知公司',
    city: job.city || '不限城市',
    salaryRange: range,
    industry: job.industry || '未分类',
    education: job.education || '不限',
    experience: job.experience || '不限',
    publishDate: (job.publishDate || job.createdAt || '').toString().slice(0, 10),
    skills
  }
}

export function jobOptionLabel(job) {
  return `${job.title} · ${job.company} · ${job.city} · ${job.salaryRange}`
}

export function buildCompareResult(jobIds, jobs) {
  const selected = jobIds.map(id => jobs.find(job => job.id === id)).filter(Boolean)
  const skillGroups = selected.map(job => new Set(job.skills.map(skill => skill.toLowerCase())))
  const commonSkills = selected[0]?.skills.filter(skill =>
    skillGroups.every(group => group.has(skill.toLowerCase()))
  ) ?? []

  const uniqueSkills = {}
  selected.forEach(job => {
    const otherSkills = new Set(
      selected
        .filter(item => item.id !== job.id)
        .flatMap(item => item.skills.map(skill => skill.toLowerCase()))
    )
    uniqueSkills[job.title] = job.skills.filter(skill => !otherSkills.has(skill.toLowerCase()))
  })

  const salarySummary = buildSalarySummary(selected)

  return {
    jobs: selected,
    summary: {
      highestSalary: salarySummary.highest,
      lowestSalary: salarySummary.lowest,
      commonSkills,
      uniqueSkills
    }
  }
}

export function buildChecklistResult(job, profile) {
  if (!job) {
    return {
      jobTitle: '目标岗位',
      matchScore: 0,
      skillGaps: [],
      learningPath: [],
      resumeTips: ['当前岗位数据已失效，请重新选择一个目标岗位。'],
      resources: []
    }
  }
  const requiredSkills = job.skills.length ? job.skills : ['沟通表达', '问题分析', '项目实践']
  const possessedSet = new Set(parseSkills(profile?.skills).map(skill => skill.toLowerCase()))
  const skillGaps = requiredSkills.map(skill => {
    const possessed = possessedSet.has(skill.toLowerCase())
    return {
      skill,
      possessed,
      description: possessed ? '已掌握，可在简历中突出' : '待补强，建议加入近期学习计划'
    }
  })
  const possessedCount = skillGaps.filter(item => item.possessed).length
  const matchScore = Math.max(35, Math.round((possessedCount / requiredSkills.length) * 100))
  const missing = skillGaps.filter(item => !item.possessed).slice(0, 6)

  return {
    jobTitle: `${job.title} · ${job.company}`,
    matchScore,
    skillGaps,
    learningPath: missing.map((item, index) => ({
      order: index + 1,
      title: `补强 ${item.skill}`,
      description: `围绕「${job.title}」岗位要求，完成 ${item.skill} 的基础概念、实战案例和项目复盘。`,
      estimatedTime: index < 2 ? '1-2 周' : '2-3 周'
    })),
    resumeTips: [
      `简历标题和求职意向建议直接对齐「${job.title}」。`,
      `项目经历中补充与 ${requiredSkills.slice(0, 3).join('、')} 相关的量化结果。`,
      `投递前准备一段 1 分钟自我介绍，重点说明为什么匹配 ${job.company}。`
    ],
    resources: missing.length
      ? missing.map(item => `${item.skill}：官方文档 / 入门课程 / 1 个可展示的小项目`)
      : ['继续沉淀项目成果，准备面试 STAR 案例和作品链接']
  }
}

function salaryMid(job) {
  const min = Number(job.salaryMin)
  const max = Number(job.salaryMax)
  if (!Number.isNaN(min) && !Number.isNaN(max) && min && max) return (min + max) / 2
  if (!Number.isNaN(min) && min) return min
  if (!Number.isNaN(max) && max) return max
  const nums = String(job.salaryRange || '').match(/\d+/g)?.map(Number) ?? []
  return nums.length ? nums.reduce((sum, value) => sum + value, 0) / nums.length : 0
}

function buildSalarySummary(jobs) {
  const valid = jobs
    .map(job => ({ job, salary: salaryMid(job) }))
    .filter(item => item.salary > 0)
    .sort((a, b) => b.salary - a.salary)
  if (!valid.length) return { highest: '暂无有效薪资数据', lowest: '暂无有效薪资数据' }

  const highest = valid[0]
  const lowest = valid[valid.length - 1]
  if (highest.job.id === lowest.job.id && valid.length > 1) {
    return {
      highest: `${highest.job.title}（${highest.job.salaryRange}，各岗位薪资相同或差异不足）`,
      lowest: '与最高薪资相同，建议结合技能成长和门槛判断'
    }
  }
  return {
    highest: `${highest.job.title}（${highest.job.salaryRange}）`,
    lowest: `${lowest.job.title}（${lowest.job.salaryRange}）`
  }
}
