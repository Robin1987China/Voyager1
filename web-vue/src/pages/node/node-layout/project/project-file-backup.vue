<template>
  <div>
    <div v-show="viewList">
      <n-data-table
        size="medium"
        :data="backupListData.list"
        :loading="backupListLoading"
        :columns="columns"
        :pagination="false"
        bordered
        :scroll="{
          x: 'max-content'
        }"
      >
        <template v-if="backupListData.path" #title> {{ $t('i18n_1b38c0bc86') }}{{ backupListData.path }} </template>

        <template #bodyCell="{ column, text, record }">
          <template v-if="column.dataIndex === 'filename'">
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
          <template v-else-if="column.dataIndex === 'fileSizeLong'">
            <n-tooltip placement="topLeft">
              <template #trigger>
                {{ text ? renderSize(text) : item.fileSize }}
              </template>
              `${text ? renderSize(text) : item.fileSize}`
            </n-tooltip>
          </template>
          <template v-else-if="column.dataIndex === 'modifyTimeLong'">
            <n-tooltip>
              <template #trigger>
                <span class="tw">
                  <span class="tw">
                    <span>{{ parseTime(record.modifyTimeLong) }}</span>
                  </span>
                </span>
              </template>
              `${parseTime(record.modifyTimeLong)}}`
            </n-tooltip>
          </template>
          <template v-else-if="column.dataIndex === 'operation'">
            <n-space>
              <n-button size="small" type="primary" @click="handleBackupFile(record)">{{
                $t('i18n_f26225bde6')
              }}</n-button>
              <n-button size="small" type="primary" danger @click="handlBackupeDelete(record)">{{
                $t('i18n_2f4aaddde3')
              }}</n-button>
            </n-space>
          </template>
        </template>
      </n-data-table>
    </div>
    <!-- 布局 -->
    <n-layout v-show="!viewList" class="file-layout">
      <!-- 目录树 -->
      <n-layout-sider theme="light" class="sider" width="25%">
        <div class="dir-container">
          <n-space>
            <n-button
              size="small"
              type="primary"
              @click="
                () => {
                  viewList = true
                }
              "
              >{{ $t('i18n_adcd1dd701') }}
            </n-button>
            <n-button size="small" type="primary" @click="loadData">{{ $t('i18n_90b5a467c1') }}</n-button>
          </n-space>
        </div>

        <n-tree
          v-model:expanded-keys="expandedKeys"
          v-model:selected-keys="selectedKeys"
          :field-names="treeReplaceFields"
          :on-load="onTreeData"
          :data="treeList"
          @update:selected-keys="nodeClick"
        ></n-tree>
      </n-layout-sider>
      <!-- 表格 -->
      <n-layout-content class="file-content">
        <n-data-table
          :data="fileList"
          size="medium"
          :loading="loading"
          :columns="fileColumns"
          :pagination="false"
          bordered
          :scroll="{
            x: 'max-content'
          }"
        >
          <template #title>
            <n-popconfirm
              :positive-text="$t('i18n_587a63264b')"
              :negative-text="$t('i18n_b1a09cee8e')"
              :positive-button-props="{
                loading: recoverLoading
              }"
              @positive-click="recoverNet('', uploadPath)"
              @negative-click="recoverNet('clear', uploadPath)"
            >
              <template #trigger>
                <span class="tw">
                  <n-button size="small" type="primary">{{ $t('i18n_69de8d7f40') }}</n-button>
                </span>
              </template>
              <template #icon>
                <QuestionCircleOutlined style="color: red" />
              </template>
              {{
                `${uploadPath ? $t('i18n_bdd4cddd22') + uploadPath + $t('i18n_dadd4907c2') : ''} ${$t(
                  'i18n_aefd8f9f27'
                )},${$t('i18n_500789168c')}`
              }}
            </n-popconfirm>

            <n-space>
              <n-tag v-if="uploadPath" color="#2db7f5">{{ $t('i18n_2c8109fa0b') }}{{ uploadPath || '' }}</n-tag>
            </n-space>
          </template>

          <template #bodyCell="{ column, text, record }">
            <!-- <template v-if="column.dataIndex === 'filename'"> -->
            <template v-if="column.dataIndex === 'filename'">
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
            <template v-else-if="column.dataIndex === 'isDirectory'">
              <n-tooltip placement="topLeft">
                <template #trigger>
                  <span class="tw">
                    <span class="tw">
                      <span>{{ text ? $t('i18n_767fa455bb') : $t('i18n_2a0c4740f1') }}</span>
                    </span>
                  </span>
                </template>
                text
              </n-tooltip>
            </template>
            <template v-else-if="column.dataIndex === 'fileSizeLong'">
              <n-tooltip placement="topLeft">
                <template #trigger>
                  {{ text ? renderSize(text) : item.fileSize }}
                </template>
                `${text ? renderSize(text) : item.fileSize}`
              </n-tooltip>
            </template>
            <template v-else-if="column.dataIndex === 'modifyTimeLong'">
              <n-tooltip>
                <template #trigger>
                  <span class="tw">
                    <span class="tw">
                      <span>{{ parseTime(record.modifyTimeLong) }}</span>
                    </span>
                  </span>
                </template>
                `${parseTime(record.modifyTimeLong)}}`
              </n-tooltip>
            </template>
            <template v-else-if="column.dataIndex === 'operation'">
              <n-space>
                <template v-if="record.isDirectory">
                  <n-tooltip>
                    <template #trigger>
                      <span class="tw">
                        <n-button size="small" type="primary" :disabled="true">{{ $t('i18n_f26ef91424') }}</n-button>
                      </span>
                    </template>
                    $t('i18n_6c14188ba0')
                  </n-tooltip>
                </template>
                <template v-else>
                  <n-button size="small" type="primary" @click="handleDownload(record)">{{
                    $t('i18n_f26ef91424')
                  }}</n-button>
                </template>
                <template v-if="record.isDirectory">
                  <!-- record.filename -->
                  <n-popconfirm
                    :positive-text="$t('i18n_587a63264b')"
                    :negative-text="$t('i18n_b1a09cee8e')"
                    :positive-button-props="{
                      loading: recoverLoading
                    }"
                    @positive-click="recoverNet('', record.filename)"
                    @negative-click="recoverNet('clear', record.filename)"
                  >
                    <template #trigger>
                      <span class="tw">
                        <n-button size="small" type="primary">{{ $t('i18n_69de8d7f40') }}</n-button>
                      </span>
                    </template>
                    <template #icon>
                      <QuestionCircleOutlined style="color: red" />
                    </template>
                    {{
                      `${
                        record.filename ? $t('i18n_bdd4cddd22') + record.filename + $t('i18n_dadd4907c2') : ''
                      } ${$t('i18n_aefd8f9f27')},${$t('i18n_500789168c')}`
                    }}
                  </n-popconfirm>
                </template>
                <template v-else>
                  <n-button size="small" type="primary" :loading="recoverLoading" @click="recover(record)">{{
                    $t('i18n_69de8d7f40')
                  }}</n-button>
                </template>

                <n-button size="small" type="primary" danger @click="handleDelete(record)">{{
                  $t('i18n_2f4aaddde3')
                }}</n-button>
              </n-space>
            </template>
          </template>
        </n-data-table>
      </n-layout-content>
    </n-layout>
  </div>
</template>
<script>
import { QuestionCircleOutlined } from '@ant-design/icons-vue'

import {
  backupDeleteProjectFile,
  backupDownloadProjectFile,
  backupFileList,
  backupRecoverProjectFile,
  listBackup
} from '@/api/node-project-backup'
import { renderSize, parseTime } from '@/utils/const'
export default {
  components: {},
  props: {
    nodeId: {
      type: String,
      default: ''
    },
    projectId: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      viewList: true,
      loading: false,
      treeList: [],
      fileList: [],
      backupListData: {
        list: []
      },
      backupListLoading: false,
      tempNode: {},
      temp: {},
      treeReplaceFields: {
        title: 'filename',
        isLeaf: 'isDirectory'
      },

      defaultProps: {
        children: 'children',
        label: 'filename'
      },
      expandedKeys: [],
      selectedKeys: [],
      columns: [
        {
          title: this.$t('i18n_d2e2560089'),
          key: 'filename',
          ellipsis: true
        },

        {
          title: this.$t('i18n_396b7d3f91'),
          key: 'fileSizeLong',
          width: 120,
          ellipsis: true
        },
        {
          title: this.$t('i18n_1303e638b5'),
          key: 'modifyTimeLong',
          width: 180,
          ellipsis: true
        },
        {
          title: this.$t('i18n_2b6bc0f293'),
          key: 'operation',
          width: 180,
          align: 'center',
          fixed: 'right'
        }
      ],

      fileColumns: [
        {
          title: this.$t('i18n_d2e2560089'),
          key: 'filename',
          ellipsis: true
        },
        {
          title: this.$t('i18n_28b988ce6a'),
          key: 'isDirectory',
          width: 100,
          ellipsis: true
        },
        {
          title: this.$t('i18n_396b7d3f91'),
          key: 'fileSizeLong',
          width: 120,
          ellipsis: true
        },
        {
          title: this.$t('i18n_1303e638b5'),
          key: 'modifyTimeLong',
          width: 180,
          ellipsis: true
        },
        {
          title: this.$t('i18n_2b6bc0f293'),
          key: 'operation',
          width: 180,
          align: 'center',
          fixed: 'right'
        }
      ],

      recoverLoading: false
    }
  },
  computed: {
    uploadPath() {
      if (!Object.keys(this.tempNode).length) {
        return ''
      }
      if (this.tempNode.level === 1) {
        return ''
      } else {
        return (this.tempNode.levelName || '') + '/' + this.tempNode.filename
      }
    }
  },
  mounted() {
    this.loadBackupList()
  },
  methods: {
    renderSize,
    parseTime,
    onTreeData(treeNode) {
      return new Promise((resolve) => {
        if (treeNode.dataRef.children || !treeNode.dataRef.isDirectory) {
          resolve()
          return
        }
        this.loadNode(treeNode.dataRef, resolve)
      })
    },
    // 查询备份列表
    loadBackupList() {
      listBackup({
        nodeId: this.nodeId,
        id: this.projectId
      }).then((res) => {
        if (res.code === 200 && res.data) {
          this.backupListData = res.data
        }
        this.backupListLoading = false
      })
    },
    // 加载数据
    loadData() {
      const key = 'root-' + new Date().getTime()
      this.treeList = [
        {
          filename: this.$t('i18n_cfeff30d2c') + (this.temp.filename || ''),
          level: 1,
          isDirectory: true,
          key: key,
          isLeaf: false
        }
      ]

      // 设置默认展开第一个
      setTimeout(() => {
        const node = this.treeList[0]
        this.tempNode = node
        this.expandKeys = [key]
        this.loadFileList()
      }, 1000)
    },
    // 加载子节点
    loadNode(data, resolve) {
      this.tempNode = data
      // 如果是目录
      if (data.isDirectory) {
        setTimeout(() => {
          // 请求参数
          const params = {
            nodeId: this.nodeId,
            id: this.projectId,
            path: this.uploadPath,
            backupId: this.temp.filename
          }
          // if (node.level === 1) {
          //   params.path = ''
          // } else {
          //   params.path = data.levelName || '' + '/' + data.filename
          // }
          // 加载文件
          backupFileList(params).then((res) => {
            if (res.code === 200) {
              const treeData = res.data
                .filter((ele) => {
                  return ele.isDirectory
                })
                .map((ele) => {
                  ele.isLeaf = !ele.isDirectory
                  ele.key = ele.filename + '-' + new Date().getTime()
                  return ele
                })
              data.children = treeData

              this.treeList = [...this.treeList]
              resolve()
            } else {
              resolve()
            }
          })
        }, 500)
      } else {
        resolve()
      }
    },

    // 点击树节点
    nodeClick(selectedKeys, { node }) {
      if (node.dataRef.isDirectory) {
        this.tempNode = node.dataRef
        this.loadFileList()
      }
    },

    // 加载文件列表
    loadFileList() {
      if (Object.keys(this.tempNode).length === 0) {
        $notification.warn({
          message: this.$t('i18n_bcaf69a038')
        })
        return false
      }
      // 请求参数
      const params = {
        nodeId: this.nodeId,
        id: this.projectId,
        path: this.uploadPath,
        backupId: this.temp.filename
      }
      this.fileList = []
      this.loading = true
      // 加载文件
      backupFileList(params).then((res) => {
        if (res.code === 200) {
          // 区分目录和文件
          res.data.forEach((element) => {
            // if (!element.isDirectory) {
            // 设置文件表格
            this.fileList.push({
              ...element
            })
            // }
          })
        }
        this.loading = false
      })
    },

    // 下载
    handleDownload(record) {
      $notification.info({
        message: this.$t('i18n_e4bf491a0d')
      })
      // 请求参数
      const params = {
        nodeId: this.nodeId,
        id: this.projectId,
        levelName: record.levelName,
        filename: record.filename,
        backupId: this.temp.filename
      }
      // 请求接口拿到 blob
      window.open(backupDownloadProjectFile(params), '_blank')
    },
    // 删除
    handleDelete(record) {
      const msg = record.isDirectory
        ? this.$t('i18n_3cc09369ad') + record.filename + this.$t('i18n_52a8df6678')
        : this.$t('i18n_3cc09369ad') + record.filename + this.$t('i18n_48e79b3340')
      $confirm({
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        content: msg,
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
        onOk: () => {
          return backupDeleteProjectFile({
            nodeId: this.nodeId,
            id: this.projectId,
            levelName: record.levelName,
            filename: record.filename,
            backupId: this.temp.filename
          }).then((res) => {
            if (res.code === 200) {
              $notification.success({
                message: res.msg
              })
              this.loadData()
              this.loadFileList()
            }
          })
        }
      })
    },
    // 删除备份
    handlBackupeDelete(record) {
      const msg = this.$t('i18n_3cc09369ad') + record.filename + this.$t('i18n_115cd58b5d')
      $confirm({
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        content: msg,
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
        onOk: () => {
          return backupDeleteProjectFile({
            nodeId: this.nodeId,
            id: this.projectId,
            levelName: '/',
            filename: '/',
            backupId: record.filename
          }).then((res) => {
            if (res.code === 200) {
              $notification.success({
                message: res.msg
              })
              this.loadBackupList()
            }
          })
        }
      })
    },
    handleBackupFile(record) {
      this.viewList = false
      this.temp = Object.assign({}, record)
      this.loadData()
    },

    recover(record) {
      if (record.isDirectory) {
        this.recoverPath(record.filename)
      } else {
        $confirm({
          title: this.$t('i18n_c4535759ee'),
          zIndex: 1009,
          content: this.$t('i18n_d2cac1245d') + record.filename + this.$t('i18n_e039ffccc8'),
          okText: this.$t('i18n_e83a256e4f'),
          cancelText: this.$t('i18n_625fb26b4b'),
          onOk: () => {
            // // 请求参数
            return this.recoverNet('', record.filename)
          }
        })
      }
    },
    // 请求参数
    recoverNet(type, filename) {
      const params = {
        nodeId: this.nodeId,
        id: this.projectId,
        type,
        levelName: this.uploadPath,
        filename,
        backupId: this.temp.filename
      }
      this.recoverLoading = true
      // 删除
      return backupRecoverProjectFile(params)
        .then((res) => {
          if (res.code === 200) {
            $notification.success({
              message: res.msg
            })
          }
        })
        .finally(() => {
          this.recoverLoading = false
        })
    }
  }
}
</script>
<style scoped>
.file-layout {
  padding: 0;
}
.sider {
  border: 1px solid #e2e2e2;
  height: calc(100vh - 80px);
  overflow-y: auto;
}
.dir-container {
  padding: 10px;
  border-bottom: 1px solid #eee;
}
.file-content {
  height: calc(100vh - 100px);
  overflow-y: auto;
  margin: 10px 10px 0;
  padding: 10px;
  /* background-color: #fff; */
}
.successTag {
  height: 32px;
  line-height: 30px;
}
</style>
