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
 * 点击消息后要跳转的前端路由：先看具体关联对象(refType)，没有再按消息类型(type)做栏目级跳转。
 *   APPLICATION → 我的投递(按角色)；REPORT → 我的报告；RECOMMEND(职位推荐) → 职位信息；
 *   SYSTEM 等纯通知 → 不跳(返回 null)。
 * @param {string} rolePrefix 当前角色路由前缀（student/teacher/hr/admin）
 */
export function messageTarget(msg, rolePrefix) {
  if (!msg) return null
  // 1) 有具体关联对象：优先按 refType 跳对应列表
  if (msg.refType === 'APPLICATION') {
    return `/${rolePrefix}/applications`   // 学生=我的投递、HR=收到的投递
  }
  if (msg.refType === 'REPORT') {
    return rolePrefix === 'student' ? '/student/reports' : `/${rolePrefix}/messages`
  }
  // 2) 无关联对象：按类型做栏目级跳转
  switch (msg.type) {
    case 'RECOMMEND':
      return rolePrefix === 'student' ? '/student/jobs' : null   // 职位推荐 → 职位信息
    case 'INTERVIEW':
    case 'OFFER':
    case 'REJECT':
      return rolePrefix === 'student' ? '/student/applications' : null   // 兜底(通常已带 APPLICATION ref)
    default:
      return null
  }
}

/** 点击消息的行动提示文字（与 messageTarget 覆盖一致；null=不可跳、不显示提示） */
export function messageActionLabel(msg) {
  if (!msg) return null
  if (msg.refType === 'APPLICATION') return '查看投递详情'
  if (msg.refType === 'REPORT') return '查看报告'
  if (msg.type === 'RECOMMEND') return '查看推荐职位'
  if (['INTERVIEW', 'OFFER', 'REJECT'].includes(msg.type)) return '查看投递详情'
  return null
}
