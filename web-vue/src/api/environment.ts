import axios from './config'

// 环境与部署
export function listEnvironments(params) {
  return axios({ url: '/environment/list', method: 'post', data: params })
}
export function saveEnvironment(data) {
  return axios({ url: '/environment/save', method: 'post', data })
}
export function deployVersion(data) {
  return axios({ url: '/environment/deploy', method: 'post', data })
}
export function listDeployRecords(params) {
  return axios({ url: '/environment/deploy-records', method: 'post', data: params })
}
