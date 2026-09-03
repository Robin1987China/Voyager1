<template>
  <div>
    <div>
      <!-- 数据表格 -->
      <n-data-table
        :data="list"
        size="medium"
        :columns="columns"
        :pagination="pagination"
        bordered
        :row-key="(row) => row.id"
        :row-selection="rowSelection"
        :scroll="{
          x: 'max-content'
        }"
        @change="
          (pagination, filters, sorter) => {
            listQuery = CHANGE_PAGE(listQuery, { pagination, sorter })
            loadData()
          }
        "
      >
        <template #title>
          <n-space>
            <n-input
              v-model:value="listQuery['%name%']"
              :placeholder="$t('i18n_d2e2560089')"
              class="search-input-item"
              @press-enter="loadData"
            />
            <n-input
              v-model:value="listQuery['%aliasCode%']"
              :placeholder="$t('i18n_2f5e828ecd')"
              class="search-input-item"
              @press-enter="loadData"
            />
            <n-input
              v-model:value="listQuery['extName']"
              :placeholder="$t('i18n_ae809e0295')"
              class="search-input-item"
              @press-enter="loadData"
            />
            <n-input
              v-model:value="listQuery['id']"
              :placeholder="$t('i18n_2168394b82')"
              class="search-input-item"
              @press-enter="loadData"
            />
            <n-tooltip>
              <template #trigger>
                <span class="tw">
                  <n-button type="primary" :loading="loading" @click="loadData">{{ $t('i18n_e5f71fc31e') }}</n-button>
                </span>
              </template>
              $t('i18n_4838a3bd20')
            </n-tooltip>
            <n-button type="primary" @click="handleUpload">{{ $t('i18n_a6fc9e3ae6') }}</n-button>
            <n-button type="primary" @click="handleRemoteDownload">{{ $t('i18n_bd7043cae3') }}</n-button>
            <n-button
              type="primary"
              danger
              :disabled="!tableSelections || tableSelections.length <= 0"
              @click="handleBatchDelete"
            >
              {{ $t('i18n_7fb62b3011') }}
            </n-button>
          </n-space>
        </template>
        <template #bodyCell="{ column, text, record }">
          <template v-if="column.tooltip">
            <n-tooltip placement="topLeft">
              <template #trigger>
                <span class="tw">
                  <span class="tw">
                    <span>{{ (text || '').slice(0, 8) }}</span>
                  </span>
                </span>
              </template>
              text
            </n-tooltip>
          </template>
          <template v-else-if="column.dataIndex === 'id'">
            <n-tooltip placement="topLeft">
              <template #trigger>
                <span class="tw">
                  <span class="tw">
                    <span v-if="record.status === 0 || record.status === 2">-</span>
                    <span v-else>{{ (text || '').slice(0, 8) }}</span>
                  </span>
                </span>
              </template>
              text
            </n-tooltip>
          </template>
          <template v-else-if="column.dataIndex === 'name'">
            <n-popover>
              <template #trigger>
                <span class="tw">
                  <!-- {{ text }} -->
                  <n-button text style="padding: 0" size="small" @click="handleEdit(record)">{{ text }}</n-button>
                </span>
              </template>
              <template #header>{{ $t('i18n_73485331c2') }}</template>

              <p>{{ $t('i18n_b9c52d9a85') }}{{ text }}</p>
              <p>{{ $t('i18n_46a04cdc9c') }}{{ record.description }}</p>
              <p v-if="record.status !== undefined">
                {{ $t('i18n_53365c29c8') }}{{ statusMap[record.status] || $t('i18n_1622dc9b6b') }}
              </p>
              <p v-if="record.progressDesc">{{ $t('i18n_fb3a2241bb') }}{{ record.progressDesc }}</p>
            </n-popover>
          </template>

          <template v-else-if="column.dataIndex === 'size'">
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
          <template v-else-if="column.dataIndex === 'source'">
            <n-tooltip placement="topLeft">
              <template #trigger>
                <span class="tw">
                  <span class="tw">
                    <span>{{ sourceMap[text] || $t('i18n_1622dc9b6b') }}</span>
                  </span>
                </span>
              </template>
              sourceMap[text] || $t('i18n_1622dc9b6b')
            </n-tooltip>
          </template>

          <template v-else-if="column.dataIndex === 'exists'">
            <n-tag v-if="text" color="green">{{ $t('i18n_df9497ea98') }}</n-tag>
            <n-tag v-else color="red">{{ $t('i18n_162e219f6d') }}</n-tag>
          </template>
          <template v-else-if="column.dataIndex === 'workspaceId'">
            <n-tag v-if="text === 'GLOBAL'">{{ $t('i18n_2be75b1044') }}</n-tag>
            <n-tag v-else>{{ $t('i18n_98d69f8b62') }}</n-tag>
          </template>
          <template v-else-if="column.dataIndex === 'operation'">
            <n-space>
              <!-- <n-button type="primary" size="small" @click="handleEdit(record)">编辑</n-button> -->
              <n-button size="small" :disabled="!record.exists" type="primary" @click="handleDownloadUrl(record)">{{
                $t('i18n_f26ef91424')
              }}</n-button>
              <n-button size="small" :disabled="!record.exists" type="primary" @click="handleReleaseFile(record)">{{
                $t('i18n_83611abd5f')
              }}</n-button>
              <n-button type="primary" danger size="small" @click="handleDelete(record)">{{
                $t('i18n_2f4aaddde3')
              }}</n-button>
            </n-space>
          </template>
        </template>
      </n-data-table>
      <!-- 上传文件 -->
      <CustomModal
        v-if="uploadVisible"
        v-model:open="uploadVisible"
        destroy-on-close
        :confirm-loading="confirmLoading"
        :closable="!uploading"
        :footer="uploading ? null : undefined"
        :keyboard="false"
        :title="`${$t('i18n_a6fc9e3ae6')}`"
        :mask-closable="false"
        @ok="handleUploadOk"
      >
        <n-form ref="form" :rules="rules" :model="temp">
          <n-form-item :label="$t('i18n_fd7e0c997d')" path="file">
            <n-progress v-if="percentage" :percent="percentage">
              <template #format="percent">
                {{ percent }}%
                <template v-if="percentageInfo.total"> ({{ renderSize(percentageInfo.total) }}) </template>
                <template v-if="percentageInfo.duration">
                  {{ $t('i18n_e710da3487') }}:{{ formatDuration(percentageInfo.duration) }}
                </template>
              </template>
            </n-progress>

            <n-upload
              v-model:file-list="fileList"
              :disabled="!!percentage"
              :custom-request="
                (file) => {
                  // 只允许上传单个文件
                  fileList = [file]
                  return false
                }
              "
              @remove="
                (file) => {
                  fileList = []
                  return true
                }
              "
            >
              <LoadingOutlined v-if="percentage" />
              <n-tag v-if="percentage >= 100">
                {{
                  fileList.find((fileItem, fileIndex) => {
                    return (fileItem.status = 'uploading')
                  })?.name
                }}{{ $t('i18n_ade63665b2') }}</n-tag
              >
              <n-button v-else type="primary"><UploadOutlined />{{ $t('i18n_fd7e0c997d') }}</n-button>
            </n-upload>
          </n-form-item>
          <n-form-item :label="$t('i18n_824607be6b')" path="keepDay">
            <n-input-number
              v-model:value="temp.keepDay"
              :min="1"
              style="width: 100%"
              :placeholder="$t('i18n_e9ea1e7c02')"
            />
          </n-form-item>
          <n-form-item :label="$t('i18n_3a6970ac26')" path="global">
            <n-radio-group v-model:value="temp.global">
              <n-radio :value="true"> {{ $t('i18n_2be75b1044') }} </n-radio>
              <n-radio :value="false"> {{ $t('i18n_691b11e443') }} </n-radio>
            </n-radio-group>
          </n-form-item>
          <n-form-item :label="$t('i18n_2f5e828ecd')" path="aliasCode" :help="$t('i18n_41638b0a48')">
            <n-input
              v-model:value="temp.aliasCode"
              :max-length="50"
              :placeholder="$t('i18n_8fbcdbc785')"
              @keyup.enter="
                () => {
                  temp = { ...temp, aliasCode: randomStr(6) }
                }
              "
            >
              <template #enterButton>
                <n-button type="primary"> {{ $t('i18n_6709f4548f') }} </n-button>
              </template>
            </n-input>
          </n-form-item>
          <n-form-item :label="$t('i18n_8d6f38b4b1')" path="description">
            <n-input v-model:value="temp.description" type="textarea" :placeholder="$t('i18n_411672c954')" />
          </n-form-item>
        </n-form>
      </CustomModal>
      <!-- 编辑文件 -->
      <CustomModal
        v-if="editVisible"
        v-model:open="editVisible"
        destroy-on-close
        :confirm-loading="confirmLoading"
        :title="`${$t('i18n_5c3b53e66c')}`"
        :mask-closable="false"
        @ok="handleEditOk"
      >
        <n-form ref="editForm" :rules="rules" :model="temp">
          <n-form-item :label="$t('i18n_29139c2a1a')" path="name">
            <n-input v-model:value="temp.name" :placeholder="$t('i18n_29139c2a1a')" />
            <!-- [\s\\/:\*\?\"<>\|] -->
            <template #help>{{ $t('i18n_9f4a0d67c6') }}</template>
          </n-form-item>
          <n-form-item :label="$t('i18n_824607be6b')" path="keepDay">
            <n-input-number
              v-model:value="temp.keepDay"
              :min="1"
              style="width: 100%"
              :placeholder="$t('i18n_e9ea1e7c02')"
            />
          </n-form-item>
          <n-form-item :label="$t('i18n_3a6970ac26')" path="global">
            <n-radio-group v-model:value="temp.global">
              <n-radio :value="true"> {{ $t('i18n_2be75b1044') }} </n-radio>
              <n-radio :value="false"> {{ $t('i18n_691b11e443') }} </n-radio>
            </n-radio-group>
          </n-form-item>
          <n-form-item :label="$t('i18n_2f5e828ecd')" path="aliasCode" :help="$t('i18n_41638b0a48')">
            <n-input
              v-model:value="temp.aliasCode"
              :max-length="50"
              :placeholder="$t('i18n_8fbcdbc785')"
              @keyup.enter="
                () => {
                  temp = { ...temp, aliasCode: randomStr(6) }
                }
              "
            >
              <template #enterButton>
                <n-button type="primary"> {{ $t('i18n_6709f4548f') }} </n-button>
              </template>
            </n-input>
          </n-form-item>
          <n-form-item :label="$t('i18n_8d6f38b4b1')" path="description">
            <n-input v-model:value="temp.description" type="textarea" :placeholder="$t('i18n_411672c954')" />
          </n-form-item>
        </n-form>
      </CustomModal>
      <!--远程下载  -->
      <CustomModal
        v-if="uploadRemoteFileVisible"
        v-model:open="uploadRemoteFileVisible"
        destroy-on-close
        :title="$t('i18n_5d488af335')"
        :mask-closable="false"
        :confirm-loading="confirmLoading"
        @ok="handleRemoteUpload"
      >
        <n-form ref="remoteForm" :model="temp" :rules="rules">
          <n-form-item :label="$t('i18n_a66fff7541')" path="url">
            <n-input v-model:value="temp.url" :placeholder="$t('i18n_7457228a61')" />
          </n-form-item>
          <n-form-item :label="$t('i18n_824607be6b')" path="keepDay">
            <n-input-number
              v-model:value="temp.keepDay"
              :min="1"
              style="width: 100%"
              :placeholder="$t('i18n_e9ea1e7c02')"
            />
          </n-form-item>
          <n-form-item :label="$t('i18n_3a6970ac26')" path="global">
            <n-radio-group v-model:value="temp.global">
              <n-radio :value="true"> {{ $t('i18n_2be75b1044') }} </n-radio>
              <n-radio :value="false"> {{ $t('i18n_691b11e443') }} </n-radio>
            </n-radio-group>
          </n-form-item>
          <n-form-item :label="$t('i18n_2f5e828ecd')" path="aliasCode" :help="$t('i18n_41638b0a48')">
            <n-input
              v-model:value="temp.aliasCode"
              :max-length="50"
              :placeholder="$t('i18n_8fbcdbc785')"
              @keyup.enter="
                () => {
                  temp = { ...temp, aliasCode: randomStr(6) }
                }
              "
            >
              <template #enterButton>
                <n-button type="primary"> {{ $t('i18n_6709f4548f') }} </n-button>
              </template>
            </n-input>
          </n-form-item>
          <n-form-item :label="$t('i18n_8d6f38b4b1')" path="description">
            <n-input v-model:value="temp.description" type="textarea" :placeholder="$t('i18n_411672c954')" />
          </n-form-item>
        </n-form>
      </CustomModal>
      <!-- 断点下载 -->
      <CustomModal
        v-if="triggerVisible"
        v-model:open="triggerVisible"
        destroy-on-close
        :title="$t('i18n_e7e8d4c1fb')"
        width="50%"
        :footer="null"
        :mask-closable="false"
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
                $t('i18n_84415a6bb1')
              </n-tooltip>
            </template>
            <n-tab-pane name="1" :tab="$t('i18n_0b58866c3e')">
              <n-space direction="vertical" style="width: 100%">
                <n-alert type="info" :title="`${$t('i18n_d911cffcd5')}(${$t('i18n_00a070c696')})`">
                  <template #description>
                    <n-p style="margin-bottom: 0">
                      <n-tag>GET</n-tag>
                      <span>{{ `${temp.triggerDownloadUrl}` }} </span>
                      <copy-text :text="temp.triggerDownloadUrl" />
                    </n-p>
                  </template>
                </n-alert>
                <a :href="temp.triggerDownloadUrl" target="_blank">
                  <n-button size="small" type="primary"><DownloadOutlined />{{ $t('i18n_2a813bc3eb') }}</n-button>
                </a>
              </n-space>
            </n-tab-pane>
            <n-tab-pane v-if="temp.triggerAliasDownloadUrl" :tab="$t('i18n_d61af4e686')">
              <n-space direction="vertical" style="width: 100%">
                <n-alert :title="$t('i18n_947d983961')" type="warning">
                  <template #description>
                    <ul>
                      <li>
                        {{ $t('i18n_ac762710a5') }}=createTimeMillis:desc

                        <p>{{ $t('i18n_35fbad84cb') }}</p>
                      </li>
                      <li>{{ $t('i18n_c83752739f') }}</li>
                      <li>{{ $t('i18n_4055a1ee9c') }}</li>
                    </ul>
                  </template>
                </n-alert>
                <n-alert type="info" :title="`${$t('i18n_d911cffcd5')}(${$t('i18n_00a070c696')})`">
                  <template #description>
                    <n-p style="margin-bottom: 0">
                      <n-tag>GET</n-tag>
                      <span>{{ `${temp.triggerAliasDownloadUrl}` }} </span>
                      <copy-text :text="temp.triggerAliasDownloadUrl" />
                    </n-p>
                  </template>
                </n-alert>
                <a :href="temp.triggerAliasDownloadUrl" target="_blank">
                  <n-button size="small" type="primary"><DownloadOutlined />{{ $t('i18n_2a813bc3eb') }}</n-button>
                </a>
              </n-space>
            </n-tab-pane>
          </n-tabs>
        </n-form>
      </CustomModal>
      <!-- 发布文件 -->
      <CustomModal
        v-if="releaseFileVisible"
        v-model:open="releaseFileVisible"
        destroy-on-close
        :confirm-loading="confirmLoading"
        :title="$t('i18n_7e930b95ef')"
        width="70%"
        :mask-closable="false"
        @ok="releaseFileOk()"
      >
        <releaseFile
          v-if="releaseFileVisible"
          ref="releaseFile"
          :file-type="1"
          :file-id="temp.fileId"
          :alias="temp.aliasCode"
          @commit="handleCommitTask"
        ></releaseFile>
      </CustomModal>
    </div>
    <!-- 选择确认区域 -->
    <div v-if="choose" style="padding-top: 50px">
      <div
        :style="{
          position: 'absolute',
          right: 0,
          bottom: 0,
          width: '100%',
          borderTop: '1px solid #e9e9e9',
          padding: '10px 16px',
          background: '#fff',
          textAlign: 'right',
          zIndex: 1
        }"
      >
        <n-space>
          <n-button
            @click="
              () => {
                $emit('cancel')
              }
            "
          >
            {{ $t('i18n_625fb26b4b') }}
          </n-button>
          <n-button type="primary" @click="handerConfirm">
            {{ $t('i18n_38cf16f220') }}
          </n-button>
        </n-space>
      </div>
    </div>
  </div>
</template>
<script>
import { DownloadOutlined, LoadingOutlined, UploadOutlined } from '@ant-design/icons-vue'

import {
  CHANGE_PAGE,
  COMPUTED_PAGINATION,
  PAGE_DEFAULT_LIST_QUERY,
  parseTime,
  renderSize,
  formatDuration,
  randomStr
} from '@/utils/const'
import {
  fileStorageList,
  uploadFile,
  uploadFileMerge,
  fileEdit,
  hasFile,
  delFile,
  sourceMap,
  remoteDownload,
  statusMap,
  triggerUrl
} from '@/api/file-manager/file-storage'
import { uploadPieces } from '@/utils/upload-pieces'

import releaseFile from './releaseFile.vue'
import { addReleaseTask } from '@/api/file-manager/release-task-log'

export default {
  components: {
    releaseFile
  },
  inject: ['globalLoading'],
  props: {
    choose: {
      // "radio"
      type: String,
      default: ''
    }
  },
  emits: ['cancel', 'confirm'],
  data() {
    return {
      loading: false,
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      list: [],
      columns: [
        {
          title: this.$t('i18n_c11eb9deff'),
          key: 'id',
          ellipsis: true,
          width: 100
        },
        {
          title: this.$t('i18n_d7ec2d3fea'),
          key: 'name',
          ellipsis: true,
          width: 150
        },
        {
          title: this.$t('i18n_2f5e828ecd'),
          key: 'aliasCode',
          ellipsis: true,
          width: 100
        },
        {
          title: this.$t('i18n_58f9666705'),
          key: 'size',
          sorter: true,
          ellipsis: true,

          width: '100px'
        },
        {
          title: this.$t('i18n_242d641eab'),
          key: 'extName',
          ellipsis: true,
          width: '80px'
        },
        {
          title: this.$t('i18n_fffd3ce745'),
          key: 'workspaceId',
          ellipsis: true,

          width: '90px'
        },
        {
          title: this.$t('i18n_26ca20b161'),
          key: 'source',
          ellipsis: true,

          width: '80px'
        },
        {
          title: this.$t('i18n_eaa5d7cb9b'),
          key: 'validUntil',
          sorter: true,
          render: (row) => {
            if (!row['validUntil']) {
              return '-'
            }
            return Math.floor((new Date(Number(row['validUntil'])).getTime() - Date.now()) / (60 * 60 * 24 * 1000))
          },
          width: '100px'
        },
        {
          title: this.$t('i18n_a3d0154996'),
          key: 'exists',
          ellipsis: true,

          width: '80px'
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
          title: this.$t('i18n_eca37cb072'),
          key: 'createTimeMillis',
          sorter: true,
          render: (row) => parseTime(row['createTimeMillis']),
          width: '170px'
        },
        {
          title: this.$t('i18n_1303e638b5'),
          key: 'modifyTimeMillis',
          sorter: true,
          render: (row) => parseTime(row['modifyTimeMillis']),
          width: '170px'
        },
        {
          title: this.$t('i18n_2b6bc0f293'),
          key: 'operation',
          align: 'center',
          ellipsis: true,

          fixed: 'right',
          width: '170px'
        }
      ],

      rules: {
        name: [{ required: true, message: this.$t('i18n_7aa81d1573'), trigger: 'blur' }],
        url: [{ required: true, message: this.$t('i18n_f4dd45fca9'), trigger: 'blur' }]
      },

      temp: {},
      sourceMap,
      statusMap,
      fileList: [],
      percentage: 0,
      percentageInfo: {},
      uploading: false,
      uploadVisible: false,
      editVisible: false,
      uploadRemoteFileVisible: false,

      triggerVisible: false,
      releaseFileVisible: false,
      tableSelections: [],
      confirmLoading: false
    }
  },
  computed: {
    pagination() {
      return COMPUTED_PAGINATION(this.listQuery)
    },
    rowSelection() {
      return {
        onChange: (selectedRowKeys) => {
          this.tableSelections = selectedRowKeys
        },
        selectedRowKeys: this.tableSelections,
        type: this.choose || 'checkbox'
      }
    }
  },
  created() {
    this.loadData()
  },
  methods: {
    randomStr,
    CHANGE_PAGE,
    renderSize,
    formatDuration,
    parseTime,
    // 加载数据
    loadData(pointerEvent) {
      this.loading = true
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page
      fileStorageList(this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data.result
          this.listQuery.total = res.data.total
        }
        this.loading = false
      })
    },
    handleUpload() {
      this.temp = {
        global: false
      }
      this.uploadVisible = true
      this.$refs['form']?.resetFields()
    },
    // 上传文件
    handleUploadOk() {
      // 检验表单
      this.$refs['form'].validate().then(() => {
        // 判断文件
        if (this.fileList.length === 0) {
          $notification.success({
            message: this.$t('i18n_9febf31146')
          })
          return false
        }
        this.percentage = 0
        this.percentageInfo = {}
        this.uploading = true
        this.confirmLoading = true
        uploadPieces({
          file: this.fileList[0],
          resolveFileProcess: (msg) => {
            this.globalLoading({
              spinning: true,
              tip: msg
            })
          },
          resolveFileEnd: () => {
            this.globalLoading(false)
          },
          uploadBeforeAbrot: (md5) => {
            return new Promise((resolve) => {
              hasFile({
                fileSumMd5: md5
              }).then((res) => {
                if (res.code === 200) {
                  if (res.data) {
                    //
                    $notification.warning({
                      message: `${this.$t('i18n_a17b5ab021')},${this.$t('i18n_b9c52d9a85')}${res.data.name} ,${this.$t(
                        'i18n_ee992d9744'
                      )}${res.data.workspaceId === 'GLOBAL' ? this.$t('i18n_0a60ac8f02') : this.$t('i18n_c9744f45e7')}`
                    })
                    //
                    this.uploading = false
                    this.confirmLoading = false
                  } else {
                    resolve()
                  }
                }
              })
            })
          },
          process: (process, end, total, duration) => {
            this.percentage = Math.max(this.percentage, process)
            this.percentageInfo = { end, total, duration }
          },
          success: (uploadData) => {
            // 准备合并
            uploadFileMerge(Object.assign({}, { ...uploadData[0] }, this.temp))
              .then((res) => {
                if (res.code === 200) {
                  this.fileList = []
                  this.loadData()

                  $notification.success({
                    message: res.msg
                  })
                }
                setTimeout(() => {
                  this.percentage = 0
                  this.percentageInfo = {}
                }, 2000)
                this.uploadVisible = false
                this.uploading = false
              })
              .catch(() => {
                this.uploading = false
              })
              .finally(() => {
                this.confirmLoading = false
              })
          },
          error: (msg) => {
            $notification.error({
              message: msg
            })
            this.uploading = false
            this.confirmLoading = false
          },
          uploadCallback: (formData) => {
            return new Promise((resolve, reject) => {
              // 上传文件
              uploadFile(formData)
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

        return true
      })
    },
    // 编辑
    handleEdit(item) {
      this.temp = {
        ...item,
        global: item.workspaceId === 'GLOBAL',
        workspaceId: ''
      }
      this.editVisible = true
      this.$refs['editForm']?.resetFields()
    },
    // 编辑确认
    handleEditOk() {
      this.$refs['editForm'].validate().then(() => {
        this.confirmLoading = true
        fileEdit(this.temp)
          .then((res) => {
            if (res.code === 200) {
              // 成功
              $notification.success({
                message: res.msg
              })

              this.editVisible = false
              this.loadData()
            }
          })
          .finally(() => {
            this.confirmLoading = false
          })
      })
    },
    // 删除文件
    handleDelete(record) {
      $confirm({
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        content: this.$t('i18n_3787283bf4') + record.name,
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
        onOk: () => {
          return delFile({
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
    // 批量删除
    handleBatchDelete() {
      if (!this.tableSelections || this.tableSelections.length <= 0) {
        $notification.error({
          message: this.$t('i18n_5d817c403e')
        })
        return
      }
      $confirm({
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        content: this.$t('i18n_52d24791ab'),
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
        onOk: () => {
          return delFile({ ids: this.tableSelections.join(',') }).then((res) => {
            if (res.code === 200) {
              $notification.success({
                message: res.msg
              })
              this.tableSelections = []
              this.loadData()
            }
          })
        }
      })
    },
    // 远程下载
    handleRemoteDownload() {
      this.uploadRemoteFileVisible = true
      this.temp = {
        global: false
      }
      this.$refs['remoteForm']?.resetFields()
    },
    // 开始远程下载
    handleRemoteUpload() {
      //
      this.$refs['remoteForm'].validate().then(() => {
        this.confirmLoading = true
        remoteDownload(this.temp)
          .then((res) => {
            if (res.code === 200) {
              // 成功
              $notification.success({
                message: res.msg
              })

              this.uploadRemoteFileVisible = false
              this.loadData()
            }
          })
          .finally(() => {
            this.confirmLoading = false
          })
      })
    },
    // 下载地址
    handleDownloadUrl(record) {
      this.temp = Object.assign({}, record)

      triggerUrl({
        id: record.id
      }).then((res) => {
        if (res.code === 200) {
          this.fillDownloadUrlResult(res)
          this.$nextTick(() => {
            this.triggerVisible = true
          })
        }
      })
    },
    // 重置触发器
    resetTrigger() {
      triggerUrl({
        id: this.temp.id,
        rest: 'rest'
      }).then((res) => {
        if (res.code === 200) {
          $notification.success({
            message: res.msg
          })
          this.fillDownloadUrlResult(res)
        }
      })
    },
    fillDownloadUrlResult(res) {
      this.temp = {
        ...this.temp,
        triggerDownloadUrl: `${location.protocol}//${location.host}${res.data.triggerDownloadUrl}`,
        triggerAliasDownloadUrl: res.data?.triggerAliasDownloadUrl
          ? `${location.protocol}//${location.host}${res.data.triggerAliasDownloadUrl}?sort=createTimeMillis:desc`
          : ''
      }
    },
    // 发布文件
    handleReleaseFile(record) {
      this.releaseFileVisible = true
      this.temp = { fileId: record.id, aliasCode: record.aliasCode }
    },

    handleCommitTask(data) {
      this.confirmLoading = true
      addReleaseTask({ ...data, fileId: this.temp.fileId, fileType: 1 })
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
    },

    releaseFileOk() {
      this.$refs.releaseFile?.tryCommit()
    },
    // 选择确认
    handerConfirm() {
      if (!this.tableSelections.length) {
        $notification.warning({
          message: this.$t('i18n_8d62b202d9')
        })
        return
      }
      const selectData = this.list.filter((item) => {
        return this.tableSelections.indexOf(item.id) > -1
      })
      if (!selectData.length) {
        $notification.warning({
          message: this.$t('i18n_8d62b202d9')
        })
        return
      }
      this.$emit('confirm', selectData)
    }
  }
}
</script>
<style scoped>
:deep(.n-progress-icon--as-text) {
  width: auto;
}
</style>
