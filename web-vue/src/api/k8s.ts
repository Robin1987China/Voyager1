import axios from './config'

// 集群管理
export function saveK8sCluster(data) {
  return axios({ url: '/k8s/cluster/save', method: 'post', data })
}
export function listK8sClusters(params) {
  return axios({ url: '/k8s/cluster/list', method: 'post', data: params || {} })
}
export function deleteK8sCluster(data) {
  return axios({ url: '/k8s/cluster/delete', method: 'post', data })
}

// 命名空间
export function listK8sNamespaces(params) {
  return axios({ url: '/k8s/namespace/list', method: 'post', data: params })
}

// 资源
export function listK8sResources(params) {
  return axios({ url: '/k8s/resource/list', method: 'post', data: params })
}
export function getK8sResourceDetail(params) {
  return axios({ url: '/k8s/resource/detail', method: 'post', data: params })
}
export function deleteK8sResource(data) {
  return axios({ url: '/k8s/resource/delete', method: 'post', data })
}

// Deployment 操作
export function scaleK8sDeployment(data) {
  return axios({ url: '/k8s/deployment/scale', method: 'post', data })
}
export function restartK8sDeployment(data) {
  return axios({ url: '/k8s/deployment/restart', method: 'post', data })
}

// Pod 日志 / 事件
export function getK8sPodLog(params) {
  return axios({ url: '/k8s/pod/log', method: 'post', data: params })
}
export function listK8sEvents(params) {
  return axios({ url: '/k8s/event/list', method: 'post', data: params })
}

// 部署
export function applyK8sManifest(data) {
  return axios({ url: '/k8s/deploy/apply', method: 'post', data })
}
