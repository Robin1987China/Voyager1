<template>
  <div>
    <CustomTable
      is-show-tools
      :active-page="activePage"
      table-name="application-list"
      :empty-description="$t('i18n_3534c3d1c4')"
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
          <n-button type="primary" @click="openCreate">{{ $t('i18n_6638cae28f') }}</n-button>
        </n-space>
      </template>
      <template #tableBodyCell="{ column, text, record }">
        <template v-if="column.dataIndex === 'repositoryId'">
          {{ repoName(record.repositoryId) }}
        </template>
        <template v-else-if="column.dataIndex === 'buildId'">
          {{ buildName(record.buildId) }}
        </template>
        <template v-else-if="column.dataIndex === 'operation'">
          <n-space>
            <n-button size="small" type="primary" @click="goDetail(record)">{{ $t('i18n_f26225bde6') }}</n-button>
            <n-button size="small" @click="openEdit(record)">{{ $t('i18n_95b351c862') }}</n-button>
            <n-button size="small" danger @click="doDel(record)">{{ $t('i18n_2f4aaddde3') }}</n-button>
          </n-space>
        </template>
        <template v-else>
          {{ text || '-' }}
        </template>
      </template>
    </CustomTable>

    <CustomModal
      v-model:open="editVisible"
      :title="editForm.id ? $t('i18n_396fb46d83') : $t('i18n_6638cae28f')"
      :mask-closable="false"
      :confirm-loading="saveLoading"
      @ok="saveEdit"
    >
      <n-form label-width="100px">
        <n-form-item :label="$t('i18n_ed16f8c39d')" required>
          <n-input v-model:value="editForm.name" :placeholder="$t('i18n_24bdde3ef6')" />
        </n-form-item>
        <n-form-item :label="$t('i18n_c30f113cff')" required>
          <n-select
            v-model:value="editForm.repositoryId"
            :options="repoOptions"
            :placeholder="$t('i18n_03b06f2350')"
            filterable
          />
        </n-form-item>
        <n-form-item :label="$t('i18n_5e4a086d54')" required>
          <n-select
            v-model:value="editForm.buildId"
            :options="buildOptions"
            :placeholder="$t('i18n_097665acde')"
            filterable
          />
        </n-form-item>
        <n-form-item :label="$t('i18n_2432b57515')">
          <n-input v-model:value="editForm.remark" type="textarea" :placeholder="$t('i18n_2432b57515')" />
        </n-form-item>
      </n-form>
    </CustomModal>
  </div>
</template>

<script lang="ts" setup>
import { ref, onMounted, reactive, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY } from '@/utils/const'
import { listApplications, saveApplication, deleteApplication } from '@/api/application'
import { getRepositoryList } from '@/api/repository'
import { getBuildList } from '@/api/build-info'

const { t: $t } = useI18n()
const route = useRoute()
const router = useRouter()

const columns = [
  { title: $t('i18n_ed16f8c39d'), key: 'name' },
  { title: $t('i18n_c30f113cff'), key: 'repositoryId' },
  { title: $t('i18n_5e4a086d54'), key: 'buildId' },
  { title: $t('i18n_2432b57515'), key: 'remark' },
  { title: $t('i18n_2b6bc0f293'), key: 'operation', width: 200 }
]

const listQuery = reactive({ ...PAGE_DEFAULT_LIST_QUERY })
const list = ref<any[]>([])
const loading = ref(false)
const activePage = computed(() => route.path === '/application/list')
// 后端返回全量数组：前端本地分页（total 联动，避免伪分页）
const pagination = computed(() => {
  const p = COMPUTED_PAGINATION(listQuery)
  p.total = list.value.length
  p.itemCount = list.value.length
  return p
})

const editVisible = ref(false)
const saveLoading = ref(false)
const editForm = reactive({ id: '', name: '', repositoryId: null as string | null, buildId: null as string | null, remark: '' })

const repositories = ref<any[]>([])
const builds = ref<any[]>([])
const repoOptions = computed(() => repositories.value.map((r) => ({ label: r.name || r.id, value: r.id })))
const buildOptions = computed(() => builds.value.map((b) => ({ label: b.name || b.id, value: b.id })))
const repoName = (id) => repositories.value.find((r) => r.id === id)?.name || id || '-'
const buildName = (id) => builds.value.find((b) => b.id === id)?.name || id || '-'

const loadData = async () => {
  loading.value = true
  try {
    const res: any = await listApplications({})
    if (res.code === 200) {
      list.value = res.data || []
    }
  } finally {
    loading.value = false
  }
}

// 下拉数据源（仓库/构建配置），失败不阻塞页面主列表
const loadRefs = async () => {
  const [repoRes, buildRes] = await Promise.allSettled([
    getRepositoryList({ page: 1, limit: 1000 }),
    getBuildList({ page: 1, limit: 1000 })
  ])
  if (repoRes.status === 'fulfilled' && (repoRes.value as any).code === 200) {
    repositories.value = (repoRes.value as any).data?.result || (repoRes.value as any).data || []
  }
  if (buildRes.status === 'fulfilled' && (buildRes.value as any).code === 200) {
    builds.value = (buildRes.value as any).data?.result || (buildRes.value as any).data || []
  }
}

const openCreate = () => {
  editForm.id = ''
  editForm.name = ''
  editForm.repositoryId = null
  editForm.buildId = null
  editForm.remark = ''
  editVisible.value = true
}

const openEdit = (record) => {
  editForm.id = record.id
  editForm.name = record.name
  editForm.repositoryId = record.repositoryId || null
  editForm.buildId = record.buildId || null
  editForm.remark = record.remark || ''
  editVisible.value = true
}

const saveEdit = async () => {
  if (!editForm.name || !editForm.name.trim()) {
    $message.warning($t('i18n_86534fb10f'))
    return
  }
  if (!editForm.repositoryId) {
    $message.warning($t('i18n_3ee8a79010'))
    return
  }
  if (!editForm.buildId) {
    $message.warning($t('i18n_5315a7ac06'))
    return
  }
  saveLoading.value = true
  try {
    const res: any = await saveApplication({ ...editForm })
    if (res.code === 200) {
      $message.success($t('i18n_3b108349b9'))
      editVisible.value = false
      loadData()
    }
  } finally {
    saveLoading.value = false
  }
}

const doDel = (record) => {
  $confirm({
    title: $t('i18n_7ded4d98d8', { name: record.name }),
    onOk: async () => {
      const res: any = await deleteApplication({ id: record.id })
      if (res.code === 200) {
        $message.success($t('i18n_0007d170'))
        loadData()
      }
    }
  })
}

const goDetail = (record) => {
  router.push({ path: '/application/detail', query: { id: record.id } })
}

onMounted(() => {
  loadData()
  loadRefs()
})
</script>
