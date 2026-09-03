import axios from './config'

// 版本管理
export function getVersionList(params) {
  return axios({ url: '/version/list', method: 'post', data: params })
}
export function createVersion(data) {
  return axios({ url: '/version/create', method: 'post', data })
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

// Pipeline
export function savePipelineConfig(data) {
  return axios({ url: '/pipeline/save-config', method: 'post', data })
}
export function listPipelineConfig(params) {
  return axios({ url: '/pipeline/list-config', method: 'post', data: params })
}
export function triggerPipeline(data) {
  return axios({ url: '/pipeline/trigger', method: 'post', data })
}
export function approvalPipeline(data) {
  return axios({ url: '/pipeline/approval', method: 'post', data })
}
export function listPipelineExecute(params) {
  return axios({ url: '/pipeline/list-execute', method: 'post', data: params })
}
export function deletePipelineConfig(data) {
  return axios({ url: '/pipeline/delete-config', method: 'post', data })
}
