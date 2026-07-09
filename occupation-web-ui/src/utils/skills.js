/**
 * 技能字段解析 —— 与后端 SkillUtils 保持同一套容错规则。
 *
 * 库里 skills 的标准格式是 JSON 数组 `["Java","MySQL"]`，
 * 但历史数据存在逗号/顿号分隔的旧格式，两种都要能解析。
 */
export function parseSkills(skills) {
  if (!skills) return []
  if (Array.isArray(skills)) return skills

  try {
    const parsed = JSON.parse(skills)
    if (Array.isArray(parsed)) {
      return parsed.map(s => String(s).trim()).filter(Boolean)
    }
  } catch {
    /* 不是 JSON，按分隔符切 */
  }
  return String(skills).split(/[,，、]/).map(s => s.trim()).filter(Boolean)
}
