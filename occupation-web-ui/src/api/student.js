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
export function applyJob(jobId) {
  return request.post(`/student/job/${jobId}/apply`)
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
