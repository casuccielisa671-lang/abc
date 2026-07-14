/**
 * 站内消息的展示元数据与跳转规则 —— 铃铛、首页格子、消息中心三处共用，避免各写一份。
 */

/** 消息类型 → { label 徽标文字, tone 色调类名 } */
const TYPE_META = {
  INTERVIEW: { label: '面试邀请', tone: 'primary' },
  OFFER:     { label: '录用通知', tone: 'success' },
  REJECT:    { label: '投递结果', tone: 'muted' },
  REPORT:    { label: '新报告', tone: 'primary' },
  RECOMMEND: { label: '职位推荐', tone: 'primary' },
  SYSTEM:    { label: '系统通知', tone: 'muted' }
}

export function messageMeta(type) {
  return TYPE_META[type] || { label: '通知', tone: 'muted' }
}

/**
 * 按 refType/refId 算出点击后要跳转的前端路由。
 * APPLICATION → 我的投递；REPORT → 我的报告；其余不跳转（返回 null）。
 * @param {string} rolePrefix 当前角色路由前缀（student/teacher/hr/admin）
 */
export function messageTarget(msg, rolePrefix) {
  if (!msg || !msg.refType) return null
  switch (msg.refType) {
    case 'APPLICATION':
      return '/student/applications'   // 面试/录用/婉拒只会发给学生
    case 'REPORT':
      return rolePrefix === 'student' ? '/student/reports' : `/${rolePrefix}/messages`
    default:
      return null
  }
}
