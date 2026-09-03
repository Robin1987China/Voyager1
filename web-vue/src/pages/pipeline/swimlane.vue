<template>
  <div>
    <n-card size="small" title="版本晋升泳道">
      <n-data-table
        :data="versions"
        :columns="columns"
        :pagination="false"
        size="small"
        :row-key="(row) => row.id"
        bordered
      />
    </n-card>
  </div>
</template>

<script lang="ts" setup>
import { ref, computed, onMounted, h } from 'vue'
import { NTag, NButton, NSpace, NPopover, NDataTable } from 'naive-ui'
import { getVersionList, submitVersion, returnVersion, releaseVersion } from '@/api/pipeline'
import { listEnvironments, deployVersion, listDeployRecords } from '@/api/environment'
import { parseTime } from '@/utils/const'

const versions = ref<any[]>([])
const environments = ref<any[]>([])
const envVersionMap = ref<Record<string, Record<string, string>>>({})

const statusText = (s) => ({ 0: '开发中', 1: '已提测', 2: '已发布', 3: '已打回' })[s] || s
const statusColor = (s) => ({ 0: 'blue', 1: 'orange', 2: 'green', 3: 'red' })[s] || 'default'

const envVersion = (version, env) => {
  const map = envVersionMap.value[version.id]
  return map ? map[env] : ''
}

const load = async () => {
  const [envRes, verRes] = await Promise.all([listEnvironments({}), getVersionList({ page: 1, limit: 50 })])
  environments.value = envRes.data || []
  versions.value = verRes.data || []
  // 并发加载每个版本在各环境的部署状态
  await Promise.all(
    versions.value.map(async (v) => {
      const recRes: any = await listDeployRecords({ versionId: v.id })
      v._records = recRes.data || []
      const map: Record<string, string> = {}
      for (const r of v._records) {
        if (r.status === 0) map[r.environment] = r.version
      }
      envVersionMap.value[v.id] = map
    })
  )
}

const deploy = async (record, env) => {
  const res: any = await deployVersion({ versionId: record.id, environment: env })
  if (res.code === 200) {
    $message.success(`已部署到 ${env}`)
    load()
  }
}

const action = async (record, act) => {
  const api = act === 'submit' ? submitVersion : act === 'return' ? returnVersion : releaseVersion
  const res: any = await api({ id: record.id, remark: '泳道操作' })
  if (res.code === 200) {
    $message.success(res.msg)
    load()
  }
}

const recordColumns = [
  { title: '环境', key: 'environment', width: 60 },
  { title: '方式', key: 'mode', width: 60 },
  {
    title: '状态',
    key: 'status',
    width: 60,
    render: (row) => ({ 0: '成功', 1: '失败', 2: '进行中' })[row.status] || row.status
  },
  {
    title: '时间',
    key: 'createTimeMillis',
    width: 100,
    render: (row) => (row.createTimeMillis ? parseTime(row.createTimeMillis) : '')
  }
]

const columns = computed(() => [
  { title: '版本', key: 'version', width: 120, fixed: 'left', render: (row) => h('b', null, row.version) },
  {
    title: '状态',
    key: 'status',
    width: 90,
    fixed: 'left',
    render: (row) => h(NTag, { color: statusColor(row.status) }, { default: () => statusText(row.status) })
  },
  ...environments.value.map((env) => ({
    title: env.name,
    key: `env_${env.id}`,
    width: 160,
    render: (row) => {
      const ver = envVersion(row, env.name)
      const cellStyle = { display: 'flex', flexDirection: 'column', gap: '4px', alignItems: 'flex-start' }
      const actionsStyle = { display: 'flex', gap: '4px' }
      if (ver) {
        return h('div', { style: cellStyle }, [
          h(NTag, { color: 'green' }, { default: () => ver }),
          env.name !== 'dev'
            ? h('div', { style: actionsStyle }, [
                h(NButton, { size: 'small', onClick: () => deploy(row, env.name) }, { default: () => '部署' })
              ])
            : null
        ])
      }
      return h('div', { style: cellStyle }, [
        h(NButton, { size: 'small', onClick: () => deploy(row, env.name) }, { default: () => `部署到${env.name}` })
      ])
    }
  })),
  {
    title: '操作',
    key: 'actions',
    width: 180,
    fixed: 'right',
    render: (row) => {
      const buttons = []
      if (row.status === 0 || row.status === 3) {
        buttons.push(
          h(
            NButton,
            { size: 'small', type: 'primary', onClick: () => action(row, 'submit') },
            { default: () => '提测' }
          )
        )
      }
      if (row.status === 1) {
        buttons.push(
          h(NButton, { size: 'small', danger: true, onClick: () => action(row, 'return') }, { default: () => '打回' })
        )
      }
      if (row.status === 1) {
        buttons.push(
          h(
            NButton,
            { size: 'small', type: 'primary', onClick: () => action(row, 'release') },
            { default: () => '发布' }
          )
        )
      }
      buttons.push(
        h(
          NPopover,
          { placement: 'left' },
          {
            trigger: () => h(NButton, { size: 'small' }, { default: () => '部署记录' }),
            default: () =>
              h(NDataTable, { data: row._records || [], columns: recordColumns, pagination: false, size: 'small' })
          }
        )
      )
      return h(NSpace, null, { default: () => buttons })
    }
  }
])

onMounted(load)
</script>

<style scoped>
.env-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
  align-items: flex-start;
}
.env-actions {
  display: flex;
  gap: 4px;
}
</style>
