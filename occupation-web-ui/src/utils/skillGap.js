import { parseSkills } from '@/utils/skills'

/**
 * 目标岗位技能对比：岗位要求技能 vs 我的技能。
 * @param {string|Array} jobSkills 岗位要求技能（JSON 数组串或数组）
 * @param {string|Array} mySkills  我的技能（画像 skills）
 * @returns {{ req:string[], have:string[], miss:string[], coverage:number }}
 */
export function skillGap(jobSkills, mySkills) {
  const req = parseSkills(jobSkills)
  const mine = parseSkills(mySkills).map(s => s.toLowerCase())
  const have = req.filter(s => mine.includes(s.toLowerCase()))
  const miss = req.filter(s => !mine.includes(s.toLowerCase()))
  const coverage = req.length ? Math.round((have.length / req.length) * 100) : 0
  return { req, have, miss, coverage }
}
