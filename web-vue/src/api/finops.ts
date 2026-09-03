import axios from './config'

// 成本明细
export function importCostBill(data) {
  return axios({ url: '/finops/bill/import', method: 'post', data })
}
export function syncCostBill(data) {
  return axios({ url: '/finops/bill/sync', method: 'post', data })
}
export function analyzeCostBill(params) {
  return axios({ url: '/finops/bill/analyze', method: 'post', data: params || {} })
}
export function totalCostBill(params) {
  return axios({ url: '/finops/bill/total', method: 'post', data: params || {} })
}
// 标签分摊规则
export function saveCostTagRule(data) {
  return axios({ url: '/finops/tag-rule/save', method: 'post', data })
}
export function listCostTagRules(params) {
  return axios({ url: '/finops/tag-rule/list', method: 'post', data: params || {} })
}
export function deleteCostTagRule(data) {
  return axios({ url: '/finops/tag-rule/delete', method: 'post', data })
}
// 预算
export function saveCostBudget(data) {
  return axios({ url: '/finops/budget/save', method: 'post', data })
}
export function listCostBudgets(params) {
  return axios({ url: '/finops/budget/list', method: 'post', data: params || {} })
}
export function deleteCostBudget(data) {
  return axios({ url: '/finops/budget/delete', method: 'post', data })
}
export function checkCostBudget(params) {
  return axios({ url: '/finops/budget/check', method: 'post', data: params || {} })
}
// 优化建议（闲置资源）
export function listIdleResources(params) {
  return axios({ url: '/finops/optimize/idle', method: 'post', data: params || {} })
}
