<template>
  <div>
    <n-card size="small" :title="$t('i18n_ff7c6ad4')">
      <n-form layout="inline">
        <n-form-item :label="$t('i18n_d7ec2d3f')"
          ><n-input v-model:value="accForm.name" :placeholder="$t('i18n_9959c18d')"
        /></n-form-item>
        <n-form-item :label="$t('i18n_70aefc6a')">
          <n-select
            v-model:value="accForm.vendor"
            style="width: 130px"
            :options="[
              { label: $t('i18n_71abfd41'), value: 'aliyun' },
              { label: $t('i18n_95afe1c8'), value: 'tencent' },
              { label: $t('i18n_6b20b72d'), value: 'huawei' },
              { label: 'AWS', value: 'aws' },
              { label: 'Azure', value: 'azure' },
              { label: 'GCP', value: 'gcp' },
              { label: $t('i18n_2830a7a2'), value: 'volcengine' }
            ]"
          />
        </n-form-item>
        <n-form-item label="AccessKey"><n-input v-model:value="accForm.accessKey" style="width: 220px" /></n-form-item>
        <n-form-item label="SecretKey"
          ><n-input v-model:value="accForm.secretKey" type="password" style="width: 220px"
        /></n-form-item>
        <n-form-item :label="$t('i18n_5dac545f')"
          ><n-input
            v-model:value="accForm.extraKey"
            type="password"
            style="width: 200px"
            placeholder="Azure tenantId / GCP projectId"
        /></n-form-item>
        <n-form-item :label="$t('i18n_d3ce40d8')"
          ><n-input v-model:value="accForm.region" style="width: 140px" placeholder="如 cn-hangzhou"
        /></n-form-item>
        <n-form-item
          ><n-button type="primary" @click="saveAccount">{{ $t('i18n_a85f0f3d') }}</n-button></n-form-item
        >
      </n-form>
      <n-data-table
        :data="accounts"
        :columns="accountColumns"
        :pagination="false"
        size="small"
        :row-key="(row) => row.id"
        style="margin-top: 12px"
      />
    </n-card>

    <n-card size="small" :title="$t('i18n_2f6824d7')" style="margin-top: 12px">
      <n-form layout="inline">
        <n-form-item label="实例ID"><n-input v-model:value="instForm.instanceId" style="width: 160px" /></n-form-item>
        <n-form-item :label="$t('i18n_d7ec2d3f')"
          ><n-input v-model:value="instForm.name" style="width: 120px"
        /></n-form-item>
        <n-form-item label="公网IP"><n-input v-model:value="instForm.publicIp" style="width: 140px" /></n-form-item>
        <n-form-item label="内网IP"><n-input v-model:value="instForm.privateIp" style="width: 140px" /></n-form-item>
        <n-form-item
          ><n-button type="primary" @click="saveInstance">{{ $t('i18n_153e9834') }}</n-button></n-form-item
        >
      </n-form>
      <n-data-table
        :data="instances"
        :columns="instanceColumns"
        :pagination="false"
        size="small"
        :row-key="(row) => row.id"
        style="margin-top: 12px"
        :scroll="{ x: 1400 }"
      />
    </n-card>

    <n-card size="small" :title="$t('i18n_20727f13')" style="margin-top: 12px">
      <n-form layout="inline">
        <n-form-item label="磁盘ID"
          ><n-input v-model:value="snapForm.diskId" style="width: 200px" placeholder="如 d-xxx / vol-xxx"
        /></n-form-item>
        <n-form-item :label="$t('i18n_953f2b80')"
          ><n-input v-model:value="snapForm.snapshotName" style="width: 160px"
        /></n-form-item>
        <n-form-item
          ><n-button type="primary" @click="createSnapshot">{{ $t('i18n_3eacefe7') }}</n-button></n-form-item
        >
      </n-form>
      <n-data-table
        :data="snapshots"
        :columns="snapshotColumns"
        :pagination="false"
        size="small"
        :row-key="(row) => row.snapshotId"
        style="margin-top: 12px"
      />
    </n-card>

    <n-card size="small" :title="$t('i18n_9226fe5d')" style="margin-top: 12px">
      <n-data-table
        :data="securityGroups"
        :pagination="false"
        size="small"
        :row-key="(row) => row.securityGroupId"
        :columns="[
          { title: '安全组ID', key: 'securityGroupId' },
          { title: $t('i18n_d7ec2d3f'), key: 'name' },
          { title: $t('i18n_3bdd08ad'), key: 'description' }
        ]"
      />
    </n-card>

    <n-card size="small" :title="$t('i18n_487d21e6')" style="margin-top: 12px">
      <n-data-table
        :data="scalingGroups"
        :columns="scalingGroupColumns"
        :pagination="false"
        size="small"
        :row-key="(row) => row.scalingGroupId"
      />
    </n-card>

    <CustomModal v-model:open="importVisible" title="导入为 SSH 机器" :mask-closable="false" @ok="doImport">
      <n-form label-width="90px">
        <n-form-item :label="$t('i18n_480c216f')">
          <n-input :value="importForm.name" disabled />
        </n-form-item>
        <n-form-item label="SSH 用户" required>
          <n-input v-model:value="importForm.sshUser" placeholder="root" />
        </n-form-item>
        <n-form-item label="SSH 端口">
          <n-input-number v-model:value="importForm.sshPort" :min="1" :max="65535" style="width: 200px" />
        </n-form-item>
        <n-form-item label="SSH 密码">
          <n-input v-model:value="importForm.password" type="password" placeholder="登录密码（或密钥）" />
        </n-form-item>
      </n-form>
    </CustomModal>

    <CustomModal v-model:open="resizeVisible" :title="$t('i18n_df932dd1')" :mask-closable="false" @ok="doResize">
      <n-form label-width="90px">
        <n-form-item :label="$t('i18n_480c216f')">
          <n-input :value="resizeForm.name" disabled />
        </n-form-item>
        <n-form-item :label="$t('i18n_cc13dfd4')">
          <n-input :value="resizeForm.instanceType" disabled />
        </n-form-item>
        <n-form-item :label="$t('i18n_ce27fe1f')" required>
          <n-input v-model:value="resizeForm.newInstanceType" placeholder="如 ecs.g7.2xlarge / t3.large" />
        </n-form-item>
      </n-form>
    </CustomModal>

    <CustomModal v-model:open="imageVisible" :title="$t('i18n_aa5c9b41')" :mask-closable="false" @ok="doImage">
      <n-form label-width="90px">
        <n-form-item :label="$t('i18n_480c216f')">
          <n-input :value="imageForm.name" disabled />
        </n-form-item>
        <n-form-item :label="$t('i18n_413f0e52')" required>
          <n-input v-model:value="imageForm.imageName" placeholder="如 web-prod-20260831" />
        </n-form-item>
      </n-form>
    </CustomModal>
  </div>
</template>

<script lang="ts" setup>
import { ref, reactive, onMounted, h } from 'vue'
import { NSpace, NButton } from 'naive-ui'
import { useI18n } from 'vue-i18n'
const { t } = useI18n()
import {
  saveCloudAccount,
  listCloudAccounts,
  listCloudInstances,
  saveCloudInstance,
  importCloudInstance,
  testCloudAccountConnectivity,
  syncCloudInstances,
  operateCloudInstance,
  resizeCloudInstance,
  createCloudSnapshot,
  listCloudSnapshots,
  deleteCloudSnapshot,
  listCloudSecurityGroups,
  createCloudImage,
  listCloudScalingGroups
} from '@/api/cloud'

const accForm = reactive({ name: '', vendor: 'aliyun', accessKey: '', secretKey: '', extraKey: '', region: '' })
const instForm = reactive({ instanceId: '', name: '', publicIp: '', privateIp: '' })
const accounts = ref<any[]>([])
const instances = ref<any[]>([])
const currentAccountId = ref('')
const syncing = ref(false)

const loadAccounts = async () => {
  const res: any = await listCloudAccounts()
  if (res.code === 200) accounts.value = res.data || []
}
const loadInstances = async () => {
  const res: any = await listCloudInstances({ accountId: currentAccountId.value })
  if (res.code === 200) instances.value = res.data || []
}
const saveAccount = async () => {
  const res: any = await saveCloudAccount(accForm)
  if (res.code === 200) {
    $message.success(t('i18n_95aea23c'))
    accForm.name = accForm.accessKey = accForm.secretKey = ''
    loadAccounts()
  } else {
    $message.error(res.msg)
  }
}
const testConnectivity = async (record) => {
  const res: any = await testCloudAccountConnectivity({ id: record.id })
  if (res.code === 200) {
    if (res.data) {
      $message.success(t('i18n_65d2951d'))
    } else {
      $message.error(res.msg || t('i18n_a886436b'))
    }
  } else {
    $message.error(res.msg)
  }
}
const syncInstances = async (record) => {
  syncing.value = true
  try {
    const res: any = await syncCloudInstances({ accountId: record.id })
    if (res.code === 200) {
      $message.success(t('i18n_705af821', { n: res.data }))
      currentAccountId.value = record.id
      loadInstances()
    } else {
      $message.error(res.msg)
    }
  } finally {
    syncing.value = false
  }
}
const selectAccount = (record) => {
  currentAccountId.value = record.id
  loadInstances()
  loadSnapshots()
  loadSecurityGroups()
  loadScalingGroups()
}
const saveInstance = async () => {
  if (!currentAccountId.value) {
    $message.warning(t('i18n_ec2f57bb'))
    return
  }
  const res: any = await saveCloudInstance({ ...instForm, accountId: currentAccountId.value })
  if (res.code === 200) {
    $message.success(t('i18n_f40e871e'))
    instForm.instanceId = instForm.name = instForm.publicIp = instForm.privateIp = ''
    loadInstances()
  } else {
    $message.error(res.msg)
  }
}
const operate = async (record, action) => {
  const res: any = await operateCloudInstance({ accountId: record.accountId, instanceId: record.instanceId, action })
  if (res.code === 200) {
    $message.success(res.msg)
    loadInstances()
  } else {
    $message.error(res.msg)
  }
}
const memG = (mb) => (mb ? `${Math.round(mb / 1024)}G` : '-')

const importForm = reactive({ id: '', name: '', sshUser: 'root', sshPort: 22, password: '' })
const importVisible = ref(false)

const openImport = (record) => {
  Object.assign(importForm, {
    id: record.id,
    name: record.name || record.instanceId,
    sshUser: 'root',
    sshPort: 22,
    password: ''
  })
  importVisible.value = true
}
const doImport = async () => {
  if (!importForm.sshUser) {
    $message.warning(t('i18n_17443889'))
    return
  }
  const res: any = await importCloudInstance({
    id: importForm.id,
    sshUser: importForm.sshUser,
    sshPort: importForm.sshPort,
    password: importForm.password
  })
  if (res.code === 200) {
    $message.success(res.msg)
    importVisible.value = false
    loadInstances()
  } else {
    $message.error(res.msg)
  }
}

const resizeForm = reactive({ id: '', accountId: '', name: '', instanceType: '', newInstanceType: '' })
const resizeVisible = ref(false)

const openResize = (record) => {
  Object.assign(resizeForm, {
    id: record.id,
    accountId: record.accountId,
    name: record.name || record.instanceId,
    instanceType: record.instanceType || '-',
    newInstanceType: ''
  })
  resizeVisible.value = true
}
const doResize = async () => {
  if (!resizeForm.newInstanceType) {
    $message.warning(t('i18n_2f1da134'))
    return
  }
  const res: any = await resizeCloudInstance({
    accountId: resizeForm.accountId,
    instanceId: resizeForm.instanceId,
    newInstanceType: resizeForm.newInstanceType
  })
  if (res.code === 200) {
    $message.success(res.msg)
    resizeVisible.value = false
    loadInstances()
  } else {
    $message.error(res.msg)
  }
}

const snapForm = reactive({ diskId: '', snapshotName: '' })
const snapshots = ref<any[]>([])

const loadSnapshots = async () => {
  if (!currentAccountId.value) return
  const res: any = await listCloudSnapshots({ accountId: currentAccountId.value })
  if (res.code === 200) snapshots.value = res.data || []
}
const createSnapshot = async () => {
  if (!currentAccountId.value) {
    $message.warning(t('i18n_ec2f57bb'))
    return
  }
  if (!snapForm.diskId) {
    $message.warning(t('i18n_d6a9c8fa'))
    return
  }
  const res: any = await createCloudSnapshot({
    accountId: currentAccountId.value,
    diskId: snapForm.diskId,
    snapshotName: snapForm.snapshotName
  })
  if (res.code === 200) {
    $message.success(res.msg)
    snapForm.diskId = snapForm.snapshotName = ''
    loadSnapshots()
  } else {
    $message.error(res.msg)
  }
}
const removeSnapshot = (record) => {
  $confirm({
    title: t('i18n_3f04bd3d'),
    content: `确定删除快照 ${record.snapshotId} 吗？`,
    okText: t('i18n_38cf16f2'),
    cancelText: t('i18n_625fb26b'),
    onOk: async () => {
      const res: any = await deleteCloudSnapshot({ accountId: currentAccountId.value, snapshotId: record.snapshotId })
      if (res.code === 200) {
        $message.success(res.msg)
        loadSnapshots()
      } else {
        $message.error(res.msg)
      }
    }
  })
}

const securityGroups = ref<any[]>([])

const loadSecurityGroups = async () => {
  if (!currentAccountId.value) return
  const res: any = await listCloudSecurityGroups({ accountId: currentAccountId.value })
  if (res.code === 200) securityGroups.value = res.data || []
}

const scalingGroups = ref<any[]>([])

const loadScalingGroups = async () => {
  if (!currentAccountId.value) return
  const res: any = await listCloudScalingGroups({ accountId: currentAccountId.value })
  if (res.code === 200) scalingGroups.value = res.data || []
}

const imageForm = reactive({ accountId: '', instanceId: '', name: '', imageName: '' })
const imageVisible = ref(false)

const openImage = (record) => {
  Object.assign(imageForm, {
    accountId: record.accountId,
    instanceId: record.instanceId,
    name: record.name || record.instanceId,
    imageName: ''
  })
  imageVisible.value = true
}
const doImage = async () => {
  if (!imageForm.imageName) {
    $message.warning(t('i18n_c236badc'))
    return
  }
  const res: any = await createCloudImage({
    accountId: imageForm.accountId,
    instanceId: imageForm.instanceId,
    imageName: imageForm.imageName
  })
  if (res.code === 200) {
    $message.success(res.msg)
    imageVisible.value = false
  } else {
    $message.error(res.msg)
  }
}

const accountColumns = [
  { title: t('i18n_d7ec2d3f'), key: 'name' },
  { title: t('i18n_70aefc6a'), key: 'vendor' },
  { title: t('i18n_d3ce40d8'), key: 'region' },
  {
    title: t('i18n_2b6bc0f2'),
    key: 'actions',
    width: 300,
    render: (row) =>
      h(NSpace, null, {
        default: () => [
          h(NButton, { size: 'small', onClick: () => testConnectivity(row) }, { default: () => t('i18n_85817aa7') }),
          h(
            NButton,
            { size: 'small', type: 'primary', loading: syncing.value, onClick: () => syncInstances(row) },
            { default: () => t('i18n_957895e9') }
          ),
          h(NButton, { size: 'small', onClick: () => selectAccount(row) }, { default: () => t('i18n_480c216f') })
        ]
      })
  }
]

const instanceColumns = [
  { title: '实例ID', key: 'instanceId', width: 120 },
  { title: t('i18n_d7ec2d3f'), key: 'name', width: 120 },
  { title: '公网IP', key: 'publicIp', width: 120 },
  { title: '内网IP', key: 'privateIp', width: 120 },
  { title: t('i18n_d3ce40d8'), key: 'regionId', width: 110 },
  { title: t('i18n_ea887bd0'), key: 'instanceType', width: 130 },
  { title: 'CPU/内存', key: 'cpuMem', width: 100, render: (row) => `${row.cpu || '-'}核/${memG(row.memory)}` },
  { title: t('i18n_3fea7ca7'), key: 'status', width: 90 },
  { title: t('i18n_22e888c2'), key: 'expireTime', width: 160 },
  {
    title: t('i18n_664b515d'),
    key: 'machineId',
    width: 80,
    render: (row) => (row.machineId ? t('i18n_ef1bcebd') : t('i18n_cd5f3771'))
  },
  {
    title: t('i18n_2b6bc0f2'),
    key: 'actions',
    width: 300,
    fixed: 'right',
    render: (row) =>
      h(NSpace, null, {
        default: () => [
          h(
            NButton,
            { size: 'small', disabled: row.status === 'Running', onClick: () => operate(row, 'start') },
            { default: () => t('i18n_8e54ddfe') }
          ),
          h(
            NButton,
            { size: 'small', disabled: row.status === 'Stopped', onClick: () => operate(row, 'stop') },
            { default: () => t('i18n_095e938e') }
          ),
          h(
            NButton,
            { size: 'small', disabled: row.status !== 'Running', onClick: () => operate(row, 'reboot') },
            { default: () => t('i18n_01b4e06f') }
          ),
          h(NButton, { size: 'small', onClick: () => openResize(row) }, { default: () => t('i18n_6f6f9488') }),
          h(
            NButton,
            { size: 'small', disabled: row.status !== 'Stopped', onClick: () => openImage(row) },
            { default: () => t('i18n_aa5c9b41') }
          ),
          h(
            NButton,
            { size: 'small', disabled: !!row.machineId, onClick: () => openImport(row) },
            { default: () => t('i18n_9c2dea94') }
          )
        ]
      })
  }
]

const snapshotColumns = [
  { title: '快照ID', key: 'snapshotId' },
  { title: t('i18n_d7ec2d3f'), key: 'name' },
  { title: t('i18n_3fea7ca7'), key: 'status', width: 90 },
  { title: t('i18n_4f5537dd'), key: 'diskId' },
  { title: t('i18n_eca37cb0'), key: 'createTime' },
  {
    title: t('i18n_2b6bc0f2'),
    key: 'actions',
    width: 100,
    render: (row) =>
      h(
        NButton,
        { size: 'small', danger: true, onClick: () => removeSnapshot(row) },
        { default: () => t('i18n_2f4aaddd') }
      )
  }
]

const scalingGroupColumns = [
  { title: '伸缩组ID', key: 'scalingGroupId' },
  { title: t('i18n_d7ec2d3f'), key: 'name' },
  { title: t('i18n_3fea7ca7'), key: 'status', width: 90 },
  {
    title: t('i18n_22c41129'),
    key: 'size',
    width: 140,
    render: (row) => `${row.minSize}/${row.maxSize}/${row.currentSize}`
  }
]

onMounted(loadAccounts)
</script>
