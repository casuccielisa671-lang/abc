import request from './request'

/**
 * 管理端 API — Dashboard / 采集 / 用户管理
 */

// ========== Dashboard ==========
export function getDashboard() {
  return request.get('/analysis/dashboard')
}

/**
 * 就业分析：投递漏斗 / 供需错配 / 自主求职流向。
 * 与 /dashboard 分开：看板讲市场有什么岗位，这里讲本校学生怎么样。
 * 数据同样来自 analysis_result，需先「重算分析数据」。
 */
export function getEmployment() {
  return request.get('/analysis/employment')
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

// ========== 采集日志 ==========
export function getCrawlerLogs(params) {
  return request.get('/admin/crawler/log', { params })
}

// ========== 分析 ==========
export function rebuildAnalysis() {
  return request.post('/analysis/rebuild')
}

/** 清洗 raw_job_data 存量数据并重算 Dashboard 聚合结果 */
export function runAnalysisPipeline() {
  return request.post('/analysis/pipeline')
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

/**
 * 把就业报告发送给某范围学生。
 * @param {number} id 报告 id
 * @param {{targetType:'ALL'|'MAJOR'|'GRADE'|'CLASS', targetValue?:string}} data
 * 返回本次新增下发人数（市场报告已全体可见，后端会拒绝下发）
 */
export function deliverReport(id, data) {
  return request.post(`/report/${id}/deliver`, data)
}

/** 该报告已下发的学生人数 */
export function getReportDeliveryCount(id) {
  return request.get(`/report/${id}/delivery-count`)
}

// ========== 班级管理（学院内组织结构） ==========
export function getClasses() {
  return request.get('/admin/classes')
}

export function saveClass(data) {
  return request.post('/admin/classes', data)
}

export function deleteClass(id) {
  return request.delete(`/admin/classes/${id}`)
}

/** 把一批学生分配到班级；userIds 为学生 userId 数组 */
export function assignStudentsToClass(classId, userIds) {
  return request.post(`/admin/classes/${classId}/students`, userIds)
}

/** 班级筛选项：当前租户的专业 / 入学年级 */
export function getClassFilters() {
  return request.get('/admin/classes/filters')
}

// ========== 资讯管理 ==========
export function getAdminNews(params) {
  return request.get('/admin/news', { params })
}
export function saveNews(data) {
  return request.post('/admin/news', data)
}
export function deleteNews(id) {
  return request.delete(`/admin/news/${id}`)
}
export function updateNewsStatus(id, status) {
  return request.put(`/admin/news/${id}/status`, null, { params: { status } })
}
/** 从站内分析数据重新生成"数据播报" */
export function generateDataCast() {
  return request.post('/admin/news/generate-datacast')
}
/** 从默认 RSS 源拉取外部资讯（源站暂不可访问时返回 0） */
export function pullRssNews(query, maxItems = 8) {
  return request.post('/admin/news/pull-rss', null, { params: { query, maxItems }, timeout: 20000 })
}

// ========== 教师可见范围配置 ==========
export function getTeacherScopes(teacherId) {
  return request.get('/admin/teacher-scopes', { params: { teacherId } })
}

export function saveTeacherScope(data) {
  return request.post('/admin/teacher-scopes', data)
}

export function deleteTeacherScope(id) {
  return request.delete(`/admin/teacher-scopes/${id}`)
}

