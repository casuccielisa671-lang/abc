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

/** Excel 批量导入用户（全量校验通过才写库，返回逐行错误报告） */
export function batchImportUsers(file) {
  const form = new FormData()
  form.append('file', file)
  return request.post('/admin/users/batch-import', form, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/** 下载导入模板 */
export function downloadImportTemplate() {
  return request.get('/admin/users/import-template', { responseType: 'blob' })
}

// ========== 报告模板 ==========
export function getReportTemplates(params) {
  return request.get('/admin/report/template', { params })
}

export function getReportTemplate(id) {
  return request.get(`/admin/report/template/${id}`)
}

export function createReportTemplate(data) {
  return request.post('/admin/report/template', data)
}

export function updateReportTemplate(id, data) {
  return request.put(`/admin/report/template/${id}`, data)
}

export function deleteReportTemplate(id) {
  return request.delete(`/admin/report/template/${id}`)
}

// ========== 报告记录 ==========
export function getReportRecords(params) {
  return request.get('/report/records', { params })
}

export function deleteReportRecord(id) {
  return request.delete(`/report/records/${id}`)
}

/**
 * 下载报告文件。
 * 过去返回一个 URL 交给 window.open —— 那样不带 Authorization 头，后端 401，
 * 表现为「点了没反应」。改为走 axios 取 Blob。
 */
export function downloadReport(id) {
  return request.get(`/report/download/${id}`, { responseType: 'blob' })
}

export function generateReport(data) {
  return request.post('/report/generate', data)
}
