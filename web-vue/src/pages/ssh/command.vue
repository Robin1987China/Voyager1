<template>
  <div>
    <CustomTable
      is-show-tools
      default-auto-refresh
      :auto-refresh-time="30"
      :active-page="activePage"
      table-name="ssh-command-list"
      :empty-description="$t('i18n_ba17b17ba2')"
      :data="commandList"
      :columns="columns"
      size="medium"
      bordered
      :pagination="pagination"
      :row-selection="rowSelection"
      row-key="id"
      :scroll="{
        x: 'max-content'
      }"
      @change="changePage"
      @refresh="getCommandData"
    >
      <template #title>
        <n-space wrap class="search-box">
          <n-input
            v-model:value="listQuery['%name%']"
            :placeholder="$t('i18n_9c2a917905')"
            class="search-input-item"
            @press-enter="getCommandData"
          />
          <n-input
            v-model:value="listQuery['%desc%']"
            :placeholder="$t('i18n_3bdd08adab')"
            class="search-input-item"
            @press-enter="getCommandData"
          />
          <n-input
            v-model:value="listQuery['%autoExecCron%']"
            :placeholder="$t('i18n_6b2e348a2b')"
            class="search-input-item"
            @press-enter="getCommandData"
          />
          <n-tooltip>
            <template #trigger>
              <span class="tw">
                <n-button type="primary" :loading="loading" @click="getCommandData">{{
                  $t('i18n_e5f71fc31e')
                }}</n-button>
              </span>
            </template>
            $t('i18n_4838a3bd20')
          </n-tooltip>
          <n-button type="primary" @click="createCommand">{{ $t('i18n_66ab5e9f24') }}</n-button>
          <n-dropdown
            :options="[
              {
                label: $t('i18n_398ce396cd'),
                key: '0',
                disabled: !tableSelections || !tableSelections.length,
                props: { onClick: () => syncToWorkspaceShow }
              }
            ]"
          >
            <a @click="(e) => e.preventDefault()"> {{ $t('i18n_0ec9eaf9c3') }} <DownOutlined /> </a>
          </n-dropdown>
        </n-space>
      </template>
      <template #tableHelp>
        <n-tooltip>
          <template #trigger>
            <QuestionCircleOutlined />
          </template>

          <div>{{ $t('i18n_4826549b41') }}</div>

          <div>
            <ul>
              <li>{{ $t('i18n_5ef72bdfce') }}</li>
              <li>{{ $t('i18n_5d368ab0a5') }}</li>
              <li>
                {{ $t('i18n_26f95520a5') }}<b>#disabled-template-auto-evn</b> {{ $t('i18n_bfacfcd978') }}({{
                  $t('i18n_8e872df7da')
                }})
              </li>
              <li>{{ $t('i18n_2ea7e70e87') }}</li>
            </ul>
          </div>
        </n-tooltip>
      </template>
      <template #tableBodyCell="{ column, text, record }">
        <template v-if="column.dataIndex === 'name'">
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
        <template v-else-if="column.dataIndex === 'desc'">
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

        <template v-else-if="column.dataIndex === 'operation'">
          <n-space>
            <n-button size="small" type="primary" @click="handleExecute(record)">{{ $t('i18n_1a6aa24e76') }}</n-button>
            <n-button size="small" type="primary" @click="handleEdit(record)">{{ $t('i18n_95b351c862') }}</n-button>
            <n-button size="small" type="primary" @click="handleTrigger(record)">{{ $t('i18n_4696724ed3') }}</n-button>
            <n-button size="small" type="primary" danger @click="handleDelete(record)">{{
              $t('i18n_2f4aaddde3')
            }}</n-button>
          </n-space>
        </template>
      </template>
    </CustomTable>
    <!-- 编辑命令 -->
    <CustomModal
      v-if="editCommandVisible"
      v-model:open="editCommandVisible"
      destroy-on-close
      width="80vw"
      :title="$t('i18n_9a0c5b150c')"
      :mask-closable="false"
      :confirm-loading="confirmLoading"
      @ok="handleEditCommandOk"
    >
      <n-form ref="editCommandForm" :rules="rules" :model="temp">
        <n-form-item :label="$t('i18n_6496a5a043')" path="name">
          <n-input v-model:value="temp.name" :max-length="100" :placeholder="$t('i18n_6496a5a043')" />
        </n-form-item>

        <n-form-item path="command" :help="$t('i18n_77c1e73c08')">
          <template #label>
            <n-tooltip>
              <template #trigger>
                <span class="tw">
                  {{ $t('i18n_ccb91317c5') }}

                  <QuestionCircleOutlined v-show="!temp.id" />
                </span>
              </template>

              <ul>
                <li>{{ $t('i18n_5fbde027e3') }}</li>
              </ul>
            </n-tooltip>
          </template>

          <n-form-item>
            <code-editor
              v-model:content="temp.command"
              height="40vh"
              :options="{ mode: 'shell', tabSize: 2 }"
              :show-tool="true"
            >
              <template #tool_before>
                <n-button text @click="scriptLibraryVisible = true">{{ $t('i18n_f685377a22') }}</n-button>
              </template>
            </code-editor>
          </n-form-item>
        </n-form-item>
        <n-form-item :label="$t('i18n_b0b9df58fd')">
          <n-select
            v-model:value="chooseSsh"
            filterable
            :placeholder="$t('i18n_649f8046f3')"
            multiple
            :options="sshList.map((item) => ({ label: item.name, value: item.id }))"
          />
        </n-form-item>

        <n-form-item :label="$t('i18n_2171d1b07d')">
          <n-form-item>
            <n-space direction="vertical" style="width: 100%">
              <n-grid v-for="(item, index) in commandParams" :key="item.key">
                <n-grid-item :span="22">
                  <n-space direction="vertical" style="width: 100%">
                    <n-input
                      v-model:value="item.desc"
                      :addon-before="$t('i18n_417fa2c2be', { index: index + 1 })"
                      :placeholder="$t('i18n_3f414ade96', { slot1: $t('i18n_2b1015e902') })"
                    />
                    <n-input
                      v-model:value="item.value"
                      :addon-before="$t('i18n_620489518c', { index: index + 1 })"
                      :placeholder="`${$t('i18n_bfed4943c5')}${$t('i18n_e9f2c62e54')}`"
                    />
                  </n-space>
                </n-grid-item>

                <n-grid-item :span="2">
                  <n-grid type="flex" justify="center" align="middle">
                    <n-grid-item>
                      <MinusCircleOutlined style="color: #ff0000" @click="() => commandParams.splice(index, 1)" />
                    </n-grid-item>
                  </n-grid>
                </n-grid-item>
              </n-grid>
              <n-button type="primary" @click="() => commandParams.push({})">{{ $t('i18n_4c0eead6ff') }}</n-button>
            </n-space>
          </n-form-item>
        </n-form-item>
        <n-form-item :label="$t('i18n_df39e42127')" path="autoExecCron">
          <n-auto-complete
            v-model:value="temp.autoExecCron"
            :placeholder="$t('i18n_5dff0d31d0')"
            :options="CRON_DATA_SOURCE"
          >
            <template #option="item"> {{ item.title }} {{ item.value }} </template>
          </n-auto-complete>
        </n-form-item>
        <n-form-item :label="$t('i18n_bf91239ad7')" path="desc">
          <n-input
            v-model:value="temp.desc"
            type="textarea"
            :max-length="255"
            :rows="3"
            style="resize: none"
            :placeholder="$t('i18n_81d7d5cd8a')"
          />
        </n-form-item>
      </n-form>
    </CustomModal>

    <CustomModal
      v-if="executeCommandVisible"
      v-model:open="executeCommandVisible"
      destroy-on-close
      width="600px"
      :title="$t('i18n_bb4740c7a7')"
      :mask-closable="false"
      :confirm-loading="confirmLoading"
      @ok="handleExecuteCommandOk"
    >
      <n-form :model="temp">
        <n-form-item :label="$t('i18n_6496a5a043')" path="name">
          <n-input v-model:value="temp.name" :disabled="true" :placeholder="$t('i18n_6496a5a043')" />
        </n-form-item>

        <n-form-item :label="$t('i18n_b0b9df58fd')" required>
          <n-select
            v-model:value="chooseSsh"
            filterable
            multiple
            :placeholder="$t('i18n_e43359ca06')"
            :options="sshList.map((item) => ({ label: item.name, value: item.id }))"
          />
        </n-form-item>

        <n-form-item :label="$t('i18n_abba4775e1')" :help="`${commandParams.length ? $t('i18n_916cde39c4') : ''}`">
          <n-space direction="vertical" style="width: 100%">
            <n-grid v-for="(item, index) in commandParams" :key="item.key">
              <n-grid-item :span="22">
                <n-input
                  v-model:value="item.value"
                  :addon-before="`${$t('i18n_3d0a2df9ec')}${index + 1}${$t('i18n_fe7509e0ed')}`"
                  :placeholder="`${$t('i18n_3d0a2df9ec')}${$t('i18n_fe7509e0ed')} ${item.desc ? ',' + item.desc : ''}`"
                >
                  <template #suffix>
                    <n-tooltip v-if="item.desc">
                      <template #trigger>
                        <InfoCircleOutlined />
                      </template>
                      item.desc
                    </n-tooltip>
                  </template>
                </n-input>
              </n-grid-item>

              <n-grid-item v-if="!item.desc" :span="2">
                <n-grid type="flex" justify="center" align="middle">
                  <n-grid-item>
                    <MinusCircleOutlined style="color: #ff0000" @click="() => commandParams.splice(index, 1)" />
                  </n-grid-item>
                </n-grid>
              </n-grid-item>
            </n-grid>
            <n-button type="primary" @click="() => commandParams.push({})">{{ $t('i18n_4c0eead6ff') }}</n-button>
          </n-space>
        </n-form-item>
      </n-form>
    </CustomModal>
    <!-- 执行日志 -->
    <CustomModal
      v-if="logVisible"
      v-model:open="logVisible"
      destroy-on-close
      :width="'80vw'"
      :title="$t('i18n_c84ddfe8a6')"
      :footer="null"
      :mask-closable="false"
    >
      <command-log v-if="logVisible" :temp="temp" />
    </CustomModal>
    <!-- 同步到其他工作空间 -->
    <CustomModal
      v-if="syncToWorkspaceVisible"
      v-model:open="syncToWorkspaceVisible"
      destroy-on-close
      :confirm-loading="confirmLoading"
      :title="$t('i18n_1a44b9e2f7')"
      :mask-closable="false"
      @ok="handleSyncToWorkspace"
    >
      <n-alert :title="$t('i18n_947d983961')" type="warning" show-icon>
        <template #description>
          <ul>
            <li>
              {{ $t('i18n_384f337da1') }}<b>{{ $t('i18n_50fb61ef9d') }}</b
              >{{ $t('i18n_50d2671541') }}
            </li>
            <li>{{ $t('i18n_770a07d78f') }}</li>
            <li>{{ $t('i18n_b5d2cf4a76') }}</li>
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

    <!-- 触发器 -->
    <CustomModal
      v-if="triggerVisible"
      v-model:open="triggerVisible"
      destroy-on-close
      :title="$t('i18n_4696724ed3')"
      width="50%"
      :footer="null"
      :mask-closable="false"
    >
      <n-form ref="editTriggerForm" :rules="rules" :model="temp">
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
              <n-alert :title="$t('i18n_947d983961')" type="warning">
                <template #description>
                  <ul>
                    <li>{{ $t('i18n_05e78c26b1') }}</li>
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
    <!-- pages.ssh.command.a36f20d3 -->
    <CustomDrawer
      v-if="scriptLibraryVisible"
      destroy-on-close
      :title="$t('i18n_53bdd93fd6')"
      placement="right"
      :open="scriptLibraryVisible"
      width="85vw"
      :footer-style="{ textAlign: 'right' }"
      @close="
        () => {
          scriptLibraryVisible = false
        }
      "
    >
      <ScriptLibraryNoPermission
        v-if="scriptLibraryVisible"
        ref="scriptLibraryRef"
        @script-confirm="
          (script) => {
            temp = { ...temp, command: script }
            scriptLibraryVisible = false
          }
        "
        @tag-confirm="
          (tag) => {
            temp = { ...temp, command: (temp.command || '') + `\nG@(\&quot;${tag}\&quot;)` }
            scriptLibraryVisible = false
          }
        "
      ></ScriptLibraryNoPermission>
      <template #footer>
        <n-space>
          <n-button
            @click="
              () => {
                scriptLibraryVisible = false
              }
            "
            >{{ $t('i18n_625fb26b4b') }}</n-button
          >
          <n-button
            type="primary"
            @click="
              () => {
                $refs['scriptLibraryRef'].handerScriptConfirm()
              }
            "
            >{{ $t('i18n_f71316d0dd') }}</n-button
          >
          <n-button
            type="primary"
            @click="
              () => {
                $refs['scriptLibraryRef'].handerTagConfirm()
              }
            "
            >{{ $t('i18n_9300692fac') }}</n-button
          >
        </n-space>
      </template>
    </CustomDrawer>
  </div>
</template>
<script>
import { DownOutlined, InfoCircleOutlined, MinusCircleOutlined, QuestionCircleOutlined } from '@ant-design/icons-vue'

import { h } from 'vue'
import { NIcon } from 'naive-ui'

import { deleteCommand, editCommand, executeBatch, getCommandList, syncToWorkspace, getTriggerUrl } from '@/api/command'
import { CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY, parseTime } from '@/utils/const'
import { CRON_DATA_SOURCE } from '@/utils/const-i18n'
import { getSshListAll } from '@/api/ssh'
import codeEditor from '@/components/codeEditor'
import CommandLog from './command-view-log'
import ScriptLibraryNoPermission from '@/pages/system/assets/script-library/no-permission'
import { getWorkSpaceListAll } from '@/api/workspace'
import { mapState } from 'pinia'
import { useAppStore } from '@/stores/app'
export default {
  components: { codeEditor, CommandLog, ScriptLibraryNoPermission },
  data() {
    return {
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      CRON_DATA_SOURCE,
      commandList: [],
      loading: false,
      editCommandVisible: false,
      executeCommandVisible: false,
      commandParams: [],
      sshList: [],
      chooseSsh: [],
      temp: {},
      logVisible: false,
      rules: {
        name: [{ required: true, message: 'Please input name', trigger: 'blur' }],
        command: [{ required: true, message: 'Please input command', trigger: 'blur' }]
      },
      columns: [
        {
          title: this.$t('i18n_6496a5a043'),
          key: 'name',
          ellipsis: true,
          width: 200
        },
        {
          title: this.$t('i18n_bf91239ad7'),
          key: 'desc',
          ellipsis: true,
          width: 250
        },
        {
          title: this.$t('i18n_6b2e348a2b'),
          key: 'autoExecCron',
          ellipsis: true,
          width: 120
        },
        {
          title: this.$t('i18n_eca37cb072'),
          key: 'createTimeMillis',
          ellipsis: true,
          sorter: true,
          render: (row) => {
            return parseTime(row['createTimeMillis'])
          },
          width: '170px'
        },
        {
          title: this.$t('i18n_1303e638b5'),
          key: 'modifyTimeMillis',
          width: '170px',
          ellipsis: true,
          sorter: true,
          render: (row) => {
            return parseTime(row['modifyTimeMillis'])
          }
        },
        {
          title: this.$t('i18n_26c1f8d83e'),
          key: 'modifyUser',
          width: 120,
          ellipsis: true
        },
        {
          title: this.$t('i18n_2b6bc0f293'),
          key: 'operation',
          align: 'center',

          fixed: 'right',
          width: '240px'
        }
      ],

      scriptLibraryVisible: false,
      tableSelections: [],
      syncToWorkspaceVisible: false,
      workspaceList: [],
      triggerVisible: false,
      confirmLoading: false
    }
  },
  computed: {
    ...mapState(useAppStore, ['getWorkspaceId']),
    pagination() {
      return COMPUTED_PAGINATION(this.listQuery)
    },
    activePage() {
      return this.$attrs.routerUrl === this.$route.path
    },
    rowSelection() {
      return {
        onChange: (selectedRowKeys) => {
          this.tableSelections = selectedRowKeys
        },
        selectedRowKeys: this.tableSelections
      }
    }
  },
  mounted() {
    this.getCommandData()
    //this.getAllSSHList();
  },
  methods: {
    // 编辑命令信息
    handleEditCommandOk() {
      this.$refs['editCommandForm'].validate().then(() => {
        if (this.commandParams && this.commandParams.length > 0) {
          for (let i = 0; i < this.commandParams.length; i++) {
            if (!this.commandParams[i].desc) {
              $notification.error({
                message: this.$t('i18n_8ae2b9915c') + (i + 1) + this.$t('i18n_c583b707ba')
              })
              return false
            }
          }
          this.temp.defParams = JSON.stringify(this.commandParams)
        } else {
          this.temp.defParams = ''
        }
        this.temp.sshIds = this.chooseSsh.join(',')
        this.confirmLoading = true
        editCommand(this.temp)
          .then((res) => {
            if (res.code === 200) {
              $notification.success({
                message: res.msg
              })
              this.editCommandVisible = false

              this.getCommandData()
            }
          })
          .finally(() => {
            this.confirmLoading = false
          })
      }).catch(() => {})
    },
    // 获取命令数据
    getCommandData(pointerEvent) {
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page
      this.loading = true
      getCommandList(this.listQuery).then((res) => {
        if (200 === res.code) {
          this.commandList = res.data.result
          this.listQuery.total = res.data.total
        }
        this.loading = false
      })
    },
    // 分页、排序、筛选变化时触发
    changePage(pagination, filters, sorter) {
      this.listQuery = CHANGE_PAGE(this.listQuery, { pagination, sorter })
      this.getCommandData()
    },

    // 创建命令弹窗
    createCommand() {
      this.editCommandVisible = true
      this.getAllSSHList()
      this.chooseSsh = []
      this.commandParams = []
      this.temp = {}
      this.$refs['editCommandForm'] && this.$refs['editCommandForm'].restoreValidation()
    },
    // 修改
    handleEdit(rowData) {
      const row = Object.assign({}, rowData)
      this.editCommandVisible = true
      this.$refs['editCommandForm'] && this.$refs['editCommandForm'].restoreValidation()
      this.commandParams = []
      if (row.defParams) {
        this.commandParams = JSON.parse(row.defParams)
      }
      this.temp = row
      this.chooseSsh = row.sshIds ? row.sshIds.split(',') : []
      this.getAllSSHList()
    },
    // 执行命令
    handleExecute(rowData) {
      const row = Object.assign({}, rowData)
      if (typeof row.defParams === 'string' && row.defParams) {
        this.commandParams = JSON.parse(row.defParams)
      } else {
        this.commandParams = []
      }
      this.temp = row
      this.chooseSsh = row.sshIds ? row.sshIds.split(',') : []
      this.executeCommandVisible = true
      this.getAllSSHList()
    },
    //  删除命令
    handleDelete(row) {
      $confirm({
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        content: this.$t('i18n_d921c4a0b6') + row.name + this.$t('i18n_c4a61acace'),
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
        onOk: () => {
          return deleteCommand(row.id).then((res) => {
            if (res.code === 200) {
              $notification.success({
                message: res.msg
              })
              this.getCommandData()
            }
          })
        }
      })
    },
    // 获取所有ssh接点
    getAllSSHList() {
      getSshListAll().then((res) => {
        this.sshList = res.data || []
      })
    },

    handleExecuteCommandOk() {
      if (!this.chooseSsh || this.chooseSsh.length <= 0) {
        $notification.error({
          message: this.$t('i18n_d7471c0261')
        })
        return false
      }
      this.confirmLoading = true
      executeBatch({
        id: this.temp.id,
        params: JSON.stringify(this.commandParams),
        nodes: this.chooseSsh.join(',')
      })
        .then((res) => {
          if (res.code === 200) {
            $notification.success({
              message: res.msg
            })
            this.executeCommandVisible = false
            this.temp = {
              commandId: this.temp.id,
              batchId: res.data
            }
            this.logVisible = true
          }
        })
        .finally(() => {
          this.confirmLoading = false
        })
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
    }
  }
}
</script>
<style scoped>
.config-editor {
  overflow-y: scroll;
  max-height: 300px;
}
</style>
