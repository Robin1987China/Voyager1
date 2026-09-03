import { t } from '@/i18n'
import axios from './config'
import { loadRouterBase } from './config'
/**
 * 容器列表
 * @param {JSON} params
 */
export function dockerSwarmList(params) {
  return axios({
    url: '/docker/swarm/list',
    method: 'post',
    data: params
  })
}

export function dockerSwarmListAll(params) {
  return axios({
    url: '/docker/swarm/list-all',
    method: 'get',
    params: params
  })
}

export function editDockerSwarm(data) {
  return axios({
    url: '/docker/swarm/edit',
    method: 'post',
    data: data
  })
}

/**
 * 删除 集群
 * @param {
 *  id: docker ID
 * } params
 */
export function delSwarm(params) {
  return axios({
    url: '/docker/swarm/del',
    method: 'get',
    params
  })
}

/**
 * 容器集群节点列表
 * @param {JSON} params
 */
export function dockerSwarmNodeList(urlPrefix, params) {
  return axios({
    url: urlPrefix + '/docker/swarm/node-list',
    method: 'post',
    data: params,
    headers: {
      loading: 'no'
    }
  })
}

/**
 * 容器集群节点修改
 * @param {JSON} params
 */
export function dockerSwarmNodeUpdate(urlPrefix, params) {
  return axios({
    url: urlPrefix + '/docker/swarm/update',
    method: 'post',
    data: params
  })
}

/**
 * 容器集群服务列表
 * @param {JSON} params
 */
export function dockerSwarmServicesList(urlPrefix, params) {
  return axios({
    url: urlPrefix + '/docker/swarm-service/list',
    method: 'post',
    data: params,
    headers: {
      loading: 'no'
    }
  })
}

/**
 * 容器集群服务任务列表
 * @param {JSON} params
 */
export function dockerSwarmServicesTaskList(urlPrefix, params) {
  return axios({
    url: urlPrefix + '/docker/swarm-service/task-list',
    method: 'post',
    data: params,
    headers: {
      loading: 'no'
    }
  })
}

/**
 * 容器集群节点 删除服务
 * @param {JSON} params
 */
export function dockerSwarmServicesDel(urlPrefix, params) {
  return axios({
    url: urlPrefix + '/docker/swarm-service/del',
    method: 'get',
    params: params
  })
}

/**
 * 容器集群节点 删除服务
 * @param {JSON} params
 */
export function dockerSwarmServicesEdit(urlPrefix, params) {
  return axios({
    url: urlPrefix + '/docker/swarm-service/edit',
    method: 'post',
    data: params,
    headers: {
      'Content-Type': 'application/json'
    }
  })
}

/**
 * 开始拉取服务日志
 * @param {JSON} params
 */
export function dockerSwarmServicesStartLog(urlPrefix, params) {
  return axios({
    url: urlPrefix + '/docker/swarm-service/start-log',
    method: 'get',
    params: params
  })
}

/**
 * 拉取服务日志
 * @param {JSON} params
 */
export function dockerSwarmServicesPullLog(urlPrefix, params) {
  return axios({
    url: urlPrefix + '/docker/swarm-service/pull-log',
    method: 'get',
    params: params,
    headers: {
      loading: 'no'
    }
  })
}

export function dockerSwarmServicesDownloaLog(urlPrefix, id) {
  return loadRouterBase(urlPrefix + '/docker/swarm-service/download-log', {
    id: id
  })
}

/**
 * <!-- Note: detail description about taskState, please @see https://docs.docker.com/engine/swarm/how-swarm-mode-works/swarm-task-states/ -->
          <!-- reference Java class: com.github.dockerjava.api.model.TaskState -->

            <!-- NEW: The task was initialized. -->
            <!-- PENDING: Resources for the task were allocated. -->
            <!-- ASSIGNED: Docker assigned the task to nodes. -->
            <!-- ACCEPTED: The task was accepted by a worker node. If a worker node rejects the task, the state changes to REJECTED. -->
            <!-- PREPARING: Docker is preparing the task. -->
            <!-- STARTING: Docker is starting the task. -->
            <!-- RUNNING: The task is executing. -->
            <!-- COMPLETE: The task exited without an error code. -->
            <!-- SHUTDOWN: Docker requested the task to shut down. -->
            <!-- FAILED: The task exited with an error code. -->
            <!-- REJECTED: The worker node rejected the task. -->
            <!-- REMOVE: The task is not terminal but the associated service was removed or scaled down. -->
            <!-- ORPHANED: The node was down for too long. -->
 */
export const TASK_STATE = {
  NEW: t('i18n_40da3fb58b'),
  // ALLOCATED: "已分配",
  PENDING: t('i18n_047109def4'),
  ASSIGNED: t('i18n_fbfa6c18bf'),
  ACCEPTED: t('i18n_5d459d550a'),
  PREPARING: t('i18n_f76540a92e'),
  READY: t('i18n_424a2ad8f7'),
  STARTING: t('i18n_a34c24719b'),
  RUNNING: t('i18n_e9e9373c6f'),
  COMPLETE: t('i18n_f56c1d014e'),
  SHUTDOWN: t('i18n_095e938e2a'),
  FAILED: t('i18n_1c83d79715'),
  REJECTED: t('i18n_7173f80900'),
  REMOVE: t('i18n_86048b4fea'),
  ORPHANED: t('i18n_788a3afc90')
}
