/**
 * 学生就业状态的统一展示口径 —— 学生端/教师端/HR 端共用，保证「状态显示一致」。
 * 后端派生：EMPLOYED=已就业 / OFFERED=收到录用待接收 / SEEKING=求职中 / IDLE=待业。
 */
const META = {
  EMPLOYED: { label: '已就业', tag: 'success' },
  OFFERED:  { label: '收到录用', tag: 'warning' },
  SEEKING:  { label: '求职中', tag: '' },
  IDLE:     { label: '待业', tag: 'info' }
}

export function employmentLabel(status) {
  return (META[status] || {}).label || '—'
}

/** 返回 el-tag 的 type（'' = 默认主题色） */
export function employmentTag(status) {
  const m = META[status]
  return m ? m.tag : 'info'
}

export function isEmployed(status) {
  return status === 'EMPLOYED'
}
