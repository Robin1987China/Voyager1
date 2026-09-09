<template>
  <div>
    <CustomTable
      is-show-tools
      :active-page="activePage"
      table-name="environment-list"
      :empty-description="'暂无环境'"
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
          <n-button type="primary" :loading="loading" @click="loadData">查询</n-button>
        </n-space>
      </template>
      <template #tableBodyCell="{ column, text, record }">
        <template v-if="column.dataIndex === 'strategy'">
          <n-tag :type="text === 'CI_CD' ? 'success' : 'warning'">{{ text === 'CI_CD' ? 'CI/CD' : '仅部署' }}</n-tag>
        </template>
        <template v-else-if="column.dataIndex === 'approvalRequired'">
          <n-tag :type="text ? 'error' : 'default'">{{ text ? '需审批' : '不需' }}</n-tag>
        </template>
        <template v-else-if="column.dataIndex === 'targets'">
          <n-space>
            <n-tag v-for="t in (record._targets || [])" :key="t.id" size="small">
              {{ t.targetType }}:{{ t.targetId }}{{ t.projectId ? '/' + t.projectId : '' }}
            </n-tag>
            <n-text v-if="!(record._targets || []).length" depth="3">未绑定</n-text>
          </n-space>
        </template>
        <template v-else-if="column.dataIndex === 'operation'">
          <n-space>
            <n-button size="small" @click="openEdit(record)">编辑</n-button>
            <n-button size="small" type="primary" @click="openBind(record)">绑定目标</n-button>
          </n-space>
        </template>
        <template v-else>
          {{ text }}
        </template>
      </template>
    </CustomTable>

    <CustomModal
      v-model:open="editVisible"
      title="编辑环境"
      :mask-closable="false"
      :confirm-loading="editLoading"
      @ok="saveEdit"
    >
      <n-form label-width="120px">
        <n-form-item label="名称">
          <n-input v-model:value="editForm.name" disabled />
        </n-form-item>
        <n-form-item label="类型">
          <n-input v-model:value="editForm.type" placeholder="dev/test/prod" />
        </n-form-item>
        <n-form-item label="策略">
          <n-select v-model:value="editForm.strategy" :options="strategyOptions" />
        </n-form-item>
        <n-form-item label="部署需审批">
          <n-switch v-model:value="editForm.approvalRequired" />
        </n-form-item>
      </n-form>
    </CustomModal>

    <CustomModal
      v-model:open="bindVisible"
      title="绑定部署目标"
      :mask-closable="false"
      :confirm-loading="bindLoading"
      @ok="saveBind"
    >
      <n-form label-width="120px">
        <n-form-item label="目标类型">
          <n-select v-model:value="bindForm.targetType" :options="targetTypeOptions" />
        </n-form-item>
        <n-form-item label="节点 id" required>
          <n-input v-model:value="bindForm.targetId" placeholder="节点 id（节点列表页可复制）" />
        </n-form-item>
        <n-form-item label="项目 id" required>
          <n-input v-model:value="bindForm.projectId" placeholder="部署到该节点上的项目 id" />
        </n-form-item>
        <n-form-item label="已绑定目标">
          <n-space>
            <n-tag
              v-for="t in bindTargets"
              :key="t.id"
              closable
              @close="unbind(t)"
            >
              {{ t.targetType }}:{{ t.targetId }}{{ t.projectId ? '/' + t.projectId : '' }}
            </n-tag>
            <n-text v-if="!bindTargets.length" depth="3">暂未绑定</n-text>
          </n-space>
        </n-form-item>
      </n-form>
    </CustomModal>
  </div>
</template>

<script lang="ts" setup>
import { ref, onMounted, reactive, computed } from 'vue'
import { useRoute } from 'vue-router'
import { COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY } from '@/utils/const'
import {
  listEnvironments,
  saveEnvironment,
  bindEnvironmentTarget,
  unbindEnvironmentTarget,
  listEnvironmentTargets
} from '@/api/environment'

const route = useRoute()

const columns = [
  { title: '名称', key: 'name' },
  { title: '类型', key: 'type' },
  { title: '策略', key: 'strategy' },
  { title: '审批', key: 'approvalRequired' },
  { title: '部署目标', key: 'targets' },
  { title: '操作', key: 'operation', width: 200 }
]
const strategyOptions = [
  { label: 'CI/CD（可构建可部署）', value: 'CI_CD' },
  { label: '仅部署（CD）', value: 'CD_ONLY' }
]
// 目前部署链路仅支持节点目标（K8S/SSH 待后续迭代，后端同样会拒绝）
const targetTypeOptions = [{ label: '节点', value: 'NODE' }]

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
      $message.success('保存成功')
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
  // 打开时重新拉取，确保是最新绑定状态
  bindTargets.value = await fetchTargets(record.id)
}

const saveBind = async () => {
  if (!bindForm.targetId || !bindForm.targetId.trim()) {
    $message.warning('请填写节点 id')
    return
  }
  if (!bindForm.projectId || !bindForm.projectId.trim()) {
    $message.warning('请填写项目 id')
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
      $message.success('绑定成功')
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
    title: `确认解绑目标 ${t.targetType}:${t.targetId}？`,
    onOk: async () => {
      const res: any = await unbindEnvironmentTarget({ id: t.id })
      if (res.code === 200) {
        $message.success('解绑成功')
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
