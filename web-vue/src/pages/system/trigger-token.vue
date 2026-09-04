<template>
  <div>
    <!-- 数据表格 -->
        <n-card size="small" :body-style="{ padding: '12px' }" style="margin-bottom: 12px">

        <n-space wrap class="search-box">
          <n-input
            v-model:value="viewOperationLogListQuery['userId']"
            class="search-input-item"
            :placeholder="$t('i18n_638cddf480')"
            @press-enter="handleListLog"
          />
          <n-input
            v-model:value="viewOperationLogListQuery['triggerToken']"
            class="search-input-item"
            :placeholder="$t('i18n_ae35be7986')"
            @press-enter="handleListLog"
          />
          <n-select
            v-model:value="viewOperationLogListQuery.type"
            clearable
            :placeholder="$t('i18n_226b091218')"
            class="search-input-item"
            :options="allTypeList.map((item) => ({ label: item.desc, value: item.name }))"
          />
          <n-date-picker
            type="datetimerange"
            format="yyyy-MM-dd HH:mm:ss"
            clearable
            @update:value="onchangeListLogTime"
          />
          <n-button type="primary" @click="handleListLog">{{ $t('i18n_e5f71fc31e') }}</n-button>
        </n-space>
      
    </n-card>
<n-data-table
      :data="viewOperationLogList"
      :loading="viewOperationLoading"
      :columns="viewOperationLogColumns"
      :pagination="viewOperationLogPagination"
      bordered
      :row-key="(row) => row.id"
      size="medium"
      @change="changeListLog"
    >
      
      <template #bodyCell="{ column, text, record }">
        <template v-if="column.key === 'commands'">
          <n-tooltip placement="topLeft">
            <template #trigger>
              <span class="tw">
                <copy-text v-if="text" :text="text" />
                {{ text }}
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
        <template v-else-if="column.key === 'operation'">
          <n-space>
            <n-button size="small" type="primary" danger @click="handleDelete(record)">{{
              $t('i18n_2f4aaddde3')
            }}</n-button>
          </n-space>
        </template>
      </template>
    </n-data-table>
  </div>
</template>
<script>
import { CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY, parseTime } from '@/utils/const'
import { triggerTokenList, triggerTokenAllType, triggerTokenDelete } from '@/api/trigger-token'
export default {
  components: {},
  props: {},

  data() {
    return {
      viewOperationLoading: false,
      viewOperationLogList: [],
      viewOperationLogListQuery: Object.assign(
        { sshId: this.sshId, machineSshId: this.machineSshId },
        PAGE_DEFAULT_LIST_QUERY
      ),
      viewOperationLogColumns: [
        {
          title: this.$t('i18n_95a43eaa59'),
          key: 'userId',
          width: 100
        },
        {
          title: 'token',
          key: 'triggerToken',
          width: 100
        },

        {
          title: this.$t('i18n_d159466d0a'),
          key: 'dataName'
          // width: 100
        },
        {
          title: this.$t('i18n_00d5bdf1c3'),
          key: 'triggerCount',
          width: 100,
          sorter: true
        },
        {
          title: this.$t('i18n_45a4922d3f'),
          key: 'dataId',
          width: 100
        },

        {
          title: this.$t('i18n_eca37cb072'),
          key: 'createTimeMillis',
          sorter: true,
          render: (row) => {
            return parseTime(row['createTimeMillis'])
          },
          width: '180px'
        },
        {
          title: this.$t('i18n_2b6bc0f293'),
          key: 'operation',
          width: '80px',

          align: 'center',
          fixed: 'right'
        }
      ],

      allTypeList: []
    }
  },
  computed: {
    viewOperationLogPagination() {
      return COMPUTED_PAGINATION(this.viewOperationLogListQuery)
    }
  },
  created() {
    triggerTokenAllType().then((res) => {
      if (res.code === 200) {
        this.allTypeList = res.data || []
      }
    })
    this.handleListLog()
  },
  methods: {
    handleListLog() {
      this.viewOperationLoading = true

      triggerTokenList(this.viewOperationLogListQuery).then((res) => {
        if (res.code === 200) {
          this.viewOperationLogList = res.data.result
          this.viewOperationLogListQuery.total = res.data.total
        }
        this.viewOperationLoading = false
      })
    },
    changeListLog(pagination, filters, sorter) {
      this.viewOperationLogListQuery = CHANGE_PAGE(this.viewOperationLogListQuery, { pagination, sorter })

      this.handleListLog()
    },
    // 选择时间
    onchangeListLogTime(value, dateString) {
      if (dateString[0]) {
        this.viewOperationLogListQuery.createTimeMillis = `${dateString[0]} ~ ${dateString[1]}`
      } else {
        this.viewOperationLogListQuery.createTimeMillis = ''
      }
    },
    // 删除
    handleDelete(record) {
      $confirm({
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        content: this.$t('i18n_bba360b084'),
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
        onOk: () => {
          return triggerTokenDelete({
            id: record.id
          }).then((res) => {
            if (res.code === 200) {
              $notification.success({
                message: res.msg
              })
              this.handleListLog()
            }
          })
        }
      })
    }
  }
}
</script>
