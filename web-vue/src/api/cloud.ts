import axios from './config'

// 云资产
export function saveCloudAccount(data) {
  return axios({ url: '/cloud/account/save', method: 'post', data })
}
export function listCloudAccounts(params) {
  return axios({ url: '/cloud/account/list', method: 'post', data: params || {} })
}
export function listCloudInstances(params) {
  return axios({ url: '/cloud/instance/list', method: 'post', data: params })
}
export function saveCloudInstance(data) {
  return axios({ url: '/cloud/instance/save', method: 'post', data })
}
export function importCloudInstance(data) {
  return axios({ url: '/cloud/instance/import-machine', method: 'post', data })
}
// 连通性校验
export function testCloudAccountConnectivity(data) {
  return axios({ url: '/cloud/account/connectivity-test', method: 'post', data })
}
// 同步云实例
export function syncCloudInstances(data) {
  return axios({ url: '/cloud/instance/sync', method: 'post', data })
}
// 实例操作（start/stop/reboot）
export function operateCloudInstance(data) {
  return axios({ url: `/cloud/instance/${data.action}`, method: 'post', data })
}
// 规格变配
export function resizeCloudInstance(data) {
  return axios({ url: '/cloud/instance/resize', method: 'post', data })
}
// 快照
export function createCloudSnapshot(data) {
  return axios({ url: '/cloud/snapshot/create', method: 'post', data })
}
export function listCloudSnapshots(params) {
  return axios({ url: '/cloud/snapshot/list', method: 'post', data: params || {} })
}
export function deleteCloudSnapshot(data) {
  return axios({ url: '/cloud/snapshot/delete', method: 'post', data })
}
// 安全组
export function listCloudSecurityGroups(params) {
  return axios({ url: '/cloud/security-group/list', method: 'post', data: params || {} })
}
// 制作镜像
export function createCloudImage(data) {
  return axios({ url: '/cloud/instance/create-image', method: 'post', data })
}
// 弹性伸缩组
export function listCloudScalingGroups(params) {
  return axios({ url: '/cloud/scaling-group/list', method: 'post', data: params || {} })
}
