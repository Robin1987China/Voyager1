///
/// Copyright (c) 2026 Voyager1
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///     http://www.apache.org/licenses/LICENSE-2.0
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

/**
 * 路由菜单
 * key 对应后台接口返回的菜单中的 id
 * value 表示该路由的 path
 */
const routeMenuMap: Record<string, string> = {
  nodeList: '/node/list',

  dockerList: '/docker/list',
  dockerSwarm: '/docker/swarm',
  sshList: '/ssh',
  commandList: '/ssh/command',
  commandLogList: '/ssh/command-log',
  outgivingList: '/dispatch/list',
  outgivingLog: '/dispatch/log',
  logRead: '/dispatch/log-read',
  outgivingWhitelistDirectory: '/dispatch/white-list',
  monitorList: '/monitor/list',
  monitorLog: '/monitor/log',
  userOptLog: '/monitor/operate-log',
  repository: '/repository/list',
  buildListOld: '/build/list',
  scriptAllList: '/node/script-all',
  serverScriptList: '/script/script-list',
  serverScriptLogList: '/script/script-log',
  /**
   * new build list page
   */
  buildList: '/build/list-info',
  buildHistory: '/build/history',
  versionList: '/pipeline/version-list',
  pipelineList: '/pipeline/pipeline-list',
  swimlane: '/pipeline/swimlane',
  cloudList: '/cloud/list',
  k8sList: '/k8s/list',
  finopsList: '/finops/list',
  userList: '/user/list',
  permission_group: '/user/permission-group',
  user_log: '/operation/log',
  user_login_log: '/user/login-log',
  monitorConfigEmail: '/system/mail',
  cacheManage: '/system/cache',
  logManage: '/system/log',
  update: '/system/upgrade',
  sysConfig: '/system/config',
  systemExtConfig: '/system/ext-config',
  projectSearch: '/node/search',
  // 数据库备份
  backup: '/system/backup',
  workspace: '/system/workspace',
  globalEnv: '/system/global-env',
  machine_node_info: '/system/assets/machine-list',
  machine_ssh_info: '/system/assets/ssh-list',
  machine_docker_info: '/system/assets/docker-list',
  global_repository: '/system/assets/repository-list',
  script_library: '/system/assets/script-library',
  configWorkspaceEnv: '/script/env-list',
  cronTools: '/tools/cron',
  netTools: '/tools/network',
  myWorkspaceList: '/my-workspace',
  fileStorage: '/file-manager/file-storage',
  staticFileStorage: '/file-manager/static-file-storage',
  fileReleaseTask: '/file-manager/release-task',
  certificate: '/certificate/list',
  authConfig: '/system/oauth-config',
  overview: '/overview',
  'system-overview': '/system/overview',
  about: '/about'
}

export default routeMenuMap
