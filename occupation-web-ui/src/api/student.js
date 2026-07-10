import request from './request'

/**
 * 学生端 API — 画像 / 推荐 / 收藏 / 投递 / 推送
 */

// ========== 画像 ==========
export function getProfile() {
  return request.get('/student/profile')
}

export function saveProfile(data) {
  return request.put('/student/profile', data)
}

export function getProfileStats() {
  return request.get('/student/profile/stats')
}

// ========== 简历 ==========
/** 未填写时后端返回 { exists: false, educations: [], ... }，不会是 null */
export function getResume() {
  return request.get('/student/resume')
}

/** 三段经历直接传数组，后端负责序列化 —— 不要在前端 JSON.stringify */
export function saveResume(data) {
  return request.put('/student/resume', data)
}

/**
 * AI 简历诊断。
 * @param {number} [targetJobId] 对标某个职位；不传则以市场热门技能为基准
 * @param {boolean} [refresh] 强制重新调用大模型（否则命中上次缓存）
 */
export function aiReviewResume(targetJobId, refresh = false) {
  return request.post('/student/resume/ai-review', null, {
    params: { targetJobId, refresh },
    timeout: 90000 // 大模型首字延迟常达数秒，默认 15s 不够
  })
}

/** AI 润色一段文字，返回 { polished } */
export function aiPolishResume(section, text) {
  return request.post('/student/resume/ai-polish', { section, text }, { timeout: 90000 })
}

// ========== AI 职业顾问 ==========
/** 服务端无状态，每次把完整对话历史传回去。role 只能是 user/assistant */
export function advisorChat(messages) {
  return request.post('/student/advisor/chat', { messages }, { timeout: 90000 })
}

/** 用自然语言解读「为什么推荐这个职位」，返回 { reply, aiGenerated } */
export function explainMatch(jobId) {
  return request.post(`/student/advisor/explain/${jobId}`, null, { timeout: 90000 })
}

// ========== 推荐 ==========
export function getRecommend(topN = 20) {
  return request.get('/student/recommend', { params: { topN } })
}

// ========== 职位 ==========
export function getJobDetail(jobId) {
  return request.get(`/student/job/${jobId}`)
}

// ========== 收藏 ==========
export function favoriteJob(jobId) {
  return request.post(`/student/job/${jobId}/favorite`)
}

export function unfavoriteJob(jobId) {
  return request.delete(`/student/job/${jobId}/favorite`)
}

export function getFavorites() {
  return request.get('/student/favorites')
}

// ========== 投递 ==========
/** 只有 HR 站内发布的职位可投（job.applicable === true），采集职位后端会拒绝 */
export function applyJob(jobId) {
  return request.post(`/student/job/${jobId}/apply`)
}

/** 我的投递 + HR 处理进度。不含 HR 的内部备注 */
export function getMyApplications() {
  return request.get('/student/applications')
}

/**
 * 自主联系：对采集来的「市场参考」职位表达求职意向。
 * 只有 job.applicable === false 的职位可以调，站内职位后端会拒绝。
 * 返回 { sourceUrl, company }，sourceUrl 为 null 表示是模拟数据、没有可跳转的真实页面。
 */
export function contactJob(jobId) {
  return request.post(`/student/job/${jobId}/contact`)
}

// ========== 推送消息 ==========
export function getPushList(params) {
  return request.get('/push/list', { params })
}

export function markRead(id) {
  return request.put(`/push/${id}/read`)
}

export function getUnreadCount() {
  return request.get('/push/unread/count')
}

// ========== 教师端 ==========
export function getTeacherStudents(params) {
  return request.get('/teacher/students', { params })
}

/** 班级概览统计（学生总数 / 已填画像 / 总浏览 / 总投递） */
export function getTeacherOverview() {
  return request.get('/teacher/overview')
}

/** 教学建议 — 技能缺口诊断（市场热度 vs 学生掌握率，全部真实数据） */
export function getTeacherSuggestions() {
  return request.get('/teacher/suggestions')
}

/** 教学建议的 AI 解读。单独一个接口：调大模型要几秒，不能拖慢表格渲染 */
export function getTeacherSuggestionsAi() {
  return request.get('/teacher/suggestions/ai', { timeout: 90000 })
}

/** 导出学生就业数据 Excel（走 axios 以携带 Token，配合 utils/download.js 保存） */
export function exportTeacherStudents() {
  return request.get('/teacher/export', { responseType: 'blob' })
}

export function getStudentStats(userId) {
  return request.get(`/teacher/students/${userId}/stats`)
}

export function getStudentBehaviors(userId, params) {
  return request.get(`/teacher/students/${userId}/behaviors`, { params })
}

// ========== HR端 ==========
export function getHrJobs(params) {
  return request.get('/hr/jobs', { params })
}

/** 收到的投递（本 HR 发布的职位上的 APPLY 行为） */
export function getHrApplications() {
  return request.get('/hr/applications')
}

/**
 * 投递人详情：姓名、联系方式、画像、简历全文。
 * 后端会校验该学生确实投递过本 HR 的职位，否则 403。
 */
export function getHrApplicant(userId) {
  return request.get(`/hr/applicants/${userId}`)
}

/**
 * 变更投递状态。status 只能是 VIEWED / INTERVIEW / OFFER / REJECTED。
 * 后端会校验归属（只能改自己职位上的投递）与流转合法性（终态不可回退）。
 */
export function changeApplicationStatus(applicationId, status, hrNote) {
  return request.put(`/hr/applications/${applicationId}/status`, { status, hrNote })
}

export function createHrJob(data) {
  return request.post('/hr/jobs', data)
}

export function updateHrJob(id, data) {
  return request.put(`/hr/jobs/${id}`, data)
}

export function deleteHrJob(id) {
  return request.delete(`/hr/jobs/${id}`)
}

export function getTalents(params) {
  return request.get('/hr/talents', { params })
}
