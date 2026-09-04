<template>
  <div>
    <CustomDrawer
      destroy-on-close
      :title="`${$t('i18n_607e7a4f37')} ${name} ${$t('i18n_3fea7ca76c')}`"
      placement="right"
      width="85vw"
      :open="true"
      @close="
        () => {
          $emit('close')
        }
      "
    >
      <n-tabs v-model:value="tabKey" tab-position="left">
        <n-tab-pane name="1" :tab="$t('i18n_3fea7ca76c')">
          <!-- 嵌套表格 -->
                    <n-card size="small" :body-style="{ padding: '12px' }" style="margin-bottom: 12px">

              <n-space>
                <div>
                  {{ $t('i18n_e703c7367c') }}
                  <n-tag v-if="data.status === 2" color="green">{{
                    statusMap[data.status] || $t('i18n_1622dc9b6b')
                  }}</n-tag>
                  <n-tag v-else-if="data.status === 1 || data.status === 0" color="orange">{{
                    statusMap[data.status] || $t('i18n_1622dc9b6b')
                  }}</n-tag>
                  <n-tag v-else-if="data.status === 3 || data.status === 4" color="red">{{
                    statusMap[data.status] || $t('i18n_1622dc9b6b')
                  }}</n-tag>
                  <n-tag v-else>{{ statusMap[data.status] || $t('i18n_1622dc9b6b') }}</n-tag>
                </div>
                <div>{{ $t('i18n_fb3a2241bb') }}{{ data.statusMsg || '-' }}</div>
                <n-button type="primary" size="small" :loading="childLoading" @click="loadData">{{
                  $t('i18n_694fc5efa9')
                }}</n-button>
                <n-statistic format="s" :title="$t('i18n_0f8403d07e')" :value="countdownTime" @finish="silenceLoadData">
                  <template #suffix>
                    <div style="font-size: 12px">{{ $t('i18n_ee6ce96abb') }}</div>
                  </template>
                </n-statistic>
              </n-space>
            
          </n-card>
<n-data-table
            :loading="childLoading"
            :columns="childColumns"
            size="medium"
            :bordered="true"
            :data="list"
            :pagination="false"
            :row-key="(row) => row.id_no"
            >
            
            <template #bodyCell="{ column, text, record }">
              <template v-if="column.key === 'nodeId'">
                <n-tooltip placement="topLeft">
                  <template #trigger>
                    <span class="tw">
                      <span class="tw">
                        <n-button text style="padding: 0" size="small" @click="toNode(text)">
                          <span>{{ nodeNameMap[text] || text }}</span>
                          <FullscreenOutlined />
                        </n-button>
                      </span>
                    </span>
                  </template>
                  text
                </n-tooltip>
              </template>
              <template v-else-if="column.key === 'projectName'">
                <n-tooltip placement="top-start">
                  <template #trigger>
                    <span class="tw">
                      <template v-if="record.disabled">
                        <n-tooltip>
                          <template #trigger>
                            <EyeInvisibleOutlined />
                          </template>
                          {{ $t('i18n_f8b3165e0d') }}
                        </n-tooltip>
                      </template>
                      <span>{{ text || record.cacheProjectName }}</span>
                    </span>
                  </template>
                  {{ text }}
                </n-tooltip>
              </template>
              <template v-else-if="column.key === 'outGivingStatus'">
                <n-tag v-if="text === 2" color="green">{{ dispatchStatusMap[text] || $t('i18n_1622dc9b6b') }}</n-tag>
                <n-tag v-else-if="text === 1 || text === 0 || text === 5" color="orange">{{
                  dispatchStatusMap[text] || $t('i18n_1622dc9b6b')
                }}</n-tag>
                <n-tag v-else-if="text === 3 || text === 4 || text === 6" color="red">{{
                  dispatchStatusMap[text] || $t('i18n_1622dc9b6b')
                }}</n-tag>
                <n-tag v-else>{{ dispatchStatusMap[text] || $t('i18n_1622dc9b6b') }}</n-tag>
              </template>
              <template v-else-if="column.key === 'outGivingResultMsg'">
                <n-tooltip placement="topLeft">
                  <template #trigger>
                    <span class="tw">
                      <span class="tw">
                        <span
                          >{{ readJsonStrField(record.outGivingResult, 'code') }}-{{
                            readJsonStrField(record.outGivingResult, 'msg') || record.outGivingResult
                          }}</span
                        >
                      </span>
                    </span>
                  </template>
                  readJsonStrField(record.outGivingResult, 'msg')
                </n-tooltip>
              </template>
              <template v-else-if="column.key === 'outGivingResultTime'">
                <n-tooltip placement="topLeft">
                  <template #trigger>
                    <span class="tw">
                      <span class="tw">
                        <span>{{ readJsonStrField(record.outGivingResult, 'upload_duration') }}</span>
                      </span>
                    </span>
                  </template>
                  readJsonStrField(record.outGivingResult, 'upload_duration')
                </n-tooltip>
              </template>
              <template v-else-if="column.key === 'outGivingResultSize'">
                <n-tooltip placement="topLeft">
                  <template #trigger>
                    {{ readJsonStrField(record.outGivingResult, 'upload_file_size') }}
                  </template>
                  readJsonStrField(record.outGivingResult, 'upload_file_size')
                </n-tooltip>
              </template>
              <template v-else-if="column.key === 'outGivingResultMsgData'">
                <n-tooltip placement="top-start">
                  <template #trigger>
                    <template v-if="record.fileSize">
                      {{ Math.floor((record.progressSize / record.fileSize) * 100) }}%
                    </template>
                    {{ readJsonStrField(record.outGivingResult, 'data') }}
                  </template>
                  {{ `${readJsonStrField(record.outGivingResult, 'data')}` }}
                </n-tooltip>
              </template>

              <template v-else-if="column.key === 'projectStatus'">
                <n-tooltip v-if="record.errorMsg">
                  <template #trigger>
                    <WarningOutlined />
                  </template>
                  record.errorMsg
                </n-tooltip>
                <n-switch
                  v-else
                  :value="text"
                  :disabled="true"
                  size="small"
                  :checked-label="$t('i18n_d679aea3aa')"
                  :unchecked-label="$t('i18n_4f8a2f0b28')"
                />
              </template>

              <template v-else-if="column.key === 'projectPid'">
                <n-tooltip placement="topLeft">
                  <template #trigger>
                    <span class="tw">
                      <span class="tw">
                        <span>{{ record.projectPid || '-' }}/{{ record.projectPort || '-' }}</span>
                      </span>
                    </span>
                  </template>
                  `${$t('i18n_2b04210d33')}${record.projectPid || '-'} / ${$t('i18n_4c096c51a3')}${ record.projectPort
                  || '-' }`
                </n-tooltip>
              </template>

              <template v-else-if="column.key === 'child-operation'">
                <n-space>
                  <n-button size="small" :disabled="!record.projectName" type="primary" @click="handleFile(record)">{{
                    $t('i18n_2a0c4740f1')
                  }}</n-button>
                  <n-button
                    size="small"
                    :disabled="!record.projectName"
                    type="primary"
                    @click="handleConsole(record)"
                    >{{ $t('i18n_b5c3770699') }}</n-button
                  >
                </n-space>
              </template>
            </template>
          </n-data-table>
        </n-tab-pane>
        <n-tab-pane name="2" :tab="$t('i18n_224e2ccda8')">
          <!-- 配置分发 -->
          <div style="width: 50vw">
            <!-- list -->
            <Container drag-handle-selector=".move" orientation="vertical" @drop="onDrop">
              <Draggable v-for="(element, index) in list" :key="index">
                <n-grid class="item-row">
                  <n-grid-item :span="18">
                    <span> {{ $t('i18n_dc2961a26f') }} {{ element.nodeName }} </span>
                    <span> {{ $t('i18n_26ffe89a7f') }} {{ element.cacheProjectName }} </span>
                  </n-grid-item>
                  <n-grid-item :span="6">
                    <n-space>
                      <n-switch
                        :checked-label="$t('i18n_7854b52a88')"
                        :unchecked-label="$t('i18n_710ad08b11')"
                        :value="element.disabled ? false : true"
                        @change="
                          (checked) => {
                            list = list.map((item2) => {
                              if (element.id === item2.id) {
                                item2.disabled = !checked
                              }
                              return { ...item2 }
                            })
                          }
                        "
                      />

                      <n-button
                        type="primary"
                        danger
                        size="small"
                        :disabled="!list || list.length <= 1"
                        @click="handleRemoveProject(element)"
                      >
                        {{ $t('i18n_663393986e') }}
                      </n-button>
                      <n-tooltip placement="left" class="move">
                        <template #trigger>
                          <MenuOutlined />
                        </template>
                        `${$t('i18n_181e1ad17d')}`
                      </n-tooltip>
                    </n-space>
                  </n-grid-item>
                </n-grid>
              </Draggable>
            </Container>
            <n-grid-item style="margin-top: 10px">
              <n-space>
                <n-button type="primary" size="small" @click="viewDispatchManagerOk">{{
                  $t('i18n_be5fbbe34c')
                }}</n-button>
              </n-space>
            </n-grid-item>
          </div>
        </n-tab-pane>
      </n-tabs>
    </CustomDrawer>

    <!-- 项目文件组件 -->
    <CustomDrawer
      v-if="drawerFileVisible"
      destroy-on-close
      :title="drawerTitle"
      placement="right"
      width="85vw"
      :open="drawerFileVisible"
      @close="onFileClose"
    >
      <file
        v-if="drawerFileVisible"
        :id="temp.id"
        :node-id="temp.nodeId"
        :project-id="temp.projectId"
        @go-console="goConsole"
        @go-read-file="goReadFile"
      />
    </CustomDrawer>
    <!-- 项目控制台组件 -->
    <CustomDrawer
      v-if="drawerConsoleVisible"
      destroy-on-close
      :title="drawerTitle"
      placement="right"
      width="85vw"
      :open="drawerConsoleVisible"
      @close="onConsoleClose"
    >
      <console
        v-if="drawerConsoleVisible"
        :id="temp.id"
        :node-id="temp.nodeId"
        :project-id="temp.projectId"
        @go-file="goFile"
      />
    </CustomDrawer>
    <!-- 项目跟踪文件组件 -->
    <CustomDrawer
      v-if="drawerReadFileVisible"
      destroy-on-close
      :title="drawerTitle"
      placement="right"
      width="85vw"
      :open="drawerReadFileVisible"
      @close="onReadFileClose"
    >
      <file-read
        v-if="drawerReadFileVisible"
        :id="temp.id"
        :node-id="temp.nodeId"
        :read-file-path="temp.readFilePath"
        :project-id="temp.projectId"
        @go-file="goFile"
      />
    </CustomDrawer>
  </div>
</template>
<script>
import { EyeInvisibleOutlined, FullscreenOutlined, MenuOutlined, WarningOutlined } from '@ant-design/icons-vue'

import {
  getDispatchProject,
  dispatchStatusMap,
  statusMap,
  removeProject,
  saveDispatchProjectConfig
} from '@/api/dispatch'
import { getNodeListAll } from '@/api/node'
import { getRuningProjectInfo } from '@/api/node-project'
import {
  readJsonStrField,
  concurrentExecution,
  randomStr,
  itemGroupBy,
  parseTime,
  renderSize,
  formatDuration,
  dropApplyDrag
} from '@/utils/const'
import File from '@/pages/node/node-layout/project/project-file'
import Console from '@/pages/node/node-layout/project/project-console'
import FileRead from '@/pages/node/node-layout/project/project-file-read'
import { Container, Draggable } from 'vue3-smooth-dnd'
export default {
  components: {
    File,
    Console,
    FileRead,
    Container,
    Draggable
  },
  props: {
    id: {
      type: String,
      default: ''
    },
    name: {
      type: String,
      default: ''
    }
  },
  emits: ['close'],
  data() {
    return {
      childLoading: true,
      statusMap,
      dispatchStatusMap,

      list: [],
      tabKey: '1',
      data: {},
      drawerTitle: '',
      drawerFileVisible: false,
      drawerConsoleVisible: false,
      drawerReadFileVisible: false,
      nodeNameMap: {},

      childColumns: [
        {
          title: this.$t('i18n_b1785ef01e'),
          key: 'nodeId',
          width: 120,
          ellipsis: true
        },
        {
          title: this.$t('i18n_738a41f965'),
          key: 'projectName',
          width: 120,
          ellipsis: true
        },
        {
          title: this.$t('i18n_3b94c70734'),
          key: 'projectStatus',
          width: 120,
          ellipsis: true
        },
        {
          title: this.$t('i18n_30849b2e10'),
          key: 'projectPid',
          width: '120px',
          ellipsis: true
        },
        {
          title: this.$t('i18n_da99dbfe1f'),
          key: 'outGivingStatus',
          width: '120px'
        },
        {
          title: this.$t('i18n_0ef396cbcc'),
          key: 'outGivingResultMsg',
          ellipsis: true,
          width: 120
        },
        {
          title: this.$t('i18n_543de6ff04'),
          key: 'outGivingResultMsgData',
          ellipsis: true,
          width: 120
        },
        {
          title: this.$t('i18n_4cd49caae4'),
          key: 'outGivingResultTime',
          width: '120px'
        },
        {
          title: this.$t('i18n_396b7d3f91'),
          key: 'outGivingResultSize',
          width: '100px'
        },
        {
          title: this.$t('i18n_fbd7ba1d9b'),
          key: 'lastTime',
          width: '170px',
          ellipsis: true,
          render: (row) => parseTime(row['lastTime'])
        },
        {
          title: this.$t('i18n_2b6bc0f293'),
          key: 'child-operation',
          fixed: 'right',

          width: '140px',
          align: 'center'
        }
      ],

      countdownTime: Date.now(),
      refreshInterval: 5,
      temp: {}
    }
  },
  computed: {},
  watch: {},
  created() {
    this.loadData()
    this.loadNodeList()
  },
  methods: {
    readJsonStrField,
    renderSize,
    formatDuration,
    randomStr,
    onDrop(dropResult) {
      this.list = dropApplyDrag(this.list, dropResult).map((item, index) => {
        return { ...item, sortValue: index }
      })
    },
    loadData() {
      this.childLoading = true
      this.handleReloadById().then(() => {
        // 重新计算倒计时
        this.countdownTime = Date.now() + this.refreshInterval * 1000
      })
    },
    // 加载节点以及项目
    loadNodeList(fn) {
      this.nodeList = []
      getNodeListAll().then((res) => {
        if (res.code === 200) {
          this.nodeList = res.data
          this.nodeList.map((item) => {
            // this.nodeNameMap[item.id] = item.name;
            this.nodeNameMap = { ...this.nodeNameMap, [item.id]: item.name }
          })
          fn && fn()
        }
      })
    },
    // 静默
    silenceLoadData() {
      if (this.tabKey !== '1') {
        // 避免配置页面数据被刷新
        // 重新计算倒计时
        this.countdownTime = Date.now() + this.refreshInterval * 1000
        return
      }
      this.childLoading = true
      this.handleReloadById().then(() => {
        // 重新计算倒计时
        this.countdownTime = Date.now() + this.refreshInterval * 1000
      })
    },

    handleReloadById() {
      return new Promise((resolve) => {
        getDispatchProject(this.id, false)
          .then((res) => {
            if (res.code === 200 && res.data) {
              let projectList =
                res.data?.projectList?.map((item) => {
                  return {
                    ...item,
                    id_no: `${item.id}-${item.nodeId}-${item.projectId}-${new Date().getTime()}`
                  }
                }) || []
              this.data = res.data?.data || {}
              let oldProjectList = this.list
              let oldProjectMap = oldProjectList.groupBy((item) => item.id)
              //console.log(oldProjectList, oldProjectMap)
              projectList = projectList.map((item) => {
                return Object.assign({}, oldProjectMap[item.id], item)
              })
              this.list = projectList
              // 查询项目状态
              const nodeProjects = itemGroupBy(projectList, 'nodeId')
              this.getRuningProjectInfo(nodeProjects)
            }
            resolve()
          })
          .catch(() => {
            resolve()
          })
          .finally(() => {
            // 取消加载中
            this.childLoading = false
          })
      })
    },
    getRuningProjectInfo(nodeProjects) {
      if (nodeProjects.length <= 0) {
        return
      }
      concurrentExecution(
        nodeProjects.map((item, index) => {
          return index
        }),
        3,
        (curItem) => {
          const data = nodeProjects[curItem]

          return new Promise((resolve, reject) => {
            const ids = data.data.map((item) => {
              return item.projectId
            })
            if (ids.length <= 0) {
              resolve()
              return
            }
            const tempParams = {
              nodeId: data.type,
              ids: JSON.stringify(ids)
            }
            getRuningProjectInfo(tempParams, 'noTip')
              .then((res2) => {
                if (res2.code === 200) {
                  this.list = this.list.map((element) => {
                    if (res2.data[element.projectId] && element.nodeId === data.type) {
                      return {
                        ...element,
                        projectStatus: res2.data[element.projectId].pid > 0,
                        projectPid: (
                          res2.data[element.projectId]?.pids || [res2.data[element.projectId]?.pid || '-']
                        ).join(','),
                        projectPort: res2.data[element.projectId]?.port || '-',
                        errorMsg: res2.data[element.projectId].error,
                        projectName: res2.data[element.projectId].name
                      }
                    }
                    return element
                  })
                  resolve()
                } else {
                  this.list = this.list.map((element) => {
                    if (element.nodeId === data.type) {
                      return {
                        ...element,
                        projectStatus: false,
                        projectPid: '-',
                        errorMsg: res2.msg
                      }
                    }
                    return element
                  })

                  reject()
                }
              })
              .catch(() => {
                this.list = this.list.map((element) => {
                  if (element.nodeId === data.type) {
                    return {
                      ...element,
                      projectStatus: false,
                      projectPid: '-',
                      errorMsg: this.$t('i18n_44ed625b19')
                    }
                  }
                  return element
                })

                reject()
              })
          })
        }
      )
    },
    // 文件管理
    handleFile(record) {
      this.temp = Object.assign({}, record)
      this.drawerTitle = `${this.$t('i18n_8780e6b3d1')}(${this.temp.projectId})`
      this.drawerFileVisible = true
    },
    // 关闭文件管理对话框
    onFileClose() {
      this.drawerFileVisible = false
    },
    // 控制台
    handleConsole(record) {
      this.temp = Object.assign({}, record)
      this.drawerTitle = `${this.$t('i18n_b5c3770699')}(${this.temp.projectId})`
      this.drawerConsoleVisible = true
    },
    // 关闭控制台
    onConsoleClose() {
      this.drawerConsoleVisible = false
    },
    //前往控制台
    goConsole() {
      //关闭文件
      this.onFileClose()
      this.handleConsole(this.temp)
    },
    //前往文件
    goFile() {
      // 关闭控制台
      this.onConsoleClose()
      this.onReadFileClose()
      this.handleFile(this.temp)
    },
    // 跟踪文件
    goReadFile(path, filename) {
      this.onFileClose()
      this.drawerReadFileVisible = true
      this.temp.readFilePath = (path + '/' + filename).replace(new RegExp('//', 'gm'), '/')
      this.drawerTitle = `${this.$t('i18n_5854370b86')}(${filename})`
    },
    onReadFileClose() {
      this.drawerReadFileVisible = false
    },
    toNode(nodeId) {
      const newpage = this.$router.resolve({
        path: '/node/list',
        query: {
          ...this.$route.query,
          nodeId: nodeId,
          pId: 'manage',
          id: 'manageList'
        }
      })
      window.open(newpage.href, '_blank')
    },
    // 删除项目
    handleRemoveProject(item) {
      const html = `
      <b style='font-size: 20px;'>
        ${this.$t('i18n_0d44f4903a')}
      </b>
      <ul style='font-size: 20px;color:red;font-weight: bold;'>
        <li>this.$t('i18n_7cc3bb7068')</b></li>
        <li>this.$t('i18n_5c93055d9c')</li>
        <li>this.$t('i18n_27d0c8772c')</li>
      </ul>
      `
      $confirm({
        title: this.$t('i18n_9362e6ddf8'),
        zIndex: 1009,
        content: h('div', null, [h('p', { innerHTML: html }, null)]),
        okButtonProps: { type: 'primary', size: 'small', danger: true },
        cancelButtonProps: { type: 'primary' },
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
        onOk: () => {
          return removeProject({
            nodeId: item.nodeId,
            projectId: item.projectId,
            id: this.id
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
    //分发管理
    viewDispatchManagerOk() {
      const temp = {
        data: this.list.map((item, index) => {
          return {
            nodeId: item.nodeId,
            projectId: item.projectId,
            sortValue: index,
            disabled: item.disabled
          }
        }),
        id: this.id
      }
      saveDispatchProjectConfig(temp).then((res) => {
        if (res.code === 200) {
          $notification.success({
            message: res.msg
          })
        }
      })
    }
  }
}
</script>
<style scoped>
:deep(.n-progress-icon--as-text) {
  width: auto;
}
:deep(.n-statistic .n-statistic-value__content),
:deep(.n-statistic .n-statistic-value__prefix),
:deep(.n-statistic .n-statistic-value__suffix) {
  font-size: 16px;
}
.box-shadow {
  box-shadow: 0 0 10px 5px rgba(223, 222, 222, 0.5);
  border-radius: 5px;
}
.item-row {
  padding: 10px;
  margin: 5px;
  border: 1px solid #e8e8e8;
  border-radius: 4px;
  background: rgba(255, 255, 255, 0.8);
}
</style>
