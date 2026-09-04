<template>
  <div>
    <CustomTable
      size="medium"
      is-show-tools
      default-auto-refresh
      :auto-refresh-time="30"
      :data="commandList"
      table-name="release-task-list"
      :active-page="activePage"
      :columns="columns"
      bordered
      :pagination="pagination"
      row-key="id"
      :scroll="{
        x: 'max-content'
      }"
      @change="
        (pagination, filters, sorter) => {
          listQuery = CHANGE_PAGE(listQuery, { pagination, sorter })
          loadData()
        }
      "
      @refresh="loadData"
    >
      <template #title>
        <n-space wrap class="search-box">
          <n-input
            v-model:value="listQuery['%name%']"
            :placeholder="$t('i18n_ce23a42b47')"
            class="search-input-item"
            @press-enter="loadData"
          />
          <n-select
            v-model:value="listQuery.status"
            filterable
            clearable
            :placeholder="$t('i18n_3fea7ca76c')"
            class="search-input-item"
            :options="Object.entries(statusMap).map(([key, val]) => ({ label: val, value: key }))"
          />
          <n-select
            v-model:value="listQuery.taskType"
            filterable
            clearable
            :placeholder="$t('i18n_8aa25f5fbe')"
            class="search-input-item"
            :options="Object.entries(taskTypeMap).map(([key, val]) => ({ label: val, value: key }))"
          />
          <n-tooltip>
            <template #trigger>
              <span class="tw">
                <n-button type="primary" :loading="loading" @click="loadData">{{ $t('i18n_e5f71fc31e') }}</n-button>
              </span>
            </template>
            $t('i18n_4838a3bd20')
          </n-tooltip>
          <n-button type="primary" @click="handleTemplate">{{ $t('i18n_938246ce8b') }}</n-button>
        </n-space>
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

        <template v-else-if="column.dataIndex === 'fileId'">
          <n-tooltip>
            <template #trigger>
              <span class="tw">
                <n-button text style="padding: 0" size="small" @click="handleViewFile(record)">{{
                  (text || '').slice(0, 8)
                }}</n-button>
              </span>
            </template>
            text
          </n-tooltip>
        </template>

        <template v-else-if="column.dataIndex === 'status'">
          <n-tag v-if="text === 2" color="green">{{ statusMap[text] || $t('i18n_1622dc9b6b') }}</n-tag>
          <n-tag v-else-if="text === 0 || text === 1" color="orange">{{
            statusMap[text] || $t('i18n_1622dc9b6b')
          }}</n-tag>
          <n-tag v-else-if="text === 4" color="blue">
            {{ statusMap[text] || $t('i18n_1622dc9b6b') }}
          </n-tag>
          <n-tag v-else-if="text === 3" color="red">{{ statusMap[text] || $t('i18n_1622dc9b6b') }}</n-tag>
          <n-tag v-else>{{ statusMap[text] || $t('i18n_1622dc9b6b') }}</n-tag>
        </template>
        <template v-else-if="column.dataIndex === 'taskType'">
          <span>{{ taskTypeMap[text] || $t('i18n_1622dc9b6b') }}</span>
        </template>
        <template v-else-if="column.dataIndex === 'fileType'">
          <span v-if="text == 2">{{ $t('i18n_28f6e7a67b') }}</span>
          <span v-else>{{ $t('i18n_26183c99bf') }}</span>
        </template>

        <template v-else-if="column.dataIndex === 'operation'">
          <n-space>
            <n-button type="primary" size="small" @click="handleView(record)">{{ $t('i18n_607e7a4f37') }}</n-button>

            <n-button type="primary" size="small" @click="handleRetask(record)">{{ $t('i18n_9e09315960') }}</n-button>
            <n-button
              type="primary"
              danger
              size="small"
              :disabled="!(record.status === 0 || record.status === 1)"
              @click="handleCancelTask(record)"
              >{{ $t('i18n_625fb26b4b') }}</n-button
            >
            <n-button
              type="primary"
              danger
              size="small"
              :disabled="record.status === 0 || record.status === 1"
              @click="handleDelete(record)"
              >{{ $t('i18n_2f4aaddde3') }}</n-button
            >
          </n-space>
        </template>
      </template>
    </CustomTable>
    <!-- 任务详情 -->
    <CustomDrawer
      v-if="detailsVisible"
      :title="$t('i18n_4a98bf0c68')"
      placement="right"
      :width="'80vw'"
      :open="detailsVisible"
      @close="
        () => {
          detailsVisible = false
        }
      "
    >
      <task-details-page v-if="detailsVisible" :task-id="temp.id" />
    </CustomDrawer>
    <!-- 重建任务 -->
    <CustomModal
      v-if="releaseFileVisible"
      v-model:open="releaseFileVisible"
      destroy-on-close
      :confirm-loading="confirmLoading"
      :title="$t('i18n_7e930b95ef')"
      width="70%"
      :mask-closable="false"
      @ok="handleReCrateTask"
    >
      <n-form ref="releaseFileForm" :rules="releaseFileRules" :model="temp">
        <n-form-item :label="$t('i18n_ce23a42b47')" path="name">
          <n-input v-model:value="temp.name" :placeholder="$t('i18n_5f4c724e61')" :max-length="50" />
        </n-form-item>

        <n-form-item :label="$t('i18n_f98994f7ec')" path="taskType">
          <n-radio-group v-model:value="temp.taskType" :disabled="true">
            <n-radio :value="0"> SSH </n-radio>
            <n-radio :value="1"> {{ $t('i18n_3bf3c0a8d6') }} </n-radio>
          </n-radio-group>
        </n-form-item>

        <n-form-item v-if="temp.taskType === 0" path="taskDataIds" :label="$t('i18n_b188393ea7')">
          <n-grid>
            <n-grid-item :span="22">
              <n-select
                v-model:value="temp.taskDataIds"
                filterable
                multiple
                :placeholder="$t('i18n_260a3234f2')"
                :options="sshList.map((ssh) => ({ label: ssh.name, value: ssh.id }))"
              />
            </n-grid-item>
            <n-grid-item :span="1" style="margin-left: 10px">
              <ReloadOutlined @click="loadSshList" />
            </n-grid-item>
          </n-grid>
        </n-form-item>
        <n-form-item v-else-if="temp.taskType === 1" path="taskDataIds" :label="$t('i18n_473badc394')">
          <n-grid>
            <n-grid-item :span="22">
              <n-select
                v-model:value="temp.taskDataIds"
                filterable
                multiple
                :placeholder="$t('i18n_f8a613d247')"
                :options="nodeList.map((ssh) => ({ label: ssh.name, value: ssh.id }))"
              />
            </n-grid-item>
            <n-grid-item :span="1" style="margin-left: 10px">
              <ReloadOutlined @click="loadNodeList" />
            </n-grid-item>
          </n-grid>
        </n-form-item>

        <n-form-item path="releasePathParent" :label="$t('i18n_dbb2df00cf')">
          <n-input v-model:value="temp.releasePath" :placeholder="$t('i18n_ee9a51488f')" :disabled="true" />
        </n-form-item>

        <n-form-item path="releasePathParent" :label="$t('i18n_a91ce167c1')">
          <n-input v-model:value="temp.fileId" :placeholder="$t('i18n_ea8a79546f')" />
        </n-form-item>

        <n-form-item :label="$t('i18n_cfb00269fd')" path="releaseBeforeCommand">
          <n-form-item>
            <n-tabs tab-position="right">
              <n-tab-pane name="before" :tab="$t('i18n_d0c879f900')">
                <code-editor
                  v-model:content="temp.beforeScript"
                  height="40vh"
                  :options="{
                    mode: 'shell'
                  }"
                ></code-editor>

                <div style="margin-top: 10px">{{ $t('i18n_00de0ae1da') }}</div>
              </n-tab-pane>
              <n-tab-pane name="after" :tab="$t('i18n_9b1c5264a0')">
                <code-editor
                  v-model:content="temp.afterScript"
                  height="40vh"
                  :options="{
                    mode: 'shell'
                  }"
                ></code-editor>

                <div style="margin-top: 10px">{{ $t('i18n_08ac1eace7') }}</div>
              </n-tab-pane>
            </n-tabs>
          </n-form-item>
        </n-form-item>
      </n-form>
    </CustomModal>
    <!-- 查看文件 -->
    <CustomModal
      v-if="viewFileVisible"
      v-model:open="viewFileVisible"
      destroy-on-close
      :title="`${$t('i18n_9de72a79fe')}`"
      :footer="null"
      :mask-closable="false"
    >
      <n-form :model="temp">
        <n-form-item :label="$t('i18n_29139c2a1a')" path="name">
          {{ temp.name }}
        </n-form-item>
        <n-form-item :label="$t('i18n_0ff425e276')" path="name">
          {{ temp.id }}
        </n-form-item>
        <n-form-item :label="$t('i18n_396b7d3f91')" path="size">
          {{ renderSize(temp.size) }}
        </n-form-item>
        <n-form-item v-if="temp.validUntil" :label="$t('i18n_1fa23f4daa')" path="validUntil">
          {{ parseTime(temp.validUntil) }}
        </n-form-item>
        <n-form-item v-if="temp.workspaceId" :label="$t('i18n_3a6970ac26')" path="global">
          {{ temp.workspaceId === 'GLOBAL' ? $t('i18n_2be75b1044') : $t('i18n_98d69f8b62') }}
        </n-form-item>
        <n-form-item :label="$t('i18n_8d6f38b4b1')" path="description">
          {{ temp.description }}
        </n-form-item>
      </n-form>
    </CustomModal>
    <!-- 查看发布模板 -->
    <CustomModal
      v-if="templateVisible"
      v-model:open="templateVisible"
      width="80%"
      height="80%"
      destroy-on-close
      :title="$t('i18n_ce1c5765e4')"
      :footer="null"
      :mask-closable="false"
    >
      <templateList v-if="templateVisible"></templateList>
    </CustomModal>
  </div>
</template>
<script>
import { ReloadOutlined } from '@ant-design/icons-vue'

import {
  fileReleaseTaskLog,
  statusMap,
  taskTypeMap,
  taskDetails,
  reReleaseTask,
  cancelReleaseTask,
  deleteReleaseTask
} from '@/api/file-manager/release-task-log'
import { CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY, parseTime, renderSize } from '@/utils/const'
import taskDetailsPage from './details.vue'
import { getSshListAll } from '@/api/ssh'
import codeEditor from '@/components/codeEditor'
import templateList from './template-list.vue'
import { hasFile } from '@/api/file-manager/file-storage'
import { getNodeListAll } from '@/api/node'
import { hasStaticFile } from '@/api/file-manager/static-storage'
export default {
  components: {
    taskDetailsPage,
    codeEditor,
    templateList
  },
  data() {
    return {
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      commandList: [],
      loading: false,
      temp: {},
      statusMap,
      taskTypeMap,
      detailsVisible: false,
      confirmLoading: false,
      columns: [
        {
          title: this.$t('i18n_78caf7115c'),
          key: 'name',
          ellipsis: true,
          width: 150
        },
        {
          title: this.$t('i18n_9e2e02ef08'),
          key: 'taskType',
          width: '100px',
          ellipsis: true
        },
        {
          title: this.$t('i18n_9d577fe51b'),
          key: 'fileType',
          width: '100px',
          ellipsis: true
        },
        {
          title: this.$t('i18n_3fea7ca76c'),
          key: 'status',
          width: '100px',
          ellipsis: true
        },

        {
          title: this.$t('i18n_920f05031b'),
          key: 'statusMsg',
          ellipsis: true,
          width: 200
        },
        {
          title: this.$t('i18n_0ff425e276'),
          key: 'fileId',
          ellipsis: true,
          width: 150
        },
        {
          title: this.$t('i18n_dbb2df00cf'),
          key: 'releasePath',
          width: '100px',
          ellipsis: true
        },
        {
          title: this.$t('i18n_a497562c8e'),
          key: 'modifyUser',
          width: '120px',
          ellipsis: true
        },
        {
          title: this.$t('i18n_b341f9a861'),
          key: 'createTimeMillis',
          sorter: true,
          ellipsis: true,
          render: (row) => parseTime(row['createTimeMillis']),
          width: '170px'
        },
        {
          title: this.$t('i18n_4871f7722d'),
          key: 'modifyTimeMillis',
          sorter: true,
          ellipsis: true,
          render: (row) => parseTime(row['modifyTimeMillis']),
          width: '170px'
        },

        {
          title: this.$t('i18n_2b6bc0f293'),
          key: 'operation',
          align: 'center',

          fixed: 'right',
          width: '230px'
        }
      ],

      sshList: [],
      nodeList: [],
      releaseFileVisible: false,
      releaseFileRules: {
        name: [{ required: true, message: this.$t('i18n_89d18c88a3'), trigger: 'blur' }],

        taskDataIds: [{ required: true, type: 'array', message: this.$t('i18n_3e51d1bc9c'), trigger: ['blur', 'change'] }]
      },
      viewFileVisible: false,
      templateVisible: false
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
    this.loadData()
  },
  methods: {
    CHANGE_PAGE,
    renderSize,
    parseTime,
    handleView(row) {
      this.temp = { ...row }
      this.detailsVisible = true
    },
    handleTemplate() {
      this.templateVisible = true
    },
    // 获取命令数据
    loadData(pointerEvent) {
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page
      this.loading = true
      fileReleaseTaskLog(this.listQuery).then((res) => {
        if (200 === res.code) {
          this.commandList = res.data.result
          this.listQuery.total = res.data.total
        }
        this.loading = false
      })
    },

    //  删除命令
    handleDelete(row) {
      $confirm({
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        content: this.$t('i18n_50fe3400c7'),
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
        onOk: () => {
          return deleteReleaseTask({
            id: row.id
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
    // 加载 SSH 列表
    loadSshList() {
      return new Promise((resolve) => {
        this.sshList = []
        getSshListAll().then((res) => {
          if (res.code === 200) {
            this.sshList = res.data
            resolve()
          }
        })
      })
    },
    // 加载节点
    loadNodeList() {
      getNodeListAll().then((res) => {
        if (res.code === 200) {
          this.nodeList = res.data
        }
      })
    },
    // 重建任务
    handleRetask(row) {
      taskDetails({
        id: row.id
      }).then((res) => {
        if (res.code === 200) {
          const taskData = res.data?.taskData
          this.temp = taskData
          delete this.temp.statusMsg
          delete this.temp.id
          if (taskData?.taskType === 0) {
            this.loadSshList()
          } else if (taskData?.taskType === 1) {
            this.loadNodeList()
          }
          const taskList = res.data?.taskList || []
          this.temp = {
            ...this.temp,
            taskDataIds: taskList.map((item) => {
              return item.taskDataId
            }),
            parentTaskId: row.id
          }
          this.releaseFileVisible = true
        }
      })
    },
    // 创建任务
    handleReCrateTask() {
      this.$refs['releaseFileForm'].validate().then(() => {
        this.confirmLoading = true
        reReleaseTask({
          ...this.temp,
          taskDataIds: this.temp.taskDataIds?.join(',')
        })
          .then((res) => {
            if (res.code === 200) {
              // 成功
              $notification.success({
                message: res.msg
              })

              this.releaseFileVisible = false
              this.loadData()
            }
          })
          .finally(() => {
            this.confirmLoading = false
          })
      }).catch(() => {})
    },
    // 取消
    handleCancelTask(record) {
      $confirm({
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        content: this.$t('i18n_7824ed010c'),
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
        oonOk: () => {
          return cancelReleaseTask({ id: record.id }).then((res) => {
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
    // 查看文件
    handleViewFile(record) {
      if (record.fileType === 2) {
        //
        hasStaticFile({
          fileId: record.fileId
        }).then((res) => {
          if (res.code === 200) {
            if (res.data) {
              this.temp = res.data
              this.viewFileVisible = true
            } else {
              $notification.warning({
                message: this.$t('i18n_3e445d03aa')
              })
            }
          }
        })
      } else {
        hasFile({
          fileSumMd5: record.fileId
        }).then((res) => {
          if (res.code === 200) {
            if (res.data) {
              this.temp = res.data
              this.viewFileVisible = true
            } else {
              $notification.warning({
                message: this.$t('i18n_3e445d03aa')
              })
            }
          }
        })
      }
    }
  }
}
</script>
