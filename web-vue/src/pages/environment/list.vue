<template>
  <div>
    <CustomTable
      is-show-tools
      :active-page="activePage"
      table-name="environment-list"
      :empty-description="$t('i18n_6f160c3f6d')"
      :columns="columns"
      :data="list"
      :loading="loading"
      size="medium"
      row-key="id"
      :pagination="pagination"
      @refresh="loadData"
    >
      <template #title>
        <n-space wrap>
          <n-button type="primary" :loading="loading" @click="loadData">{{ $t('i18n_bee912d79e') }}</n-button>
        </n-space>
      </template>
      <template #tableBodyCell="{ column, text, record }">
        <template v-if="column.dataIndex === 'strategy'">
          <n-tag :type="text === 'CI_CD' ? 'success' : 'warning'">{{ text === 'CI_CD' ? 'CI/CD' : $t('i18n_9c2e7a4b63') }}</n-tag>
        </template>
        <template v-else-if="column.dataIndex === 'approvalRequired'">
          <n-tag :type="text ? 'error' : 'default'">{{ text ? $t('i18n_94f5cb1a18') : $t('i18n_5a3f2f14d0') }}</n-tag>
        </template>
        <template v-else-if="column.dataIndex === 'targets'">
          <n-space>
            <n-tag v-for="t in (record._targets || [])" :key="t.id" size="small">
              {{ t.targetType }}:{{ t.targetId }}{{ t.projectId ? '/' + t.projectId : '' }}
            </n-tag>
            <n-text v-if="!(record._targets || []).length" depth="3">{{ $t('i18n_906ad18b48') }}</n-text>
          </n-space>
        </template>
        <template v-else-if="column.dataIndex === 'operation'">
          <n-space>
            <n-button size="small" @click="openEdit(record)">{{ $t('i18n_95b351c862') }}</n-button>
            <n-button size="small" type="primary" @click="openBind(record)">{{ $t('i18n_6c8f934340') }}</n-button>
          </n-space>
        </template>
        <template v-else>
          {{ text }}
        </template>
      </template>
    </CustomTable>

    <CustomModal
      v-model:open="editVisible"
      :title="$t('i18n_6b3d24d59e')"
      :mask-closable="false"
      :confirm-loading="editLoading"
      @ok="saveEdit"
    >
      <n-form label-width="120px">
        <n-form-item :label="$t('i18n_d7ec2d3fea')">
          <n-input v-model:value="editForm.name" disabled />
        </n-form-item>
        <n-form-item :label="$t('i18n_226b091218')">
          <n-input v-model:value="editForm.type" placeholder="dev/test/prod" />
        </n-form-item>
        <n-form-item :label="$t('i18n_66914536fa')">
          <n-select v-model:value="editForm.strategy" :options="strategyOptions" />
        </n-form-item>
        <n-form-item :label="$t('i18n_65028db95a')">
          <n-switch v-model:value="editForm.approvalRequired" />
        </n-form-item>
      </n-form>
    </CustomModal>

    <CustomModal
      v-model:open="bindVisible"
      :title="$t('i18n_0ac632064c')"
      :mask-closable="false"
      :confirm-loading="bindLoading"
      @ok="saveBind"
    >
      <n-form label-width="120px">
        <n-form-item :label="$t('i18n_b02b8bfbdc')">
          <n-select v-model:value="bindForm.targetType" :options="targetTypeOptions" @update:value="onTargetTypeChange" />
        </n-form-item>
        <n-form-item :label="targetIdLabel" required>
          <n-select
            v-if="bindForm.targetType === 'K8S'"
            v-model:value="bindForm.targetId"
            :options="clusterOptions"
            :placeholder="$t('i18n_c3b5b89d4b')"
            filterable
          />
          <n-select
            v-else-if="bindForm.targetType === 'SSH'"
            v-model:value="bindForm.targetId"
            :options="sshOptions"
            :placeholder="$t('i18n_f842f497bc')"
            filterable
          />
          <n-input v-else v-model:value="bindForm.targetId" :placeholder="$t('i18n_d591f6dd40')" />
        </n-form-item>
        <n-form-item :label="projectIdLabel" :required="bindForm.targetType === 'NODE'">
          <n-input
            v-model:value="bindForm.projectId"
            :placeholder="bindForm.targetType === 'K8S' ? $t('i18n_a4b28a416f') : bindForm.targetType === 'SSH' ? $t('i18n_dbb2df00cf') : $t('i18n_0e7c0b0209')"
          />
        </n-form-item>
        <n-form-item :label="$t('i18n_347002304e')">
          <n-space>
            <n-tag
              v-for="t in bindTargets"
              :key="t.id"
              closable
              @close="unbind(t)"
            >
              {{ t.targetType }}:{{ t.targetId }}{{ t.projectId ? '/' + t.projectId : '' }}
            </n-tag>
            <n-text v-if="!bindTargets.length" depth="3">{{ $t('i18n_0f3222f98c') }}</n-text>
          </n-space>
        </n-form-item>
      </n-form>
    </CustomModal>
  </div>
</template>

<script lang="ts" setup>
import { ref, onMounted, reactive, computed } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY } from '@/utils/const'
import {
  listEnvironments,
  saveEnvironment,
  bindEnvironmentTarget,
  unbindEnvironmentTarget,
  listEnvironmentTargets
} from '@/api/environment'
import { listK8sClusters } from '@/api/k8s'
import { getSshListAll } from '@/api/ssh'

const { t: $t } = useI18n()
const route = useRoute()

const columns = [
  { title: $t('i18n_d7ec2d3fea'), key: 'name' },
  { title: $t('i18n_226b091218'), key: 'type' },
  { title: $t('i18n_66914536fa'), key: 'strategy' },
  { title: $t('i18n_0273ba5c95'), key: 'approvalRequired' },
  { title: $t('i18n_b332ac2a0e'), key: 'targets' },
  { title: $t('i18n_2b6bc0f293'), key: 'operation', width: 200 }
]
const strategyOptions = [
  { label: $t('i18n_5869f8e756'), value: 'CI_CD' },
  { label: $t('i18n_0b07e6ae48'), value: 'CD_ONLY' }
]
// 部署目标类型：节点 / K8S 集群 / SSH 主机
const targetTypeOptions = [
  { label: $t('i18n_3bf3c0a8d6'), value: 'NODE' },
  { label: $t('i18n_c3b5b89d4b'), value: 'K8S' },
  { label: $t('i18n_f842f497bc'), value: 'SSH' }
]

const listQuery = reactive({ ...PAGE_DEFAULT_LIST_QUERY })
const list = ref<any[]>([])
const loading = ref(false)
const activePage = computed(() => route.path === '/environment/list')
// 后端返回全量数组：前端本地分页（total 联动，避免伪分页）
const pagination = computed(() => {
  const p = COMPUTED_PAGINATION(listQuery)
  p.total = list.value.length
  p.itemCount = list.value.length
  return p
})

const editVisible = ref(false)
const editLoading = ref(false)
const editForm = reactive({ id: '', name: '', type: '', strategy: 'CI_CD', approvalRequired: false })

const bindVisible = ref(false)
const bindLoading = ref(false)
const bindForm = reactive({ environmentId: '', targetType: 'NODE', targetId: '', projectId: '' })
// 弹窗内已绑定目标列表：独立维护，绑定/解绑后实时同步，不用陈旧快照
const bindTargets = ref<any[]>([])
// K8S 集群 / SSH 主机数据源（打开绑定弹窗时懒加载）
const clusters = ref<any[]>([])
const sshHosts = ref<any[]>([])
const clusterOptions = computed(() => clusters.value.map((c) => ({ label: c.name || c.id, value: c.id })))
const sshOptions = computed(() => sshHosts.value.map((s) => ({ label: s.name || s.id, value: s.id })))
const targetIdLabel = computed(() =>
  bindForm.targetType === 'K8S' ? $t('i18n_c3b5b89d4b') : bindForm.targetType === 'SSH' ? $t('i18n_f842f497bc') : $t('i18n_8af49c617c')
)
const projectIdLabel = computed(() =>
  bindForm.targetType === 'K8S' ? $t('i18n_a4b28a416f') : bindForm.targetType === 'SSH' ? $t('i18n_dbb2df00cf') : $t('i18n_3936ddf911')
)

const loadTargetSources = async () => {
  const [k8sRes, sshRes] = await Promise.allSettled([
    listK8sClusters({}),
    getSshListAll({})
  ])
  if (k8sRes.status === 'fulfilled' && (k8sRes.value as any).code === 200) {
    clusters.value = (k8sRes.value as any).data || []
  }
  if (sshRes.status === 'fulfilled' && (sshRes.value as any).code === 200) {
    sshHosts.value = (sshRes.value as any).data || []
  }
}

const fetchTargets = async (environmentId: string) => {
  const t: any = await listEnvironmentTargets({ environmentId })
  return t.code === 200 ? t.data || [] : []
}

const loadData = async () => {
  loading.value = true
  try {
    const res: any = await listEnvironments({})
    if (res.code === 200) {
      const envs = res.data || []
      // 并发拉取各环境目标，单点失败不影响整页
      const results = await Promise.allSettled(envs.map((e) => fetchTargets(e.id)))
      envs.forEach((e, i) => {
        e._targets = results[i].status === 'fulfilled' ? results[i].value : []
      })
      list.value = envs
    }
  } finally {
    loading.value = false
  }
}

const openEdit = (record) => {
  editForm.id = record.id
  editForm.name = record.name
  editForm.type = record.type || record.name
  editForm.strategy = record.strategy || 'CI_CD'
  editForm.approvalRequired = !!record.approvalRequired
  editVisible.value = true
}

const saveEdit = async () => {
  editLoading.value = true
  try {
    const res: any = await saveEnvironment({
      id: editForm.id,
      name: editForm.name,
      type: editForm.type,
      strategy: editForm.strategy,
      approvalRequired: editForm.approvalRequired
    })
    if (res.code === 200) {
      $message.success($t('i18n_3b108349b9'))
      editVisible.value = false
      loadData()
    }
  } finally {
    editLoading.value = false
  }
}

const openBind = async (record) => {
  bindForm.environmentId = record.id
  bindForm.targetType = 'NODE'
  bindForm.targetId = ''
  bindForm.projectId = ''
  bindTargets.value = record._targets || []
  bindVisible.value = true
  // 打开时重新拉取，确保是最新绑定状态；同时懒加载 K8S 集群 / SSH 主机数据源
  bindTargets.value = await fetchTargets(record.id)
  loadTargetSources()
}

const onTargetTypeChange = () => {
  bindForm.targetId = ''
  bindForm.projectId = ''
}

const saveBind = async () => {
  if (!bindForm.targetId || !bindForm.targetId.trim()) {
    $message.warning($t('i18n_8cec52fd30'))
    return
  }
  // 仅 NODE 目标强制要求 projectId；K8S 命名空间 / SSH 发布目录可留空
  if (bindForm.targetType === 'NODE' && (!bindForm.projectId || !bindForm.projectId.trim())) {
    $message.warning($t('i18n_fbf66d4ef8'))
    return
  }
  bindLoading.value = true
  try {
    const res: any = await bindEnvironmentTarget({
      environmentId: bindForm.environmentId,
      targetType: bindForm.targetType,
      targetId: bindForm.targetId.trim(),
      projectId: bindForm.projectId.trim()
    })
    if (res.code === 200) {
      $message.success($t('i18n_1974fe5349'))
      bindForm.targetId = ''
      bindForm.projectId = ''
      // 同步刷新弹窗内列表与底层表格行
      bindTargets.value = await fetchTargets(bindForm.environmentId)
      const row = list.value.find((e) => e.id === bindForm.environmentId)
      if (row) {
        row._targets = bindTargets.value
      }
    }
  } finally {
    bindLoading.value = false
  }
}

const unbind = (t) => {
  $confirm({
    title: $t('i18n_0131f9a074', { targetType: t.targetType, targetId: t.targetId }),
    onOk: async () => {
      const res: any = await unbindEnvironmentTarget({ id: t.id })
      if (res.code === 200) {
        $message.success($t('i18n_1c4385b583'))
        // 弹窗内即时移除 + 同步底层表格行
        bindTargets.value = bindTargets.value.filter((x) => x.id !== t.id)
        const row = list.value.find((e) => e.id === bindForm.environmentId)
        if (row) {
          row._targets = bindTargets.value
        }
      }
    }
  })
}

onMounted(loadData)
</script>
