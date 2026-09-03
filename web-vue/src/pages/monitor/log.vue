<template>
  <div>
    <!-- 数据表格 -->
    <CustomTable
      is-show-tools
      default-auto-refresh
      :auto-refresh-time="30"
      table-name="monitor-log-list"
      :active-page="activePage"
      :data="list"
      size="medium"
      :columns="columns"
      :pagination="pagination"
      bordered
      :scroll="{
        x: 'max-content'
      }"
      @change="change"
      @refresh="loadData"
    >
      <template #title>
        <n-space wrap class="search-box">
          <n-select
            v-model:value="listQuery.nodeId"
            clearable
            :placeholder="$t('i18n_f8a613d247')"
            class="search-input-item"
            :options="Object.entries(nodeMap).map(([key, nodeName]) => ({ label: nodeName, value: key }))"
          />
          <n-select
            v-model:value="listQuery.status"
            clearable
            :placeholder="$t('i18n_db4470d98d')"
            class="search-input-item"
            :options="[
              { label: $t('i18n_fd6e80f1e0'), value: 1 },
              { label: $t('i18n_c195df6308'), value: 0 }
            ]"
          />
          <n-select
            v-model:value="listQuery.notifyStatus"
            clearable
            :placeholder="$t('i18n_8023baf064')"
            class="search-input-item"
            :options="[
              { label: $t('i18n_330363dfc5'), value: 1 },
              { label: $t('i18n_acd5cb847a'), value: 0 }
            ]"
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
        <template v-if="column.dataIndex === 'nodeId'">
          <n-tooltip placement="topLeft">
            <template #trigger>
              <span class="tw">
                <span class="tw">
                  <span>{{ nodeMap[text] }}</span>
                </span>
              </span>
            </template>
            text
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
        <template v-else-if="column.dataIndex === 'status'">
          <span>{{ text ? $t('i18n_fd6e80f1e0') : $t('i18n_c195df6308') }}</span>
        </template>
        <template v-else-if="column.dataIndex === 'notifyStyle'">
          {{ notifyStyle[text] || $t('i18n_1622dc9b6b') }}
        </template>
        <template v-else-if="column.dataIndex === 'notifyStatus'">
          <span>{{ text ? $t('i18n_330363dfc5') : $t('i18n_acd5cb847a') }}</span>
        </template>
        <template v-else-if="column.dataIndex === 'operation'">
          <n-button size="small" type="primary" @click="handleDetail(record)">{{ $t('i18n_f26225bde6') }}</n-button>
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
      <n-list item-layout="horizontal" :data="detailData">
        <template #renderItem="{ item }">
          <n-list-item>
            <n-list-item :description="item.description">
              <template #title>
                <h4>{{ item.title }}</h4>
              </template>
            </n-list-item>
          </n-list-item>
        </template>
      </n-list>
    </CustomModal>
  </div>
</template>
<script>
import { getMonitorLogList, notifyStyle } from '@/api/monitor'
import { getNodeListAll } from '@/api/node'
import { CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY, parseTime } from '@/utils/const'

export default {
  data() {
    return {
      loading: false,
      list: [],
      nodeMap: {},
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      temp: {},
      detailVisible: false,
      notifyStyle,
      detailData: [],
      columns: [
        {
          title: this.$t('i18n_36b3f3a2f6'),
          key: 'title',
          ellipsis: true
        },
        {
          title: this.$t('i18n_b1785ef01e'),
          key: 'nodeId',
          width: 100,
          ellipsis: true
        },
        {
          title: this.$t('i18n_4fdd2213b5'),
          key: 'projectId',
          width: 100,
          ellipsis: true
        },
        {
          title: this.$t('i18n_db4470d98d'),
          key: 'status',
          width: 100,
          align: 'center',
          ellipsis: true
        },
        {
          title: this.$t('i18n_52eedb4a12'),
          key: 'notifyStyle',
          width: 100,
          align: 'center',
          ellipsis: true
        },
        {
          title: this.$t('i18n_4741e596ac'),
          key: 'createTime',
          render: (row) => {
            return parseTime(row['createTime'])
          },
          width: '170px'
        },
        {
          title: this.$t('i18n_8023baf064'),
          key: 'notifyStatus',
          width: 100,
          ellipsis: true
        },
        {
          title: this.$t('i18n_2b6bc0f293'),
          key: 'operation',
          align: 'center',
          fixed: 'right',
          width: '80px'
        }
      ]
    }
  },
  computed: {
    // 分页
    pagination() {
      return COMPUTED_PAGINATION(this.listQuery)
    },
    activePage() {
      return this.$attrs.routerUrl === this.$route.path
    }
  },
  created() {
    this.loadNodeList(() => {
      this.loadData()
    })
  },
  methods: {
    // 加载 node
    loadNodeList(fn) {
      getNodeListAll().then((res) => {
        if (res.code === 200) {
          res.data.forEach((element) => {
            this.nodeMap[element.id] = element.name
          })
          fn && fn()
        }
      })
    },
    // 加载数据
    loadData(pointerEvent) {
      this.loading = true
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page
      getMonitorLogList(this.listQuery).then((res) => {
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
      if (dateString[0]) {
        this.listQuery.createTimeMillis = `${dateString[0]} ~ ${dateString[1]}`
      } else {
        this.listQuery.createTimeMillis = ''
      }
    },

    // 查看详情
    handleDetail(record) {
      this.detailData = []
      this.detailVisible = true
      this.temp = Object.assign({}, record)
      this.detailData.push({ title: this.$t('i18n_32c65d8d74'), description: this.temp.title })
      this.detailData.push({ title: this.$t('i18n_2d711b09bd'), description: this.temp.content })
      this.detailData.push({
        title: this.$t('i18n_59c75681b4'),
        description: this.temp.notifyObject
      })
      if (!this.temp.notifyStatus) {
        this.detailData.push({
          title: this.$t('i18n_fcb4c2610a'),
          description: this.temp.notifyError
        })
      }
    }
  }
}
</script>
