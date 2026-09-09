<template>
  <div>
    <CustomTable
      is-show-tools
      :active-page="activePage"
      table-name="deploy-records"
      :empty-description="'暂无部署记录'"
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
          <n-select
            v-model:value="queryEnvironment"
            :options="envOptions"
            placeholder="全部环境"
            clearable
            style="width: 180px"
            @update:value="loadData"
          />
          <n-button type="primary" :loading="loading" @click="loadData">查询</n-button>
        </n-space>
      </template>
      <template #tableBodyCell="{ column, text, record }">
        <template v-if="column.dataIndex === 'status'">
          <n-tag :type="statusType(text)">{{ statusText(text) }}</n-tag>
        </template>
        <template v-else-if="column.dataIndex === 'remark'">
          <n-ellipsis style="max-width: 200px" :tooltip="true">{{ text || '-' }}</n-ellipsis>
        </template>
        <template v-else-if="column.dataIndex === 'createTimeMillis'">
          {{ text ? parseTime(text) : '' }}
        </template>
        <template v-else-if="column.dataIndex === 'operation'">
          <n-space v-if="record.status === 3">
            <n-button type="primary" size="small" @click="approve(record, true)">批准</n-button>
            <n-button danger size="small" @click="openReject(record)">拒绝</n-button>
          </n-space>
          <span v-else>-</span>
        </template>
        <template v-else>
          {{ text }}
        </template>
      </template>
    </CustomTable>

    <CustomModal
      v-model:open="rejectVisible"
      title="拒绝部署"
      :mask-closable="false"
      :confirm-loading="rejectLoading"
      @ok="doReject"
    >
      <n-form label-width="100px">
        <n-form-item label="拒绝原因" required>
          <n-input v-model:value="rejectRemark" type="textarea" placeholder="请填写拒绝原因（必填）" />
        </n-form-item>
      </n-form>
    </CustomModal>
  </div>
</template>

<script lang="ts" setup>
import { ref, onMounted, computed, reactive } from 'vue'
import { useRoute } from 'vue-router'
import { COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY, parseTime } from '@/utils/const'
import { listDeployRecords, listEnvironments, approveDeploy } from '@/api/environment'

const route = useRoute()

const columns = [
  { title: '版本', key: 'version' },
  { title: '环境', key: 'environment' },
  { title: '方式', key: 'mode' },
  { title: '操作者', key: 'operator' },
  { title: '状态', key: 'status' },
  { title: '备注', key: 'remark' },
  { title: '部署时间', key: 'createTimeMillis' },
  { title: '操作', key: 'operation', width: 140 }
]

const listQuery = reactive({ ...PAGE_DEFAULT_LIST_QUERY })
const list = ref<any[]>([])
const loading = ref(false)
const activePage = computed(() => route.path === '/deploy/records')
const queryEnvironment = ref(null)
const environments = ref<any[]>([])
const rejectVisible = ref(false)
const rejectLoading = ref(false)
const rejectRecord = ref<any>(null)
const rejectRemark = ref('')

const envOptions = computed(() => environments.value.map((e) => ({ label: e.name, value: e.name })))

// 后端返回全量数组：前端本地分页（total 联动，避免伪分页）
const pagination = computed(() => {
  const p = COMPUTED_PAGINATION(listQuery)
  p.total = list.value.length
  p.itemCount = list.value.length
  return p
})

// 0 成功 1 失败 2 进行中 3 待审批 4 已拒绝
const statusText = (s) => ({ 0: '成功', 1: '失败', 2: '进行中', 3: '待审批', 4: '已拒绝' })[s] || s
const statusType = (s) => ({ 0: 'success', 1: 'error', 2: 'info', 3: 'warning', 4: 'default' })[s] || 'default'

const loadData = async () => {
  loading.value = true
  try {
    const res: any = await listDeployRecords({ environment: queryEnvironment.value || '' })
    if (res.code === 200) {
      list.value = res.data || []
    }
  } finally {
    loading.value = false
  }
}

const loadEnvironments = async () => {
  const res: any = await listEnvironments({})
  if (res.code === 200) {
    environments.value = res.data || []
  }
}

const approve = (record, pass: boolean) => {
  $confirm({
    title: `确认批准版本 ${record.version} 部署到 ${record.environment}？`,
    onOk: async () => {
      const res: any = await approveDeploy({ recordId: record.id, approve: pass })
      if (res.code === 200) {
        $message.success(res.msg)
        loadData()
      }
    }
  })
}

const openReject = (record) => {
  rejectRecord.value = record
  rejectRemark.value = ''
  rejectVisible.value = true
}

const doReject = async () => {
  if (!rejectRemark.value.trim()) {
    $message.warning('请填写拒绝原因')
    return
  }
  rejectLoading.value = true
  try {
    const res: any = await approveDeploy({ recordId: rejectRecord.value.id, approve: false, remark: rejectRemark.value.trim() })
    if (res.code === 200) {
      $message.success(res.msg)
      rejectVisible.value = false
      loadData()
    }
  } finally {
    rejectLoading.value = false
  }
}

onMounted(() => {
  loadEnvironments()
  loadData()
})
</script>
