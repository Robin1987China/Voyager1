<template>
  <div>
    <!-- 数据表格 -->
        <n-card size="small" :body-style="{ padding: '12px' }" style="margin-bottom: 12px">

        <n-space wrap class="search-box">
          <n-input
            v-model:value="listQuery['%name%']"
            :placeholder="$t('i18n_d7ec2d3fea')"
            clearable
            class="search-input-item"
          />
          <n-select
            v-model:value="listQuery.triggerExecType"
            filterable
            clearable
            :placeholder="$t('i18n_ff9814bf6b')"
            class="search-input-item"
            :options="Object.entries(triggerExecTypeMap).map(([key, val]) => ({ label: val, value: key }))"
          />
          <n-date-picker
            type="datetimerange"
            clearable
            input-readonly
            format="yyyy-MM-dd HH:mm:ss"
            value-format="yyyy-MM-dd HH:mm:ss"
            @update:value="
              (value, dateString) => {
                if (!dateString[0] || !dateString[1]) {
                  listQuery.createTimeMillis = ''
                } else {
                  listQuery.createTimeMillis = `${dateString[0]} ~ ${dateString[1]}`
                }
              }
            "
          />
          <n-tooltip>
            <template #trigger>
              <span class="tw">
                <n-button type="primary" :loading="loading" @click="loadData">{{ $t('i18n_e5f71fc31e') }}</n-button>
              </span>
            </template>
            $t('i18n_4838a3bd20')
          </n-tooltip>
          <n-tooltip>
            <template #trigger>
              <QuestionCircleOutlined />
            </template>

            <div>{{ $t('i18n_52b6b488e2') }}</div>
            <div>
              <ul>
                <li>{{ $t('i18n_47bb635a5c') }}</li>
              </ul>
            </div>
          </n-tooltip>
        </n-space>
      
    </n-card>
<n-data-table
      :data="list"
      size="medium"
      :columns="columns"
      :pagination="pagination"
      bordered
      :row-key="(row) => row.id"
      @change="changePage"
    >
      

      <template #bodyCell="{ column, text, record }">
        <template v-if="column.key === 'scriptName'">
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
        <template v-else-if="column.key === 'modifyUser'">
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
        <template v-else-if="column.key === 'triggerExecType'">
          <span>{{ triggerExecTypeMap[text] || $t('i18n_1622dc9b6b') }}</span>
        </template>
        <template v-else-if="column.key === 'workspaceId'">
          <n-tag v-if="text === 'GLOBAL'">{{ $t('i18n_2be75b1044') }}</n-tag>
          <n-tag v-else>{{ $t('i18n_98d69f8b62') }}</n-tag>
        </template>
        <template v-else-if="column.key === 'createTimeMillis'">
          <n-tooltip>
            <template #trigger>
              <span class="tw">
                <span class="tw">
                  <span>{{ parseTime(record.createTimeMillis) }}</span>
                </span>
              </span>
            </template>
            `${parseTime(record.createTimeMillis)}`
          </n-tooltip>
        </template>
        <template v-else-if="column.key === 'operation'">
          <n-space>
            <n-button size="small" type="primary" @click="viewLog(record)">{{ $t('i18n_0ea78e4279') }}</n-button>

            <n-button size="small" type="primary" danger @click="handleDelete(record)">{{
              $t('i18n_2f4aaddde3')
            }}</n-button>
          </n-space>
        </template>
      </template>
    </n-data-table>
    <!-- 日志 -->

    <script-log-view
      v-if="logVisible > 0"
      :show="logVisible != 0"
      :temp="temp"
      @close="
        () => {
          logVisible = 0
        }
      "
    />
  </div>
</template>
<script>
import { QuestionCircleOutlined } from '@ant-design/icons-vue'

import { getScriptLogList, scriptDel, triggerExecTypeMap } from '@/api/node-other'
// import {triggerExecTypeMap} from "@/api/node-script";
import ScriptLogView from '@/pages/node/node-layout/other/script-log-view'
import { CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY, parseTime } from '@/utils/const'

export default {
  components: {
    ScriptLogView
  },
  props: {
    nodeId: {
      type: String,
      default: ''
    },
    scriptId: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      loading: false,
      listQuery: Object.assign(
        {
          scriptId: this.scriptId
        },
        PAGE_DEFAULT_LIST_QUERY
      ),
      triggerExecTypeMap: triggerExecTypeMap,
      list: [],
      temp: {},
      logVisible: 0,
      columns: [
        {
          title: this.$t('i18n_d7ec2d3fea'),
          key: 'scriptName',
          ellipsis: true,
          width: 100
        },
        {
          title: this.$t('i18n_70b3635aa3'),
          key: 'createTimeMillis',
          ellipsis: true,
          width: '160px'
        },
        {
          title: this.$t('i18n_ff9814bf6b'),
          key: 'triggerExecType',
          width: 100,
          ellipsis: true
        },
        {
          title: this.$t('i18n_2a0bea27c4'),
          key: 'workspaceId',
          ellipsis: true,

          width: '90px'
        },
        {
          title: this.$t('i18n_a497562c8e'),
          key: 'modifyUser',
          ellipsis: true,
          width: 100
        },
        {
          title: this.$t('i18n_2b6bc0f293'),
          key: 'operation',
          align: 'center',

          fixed: 'right',
          width: '100px'
        }
      ]
    }
  },
  computed: {
    pagination() {
      return COMPUTED_PAGINATION(this.listQuery)
    }
  },
  mounted() {
    this.loadData()
  },
  methods: {
    // 加载数据
    loadData(pointerEvent) {
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page
      this.listQuery.nodeId = this.nodeId
      this.loading = true
      getScriptLogList(this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data.result
          this.listQuery.total = res.data.total
        }
        this.loading = false
      })
    },
    parseTime(v) {
      return parseTime(v)
    },
    viewLog(record) {
      this.logVisible = new Date() * Math.random()
      this.temp = record
    },
    handleDelete(record) {
      $confirm({
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        content: this.$t('i18n_7b8e7d4abc'),
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
        onOk: () => {
          return scriptDel({
            nodeId: this.nodeId,
            id: record.scriptId,
            executeId: record.id
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
    // 分页、排序、筛选变化时触发
    changePage(pagination, filters, sorter) {
      this.listQuery = CHANGE_PAGE(this.listQuery, { pagination, sorter })
      this.loadData()
    }
  }
}
</script>
