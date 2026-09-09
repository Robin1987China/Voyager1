<template>
  <div>
    <CustomTable
      is-show-tools
      :active-page="activePage"
      table-name="deploy-records"
      :empty-description="$t('i18n_9203626614')"
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
            :placeholder="$t('i18n_2ff449a4ea')"
            clearable
            style="width: 180px"
            @update:value="onEnvChange"
          />
          <n-button type="primary" :loading="loading" @click="loadData">{{ $t('i18n_bee912d79e') }}</n-button>
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
            <n-button type="primary" size="small" @click="approve(record, true)">{{ $t('i18n_7a712b9a45') }}</n-button>
            <n-button danger size="small" @click="openReject(record)">{{ $t('i18n_7173f80900') }}</n-button>
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
      :title="$t('i18n_8e7497e2de')"
      :mask-closable="false"
      :confirm-loading="rejectLoading"
      @ok="doReject"
    >
      <n-form label-width="100px">
        <n-form-item :label="$t('i18n_f48f949c1e')" required>
          <n-input v-model:value="rejectRemark" type="textarea" :placeholder="$t('i18n_5e7688eca1')" />
        </n-form-item>
      </n-form>
    </CustomModal>
  </div>
</template>

<script lang="ts" setup>
import { ref, onMounted, computed, reactive } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY, parseTime } from '@/utils/const'
import { listDeployRecords, listEnvironments, approveDeploy } from '@/api/environment'

const { t: $t } = useI18n()
const route = useRoute()

const columns = [
  { title: $t('i18n_fe2df04a16'), key: 'version' },
  { title: $t('i18n_fa405f5965'), key: 'environment' },
  { title: $t('i18n_7220e4d5f9'), key: 'mode' },
  { title: $t('i18n_6b0bc6432d'), key: 'operator' },
  { title: $t('i18n_3fea7ca76c'), key: 'status' },
  { title: $t('i18n_2432b57515'), key: 'remark' },
  { title: $t('i18n_66d588da42'), key: 'createTimeMillis' },
  { title: $t('i18n_2b6bc0f293'), key: 'operation', width: 140 }
]

const listQuery = reactive({ ...PAGE_DEFAULT_LIST_QUERY })
const list = ref<any[]>([])
const loading = ref(false)
const activePage = computed(() => route.path === '/deploy/records')
const queryEnvironment = ref(localStorage.getItem('deploy-records-env-filter') || null)
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
const statusText = (s) =>
  ({ 0: $t('i18n_330363dfc5'), 1: $t('i18n_acd5cb847a'), 2: $t('i18n_fb852fc6cc'), 3: $t('i18n_b0bf01a4a8'), 4: $t('i18n_81233d755c') })[s] || s
const statusType = (s) => ({ 0: 'success', 1: 'error', 2: 'info', 3: 'warning', 4: 'default' })[s] || 'default'

const onEnvChange = (value) => {
  localStorage.setItem('deploy-records-env-filter', value || '')
  loadData()
}

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
    title: $t('i18n_5782962b8a', { version: record.version, environment: record.environment }),
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
    $message.warning($t('i18n_0f9381e8a9'))
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
