import request from './request'

/** 推荐职业列表 */
export function getRecommendJobs() {
  return request.get('/map/recommendJobs')
}

/** 某职业各城市聚集度 */
export function getJobCityHeat(jobName) {
  return request.get('/map/getJobCityHeat', { params: { jobName } })
}

/** 全量城市岗位分布（岗位数 + 平均薪资 + 坐标），3D 柱状地图默认展示 */
export function getCityDistribution() {
  return request.get('/map/cityDistribution')
}

/** 管理员 — 重算职业聚集度（Spark 分析流水线） */
export function rebuildJobGather() {
  return request.post('/map/rebuildJobGather')
}
