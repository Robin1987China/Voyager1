<template>
  <div>
    <n-data-table
      size="medium"
      :data="commandList"
      :columns="columns"
      bordered
      :row-key="(row) => row.id"
      :pagination="pagination"
      :scroll="{
        x: 'max-content'
      }"
      @change="changePage"
    >
      <template #title>
        <n-space wrap class="search-box">
          <n-input
            v-model:value="listQuery['%commandName%']"
            :placeholder="$t('i18n_1f08329bc4')"
            class="search-input-item"
            @press-enter="getCommandLogData"
          />
          <n-input
            v-model:value="listQuery['%sshName%']"
            :placeholder="$t('i18n_d584e1493b')"
            class="search-input-item"
            @press-enter="getCommandLogData"
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
            v-model:value="listQuery.triggerExecType"
            filterable
            clearable
            :placeholder="$t('i18n_ff9814bf6b')"
            class="search-input-item"
            :options="Object.entries(triggerExecTypeMap).map(([key, val]) => ({ label: val, value: key }))"
          />
          <n-tooltip>
            <template #trigger>
              <span class="tw">
                <n-button type="primary" :loading="loading" @click="getCommandLogData">{{
                  $t('i18n_e5f71fc31e')
                }}</n-button>
              </span>
            </template>
            $t('i18n_4838a3bd20')
          </n-tooltip>
        </n-space>
      </template>
      <template #bodyCell="{ column, text, record }">
        <template v-if="column.dataIndex === 'sshName'">
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
        <template v-else-if="column.dataIndex === 'commandName'">
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
        <template v-else-if="column.dataIndex === 'status'">
          <span>{{ statusMap[text] || $t('i18n_1622dc9b6b') }}</span>
        </template>
        <template v-else-if="column.dataIndex === 'triggerExecType'">
          <span>{{ triggerExecTypeMap[text] || $t('i18n_1622dc9b6b') }}</span>
        </template>
        <template v-else-if="column.dataIndex === 'exitCode'">
          <n-tag v-if="text == 0" color="green">{{ $t('i18n_330363dfc5') }}</n-tag>
          <n-tag v-else color="orange">{{ text || '-' }}</n-tag>
        </template>

        <template v-else-if="column.dataIndex === 'operation'">
          <n-space>
            <n-button type="primary" size="small" :disabled="!record.hasLog" @click="handleView(record)">{{
              $t('i18n_607e7a4f37')
            }}</n-button>
            <n-button type="primary" size="small" :disabled="!record.hasLog" @click="handleDownload(record)"
              ><DownloadOutlined />{{ $t('i18n_456d29ef8b') }}</n-button
            >
            <n-button type="primary" danger size="small" @click="handleDelete(record)">{{
              $t('i18n_2f4aaddde3')
            }}</n-button>
          </n-space>
        </template>
      </template>
    </n-data-table>
    <!-- 构建日志 -->
    <CustomModal
      v-if="logVisible"
      v-model:open="logVisible"
      destroy-on-close
      :width="style.width"
      :body-style="style.bodyStyle"
      :style="style.style"
      :title="$t('i18n_c84ddfe8a6')"
      :footer="null"
      :mask-closable="false"
    >
      <command-log v-if="logVisible" :height="style.bodyStyle.height" :temp="temp" />
    </CustomModal>
  </div>
</template>
<script>
import { DownloadOutlined } from '@ant-design/icons-vue'

import { deleteCommandLog, downloadLog, getCommandLogList, statusMap, triggerExecTypeMap } from '@/api/command'
import { CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY, parseTime } from '@/utils/const'
import CommandLog from './command-view-log'
import { mapState } from 'pinia'
import { useGuideStore } from '@/stores/guide'
export default {
  components: {
    CommandLog
  },
  data() {
    return {
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      commandList: [],
      loading: false,
      temp: {},
      statusMap: statusMap,
      triggerExecTypeMap: triggerExecTypeMap,
      logVisible: false,
      columns: [
        {
          title: `ssh ${this.$t('i18n_d7ec2d3fea')}`,
          key: 'sshName',
          ellipsis: true
        },
        {
          title: this.$t('i18n_6496a5a043'),
          key: 'commandName',
          ellipsis: true
        },
        {
          title: this.$t('i18n_3fea7ca76c'),
          key: 'status',
          width: 100,
          ellipsis: true
        },
        {
          title: this.$t('i18n_3fb63afb4e'),
          key: 'exitCode',
          width: 100,
          ellipsis: true
        },
        {
          title: this.$t('i18n_ff9814bf6b'),
          key: 'triggerExecType',
          width: 100,
          ellipsis: true
        },
        {
          title: this.$t('i18n_70b3635aa3'),
          key: 'createTimeMillis',
          sorter: true,
          ellipsis: true,
          render: (row) => {
            return parseTime(row['createTimeMillis'])
          },
          width: '170px'
        },
        {
          title: this.$t('i18n_f782779e8b'),
          key: 'modifyTimeMillis',
          sorter: true,
          ellipsis: true,
          render: (row) => {
            return parseTime(row['modifyTimeMillis'])
          },
          width: '170px'
        },
        {
          title: this.$t('i18n_a497562c8e'),
          key: 'modifyUser',
          width: 120,
          ellipsis: true
        },
        {
          title: this.$t('i18n_2b6bc0f293'),
          key: 'operation',
          align: 'center',
          fixed: 'right',
          width: '200px'
        }
      ]
    }
  },
  computed: {
    ...mapState(useGuideStore, ['getFullscreenViewLogStyle']),
    pagination() {
      return COMPUTED_PAGINATION(this.listQuery)
    },
    style() {
      return this.getFullscreenViewLogStyle()
    }
  },
  created() {},
  mounted() {
    this.getCommandLogData()
  },
  methods: {
    handleView(row) {
      this.temp = row
      this.logVisible = true
    },

    // 获取命令数据
    getCommandLogData(pointerEvent) {
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page
      this.loading = true
      getCommandLogList(this.listQuery).then((res) => {
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
      this.getCommandLogData()
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
          return deleteCommandLog(row.id).then((res) => {
            if (res.code === 200) {
              $notification.success({
                message: res.msg
              })
              this.getCommandLogData()
            }
          })
        }
      })
    },
    // 下载构建日志
    handleDownload(record) {
      window.open(downloadLog(record.id), '_blank')
    }
  }
}
</script>
