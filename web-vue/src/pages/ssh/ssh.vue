<template>
  <div>
    <template v-if="useSuggestions">
      <n-result :title="$t('i18n_ce043fac7d')" :sub-title="$t('i18n_ace71047a0')">
        <template #extra>
          <router-link to="/system/assets/ssh-list">
            <n-button key="console" type="primary">{{ $t('i18n_6dcf6175d8') }}</n-button></router-link
          >
        </template>
      </n-result>
    </template>
    <!-- 数据表格 -->
    <CustomTable
      v-else
      is-show-tools
      default-auto-refresh
      :auto-refresh-time="5"
      table-name="ssh-list"
      :empty-description="$t('i18n_a9795c06c8')"
      :active-page="activePage"
      :data="list"
      :columns="columns"
      size="medium"
      :pagination="pagination"
      bordered
      row-key="id"
      :row-selection="rowSelection"
      :scroll="{
        x: 'max-content'
      }"
      @change="changePage"
      @refresh="loadData"
    >
      <template #title>
        <n-space wrap class="search-box">
          <n-input
            v-model:value="listQuery['%name%']"
            class="search-input-item"
            :placeholder="$t('i18n_46ad87708f')"
            @press-enter="loadData"
          />
          <n-select
            v-model:value="listQuery.group"
            filterable
            clearable
            :placeholder="$t('i18n_829abe5a8d')"
            class="search-input-item"
            :options="groupList"
          />
          <n-tooltip>
            <template #trigger>
              <span class="tw">
                <n-button type="primary" :loading="loading" @click="loadData">{{ $t('i18n_e5f71fc31e') }}</n-button>
              </span>
            </template>
            $t('i18n_4838a3bd20')
          </n-tooltip>

          <n-button
            type="primary"
            :disabled="!tableSelections || !tableSelections.length"
            @click="syncToWorkspaceShow"
            >{{ $t('i18n_398ce396cd') }}</n-button
          >
          <n-button type="primary" @click="toSshTabs">{{ $t('i18n_848c07af9b') }}</n-button>
        </n-space>
      </template>
      <template #tableHelp>
        <n-tooltip>
          <template #trigger>
            <QuestionCircleOutlined />
          </template>

          <div>
            <ul>
              <li>{{ $t('i18n_a13d8ade6a') }}</li>
              <li>{{ $t('i18n_fda92d22d9') }}</li>
              <li>{{ $t('i18n_1278df0cfc') }}</li>
            </ul>
          </div>
        </n-tooltip>
      </template>
      <template #tableBodyCell="{ column, text, record }">
        <template v-if="column.tooltip">
          <n-tooltip>
            <template #trigger>
              {{ text }}
            </template>
            text
          </n-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'host'">
          <n-tooltip>
            <template #trigger>
              {{ record.machineSsh && record.machineSsh.host }}:{{ record.machineSsh && record.machineSsh.port }}
            </template>
            `${record.machineSsh && record.machineSsh.host}:${record.machineSsh && record.machineSsh.port}`
          </n-tooltip>
        </template>
        <template v-else-if="column.dataIndex instanceof Array && column.dataIndex.includes('status')">
          <n-tooltip>
            <template #trigger>
              <span class="tw">
                <n-tag
                  :color="
                    statusMap[record.machineSsh && record.machineSsh.status] &&
                    statusMap[record.machineSsh && record.machineSsh.status].color
                  "
                >
                  {{
                    (statusMap[record.machineSsh && record.machineSsh.status] &&
                      statusMap[record.machineSsh && record.machineSsh.status].desc) ||
                    $t('i18n_1622dc9b6b')
                  }}
                </n-tag>
              </span>
            </template>
            record.machineSsh && record.machineSsh.statusMsg
          </n-tooltip>
        </template>
        <template v-else-if="column.dataIndex instanceof Array && column.dataIndex.includes('osName')">
          <n-popover>
            <template #trigger>
              {{ text || $t('i18n_1622dc9b6b') }}
            </template>
            <template #header>{{ $t('i18n_b7ea5e506c') }}</template>

            <p>{{ $t('i18n_c17aefeebf') }}{{ record.machineSsh && record.machineSsh.osName }}</p>
            <p>{{ $t('i18n_f425f59044') }}{{ record.machineSsh && record.machineSsh.osVersion }}</p>
            <p>CPU{{ $t('i18n_045cd62da3') }}{{ record.machineSsh && record.machineSsh.osCpuIdentifierName }}</p>
            <p>{{ $t('i18n_07a0e44145') }}{{ record.machineSsh && record.machineSsh.hostName }}</p>
            <p>
              {{ $t('i18n_8a745296f4') }}{{ formatDuration(record.machineSsh && record.machineSsh.osSystemUptime) }}
            </p>
          </n-popover>
        </template>
        <template v-else-if="column.dataIndex instanceof Array && column.dataIndex.includes('osOccupyMemory')">
          <n-tooltip placement="topLeft">
            <template #trigger>
              <span class="tw">
                <span class="tw">
                  <span
                    >{{ formatPercent(record.machineSsh && record.machineSsh.osOccupyMemory) }}/{{
                      renderSize(record.machineSsh && record.machineSsh.osMoneyTotal)
                    }}</span
                  >
                </span>
              </span>
            </template>
            `${$t('i18n_ca32cdfd59')}${formatPercent( record.machineSsh && record.machineSsh.osOccupyMemory
            )},${$t('i18n_a0a3d583b9')}${renderSize(record.machineSsh && record.machineSsh.osMoneyTotal)}`
          </n-tooltip>
        </template>

        <template v-else-if="column.dataIndex instanceof Array && column.dataIndex.includes('osOccupyCpu')">
          <n-tooltip placement="topLeft">
            <template #trigger>
              <span class="tw">
                <span class="tw">
                  <span
                    >{{ (formatPercent2Number(record.machineSsh && record.machineSsh.osOccupyCpu) || '-') + '%' }} /
                    {{ record.machineSsh && record.machineSsh.osCpuCores }}</span
                  >
                </span>
              </span>
            </template>
            `CPU${$t('i18n_afb9fe400b')}${formatPercent2Number( record.machineSsh && record.machineSsh.osOccupyCpu
            )}%,CPU${$t('i18n_40349f5514')}${record.machineSsh && record.machineSsh.osCpuCores}`
          </n-tooltip>
        </template>

        <template v-else-if="column.dataIndex instanceof Array && column.dataIndex.includes('osMaxOccupyDisk')">
          <n-popover>
            <template #trigger>
              <span class="tw">
                <span class="tw">
                  <span
                    >{{ formatPercent(record.machineSsh && record.machineSsh.osMaxOccupyDisk) }}
                    /
                    {{ renderSize(record.machineSsh && record.machineSsh.osFileStoreTotal) }}</span
                  >
                </span>
              </span>
            </template>
            <template #header>{{ $t('i18n_a74b62f4bb') }}</template>

            <p>{{ $t('i18n_7e359f4b71') }}{{ renderSize(record.machineSsh && record.machineSsh.osFileStoreTotal) }}</p>
            <p>
              {{ $t('i18n_de17fc0b78') }}{{ formatPercent(record.machineSsh && record.machineSsh.osMaxOccupyDisk) }}
            </p>
            <p>{{ $t('i18n_ba452d57f2') }}{{ record.machineSsh && record.machineSsh.osMaxOccupyDiskName }}</p>
          </n-popover>
        </template>

        <template v-else-if="column.dataIndex === 'nodeId'">
          <template v-if="record.linkNode">
            <n-tooltip placement="topLeft">
              <template #trigger>
                <span class="tw">
                  <n-button
                    size="small"
                    style="width: 90px; padding: 0 10px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis"
                    text
                    @click="toNode(record.linkNode)"
                  >
                    {{ record.linkNode.name }}
                  </n-button>
                </span>
              </template>
              `${$t('i18n_5d83794cfa')}${record.linkNode.name}`
            </n-tooltip>
          </template>
          <template v-else>-</template>
        </template>
        <template v-else-if="column.dataIndex === 'operation'">
          <n-space>
            <n-dropdown
              :options="[
                {
                  label: $t('i18n_a3296ef4f6'),
                  key: '0',
                  icon: () => h(NIcon, null, { default: () => h(FullscreenOutlined) }),
                  props: { onClick: () => handleTerminal(record, true) }
                },
                {
                  label: $t('i18n_0934f7777a'),
                  key: '1',
                  icon: () => h(NIcon, null, { default: () => h(FullscreenOutlined) }),
                  props: { onClick: () => handleFullTerminal(record) }
                }
              ]"
            >
              <n-button size="small" type="primary" @click="handleTerminal(record, false)"
                >{{ $t('i18n_4722bc0c56') }}<DownOutlined
              /></n-button>
            </n-dropdown>
            <template v-if="record.fileDirs">
              <n-button size="small" type="primary" @click="handleFile(record)">{{ $t('i18n_2a0c4740f1') }}</n-button>
            </template>
            <template v-else>
              <n-tooltip placement="topLeft">
                <template #trigger>
                  <span class="tw">
                    <n-button size="small" type="primary" :disabled="true">{{ $t('i18n_2a0c4740f1') }}</n-button>
                  </span>
                </template>
                $t('i18n_46c8ba7b7f')
              </n-tooltip>
            </template>

            <n-dropdown
              :options="[
                { label: $t('i18n_95b351c862'), key: '0', props: { onClick: () => handleEdit(record) } },
                { label: $t('i18n_2f4aaddde3'), key: '1', props: { onClick: () => handleDelete(record) } },
                { label: $t('i18n_3ed3733078'), key: '2', props: { onClick: () => handleViewLog(record) } }
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
    <CustomModal
      v-if="editSshVisible"
      v-model:open="editSshVisible"
      destroy-on-close
      width="600px"
      :title="$t('i18n_7a30792e2a')"
      :mask-closable="false"
      :confirm-loading="confirmLoading"
      @ok="handleEditSshOk"
    >
      <n-form ref="editSshForm" :rules="rules" :model="temp">
        <template v-if="getUserInfo && getUserInfo.systemUser">
          <n-alert type="info" show-icon style="width: 100%; margin-bottom: 10px">
            <template #message>
              <ul>
                <li>{{ $t('i18n_d0b7462bdc') }}</li>
                <li>{{ $t('i18n_dc3356300f') }}</li>
                <li>{{ $t('i18n_0b2fab7493') }}</li>
              </ul>
            </template>
          </n-alert>
        </template>
        <n-form-item :label="$t('i18n_10f6fc171a')" path="name">
          <n-input v-model:value="temp.name" :max-length="50" :placeholder="$t('i18n_10f6fc171a')" />
        </n-form-item>
        <n-form-item :label="$t('i18n_1014b33d22')" path="group">
          <custom-select
            v-model:value="temp.group"
            :data="groupList"
            :input-placeholder="$t('i18n_bd0362bed3')"
            :select-placeholder="$t('i18n_9cac799f2f')"
          >
            <template #suffix>
              <n-tooltip>
                <template #trigger>
                  <QuestionCircleOutlined />
                </template>

                <div>
                  {{ $t('i18n_bd7c7abc8c') }}
                </div>
              </n-tooltip>
            </template>
          </custom-select>
        </n-form-item>
      </n-form>
    </CustomModal>

    <!-- 文件管理 -->
    <CustomDrawer
      v-if="drawerVisible"
      destroy-on-close
      :open="drawerVisible"
      :title="`${temp.name} ${$t('i18n_8780e6b3d1')}`"
      placement="right"
      width="90vw"
      @close="
        () => {
          drawerVisible = false
        }
      "
    >
      <ssh-file v-if="drawerVisible" :ssh-id="temp.id" />
    </CustomDrawer>
    <!-- Terminal -->
    <CustomModal
      v-if="terminalVisible"
      v-model:open="terminalVisible"
      destroy-on-close
      :style="{
        maxWidth: '100vw',
        top: terminalFullscreen ? 0 : false,
        paddingBottom: 0
      }"
      :width="terminalFullscreen ? '100vw' : '80vw'"
      :body-style="{
        padding: '0 5px',
        paddingTop: '10px',
        marginRight: '10px',
        height: `${terminalFullscreen ? 'calc(100vh - 80px)' : '70vh'}`
      }"
      :title="temp.name"
      :footer="null"
      :mask-closable="false"
    >
      <!-- <div :style="`height: ${this.terminalFullscreen ? 'calc(100vh - 70px - 20px)' : 'calc(70vh - 20px)'}`"> -->
      <terminal1 v-if="terminalVisible" :ssh-id="temp.id" />
      <!-- </div> -->
    </CustomModal>
    <!-- 操作日志 -->
    <CustomModal
      v-if="viewOperationLog"
      v-model:open="viewOperationLog"
      destroy-on-close
      :title="$t('i18n_cda84be2f6')"
      width="80vw"
      :footer="null"
      :mask-closable="false"
    >
      <OperationLog v-if="viewOperationLog" :ssh-id="temp.id"></OperationLog>
    </CustomModal>
    <!-- 同步到其他工作空间 -->
    <CustomModal
      v-if="syncToWorkspaceVisible"
      v-model:open="syncToWorkspaceVisible"
      destroy-on-close
      :title="$t('i18n_1a44b9e2f7')"
      :confirm-loading="confirmLoading"
      :mask-closable="false"
      @ok="handleSyncToWorkspace"
    >
      <n-alert :title="$t('i18n_947d983961')" type="warning" show-icon>
        <template #description>
          <ul>
            <li>{{ $t('i18n_59a15a0848') }}</li>
            <li>{{ $t('i18n_412504968d') }}</li>
            <li>{{ $t('i18n_57b7990b45') }}</li>
          </ul>
        </template>
      </n-alert>
      <n-form :model="temp">
        <n-form-item> </n-form-item>
        <n-form-item :label="$t('i18n_b4a8c78284')" path="workspaceId">
          <n-select
            v-model:value="temp.workspaceId"
            filterable
            :placeholder="$t('i18n_b3bda9bf9e')"
            :options="workspaceList.map((item) => ({ label: item.name, value: item.id }))"
          />
        </n-form-item>
      </n-form>
    </CustomModal>
  </div>
</template>
<script>
import { DownOutlined, QuestionCircleOutlined } from '@ant-design/icons-vue'

import { h } from 'vue'
import { NIcon } from 'naive-ui'
import { FullscreenOutlined } from '@ant-design/icons-vue'

import { deleteSsh, editSsh, getSshList, syncToWorkspace, getSshGroupAll } from '@/api/ssh'
import { statusMap } from '@/api/system/assets-ssh'
import SshFile from '@/pages/ssh/ssh-file'
import Terminal1 from '@/pages/ssh/terminal'
import {
  CHANGE_PAGE,
  COMPUTED_PAGINATION,
  PAGE_DEFAULT_LIST_QUERY,
  parseTime,
  formatPercent2Number,
  renderSize,
  formatDuration,
  formatPercent
} from '@/utils/const'
import { getWorkSpaceListAll } from '@/api/workspace'

import { mapState } from 'pinia'
import { useUserStore } from '@/stores/user'
import { useAppStore } from '@/stores/app'
import OperationLog from '@/pages/system/assets/ssh/operation-log'
import CustomSelect from '@/components/customSelect'

export default {
  components: {
    SshFile,
    Terminal1,
    OperationLog,
    CustomSelect
  },
  setup() {
    // 模板内联 dropdown options 的 icon 函数需访问模块作用域的 h/NIcon/图标组件
    return { h, NIcon, FullscreenOutlined }
  },
  data() {
    return {
      loading: true,
      list: [],
      temp: {},
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      editSshVisible: false,

      syncToWorkspaceVisible: false,
      tableSelections: [],
      workspaceList: [],
      tempNode: {},
      // fileList: [],
      statusMap,

      drawerVisible: false,
      terminalVisible: false,
      terminalFullscreen: false,
      viewOperationLog: false,

      columns: [
        {
          title: this.$t('i18n_d7ec2d3fea'),
          key: 'name',
          sorter: true,
          width: 100,
          ellipsis: true
        },

        {
          title: 'Host',
          key: ['machineSsh', 'host'],
          width: 100,
          ellipsis: true
        },
        // { title: "Port", key: "machineSsh.port", sorter: true, width: 80, ellipsis: true, },
        {
          title: this.$t('i18n_819767ada1'),
          key: ['machineSsh', 'user'],
          width: '100px',
          ellipsis: true
        },

        {
          title: this.$t('i18n_cdc478d90c'),
          key: ['machineSsh', 'osName'],
          width: 80,
          ellipsis: true
        },
        // { title: "系统版本", key: "machineSsh.osVersion", sorter: true, ellipsis: true,},
        {
          title: 'CPU',
          key: ['machineSsh', 'osOccupyCpu'],
          width: 80,
          ellipsis: true
        },
        {
          title: this.$t('i18n_9932551cd5'),
          key: ['machineSsh', 'osOccupyMemory'],
          width: 80,
          ellipsis: true
        },
        {
          title: this.$t('i18n_1d650a60a5'),
          key: ['machineSsh', 'osMaxOccupyDisk'],
          width: 80,
          ellipsis: true
        },
        // { title: "编码格式", key: "charset", sorter: true, width: 120, ellipsis: true,  },
        {
          title: this.$t('i18n_7912615699'),
          key: ['machineSsh', 'status'],
          ellipsis: true,
          align: 'center',
          width: '90px'
        },
        // { title: "编码格式", key: "machineSsh.charset", sorter: true, width: 120, ellipsis: true, },
        {
          title: this.$t('i18n_222316382d'),
          key: 'nodeId',

          width: '100px',
          ellipsis: true
        },
        {
          title: this.$t('i18n_eca37cb072'),
          key: 'createTimeMillis',
          ellipsis: true,
          sorter: true,
          render: (row) => parseTime(row['createTimeMillis']),
          width: '170px'
        },
        {
          title: this.$t('i18n_1303e638b5'),
          key: 'modifyTimeMillis',
          sorter: true,
          ellipsis: true,
          render: (row) => parseTime(row['modifyTimeMillis']),
          width: '170px'
        },
        {
          title: this.$t('i18n_2b6bc0f293'),
          key: 'operation',

          width: '200px',
          align: 'center',
          // ellipsis: true,
          fixed: 'right'
        }
      ],

      // 表单校验规则
      rules: {
        name: [{ required: true, message: this.$t('i18n_9f6fa346d8'), trigger: 'blur' }]
      },

      groupList: [],
      confirmLoading: false
    }
  },
  computed: {
    ...mapState(useUserStore, ['getUserInfo']),
    ...mapState(useAppStore, ['getWorkspaceId']),
    pagination() {
      return COMPUTED_PAGINATION(this.listQuery)
    },
    rowSelection() {
      return {
        onChange: (selectedRowKeys) => {
          this.tableSelections = selectedRowKeys
        },
        selectedRowKeys: this.tableSelections
      }
    },
    activePage() {
      return this.$attrs.routerUrl === this.$route.path
    },
    useSuggestions() {
      if (this.loading) {
        // 加载中不提示
        return false
      }
      if (!this.getUserInfo || !this.getUserInfo.systemUser) {
        // 没有登录或者不是超级管理员
        return false
      }
      if (this.listQuery.page !== 1 || this.listQuery.total > 0) {
        // 不是第一页 或者总记录数大于 0
        return false
      }
      // 判断是否存在搜索条件
      const nowKeys = Object.keys(this.listQuery)
      const defaultKeys = Object.keys(PAGE_DEFAULT_LIST_QUERY)
      const dictOrigin = nowKeys.filter((item) => !defaultKeys.includes(item))
      return dictOrigin.length === 0
    }
  },
  created() {
    this.loadData()
    this.loadGroupList()
  },
  methods: {
    formatPercent2Number,
    renderSize,
    formatPercent,
    formatDuration,
    // 加载数据
    loadData(pointerEvent) {
      this.loading = true
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page
      getSshList(this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data.result
          this.listQuery.total = res.data.total
          //
        }
        this.loading = false
      })
    },
    // 获取所有的分组
    loadGroupList() {
      getSshGroupAll().then((res) => {
        if (res.data) {
          this.groupList = res.data
        }
      })
    },
    // 修改
    handleEdit(record) {
      this.temp = Object.assign({}, { id: record.id, group: record.group, name: record.name })

      this.editSshVisible = true
      // @author jzy 08-04
      this.$refs['editSshForm'] && this.$refs['editSshForm'].restoreValidation()
    },
    // 提交 SSH 数据
    handleEditSshOk() {
      // 检验表单
      this.$refs['editSshForm'].validate().then(() => {
        // 提交数据
        this.confirmLoading = true
        editSsh(this.temp)
          .then((res) => {
            if (res.code === 200) {
              $notification.success({
                message: res.msg
              })
              //this.$refs['editSshForm'].restoreValidation();
              // this.fileList = [];
              this.editSshVisible = false
              this.loadData()
              this.loadGroupList()
            }
          })
          .finally(() => {
            this.confirmLoading = false
          })
      }).catch(() => {})
    },
    // 进入终端
    handleTerminal(record, terminalFullscreen) {
      this.temp = Object.assign({}, record)
      this.terminalVisible = true
      this.terminalFullscreen = terminalFullscreen
    },
    // 新标签页打开终端
    handleFullTerminal(record) {
      const newpage = this.$router.resolve({
        path: '/full-terminal',
        query: { id: record.id, wid: this.getWorkspaceId() }
      })
      window.open(newpage.href, '_blank')
    },
    // 操作日志
    handleViewLog(record) {
      this.temp = Object.assign({}, record)
      this.viewOperationLog = true
    },

    // 文件管理
    handleFile(record) {
      this.temp = Object.assign({}, { id: record.id, group: record.group, name: record.name })

      this.drawerVisible = true
    },
    // 删除
    handleDelete(record) {
      $confirm({
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        content: this.$t('i18n_99cba05f94'),
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
        onOk: () => {
          return deleteSsh(record.id).then((res) => {
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
    // 前往节点
    toNode(node) {
      const newpage = this.$router.resolve({
        path: '/node/list',
        query: {
          ...this.$route.query,
          nodeId: node.id,
          pId: 'manage',
          id: 'manageList',
          wid: node.workspaceId,
          searchNodeName: node.name
        }
      })
      window.open(newpage.href, '_blank')

      // this.$router.push({
      //   path: "/node/list",
      //   query: {
      //     ...this.$route.query,
      //     tipNodeId: nodeId,
      //   },
      // });
    },
    toSshTabs() {
      const newpage = this.$router.resolve({
        path: '/ssh-tabs',
        query: {
          wid: this.getWorkspaceId()
        }
      })
      window.open(newpage.href, '_blank')
    },
    // 分页、排序、筛选变化时触发
    changePage(pagination, filters, sorter) {
      this.listQuery = CHANGE_PAGE(this.listQuery, { pagination, sorter })
      this.loadData()
    },

    // 加载工作空间数据
    loadWorkSpaceListAll() {
      getWorkSpaceListAll().then((res) => {
        if (res.code === 200) {
          this.workspaceList = res.data
        }
      })
    },
    // 同步到其他工作情况
    syncToWorkspaceShow() {
      this.syncToWorkspaceVisible = true
      this.loadWorkSpaceListAll()
      this.temp = {
        workspaceId: undefined
      }
    },
    //
    handleSyncToWorkspace() {
      if (!this.temp.workspaceId) {
        $notification.warn({
          message: this.$t('i18n_b3bda9bf9e')
        })
        return false
      }
      // 同步
      this.confirmLoading = true
      syncToWorkspace({
        ids: this.tableSelections.join(','),
        toWorkspaceId: this.temp.workspaceId
      })
        .then((res) => {
          if (res.code === 200) {
            $notification.success({
              message: res.msg
            })
            this.tableSelections = []
            this.syncToWorkspaceVisible = false
            return false
          }
        })
        .finally(() => {
          this.confirmLoading = false
        })
    }
  }
}
</script>
