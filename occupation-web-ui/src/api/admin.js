import request from './request'

/**
 * 管理端 API — Dashboard / 采集 / 用户管理
 */

// ========== Dashboard ==========
export function getDashboard() {
  return request.get('/analysis/dashboard')
}

// ========== 采集任务 ==========
export function getCrawlerTasks(params) {
  return request.get('/admin/crawler/task', { params })
}

export function getCrawlerTask(id) {
  return request.get(`/admin/crawler/task/${id}`)
}

export function createCrawlerTask(data) {
  return request.post('/admin/crawler/task', data)
}

export function updateCrawlerTask(id, data) {
  return request.put(`/admin/crawler/task/${id}`, data)
}

export function deleteCrawlerTask(id) {
  return request.delete(`/admin/crawler/task/${id}`)
}

export function startCrawlerTask(id) {
  return request.put(`/admin/crawler/task/${id}/start`)
}

export function stopCrawlerTask(id) {
  return request.put(`/admin/crawler/task/${id}/stop`)
}

export function mockCrawl(dataFile) {
  return request.post('/admin/crawler/task/mock', null, { params: { dataFile } })
}

// ========== 采集日志 ==========
export function getCrawlerLogs(params) {
  return request.get('/admin/crawler/log', { params })
}

// ========== 分析 ==========
export function rebuildAnalysis() {
  return request.post('/analysis/rebuild')
}

// ========== 用户管理 ==========
export function getUsers(params) {
  return request.get('/admin/users', { params })
}

export function createUser(data) {
  return request.post('/admin/users', data)
}

export function updateUser(id, data) {
  return request.put(`/admin/users/${id}`, data)
}

export function updateUserStatus(id, status) {
  return request.put(`/admin/users/${id}/status`, null, { params: { status } })
}
