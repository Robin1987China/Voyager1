<template>
  <div>
    <CustomTable
      is-show-tools
      :active-page="activePage"
      table-name="version-list"
      :empty-description="'暂无版本'"
      :columns="columns"
      :data="list"
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
            v-model:value="listQuery['%version%']"
            class="search-input-item"
            placeholder="版本号"
            @press-enter="loadData"
          />
          <n-button type="primary" :loading="loading" @click="loadData">查询</n-button>
          <n-button type="primary" @click="createVisible = true">创建版本</n-button>
        </n-space>
      </template>
      <template #tableBodyCell="{ column, text, record }">
        <template v-if="column.dataIndex === 'status'">
          <n-tag :color="statusColor(text)">{{ statusText(text) }}</n-tag>
        </template>
        <template v-else-if="column.dataIndex === 'operation'">
          <n-space>
            <n-button v-if="record.status === 0" type="primary" size="small" @click="doAction(record, 'submit')"
              >提测</n-button
            >
            <n-button v-if="record.status === 1" danger size="small" @click="doAction(record, 'return')">打回</n-button>
            <n-button v-if="record.status === 1" type="primary" size="small" @click="doAction(record, 'release')"
              >发布</n-button
            >
          </n-space>
        </template>
        <template v-else>
          {{ text }}
        </template>
      </template>
    </CustomTable>

    <CustomModal v-model:open="createVisible" title="创建版本" :mask-closable="false" @ok="createVersion">
      <n-form label-width="100px">
        <n-form-item label="应用">
          <n-input v-model:value="createForm.buildId" placeholder="构建配置 id（应用）" />
        </n-form-item>
        <n-form-item label="版本号">
          <n-input v-model:value="createForm.version" placeholder="如 v1.2.3" />
        </n-form-item>
        <n-form-item label="产物引用">
          <n-input v-model:value="createForm.artifactRef" placeholder="产物路径/标识" />
        </n-form-item>
      </n-form>
    </CustomModal>
  </div>
</template>

<script lang="ts" setup>
import { ref, onMounted, reactive, computed } from 'vue'
import { CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY, parseTime } from '@/utils/const'
import {
  getVersionList,
  createVersion as createVersionApi,
  submitVersion,
  returnVersion,
  releaseVersion
} from '@/api/pipeline'

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
  { title: '操作', key: 'operation', width: 200 }
]
const listQuery = reactive({ ...PAGE_DEFAULT_LIST_QUERY, '%version%': '' })
const list = ref<any[]>([])
const loading = ref(false)
const activePage = ref(1)
const pagination = computed(() => COMPUTED_PAGINATION(listQuery))
const createVisible = ref(false)
const createForm = reactive({ buildId: '', version: '', artifactRef: '' })

const statusText = (s) => ({ 0: '开发中', 1: '已提测', 2: '已发布', 3: '已打回' })[s] || s
const statusColor = (s) => ({ 0: 'blue', 1: 'orange', 2: 'green', 3: 'red' })[s] || 'default'

const loadData = async () => {
  loading.value = true
  try {
    const res: any = await getVersionList({ ...listQuery, buildId: listQuery.buildId || '' })
    if (res.code === 200) {
      list.value = res.data
    }
  } finally {
    loading.value = false
  }
}

const changePage = (pagination, filters, sorter) => {
  listQuery.page = pagination.current
  listQuery.limit = pagination.pageSize
  loadData()
}

const createVersion = async () => {
  const res: any = await createVersionApi(createForm)
  if (res.code === 200) {
    $message.success('创建成功')
    createVisible.value = false
    loadData()
  }
}

const doAction = (record, action) => {
  $confirm({
    title: `确认${action === 'submit' ? '提测' : action === 'return' ? '打回' : '发布'}版本 ${record.version}？`,
    onOk: async () => {
      const api = action === 'submit' ? submitVersion : action === 'return' ? returnVersion : releaseVersion
      const res: any = await api({ id: record.id, remark: '页面操作' })
      if (res.code === 200) {
        $message.success(res.msg)
        loadData()
      }
    }
  })
}

onMounted(loadData)
</script>
