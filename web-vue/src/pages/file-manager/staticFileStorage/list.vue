<template>
  <div>
    <div>
      <!-- 数据表格 -->
            <n-card size="small" :body-style="{ padding: '12px' }" style="margin-bottom: 12px">

          <n-space>
            <n-input
              v-model:value="listQuery['%name%']"
              :placeholder="$t('i18n_d2e2560089')"
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
            <!-- <n-button type="primary" @click="handleUpload">上传文件</n-button> -->
            <n-button type="primary" @click="reScanner">{{ $t('i18n_56525d62ac') }}</n-button>

            <n-button
              type="primary"
              danger
              :disabled="!tableSelections || tableSelections.length <= 0"
              @click="handleBatchDelete"
            >
              {{ $t('i18n_7fb62b3011') }}
            </n-button>
            <n-button
              size="small"
              text
              @click="
                () => {
                  configDir = true
                }
              "
            >
              <InfoCircleOutlined /> {{ $t('i18n_1e5533c401') }}
            </n-button>
          </n-space>
        
      </n-card>
<n-data-table
        :data="list"
        size="medium"
        :columns="columns"
        :pagination="pagination"
        bordered
        :row-key="(row) => row.id"
        :row-selection="rowSelection"
        @change="
          (pagination, filters, sorter) => {
            listQuery = CHANGE_PAGE(listQuery, { pagination, sorter })
            loadData()
          }
        "
      >
        
        <template #bodyCell="{ column, text, record }">
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
          <template v-else-if="column.key === 'id'">
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
          <template v-else-if="column.key === 'name'">
            <n-popover>
              <template #trigger>
                <span class="tw">
                  <!-- {{ text }} -->
                  <n-button text style="padding: 0" size="small" @click="handleEdit(record)">{{ text }}</n-button>
                </span>
              </template>
              <template #header>{{ $t('i18n_73485331c2') }}</template>

              <p>{{ $t('i18n_f37f8407ec') }}{{ record.id }}</p>
              <p>{{ $t('i18n_b9c52d9a85') }}{{ text }}</p>
              <p>{{ $t('i18n_46a04cdc9c') }}{{ record.description }}</p>
            </n-popover>
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
          <template v-else-if="column.key === 'source'">
            <n-tooltip placement="topLeft">
              <template #trigger>
                <span class="tw">
                  <span class="tw">
                    <span>{{ sourceMap[text] || $t('i18n_1622dc9b6b') }}</span>
                  </span>
                </span>
              </template>
              `${sourceMap[text] || $t('i18n_1622dc9b6b')}`
            </n-tooltip>
          </template>

          <template v-else-if="column.key === 'status'">
            <n-tag v-if="text === 1" color="green">{{ $t('i18n_df9497ea98') }}</n-tag>
            <n-tag v-else color="red">{{ $t('i18n_162e219f6d') }}</n-tag>
          </template>

          <template v-else-if="column.key === 'type'">
            <n-tag v-if="text === 1">{{ $t('i18n_2a0c4740f1') }}</n-tag>
            <n-tag v-else>{{ $t('i18n_1f4c1042ed') }}</n-tag>
          </template>

          <template v-else-if="column.key === 'operation'">
            <n-space>
              <!-- <n-button type="primary" size="small" @click="handleEdit(record)">编辑</n-button> -->
              <n-button
                size="small"
                :disabled="!(record.status === 1 && record.type === 1)"
                type="primary"
                @click="handleDownloadUrl(record)"
              >
                {{ $t('i18n_f26ef91424') }}</n-button
              >
              <n-button
                size="small"
                :disabled="!(record.status === 1 && record.type === 1)"
                type="primary"
                @click="handleReleaseFile(record)"
                >{{ $t('i18n_83611abd5f') }}</n-button
              >
              <n-button type="primary" danger size="small" @click="handleDelete(record)">{{
                $t('i18n_2f4aaddde3')
              }}</n-button>
            </n-space>
          </template>
        </template>
      </n-data-table>

      <!-- 编辑文件 -->
      <CustomModal
        v-if="editVisible"
        v-model:open="editVisible"
        destroy-on-close
        :title="`${$t('i18n_5c3b53e66c')}`"
        :confirm-loading="confirmLoading"
        :mask-closable="false"
        @ok="handleEditOk"
      >
        <n-form ref="editForm" :rules="rules" :model="temp">
          <n-form-item :label="$t('i18n_29139c2a1a')" path="name">
            <n-input v-model:value="temp.name" :placeholder="$t('i18n_29139c2a1a')" :disabled="true" />
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
        :title="$t('i18n_7e930b95ef')"
        width="70%"
        :mask-closable="false"
        :confirm-loading="confirmLoading"
        @ok="releaseFileOk()"
      >
        <releaseFile
          v-if="releaseFileVisible"
          ref="releaseFile"
          :file-type="2"
          :file-id="temp.fileId"
          @commit="handleCommitTask"
        ></releaseFile>
      </CustomModal>
    </div>

    <!-- 配置工作空间授权目录 -->
    <CustomModal
      v-if="configDir"
      v-model:open="configDir"
      destroy-on-close
      :title="`${$t('i18n_eee6510292')}`"
      :footer="null"
      width="50vw"
      :mask-closable="false"
      @cancel="
        () => {
          configDir = false
        }
      "
    >
      <whiteList
        v-if="configDir"
        @cancel="
          () => {
            configDir = false
            loadData()
          }
        "
      ></whiteList>
    </CustomModal>
  </div>
</template>
<script>
import { DownloadOutlined, InfoCircleOutlined } from '@ant-design/icons-vue'

import {
  CHANGE_PAGE,
  COMPUTED_PAGINATION,
  PAGE_DEFAULT_LIST_QUERY,
  parseTime,
  renderSize,
  formatDuration,
  randomStr
} from '@/utils/const'
// import { uploadFile, uploadFileMerge, hasFile } from "@/api/file-manager/file-storage";
import { staticFileStorageList, delFile, triggerUrl, fileEdit, staticScanner } from '@/api/file-manager/static-storage'

import releaseFile from '@/pages/file-manager/fileStorage/releaseFile'
import { addReleaseTask } from '@/api/file-manager/release-task-log'
import whiteList from '@/pages/dispatch/white-list'
export default {
  components: {
    releaseFile,
    whiteList
  },
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
      loading: true,
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      list: [],
      columns: [
        {
          title: this.$t('i18n_d7ec2d3fea'),
          key: 'name',
          ellipsis: true,
          width: 150
        },
        {
          title: this.$t('i18n_3bdd08adab'),
          key: 'description',
          ellipsis: true,
          width: 150
        },
        {
          title: this.$t('i18n_4f35e80da6'),
          key: 'absolutePath',
          ellipsis: true,
          width: 150
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
          title: this.$t('i18n_226b091218'),
          key: 'type',
          ellipsis: true,

          width: '80px'
        },
        {
          title: this.$t('i18n_a3d0154996'),
          key: 'status',
          ellipsis: true,

          width: '80px'
        },

        {
          title: this.$t('i18n_e06caa0060'),
          key: 'lastModified',
          sorter: true,
          render: (row) => parseTime(row['lastModified']),
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

      fileList: [],
      percentage: 0,
      percentageInfo: {},
      uploading: false,
      uploadVisible: false,
      editVisible: false,
      configDir: false,
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
      staticFileStorageList(this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data.result
          this.listQuery.total = res.data.total
        }
        this.loading = false
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
      this.$refs['editForm']?.restoreValidation()
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
      }).catch(() => {})
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
            id: record.id,
            thorough: false
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
        $notification.warning({
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
          return delFile({
            ids: this.tableSelections.join(','),
            thorough: false
          }).then((res) => {
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
      this.temp = { fileId: record.id }
    },

    handleCommitTask(data) {
      this.confirmLoading = true
      addReleaseTask({ ...data, fileId: this.temp.fileId, fileType: 2 })
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
    },
    // 扫描
    reScanner() {
      staticScanner({}).then((res) => {
        if (res.code === 200) {
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
<style scoped>
:deep(.n-progress-icon--as-text) {
  width: auto;
}
</style>
