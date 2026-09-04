<template>
  <div>
    <!-- 数据表格 -->
    <CustomTable
      is-show-tools
      default-auto-refresh
      :auto-refresh-time="30"
      :active-page="activePage"
      table-name="dispatch-log-list"
      :empty-description="$t('i18n_8d1286cd2e')"
      size="medium"
      :data="list"
      :columns="columns"
      :pagination="pagination"
      bordered
      :scroll="{
        x: 'max-content'
      }"
      @change="changePage"
      @refresh="loadData"
    >
      <template #title>
        <n-space wrap class="search-box">
          <n-select
            v-model:value="listQuery.nodeId"
            clearable
            :placeholder="$t('i18n_f8a613d247')"
            class="search-input-item"
            :options="nodeList.map((node) => ({ label: node.name, value: node.id }))"
          />
          <n-select
            v-model:value="listQuery.outGivingId"
            clearable
            :placeholder="$t('i18n_bc8752e529')"
            class="search-input-item"
            :options="dispatchList.map((dispatch) => ({ label: dispatch.name, value: dispatch.id }))"
          />
          <n-select
            v-model:value="listQuery.status"
            clearable
            :placeholder="$t('i18n_e1c965efff')"
            class="search-input-item"
            :options="Object.entries(dispatchStatusMap).map(([key, item]) => ({ label: item, value: key }))"
          />
          <n-date-picker type="datetimerange" format="yyyy-MM-dd HH:mm:ss" clearable @update:value="onchangeTime" />
          <n-tooltip>
            <template #trigger>
              <span class="tw">
                <n-button :loading="loading" type="primary" @click="loadData">{{ $t('i18n_e5f71fc31e') }}</n-button>
              </span>
            </template>
            $t('i18n_4838a3bd20')
          </n-tooltip>
        </n-space>
      </template>
      <template #tableBodyCell="{ column, text, record }">
        <template v-if="column.dataIndex === 'outGivingId'">
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

        <template v-else-if="column.dataIndex === 'nodeName'">
          <n-tooltip placement="top-start">
            <template #trigger>
              <span class="tw">
                <span class="tw">
                  <span>{{
                    nodeList.filter((item) => item.id === record.nodeId) &&
                    nodeList.filter((item) => item.id === record.nodeId)[0] &&
                    nodeList.filter((item) => item.id === record.nodeId)[0].name
                  }}</span>
                </span>
              </span>
            </template>
            {{
              nodeList.filter((item) => item.id === record.nodeId) &&
              nodeList.filter((item) => item.id === record.nodeId)[0] &&
              nodeList.filter((item) => item.id === record.nodeId)[0].name
            }}
          </n-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'projectId'">
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
        <template v-else-if="column.dataIndex === 'mode'">
          <n-tooltip placement="topLeft">
            <template #trigger>
              <span class="tw">
                <span class="tw">
                  <span>{{ dispatchMode[text] || '' }}</span>
                </span>
              </span>
            </template>
            `${dispatchMode[text] || ''} ${$t('i18n_b04209e785')}${record.modeData || ''}`
          </n-tooltip>
        </template>

        <template v-else-if="column.dataIndex === 'outGivingResultMsg'">
          <n-tooltip placement="topLeft">
            <template #trigger>
              <span class="tw">
                <span class="tw">
                  <span
                    >{{ readJsonStrField(record.result, 'code') }}-{{
                      readJsonStrField(record.result, 'msg') || record.result
                    }}</span
                  >
                </span>
              </span>
            </template>
            readJsonStrField(record.result, 'msg')
          </n-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'outGivingResultTime'">
          <n-tooltip placement="topLeft">
            <template #trigger>
              <span class="tw">
                <span class="tw">
                  <span>{{ readJsonStrField(record.result, 'upload_duration') }}</span>
                </span>
              </span>
            </template>
            readJsonStrField(record.result, 'upload_duration')
          </n-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'outGivingResultSize'">
          <n-tooltip placement="topLeft">
            <template #trigger>
              {{ readJsonStrField(record.result, 'upload_file_size') }}
            </template>
            readJsonStrField(record.result, 'upload_file_size')
          </n-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'outGivingResultMsgData'">
          <n-tooltip placement="top-start">
            <template #trigger>
              <template v-if="record.fileSize">
                {{ Math.floor((record.progressSize / record.fileSize) * 100) }}%
              </template>
              {{ readJsonStrField(record.result, 'data') }}
            </template>
            {{ `${readJsonStrField(record.result, 'data')}` }}
          </n-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'status'">
          <!-- {{ dispatchStatusMap[text] || "未知" }} -->
          <n-tag v-if="text === 2" color="green">{{ dispatchStatusMap[text] || $t('i18n_1622dc9b6b') }}</n-tag>
          <n-tag v-else-if="text === 1 || text === 0 || text === 5" color="orange">{{
            dispatchStatusMap[text] || $t('i18n_1622dc9b6b')
          }}</n-tag>
          <n-tag v-else-if="text === 3 || text === 4 || text === 6" color="red">{{
            dispatchStatusMap[text] || $t('i18n_1622dc9b6b')
          }}</n-tag>
          <n-tag v-else>{{ dispatchStatusMap[text] || $t('i18n_1622dc9b6b') }}</n-tag>
        </template>
        <template v-else-if="column.dataIndex === 'operation'">
          <n-button type="primary" size="small" @click="handleDetail(record)">{{ $t('i18n_f26225bde6') }}</n-button>
        </template>
      </template>
    </CustomTable>
    <!-- 详情区 -->
    <CustomModal
      v-if="detailVisible"
      v-model:open="detailVisible"
      destroy-on-close
      width="600px"
      :title="$t('i18n_3032257aa3')"
      :footer="null"
    >
      <n-list>
        <n-list-item v-for="(item, index) in detailData" :key="index">
          <div style="width: 100%">
            <h4 style="margin: 4px 0">{{ item.title }}</h4>
            <code v-if="item.description">{{ item.description }}</code>
          </div>
        </n-list-item>
      </n-list>
    </CustomModal>
  </div>
</template>
<script>
import { getNodeListAll } from '@/api/node'
import { dispatchStatusMap, getDishPatchListAll, getDishPatchLogList, dispatchMode } from '@/api/dispatch'
import { CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY, readJsonStrField, parseTime } from '@/utils/const'

export default {
  data() {
    return {
      dispatchMode,
      loading: true,
      list: [],
      nodeList: [],
      dispatchList: [],
      total: 0,
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      dispatchStatusMap: dispatchStatusMap,
      temp: {},
      detailVisible: false,
      detailData: [],
      columns: [
        {
          title: this.$t('i18n_b714160f52'),
          key: 'outGivingId',
          width: 100,
          ellipsis: true
        },

        {
          title: this.$t('i18n_b1785ef01e'),
          key: 'nodeName',
          ellipsis: true,
          width: 150
        },
        {
          title: this.$t('i18n_4fdd2213b5'),
          key: 'projectId',
          ellipsis: true,
          width: 100
        },
        {
          title: this.$t('i18n_174062da44'),
          key: 'mode',
          ellipsis: true,
          width: '100px'
        },
        {
          title: this.$t('i18n_0ef396cbcc'),
          key: 'outGivingResultMsg',
          ellipsis: true,
          width: 200
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
          title: this.$t('i18n_592c595891'),
          key: 'startTime',
          render: (row) => {
            return parseTime(row['startTime'])
          },
          sorter: true,
          width: '170px'
        },
        {
          title: this.$t('i18n_f782779e8b'),
          key: 'endTime',
          sorter: true,
          render: (row) => {
            return parseTime(row['endTime'])
          },
          width: '170px'
        },
        {
          title: this.$t('i18n_543de6ff04'),
          key: 'outGivingResultMsgData',
          ellipsis: true,
          width: 100
        },
        {
          title: this.$t('i18n_45a4922d3f'),
          key: 'modeData',
          ellipsis: true,
          width: 100
        },
        {
          title: this.$t('i18n_f9ac4b2aa6'),
          key: 'modifyUser',
          ellipsis: true,

          width: 120
        },
        {
          title: this.$t('i18n_3fea7ca76c'),
          key: 'status',
          width: 100,
          ellipsis: true,
          fixed: 'right'
        },
        {
          title: this.$t('i18n_2b6bc0f293'),
          key: 'operation',
          align: 'center',
          width: '100px',
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
    }
  },
  created() {
    this.handleFilter()
  },
  methods: {
    readJsonStrField,
    // 搜索
    handleFilter() {
      this.loadNodeList()
      this.loadDispatchList()
      this.loadData()
    },
    // 加载 node
    loadNodeList() {
      getNodeListAll().then((res) => {
        if (res.code === 200) {
          this.nodeList = res.data
        }
      })
    },
    // 加载分发项目
    loadDispatchList() {
      getDishPatchListAll().then((res) => {
        if (res.code === 200) {
          this.dispatchList = res.data
        }
      })
    },
    // 加载数据
    loadData(pointerEvent) {
      this.loading = true
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page
      getDishPatchLogList(this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data.result
          this.listQuery.total = res.data.total
        }
        this.loading = false
      })
    },
    // 选择时间
    onchangeTime(value, dateString) {
      this.listQuery.createTimeMillis = `${dateString[0]} ~ ${dateString[1]}`
    },
    // 查看详情
    handleDetail(record) {
      this.detailData = []
      this.detailVisible = true
      this.temp = Object.assign({}, record)

      this.detailData.push({ title: this.$t('i18n_0ef396cbcc'), description: this.temp.result })
    },
    // 分页、排序、筛选变化时触发
    changePage(pagination, filters, sorter) {
      this.listQuery = CHANGE_PAGE(this.listQuery, { pagination, sorter })
      this.loadData()
    }
  }
}
</script>
