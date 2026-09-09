<template>
  <div class="k8s-page">
    <n-grid :x-gap="12">
      <!-- 左栏：集群管理 -->
      <n-grid-item :span="6">
        <n-card size="small" :title="$t('i18n_8da25ce625')">
          <template #extra>
            <n-button size="small" type="primary" @click="resetForm">{{ $t('i18n_66ab5e9f24') }}</n-button>
          </template>
          <n-form layout="vertical" size="small">
            <n-form-item :label="$t('i18n_d7ec2d3fea')" required>
              <n-input v-model:value="form.name" :placeholder="$t('i18n_10787209c1')" />
            </n-form-item>
            <n-form-item :label="$t('i18n_f562f75c64')">
              <n-input v-model:value="form.serverUrl" placeholder="https://k8s-api:6443" />
            </n-form-item>
            <n-form-item :label="$t('i18n_160e6e4167')">
              <n-input v-model:value="form.namespace" placeholder="default" />
            </n-form-item>
            <n-form-item label="kubeconfig" required>
              <n-input
                v-model:value="form.kubeconfig"
                type="textarea"
                :rows="5"
                :placeholder="$t('i18n_b2f2c8555f')"
              />
            </n-form-item>
            <n-form-item>
              <n-button type="primary" block :loading="saving" @click="saveCluster">
                {{ form.id ? $t('i18n_076067a962') : $t('i18n_5f963a58f4') }}
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
                <n-button size="small" text @click="editCluster(c)">{{ $t('i18n_95b351c862') }}</n-button>
                <n-popconfirm @positive-click="deleteCluster(c)">
                  <template #trigger>
                    <span class="tw">
                      <n-button size="small" text danger>{{ $t('i18n_2f4aaddde3') }}</n-button>
                    </span>
                  </template>
                  {{ $t('i18n_f0f9dae974') }}
                </n-popconfirm>
              </div>
            </div>
            <n-empty v-if="!clusters.length" :description="$t('i18n_26d513ec39')" style="padding: 20px 0" />
          </div>
        </n-card>
      </n-grid-item>

      <!-- 右栏：资源浏览 -->
      <n-grid-item :span="18">
        <n-card size="small">
          <template #header>
            <n-space align="center">
              <span>{{ $t('i18n_b1d1de774f') }}</span>
              <n-tag v-if="current" color="blue">{{ current.name }}</n-tag>
            </n-space>
          </template>
          <template #extra>
            <n-space>
              <n-select
                v-model:value="nsFilter"
                style="width: 150px"
                :placeholder="$t('i18n_58173b2214')"
                :options="[{ label: $t('i18n_58173b2214'), value: 'all' }, ...namespaces.map((n) => ({ label: n, value: n }))]"
                @update:value="loadResources"
              />
              <n-select
                v-model:value="resType"
                style="width: 170px"
                :options="resourceTypes.map((t) => ({ label: t.label, value: t.value }))"
                @update:value="loadResources"
              />
              <n-button @click="loadResources">{{ $t('i18n_694fc5efa9') }}</n-button>
              <n-button @click="showEvents">{{ $t('i18n_10b2761db5') }}</n-button>
              <n-button type="primary" @click="deployVisible = true">{{ $t('i18n_a9f94dcd57') }}</n-button>
            </n-space>
          </template>

          <n-data-table
            :data="resources"
            :columns="columns"
            :loading="loading"
            :pagination="{ pageSize: 20 }"
            size="small"
            :row-key="(row) => `${row.type}/${row.namespace || '_'}/${row.name}`"
            :scroll-x="1400"
          />
          <n-empty
            v-if="current && !resources.length && !loading"
            :description="$t('i18n_809ef68c96')"
            style="padding: 30px 0"
          />
          <n-empty v-if="!current" :description="$t('i18n_2e84196cb0')" style="padding: 40px 0" />
        </n-card>
      </n-grid-item>
    </n-grid>

    <!-- 详情抽屉 -->
    <CustomDrawer :open="detailVisible" :title="$t('i18n_91415e5fa7', { name: detailName })" width="640" @close="detailVisible = false">
      <pre class="yaml-view">{{ detailYaml }}</pre>
    </CustomDrawer>

    <!-- 日志弹窗 -->
    <CustomModal v-model:open="logVisible" :title="$t('i18n_f4f22b2ee9', { name: logName })" width="720" :footer="null">
      <pre class="log-view">{{ logContent || $t('i18n_25c4e1a940') }}</pre>
    </CustomModal>

    <!-- 事件弹窗 -->
    <CustomModal v-model:open="eventVisible" :title="$t('i18n_10b2761db5')" width="760" :footer="null">
      <n-data-table
        :data="events"
        :columns="eventColumns"
        :pagination="{ pageSize: 20 }"
        size="small"
        :row-key="(row) => `${row.time}/${row.object}/${row.reason}`"
      />
    </CustomModal>

    <!-- 扩缩容弹窗 -->
    <CustomModal v-model:open="scaleVisible" :title="$t('i18n_80ca5c6501', { name: scaleName })" @ok="doScale">
      <n-form-item :label="$t('i18n_532495b65b')">
        <n-input-number v-model:value="scaleReplicas" :min="0" style="width: 200px" />
      </n-form-item>
    </CustomModal>

    <!-- 部署弹窗 -->
    <CustomModal v-model:open="deployVisible" :title="$t('i18n_505f1ed1a1')" width="680" @ok="doDeploy">
      <n-form-item :label="$t('i18n_a4b28a416f')">
        <n-input v-model:value="deployNs" :placeholder="$t('i18n_91ee459dd3')" style="width: 200px" />
      </n-form-item>
      <n-input
        v-model:value="manifest"
        type="textarea"
        :rows="10"
        :placeholder="$t('i18n_17383227e2')"
      />
    </CustomModal>
  </div>
</template>

<script lang="ts" setup>
import { ref, reactive, onMounted, h } from 'vue'
import { useI18n } from 'vue-i18n'
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

const { t: $t } = useI18n()

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
  if (['running', 'ready', 'bound', 'active', 'completed', 'succeeded', $t('i18n_769d88e425')].includes(s)) return 'green'
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
  if (!form.name) return $message.warning($t('i18n_debdfce084'))
  // 新增必须粘贴 kubeconfig；编辑时留空表示不修改
  if (!form.id && !form.kubeconfig) return $message.warning($t('i18n_257722c860'))
  saving.value = true
  try {
    const res: any = await saveK8sCluster(form)
    if (res.code === 200) {
      $message.success($t('i18n_6179c4c137'))
      resetForm()
      loadClusters()
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
    // 凭证明文不回显：留空表示不修改（后端保留原 kubeconfig）
    kubeconfig: ''
  })
}
const deleteCluster = async (c) => {
  const res: any = await deleteK8sCluster({ id: c.id })
  if (res.code === 200) {
    $message.success($t('i18n_5cc232620c'))
    if (current.value && current.value.id === c.id) {
      // 删除的是当前集群：同步清空右侧资源表，避免对已删集群的僵尸操作
      current.value = null
      resources.value = []
      namespaces.value = []
    }
    loadClusters()
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
    }
  } finally {
    loading.value = false
  }
}
const showDetail = async (record) => {
  if (!current.value) return $message.warning($t('i18n_de352e6923'))
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
  }
}
const removeResource = async (record) => {
  if (!current.value) return $message.warning($t('i18n_de352e6923'))
  const res: any = await deleteK8sResource({
    id: current.value.id,
    namespace: record.namespace,
    type: record.type,
    name: record.name
  })
  if (res.code === 200) {
    $message.success($t('i18n_5cc232620c'))
    loadResources()
  }
}
const showScale = (record) => {
  scaleName.value = record.name
  scaleNamespace.value = record.namespace
  scaleReplicas.value = 1
  scaleVisible.value = true
}
const doScale = async () => {
  if (!current.value) return $message.warning($t('i18n_de352e6923'))
  const res: any = await scaleK8sDeployment({
    id: current.value.id,
    namespace: scaleNamespace.value,
    name: scaleName.value,
    replicas: scaleReplicas.value
  })
  if (res.code === 200) {
    $message.success($t('i18n_0b3ea15791'))
    scaleVisible.value = false
    loadResources()
  }
}
// 滚动重启生产负载属破坏性操作：二次确认（与同页删除操作保持一致）
const doRestart = (record) => {
  if (!current.value) return $message.warning($t('i18n_de352e6923'))
  $confirm({
    title: $t('i18n_f6fd587d70', { type: record.type, name: record.name }),
    onOk: async () => {
      const res: any = await restartK8sDeployment({ id: current.value.id, namespace: record.namespace, name: record.name })
      if (res.code === 200) {
        $message.success($t('i18n_aba3420d49'))
        loadResources()
      }
    }
  })
}
const showLog = async (record) => {
  if (!current.value) return $message.warning($t('i18n_de352e6923'))
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
  }
}
const showEvents = async () => {
  if (!current.value) return $message.warning($t('i18n_de352e6923'))
  const res: any = await listK8sEvents({
    id: current.value.id,
    namespace: nsFilter.value === 'all' ? '' : nsFilter.value
  })
  if (res.code === 200) {
    events.value = res.data || []
    eventVisible.value = true
  }
}
const doDeploy = async () => {
  if (!current.value) return $message.warning($t('i18n_de352e6923'))
  if (!manifest.value) return $message.warning($t('i18n_65d8e31bcb'))
  const res: any = await applyK8sManifest({ id: current.value.id, namespace: deployNs.value, manifest: manifest.value })
  if (res.code === 200) {
    $message.success($t('i18n_446c6b6d6b'))
    deployVisible.value = false
    manifest.value = ''
    loadResources()
  }
}

const columns = [
  {
    title: $t('i18n_d7ec2d3fea'),
    key: 'name',
    fixed: 'left',
    width: 220,
    render: (row) => h('a', { onClick: () => showDetail(row) }, row.name)
  },
  { title: $t('i18n_a4b28a416f'), key: 'namespace', width: 140 },
  { title: 'Kind', key: 'kind', width: 130 },
  {
    title: $t('i18n_3fea7ca76c'),
    key: 'status',
    width: 120,
    render: (row) => h(NTag, { color: statusColor(row) }, { default: () => row.status || '-' })
  },
  { title: $t('i18n_c0d2181d57'), key: 'ready', width: 90 },
  {
    title: $t('i18n_eca37cb072'),
    key: 'createdAt',
    width: 160,
    render: (row) => (row.createdAt ? formatTime(row.createdAt) : '')
  },
  {
    title: $t('i18n_2b6bc0f293'),
    key: 'actions',
    width: 280,
    fixed: 'right',
    render: (row) => {
      const buttons = [
        h(NButton, { size: 'small', text: true, onClick: () => showDetail(row) }, { default: () => $t('i18n_f26225bde6') })
      ]
      if (isWorkload(row)) {
        buttons.push(
          h(NButton, { size: 'small', text: true, onClick: () => showScale(row) }, { default: () => $t('i18n_4527a7d8cd') })
        )
      }
      if (isWorkload(row)) {
        buttons.push(
          h(NButton, { size: 'small', text: true, onClick: () => doRestart(row) }, { default: () => $t('i18n_01b4e06f39') })
        )
      }
      if (row.type === 'pods') {
        buttons.push(h(NButton, { size: 'small', text: true, onClick: () => showLog(row) }, { default: () => $t('i18n_456d29ef8b') }))
      }
      buttons.push(
        h(
          NPopconfirm,
          { onPositiveClick: () => removeResource(row) },
          {
            trigger: () => h(NButton, { size: 'small', text: true, danger: true }, { default: () => $t('i18n_2f4aaddde3') }),
            default: () => $t('i18n_703e56f2dd')
          }
        )
      )
      return h(NSpace, { size: 0, wrap: true }, { default: () => buttons })
    }
  }
]

const eventColumns = [
  {
    title: $t('i18n_226b091218'),
    key: 'type',
    width: 80,
    render: (row) => h(NTag, { color: row.type === 'Warning' ? 'red' : 'blue' }, { default: () => row.type })
  },
  { title: $t('i18n_41dfb0bf61'), key: 'reason', width: 110 },
  { title: $t('i18n_b14494137c'), key: 'object', width: 160 },
  { title: $t('i18n_a4b28a416f'), key: 'namespace', width: 120 },
  { title: $t('i18n_ff692f04ac'), key: 'message', ellipsis: { tooltip: true } },
  { title: $t('i18n_f965fea308'), key: 'count', width: 60 }
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
