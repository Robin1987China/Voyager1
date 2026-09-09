<template>
  <div>
    <CustomTable
      is-show-tools
      :active-page="activePage"
      table-name="application-list"
      :empty-description="'暂无应用'"
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
          <n-button type="primary" @click="openCreate">新建应用</n-button>
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
            <n-button size="small" type="primary" @click="goDetail(record)">详情</n-button>
            <n-button size="small" @click="openEdit(record)">编辑</n-button>
            <n-button size="small" danger @click="doDel(record)">删除</n-button>
          </n-space>
        </template>
        <template v-else>
          {{ text || '-' }}
        </template>
      </template>
    </CustomTable>

    <CustomModal
      v-model:open="editVisible"
      :title="editForm.id ? '编辑应用' : '新建应用'"
      :mask-closable="false"
      :confirm-loading="saveLoading"
      @ok="saveEdit"
    >
      <n-form label-width="100px">
        <n-form-item label="应用名" required>
          <n-input v-model:value="editForm.name" placeholder="如 order-svc" />
        </n-form-item>
        <n-form-item label="代码仓库" required>
          <n-select
            v-model:value="editForm.repositoryId"
            :options="repoOptions"
            placeholder="选择代码仓库"
            filterable
          />
        </n-form-item>
        <n-form-item label="构建配置" required>
          <n-select
            v-model:value="editForm.buildId"
            :options="buildOptions"
            placeholder="选择构建配置"
            filterable
          />
        </n-form-item>
        <n-form-item label="备注">
          <n-input v-model:value="editForm.remark" type="textarea" placeholder="备注" />
        </n-form-item>
      </n-form>
    </CustomModal>
  </div>
</template>

<script lang="ts" setup>
import { ref, onMounted, reactive, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY } from '@/utils/const'
import { listApplications, saveApplication, deleteApplication } from '@/api/application'
import { getRepositoryList } from '@/api/repository'
import { getBuildList } from '@/api/build-info'

const route = useRoute()
const router = useRouter()

const columns = [
  { title: '应用名', key: 'name' },
  { title: '代码仓库', key: 'repositoryId' },
  { title: '构建配置', key: 'buildId' },
  { title: '备注', key: 'remark' },
  { title: '操作', key: 'operation', width: 200 }
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
    $message.warning('请填写应用名')
    return
  }
  if (!editForm.repositoryId) {
    $message.warning('请选择代码仓库')
    return
  }
  if (!editForm.buildId) {
    $message.warning('请选择构建配置')
    return
  }
  saveLoading.value = true
  try {
    const res: any = await saveApplication({ ...editForm })
    if (res.code === 200) {
      $message.success('保存成功')
      editVisible.value = false
      loadData()
    }
  } finally {
    saveLoading.value = false
  }
}

const doDel = (record) => {
  $confirm({
    title: `确认删除应用 ${record.name}？仅删除应用定义，不影响构建/版本/部署记录`,
    onOk: async () => {
      const res: any = await deleteApplication({ id: record.id })
      if (res.code === 200) {
        $message.success('删除成功')
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
