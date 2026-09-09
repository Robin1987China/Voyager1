<template>
  <div>
    <CustomTable
      is-show-tools
      :active-page="activePage"
      table-name="version-list"
      :empty-description="'暂无版本'"
      :columns="columns"
      :data="filteredList"
      :loading="loading"
      size="medium"
      row-key="id"
      :pagination="pagination"
      :scroll="{ x: 'max-content' }"
      @change="changePage"
      @refresh="loadData"
    >
      <template #title>
        <n-space wrap>
          <n-input
            v-model:value="keyword"
            class="search-input-item"
            placeholder="版本号（模糊匹配）"
            clearable
            @press-enter="changePage({ current: 1, pageSize: listQuery.limit })"
          />
          <n-button type="primary" :loading="loading" @click="changePage({ current: 1, pageSize: listQuery.limit })">查询</n-button>
          <n-button type="primary" @click="openCreate">创建版本</n-button>
        </n-space>
      </template>
      <template #tableBodyCell="{ column, text, record }">
        <template v-if="column.dataIndex === 'status'">
          <n-tag :color="statusColor(text)">{{ statusText(text) }}</n-tag>
        </template>
        <template v-else-if="column.dataIndex === 'artifactRef'">
          <n-ellipsis style="max-width: 240px" :tooltip="true">{{ text || '-' }}</n-ellipsis>
        </template>
        <template v-else-if="column.dataIndex === 'remark'">
          <n-ellipsis style="max-width: 160px" :tooltip="true">{{ text || '-' }}</n-ellipsis>
        </template>
        <template v-else-if="column.dataIndex === 'operation'">
          <n-space>
            <n-button v-if="record.status === 0" type="primary" size="small" @click="openAction(record, 'submit')"
              >提测</n-button
            >
            <n-button v-if="record.status === 1" danger size="small" @click="openAction(record, 'return')">打回</n-button>
            <n-button v-if="record.status === 1" type="primary" size="small" @click="openAction(record, 'release')"
              >发布</n-button
            >
            <n-dropdown
              v-if="record.status !== 3"
              trigger="click"
              :options="envOptions"
              @select="(key) => deployToEnv(record, key)"
            >
              <n-button type="info" size="small">部署到环境</n-button>
            </n-dropdown>
            <n-dropdown
              v-if="record.status === 1 || record.status === 2"
              trigger="click"
              :options="promoteOptions"
              @select="(key) => promote(record, key)"
            >
              <n-button type="success" size="small">晋升</n-button>
            </n-dropdown>
          </n-space>
        </template>
        <template v-else>
          {{ text }}
        </template>
      </template>
    </CustomTable>

    <CustomModal
      v-model:open="createVisible"
      title="从构建记录生成版本"
      :mask-closable="false"
      :confirm-loading="createLoading"
      @ok="createVersion"
    >
      <n-form label-width="100px">
        <n-form-item label="构建配置" required>
          <n-input v-model:value="createForm.buildId" placeholder="构建配置 id（应用）" />
        </n-form-item>
        <n-form-item label="构建记录" required>
          <n-input-number v-model:value="createForm.buildNumberId" placeholder="构建记录编号 #" style="width: 100%" />
        </n-form-item>
        <n-form-item label="备注">
          <n-input v-model:value="createForm.remark" placeholder="版本备注（可选）" />
        </n-form-item>
      </n-form>
    </CustomModal>

    <CustomModal
      v-model:open="actionVisible"
      :title="actionTitle"
      :mask-closable="false"
      :confirm-loading="actionLoading"
      @ok="doAction"
    >
      <n-form label-width="100px">
        <n-form-item :label="actionType === 'return' ? '打回原因' : '备注'" :required="actionType === 'return'">
          <n-input
            v-model:value="actionRemark"
            type="textarea"
            :placeholder="actionType === 'return' ? '请填写打回原因（必填，将记录到版本审计）' : '操作备注（可选）'"
          />
        </n-form-item>
      </n-form>
    </CustomModal>
  </div>
</template>

<script lang="ts" setup>
import { ref, onMounted, reactive, computed } from 'vue'
import { useRoute } from 'vue-router'
import { COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY, parseTime } from '@/utils/const'
import {
  getVersionList,
  createVersionFromBuild,
  submitVersion,
  returnVersion,
  releaseVersion
} from '@/api/pipeline'
import { listEnvironments, deployVersion, promoteVersion } from '@/api/environment'

const route = useRoute()

const columns = [
  { title: '版本号', key: 'version' },
  { title: '应用', key: 'buildId' },
  { title: '构建记录', key: 'buildNumberId' },
  { title: '状态', key: 'status' },
  { title: '产物', key: 'artifactRef' },
  { title: '备注', key: 'remark' },
  {
    title: '创建时间',
    key: 'createTimeMillis',
    render: (row) => (row['createTimeMillis'] ? parseTime(row['createTimeMillis']) : '')
  },
  { title: '操作', key: 'operation', width: 220 }
]
const listQuery = reactive({ ...PAGE_DEFAULT_LIST_QUERY })
const keyword = ref('')
const list = ref<any[]>([])
const loading = ref(false)
const activePage = computed(() => route.path === '/pipeline/version-list')
const createVisible = ref(false)
const createLoading = ref(false)
const createForm = reactive({ buildId: '', buildNumberId: null as number | null, remark: '' })
const environments = ref<any[]>([])
const actionVisible = ref(false)
const actionLoading = ref(false)
const actionType = ref<'submit' | 'return' | 'release'>('submit')
const actionRecord = ref<any>(null)
const actionRemark = ref('')

// 后端返回全量数组：版本号搜索与分页均在前端本地完成（total 随过滤结果联动，避免伪分页）
const filteredList = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  if (!kw) {
    return list.value
  }
  return list.value.filter((v) => (v.version || '').toLowerCase().includes(kw))
})
const pagination = computed(() => {
  const p = COMPUTED_PAGINATION(listQuery)
  p.total = filteredList.value.length
  p.itemCount = filteredList.value.length
  return p
})

const envOptions = computed(() =>
  environments.value.map((e) => ({ label: `${e.name}（${e.strategy || 'CI_CD'}）`, key: e.name }))
)
// 晋升目标：排除首个环境（首个环境无需晋升，直接部署）
const promoteOptions = computed(() =>
  environments.value.slice(1).map((e) => ({ label: `晋升到 ${e.name}`, key: e.name }))
)

const actionTitle = computed(() => {
  const name = actionType.value === 'submit' ? '提测' : actionType.value === 'return' ? '打回' : '发布'
  return `${name}版本 ${actionRecord.value?.version || ''}`
})

const statusText = (s) => ({ 0: '开发中', 1: '已提测', 2: '已发布', 3: '已打回' })[s] || s
const statusColor = (s) => ({ 0: 'blue', 1: 'orange', 2: 'green', 3: 'red' })[s] || 'default'

const loadEnvironments = async () => {
  const res: any = await listEnvironments({})
  if (res.code === 200) {
    environments.value = res.data || []
  }
}

const loadData = async () => {
  loading.value = true
  try {
    const res: any = await getVersionList({ buildId: '' })
    if (res.code === 200) {
      list.value = res.data || []
    }
  } finally {
    loading.value = false
  }
}

const changePage = (pagination, filters, sorter) => {
  listQuery.page = pagination.current
  listQuery.limit = pagination.pageSize
}

const openCreate = () => {
  createForm.buildId = ''
  createForm.buildNumberId = null
  createForm.remark = ''
  createVisible.value = true
}

const createVersion = async () => {
  if (!createForm.buildId || !createForm.buildId.trim()) {
    $message.warning('请填写构建配置 id')
    return
  }
  if (createForm.buildNumberId === null || createForm.buildNumberId === undefined) {
    $message.warning('请填写构建记录编号')
    return
  }
  createLoading.value = true
  try {
    const res: any = await createVersionFromBuild(createForm)
    if (res.code === 200) {
      $message.success('生成成功')
      createVisible.value = false
      loadData()
    }
  } finally {
    createLoading.value = false
  }
}

const openAction = (record, action) => {
  actionRecord.value = record
  actionType.value = action
  actionRemark.value = ''
  actionVisible.value = true
}

const doAction = async () => {
  if (actionType.value === 'return' && !actionRemark.value.trim()) {
    $message.warning('打回必须填写原因')
    return
  }
  actionLoading.value = true
  try {
    const api = actionType.value === 'submit' ? submitVersion : actionType.value === 'return' ? returnVersion : releaseVersion
    const res: any = await api({ id: actionRecord.value.id, remark: actionRemark.value.trim() || undefined })
    if (res.code === 200) {
      $message.success(res.msg)
      actionVisible.value = false
      loadData()
    }
  } finally {
    actionLoading.value = false
  }
}

const deployToEnv = (record, envName) => {
  $confirm({
    title: `确认部署版本 ${record.version} 到环境 ${envName}？`,
    onOk: async () => {
      const res: any = await deployVersion({ versionId: record.id, environment: envName })
      if (res.code === 200) {
        $message.success(res.msg)
        loadData()
      }
    }
  })
}

const promote = (record, envName) => {
  $confirm({
    title: `确认晋升版本 ${record.version} 到 ${envName}？（同一制品，不重新构建）`,
    onOk: async () => {
      const res: any = await promoteVersion({ versionId: record.id, environment: envName })
      if (res.code === 200) {
        $message.success(res.msg)
        loadData()
      }
    }
  })
}

onMounted(() => {
  loadData()
  loadEnvironments()
})
</script>
