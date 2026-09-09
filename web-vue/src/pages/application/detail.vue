<template>
  <div>
    <!-- 应用头 -->
    <n-card :bordered="false" size="small" style="margin-bottom: 16px">
      <n-space align="center" justify="space-between">
        <n-space align="center">
          <n-button text @click="router.back()">{{ $t('i18n_5f411223ca') }}</n-button>
          <n-h3 style="margin: 0">{{ application.name || '-' }}</n-h3>
          <n-tag v-if="application.repositoryId" size="small">{{ $t('i18n_c270fc6fc9') }} {{ application.repositoryId }}</n-tag>
          <n-tag v-if="application.buildId" size="small" type="info">{{ $t('i18n_fcba60e773') }} {{ application.buildId }}</n-tag>
        </n-space>
        <n-text v-if="application.remark" depth="3">{{ application.remark }}</n-text>
      </n-space>
    </n-card>

    <!-- 环境泳道 -->
    <n-h4>{{ $t('i18n_789670da2f') }}</n-h4>
    <n-grid :cols="environments.length || 3" :x-gap="12">
      <n-gi v-for="env in environments" :key="env.name">
        <n-card size="small" :bordered="true">
          <template #header>
            <n-space align="center" justify="space-between">
              <n-text strong>{{ env.name }}</n-text>
              <n-space>
                <n-tag size="small" :type="env.strategy === 'CI_CD' ? 'success' : 'warning'">
                  {{ env.strategy === 'CI_CD' ? 'CI/CD' : $t('i18n_9c2e7a4b63') }}
                </n-tag>
                <n-tag v-if="env.approvalRequired" size="small" type="error">{{ $t('i18n_94f5cb1a18') }}</n-tag>
              </n-space>
            </n-space>
          </template>
          <template #default>
            <div v-if="env.current" style="margin-bottom: 12px">
              <div>
                <n-text strong>{{ env.current.version }}</n-text>
              </div>
              <n-space size="small" style="margin-top: 6px">
                <n-tag size="small" :type="deployStatusType(env.current.status)">
                  {{ deployStatusText(env.current.status) }}
                </n-tag>
                <n-text depth="3" style="font-size: 12px">{{ parseTime(env.current.createTimeMillis) }}</n-text>
              </n-space>
            </div>
            <n-text v-else depth="3">{{ $t('i18n_647f22235e') }}</n-text>
            <n-button
              block
              size="small"
              type="primary"
              style="margin-top: 12px"
              @click="openDeploy(env.name)"
            >
              {{ $t('i18n_acab386f69') }}
            </n-button>
          </template>
        </n-card>
      </n-gi>
    </n-grid>

    <!-- 构建历史 -->
    <n-h4 style="margin-top: 24px">{{ $t('i18n_a05c1667ca') }}</n-h4>
    <CustomTable
      :columns="historyColumns"
      :data="buildHistory"
      size="medium"
      row-key="id"
      :pagination="false"
      :empty-description="$t('i18n_819f5fd066')"
    >
      <template #tableBodyCell="{ column, text, record }">
        <template v-if="column.dataIndex === 'buildNumberId'">
          #{{ text }}
        </template>
        <template v-else-if="column.dataIndex === 'status'">
          <n-tag :type="buildStatusType(text)">{{ buildStatusText(text) }}</n-tag>
        </template>
        <template v-else-if="column.dataIndex === 'repositoryLastCommitId'">
          {{ (text || '').slice(0, 8) }}
        </template>
        <template v-else-if="column.dataIndex === 'startTime' || column.dataIndex === 'endTime'">
          {{ parseTime(text) }}
        </template>
        <template v-else-if="column.dataIndex === 'operation'">
          <n-dropdown trigger="click" :options="envDeployOptions" @select="(key) => deployBuildToEnv(record, key)">
            <n-button size="small" type="primary">{{ $t('i18n_739426ab8c') }}</n-button>
          </n-dropdown>
        </template>
        <template v-else>
          {{ text || '-' }}
        </template>
      </template>
    </CustomTable>

    <!-- 部署记录 -->
    <n-h4 style="margin-top: 24px">{{ $t('i18n_c4a370135e') }}</n-h4>
    <CustomTable
      :columns="recordColumns"
      :data="deploymentRecords"
      size="medium"
      row-key="id"
      :pagination="false"
      :empty-description="$t('i18n_9203626614')"
    >
      <template #tableBodyCell="{ column, text, record }">
        <template v-if="column.dataIndex === 'status'">
          <n-tag :type="deployStatusType(text)">{{ deployStatusText(text) }}</n-tag>
        </template>
        <template v-else-if="column.dataIndex === 'createTimeMillis'">
          {{ parseTime(text) }}
        </template>
        <template v-else>
          {{ text || '-' }}
        </template>
      </template>
    </CustomTable>

    <!-- 部署弹窗 -->
    <CustomModal v-model:open="deployVisible" :title="$t('i18n_de837b284e')" :mask-closable="false" @ok="confirmDeploy">
      <n-form label-width="100px">
        <n-form-item :label="$t('i18n_63098aef60')">
          <n-select v-model:value="deployForm.environment" :options="envDeployOptions" />
        </n-form-item>
        <n-form-item :label="$t('i18n_60af0e54e5')">
          <n-select
            v-model:value="deployForm.buildNumberId"
            :options="historyOptions"
            :placeholder="$t('i18n_06a449c1bf')"
          />
        </n-form-item>
        <n-text depth="3">{{ $t('i18n_83564e93ef') }}</n-text>
      </n-form>
    </CustomModal>
  </div>
</template>

<script lang="ts" setup>
import { ref, onMounted, reactive, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getApplicationDetail } from '@/api/application'
import { createVersionFromBuild } from '@/api/pipeline'
import { deployVersion } from '@/api/environment'
import { parseTime } from '@/utils/const'
import { useI18n } from 'vue-i18n'

const router = useRouter()
const route = useRoute()

const { t: $t } = useI18n()

const application = ref<any>({})
const environments = ref<any[]>([])
const buildHistory = ref<any[]>([])
const deploymentRecords = ref<any[]>([])

const historyColumns = [
  { title: $t('i18n_e5c31b80e6'), key: 'buildNumberId' },
  { title: $t('i18n_30110a24a6'), key: 'buildName' },
  { title: $t('i18n_3fea7ca76c'), key: 'status' },
  { title: $t('i18n_939d5345ad'), key: 'repositoryLastCommitId' },
  { title: $t('i18n_592c595891'), key: 'startTime' },
  { title: $t('i18n_f782779e8b'), key: 'endTime' },
  { title: $t('i18n_2432b57515'), key: 'buildRemark' },
  { title: $t('i18n_2b6bc0f293'), key: 'operation', width: 140 }
]
const recordColumns = [
  { title: $t('i18n_fe2df04a16'), key: 'version' },
  { title: $t('i18n_fa405f5965'), key: 'environment' },
  { title: $t('i18n_3fea7ca76c'), key: 'status' },
  { title: $t('i18n_7220e4d5f9'), key: 'mode' },
  { title: $t('i18n_6b0bc6432d'), key: 'operator' },
  { title: $t('i18n_19fcb9eb25'), key: 'createTimeMillis' }
]

const envDeployOptions = computed(() => environments.value.map((e) => ({ label: e.name, key: e.name })))
const historyOptions = computed(() =>
  buildHistory.value.map((h) => ({ label: `#${h.buildNumberId}（${buildStatusText(h.status)}）`, value: h.buildNumberId }))
)

const buildStatusText = (s) =>
  ({ 0: $t('i18n_d30b8b0e43'), 1: $t('i18n_32493aeef9'), 2: $t('i18n_330363dfc5'), 3: $t('i18n_acd5cb847a'), 4: $t('i18n_0baa0e3fc4'), 5: $t('i18n_2fff079bc7'), 6: $t('i18n_250688d7c9'), 7: $t('i18n_2111ccbb19'), 8: $t('i18n_e13531d775'), 9: $t('i18n_e5ac1d2029'), 10: $t('i18n_8160b4be4e') })[s] || s
const buildStatusType = (s) =>
  ({ 0: 'default', 1: 'info', 2: 'success', 3: 'error', 4: 'info', 5: 'success', 6: 'error', 7: 'default', 8: 'warning', 9: 'info', 10: 'warning' })[s] || 'default'

const deployStatusText = (s) => ({ 0: $t('i18n_330363dfc5'), 1: $t('i18n_acd5cb847a'), 2: $t('i18n_fb852fc6cc'), 3: $t('i18n_b0bf01a4a8'), 4: $t('i18n_81233d755c') })[s] || s
const deployStatusType = (s) => ({ 0: 'success', 1: 'error', 2: 'warning' })[s] || 'default'

const deployVisible = ref(false)
const deployForm = reactive({ environment: 'test', buildNumberId: null })

const loadData = async () => {
  const id = route.query.id as string
  if (!id) {
    return
  }
  const res: any = await getApplicationDetail({ id })
  if (res.code === 200) {
    application.value = res.data.application || {}
    environments.value = res.data.environments || []
    buildHistory.value = res.data.buildHistory || []
    deploymentRecords.value = res.data.deploymentRecords || []
  }
}

const openDeploy = (envName) => {
  deployForm.environment = envName
  deployForm.buildNumberId = null
  deployVisible.value = true
}

const confirmDeploy = () => {
  if (deployForm.buildNumberId == null) {
    $message.warning($t('i18n_07d7bf6e88'))
    return
  }
  // 弹窗路径与表格路径保持一致的二次确认；失败不关窗（由 doDeploy 成功后关闭）
  $confirm({
    title: $t('i18n_33174d968b', { slot: deployForm.buildNumberId, env: deployForm.environment }),
    onOk: async () => {
      const ok = await doDeploy(deployForm.buildNumberId, deployForm.environment)
      if (ok) {
        deployVisible.value = false
      }
    }
  })
}

const deployBuildToEnv = async (buildRecord, envName, silent = false) => {
  if (!application.value.buildId) {
    $message.warning($t('i18n_3f9f41a912'))
    return
  }
  if (!silent) {
    $confirm({
      title: $t('i18n_33174d968b', { slot: buildRecord.buildNumberId, env: envName }),
      onOk: () => doDeploy(buildRecord.buildNumberId, envName)
    })
    return
  }
  await doDeploy(buildRecord.buildNumberId, envName)
}

// 返回是否成功（调用方据此决定是否关闭弹窗）；失败提示由全局拦截器统一弹出
const doDeploy = async (buildNumberId, envName) => {
  const vres: any = await createVersionFromBuild({
    buildId: application.value.buildId,
    buildNumberId: buildNumberId
  })
  if (vres.code !== 200) {
    return false
  }
  const versionId = vres.data.id
  const dres: any = await deployVersion({ versionId, environment: envName })
  if (dres.code === 200) {
    $message.success($t('i18n_7b3191f9f7', { version: vres.data.version, env: envName }))
    loadData()
    return true
  }
  return false
}

onMounted(loadData)
</script>
