import axios from './config'

// 版本管理
export function getVersionList(params) {
  return axios({ url: '/version/list', method: 'post', data: params })
}
export function createVersion(data) {
  return axios({ url: '/version/create', method: 'post', data })
}
export function createVersionFromBuild(data) {
  return axios({ url: '/version/create-from-build', method: 'post', data })
}
export function submitVersion(data) {
  return axios({ url: '/version/submit', method: 'post', data })
}
export function returnVersion(data) {
  return axios({ url: '/version/return', method: 'post', data })
}
export function releaseVersion(data) {
  return axios({ url: '/version/release', method: 'post', data })
}
