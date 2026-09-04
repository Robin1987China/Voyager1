<template>
  <div class="">
    <!-- 表格 -->
        <n-card size="small" :body-style="{ padding: '12px' }" style="margin-bottom: 12px">

        <n-space>
          <n-input
            v-model:value="listQuery['%name%']"
            :placeholder="$t('i18n_a1b745fba0')"
            class="search-input-item"
            @press-enter="loadData"
          />
          <n-input
            v-model:value="listQuery['%version%']"
            :placeholder="$t('i18n_0f4f503547')"
            class="search-input-item"
            @press-enter="loadData"
          />
          <n-select
            v-model:value="listQuery.backupType"
            clearable
            :placeholder="$t('i18n_43ebf364ed')"
            class="search-input-item"
            :options="
              backupTypeList.map((backupTypeItem) => ({ label: backupTypeItem.value, value: backupTypeItem.key }))
            "
          />
          <n-tooltip>
            <template #trigger>
              <span class="tw">
                <n-button :loading="loading" type="primary" @click="loadData">{{ $t('i18n_e5f71fc31e') }}</n-button>
              </span>
            </template>
            $t('i18n_4838a3bd20')
          </n-tooltip>
          <n-button type="primary" @click="handleAdd">{{ $t('i18n_a4006e5c1e') }}</n-button>
          <n-button type="primary" @click="handleSqlUpload">{{ $t('i18n_90c0458a4c') }}</n-button>
          <n-button type="primary" @click="handleTrigger()">{{ $t('i18n_4696724ed3') }}</n-button>
        </n-space>
      
    </n-card>
<n-data-table
      size="medium"
      :columns="columns"
      :data="list"
      bordered
      :row-key="(row) => row.id"
      :pagination="pagination"
      @update:page="(page) => changePage({ ...pagination, current: page })"
      @update:page-size="(pageSize) => changePage({ ...pagination, current: 1, pageSize })"
    >
      
      <template #bodyCell="{ column, text, record }">
        <template v-if="column.key === 'name'">
          <n-tooltip>
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
        <template v-else-if="column.key === 'backupType'">
          <n-tooltip>
            <template #trigger>
              <span class="tw">
                <span class="tw">
                  <span>{{ backupTypeMap[text] }}</span>
                </span>
              </span>
            </template>
            text
          </n-tooltip>
        </template>
        <template v-else-if="column.key === 'baleTimeStamp'">
          <n-tooltip>
            <template #trigger>
              {{ parseTime(text) }}
            </template>
            `${parseTime(text)}`
          </n-tooltip>
        </template>
        <template v-else-if="column.key === 'status'">
          <n-tooltip v-if="record.fileExist">
            <template #trigger>
              <span class="tw">
                <div>
                  <n-p style="margin-bottom: 0">
                    {{ backupStatusMap[text] }}
                    <copy-text :text="record.filePath" />
                  </n-p>
                </div>
              </span>
            </template>
            `${backupStatusMap[text]} ${$t('i18n_ae12edc5bf')}`
          </n-tooltip>
          <n-tooltip v-else>
            <template #trigger>
              <WarningOutlined />
            </template>
            `${$t('i18n_96283fc523')}:${record.filePath}`
          </n-tooltip>
        </template>

        <template v-else-if="column.key === 'fileSize'">
          <n-tooltip placement="topLeft">
            <template #trigger>
              <span class="tw">
                <n-tag color="#108ee9">{{ renderSizeFormat(text) }}</n-tag>
              </span>
            </template>
            renderSizeFormat(text) + ' ' + record.sha1Sum
          </n-tooltip>
        </template>

        <template v-else-if="column.key === 'operation'">
          <n-space>
            <n-button
              size="small"
              type="primary"
              :disabled="!record.fileExist || record.status !== 1"
              @click="handleDownload(record)"
              >{{ $t('i18n_f26ef91424') }}</n-button
            >
            <n-button
              size="small"
              type="primary"
              danger
              :disabled="!record.fileExist || record.status !== 1"
              @click="handleRestore(record)"
              >{{ $t('i18n_69de8d7f40') }}</n-button
            >
            <n-button size="small" type="primary" danger @click="handleDelete(record)">{{
              $t('i18n_2f4aaddde3')
            }}</n-button>
          </n-space>
        </template>
      </template>
    </n-data-table>
    <!-- 创建备份信息区 -->
    <CustomModal
      v-if="createBackupVisible"
      v-model:open="createBackupVisible"
      destroy-on-close
      :confirm-loading="confirmLoading"
      :title="$t('i18n_adbec9b14d')"
      width="600px"
      :mask-closable="false"
      @ok="handleCreateBackupOk"
    >
      <n-form ref="editBackupForm" :rules="rules" :model="temp">
        <n-form-item :label="$t('i18n_8c61c92b4b')" path="backupType">
          <n-radio-group v-model:value="temp.backupType" name="backupType">
            <n-radio v-for="item in backupTypeList" v-show="!item.disabled" :key="item.key" :value="item.key">{{
              item.value
            }}</n-radio>
          </n-radio-group>
        </n-form-item>
        <!-- 部分备份 -->
        <n-form-item
          v-if="temp.backupType === 1"
          :label="$t('i18n_b8ac664d98')"
          path="tableNameList"
          class="feature voyager1-role"
        >
          <n-transfer v-model:value="targetKeys" :options="tableNameList" filterable @update:value="handleChange">
            <template #render="item">
              <n-tooltip>
                <template #trigger>
                  {{ item.title }}
                </template>
                item.title
              </n-tooltip>
            </template>
          </n-transfer>
        </n-form-item>
      </n-form>
    </CustomModal>
    <!-- 上传 SQL 备份文件 -->
    <CustomModal
      v-if="uploadSqlFileVisible"
      v-model:open="uploadSqlFileVisible"
      destroy-on-close
      :confirm-loading="confirmLoading"
      width="300px"
      :title="$t('i18n_b5b51ff786')"
      :mask-closable="true"
      @ok="startSqlUpload"
    >
      <n-upload
        v-model:file-list="uploadFileList"
        :custom-request="beforeSqlUpload"
        accept=".sql"
        @remove="handleSqlRemove"
      >
        <n-button><UploadOutlined />{{ $t('i18n_c8c452749e') }}</n-button>
      </n-upload>
      <!-- <br />
        <n-radio-group v-model="backupType" name="backupType">
          <n-radio :value="0">全量备份</n-radio>
          <n-radio :value="1">部分备份</n-radio>
        </n-radio-group>
        <br /> -->
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
          <n-tab-pane name="1" :tab="$t('i18n_664b37da22')">
            <n-space direction="vertical" style="width: 100%">
              <n-alert :title="$t('i18n_947d983961')" type="warning">
                <template #description>
                  <ul>
                    <li>{{ $t('i18n_caed797183') }}</li>
                  </ul>
                </template>
              </n-alert>

              <n-alert type="info">
                <template #message>
                  <n-p style="margin-bottom: 0">{{ $t('i18n_af83388834') }} <copy-text :text="temp.triggerUrl" /></n-p>
                </template>
                <template #description>
                  <n-tag>GET</n-tag> <span>{{ temp.triggerUrl }} </span>
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
import { UploadOutlined, WarningOutlined } from '@ant-design/icons-vue'

import {
  backupStatusMap,
  backupTypeArray,
  backupTypeMap,
  createBackup,
  deleteBackup,
  downloadBackupFile,
  getBackupList,
  getTableNameList,
  restoreBackup,
  uploadBackupFile,
  getTriggerUrl
} from '@/api/backup-info'
import { CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY, parseTime, renderSize } from '@/utils/const'

export default {
  components: {},
  data() {
    return {
      backupTypeMap: backupTypeMap,
      backupStatusMap: backupStatusMap,
      loading: false,
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      backupTypeList: backupTypeArray,
      list: [],
      total: 0,
      tableNameList: [],
      targetKeys: [],
      uploadFileList: [],
      temp: {},
      createBackupVisible: false,
      uploadSqlFileVisible: false,

      backupType: 0,
      triggerVisible: false,
      columns: [
        {
          title: this.$t('i18n_77b9ecc8b1'),
          key: 'name',
          ellipsis: true
        },
        {
          title: this.$t('i18n_2c014aeeee'),
          width: 170,
          key: 'baleTimeStamp',
          // ellipsis: true,
          sorter: true
        },
        {
          title: this.$t('i18n_fe2df04a16'),
          key: 'version',
          width: 100
          // ellipsis: true,
        },
        {
          title: this.$t('i18n_8c61c92b4b'),
          key: 'backupType',
          width: 100,
          ellipsis: true
        },
        {
          title: this.$t('i18n_396b7d3f91'),
          key: 'fileSize',
          width: 100
          // ellipsis: true,
        },
        {
          title: this.$t('i18n_3fea7ca76c'),
          key: 'status',
          width: 120
        },
        // {
        //   title: "文件地址",
        //   key: "filePath",
        //   // width: 150,
        //   ellipsis: true,

        // },
        {
          title: this.$t('i18n_9baca0054e'),
          key: 'modifyUser',
          ellipsis: true,

          width: 120
        },
        {
          title: this.$t('i18n_ae0fd9b9d2'),
          key: 'createTimeMillis',
          sorter: true,
          render: (row) => {
            return parseTime(row['createTimeMillis'])
          },
          width: '170px'
        },
        {
          title: this.$t('i18n_2b6bc0f293'),
          key: 'operation',
          width: '180px',

          align: 'center',
          fixed: 'right'
        }
      ],

      rules: {},
      confirmLoading: false,
      timer: null
    }
  },
  computed: {
    // 分页
    pagination() {
      return COMPUTED_PAGINATION(this.listQuery)
    }
  },
  created() {
    // console.log(backupTypeMap);
    this.loadData()
  },
  beforeUnmount() {
    this.timer && clearTimeout(this.timer)
  },
  methods: {
    // 格式化文件大小
    renderSizeFormat(value) {
      return renderSize(value)
    },
    parseTime: parseTime,
    // 加载数据
    loadData(pointerEvent) {
      this.loading = true
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page

      getBackupList(this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data.result
          this.listQuery.total = res.data.total
        }
        this.loading = false
      })
    },
    // 加载数据库表名列表
    loadTableNameList() {
      this.tableNameList = []
      getTableNameList().then((res) => {
        if (res.code === 200) {
          res.data.forEach((element) => {
            this.tableNameList.push({
              key: element.tableName,
              title: element.tableDesc
            })
          })
        }
      })
    },

    // 穿梭框筛选
    filterOption(inputValue, option) {
      return option.title.indexOf(inputValue) > -1
    },
    // 穿梭框 change
    handleChange(targetKeys) {
      this.targetKeys = targetKeys
    },
    // 创建备份
    handleAdd() {
      this.targetKeys = []
      this.temp = {
        backupType: 0
      }
      this.loadTableNameList()
      this.createBackupVisible = true
    },
    // 提交节点数据
    handleCreateBackupOk() {
      // 检验表单
      this.$refs['editBackupForm'].validate().then(() => {
        this.confirmLoading = true
        // 提交数据
        createBackup(this.targetKeys)
          .then((res) => {
            if (res.code === 200) {
              // 成功
              $notification.success({
                message: res.msg
              })
              this.$refs['editBackupForm'].restoreValidation()
              this.createBackupVisible = false
              this.loadData()
            }
          })
          .finally(() => {
            this.confirmLoading = false
          })
      }).catch(() => {})
    },
    // 下载
    handleDownload(record) {
      window.open(downloadBackupFile(record.id), '_blank')
    },
    // 删除
    handleDelete(record) {
      $confirm({
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        content: this.$t('i18n_814dd5fb7d'),
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
        onOk: () => {
          return deleteBackup(record.id).then((res) => {
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
    // 还原备份
    handleRestore(record) {
      const html = `
        ${this.$t('i18n_4d18dcbd15')}
        <ul style='color:red;'>
        <li>${this.$t('i18n_6ac61b0e74')}</li>
        <li>${this.$t('i18n_a9eed33cfb')}</li>
        <li>${this.$t('i18n_5ed197a129')} <b> --rest:load_init_db </b> </li>
      </ul>${this.$t('i18n_d0132b0170')}`

      $confirm({
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        content: h('div', null, [h('p', { innerHTML: html }, null)]),
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
        width: 600,
        onOk: () => {
          return restoreBackup(record.id).then((res) => {
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
    // 上传压缩文件
    handleSqlUpload() {
      this.uploadSqlFileVisible = true
      // clearInterval(this.timer)

      this.uploadFileList = []
    },
    handleSqlRemove() {
      this.handleSqlUpload()
      return true
    },
    beforeSqlUpload({ file, onFinish, onError }) {
      this.uploadFileList = [file]
      return false
    },
    // 开始上传 SQL 文件
    startSqlUpload() {
      if (this.uploadFileList.length != 1) {
        $notification.warning({
          message: this.$t('i18n_1a704f73c2')
        })
        return
      }

      // 上传文件
      const file = this.uploadFileList[0]
      const formData = new FormData()
      formData.append('file', file.file)
      formData.append('backupType', this.backupType)
      // 上传文件
      this.confirmLoading = true
      uploadBackupFile(formData)
        .then((res) => {
          if (res.code === 200) {
            $notification.success({
              message: res.msg
            })

            this.uploadSqlFileVisible = false
            this.loadData()
          }
        })
        .finally(() => {
          this.confirmLoading = false
        })
    },
    // 分页、排序、筛选变化时触发
    changePage(pagination, filters, sorter) {
      this.listQuery = CHANGE_PAGE(this.listQuery, { pagination, sorter })
      this.loadData()
    }, // 触发器
    handleTrigger() {
      this.temp = Object.assign({}, {})

      getTriggerUrl({}).then((res) => {
        if (res.code === 200) {
          this.fillTriggerResult(res)
          this.triggerVisible = true
        }
      })
    },
    // 重置触发器
    resetTrigger() {
      getTriggerUrl({
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
      this.temp.triggerUrl = `${this.temp.triggerUrl}?reserveCount=2`
      this.temp = { ...this.temp }
    }
  }
}
</script>
