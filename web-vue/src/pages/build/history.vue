<template>
  <div class="">
    <!-- 数据表格 -->
    <CustomTable
      is-show-tools
      default-auto-refresh
      :auto-refresh-time="30"
      :active-page="activePage"
      table-name="build-history-list"
      :empty-description="$t('i18n_2b36926bc1')"
      :data="list"
      size="medium"
      :columns="columns"
      :pagination="pagination"
      bordered
      row-key="id"
      :row-selection="rowSelection"
      :scroll="{
        x: 'max-content'
      }"
      @change="change"
      @refresh="loadData"
    >
      <template #title>
        <n-space wrap class="search-box">
          <n-input
            v-model:value="listQuery['%buildName%']"
            clearable
            class="search-input-item"
            :placeholder="$t('i18n_50a299c847')"
            @press-enter="loadData"
          />
          <n-select
            v-model:value="listQuery.status"
            filterable
            clearable
            :placeholder="$t('i18n_e1c965efff')"
            class="search-input-item"
            :options="Object.entries(statusMap).map(([key, val]) => ({ label: val, value: key }))"
          />
          <n-select
            v-model:value="listQuery.triggerBuildType"
            filterable
            clearable
            :placeholder="$t('i18n_9057ac9664')"
            class="search-input-item"
            :options="Object.entries(triggerBuildTypeMap).map(([key, val]) => ({ label: val, value: key }))"
          />
          <n-date-picker type="datetimerange" format="yyyy-MM-dd HH:mm:ss" clearable @update:value="onchangeTime" />
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
            danger
            :disabled="!tableSelections || tableSelections.length <= 0"
            @click="handleBatchDelete"
          >
            {{ $t('i18n_7fb62b3011') }}
          </n-button>
        </n-space>
      </template>
      <template #tableHelp>
        <n-tooltip>
          <template #trigger>
            <QuestionCircleOutlined />
          </template>

          <div>{{ $t('i18n_005de9a4eb') }}</div>
          <div>{{ $t('i18n_9cd0554305') }}</div>
          <div>{{ $t('i18n_952232ca52') }}</div>
        </n-tooltip>
      </template>
      <template #tableBodyCell="{ column, text, record }">
        <template v-if="column.tooltip">
          <n-tooltip>
            <template #trigger>
              <span class="tw">
                <span class="tw">
                  <span>{{ text || '' }}</span>
                </span>
              </span>
            </template>
            text
          </n-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'buildNumberId'">
          <n-tooltip>
            <template #trigger>
              <span class="tw">
                <n-tag color="#108ee9" @click="handleBuildLog(record)">
                  #{{ text }}<template v-if="record.fromBuildNumberId">&lt;-{{ record.fromBuildNumberId }}</template>
                </n-tag>
              </span>
            </template>
            {{ text + `( ${$t('i18n_aac62bc255')} )` }}
          </n-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'status'">
          <n-tooltip>
            <template #trigger>
              <span class="tw">
                <n-tag :color="statusColor[record.status]">{{ statusMap[text] || $t('i18n_1622dc9b6b') }}</n-tag>
              </span>
            </template>
            record.statusMsg || statusMap[text] || $t('i18n_1622dc9b6b')
          </n-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'releaseMethod'">
          <n-tooltip>
            <template #trigger>
              <span class="tw">
                <span class="tw">
                  <span>{{ releaseMethodMap[text] }}</span>
                </span>
              </span>
            </template>
            releaseMethodMap[text]
          </n-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'triggerBuildType'">
          <n-tooltip>
            <template #trigger>
              <span class="tw">
                <span class="tw">
                  <span>{{ triggerBuildTypeMap[text] }}</span>
                </span>
              </span>
            </template>
            triggerBuildTypeMap[text]
          </n-tooltip>
        </template>

        <template v-else-if="column.dataIndex === 'resultFileSize'">
          <n-tooltip>
            <template #trigger>
              <span class="tw">
                <span class="tw">
                  <span v-if="record.resultFileSize">{{ renderSize(record.resultFileSize) }}</span>
                  <span v-else-if="record.buildLogFileSize">{{ renderSize(record.buildLogFileSize) }}</span>
                  <span v-else>-</span>
                </span>
              </span>
            </template>
            `${$t('i18n_16646e46b1')}${renderSize(record.resultFileSize)}， ${$t( 'i18n_77e501b44b' )}
            ${renderSize(record.buildLogFileSize)}`
          </n-tooltip>
        </template>

        <template v-else-if="column.dataIndex === 'endTime'">
          <n-tooltip>
            <template #trigger>
              <span class="tw">
                <span class="tw">
                  <span v-if="record.endTime">{{
                    formatDuration((record.endTime || 0) - (record.startTime || 0), '', 2)
                  }}</span>
                  <span v-else>-</span>
                </span>
              </span>
            </template>
            `${$t('i18n_61e84eb5bb')}${parseTime(record.startTime)}，${ record.endTime ? $t('i18n_590dbb68cf') +
            parseTime(record.endTime) : '' }`
          </n-tooltip>
        </template>

        <template v-else-if="column.dataIndex === 'operation'">
          <n-space>
            <n-tooltip>
              <template #trigger>
                <span class="tw">
                  <n-button size="small" type="primary" :disabled="!record.hasLog" @click="handleDownload(record)"
                    ><DownloadOutlined />{{ $t('i18n_456d29ef8b') }}</n-button
                  >
                </span>
              </template>
              $t('i18n_b38d7db9b0')
            </n-tooltip>

            <n-tooltip>
              <template #trigger>
                <span class="tw">
                  <n-button size="small" type="primary" :disabled="!record.hasFile" @click="handleFile(record)"
                    ><DownloadOutlined />
                    {{ $t('i18n_7dfcab648d') }}
                  </n-button>
                </span>
              </template>
              $t('i18n_02e35447d4')
            </n-tooltip>

            <n-dropdown
              :options="[
                {
                  label: $t('i18n_d00b485b26'),
                  key: '0',
                  disabled: record.releaseMethod === 5 || !record.hasFile || record.releaseMethod === 0,
                  props: { onClick: () => handleRollback(record) }
                },
                { label: $t('i18n_2f4aaddde3'), key: '1', props: { onClick: () => handleDelete(record) } }
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
    <!-- 构建日志 -->
    <build-log
      v-if="buildLogVisible > 0"
      :temp="temp"
      :show="buildLogVisible != 0"
      @close="
        () => {
          buildLogVisible = 0
        }
      "
    />
    <!-- 选择确认区域
    <div style="padding-top: 50px" v-if="this.choose">
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
                this.$emit('cancel')
              }
            "
          >
            取消
          </n-button>
          <n-button type="primary" @click="handerConfirm"> 确定 </n-button>
        </n-space>
      </div>
    </div> -->
  </div>
</template>
<script>
import { DownOutlined, DownloadOutlined, QuestionCircleOutlined } from '@ant-design/icons-vue'

import BuildLog from './log'
import {
  deleteBuildHistory,
  downloadBuildFile,
  downloadBuildLog,
  geteBuildHistory,
  releaseMethodMap,
  rollback,
  statusMap,
  statusColor,
  triggerBuildTypeMap
} from '@/api/build-info'
import {
  CHANGE_PAGE,
  COMPUTED_PAGINATION,
  PAGE_DEFAULT_LIST_QUERY,
  formatDuration,
  parseTime,
  renderSize
} from '@/utils/const'

export default {
  components: {
    BuildLog
  },
  props: {
    choose: {
      type: String,
      default: ''
    },
    buildId: {
      type: String,
      default: ''
    }
  },
  emits: ['cancel', 'confirm'],
  data() {
    return {
      releaseMethodMap,
      triggerBuildTypeMap,
      loading: false,
      list: [],

      total: 0,
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      statusMap,
      statusColor,
      temp: {},
      buildLogVisible: 0,
      tableSelections: [],
      columns: [
        {
          title: this.$t('i18n_50a299c847'),
          key: 'buildName',
          width: 120,
          ellipsis: true
        },
        {
          title: this.$t('i18n_46e4265791'),
          key: 'buildNumberId',
          width: '90px',
          align: 'center',
          ellipsis: true
        },
        {
          title: this.$t('i18n_2432b57515'),
          key: 'buildRemark',
          width: 120,
          ellipsis: true
        },

        {
          title: this.$t('i18n_3fea7ca76c'),
          key: 'status',
          width: '100px',
          align: 'center',
          ellipsis: true
        },
        {
          title: this.$t('i18n_ff9814bf6b'),
          key: 'triggerBuildType',
          align: 'center',
          width: '100px',
          ellipsis: true
        },
        {
          title: this.$t('i18n_ad35f58fb3'),
          key: 'resultFileSize',
          width: '100px',
          sorter: true,
          ellipsis: true
        },
        {
          title: this.$t('i18n_592c595891'),
          key: 'startTime',
          sorter: true,
          render: (row) => parseTime(row['startTime']),
          width: '170px'
        },
        {
          title: this.$t('i18n_39f1374d36'),
          key: 'endTime',
          // sorter: true,

          width: '120px'
        },
        {
          title: this.$t('i18n_af427d2541'),
          key: 'modifyTimeMillis',
          sorter: true,
          render: (row) => parseTime(row['modifyTimeMillis']),
          width: '170px'
        },
        {
          title: this.$t('i18n_f98994f7ec'),
          key: 'releaseMethod',
          width: '100px',
          ellipsis: true
        },
        {
          title: this.$t('i18n_f9ac4b2aa6'),
          key: 'modifyUser',
          width: '130px',
          ellipsis: true
        },
        {
          title: this.$t('i18n_2b6bc0f293'),
          key: 'operation',

          width: '220px',
          align: 'center',
          fixed: 'right'
        }
      ]
    }
  },
  computed: {
    pagination() {
      return COMPUTED_PAGINATION(this.listQuery)
    },
    activePage() {
      return this.$attrs.routerUrl === this.$route.path
    },
    rowSelection() {
      return {
        onChange: this.tableSelectionChange,
        selectedRowKeys: this.tableSelections,
        type: this.choose || 'checkbox'
      }
    }
  },
  created() {
    // this.loadBuildList();
    this.loadData()
  },
  methods: {
    parseTime,
    renderSize,
    formatDuration,
    // 加载数据
    loadData(pointerEvent) {
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page
      this.buildId && (this.listQuery.buildDataId = this.buildId)
      this.loading = true

      geteBuildHistory(this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data.result
          this.listQuery.total = res.data.total
        }
        this.loading = false
      })
    },
    // 分页、排序、筛选变化时触发
    change(pagination, filters, sorter) {
      this.listQuery = CHANGE_PAGE(this.listQuery, { pagination, sorter })
      this.loadData()
    },
    // 选择时间
    onchangeTime(value, dateString) {
      if (!dateString[0] || !dateString[1]) {
        this.listQuery.startTime = ''
      } else {
        this.listQuery.startTime = `${dateString[0]} ~ ${dateString[1]}`
      }
    },

    // 下载构建日志
    handleDownload(record) {
      window.open(downloadBuildLog(record.id), '_blank')
    },

    // 下载构建产物
    handleFile(record) {
      window.open(downloadBuildFile(record.id), '_blank')
    },

    // 回滚
    handleRollback(record) {
      $confirm({
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        content: this.$t('i18n_fb61d4d708'),
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
        onOk: () => {
          // 重新发布
          return rollback(record.id).then((res) => {
            if (res.code === 200) {
              $notification.success({
                message: res.msg
              })
              this.loadData()
              // 弹窗
              this.temp = {
                id: record.buildDataId,
                buildId: res.data
              }
              this.buildLogVisible = new Date() * Math.random()
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
        content: this.$t('i18n_ad8b626496'),
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
        onOk: () => {
          return deleteBuildHistory(record.id).then((res) => {
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
        content: this.$t('i18n_02d46f7e6f'),
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
        onOk: () => {
          // 删除
          return deleteBuildHistory(this.tableSelections.join(',')).then((res) => {
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
    // 查看构建日志
    handleBuildLog(record) {
      this.temp = {
        id: record.buildDataId,
        buildId: record.buildNumberId
      }
      this.buildLogVisible = new Date() * Math.random()
    },
    // 关闭日志对话框
    closeBuildLogModel() {
      this.loadData()
    },
    // 多选相关
    tableSelectionChange(selectedRowKeys) {
      this.tableSelections = selectedRowKeys
    },
    // 选择确认
    handerConfirm() {
      if (!this.tableSelections.length) {
        $notification.warning({
          message: this.$t('i18n_2b4cf3d74e')
        })
        return
      }
      const selectData = this.list
        .filter((item) => {
          return this.tableSelections.indexOf(item.id) > -1
        })
        .filter((item) => {
          return item.hasFile
        })
        .map((item) => {
          return item.buildNumberId
        })
      if (!selectData.length) {
        $notification.warning({
          message: this.$t('i18n_a637a42173')
        })
        return
      }
      this.$emit('confirm', selectData)
    }
  }
}
</script>
