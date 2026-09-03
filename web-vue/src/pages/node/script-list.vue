<template>
  <div class="">
    <!-- 数据表格 -->
    <CustomTable
      is-show-tools
      default-auto-refresh
      :auto-refresh-time="30"
      :active-page="activePage"
      table-name="node-script-list"
      :empty-description="$t('i18n_d2f4a1550a')"
      :data="list"
      size="medium"
      :columns="columns"
      :pagination="pagination"
      bordered
      row-key="id"
      :scroll="{
        x: 'max-content'
      }"
      @change="changePage"
      @refresh="loadData"
    >
      <template #title>
        <n-space wrap class="search-box">
          <n-select
            v-if="!nodeId"
            v-model:value="listQuery.nodeId"
            clearable
            :placeholder="$t('i18n_f8a613d247')"
            class="search-input-item"
            :options="Object.entries(nodeMap).map(([key, nodeName]) => ({ label: nodeName, value: key }))"
          />
          <n-input
            v-model:value="listQuery['%name%']"
            :placeholder="$t('i18n_d7ec2d3fea')"
            clearable
            class="search-input-item"
            @press-enter="loadData"
          />
          <n-input
            v-model:value="listQuery['%autoExecCron%']"
            :placeholder="$t('i18n_6b2e348a2b')"
            class="search-input-item"
            @press-enter="loadData"
          />
          <n-tooltip>
            <template #trigger>
              <span class="tw">
                <n-button :loading="loading" type="primary" @click="loadData">{{ $t('i18n_e5f71fc31e') }}</n-button>
              </span>
            </template>
            $t('i18n_4838a3bd20')
          </n-tooltip>

          <n-button type="primary" @click="handleEdit()">{{ $t('i18n_66ab5e9f24') }}</n-button>

          <template v-if="!nodeId">
            <n-dropdown
              v-if="nodeMap && Object.keys(nodeMap).length"
              :options="
                Object.entries(nodeMap).map(([key, nodeName]) => ({
                  label: nodeName,
                  key,
                  icon: () => h(NIcon, null, { default: () => h(SyncOutlined) }),
                  props: { onClick: () => sync(key) }
                }))
              "
            >
              <n-button type="primary" danger> {{ $t('i18n_b384470769') }}<DownOutlined /></n-button>
            </n-dropdown>
          </template>
          <n-button v-else type="primary" danger @click="sync(nodeId)">
            <SyncOutlined />{{ $t('i18n_b384470769') }}
          </n-button>
        </n-space>
      </template>
      <template #tableHelp>
        <n-tooltip>
          <template #trigger>
            <QuestionCircleOutlined />
          </template>

          <div>{{ $t('i18n_8ea93ff060') }}</div>

          <div>
            <ul>
              <li>{{ $t('i18n_5ecc709db7') }}</li>
              <li>{{ $t('i18n_14ee5b5dc5') }}</li>
              <li>{{ $t('i18n_fad1b9fb87') }}</li>
            </ul>
          </div>
        </n-tooltip>
      </template>
      <template #tableBodyCell="{ column, text, record }">
        <template v-if="column.tooltip">
          <n-tooltip placement="topLeft">
            <template #trigger>
              <span class="tw">
                <span class="tw">
                  <span>{{ text }}</span>
                </span>
              </span>
            </template>
            text
          </n-tooltip>
        </template>

        <template v-else-if="column.dataIndex === 'name'">
          <n-tooltip placement="topLeft" @click="handleEdit(record)">
            <template #trigger>
              <span class="tw">
                <span class="tw">
                  <!-- <span>{{ text }}</span> -->
                  <n-button text style="padding: 0" size="small">{{ text }}</n-button>
                </span>
              </span>
            </template>
            text
          </n-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'workspaceId'">
          <n-tag v-if="text === 'GLOBAL'">{{ $t('i18n_2be75b1044') }}</n-tag>
          <n-tag v-else>{{ $t('i18n_98d69f8b62') }}</n-tag>
        </template>
        <template v-else-if="column.dataIndex === 'scriptType'">
          <n-tooltip v-if="text === 'server-sync'">
            <template #trigger>
              <ClusterOutlined />
            </template>
            $t('i18n_51341b5024')
          </n-tooltip>
          <n-tooltip v-else>
            <template #trigger>
              <FileTextOutlined />
            </template>
            $t('i18n_3eab0eb8a9')
          </n-tooltip>
        </template>

        <template v-else-if="column.dataIndex === 'operation'">
          <n-space>
            <n-button size="small" type="primary" @click="handleExec(record)">{{ $t('i18n_1a6aa24e76') }}</n-button>
            <n-button size="small" type="primary" @click="handleLog(record)">{{ $t('i18n_456d29ef8b') }}</n-button>
            <n-button size="small" type="primary" @click="handleTrigger(record)">{{ $t('i18n_4696724ed3') }}</n-button>

            <n-dropdown
              :options="[
                {
                  label: $t('i18n_2f4aaddde3'),
                  key: '0',
                  disabled: record.scriptType === 'server-sync',
                  props: { onClick: () => handleDelete(record) }
                },
                { label: $t('i18n_663393986e'), key: '1', props: { onClick: () => handleUnbind(record) } }
              ]"
            >
              <a @click="(e) => e.preventDefault()">
                {{ $t('i18n_0ec9eaf9c3') }}
                <DownOutlined />
              </a>
            </n-dropdown>
          </n-space>
        </template>
      </template>
    </CustomTable>
    <!-- 编辑区 -->
    <ScriptEdit
      v-if="editScriptVisible"
      :node-id="temp.nodeId"
      :script-id="temp.scriptId"
      @close="
        () => {
          editScriptVisible = false
        }
      "
    ></ScriptEdit>
    <!-- 脚本控制台组件 -->
    <CustomDrawer
      v-if="drawerConsoleVisible"
      :title="drawerTitle"
      placement="right"
      width="85vw"
      :open="drawerConsoleVisible"
      @close="
        () => {
          drawerConsoleVisible = false
        }
      "
    >
      <script-console
        v-if="drawerConsoleVisible"
        :id="temp.id"
        :node-id="temp.nodeId"
        :def-args="temp.defArgs"
        :script-id="temp.scriptId"
      />
    </CustomDrawer>
    <!-- 脚本日志 -->
    <CustomDrawer
      v-if="drawerLogVisible"
      destroy-on-close
      :title="drawerTitle"
      width="50vw"
      :open="drawerLogVisible"
      @close="
        () => {
          drawerLogVisible = false
        }
      "
    >
      <script-log v-if="drawerLogVisible" :script-id="temp.scriptId" :node-id="temp.nodeId" />
    </CustomDrawer>
    <!-- 触发器 -->
    <CustomModal
      v-if="triggerVisible"
      v-model:open="triggerVisible"
      destroy-on-close
      :title="$t('i18n_4696724ed3')"
      width="50%"
      :footer="null"
    >
      <n-form ref="editTriggerForm" :model="temp">
        <n-tabs default-active-key="1">
          <template #rightExtra>
            <n-tooltip>
              <template #trigger>
                <span class="tw">
                  <n-button type="primary" size="small" @click="resetTrigger">{{ $t('i18n_4b9c3271dc') }}</n-button>
                </span>
              </template>
              $t('i18n_01ad26f4a9')
            </n-tooltip>
          </template>
          <n-tab-pane name="1" :tab="$t('i18n_1a6aa24e76')">
            <n-space direction="vertical" style="width: 100%">
              <n-alert :title="$t('i18n_947d983961')" type="warning" show-icon>
                <template #description>
                  <ul>
                    <li>{{ $t('i18n_9308f22bf6') }}</li>
                    <li>{{ $t('i18n_632a907224') }}</li>
                    <li>{{ $t('i18n_3fca26a684') }}</li>
                    <li>{{ $t('i18n_a04b7a8f5d') }}</li>
                  </ul>
                </template>
              </n-alert>
              <n-alert type="info" :title="`${$t('i18n_de78b73dab')}(${$t('i18n_00a070c696')})`">
                <template #description>
                  <n-p style="margin-bottom: 0">
                    <n-tag>GET</n-tag> <span>{{ temp.triggerUrl }} </span>
                    <copy-text :text="temp.triggerUrl" />
                  </n-p>
                </template>
              </n-alert>
              <n-alert type="info" :title="`${$t('i18n_8d202b890c')}(${$t('i18n_00a070c696')})`">
                <template #description>
                  <n-p style="margin-bottom: 0">
                    <n-tag>POST</n-tag> <span>{{ temp.batchTriggerUrl }} </span>
                    <copy-text :text="temp.batchTriggerUrl" />
                  </n-p>
                </template>
              </n-alert>
            </n-space>
          </n-tab-pane>
        </n-tabs>
      </n-form>
    </CustomModal>
  </div>
</template>
<script>
import { ClusterOutlined, DownOutlined, FileTextOutlined, QuestionCircleOutlined } from '@ant-design/icons-vue'

import { h } from 'vue'
import { NIcon } from 'naive-ui'
import { SyncOutlined } from '@ant-design/icons-vue'
import { deleteScript, getScriptListAll, getTriggerUrl, unbindScript, syncScript } from '@/api/node-other'

import { getNodeListAll } from '@/api/node'
import ScriptConsole from '@/pages/node/node-layout/other/script-console'
import { CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY, parseTime } from '@/utils/const'
import ScriptLog from '@/pages/node/node-layout/other/script-log'
import ScriptEdit from '@/pages/node/script-edit'

export default {
  components: {
    ScriptConsole,
    ScriptEdit,
    ScriptLog
  },
  props: {
    nodeId: {
      type: String,
      default: ''
    }
  },
  setup() {
    // 模板内联 dropdown options 的 icon 函数需访问模块作用域的 h/NIcon/图标组件
    return { h, NIcon, SyncOutlined }
  },
  data() {
    return {
      loading: false,
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),

      list: [],
      temp: {},
      nodeMap: {},
      editScriptVisible: false,
      drawerTitle: '',
      drawerConsoleVisible: false,
      drawerLogVisible: false,
      columns: [
        {
          title: 'scriptId',
          key: 'scriptId',
          ellipsis: true,
          width: 150
        },
        {
          title: this.$t('i18n_d7ec2d3fea'),
          key: 'name',
          ellipsis: true,
          width: 200
        },
        {
          title: this.$t('i18n_b1785ef01e'),
          key: 'nodeName',
          ellipsis: true,
          width: 150
        },
        {
          title: this.$t('i18n_6a588459d0'),
          key: 'workspaceName',
          ellipsis: true,
          width: 150
        },
        {
          title: this.$t('i18n_226b091218'),
          key: 'scriptType',
          width: 70,
          align: 'center',
          ellipsis: true
        },
        {
          title: this.$t('i18n_fffd3ce745'),
          key: 'workspaceId',
          ellipsis: true,

          width: '90px'
        },
        {
          title: this.$t('i18n_6b2e348a2b'),
          key: 'autoExecCron',
          ellipsis: true,
          width: 120
        },
        {
          title: this.$t('i18n_1303e638b5'),
          key: 'modifyTimeMillis',
          sorter: true,
          width: '170px',
          ellipsis: true,
          render: (row) => parseTime(row['modifyTimeMillis'])
        },
        {
          title: this.$t('i18n_eca37cb072'),
          key: 'createTimeMillis',
          sorter: true,
          width: '170px',
          ellipsis: true,
          render: (row) => parseTime(row['createTimeMillis'])
        },
        {
          title: this.$t('i18n_95a43eaa59'),
          key: 'createUser',
          ellipsis: true,
          width: '120px'
        },
        {
          title: this.$t('i18n_9baca0054e'),
          key: 'modifyUser',
          ellipsis: true,
          width: '120px'
        },
        {
          title: this.$t('i18n_26c1f8d83e'),
          key: 'lastRunUser',
          ellipsis: true,
          width: '120px'
        },
        {
          title: this.$t('i18n_2b6bc0f293'),
          key: 'operation',
          align: 'center',

          fixed: 'right',
          width: '250px'
        }
      ],

      triggerVisible: false
    }
  },
  computed: {
    pagination() {
      return COMPUTED_PAGINATION(this.listQuery)
    },
    activePage() {
      return this.$attrs.routerUrl === this.$route.path
    }
  },
  mounted() {
    // this.calcTableHeight();

    getNodeListAll().then((res) => {
      if (res.code === 200) {
        res.data.forEach((item) => {
          this.nodeMap[item.id] = item.name
        })
      }
      this.loadData()
    })
  },
  methods: {
    // 加载数据
    loadData(pointerEvent) {
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page
      this.loading = true
      this.nodeId && (this.listQuery.nodeId = this.nodeId)
      getScriptListAll(this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data.result
          this.listQuery.total = res.data.total
        }
        this.loading = false
      })
    },
    parseTime,
    // 编辑
    handleEdit(record) {
      if (record) {
        this.temp = { ...record }
      } else {
        this.temp = { nodeId: this.listQuery.nodeId }
      }

      this.editScriptVisible = true
    },

    handleDelete(record) {
      $confirm({
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        content: this.$t('i18n_3b19b2a75c'),
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
        onOk: () => {
          return deleteScript({
            nodeId: record.nodeId,
            id: record.scriptId
          }).then((res) => {
            if (res.code === 200) {
              $notification.success({
                message: res.msg
              })
              this.loadData()
            }
          })
        }
      })
    },
    // 执行 Script
    handleExec(record) {
      this.temp = Object.assign({}, record)
      this.drawerTitle = `${this.$t('i18n_b5c3770699')}(${this.temp.name})`
      this.drawerConsoleVisible = true
    },
    handleLog(record) {
      this.temp = Object.assign({}, record)
      this.drawerTitle = `${this.$t('i18n_456d29ef8b')}(${this.temp.name})`
      this.drawerLogVisible = true
    },
    // // 关闭 console
    // onConsoleClose() {
    //   this.drawerConsoleVisible = false;
    // },

    // 分页、排序、筛选变化时触发
    changePage(pagination, filters, sorter) {
      this.listQuery = CHANGE_PAGE(this.listQuery, { pagination, sorter })
      this.loadData()
    },
    // 触发器
    handleTrigger(record) {
      this.temp = Object.assign({}, record)

      getTriggerUrl({
        id: record.id
      }).then((res) => {
        if (res.code === 200) {
          this.fillTriggerResult(res)
          this.triggerVisible = true
        }
      })
    },
    // 重置触发器
    resetTrigger() {
      getTriggerUrl({
        id: this.temp.id,
        rest: 'rest'
      }).then((res) => {
        if (res.code === 200) {
          $notification.success({
            message: res.msg
          })
          this.fillTriggerResult(res)
        }
      })
    },
    fillTriggerResult(res) {
      this.temp.triggerUrl = `${location.protocol}//${location.host}${res.data.triggerUrl}`
      this.temp.batchTriggerUrl = `${location.protocol}//${location.host}${res.data.batchTriggerUrl}`

      this.temp = { ...this.temp }
    },
    // 解绑
    handleUnbind(record) {
      const html = `
      <b style='font-size: 20px;'>${this.$t('i18n_2025ad11ee')}</b>
      <ul style='font-size: 20px;color:red;font-weight: bold;'>
        <li>${this.$t('i18n_56230405ae')}</b></li>
        <li>${this.$t('i18n_5c93055d9c')}</li>
        <li>${this.$t('i18n_27d0c8772c')}</li>
      </ul>
      `
      $confirm({
        title: this.$t('i18n_9362e6ddf8'),
        zIndex: 1009,
        content: h('div', null, [h('p', { innerHTML: html }, null)]),
        okButtonProps: { props: { type: 'danger', size: 'small' } },
        cancelButtonProps: { props: { type: 'primary' } },
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
        onOk: () => {
          return unbindScript({
            id: record.id
          }).then((res) => {
            if (res.code === 200) {
              $notification.success({
                message: res.msg
              })
              this.loadData()
            }
          })
        }
      })
    },
    sync(nodeId) {
      syncScript({
        nodeId: nodeId
      }).then((res) => {
        if (res.code == 200) {
          $notification.success({
            message: res.msg
          })
          this.loadData()
        }
      })
    }
  }
}
</script>
