/**
 * 展示格式化
 *
 * 薪资在库里是「元/月」的整数。各页原本各写一份 `(min/1000).toFixed(0)+'k'`，
 * 且都没处理 null —— 职位薪资面议时会渲染成 `NaNk - NaNk`。
 */

/** 单个薪资 → "8k"；空值返回 null */
function toK(value) {
  if (value == null || Number.isNaN(Number(value))) return null
  return `${Math.round(Number(value) / 1000)}k`
}

/**
 * 薪资区间 → "8k - 12k" / "8k 起" / "面议"
 */
export function salaryRange(min, max, fallback = '面议') {
  const lo = toK(min)
  const hi = toK(max)
  if (lo && hi) return `${lo} - ${hi}`
  if (lo) return `${lo} 起`
  if (hi) return `至 ${hi}`
  return fallback
}

/** 日期时间截断到分钟，表格里不需要秒 */
export function formatTime(value) {
  if (!value) return '—'
  return String(value).replace('T', ' ').slice(0, 16)
}
