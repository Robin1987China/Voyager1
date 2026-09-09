import axios from './config'

// 应用（逻辑服务，跨环境）
export function listApplications(params) {
  return axios({ url: '/application/list', method: 'post', data: params })
}
export function saveApplication(data) {
  return axios({ url: '/application/save', method: 'post', data })
}
export function getApplicationDetail(params) {
  return axios({ url: '/application/detail', method: 'post', data: params })
}
export function deleteApplication(data) {
  return axios({ url: '/application/del', method: 'post', data })
}
