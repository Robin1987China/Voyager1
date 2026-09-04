<template>
  <!-- 布局 -->
  <n-layout class="ssh-file-layout">
    <!-- 目录树 -->
    <n-layout-sider theme="light" class="sider" width="25%">
      <n-grid class="dir-container">
        <n-space>
          <n-button size="small" type="primary" @click="loadData()">{{ $t('i18n_694fc5efa9') }}</n-button>
          <n-dropdown
            :options="
              sortMethodList.map((item) => ({
                label: item.name,
                key: item.key,
                props: { onClick: () => changeSort(item.key, sortMethod.asc) }
              }))
            "
          >
            <n-button
              size="small"
              type="primary"
              @click="
                () => {
                  changeSort(sortMethod.key, !sortMethod.asc)
                }
              "
            >
              {{
                sortMethodList.find((item) => {
                  return item.key === sortMethod.key
                }) &&
                sortMethodList.find((item) => {
                  return item.key === sortMethod.key
                }).name
              }}{{ $t('i18n_c360e994db') }}
              <SortAscendingOutlined v-if="sortMethod.asc" />
              <SortDescendingOutlined v-else />
            </n-button>
          </n-dropdown>
        </n-space>
      </n-grid>
      <n-empty v-if="treeList.length === 0" />
      <n-spin v-else :tip="$t('i18n_f013ea9dcb')" :spinning="loading">
        <div class="tree-container">
          <n-tree
            v-model:selected-keys="selectedKeys"
            v-model:expanded-keys="expandedKeys"
            :data="treeList"
            :label-field="'name'"
            :key-field="'key'"
            :children-field="'children'"
            @update:selected-keys="onSelect"
            @expand="
              (expandedKeys, { expanded, node }) => {
                if (expanded) {
                  onSelect(expandedKeys, { node })
                }
              }
            "
          ></n-tree>
        </div>
      </n-spin>
    </n-layout-sider>
    <!-- 表格 -->
    <n-layout-content class="file-content">
      <!-- <div ref="filter" class="filter"></div> -->
            <n-card size="small" :body-style="{ padding: '12px' }" style="margin-bottom: 12px">

          <n-space>
            <n-dropdown
              :disabled="!tempNode.nextPath"
              :options="[
                {
                  label: $t('i18n_a6fc9e3ae6'),
                  key: '0',
                  icon: () => h(NIcon, null, { default: () => h(FileAddOutlined) }),
                  props: { onClick: () => handleUpload }
                },
                {
                  label: $t('i18n_66b71b06c6'),
                  key: '1',
                  icon: () => h(NIcon, null, { default: () => h(FileZipOutlined) }),
                  props: { onClick: () => handleUploadZip }
                }
              ]"
            >
              <n-button size="small" type="primary" @click="(e) => e.preventDefault()">{{
                $t('i18n_01198a1673')
              }}</n-button>
            </n-dropdown>
            <n-button
              size="small"
              :disabled="!tempNode.nextPath"
              type="primary"
              @click="uploadShardingFileVisible = true"
              >{{ $t('i18n_dda8b4c10f') }}</n-button
            >
            <n-dropdown
              :disabled="!tempNode.nextPath"
              :options="[
                {
                  label: $t('i18n_547ee197e5'),
                  key: '0',
                  icon: () => h(NIcon, null, { default: () => h(FolderAddOutlined) }),
                  props: { onClick: () => handleAddFolder() }
                },
                {
                  label: $t('i18n_497ddf508a'),
                  key: '1',
                  icon: () => h(NIcon, null, { default: () => h(FileAddOutlined) }),
                  props: { onClick: () => handleAddFile() }
                }
              ]"
            >
              <n-button size="small" type="primary" @click="(e) => e.preventDefault()">{{
                $t('i18n_26bb841878')
              }}</n-button>
            </n-dropdown>
            <n-button size="small" :disabled="!tempNode.nextPath" type="primary" @click="loadFileList()">{{
              $t('i18n_694fc5efa9')
            }}</n-button>
            <n-button size="small" :disabled="!tempNode.nextPath" type="primary" danger @click="handleDeletePath()">{{
              $t('i18n_2f4aaddde3')
            }}</n-button>
            <div>
              {{ $t('i18n_4cbc136874') }}
              <n-switch
                v-model:value="listShowDir"
                :disabled="!tempNode.nextPath"
                :checked-label="$t('i18n_4d775d4cd7')"
                :unchecked-label="$t('i18n_dce5379cb9')"
                @change="changeListShowDir"
              />
            </div>
            <span v-if="nowPath">{{ $t('i18n_4e33dde280') }}{{ nowPath }}</span>
            <!-- <span v-if="this.nowPath">{{ this.tempNode.parentDir }}</span> -->
          </n-space>
        
      </n-card>
<n-data-table
        size="medium"
        :data="sortFileList"
        :loading="loading"
        :columns="columns"
        :pagination="false"
        bordered
        >
        

        <template #bodyCell="{ column, text, record }">
          <template v-if="column.key === 'name'">
            <n-tooltip placement="top-start">
              <template #trigger>
                <span class="tw">
                  <span class="tw">
                    <n-dropdown
                      :trigger="['contextmenu']"
                      :options="[
                        {
                          label: $t('i18n_c8ce4b36cb'),
                          key: '0',
                          icon: () => h(NIcon, null, { default: () => h(HighlightOutlined) }),
                          props: { onClick: () => handleRenameFile(record) }
                        }
                      ]"
                    >
                      <div>{{ text }}</div>
                    </n-dropdown>

                    <!-- <span>{{ text }}</span> -->
                  </span>
                </span>
              </template>

              <div>{{ $t('i18n_551e46c0ea') }}{{ text }}</div>
              <div>{{ $t('i18n_964d939a96') }}{{ record.longname }}</div>
            </n-tooltip>
          </template>
          <template v-else-if="column.key === 'dir'">
            <n-tooltip placement="topLeft">
              <template #trigger>
                <span class="tw">
                  <span class="tw">
                    <span>{{
                      record.link ? $t('i18n_bfe68d5844') : text ? $t('i18n_767fa455bb') : $t('i18n_2a0c4740f1')
                    }}</span>
                  </span>
                </span>
              </template>
              `${record.link ? $t('i18n_bfe68d5844') : text ? $t('i18n_767fa455bb') : $t('i18n_2a0c4740f1')}`
            </n-tooltip>
          </template>
          <template v-else-if="column.key === 'size'">
            <n-tooltip placement="topLeft">
              <template #trigger>
                <span class="tw">
                  <span class="tw">
                    <span>{{ renderSize(text) }}</span>
                  </span>
                </span>
              </template>
              renderSize(text)
            </n-tooltip>
          </template>
          <template v-else-if="column.tooltip">
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
          <template v-else-if="column.key === 'operation'">
            <n-space>
              <n-tooltip>
                <template #trigger>
                  <span class="tw">
                    <n-button
                      size="small"
                      type="primary"
                      :disabled="!record.textFileEdit"
                      @click="handleEdit(record)"
                      >{{ $t('i18n_95b351c862') }}</n-button
                    >
                  </span>
                </template>
                $t('i18n_af0df2e295')
              </n-tooltip>
              <n-tooltip>
                <template #trigger>
                  <span class="tw">
                    <n-button size="small" type="primary" @click="handleFilePermission(record)">{{
                      $t('i18n_ba6e91fa9e')
                    }}</n-button>
                  </span>
                </template>
                $t('i18n_5cc7e8e30a')
              </n-tooltip>
              <n-button size="small" type="primary" :disabled="record.dir" @click="handleDownload(record)">{{
                $t('i18n_f26ef91424')
              }}</n-button>
              <n-button size="small" type="primary" danger @click="handleDelete(record)">{{
                $t('i18n_2f4aaddde3')
              }}</n-button>
            </n-space>
          </template>
        </template>
      </n-data-table>
      <!-- 上传文件 -->
      <CustomModal
        v-if="uploadFileVisible"
        v-model:open="uploadFileVisible"
        destroy-on-close
        width="300px"
        :title="$t('i18n_a6fc9e3ae6')"
        :confirm-loading="confirmLoading"
        :footer="null"
        :mask-closable="true"
        @cancel="closeUploadFile"
      >
        <n-upload
          v-model:file-list="uploadFileList"
          :custom-request="beforeUpload"
          :accept="`${uploadFileZip ? ZIP_ACCEPT : ''}`"
          :multiple="!uploadFileZip"
          @remove="handleRemove"
        >
          <n-button>
            <UploadOutlined />
            {{ $t('i18n_fd7e0c997d') }}
            {{ uploadFileZip ? $t('i18n_c806d0fa38') : '' }}
          </n-button>
        </n-upload>
        <br />
        <n-button
          type="primary"
          :disabled="uploadFileList.length === 0"
          :loading="confirmLoading"
          @click="startUpload"
          >{{ $t('i18n_020f1ecd62') }}</n-button
        >
      </CustomModal>
      <!-- 分片上传 -->
      <CustomModal
        v-if="uploadShardingFileVisible"
        v-model:open="uploadShardingFileVisible"
        destroy-on-close
        :confirm-loading="confirmLoading"
        :closable="!confirmLoading"
        :keyboard="false"
        width="35vw"
        :title="$t('i18n_d65551b090')"
        :footer="null"
        :mask-closable="false"
      >
        <n-space direction="vertical" size="large" style="width: 100%">
          <n-alert :title="$t('i18n_776bf504a4')" type="warning">
            <template #description>
              <ul>
                <li>
                  {{ $t('i18n_383952103d') }}
                </li>
                <li>{{ $t('i18n_d85279c536') }}</li>
              </ul>
            </template>
          </n-alert>
          <n-upload
            v-model:file-list="uploadFileList"
            :custom-request="
              (file) => {
                uploadFileList = [file]
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
                </template>
              </n-progress>
            </n-grid-item>
          </n-grid>

          <n-button type="primary" :disabled="fileUploadDisabled" @click="startUploadSharding">{{
            $t('i18n_020f1ecd62')
          }}</n-button>
        </n-space>
      </CustomModal>
      <!--  新增文件 目录    -->
      <CustomModal
        v-if="addFileFolderVisible"
        v-model:open="addFileFolderVisible"
        width="300px"
        :title="temp.addFileOrFolderType === 1 ? $t('i18n_2d9e932510') : $t('i18n_e48a715738')"
        :footer="null"
        :mask-closable="true"
      >
        <n-space direction="vertical" style="width: 100%">
          <span v-if="nowPath">{{ $t('i18n_4e33dde280') }}{{ nowPath }}</span>
          <!-- <n-tag v-if="">目录创建成功后需要手动刷新右边树才能显示出来哟</n-tag> -->
          <n-tooltip>
            <template #trigger>
              <n-input v-model:value="temp.fileFolderName" :placeholder="$t('i18n_55939c108f')" />
            </template>
            temp.addFileOrFolderType === 1 ? $t('i18n_fe1b192913') : ''
          </n-tooltip>
          <n-grid type="flex" justify="center">
            <n-button
              type="primary"
              :disabled="!temp.fileFolderName || temp.fileFolderName.length === 0"
              @click="startAddFileFolder"
              >{{ $t('i18n_e83a256e4f') }}</n-button
            >
          </n-grid>
        </n-space>
      </CustomModal>
      <!-- 编辑文件 -->
      <CustomModal
        v-if="editFileVisible"
        v-model:open="editFileVisible"
        destroy-on-close
        :confirm-loading="confirmLoading"
        width="80vw"
        :title="$t('i18n_47ff744ef6')"
        :cancel-text="$t('i18n_b15d91274e')"
        :mask-closable="true"
        @ok="updateFileData"
      >
        <code-editor v-model:content="temp.fileContent" height="60vh" show-tool :file-suffix="temp.name">
          <template #tool_before>
            <n-tag>
              {{
                ((temp.allowPathParent || '/ ') + '/' + (temp.nextPath || '/') + '/' + (temp.name || '/')).replace(
                  new RegExp('//+', 'gm'),
                  '/'
                )
              }}
              <!-- {{ temp.name }} -->
            </n-tag>
          </template>
        </code-editor>
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
          <n-input v-model:value="temp.fileFolderName" :placeholder="$t('i18n_f139c5cf32')" />

          <n-grid v-if="temp.fileFolderName" type="flex" justify="center">
            <n-button
              :loading="confirmLoading"
              type="primary"
              :disabled="temp.fileFolderName.length === 0 || temp.fileFolderName === temp.oldFileFolderName"
              @click="renameFileFolder"
              >{{ $t('i18n_e83a256e4f') }}</n-button
            >
          </n-grid>
        </n-space>
      </CustomModal>

      <!-- 修改文件权限 -->
      <CustomModal
        v-if="editFilePermissionVisible"
        v-model:open="editFilePermissionVisible"
        destroy-on-close
        width="400px"
        :title="`${$t('i18n_5cc7e8e30a')}`"
        :footer="null"
        :mask-closable="true"
      >
        <n-grid>
          <n-grid-item :span="6"
            ><span class="title">{{ $t('i18n_ba6e91fa9e') }}</span></n-grid-item
          >
          <n-grid-item :span="6"
            ><span class="title">{{ $t('i18n_8306971039') }}</span></n-grid-item
          >
          <n-grid-item :span="6"
            ><span class="title">{{ $t('i18n_e72a0ba45a') }}</span></n-grid-item
          >
          <n-grid-item :span="6"
            ><span class="title">{{ $t('i18n_0d98c74797') }}</span></n-grid-item
          >
        </n-grid>
        <n-grid>
          <n-grid-item :span="6">
            <span>{{ $t('i18n_75769d1ac8') }}</span>
          </n-grid-item>
          <n-grid-item :span="6">
            <n-checkbox v-model:value="permissions.owner.read" />
          </n-grid-item>
          <n-grid-item :span="6">
            <n-checkbox v-model:value="permissions.group.read" />
          </n-grid-item>
          <n-grid-item :span="6">
            <n-checkbox v-model:value="permissions.others.read" />
          </n-grid-item>
        </n-grid>
        <n-grid>
          <n-grid-item :span="6">
            <span>{{ $t('i18n_4d7dc6c5f8') }}</span>
          </n-grid-item>
          <n-grid-item :span="6">
            <n-checkbox v-model:value="permissions.owner.write" />
          </n-grid-item>
          <n-grid-item :span="6">
            <n-checkbox v-model:value="permissions.group.write" />
          </n-grid-item>
          <n-grid-item :span="6">
            <n-checkbox v-model:value="permissions.others.write" />
          </n-grid-item>
        </n-grid>
        <n-grid>
          <n-grid-item :span="6">
            <span>{{ $t('i18n_1a6aa24e76') }}</span>
          </n-grid-item>
          <n-grid-item :span="6">
            <n-checkbox v-model:value="permissions.owner.execute" />
          </n-grid-item>
          <n-grid-item :span="6">
            <n-checkbox v-model:value="permissions.group.execute" />
          </n-grid-item>
          <n-grid-item :span="6">
            <n-checkbox v-model:value="permissions.others.execute" />
          </n-grid-item>
        </n-grid>
        <n-grid type="flex" style="margin-top: 20px">
          <n-button type="primary" @click="updateFilePermissions">{{ $t('i18n_49e56c7b90') }}</n-button>
        </n-grid>
        <!-- <n-grid>
            <n-alert style="margin-top: 20px" :title="permissionTips" type="success" />
          </n-grid> -->
      </CustomModal>
    </n-layout-content>
  </n-layout>
</template>
<script>
import { LoadingOutlined, SortAscendingOutlined, SortDescendingOutlined, UploadOutlined } from '@ant-design/icons-vue'

import { h } from 'vue'
import { NIcon } from 'naive-ui'
import { HighlightOutlined, FileAddOutlined, FileZipOutlined, FolderAddOutlined } from '@ant-design/icons-vue'

import {
  deleteFile,
  downloadFile,
  getFileList,
  getRootFileList,
  newFileFolder,
  readFile,
  renameFileFolder,
  updateFileData,
  uploadFile,
  parsePermissions,
  calcFilePermissionValue,
  changeFilePermission,
  uploadShardingFile
} from '@/api/ssh-file'

import codeEditor from '@/components/codeEditor'
import { ZIP_ACCEPT, renderSize, parseTime, concurrentExecution, formatDuration } from '@/utils/const'
import { uploadPieces } from '@/utils/upload-pieces'
import { NEmpty as Empty } from 'naive-ui'
export default {
  components: {
    codeEditor
  },
  inject: ['globalLoading'],
  props: {
    sshId: {
      type: String,
      default: ''
    },
    machineSshId: {
      type: String,
      default: ''
    }
  },
  setup() {
    // 模板内联 dropdown options 的 icon 函数需访问模块作用域的 h/NIcon/图标组件
    return { h, NIcon, FileAddOutlined, FileZipOutlined, FolderAddOutlined, HighlightOutlined }
  },
  data() {
    return {
      Empty,
      loading: false,
      treeList: [],
      fileList: [],
      uploadFileList: [],
      tempNode: {},
      temp: {},
      uploadFileVisible: false,
      uploadFileZip: false,
      ZIP_ACCEPT: ZIP_ACCEPT,
      renameFileFolderVisible: false,
      listShowDir: false,
      tableHeight: '80vh',
      replaceFields: {
        children: 'children',
        title: 'name',
        key: 'key'
      },
      columns: [
        {
          title: this.$t('i18n_d2e2560089'),
          key: 'name',
          width: 200,
          ellipsis: true,

          sorter: (a, b) => (a.name || '').localeCompare(b.name || '')
        },
        {
          title: this.$t('i18n_28b988ce6a'),
          key: 'dir',
          width: '100px',
          ellipsis: true
        },
        {
          title: this.$t('i18n_396b7d3f91'),
          key: 'size',
          width: 120,
          ellipsis: true,

          sorter: (a, b) => Number(a.size) - new Number(b.size)
        },
        {
          title: this.$t('i18n_ba6e91fa9e'),
          key: 'permissions',
          width: 120,
          ellipsis: true
        },
        {
          title: this.$t('i18n_1303e638b5'),
          key: 'modifyTime',
          width: '170px',
          ellipsis: true,
          render: (row) => parseTime(row['modifyTime']),
          sorter: (a, b) => Number(a.modifyTime) - new Number(b.modifyTime)
        },
        {
          title: this.$t('i18n_2b6bc0f293'),
          key: 'operation',
          align: 'center',
          fixed: 'right',

          width: '220px'
        }
      ],

      editFileVisible: false,
      addFileFolderVisible: false,
      editFilePermissionVisible: false,
      permissions: {
        owner: { read: false, write: false, execute: false },
        group: { read: false, write: false, execute: false },
        others: { read: false, write: false, execute: false }
      },
      // permissionTips: "",
      sortMethodList: [
        {
          name: this.$t('i18n_29139c2a1a'),
          key: 'name'
        },
        {
          name: this.$t('i18n_1303e638b5'),
          key: 'modifyTime'
        }
      ],

      sortMethod: {
        key: 'name',
        asc: true
      },
      confirmLoading: false,
      selectedKeys: [],
      expandedKeys: [],
      uploadShardingFileVisible: false,
      percentage: 0,
      percentageInfo: {}
    }
  },
  computed: {
    fileUploadDisabled() {
      return this.uploadFileList.length === 0 || this.confirmLoading
    },
    nowPath() {
      if (!this.tempNode.allowPathParent) {
        return ''
      }
      return ((this.tempNode.allowPathParent || '') + '/' + (this.tempNode.nextPath || '')).replace(
        new RegExp('//+', 'gm'),
        '/'
      )
    },
    baseUrl() {
      if (this.sshId) {
        return '/node/ssh/'
      }
      return '/system/assets/ssh-file/'
    },
    reqDataId() {
      return this.sshId || this.machineSshId
    },
    sortFileList() {
      return this.fileList.slice(0).sort((a, b) => {
        const aV = a[this.sortMethod.key] || ''
        const bV = b[this.sortMethod.key] || ''
        return this.sortMethod.asc ? bV.localeCompare(aV) : aV.localeCompare(bV)
      })
    }
  },
  mounted() {
    this.listShowDir = Boolean(localStorage.getItem('ssh-list-show-dir'))
    try {
      this.sortMethod = JSON.parse(localStorage.getItem('ssh-list-sort') || JSON.stringify(this.sortMethod))
    } catch (e) {
      console.error(e)
    }
    this.loadData()
  },
  methods: {
    formatDuration,
    changeSort(key, asc) {
      this.sortMethod = { key: key, asc: asc }
      localStorage.setItem('ssh-list-sort', JSON.stringify(this.sortMethod))
      this.loadData()
    },
    renderSize,
    // 加载数据
    loadData() {
      this.loading = true
      this.treeList = []
      this.fileList = []
      this.selectedKeys = []
      this.expandedKeys = []
      this.tempNode = {}
      getRootFileList(this.baseUrl, this.reqDataId).then((res) => {
        if (res.code === 200) {
          this.treeList = res.data
            .map((element, index) => {
              return {
                key: element.id,
                name: element.allowPathParent,
                allowPathParent: element.allowPathParent,
                nextPath: '/',
                isLeaf: false,
                // 配置的授权目录可能不存在
                disabled: !!element.error,
                modifyTime: element.modifyTime,
                activeKey: [index]
              }
            })
            .sort((a, b) => {
              const aV = a[this.sortMethod.key] || ''
              const bV = b[this.sortMethod.key] || ''
              return this.sortMethod.asc ? bV.localeCompare(aV) : aV.localeCompare(bV)
            })
        }
        this.loading = false
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
        .filter((element) => element.dir)
        .map((element) => ({
          key: element.id,
          name: element.name,
          allowPathParent: node.allowPathParent,
          nextPath: (element.nextPath + '/' + element.name).replace(new RegExp('//+', 'gm'), '/'),
          isLeaf: !element.dir,
          // 可能有错误
          disabled: !!element.error,
          modifyTime: element.modifyTime
        }))
        .sort((a, b) => {
          const aV = a[this.sortMethod.key] || ''
          const bV = b[this.sortMethod.key] || ''
          return this.sortMethod.asc ? bV.localeCompare(aV) : aV.localeCompare(bV)
        })
        .map((element, index) => ({ ...element, activeKey: node.activeKey.concat(index) }))
      this.updateTreeChildren(node.activeKey, children)
    },
    /**
     * 加载文件列表
     */
    loadTreeNode() {
      const { allowPathParent, nextPath } = this.tempNode
      // 请求参数
      const params = {
        id: this.reqDataId,
        allowPathParent: allowPathParent,
        nextPath: nextPath
      }
      this.fileList = []
      this.loading = true
      // 加载文件
      getFileList(this.baseUrl, params).then((res) => {
        if (res.code === 200) {
          // let children = []
          // 区分目录和文件
          res.data.forEach((element) => {
            if (element.dir) {
              if (this.listShowDir) {
                this.fileList.push({
                  // path: node.dataRef.path,
                  ...element
                })
              }
            } else {
              // 设置文件表格
              this.fileList.push({
                // path: node.dataRef.path,
                ...element
              })
            }
          })
          //  更新tree 方法抽离封装
          this.fileList2TreeData(res.data)
        }
        this.loading = false
      })
    },
    // 选中目录
    onSelect(selectedKeys, { node }) {
      if (node.dataRef.disabled) {
        return
      }
      // console.log(node.dataRef, this.tempNode.key);
      if (node.dataRef.key === this.tempNode.key) {
        return
      }
      this.tempNode = node.dataRef
      this.loadTreeNode()
    },
    changeListShowDir() {
      this.loadFileList()
      localStorage.setItem('ssh-list-show-dir', this.listShowDir)
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
        id: this.reqDataId,
        allowPathParent: this.tempNode.allowPathParent,
        nextPath: this.tempNode.nextPath
      }
      // this.fileList = [];
      this.loading = true
      // 加载文件
      getFileList(this.baseUrl, params).then((res) => {
        if (res.code === 200) {
          // 区分目录和文件
          this.fileList = res.data
            .filter((element) => {
              if (this.listShowDir) {
                return true
              }
              return !element.dir
            })
            .map((element) => {
              // 设置文件表格
              return {
                // path: this.tempNode.path,
                ...element
              }
            })
          // 更新tree
          this.fileList2TreeData(res.data)
        }
        this.loading = false
      })
    },
    // 上传文件
    handleUpload() {
      if (Object.keys(this.tempNode).length === 0) {
        $notification.error({
          message: this.$t('i18n_bcaf69a038')
        })
        return
      }
      this.uploadFileVisible = true
      this.uploadFileZip = false
    },
    handleUploadZip() {
      this.handleUpload()
      this.uploadFileZip = true
    },
    handleAddFolder() {
      this.addFileFolderVisible = true
      // 目录1 文件2 标识
      // addFileOrFolderType: 1,
      //       fileFolderName: "",
      this.temp = {
        fileFolderName: '',
        addFileOrFolderType: 1,
        allowPathParent: this.tempNode.allowPathParent,
        nextPath: this.tempNode.nextPath
      }
    },
    handleAddFile() {
      this.addFileFolderVisible = true
      // 目录1 文件2 标识
      // addFileOrFolderType: 1,
      //       fileFolderName: "",
      this.temp = {
        fileFolderName: '',
        addFileOrFolderType: 2,
        allowPathParent: this.tempNode.allowPathParent,
        nextPath: this.tempNode.nextPath
      }
    },
    // closeAddFileFolder() {
    //   this.addFileFolderVisible = false;
    //   this.fileFolderName = "";
    // },
    // 确认新增文件  目录
    startAddFileFolder() {
      const params = {
        id: this.reqDataId,
        allowPathParent: this.temp.allowPathParent,
        nextPath: this.temp.nextPath,
        name: this.temp.fileFolderName,
        unFolder: this.temp.addFileOrFolderType !== 1
      }
      newFileFolder(this.baseUrl, params).then((res) => {
        if (res.code === 200) {
          $notification.success({
            message: res.msg
          })
          this.addFileFolderVisible = false
          this.loadFileList()
          // this.closeAddFileFolder();
        }
      })
    },
    handleRemove(file) {
      const index = this.uploadFileList.indexOf(file)
      const newFileList = this.uploadFileList.slice()
      newFileList.splice(index, 1)
      this.uploadFileList = newFileList
      return true
    },
    beforeUpload({ file, onFinish, onError }) {
      this.uploadFileList = [...this.uploadFileList, file]
      return false
    },
    closeUploadFile() {
      this.uploadFileList = []
    },
    // 开始上传文件
    startUpload() {
      this.uploadFileList.forEach((file) => {
        const formData = new FormData()
        formData.append('file', file.file)
        formData.append('id', this.reqDataId)
        formData.append('allowPathParent', this.tempNode.allowPathParent)
        formData.append('unzip', this.uploadFileZip)
        formData.append('nextPath', this.tempNode.nextPath)
        this.confirmLoading = true
        // 上传文件
        uploadFile(this.baseUrl, formData)
          .then((res) => {
            if (res.code === 200) {
              $notification.success({
                message: res.msg
              })
              this.loadFileList()
              this.closeUploadFile()
              this.uploadFileVisible = false
            }
          })
          .finally(() => {
            this.confirmLoading = false
          })
      })
    },
    startUploadSharding() {
      // 设置上传状态
      this.confirmLoading = true
      // 遍历上传文件
      concurrentExecution(
        this.uploadFileList.map((item, index) => {
          // console.log(item);
          return index
        }),
        // 并发数只能是 1
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
              /** ssh 文件上传 目前切片并发控制为1 */
              concurrentNum: 1,
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
              success: () => {
                // 合并
                $notification.success({
                  message: this.$t('i18n_a7699ba731')
                })
                this.uploadFileList = this.uploadFileList.map((fileItem, fileIndex) => {
                  if (fileIndex === curItem) {
                    fileItem.status = 'done'
                  }
                  return fileItem
                })

                resolve()
              },
              uploadChunkError: () => {
                this.confirmLoading = false
                this.percentage = 0
                this.percentageInfo = {}
                this.uploadFileList = []
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
                this.confirmLoading = false
                this.percentage = 0
                this.percentageInfo = {}
                reject()
              },
              uploadCallback: (formData) => {
                return new Promise((resolve, reject) => {
                  formData.append('id', this.reqDataId)
                  formData.append('allowPathParent', this.tempNode.allowPathParent)
                  formData.append('unzip', this.uploadFileZip)
                  formData.append('nextPath', this.tempNode.nextPath)

                  // 上传文件
                  uploadShardingFile(this.baseUrl, formData)
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
      )
        .then(() => {
          //this.uploading = this.successSize !== this.uploadFileList.length
          // // 判断是否全部上传完成
          // if (!this.uploading) {
          //   this.uploadFileList = []
          //   setTimeout(() => {
          //     this.loadFileList()
          //     this.uploadFileVisible = false
          //   }, 2000)
          // }
          this.percentage = 0
          this.percentageInfo = {}
          this.uploadFileList = []
          this.loadFileList()
          this.uploadShardingFileVisible = false
        })
        .finally(() => {
          this.confirmLoading = false
          //
        })
    },
    // 编辑
    handleEdit(record) {
      this.temp = Object.assign({}, record)
      const params = {
        id: this.reqDataId,
        allowPathParent: record.allowPathParent,
        nextPath: record.nextPath,
        name: record.name
      }
      readFile(this.baseUrl, params).then((res) => {
        if (res.code === 200) {
          this.temp = { ...this.temp, fileContent: res.data }
          this.editFileVisible = true
        }
      })
      //
    },
    updateFileData() {
      const params = {
        id: this.reqDataId,
        allowPathParent: this.temp.allowPathParent,
        nextPath: this.temp.nextPath,
        name: this.temp.name,
        content: this.temp.fileContent
      }
      this.confirmLoading = true
      updateFileData(this.baseUrl, params)
        .then((res) => {
          if (res.code === 200) {
            $notification.success({
              message: res.msg
            })
            this.editFileVisible = false
          }
        })
        .finally(() => {
          this.confirmLoading = false
        })
    },
    // 修改文件权限
    handleFilePermission(record) {
      this.temp = Object.assign({}, record)
      this.permissions = parsePermissions(this.temp.permissions)
      //const permissionsValue = calcFilePermissionValue(this.permissions);
      //this.permissionTips = `cd ${this.temp.nextPath} && chmod ${permissionsValue} ${this.temp.name}`;
      this.editFilePermissionVisible = true
    },
    // 更新文件权限提示
    renderFilePermissionsTips() {
      //const permissionsValue = calcFilePermissionValue(this.permissions);
      //this.permissionTips = `cd ${this.temp.nextPath} && chmod ${permissionsValue} ${this.temp.name}`;
    }, // 确认修改文件权限
    updateFilePermissions() {
      // 请求参数
      const params = {
        id: this.reqDataId,
        allowPathParent: this.temp.allowPathParent,
        nextPath: this.temp.nextPath,
        fileName: this.temp.name,
        permissionValue: calcFilePermissionValue(this.permissions)
      }
      changeFilePermission(this.baseUrl, params).then((res) => {
        if (res.code === 200) {
          $notification.success({
            message: res.msg
          })
          this.editFilePermissionVisible = false
          this.loadFileList()
        }
      })
    },

    // 下载
    handleDownload(record) {
      // 请求参数
      const params = {
        id: this.reqDataId,
        allowPathParent: record.allowPathParent,
        nextPath: record.nextPath,
        name: record.name
      }
      // 请求接口拿到 blob
      window.open(downloadFile(this.baseUrl, params), '_blank')
    },
    // 删除文件夹
    handleDeletePath() {
      $confirm({
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        content: this.$t('i18n_8756efb8f4'),
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
        onOk: async () => {
          return deleteFile(this.baseUrl, {
            id: this.reqDataId,
            allowPathParent: this.tempNode.allowPathParent,
            nextPath: this.tempNode.nextPath
          }).then((res) => {
            if (res.code === 200) {
              $notification.success({
                message: res.msg
              })
              // 刷新树
              const activeKey = this.tempNode.activeKey
              // 获取上一级节点
              const parentNode = this.getTreeNode(activeKey.slice(0, activeKey.length - 1))
              // 设置当前选中
              this.selectedKeys = [parentNode.key]
              // 设置缓存节点
              this.tempNode = parentNode
              // 加载上一级文件列表
              this.loadTreeNode()

              this.fileList = []
              //this.loadFileList();
            }
          })
        }
      })
    },
    // 删除
    handleDelete(record) {
      $confirm({
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        content: this.$t('i18n_3a6bc88ce0'),
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
        onOk: () => {
          return deleteFile(this.baseUrl, {
            id: this.reqDataId,
            allowPathParent: record.allowPathParent,
            nextPath: record.nextPath,
            name: record.name
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
    handleRenameFile(record) {
      this.renameFileFolderVisible = true
      this.temp = {
        fileFolderName: record.name,
        oldFileFolderName: record.name,
        allowPathParent: record.allowPathParent,
        nextPath: record.nextPath
      }
    },
    // 确认修改文件 目录名称
    renameFileFolder() {
      const params = {
        id: this.reqDataId,
        name: this.temp.oldFileFolderName,
        newname: this.temp.fileFolderName,
        allowPathParent: this.temp.allowPathParent,
        nextPath: this.temp.nextPath
      }
      this.confirmLoading = true
      renameFileFolder(this.baseUrl, params)
        .then((res) => {
          if (res.code === 200) {
            $notification.success({
              message: res.msg
            })
            this.renameFileFolderVisible = false
            this.loadFileList()
          }
        })
        .finally(() => {
          this.confirmLoading = false
        })
    }
  }
}
</script>
<style lang="less" scoped>
:deep(.n-progress-icon--as-text) {
  width: auto;
}
.ssh-file-layout {
  padding: 0;
  min-height: calc(100vh - 75px);
}
.dir-container {
  padding: 10px;
  border-bottom: 1px solid #eee;
}
.sider {
  border: 1px solid #e2e2e2;
  /* overflow-x: auto; */
}
.file-content {
  margin: 10px 10px 0;
  padding: 10px;
  /* background-color: #fff; */
}
.title {
  font-weight: 600;
  font-size: larger;
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
