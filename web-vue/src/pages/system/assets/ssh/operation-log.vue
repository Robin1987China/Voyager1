<template>
  <div>
    <!-- 数据表格 -->
        <n-card size="small" :body-style="{ padding: '12px' }" style="margin-bottom: 12px">

        <n-space wrap class="search-box">
          <n-input
            v-model:value="viewOperationLogListQuery['modifyUser']"
            class="search-input-item"
            :placeholder="$t('i18n_f9ac4b2aa6')"
            @press-enter="handleListLog"
          />
          <n-input
            v-model:value="viewOperationLogListQuery['%sshName%']"
            class="search-input-item"
            :placeholder="$t('i18n_28e1c746f7')"
            @press-enter="handleListLog"
          />
          <n-input
            v-model:value="viewOperationLogListQuery['%machineSshName%']"
            class="search-input-item"
            :placeholder="$t('i18n_bb4409015b')"
            @press-enter="handleListLog"
          />
          <n-input
            v-model:value="viewOperationLogListQuery['ip']"
            class="search-input-item"
            placeholder="ip"
            @press-enter="handleListLog"
          />
          <n-input
            v-model:value="viewOperationLogListQuery['%commands%']"
            class="search-input-item"
            :placeholder="$t('i18n_24cc0de832')"
            @press-enter="handleListLog"
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
        <template v-else-if="column.key === 'modifyUser'">
          <n-tooltip placement="topLeft">
            <template #trigger>
              <span class="tw">
                <span class="tw">
                  <span>{{ record.modifyUser || record.userId }}</span>
                </span>
              </span>
            </template>
            record.modifyUser || record.userId
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
        <template v-else-if="column.key === 'refuse'">
          <span>{{ text ? $t('i18n_330363dfc5') : $t('i18n_7173f80900') }}</span>
        </template>
      </template>
    </n-data-table>
  </div>
</template>
<script>
import { getSshOperationLogList } from '@/api/ssh'
import { CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY, parseTime } from '@/utils/const'
import { getMachineSshOperationLogList } from '@/api/system/assets-ssh'
export default {
  components: {},
  props: {
    sshId: {
      type: String,
      default: ''
    },
    machineSshId: {
      type: String,
      default: ''
    },
    type: {
      type: String,
      default: ''
    }
  },

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
          title: this.$t('i18n_6b0bc6432d'),
          key: 'modifyUser',
          width: 100
        },
        { title: 'IP', key: 'ip', width: '130px' },
        {
          title: `ssh${this.$t('i18n_4f8ca95e7b')}`,
          key: 'sshName',
          width: '200px',
          ellipsis: true
        },
        {
          title: this.$t('i18n_18b0ab4dd2'),
          key: 'machineSshName',
          width: '200px',
          ellipsis: true
        },
        {
          title: this.$t('i18n_24cc0de832'),
          key: 'commands',
          width: 200,
          ellipsis: true
        },
        {
          title: 'userAgent',
          key: 'userAgent',
          width: 240,
          ellipsis: true
        },

        {
          title: this.$t('i18n_7e951d56d9'),
          key: 'createTimeMillis',
          sorter: true,
          render: (row) => {
            return parseTime(row['createTimeMillis'])
          },
          width: '180px'
        },
        {
          title: this.$t('i18n_5e9f2dedca'),
          key: 'refuse',
          width: '100px',
          ellipsis: true,
          fixed: 'right'
        }
      ]
    }
  },
  computed: {
    viewOperationLogPagination() {
      return COMPUTED_PAGINATION(this.viewOperationLogListQuery)
    }
  },
  created() {
    this.handleListLog()
  },
  methods: {
    handleListLog() {
      this.viewOperationLoading = true
      let api
      if (this.type == 'machinessh') {
        // 查看所有日志
        api = getMachineSshOperationLogList
      } else {
        api = this.machineSshId ? getMachineSshOperationLogList : getSshOperationLogList
      }

      api(this.viewOperationLogListQuery).then((res) => {
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
    }
  }
}
</script>
