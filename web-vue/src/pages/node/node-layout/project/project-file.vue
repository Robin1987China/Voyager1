<template>
  <div>
    <!-- 布局 -->
    <n-spin tip="Loading..." :spinning="loading">
      <n-layout class="file-layout">
        <!-- 目录树 -->
        <n-layout-sider theme="light" class="sider" width="25%">
          <div class="dir-container">
            <n-space>
              <n-button size="small" type="primary" @click="loadData">{{ $t('i18n_90b5a467c1') }}</n-button>
              <n-button v-show="noFileModes.includes(runMode)" size="small" type="primary" @click="goConsole">
                {{ $t('i18n_b5c3770699') }}
              </n-button>
              <n-button size="small" type="primary" @click="backupList">
                {{ $t('i18n_9014d6d289') }}
              </n-button>
            </n-space>
          </div>
          <n-spin :tip="$t('i18n_f013ea9dcb')" :spinning="loading">
            <div class="tree-container">
              <n-tree
                v-model:selected-keys="selectedKeys"
                v-model:expanded-keys="expandedKeys"
                auto-expand-parent
                :field-names="treeReplaceFields"
                :data="treeList"
                @update:selected-keys="nodeClick"
                @expand="
                  (expandedKeys, { expanded, node }) => {
                    if (expanded) {
                      nodeClick(expandedKeys, { node })
                    }
                  }
                "
              ></n-tree>
            </div>
          </n-spin>
          <!--   :loadData="onTreeData" -->
        </n-layout-sider>
        <!-- 表格 -->
        <n-layout-content class="file-content">
          <n-data-table
            :data="fileList"
            size="medium"
            :loading="loading"
            :columns="columns"
            :pagination="false"
            bordered
            :scroll="{
              x: 'max-content'
            }"
          >
            <template #title>
              <!-- <n-tag color="#2db7f5">项目目录: {{ absPath }}</n-tag>-->
              <n-space>
                <n-dropdown
                  :disabled="!Object.keys(tempNode).length"
                  :options="[
                    {
                      label: $t('i18n_a6fc9e3ae6'),
                      key: '0',
                      icon: () => h(NIcon, null, { default: () => h(FileOutlined) }),
                      props: { onClick: () => handleUpload }
                    },
                    {
                      label: $t('i18n_37f031338a'),
                      key: '1',
                      icon: () => h(NIcon, null, { default: () => h(FileZipOutlined) }),
                      props: { onClick: () => handleZipUpload }
                    }
                  ]"
                >
                  <n-button size="small" type="primary" @click="(e) => e.preventDefault()"
                    ><UploadOutlined />{{ $t('i18n_d5a73b0c7f') }}</n-button
                  >
                </n-dropdown>
                <n-dropdown
                  :disabled="!Object.keys(tempNode).length"
                  :options="[
                    {
                      label: $t('i18n_547ee197e5'),
                      key: '0',
                      icon: () => h(NIcon, null, { default: () => h(FolderAddOutlined) }),
                      props: { onClick: () => handleAddFile(1) }
                    },
                    {
                      label: $t('i18n_497ddf508a'),
                      key: '1',
                      icon: () => h(NIcon, null, { default: () => h(FileAddOutlined) }),
                      props: { onClick: () => handleAddFile(2) }
                    }
                  ]"
                >
                  <n-button size="small" type="primary" @click="(e) => e.preventDefault()"
                    ><PlusOutlined />{{ $t('i18n_26bb841878') }}</n-button
                  >
                </n-dropdown>
                <n-tooltip>
                  <template #trigger>
                    <span class="tw">
                      <n-button size="small" type="primary" @click="openRemoteUpload"
                        ><CloudDownloadOutlined
                      /></n-button>
                    </span>
                  </template>
                  $t('i18n_9065a208e8')
                </n-tooltip>
                <n-tooltip>
                  <template #trigger>
                    <span class="tw">
                      <n-button size="small" type="primary" @click="loadFileList"><ReloadOutlined /></n-button>
                    </span>
                  </template>
                  $t('i18n_5e32f72bbf')
                </n-tooltip>
                <n-tooltip>
                  <template #trigger>
                    <span class="tw">
                      <n-button size="small" type="primary" danger @click="clearFile"><DeleteOutlined /></n-button>
                    </span>
                  </template>
                  $t('i18n_de6bc95d3b')
                </n-tooltip>

                <n-tag v-if="uploadPath" color="#2db7f5">{{ $t('i18n_2c8109fa0b') }}{{ uploadPath || '' }}</n-tag>
                <div>{{ $t('i18n_9e98fa5c0d') }}</div>
              </n-space>
            </template>

            <template #bodyCell="{ column, text, record }">
              <template v-if="column.dataIndex === 'filename'">
                <n-tooltip placement="top-start">
                  <template #trigger>
                    <span class="tw">
                      <n-dropdown
                        :trigger="['contextmenu']"
                        :options="[
                          {
                            label: $t('i18n_5854370b86'),
                            key: '1',
                            icon: () => h(NIcon, null, { default: () => h(BarsOutlined) }),
                            disabled: !record.textFileEdit,
                            props: { onClick: () => goReadFile(record) }
                          },
                          {
                            label: $t('i18n_c8ce4b36cb'),
                            key: '2',
                            icon: () => h(NIcon, null, { default: () => h(HighlightOutlined) }),
                            props: { onClick: () => handleRenameFile(record) }
                          },
                          {
                            label: $t('i18n_7a811cc1e5'),
                            key: '3',
                            icon: () => h(NIcon, null, { default: () => h(CopyOutlined) }),
                            props: { onClick: () => hannderCopy(record) }
                          },
                          {
                            label: $t('i18n_072fa90836'),
                            key: '4',
                            icon: () => h(NIcon, null, { default: () => h(CompressOutlined) }),
                            disabled: !record.isDirectory,
                            children: [
                              {
                                label: 'zip',
                                key: '4-0',
                                icon: () => h(NIcon, null, { default: () => h(FileZipOutlined) }),
                                props: { onClick: () => hannderCompress(record, 'zip') }
                              },
                              {
                                label: 'tar',
                                key: '4-1',
                                icon: () => h(NIcon, null, { default: () => h(FileZipOutlined) }),
                                props: { onClick: () => hannderCompress(record, 'tar') }
                              },
                              {
                                label: 'tar.gz',
                                key: '4-2',
                                icon: () => h(NIcon, null, { default: () => h(FileZipOutlined) }),
                                props: { onClick: () => hannderCompress(record, 'tar.gz') }
                              }
                            ]
                          }
                        ]"
                      >
                        <div>{{ text }}</div>
                      </n-dropdown>
                    </span>
                  </template>

                  <n-button text><CompressOutlined />{{ $t('i18n_072fa90836') }}</n-button>
                </n-tooltip>
              </template>
              <template v-else-if="column.dataIndex === 'isDirectory'">
                <span>{{ text ? $t('i18n_767fa455bb') : $t('i18n_2a0c4740f1') }}</span>
              </template>
              <template v-else-if="column.dataIndex === 'fileSizeLong'">
                <n-tooltip placement="topLeft">
                  <template #trigger>
                    {{ text ? renderSize(text) : record.fileSize }}
                  </template>
                  `${text ? renderSize(text) : record.fileSize}`
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
                          <n-button size="small" type="primary" :disabled="true">{{ $t('i18n_95b351c862') }}</n-button>
                        </span>
                      </template>
                      $t('i18n_c6f1c6e062')
                    </n-tooltip>
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
                    <n-tooltip>
                      <template #trigger>
                        <span class="tw">
                          <n-button
                            size="small"
                            type="primary"
                            :loading="editLoading"
                            :disabled="!record.textFileEdit"
                            @click="handleEditFile(record)"
                            >{{ $t('i18n_95b351c862') }}</n-button
                          >
                        </span>
                      </template>
                      $t('i18n_17b5e684e5')
                    </n-tooltip>
                    <n-button size="small" type="primary" @click="handleDownload(record)">{{
                      $t('i18n_f26ef91424')
                    }}</n-button>
                  </template>
                  <n-button size="small" type="primary" danger @click="handleDelete(record)">{{
                    $t('i18n_2f4aaddde3')
                  }}</n-button>
                </n-space>
              </template>
            </template>
          </n-data-table>
          <!-- 批量上传文件 -->
          <CustomModal
            v-if="uploadFileVisible"
            v-model:open="uploadFileVisible"
            destroy-on-close
            :closable="!uploading"
            :keyboard="false"
            width="35vw"
            :title="$t('i18n_15c0ba2767')"
            :footer="null"
            :mask-closable="false"
          >
            <n-space direction="vertical" size="large" style="width: 100%">
              <n-upload
                v-model:file-list="uploadFileList"
                :custom-request="
                  (file) => {
                    uploadFileList = [...uploadFileList, file]
                    return false
                  }
                "
                multiple
                :disabled="!!percentage"
                @remove="
                  (file) => {
                    const index = uploadFileList.indexOf(file)
                    //const newFileList = this.uploadFileList.slice();

                    uploadFileList.splice(index, 1)
                    return true
                  }
                "
              >
                <template v-if="percentage">
                  <template v-if="uploadFileList?.length">
                    <LoadingOutlined v-if="uploadFileList.length > 1" />
                  </template>
                </template>

                <n-button v-else><UploadOutlined />{{ $t('i18n_fd7e0c997d') }}</n-button>
              </n-upload>

              <n-grid v-if="percentage">
                <n-grid-item span="24">
                  <n-progress :percent="percentage" class="max-progress">
                    <template #format="percent">
                      {{ percent }}%<template v-if="percentageInfo.total">
                        ({{ renderSize(percentageInfo.total) }})
                      </template>
                      <template v-if="percentageInfo.duration">
                        {{ $t('i18n_833249fb92') }}:{{ formatDuration(percentageInfo.duration) }}
                      </template>
                      <template v-if="uploadFileList.length">
                        {{ $t('i18n_769d88e425') }} {{ successSize }} {{ $t('i18n_d047d84986')
                        }}{{ uploadFileList.length }}{{ $t('i18n_930882bb0a') }}
                      </template>
                    </template>
                  </n-progress>
                  <n-tag v-if="percentage >= 100">
                    {{
                      uploadFileList.find((fileItem, fileIndex) => {
                        return (fileItem.status = 'uploading')
                      })?.name
                    }}{{ $t('i18n_ade63665b2') }}</n-tag
                  >
                </n-grid-item>
              </n-grid>

              <n-button type="primary" :disabled="fileUploadDisabled" @click="startUpload">{{
                $t('i18n_020f1ecd62')
              }}</n-button>
            </n-space>
          </CustomModal>
          <!-- 上传压缩文件 -->
          <CustomModal
            v-if="uploadZipFileVisible"
            v-model:open="uploadZipFileVisible"
            destroy-on-close
            :closable="!uploading"
            :keyboard="false"
            width="35vw"
            :title="$t('i18n_e31ca72849')"
            :footer="null"
            :mask-closable="false"
          >
            <n-space direction="vertical" size="large" style="width: 100%">
              <n-upload
                v-model:file-list="uploadFileList"
                :disabled="!!percentage"
                :custom-request="
                  (file) => {
                    uploadFileList = [file]
                    return false
                  }
                "
                :accept="ZIP_ACCEPT"
                @remove="
                  () => {
                    uploadFileList = []
                    return true
                  }
                "
              >
                <LoadingOutlined v-if="percentage" />
                <n-button v-else><UploadOutlined />{{ $t('i18n_a17450a5ff') }}</n-button>
              </n-upload>
              <n-grid v-if="percentage">
                <n-grid-item span="24">
                  <n-progress :percent="percentage" class="max-progress">
                    <template #format="percent">
                      {{ percent }}%<template v-if="percentageInfo.total">
                        ({{ renderSize(percentageInfo.total) }})
                      </template>
                      <template v-if="percentageInfo.duration">
                        {{ $t('i18n_833249fb92') }}:{{ formatDuration(percentageInfo.duration) }}
                      </template>
                      <template v-if="uploadFileList.length">
                        {{ $t('i18n_769d88e425') }} {{ successSize }} {{ $t('i18n_d047d84986')
                        }}{{ uploadFileList.length }}{{ $t('i18n_930882bb0a') }}
                      </template>
                    </template>
                  </n-progress>
                </n-grid-item>
              </n-grid>

              <n-switch
                v-model:value="uploadData.checkBox"
                :checked-label="$t('i18n_164cf07e1c')"
                :unchecked-label="$t('i18n_fd7b461411')"
                style="margin-bottom: 10px"
              />

              <n-input-number
                v-model:value="uploadData.stripComponents"
                style="width: 100%"
                :min="0"
                :placeholder="$t('i18n_3f8b64991f')"
              />

              <n-button type="primary" :disabled="fileUploadDisabled" @click="startZipUpload">{{
                $t('i18n_020f1ecd62')
              }}</n-button>
            </n-space>
          </CustomModal>

          <CustomModal
            v-if="editFileVisible"
            v-model:open="editFileVisible"
            destroy-on-close
            width="80vw"
            :title="`${$t('i18n_47ff744ef6')} ${filename}`"
            :mask-closable="true"
            @cancel="handleCloseModal"
          >
            <code-editor
              v-if="editFileVisible"
              v-model:content="fileContent"
              height="60vh"
              show-tool
              :file-suffix="filename"
            >
              <template #tool_before>
                <n-tag>
                  {{ filename }}
                  <!-- {{ temp.name }} -->
                </n-tag>
              </template>
            </code-editor>

            <template #footer>
              <n-button @click="handleCloseModal">
                {{ $t('i18n_b15d91274e') }}
              </n-button>
              <n-button type="primary" @click="updateFileData">
                {{ $t('i18n_be5fbbe34c') }}
              </n-button>
              <n-button
                type="primary"
                @click="
                  () => {
                    updateFileData()
                    handleCloseModal()
                  }
                "
              >
                {{ $t('i18n_280379cee4') }}
              </n-button>
            </template>
          </CustomModal>
          <!--远程下载  -->
          <CustomModal
            v-if="uploadRemoteFileVisible"
            v-model:open="uploadRemoteFileVisible"
            destroy-on-close
            :confirm-loading="confirmLoading"
            :title="$t('i18n_5d488af335')"
            :mask-closable="false"
            @ok="handleRemoteUpload"
            @cancel="closeRemoteUpload"
          >
            <n-form ref="ruleForm" :model="remoteDownloadData" :rules="rules">
              <n-form-item :label="$t('i18n_a66fff7541')" path="url">
                <n-input v-model:value="remoteDownloadData.url" :placeholder="$t('i18n_7457228a61')" />
              </n-form-item>
              <n-form-item :label="$t('i18n_50fefde769')">
                <n-switch
                  v-model:value="remoteDownloadData.unzip"
                  :checked-label="$t('i18n_0a60ac8f02')"
                  :unchecked-label="$t('i18n_c9744f45e7')"
                />
              </n-form-item>
              <n-form-item v-if="remoteDownloadData.unzip" :label="$t('i18n_5effe31353')">
                <n-input-number
                  v-model:value="remoteDownloadData.stripComponents"
                  style="width: 100%"
                  :min="0"
                  :placeholder="$t('i18n_3f8b64991f')"
                />
              </n-form-item>
            </n-form>
          </CustomModal>
          <!-- 创建文件/文件夹 -->
          <CustomModal
            v-if="addFileFolderVisible"
            v-model:open="addFileFolderVisible"
            destroy-on-close
            width="300px"
            :title="addFileOrFolderType === 1 ? $t('i18n_2d9e932510') : $t('i18n_e48a715738')"
            :footer="null"
            :mask-closable="true"
          >
            <n-space direction="vertical" style="width: 100%">
              <span v-if="uploadPath">{{ $t('i18n_4e33dde280') }}{{ uploadPath }}</span>
              <!-- <n-tag v-if="">目录创建成功后需要手动刷新右边树才能显示出来哟</n-tag> -->

              <n-input v-model:value="fileFolderName" :placeholder="$t('i18n_55939c108f')" />
              <n-grid type="flex" justify="center">
                <n-button type="primary" :disabled="fileFolderName.length === 0" @click="startAddFileFolder">{{
                  $t('i18n_e83a256e4f')
                }}</n-button>
              </n-grid>
            </n-space>
          </CustomModal>
          <!-- 从命名文件/文件夹 -->
          <CustomModal
            v-if="renameFileFolderVisible"
            v-model:open="renameFileFolderVisible"
            destroy-on-close
            width="300px"
            :title="`${$t('i18n_c8ce4b36cb')}`"
            :footer="null"
            :mask-closable="true"
          >
            <n-space direction="vertical" style="width: 100%">
              <n-input v-model:value="fileFolderName" :placeholder="$t('i18n_f139c5cf32')" />

              <n-grid type="flex" justify="center">
                <n-button type="primary" :disabled="fileFolderName.length === 0" @click="renameFileFolder">{{
                  $t('i18n_e83a256e4f')
                }}</n-button>
              </n-grid>
            </n-space>
          </CustomModal>
        </n-layout-content>
      </n-layout>
    </n-spin>
    <!-- 查看备份列表 -->
    <CustomModal
      v-if="backupListVisible"
      v-model:open="backupListVisible"
      destroy-on-close
      width="80vw"
      height="80vh"
      :title="$t('i18n_9014d6d289')"
      :footer="null"
      :mask-closable="true"
      @cancel="
        () => {
          loadData()
        }
      "
    >
      <projectFileBackup v-if="backupListVisible" :node-id="nodeId" :project-id="projectId"></projectFileBackup>
    </CustomModal>
  </div>
</template>
<script>
import {
  CloudDownloadOutlined,
  DeleteOutlined,
  LoadingOutlined,
  PlusOutlined,
  ReloadOutlined,
  UploadOutlined
} from '@ant-design/icons-vue'

import { h } from 'vue'
import { NIcon } from 'naive-ui'
import {
  BarsOutlined,
  CompressOutlined,
  CopyOutlined,
  FileAddOutlined,
  FileOutlined,
  FileZipOutlined,
  FolderAddOutlined,
  HighlightOutlined
} from '@ant-design/icons-vue'

import {
  deleteProjectFile,
  downloadProjectFile,
  getFileList,
  newFileFolder,
  noFileModes,
  readFile,
  remoteDownload,
  renameFileFolder,
  updateFile,
  uploadProjectFile,
  shardingMerge,
  copyFileFolder,
  compressFileFolder
} from '@/api/node-project'
import { ZIP_ACCEPT, renderSize, formatDuration, concurrentExecution, parseTime } from '@/utils/const'
import codeEditor from '@/components/codeEditor'
import projectFileBackup from './project-file-backup.vue'
import { uploadPieces } from '@/utils/upload-pieces'

export default {
  components: {
    codeEditor,
    projectFileBackup
  },
  inject: ['globalLoading'],
  props: {
    nodeId: {
      type: String,
      default: ''
    },
    projectId: {
      type: String,
      default: ''
    },
    runMode: {
      type: String,
      default: ''
    },
    absPath: {
      type: String,
      default: ''
    }
  },
  emits: ['goReadFile', 'goConsole'],
  setup() {
    // 模板内联 dropdown options 的 icon 函数需访问模块作用域的 h/NIcon/图标组件
    return {
      h,
      NIcon,
      BarsOutlined,
      CompressOutlined,
      CopyOutlined,
      FileAddOutlined,
      FileOutlined,
      FileZipOutlined,
      FolderAddOutlined,
      HighlightOutlined
    }
  },
  data() {
    return {
      ZIP_ACCEPT,
      noFileModes,
      loading: false,
      treeList: [],
      fileList: [],
      uploadFileList: [],
      tempNode: {},
      temp: {},
      filename: '',
      uploadFileVisible: false,
      uploadZipFileVisible: false,
      uploadRemoteFileVisible: false,
      editFileVisible: false,
      backupListVisible: false,
      successSize: 0,
      fileContent: '',
      treeReplaceFields: {
        title: 'filename',
        isLeaf: 'isDirectory'
      },

      // 是否是上传状态
      uploading: false,
      percentage: 0,
      percentageInfo: {},
      uploadData: {
        checkBox: false
      },

      // tableHeight: "80vh",
      defaultProps: {
        children: 'children',
        label: 'filename'
      },
      remoteDownloadData: {
        id: '',
        url: '',
        unzip: false
      },
      columns: [
        {
          title: this.$t('i18n_d2e2560089'),
          key: 'filename',
          ellipsis: true
        },
        {
          title: this.$t('i18n_28b988ce6a'),
          key: 'isDirectory',
          width: '100px',
          ellipsis: true
        },
        {
          title: this.$t('i18n_396b7d3f91'),
          key: 'fileSizeLong',
          width: 120,
          ellipsis: true,

          sorter: (a, b) => a.fileSizeLong - b.fileSizeLong
        },
        {
          title: this.$t('i18n_1303e638b5'),
          key: 'modifyTimeLong',
          width: '180px',
          ellipsis: true,

          sorter: (a, b) => a.modifyTimeLong - b.modifyTimeLong
        },
        {
          title: this.$t('i18n_2b6bc0f293'),
          key: 'operation',
          width: '180px',
          align: 'center',
          fixed: 'right'
        }
      ],

      rules: {
        url: [
          {
            required: true,
            message: this.$t('i18n_0221d43e46'),
            trigger: 'change'
          }
        ]
      },
      addFileFolderVisible: false,
      // 目录1 文件2 标识
      addFileOrFolderType: 1,
      fileFolderName: '',
      oldFileFolderName: '',
      renameFileFolderVisible: false,
      editLoading: false,
      confirmLoading: false,
      selectedKeys: [],
      expandedKeys: []
    }
  },
  computed: {
    fileUploadDisabled() {
      return this.uploadFileList.length === 0 || this.uploading
    },
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
    this.loadData()
  },
  methods: {
    renderSize,
    uploadPieces,
    formatDuration,
    parseTime,
    // 加载数据
    loadData() {
      const key = 'root-' + new Date().getTime()
      this.tempNode = {
        filename: this.$t('i18n_cfeff30d2c') + (this.absPath || ''),
        level: 1,
        isDirectory: true,
        key: key,
        isLeaf: false,
        activeKey: [0]
      }
      this.treeList = [this.tempNode]
      this.selectedKeys = [key]
      this.expandedKeys = [key]
      this.loadFileList()
      // // 设置默认展开第一个
      // setTimeout(() => {
      //   const node = this.treeList[0]
      //   this.tempNode = node
      //   this.expandKeys = [key]
      //   this.loadFileList()
      // }, 1000)
    },
    handleEditFile(record) {
      const params = {
        nodeId: this.nodeId,
        id: this.projectId,
        filePath: this.uploadPath,
        filename: record.filename
      }
      this.editLoading = true
      // 读取文件数据
      readFile(params)
        .then((res) => {
          if (res.code === 200) {
            this.editFileVisible = true
            this.filename = record.filename

            this.fileContent = res.data
          }
        })
        .finally(() => {
          this.editLoading = false
        })
    },
    // 关闭编辑器弹窗
    handleCloseModal() {
      this.fileContent = ''
      this.editFileVisible = false
    },

    updateFileData() {
      const params = {
        nodeId: this.nodeId,
        id: this.projectId,
        filePath: this.uploadPath,
        filename: this.filename,
        fileText: this.fileContent
      }

      updateFile(params).then((res) => {
        if (res.code === 200) {
          $notification.success({
            message: res.msg
          })
        }
      })
    },

    // 点击树节点
    nodeClick(selectedKeys, { node }) {
      if (node.dataRef.isDirectory) {
        if (node.dataRef.key === this.tempNode.key) {
          return
        }
        this.tempNode = node.dataRef
        this.expandedKeys = [this.tempNode.key]
        this.loadFileList()
      }
    },
    // 上传文件
    handleUpload() {
      if (Object.keys(this.tempNode).length === 0) {
        $notification.error({
          message: this.$t('i18n_bcaf69a038')
        })
        return
      }
      //初始化成功数
      this.successSize = 0
      this.uploadFileList = []
      this.uploading = false
      this.percentage = 0
      this.percentageInfo = {}
      this.uploadFileVisible = true
    },
    // handleRemove(file) {},

    // 开始上传文件
    startUpload() {
      // 设置上传状态
      this.uploading = true
      this.successSize = 0
      // 遍历上传文件
      concurrentExecution(
        this.uploadFileList.map((item, index) => {
          // console.log(item);
          return index
        }),
        1,
        (curItem) => {
          const file = this.uploadFileList[curItem]
          this.uploadFileList = this.uploadFileList.map((fileItem, fileIndex) => {
            if (fileIndex === curItem) {
              fileItem.status = 'uploading'
            }
            return fileItem
          })
          this.percentage = 0
          this.percentageInfo = {}
          return new Promise((resolve, reject) => {
            uploadPieces({
              file,
              resolveFileProcess: (msg) => {
                this.globalLoading({
                  spinning: true,
                  tip: msg
                })
              },
              resolveFileEnd: () => {
                this.globalLoading(false)
              },
              process: (process, end, total, duration) => {
                this.percentage = Math.max(this.percentage, process)
                this.percentageInfo = { end, total, duration }
              },
              success: (uploadData, name) => {
                // 准备合并
                shardingMerge({
                  ...uploadData[0],
                  nodeId: this.nodeId,
                  id: this.projectId,
                  levelName: this.uploadPath
                }).then((res) => {
                  if (res.code === 200) {
                    this.successSize++
                    $notification.success({
                      message: name + ' ' + res.msg
                    })
                    this.uploadFileList = this.uploadFileList.map((fileItem, fileIndex) => {
                      if (fileIndex === curItem) {
                        fileItem.status = 'done'
                      }
                      return fileItem
                    })

                    resolve()
                  } else {
                    this.uploadFileList = this.uploadFileList.map((fileItem, fileIndex) => {
                      if (fileIndex === curItem) {
                        fileItem.status = 'error'
                      }
                      return fileItem
                    })
                    reject()
                  }
                })
              },
              error: (msg) => {
                this.uploadFileList = this.uploadFileList.map((fileItem, fileIndex) => {
                  if (fileIndex === curItem) {
                    fileItem.status = 'error'
                  }
                  return fileItem
                })
                $notification.error({
                  message: msg
                })
                reject()
              },
              uploadCallback: (formData) => {
                return new Promise((resolve, reject) => {
                  formData.append('nodeId', this.nodeId)
                  formData.append('id', this.projectId)
                  // 计算属性 uploadPath
                  formData.append('levelName', this.uploadPath)

                  // 上传文件
                  uploadProjectFile(formData)
                    .then((res) => {
                      if (res.code === 200) {
                        resolve()
                      } else {
                        reject()
                      }
                    })
                    .catch(() => {
                      reject()
                    })
                })
              }
            })
          })
        }
      ).then(() => {
        this.uploading = this.successSize !== this.uploadFileList.length
        // // 判断是否全部上传完成
        if (!this.uploading) {
          this.uploadFileList = []
          setTimeout(() => {
            this.percentage = 0
            this.percentageInfo = {}
            this.loadFileList()
            this.uploadFileVisible = false
          }, 2000)
        }
      })
    },
    // 上传压缩文件
    handleZipUpload() {
      if (Object.keys(this.tempNode).length === 0) {
        $notification.error({
          message: this.$t('i18n_bcaf69a038')
        })
        return
      }
      this.uploadData = {}
      this.successSize = 0
      this.uploadFileList = []
      this.uploading = false
      this.percentage = 0
      this.percentageInfo = {}
      this.uploadZipFileVisible = true
    },

    // 开始上传压缩文件
    startZipUpload() {
      // 设置上传状态
      this.uploading = true
      this.percentage = 0
      this.percentageInfo = {}
      const file = this.uploadFileList[0]
      uploadPieces({
        file,
        resolveFileProcess: (msg) => {
          this.globalLoading({
            spinning: true,
            tip: msg
          })
        },
        resolveFileEnd: () => {
          this.globalLoading(false)
        },
        process: (process, end, total, duration) => {
          this.percentage = Math.max(this.percentage, process)
          this.percentageInfo = { end, total, duration }
        },
        success: (uploadData, name) => {
          // 准备合并
          // formData.append("type", "unzip");
          // formData.append("stripComponents", this.uploadData.stripComponents || 0);
          // formData.append("clearType", this.uploadData.checkBox ? "clear" : "noClear");
          shardingMerge({
            ...uploadData[0],
            nodeId: this.nodeId,
            id: this.projectId,
            levelName: this.uploadPath,
            type: 'unzip',
            stripComponents: this.uploadData.stripComponents || 0,
            clearType: this.uploadData.checkBox ? 'clear' : 'noClear'
          }).then((res) => {
            if (res.code === 200) {
              this.successSize++
              $notification.success({
                message: name + ' ' + res.msg
              })
              this.uploading = this.successSize !== this.uploadFileList.length
              // // 判断是否全部上传完成
              if (!this.uploading) {
                this.uploadFileList = []
                setTimeout(() => {
                  this.percentage = 0
                  this.percentageInfo = {}
                  this.loadFileList()
                  this.uploadZipFileVisible = false
                }, 2000)
              }
            }
          })
        },
        error: (msg) => {
          $notification.error({
            message: msg
          })
        },
        uploadCallback: (formData) => {
          return new Promise((resolve, reject) => {
            formData.append('nodeId', this.nodeId)
            formData.append('id', this.projectId)
            // 计算属性 uploadPath
            formData.append('levelName', this.uploadPath)
            formData.append('type', 'unzip')
            formData.append('stripComponents', this.uploadData.stripComponents || 0)
            formData.append('clearType', this.uploadData.checkBox ? 'clear' : 'noClear')

            // 上传文件
            uploadProjectFile(formData)
              .then((res) => {
                if (res.code === 200) {
                  resolve()
                } else {
                  reject()
                }
              })
              .catch(() => {
                reject()
              })
          })
        }
      })

      // // 上传文件
      // const file = this.uploadFileList[0];
      // const formData = new FormData();

      // // 上传文件
      // uploadProjectFile(formData).then((res) => {
      //   if (res.code === 200) {
      //     $notification.success({
      //       message: res.msg,
      //     });
      //     this.successSize++;
      //     this.percentage = 100;
      //     setTimeout(() => {
      //       this.percentage = 0;
      //       this.uploading = false;
      //       clearInterval(this.timer);
      //       this.uploadData = false;
      //       this.uploadFileList = [];
      //       this.loadFileList();
      //     }, 1000);
      //   }
      //   this.percentage = 0;
      // });
    },
    //打开远程上传
    openRemoteUpload() {
      // this.$refs.ruleForm.resetFields();
      this.uploadRemoteFileVisible = true
    },
    //关闭远程上传
    closeRemoteUpload() {
      //   this.$refs.ruleForm.resetFields();
      this.uploadRemoteFileVisible = false
    },
    //处理上传文件
    handleRemoteUpload() {
      this.$refs.ruleForm.validate().then(() => {
        const params = {
          id: this.projectId,
          nodeId: this.nodeId,
          url: this.remoteDownloadData.url,
          levelName: this.uploadPath,
          unzip: this.remoteDownloadData.unzip,
          stripComponents: this.remoteDownloadData.stripComponents || 0
        }
        this.confirmLoading = true
        remoteDownload(params)
          .then((res) => {
            if (res.code === 200) {
              $notification.success({
                message: res.msg
              })
              this.remoteDownloadData = {}
              this.uploadRemoteFileVisible = false
              this.loadFileList()
            }
          })
          .finally(() => {
            this.confirmLoading = false
          })
      })
    },
    /**
     * 根据key获取树节点
     * @param keys
     * @returns {*}
     */
    getTreeNode(keys) {
      let node = this.treeList.find((node) => node.activeKey[0] == keys.slice(0, 1)[0])
      const nodeKeys = keys.slice(1)
      for (let [index, key] of nodeKeys.entries()) {
        if (key >= 0 && key < node.children.length) {
          node = node.children.find((node) => node.activeKey.slice(index + 1, index + 2) == key)
        } else {
          throw new Error('Invalid key: ' + key)
        }
      }
      return node
    },
    /**
     * 更新树节点的方法抽离封装
     * @param keys
     * @param value
     */
    updateTreeChildren(keys, value) {
      const node = this.getTreeNode(keys)
      node.children = value
    },
    /**
     * 文件列表转树结构
     * @param data
     */
    fileList2TreeData(data) {
      const node = this.tempNode
      const children = data
        .filter((ele) => {
          return ele.isDirectory
        })
        .map((ele, index) => {
          ele.isLeaf = !ele.isDirectory
          ele.key = ele.filename + '-' + new Date().getTime()
          ele.activeKey = node.activeKey.concat([index])
          return ele
        })
      this.updateTreeChildren(node.activeKey, children)
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
        path: this.uploadPath
      }
      this.loading = true
      this.fileList = []
      // 加载文件
      getFileList(params).then((res) => {
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
          this.fileList2TreeData(res.data)
        }
        this.loading = false
      })
    },
    // 清空文件
    clearFile() {
      const msg = this.uploadPath
        ? this.$t('i18n_c840c88b7c') + this.uploadPath + this.$t('i18n_3f553922ae')
        : this.$t('i18n_26bd746dc3')
      $confirm({
        title: this.$t('i18n_c4535759ee'),
        content: msg,
        okText: this.$t('i18n_e83a256e4f'),
        zIndex: 1009,
        cancelText: this.$t('i18n_625fb26b4b'),
        onOk: () => {
          return deleteProjectFile({
            nodeId: this.nodeId,
            id: this.projectId,
            type: 'clear',
            levelName: this.uploadPath
          }).then((res) => {
            if (res.code === 200) {
              $notification.success({
                message: res.msg
              })
              this.loadFileList()
            }
          })
        }
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
        filename: record.filename
      }
      window.open(downloadProjectFile(params), '_blank')
    },
    // 删除
    handleDelete(record) {
      const msg = record.isDirectory
        ? this.$t('i18n_3cc09369ad') + record.filename + this.$t('i18n_52a8df6678')
        : this.$t('i18n_3cc09369ad') + record.filename + this.$t('i18n_48e79b3340')
      $confirm({
        title: this.$t('i18n_c4535759ee'),
        content: msg,
        okText: this.$t('i18n_e83a256e4f'),
        zIndex: 1009,
        cancelText: this.$t('i18n_625fb26b4b'),
        onOk: () => {
          return deleteProjectFile({
            nodeId: this.nodeId,
            id: this.projectId,
            levelName: record.levelName,
            filename: record.filename
          }).then((res) => {
            if (res.code === 200) {
              $notification.success({
                message: res.msg
              })
              this.loadFileList()
            }
          })
        }
      })
    },
    goConsole() {
      this.$emit('goConsole')
    },
    goReadFile(record) {
      // const filePath = this.uploadPath + record.filename;
      this.$emit('goReadFile', this.uploadPath, record.filename)
    },
    handleAddFile(type) {
      this.addFileFolderVisible = true
      this.addFileOrFolderType = type
      this.fileFolderName = ''
    },
    // 确认新增文件  目录
    startAddFileFolder() {
      const params = {
        nodeId: this.nodeId,
        id: this.projectId,
        levelName: this.uploadPath,
        filename: this.fileFolderName,
        unFolder: this.addFileOrFolderType === 1 ? false : true
      }
      newFileFolder(params).then((res) => {
        if (res.code === 200) {
          $notification.success({
            message: res.msg
          })
          this.addFileFolderVisible = false
          this.loadFileList()
        }
      })
    },
    handleRenameFile(record) {
      this.renameFileFolderVisible = true
      this.fileFolderName = record.filename
      this.oldFileFolderName = record.filename
    },
    // 确认修改文件 目录名称
    renameFileFolder() {
      const params = {
        nodeId: this.nodeId,
        id: this.projectId,
        levelName: this.uploadPath,
        newname: this.fileFolderName,
        filename: this.oldFileFolderName
      }
      renameFileFolder(params).then((res) => {
        if (res.code === 200) {
          $notification.success({
            message: res.msg
          })
          this.renameFileFolderVisible = false
          this.loadFileList()
        }
      })
    },
    // 查看备份列表
    backupList() {
      this.backupListVisible = true
    },
    // 复制文件
    hannderCopy(record) {
      const params = {
        nodeId: this.nodeId,
        id: this.projectId,
        filePath: this.uploadPath,
        filename: record.filename
      }
      copyFileFolder(params).then((res) => {
        if (res.code === 200) {
          $notification.success({
            message: res.msg
          })
          this.loadFileList()
        }
      })
    },
    // 压缩文件
    hannderCompress(record, type) {
      const params = {
        nodeId: this.nodeId,
        id: this.projectId,
        filePath: this.uploadPath,
        filename: record.filename,
        type: type
      }
      compressFileFolder(params).then((res) => {
        if (res.code === 200) {
          $notification.success({
            message: res.msg
          })
          this.loadFileList()
        }
      })
    }
  }
}
</script>
<style lang="less" scoped>
:deep(.n-progress-icon--as-text) {
  width: auto;
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
.tree-container {
  overflow-x: auto;
  :deep(.n-tree-node-content__text) {
    word-break: keep-all;
    white-space: nowrap;
  }
  :deep(.n-tree-node-content) {
    display: flex;
    align-items: center;
  }
  :deep(.n-tree-node--selected > .n-tree-node-content) {
    background-color: #1677ff;
    color: #fff;
  }
}
</style>
