const TECH_KEYWORDS = ['Java', 'Python', 'Vue', 'React', 'Spring', 'SQL', 'Redis', 'Docker']

export function analyzeJd(text) {
  const content = (text || '').trim()
  if (!content) return null

  const dimensions = [
    { name: '吸引力', score: scoreByKeywords(content, ['福利', '假期', '补贴', '晋升', '成长', '培训'], 50) },
    { name: '完整性', score: scoreByKeywords(content, ['职责', '负责', '要求', '学历', '经验', '薪资'], 50) },
    { name: '关键词覆盖', score: scoreByKeywords(content, TECH_KEYWORDS, 45) },
    { name: '结构清晰度', score: Math.min(100, 45 + content.split('\n').length * 5 + (/[1一][.、]/.test(content) ? 15 : 0)) },
    { name: '薪资竞争力', score: /\d+\s*[kK千]/.test(content) ? 75 : 50 }
  ]
  const score = Math.round(dimensions.reduce((sum, item) => sum + item.score, 0) / dimensions.length)
  return { score, dimensions, suggestions: buildSuggestions(content, dimensions) }
}

function scoreByKeywords(text, keywords, base) {
  const lower = text.toLowerCase()
  const matched = keywords.filter((keyword) => lower.includes(keyword.toLowerCase())).length
  return Math.min(100, base + matched * 8)
}

function buildSuggestions(text, dimensions) {
  const suggestionMap = {
    吸引力: '补充团队亮点、成长路径和福利信息，增强候选人点击意愿。',
    完整性: '将岗位职责、任职要求、学历经验、薪资范围补充完整。',
    关键词覆盖: '加入核心技术栈关键词，提升岗位检索和匹配效果。',
    结构清晰度: '用分点结构拆分职责、要求、加分项，降低阅读成本。',
    薪资竞争力: '建议明确薪资范围或福利补充，减少候选人不确定感。'
  }
  const suggestions = dimensions
    .filter((item) => item.score < 65)
    .map((item) => suggestionMap[item.name])
    .filter(Boolean)

  if (text.length < 200) {
    suggestions.push('JD 内容偏短，建议补充项目背景和业务场景。')
  }
  return suggestions.length ? suggestions : ['JD 质量较高，建议定期根据市场反馈微调关键词和福利表达。']
}
