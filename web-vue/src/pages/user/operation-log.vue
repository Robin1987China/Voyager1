<template>
  <div>
    <!-- 数据表格 -->
    <CustomTable
      is-show-tools
      default-auto-refresh
      :auto-refresh-time="30"
      :active-page="activePage"
      table-name="systemUserOperationLog"
      :empty-description="$t('i18n_c31ea1e3c4')"
      :data="list"
      :columns="columns"
      :pagination="pagination"
      bordered
      row-key="id"
      :scroll="{
        x: 'max-content'
      }"
      @change="change"
      @refresh="loadData"
    >
      <template #title>
        <n-space wrap class="search-box">
          <n-select
            v-model:value="listQuery.userId"
            filterable
            clearable
            :placeholder="$t('i18n_512e1a7722')"
            class="search-input-item"
            :options="userList.map((item) => ({ label: item.name, value: item.id }))"
          />
          <n-select
            v-model:value="listQuery.nodeId"
            filterable
            clearable
            :placeholder="$t('i18n_f8a613d247')"
            class="search-input-item"
            :options="nodeList.map((node) => ({ label: node.name, value: node.id }))"
          />
          <n-select
            v-model:value="listQuery.classFeature"
            filterable
            clearable
            :placeholder="$t('i18n_8432a98819')"
            class="search-input-item"
            :options="classFeature.map((item) => ({ label: item.title, value: item.value }))"
          />
          <n-select
            v-model:value="listQuery.methodFeature"
            filterable
            clearable
            :placeholder="$t('i18n_a9de52acb0')"
            class="search-input-item"
            :options="methodFeature.map((item) => ({ label: item.title, value: item.value }))"
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
            nodeMap[text]
          </n-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'classFeature'">
          <n-tooltip placement="topLeft">
            <template #trigger>
              <span class="tw">
                <span class="tw">
                  <span>{{ classFeatureMap[text] }}</span>
                </span>
              </span>
            </template>
            classFeatureMap[text]
          </n-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'methodFeature'">
          <n-tooltip placement="topLeft">
            <template #trigger>
              <span class="tw">
                <span class="tw">
                  <span>{{ methodFeatureMap[text] }}</span>
                </span>
              </span>
            </template>
            methodFeatureMap[text]
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

        <template v-else-if="column.dataIndex === 'username'">
          <n-tooltip placement="topLeft">
            <template #trigger>
              <span class="tw">
                <span class="tw">
                  <span>{{ text || item.userId }}</span>
                </span>
              </span>
            </template>
            text
          </n-tooltip>
        </template>

        <template v-else-if="column.dataIndex === 'optStatus'">
          <n-tooltip placement="topLeft">
            <template #trigger>
              <span class="tw">
                <span class="tw">
                  <span>{{ text }}</span>
                </span>
              </span>
            </template>
            `${$t('i18n_be4b9241ec')},${$t('i18n_69056f4792')},${$t('i18n_27b36afd36')}`
          </n-tooltip>
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
      detail-visible
      destroy-on-close
      width="600px"
      :title="$t('i18n_3032257aa3')"
      :footer="null"
    >
      <n-list item-layout="horizontal" :data="detailData">
        <template #renderItem="{ item }">
          <n-list-item>
            <n-list-item>
              <template #title>
                <h4>{{ item.title }}</h4>
              </template>
              <template #description>
                <div v-if="item.description">{{ item.description }}</div>
                <pre v-if="item.json" style="overflow: scroll">{{ item.value }}</pre>
              </template>
            </n-list-item>
          </n-list-item>
        </template>
      </n-list>
    </CustomModal>
  </div>
</template>
<script>
import { getOperationLogList } from '@/api/operation-log'
import { getMonitorOperateTypeList } from '@/api/monitor'
import { getNodeListAll } from '@/api/node'
import { getUserListAll } from '@/api/user/user'
import { CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY, parseTime } from '@/utils/const'

export default {
  components: {},
  data() {
    return {
      loading: false,
      list: [],
      nodeList: [],
      nodeMap: {},
      userList: [],
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      methodFeature: [],
      classFeature: [],
      methodFeatureMap: {},
      classFeatureMap: {},
      temp: {},
      detailVisible: false,
      detailData: [],
      columns: [
        {
          title: this.$t('i18n_30acd20d6e'),
          key: 'userId',
          ellipsis: true
        },
        {
          title: this.$t('i18n_9a56bb830e'),
          key: 'username',
          ellipsis: true
        },
        { title: 'IP', key: 'ip' /*width: 130*/ },
        {
          title: this.$t('i18n_3bf3c0a8d6'),
          key: 'nodeId',
          width: 120,
          ellipsis: true
        },
        {
          title: this.$t('i18n_5a1419b7a2'),
          key: 'dataName',
          /*width: 240,*/
          ellipsis: true
        },
        {
          title: this.$t('i18n_4524ed750d'),
          key: 'workspaceName',
          /*width: 240,*/
          ellipsis: true
        },
        // { title: "数据 ID", key: "dataId", /*width: 240,*/ ellipsis: true, },
        {
          title: this.$t('i18n_8432a98819'),
          key: 'classFeature',
          /*width: 240,*/
          ellipsis: true
        },
        {
          title: this.$t('i18n_a9de52acb0'),
          key: 'methodFeature',
          /*width: 240,*/
          ellipsis: true
        },
        {
          title: this.$t('i18n_771d897d9a'),
          key: 'optStatus',
          width: 90
        },
        {
          title: this.$t('i18n_7e951d56d9'),
          key: 'createTimeMillis',
          sorter: true,
          render: (row) => {
            return parseTime(row['createTimeMillis'] || row['optTime'])
          },
          width: '170px'
        },
        {
          title: this.$t('i18n_2b6bc0f293'),
          align: 'center',
          key: 'operation',
          fixed: 'right',
          width: '80px'
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
    this.loadData()
    this.loadUserList()
    this.loadNodeList()

    getMonitorOperateTypeList().then((res) => {
      this.methodFeature = res.data.methodFeature
      this.classFeature = res.data.classFeature
      res.data.methodFeature.forEach((item) => {
        this.methodFeatureMap[item.value] = item.title
      })
      res.data.classFeature.forEach((item) => {
        this.classFeatureMap[item.value] = item.title
      })
    })
  },
  methods: {
    // 加载 node
    loadNodeList() {
      getNodeListAll().then((res) => {
        if (res.code === 200) {
          this.nodeList = res.data

          res.data.forEach((item) => {
            this.nodeMap[item.id] = item.name
          })
        }
      })
    },
    // 加载数据
    loadData(pointerEvent) {
      this.loading = true
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page
      getOperationLogList(this.listQuery).then((res) => {
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
    // 加载用户列表
    loadUserList() {
      getUserListAll().then((res) => {
        if (res.code === 200) {
          this.userList = res.data
        }
      })
    },
    // 选择时间
    onchangeTime(value, dateString) {
      this.listQuery.createTimeMillis = `${dateString[0]} ~ ${dateString[1]}`
    },
    // 查看详情
    handleDetail(record) {
      this.detailData = []

      this.temp = Object.assign({}, record)
      try {
        this.temp.reqData = JSON.parse(this.temp.reqData)
      } catch (e) {
        console.error(e)
      }
      try {
        this.temp.resultMsg = JSON.parse(this.temp.resultMsg)
      } catch (e) {
        console.error(e)
      }
      this.detailData.push({ title: this.$t('i18n_37189681ad'), description: this.temp.dataId })
      this.detailData.push({
        title: this.$t('i18n_d72471c540'),
        description: this.temp.userAgent
      })
      this.detailData.push({
        title: this.$t('i18n_527466ff94'),
        json: true,
        value: this.temp.reqData
      })
      this.detailData.push({
        title: this.$t('i18n_15d5fffa6a'),
        json: true,
        value: this.temp.resultMsg
      })
      this.detailVisible = true
    }
  }
}
</script>
