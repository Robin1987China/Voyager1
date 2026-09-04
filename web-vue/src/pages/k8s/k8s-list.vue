<template>
  <div class="k8s-page">
    <n-grid :x-gap="12">
      <!-- 左栏：集群管理 -->
      <n-grid-item :span="6">
        <n-card size="small" title="K8s 集群">
          <template #extra>
            <n-button size="small" type="primary" @click="resetForm">新增</n-button>
          </template>
          <n-form layout="vertical" size="small">
            <n-form-item label="名称" required>
              <n-input v-model:value="form.name" placeholder="如 生产集群" />
            </n-form-item>
            <n-form-item label="服务地址">
              <n-input v-model:value="form.serverUrl" placeholder="https://k8s-api:6443" />
            </n-form-item>
            <n-form-item label="默认命名空间">
              <n-input v-model:value="form.namespace" placeholder="default" />
            </n-form-item>
            <n-form-item label="kubeconfig" required>
              <n-input
                v-model:value="form.kubeconfig"
                type="textarea"
                :rows="5"
                placeholder="粘贴 kubeconfig 内容（集群访问凭证）"
              />
            </n-form-item>
            <n-form-item>
              <n-button type="primary" block :loading="saving" @click="saveCluster">
                {{ form.id ? '更新集群' : '保存集群' }}
              </n-button>
            </n-form-item>
          </n-form>

          <n-divider style="margin: 8px 0" />
          <div class="cluster-list">
            <div
              v-for="c in clusters"
              :key="c.id"
              class="cluster-item"
              :class="{ active: current && current.id === c.id }"
              @click="selectCluster(c)"
            >
              <div class="cluster-name">{{ c.name }}</div>
              <div class="cluster-meta">{{ c.serverUrl || '—' }} · {{ c.namespace }}</div>
              <div class="cluster-actions" @click.stop>
                <n-button size="small" text @click="editCluster(c)">编辑</n-button>
                <n-popconfirm @positive-click="deleteCluster(c)">
                  <template #trigger>
                    <span class="tw">
                      <n-button size="small" text danger>删除</n-button>
                    </span>
                  </template>
                  确认删除该集群？
                </n-popconfirm>
              </div>
            </div>
            <n-empty v-if="!clusters.length" description="暂无集群" style="padding: 20px 0" />
          </div>
        </n-card>
      </n-grid-item>

      <!-- 右栏：资源浏览 -->
      <n-grid-item :span="18">
        <n-card size="small">
          
          <template #extra>
            <n-space>
              <n-select
                v-model:value="nsFilter"
                style="width: 150px"
                placeholder="所有命名空间"
                :options="[{ label: '所有命名空间', value: 'all' }, ...namespaces.map((n) => ({ label: n, value: n }))]"
                @update:value="loadResources"
              />
              <n-select
                v-model:value="resType"
                style="width: 170px"
                :options="resourceTypes.map((t) => ({ label: t.label, value: t.value }))"
                @update:value="loadResources"
              />
              <n-button @click="loadResources">刷新</n-button>
              <n-button @click="showEvents">事件</n-button>
              <n-button type="primary" @click="deployVisible = true">部署</n-button>
            </n-space>
          </template>

                    <n-card size="small" :body-style="{ padding: '12px' }" style="margin-bottom: 12px">

            <n-space wrap>
              <span>资源浏览</span>
              <n-tag v-if="current" color="blue">{{ current.name }}</n-tag>
            </n-space>
          
          </n-card>
<n-data-table
            :data="resources"
            :columns="columns"
            :loading="loading"
            :pagination="false"
            size="small"
            :row-key="(row) => row.name"
            
          />
          <n-empty
            v-if="current && !resources.length && !loading"
            description="该类型暂无资源"
            style="padding: 30px 0"
          />
          <n-empty v-if="!current" description="请先选择左侧集群" style="padding: 40px 0" />
        </n-card>
      </n-grid-item>
    </n-grid>

    <!-- 详情抽屉 -->
    <CustomDrawer :open="detailVisible" :title="`${detailName} 详情`" width="640" @close="detailVisible = false">
      <pre class="yaml-view">{{ detailYaml }}</pre>
    </CustomDrawer>

    <!-- 日志弹窗 -->
    <CustomModal v-model:open="logVisible" :title="`日志：${logName}`" width="720" :footer="null">
      <pre class="log-view">{{ logContent || '（无日志）' }}</pre>
    </CustomModal>

    <!-- 事件弹窗 -->
    <CustomModal v-model:open="eventVisible" title="事件" width="760" :footer="null">
      <n-data-table
        :data="events"
        :columns="eventColumns"
        :pagination="false"
        size="small"
        :row-key="(row) => row.time"
      />
    </CustomModal>

    <!-- 扩缩容弹窗 -->
    <CustomModal v-model:open="scaleVisible" :title="`扩缩容：${scaleName}`" @ok="doScale">
      <n-form-item label="副本数">
        <n-input-number v-model:value="scaleReplicas" :min="0" style="width: 200px" />
      </n-form-item>
    </CustomModal>

    <!-- 部署弹窗 -->
    <CustomModal v-model:open="deployVisible" title="部署（Apply Manifest）" width="680" @ok="doDeploy">
      <n-form-item label="命名空间">
        <n-input v-model:value="deployNs" placeholder="留空使用 manifest 内定义" style="width: 200px" />
      </n-form-item>
      <n-input
        v-model:value="manifest"
        type="textarea"
        :rows="10"
        placeholder="粘贴 YAML manifest（Deployment/Service 等，可多资源）"
      />
    </CustomModal>
  </div>
</template>

<script lang="ts" setup>
import { ref, reactive, onMounted, h } from 'vue'
import { NTag, NButton, NSpace, NPopconfirm } from 'naive-ui'
import dayjs from 'dayjs'
import {
  saveK8sCluster,
  listK8sClusters,
  deleteK8sCluster,
  listK8sNamespaces,
  listK8sResources,
  getK8sResourceDetail,
  deleteK8sResource,
  scaleK8sDeployment,
  restartK8sDeployment,
  getK8sPodLog,
  listK8sEvents,
  applyK8sManifest
} from '@/api/k8s'

const resourceTypes = [
  { value: 'pods', label: 'Pod' },
  { value: 'deployments', label: 'Deployment' },
  { value: 'services', label: 'Service' },
  { value: 'configmaps', label: 'ConfigMap' },
  { value: 'secrets', label: 'Secret' },
  { value: 'statefulsets', label: 'StatefulSet' },
  { value: 'daemonsets', label: 'DaemonSet' },
  { value: 'jobs', label: 'Job' },
  { value: 'cronjobs', label: 'CronJob' },
  { value: 'ingresses', label: 'Ingress' },
  { value: 'nodes', label: 'Node' },
  { value: 'persistentvolumes', label: 'PersistentVolume' },
  { value: 'persistentvolumeclaims', label: 'PersistentVolumeClaim' },
  { value: 'namespaces', label: 'Namespace' }
]

const form = reactive({ id: '', name: '', serverUrl: '', namespace: 'default', kubeconfig: '' })
const clusters = ref<any[]>([])
const current = ref<any>(null)
const namespaces = ref<string[]>([])
const nsFilter = ref('all')
const resType = ref('pods')
const resources = ref<any[]>([])
const loading = ref(false)
const saving = ref(false)

const detailVisible = ref(false)
const detailName = ref('')
const detailYaml = ref('')

const logVisible = ref(false)
const logName = ref('')
const logContent = ref('')

const eventVisible = ref(false)
const events = ref<any[]>([])

const scaleVisible = ref(false)
const scaleName = ref('')
const scaleNamespace = ref('')
const scaleReplicas = ref(1)

const deployVisible = ref(false)
const deployNs = ref('')
const manifest = ref('')

const statusColor = (r) => {
  const s = (r.status || '').toLowerCase()
  if (['running', 'ready', 'bound', 'active', 'completed', 'succeeded', '完成'].includes(s)) return 'green'
  if (['pending', 'containercreating', 'terminating', 'notready'].includes(s)) return 'orange'
  if (['failed', 'error', 'crashloopbackoff', 'lost', 'released'].includes(s)) return 'red'
  return 'default'
}
const formatTime = (t) => (t ? dayjs(t).format('YYYY-MM-DD HH:mm:ss') : '')
const isWorkload = (r) => ['deployments', 'statefulsets', 'daemonsets'].includes(r.type)

const loadClusters = async () => {
  const res: any = await listK8sClusters({})
  if (res.code === 200) clusters.value = res.data || []
}
const resetForm = () => {
  Object.assign(form, { id: '', name: '', serverUrl: '', namespace: 'default', kubeconfig: '' })
}
const saveCluster = async () => {
  if (!form.name) return $message.warning('请输入集群名称')
  if (!form.kubeconfig) return $message.warning('请粘贴 kubeconfig')
  saving.value = true
  try {
    const res: any = await saveK8sCluster(form)
    if (res.code === 200) {
      $message.success('集群已保存')
      resetForm()
      loadClusters()
    } else {
      $message.error(res.msg)
    }
  } finally {
    saving.value = false
  }
}
const editCluster = (c) => {
  Object.assign(form, {
    id: c.id,
    name: c.name,
    serverUrl: c.serverUrl,
    namespace: c.namespace,
    kubeconfig: c.kubeconfig
  })
}
const deleteCluster = async (c) => {
  const res: any = await deleteK8sCluster({ id: c.id })
  if (res.code === 200) {
    $message.success('已删除')
    if (current.value && current.value.id === c.id) current.value = null
    loadClusters()
  } else {
    $message.error(res.msg)
  }
}
const selectCluster = async (c) => {
  current.value = c
  nsFilter.value = 'all'
  await loadNamespaces()
  await loadResources()
}
const loadNamespaces = async () => {
  if (!current.value) return
  const res: any = await listK8sNamespaces({ id: current.value.id })
  if (res.code === 200) namespaces.value = res.data || []
}
const loadResources = async () => {
  if (!current.value) return
  loading.value = true
  try {
    const res: any = await listK8sResources({ id: current.value.id, namespace: nsFilter.value, type: resType.value })
    if (res.code === 200) {
      resources.value = res.data || []
    } else {
      resources.value = []
      $message.error(res.msg)
    }
  } finally {
    loading.value = false
  }
}
const showDetail = async (record) => {
  const res: any = await getK8sResourceDetail({
    id: current.value.id,
    namespace: record.namespace,
    type: record.type,
    name: record.name
  })
  if (res.code === 200) {
    detailName.value = record.name
    detailYaml.value = res.data || ''
    detailVisible.value = true
  } else {
    $message.error(res.msg)
  }
}
const removeResource = async (record) => {
  const res: any = await deleteK8sResource({
    id: current.value.id,
    namespace: record.namespace,
    type: record.type,
    name: record.name
  })
  if (res.code === 200) {
    $message.success('已删除')
    loadResources()
  } else {
    $message.error(res.msg)
  }
}
const showScale = (record) => {
  scaleName.value = record.name
  scaleNamespace.value = record.namespace
  scaleReplicas.value = 1
  scaleVisible.value = true
}
const doScale = async () => {
  const res: any = await scaleK8sDeployment({
    id: current.value.id,
    namespace: scaleNamespace.value,
    name: scaleName.value,
    replicas: scaleReplicas.value
  })
  if (res.code === 200) {
    $message.success('扩缩容成功')
    scaleVisible.value = false
    loadResources()
  } else {
    $message.error(res.msg)
  }
}
const doRestart = async (record) => {
  const res: any = await restartK8sDeployment({ id: current.value.id, namespace: record.namespace, name: record.name })
  if (res.code === 200) {
    $message.success('已触发滚动重启')
    loadResources()
  } else {
    $message.error(res.msg)
  }
}
const showLog = async (record) => {
  const res: any = await getK8sPodLog({
    id: current.value.id,
    namespace: record.namespace,
    name: record.name,
    tailLines: 500
  })
  if (res.code === 200) {
    logName.value = record.name
    logContent.value = res.data || ''
    logVisible.value = true
  } else {
    $message.error(res.msg)
  }
}
const showEvents = async () => {
  if (!current.value) return $message.warning('请先选择集群')
  const res: any = await listK8sEvents({
    id: current.value.id,
    namespace: nsFilter.value === 'all' ? '' : nsFilter.value
  })
  if (res.code === 200) {
    events.value = res.data || []
    eventVisible.value = true
  } else {
    $message.error(res.msg)
  }
}
const doDeploy = async () => {
  if (!current.value) return $message.warning('请先选择集群')
  if (!manifest.value) return $message.warning('请粘贴 manifest')
  const res: any = await applyK8sManifest({ id: current.value.id, namespace: deployNs.value, manifest: manifest.value })
  if (res.code === 200) {
    $message.success('部署成功')
    deployVisible.value = false
    manifest.value = ''
    loadResources()
  } else {
    $message.error(res.msg)
  }
}

const columns = [
  {
    title: '名称',
    key: 'name',
    fixed: 'left',
    width: 220,
    render: (row) => h('a', { onClick: () => showDetail(row) }, row.name)
  },
  { title: '命名空间', key: 'namespace', width: 140 },
  { title: 'Kind', key: 'kind', width: 130 },
  {
    title: '状态',
    key: 'status',
    width: 120,
    render: (row) => h(NTag, { color: statusColor(row) }, { default: () => row.status || '-' })
  },
  { title: '就绪', key: 'ready', width: 90 },
  {
    title: '创建时间',
    key: 'createdAt',
    width: 160,
    render: (row) => (row.createdAt ? formatTime(row.createdAt) : '')
  },
  {
    title: '操作',
    key: 'actions',
    width: 280,
    fixed: 'right',
    render: (row) => {
      const buttons = [
        h(NButton, { size: 'small', text: true, onClick: () => showDetail(row) }, { default: () => '详情' })
      ]
      if (isWorkload(row)) {
        buttons.push(
          h(NButton, { size: 'small', text: true, onClick: () => showScale(row) }, { default: () => '扩缩容' })
        )
      }
      if (isWorkload(row)) {
        buttons.push(
          h(NButton, { size: 'small', text: true, onClick: () => doRestart(row) }, { default: () => '重启' })
        )
      }
      if (row.type === 'pods') {
        buttons.push(h(NButton, { size: 'small', text: true, onClick: () => showLog(row) }, { default: () => '日志' }))
      }
      buttons.push(
        h(
          NPopconfirm,
          { onPositiveClick: () => removeResource(row) },
          {
            trigger: () => h(NButton, { size: 'small', text: true, danger: true }, { default: () => '删除' }),
            default: () => '确认删除该资源？'
          }
        )
      )
      return h(NSpace, { size: 0, wrap: true }, { default: () => buttons })
    }
  }
]

const eventColumns = [
  {
    title: '类型',
    key: 'type',
    width: 80,
    render: (row) => h(NTag, { color: row.type === 'Warning' ? 'red' : 'blue' }, { default: () => row.type })
  },
  { title: '原因', key: 'reason', width: 110 },
  { title: '对象', key: 'object', width: 160 },
  { title: '命名空间', key: 'namespace', width: 120 },
  { title: '消息', key: 'message', ellipsis: true },
  { title: '次数', key: 'count', width: 60 }
]

onMounted(loadClusters)
</script>

<style scoped>
.cluster-list {
  max-height: 460px;
  overflow-y: auto;
}
.cluster-item {
  padding: 8px 10px;
  border: 1px solid var(--app-color-border-secondary);
  border-radius: 6px;
  margin-bottom: 6px;
  cursor: pointer;
  transition: all 0.2s;
}
.cluster-item:hover {
  border-color: #1677ff;
}
.cluster-item.active {
  border-color: #1677ff;
  background: var(--app-color-primary-bg);
}
.cluster-name {
  font-weight: 600;
  font-size: 13px;
}
.cluster-meta {
  color: var(--app-color-text-secondary);
  font-size: 12px;
  margin: 2px 0 4px;
}
.cluster-actions {
  display: flex;
  gap: 4px;
}
.yaml-view {
  background: #0b1220;
  color: #7bd88f;
  padding: 12px;
  border-radius: 6px;
  font-size: 12px;
  line-height: 1.5;
  max-height: calc(100vh - 120px);
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-all;
}
.log-view {
  background: #0b1220;
  color: #d0d0d0;
  padding: 12px;
  border-radius: 6px;
  font-size: 12px;
  line-height: 1.5;
  max-height: 480px;
  overflow: auto;
  white-space: pre-wrap;
}
</style>
