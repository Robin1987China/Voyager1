<template>
  <div>
    <!-- 应用头 -->
    <n-card :bordered="false" size="small" style="margin-bottom: 16px">
      <n-space align="center" justify="space-between">
        <n-space align="center">
          <n-button text @click="router.back()">返回</n-button>
          <n-h3 style="margin: 0">{{ application.name || '-' }}</n-h3>
          <n-tag v-if="application.repositoryId" size="small">仓库 {{ application.repositoryId }}</n-tag>
          <n-tag v-if="application.buildId" size="small" type="info">构建 {{ application.buildId }}</n-tag>
        </n-space>
        <n-text v-if="application.remark" depth="3">{{ application.remark }}</n-text>
      </n-space>
    </n-card>

    <!-- 环境泳道 -->
    <n-h4>环境泳道</n-h4>
    <n-grid :cols="environments.length || 3" :x-gap="12">
      <n-gi v-for="env in environments" :key="env.name">
        <n-card size="small" :bordered="true">
          <template #header>
            <n-space align="center" justify="space-between">
              <n-text strong>{{ env.name }}</n-text>
              <n-space>
                <n-tag size="small" :type="env.strategy === 'CI_CD' ? 'success' : 'warning'">
                  {{ env.strategy === 'CI_CD' ? 'CI/CD' : '仅部署' }}
                </n-tag>
                <n-tag v-if="env.approvalRequired" size="small" type="error">需审批</n-tag>
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
            <n-text v-else depth="3">未部署</n-text>
            <n-button
              block
              size="small"
              type="primary"
              style="margin-top: 12px"
              @click="openDeploy(env.name)"
            >
              部署到此环境
            </n-button>
          </template>
        </n-card>
      </n-gi>
    </n-grid>

    <!-- 构建历史 -->
    <n-h4 style="margin-top: 24px">构建历史</n-h4>
    <CustomTable
      :columns="historyColumns"
      :data="buildHistory"
      size="medium"
      row-key="id"
      :pagination="false"
      :empty-description="'暂无构建记录'"
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
            <n-button size="small" type="primary">部署到环境</n-button>
          </n-dropdown>
        </template>
        <template v-else>
          {{ text || '-' }}
        </template>
      </template>
    </CustomTable>

    <!-- 部署记录 -->
    <n-h4 style="margin-top: 24px">部署记录</n-h4>
    <CustomTable
      :columns="recordColumns"
      :data="deploymentRecords"
      size="medium"
      row-key="id"
      :pagination="false"
      :empty-description="'暂无部署记录'"
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
    <CustomModal v-model:open="deployVisible" title="从构建记录部署" :mask-closable="false" @ok="confirmDeploy">
      <n-form label-width="100px">
        <n-form-item label="目标环境">
          <n-select v-model:value="deployForm.environment" :options="envDeployOptions" />
        </n-form-item>
        <n-form-item label="构建记录">
          <n-select
            v-model:value="deployForm.buildNumberId"
            :options="historyOptions"
            placeholder="选择构建记录（自动生成版本）"
          />
        </n-form-item>
        <n-text depth="3">选择构建记录后，系统将从该构建自动生成版本并部署到目标环境（一次构建，多次部署）。</n-text>
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

const router = useRouter()
const route = useRoute()

const application = ref<any>({})
const environments = ref<any[]>([])
const buildHistory = ref<any[]>([])
const deploymentRecords = ref<any[]>([])

const historyColumns = [
  { title: '构建编号', key: 'buildNumberId' },
  { title: '构建名', key: 'buildName' },
  { title: '状态', key: 'status' },
  { title: '提交', key: 'repositoryLastCommitId' },
  { title: '开始时间', key: 'startTime' },
  { title: '结束时间', key: 'endTime' },
  { title: '备注', key: 'buildRemark' },
  { title: '操作', key: 'operation', width: 140 }
]
const recordColumns = [
  { title: '版本', key: 'version' },
  { title: '环境', key: 'environment' },
  { title: '状态', key: 'status' },
  { title: '方式', key: 'mode' },
  { title: '操作者', key: 'operator' },
  { title: '时间', key: 'createTimeMillis' }
]

const envDeployOptions = computed(() => environments.value.map((e) => ({ label: e.name, key: e.name })))
const historyOptions = computed(() =>
  buildHistory.value.map((h) => ({ label: `#${h.buildNumberId}（${buildStatusText(h.status)}）`, value: h.buildNumberId }))
)

const buildStatusText = (s) =>
  ({ 0: '未构建', 1: '构建中', 2: '成功', 3: '失败', 4: '发布中', 5: '发布成功', 6: '发布失败', 7: '已取消', 8: '已中断', 9: '排队中', 10: '异常关闭' })[s] || s
const buildStatusType = (s) =>
  ({ 0: 'default', 1: 'info', 2: 'success', 3: 'error', 4: 'info', 5: 'success', 6: 'error', 7: 'default', 8: 'warning', 9: 'info', 10: 'warning' })[s] || 'default'

const deployStatusText = (s) => ({ 0: '成功', 1: '失败', 2: '进行中', 3: '待审批', 4: '已拒绝' })[s] || s
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
    $message.warning('请选择构建记录')
    return
  }
  // 弹窗路径与表格路径保持一致的二次确认；失败不关窗（由 doDeploy 成功后关闭）
  $confirm({
    title: `确认从构建记录 #${deployForm.buildNumberId} 生成版本并部署到环境 ${deployForm.environment}？`,
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
    $message.warning('该应用未绑定构建配置')
    return
  }
  if (!silent) {
    $confirm({
      title: `确认从构建记录 #${buildRecord.buildNumberId} 生成版本并部署到环境 ${envName}？`,
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
    $message.success(`已生成版本 ${vres.data.version} 并部署到 ${envName}`)
    loadData()
    return true
  }
  return false
}

onMounted(loadData)
</script>
