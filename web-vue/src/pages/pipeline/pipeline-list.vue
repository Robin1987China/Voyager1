<template>
  <div class="pipeline-editor">
    <n-grid :x-gap="12">
      <!-- 节点库 -->
      <n-grid-item :span="4">
        <n-card title="节点库" size="small">
          <div
            v-for="item in nodeTypes"
            :key="item.type"
            class="palette-node"
            :style="{ borderColor: item.color }"
            @click="addStage(item.type)"
          >
            <span class="palette-dot" :style="{ background: item.color }"></span>
            {{ item.label }}
          </div>
          <div style="margin-top: 12px; color: var(--app-color-text-secondary); font-size: 12px">
            点击节点添加到画布，画布内可拖拽排序
          </div>
        </n-card>
        <n-card title="配置" size="small" style="margin-top: 12px">
          <n-form layout="vertical" size="small">
            <n-form-item label="名称">
              <n-input v-model:value="configForm.name" />
            </n-form-item>
            <n-form-item label="应用">
              <n-select
                v-model:value="configForm.buildId"
                filterable
                placeholder="选择构建配置"
                :options="buildOptions.map((b) => ({ label: `${b.name} (${b.id.slice(0, 8)})`, value: b.id }))"
              />
            </n-form-item>
            <n-form-item label="触发">
              <n-checkbox v-model:value="triggerCronEnabled">定时</n-checkbox>
              <n-input
                v-if="triggerCronEnabled"
                v-model:value="triggerCron"
                placeholder="cron 表达式，如 0 0 2 * * ?"
                style="margin-top: 4px"
              />
              <n-checkbox v-model:value="triggerWebhookEnabled" style="margin-top: 4px">WebHook</n-checkbox>
              <div v-if="triggerWebhookEnabled" style="margin-top: 4px">
                <n-button size="small" @click="generateToken">生成 Token</n-button>
                <div
                  v-if="triggerToken"
                  style="
                    font-size: 11px;
                    color: var(--app-color-text-secondary);
                    margin-top: 4px;
                    word-break: break-all;
                  "
                >
                  {{ triggerToken }}
                </div>
              </div>
            </n-form-item>
            <n-form-item>
              <n-button type="primary" block :loading="saving" @click="saveConfig">保存配置</n-button>
            </n-form-item>
          </n-form>
          <n-divider style="margin: 8px 0" />
          <div style="font-size: 12px; color: var(--app-color-text-secondary); margin-bottom: 6px">我的配置</div>
          <div v-for="cfg in configList" :key="cfg.id" class="config-item">
            <div @click="loadConfig(cfg)">
              <span class="config-name">{{ cfg.name }}</span>
              <span class="config-seq">{{ stageSummary(cfg.stages) }}</span>
            </div>
            <n-button type="primary" size="small" style="margin-top: 4px" @click="trigger(cfg)">触发</n-button>
            <n-popconfirm @positive-click="removeConfig(cfg)">
              <template #trigger>
                <span class="tw">
                  <n-button text danger size="small">删</n-button>
                </span>
              </template>
              确认删除？
            </n-popconfirm>
          </div>
        </n-card>
      </n-grid-item>

      <!-- 画布 -->
      <n-grid-item :span="13">
        <n-card title="Pipeline 画布" size="small" class="canvas-card">
          <div v-if="stages.length" class="canvas">
            <draggable :list="stages" class="canvas-drag" handle=".stage-handle" :animation="200" :lock-axis="'y'">
              <div v-for="(stage, index) in stages" :key="stage.id" class="stage-node">
                <div class="stage-head">
                  <span class="stage-handle">⠿</span>
                  <span class="stage-index">{{ index + 1 }}</span>
                  <span class="stage-type" :style="{ color: typeColor(stage.type) }">
                    {{ typeLabel(stage.type) }}
                  </span>
                  <n-button text danger size="small" @click="removeStage(index)">✕</n-button>
                </div>
                <div class="stage-body">
                  <template v-if="stage.type === 'exec'">
                    <n-input
                      v-model:value="stage.command"
                      type="textarea"
                      :rows="2"
                      placeholder="命令，如 mvn test"
                      @click.stop
                    />
                  </template>
                  <template v-if="stage.type === 'publish'">
                    <n-input
                      v-model:value="stage.environment"
                      placeholder="环境: dev/test/prod"
                      size="small"
                      @click.stop
                    />
                    <n-input
                      v-model:value="stage.buildNumberId"
                      placeholder="构建编号（可留空取构建结果）"
                      size="small"
                      style="margin-top: 4px"
                      @click.stop
                    />
                  </template>
                  <template v-if="stage.type === 'build'">
                    <n-input
                      v-model:value="stage.buildId"
                      placeholder="构建配置（留空取应用）"
                      size="small"
                      @click.stop
                    />
                  </template>
                  <template v-if="stage.type === 'approval'">
                    <n-input v-model:value="stage.desc" placeholder="审批说明" size="small" @click.stop />
                  </template>
                </div>
                <div v-if="index < stages.length - 1" class="stage-arrow">↓</div>
              </div>
            </draggable>
          </div>
          <n-empty v-else description="从左侧点击添加阶段" style="padding: 40px 0" />
        </n-card>
        <n-card title="执行记录" size="small" style="margin-top: 12px">
          <n-data-table
            :data="executeList"
            :columns="executeColumns"
            :pagination="false"
            size="small"
            :row-key="(row) => row.id"
          />
        </n-card>
      </n-grid-item>

      <!-- 参数面板 -->
      <n-grid-item :span="7">
        <n-card title="阶段序列" size="small">
          <div v-if="stages.length" class="sequence">
            <template v-for="(stage, i) in stages" :key="stage.id">
              <span :style="{ color: typeColor(stage.type) }">{{ typeLabel(stage.type) }}</span>
              <span v-if="i < stages.length - 1" class="seq-arrow">→</span>
            </template>
          </div>
          <n-empty v-else description="空" />
        </n-card>
        <n-card title="使用帮助" size="small" style="margin-top: 12px">
          <ol style="font-size: 12px; color: var(--app-color-text-secondary); padding-left: 18px">
            <li>从左侧节点库点击添加阶段</li>
            <li>画布内拖拽 ⠿ 可调整顺序</li>
            <li>节点内直接编辑参数</li>
            <li>保存后在下方配置列表点击可加载/触发</li>
          </ol>
        </n-card>
      </n-grid-item>
    </n-grid>
  </div>
</template>

<script lang="ts" setup>
import { ref, reactive, onMounted, h } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { NButton } from 'naive-ui'
import { Draggable as draggable } from 'vue3-smooth-dnd'
import {
  savePipelineConfig,
  listPipelineConfig,
  triggerPipeline,
  approvalPipeline,
  listPipelineExecute,
  deletePipelineConfig
} from '@/api/pipeline'
import { getBuildList } from '@/api/build-info'

const nodeTypes = [
  { type: 'build', label: '构建', color: '#1677ff' },
  { type: 'exec', label: '命令', color: '#52c41a' },
  { type: 'publish', label: '发布', color: '#fa8c16' },
  { type: 'approval', label: '审批', color: '#eb2f96' }
]
const typeLabel = (t) => (nodeTypes.find((n) => n.type === t) || { label: t }).label
const typeColor = (t) => (nodeTypes.find((n) => n.type === t) || { color: '#999' }).color

const configForm = reactive({ name: '', buildId: '', id: '' })
const route = useRoute()
const router = useRouter()
const buildOptions = ref<any[]>([])
const stages = ref<any[]>([])
const configList = ref<any[]>([])
const executeList = ref<any[]>([])
const saving = ref(false)
const triggerCronEnabled = ref(false)
const triggerWebhookEnabled = ref(false)
const triggerCron = ref('')
const triggerToken = ref('')
let currentPipelineId = ''

const executeStatusText = (s) =>
  ({ 0: '等待中', 1: '运行中', 2: '成功', 3: '失败', 4: '已取消', 5: '等待审批' })[s] || s

const newStage = (type) => ({
  id: `stage-${Date.now()}-${Math.floor(Math.random() * 1000)}`,
  type,
  command: '',
  environment: '',
  buildNumberId: '',
  buildId: '',
  desc: ''
})

const addStage = (type) => {
  stages.value.push(newStage(type))
}
const removeStage = (index) => {
  stages.value.splice(index, 1)
}

const buildStagesJson = () => {
  return stages.value.map((s) => {
    const params: Record<string, string> = {}
    if (s.type === 'exec') params.command = s.command
    if (s.type === 'publish') {
      params.environment = s.environment
      params.buildNumberId = s.buildNumberId
    }
    if (s.type === 'build') params.buildId = s.buildId
    if (s.type === 'approval') params.desc = s.desc
    return { id: s.id, type: s.type, params }
  })
}

const parseStagesFromJson = (stagesJson) => {
  try {
    return JSON.parse(stagesJson).map((s) => {
      const st = newStage(s.type)
      st.id = s.id || st.id
      const p = s.params || {}
      st.command = p.command || ''
      st.environment = p.environment || ''
      st.buildNumberId = p.buildNumberId || ''
      st.buildId = p.buildId || ''
      st.desc = p.desc || ''
      return st
    })
  } catch (e) {
    return []
  }
}

const stageSummary = (stagesJson) => {
  const s = parseStagesFromJson(stagesJson)
  return s.length ? s.map((x) => typeLabel(x.type)).join(' → ') : ''
}
const executeStageSummary = (stagesJson) => {
  try {
    return JSON.parse(stagesJson)
      .map((x) => `${typeLabel(x.type)}(${x.status || 'wait'})`)
      .join(' → ')
  } catch (e) {
    return stagesJson || ''
  }
}

const loadConfigs = async () => {
  const res: any = await listPipelineConfig({ buildId: '' })
  if (res.code === 200) {
    configList.value = res.data
  }
}

const loadConfig = (cfg) => {
  configForm.name = cfg.name
  configForm.buildId = cfg.buildId
  configForm.id = cfg.id
  stages.value = parseStagesFromJson(cfg.stages)
  parseTriggers(cfg.triggers)
  $message.info(`已加载配置: ${cfg.name}`)
}

const buildTriggersJson = () => {
  const triggers: any[] = []
  if (triggerCronEnabled.value && triggerCron.value) {
    triggers.push({ type: 'cron', cron: triggerCron.value })
  }
  if (triggerWebhookEnabled.value) {
    if (!triggerToken.value) {
      triggerToken.value = Math.random().toString(36).slice(2) + Date.now().toString(36)
    }
    triggers.push({ type: 'webhook', token: triggerToken.value })
  }
  return JSON.stringify(triggers)
}

const parseTriggers = (triggersJson) => {
  try {
    const triggers = JSON.parse(triggersJson || '[]')
    triggerCronEnabled.value = triggers.some((t) => t.type === 'cron')
    const cron = triggers.find((t) => t.type === 'cron')
    triggerCron.value = cron ? cron.cron : ''
    triggerWebhookEnabled.value = triggers.some((t) => t.type === 'webhook')
    const wh = triggers.find((t) => t.type === 'webhook')
    triggerToken.value = wh ? wh.token : ''
  } catch (e) {
    // ignore
  }
}

const generateToken = () => {
  triggerToken.value = Math.random().toString(36).slice(2) + Date.now().toString(36)
}

const saveConfig = async () => {
  if (!configForm.name) {
    $message.warning('请填写名称')
    return
  }
  if (!stages.value.length) {
    $message.warning('请至少添加一个阶段')
    return
  }
  saving.value = true
  try {
    const res: any = await savePipelineConfig({
      id: configForm.id || undefined,
      name: configForm.name,
      buildId: configForm.buildId || 'default',
      triggers: buildTriggersJson(),
      stages: JSON.stringify(buildStagesJson()),
      enabled: true
    })
    if (res.code === 200) {
      $message.success('保存成功')
      configForm.id = res.data
      loadConfigs()
    }
  } finally {
    saving.value = false
  }
}

const removeConfig = async (cfg) => {
  const res: any = await deletePipelineConfig({ id: cfg.id })
  if (res.code === 200) {
    $message.success('已删除')
    loadConfigs()
  }
}

const trigger = async (cfg) => {
  const res: any = await triggerPipeline({ pipelineId: cfg.id })
  if (res.code === 200) {
    $message.success(res.msg)
    currentPipelineId = cfg.id
    loadExecute(cfg.id)
  }
}

const loadExecute = async (pipelineId) => {
  const res: any = await listPipelineExecute({ pipelineId })
  if (res.code === 200) {
    executeList.value = res.data
  }
}

const approval = async (record, approve) => {
  const res: any = await approvalPipeline({ executeId: record.id, approve })
  if (res.code === 200) {
    $message.success(res.msg)
    if (currentPipelineId) loadExecute(currentPipelineId)
  }
}

const executeColumns = [
  { title: '触发', key: 'triggerType', width: 80 },
  { title: '状态', key: 'status', width: 90, render: (row) => executeStatusText(row.status) },
  { title: '阶段', key: 'stages', render: (row) => executeStageSummary(row.stages) },
  {
    title: '操作',
    key: 'actions',
    width: 150,
    render: (row) =>
      row.status === 5
        ? [
            h(
              NButton,
              { type: 'primary', size: 'small', onClick: () => approval(row, true) },
              { default: () => '批准' }
            ),
            h(
              NButton,
              { danger: true, size: 'small', style: { marginLeft: '4px' }, onClick: () => approval(row, false) },
              { default: () => '拒绝' }
            )
          ]
        : null
  }
]

onMounted(async () => {
  // 构建配置下拉
  const buildRes: any = await getBuildList({ page: 1, limit: 100 })
  if (buildRes.code === 200) {
    buildOptions.value = buildRes.data?.result || buildRes.data || []
  }
  loadConfigs()
  // 从构建列表跳转带入 buildId
  if (route.query.buildId) {
    configForm.buildId = String(route.query.buildId)
    router.replace({ query: {} })
  }
})
</script>

<style scoped>
.palette-node {
  border: 1px solid;
  border-radius: 6px;
  padding: 8px 10px;
  margin-bottom: 8px;
  cursor: pointer;
  font-size: 13px;
  transition: all 0.2s;
  background: var(--app-color-bg-container);
}
.palette-node:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.12);
}
.palette-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-right: 6px;
}
.canvas-card .canvas {
  min-height: 320px;
  max-height: 60vh;
  overflow-y: auto;
  padding: 8px;
}
.stage-node {
  border: 1px solid var(--app-color-border-secondary);
  border-radius: 8px;
  margin-bottom: 4px;
  background: var(--app-color-bg-container);
}
.stage-head {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px;
  border-bottom: 1px solid var(--app-color-border-secondary);
}
.stage-handle {
  cursor: grab;
  color: var(--app-color-text-quaternary);
  font-size: 14px;
  user-select: none;
}
.stage-index {
  background: #1677ff;
  color: #fff;
  border-radius: 50%;
  width: 18px;
  height: 18px;
  font-size: 11px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.stage-type {
  flex: 1;
  font-weight: 600;
  font-size: 13px;
}
.stage-body {
  padding: 8px 10px;
}
.stage-arrow {
  text-align: center;
  color: var(--app-color-text-quaternary);
  line-height: 1;
  padding: 2px 0;
}
.sequence {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
  font-size: 13px;
}
.seq-arrow {
  color: var(--app-color-text-quaternary);
}
.config-item {
  padding: 6px 8px;
  border: 1px solid var(--app-color-border-secondary);
  border-radius: 6px;
  margin-bottom: 6px;
  cursor: pointer;
  font-size: 12px;
}
.config-item:hover {
  border-color: #1677ff;
}
.config-name {
  font-weight: 600;
  margin-right: 6px;
}
.config-seq {
  color: var(--app-color-text-secondary);
}
</style>
