<template>
  <div>
    <CustomTable
      is-show-tools
      :active-page="activePage"
      table-name="version-list"
      :empty-description="$t('i18n_4f31e0e575')"
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
            :placeholder="$t('i18n_9beff70da6')"
            clearable
            @press-enter="changePage({ current: 1, pageSize: listQuery.limit })"
          />
          <n-button type="primary" :loading="loading" @click="changePage({ current: 1, pageSize: listQuery.limit })">{{ $t('i18n_bee912d79e') }}</n-button>
          <n-button type="primary" @click="openCreate">{{ $t('i18n_305445c8f0') }}</n-button>
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
              >{{ $t('i18n_e6a92d13a1') }}</n-button
            >
            <n-button v-if="record.status === 1" danger size="small" @click="openAction(record, 'return')">{{ $t('i18n_9a7b15dccc') }}</n-button>
            <n-button v-if="record.status === 1" type="primary" size="small" @click="openAction(record, 'release')"
              >{{ $t('i18n_83611abd5f') }}</n-button
            >
            <n-dropdown
              v-if="record.status !== 3"
              trigger="click"
              :options="envOptions"
              @select="(key) => deployToEnv(record, key)"
            >
              <n-button type="info" size="small">{{ $t('i18n_739426ab8c') }}</n-button>
            </n-dropdown>
            <n-dropdown
              v-if="record.status === 1 || record.status === 2"
              trigger="click"
              :options="promoteOptions"
              @select="(key) => promote(record, key)"
            >
              <n-button type="success" size="small">{{ $t('i18n_e5cab0a893') }}</n-button>
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
      :title="$t('i18n_8bc9b8680a')"
      :mask-closable="false"
      :confirm-loading="createLoading"
      @ok="createVersion"
    >
      <n-form label-width="100px">
        <n-form-item :label="$t('i18n_5e4a086d54')" required>
          <n-input v-model:value="createForm.buildId" :placeholder="$t('i18n_4b9a47f04c')" />
        </n-form-item>
        <n-form-item :label="$t('i18n_60af0e54e5')" required>
          <n-input-number v-model:value="createForm.buildNumberId" :placeholder="$t('i18n_c501573294')" style="width: 100%" />
        </n-form-item>
        <n-form-item :label="$t('i18n_2432b57515')">
          <n-input v-model:value="createForm.remark" :placeholder="$t('i18n_2cd6e479ce')" />
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
        <n-form-item :label="actionType === 'return' ? $t('i18n_181eda4b58') : $t('i18n_2432b57515')" :required="actionType === 'return'">
          <n-input
            v-model:value="actionRemark"
            type="textarea"
            :placeholder="actionType === 'return' ? $t('i18n_ec621ddf56') : $t('i18n_8acc54f2ef')"
          />
        </n-form-item>
      </n-form>
    </CustomModal>
  </div>
</template>

<script lang="ts" setup>
import { ref, onMounted, reactive, computed } from 'vue'
import { useI18n } from 'vue-i18n'
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

const { t: $t } = useI18n()

const route = useRoute()

const columns = [
  { title: $t('i18n_d0b2958432'), key: 'version' },
  { title: $t('i18n_5b0520a9bf'), key: 'buildId' },
  { title: $t('i18n_60af0e54e5'), key: 'buildNumberId' },
  { title: $t('i18n_3fea7ca76c'), key: 'status' },
  { title: $t('i18n_7dfcab648d'), key: 'artifactRef' },
  { title: $t('i18n_2432b57515'), key: 'remark' },
  {
    title: $t('i18n_eca37cb072'),
    key: 'createTimeMillis',
    render: (row) => (row['createTimeMillis'] ? parseTime(row['createTimeMillis']) : '')
  },
  { title: $t('i18n_2b6bc0f293'), key: 'operation', width: 220 }
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
  environments.value.slice(1).map((e) => ({ label: $t('i18n_64aecd297d', { name: e.name }), key: e.name }))
)

const actionTitle = computed(() => {
  const name = actionType.value === 'submit' ? $t('i18n_e6a92d13a1') : actionType.value === 'return' ? $t('i18n_9a7b15dccc') : $t('i18n_83611abd5f')
  return $t('i18n_a201e18bbc', { name, version: actionRecord.value?.version || '' })
})

const statusText = (s) => ({ 0: $t('i18n_29dd651c2f'), 1: $t('i18n_2b436a16fa'), 2: $t('i18n_dca0c13b83'), 3: $t('i18n_72649d75af') })[s] || s
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
    $message.warning($t('i18n_e9b9cb8c24'))
    return
  }
  if (createForm.buildNumberId === null || createForm.buildNumberId === undefined) {
    $message.warning($t('i18n_a208489bf7'))
    return
  }
  createLoading.value = true
  try {
    const res: any = await createVersionFromBuild(createForm)
    if (res.code === 200) {
      $message.success($t('i18n_b6c4a445a2'))
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
    $message.warning($t('i18n_c82573fafe'))
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
    title: $t('i18n_7786fef335', { version: record.version, envName }),
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
    title: $t('i18n_91297526a1', { version: record.version, envName }),
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
