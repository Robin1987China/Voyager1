<template>
  <div>
    <CustomTable
      is-show-tools
      default-auto-refresh
      :auto-refresh-time="30"
      :active-page="activePage"
      table-name="system-task-stat"
      :empty-description="$t('i18n_4ef719810b')"
      size="medium"
      row-key="taskId"
      :columns="taskColumns"
      bordered
      :data="taskList"
      :pagination="false"
      @refresh="refresh"
    >
      <!-- <template #title>
        <n-button size="small" type="primary" @click="refresh"><ReloadOutlined /></n-button>
      </template> -->
      <template #tableBodyCell="{ column, text, record }">
        <n-tooltip v-if="column.tooltip" placement="topLeft">
          <template #trigger>
            <span class="tw">
              <span class="tw">
                <span>{{ text }}</span>
              </span>
            </span>
          </template>
          text
        </n-tooltip>
        <n-tooltip v-else-if="column.dataIndex === 'lastExecuteTime'">
          <template #trigger>
            <span class="tw">
              <span class="tw">
                <span>{{ parseTime(text) }}</span>
              </span>
            </span>
          </template>
          parseTime(text)
        </n-tooltip>
        <n-tooltip v-else-if="column.dataIndex === 'desc'">
          <template #trigger>
            <span class="tw">
              <span class="tw">
                <span>{{ text }}</span>
              </span>
            </span>
          </template>
          text
        </n-tooltip>
        <n-tooltip v-else-if="column.dataIndex === 'cron'" placement="top-start">
          <template #trigger>
            <span class="tw">
              <n-button v-if="text" text style="padding: 0" size="small" @click="toCronTaskList(text)">
                {{ text }} <UnorderedListOutlined />
              </n-button>
              <template v-else>{{ record.desc }}</template>
            </span>
          </template>
          {{ text }}
        </n-tooltip>
      </template>
    </CustomTable>
  </div>
</template>
<script>
import { ReloadOutlined, UnorderedListOutlined } from '@ant-design/icons-vue'

import { parseTime } from '@/utils/const'
export default {
  name: 'TaskStat',
  props: {
    taskList: {
      type: Array,
      default: () => []
    }
  },
  emits: ['refresh'],
  data() {
    return {
      temp: {},

      taskColumns: [
        {
          title: this.$t('i18n_3a3778f20c'),
          key: 'taskId',

          // sorter: (a, b) => (a && b ? a.localeCompare(b, "zh-CN") : 0),

          ellipsis: true,
          filters: [
            {
              text: this.$t('i18n_fcba60e773'),
              value: 'build'
            },
            {
              text: this.$t('i18n_e0ba3b9145'),
              value: 'script'
            },
            {
              text: this.$t('i18n_8c7c7f3cfa'),
              value: 'server_script'
            },
            {
              text: `ssh ${this.$t('i18n_ba311d8a6a')}`,
              value: 'ssh_command'
            }
          ],

          onFilter: (value, record) => record.taskId.indexOf(value) === 0
        },
        {
          title: 'cron',
          key: 'cron'
          // sorter: (a, b) => (a && b ? a.localeCompare(b, "zh-CN") : 0),
          // sortDirections: ["descend", "ascend"],
        },
        // {
        //   title: '描述',
        //   key: 'desc'
        //   // sorter: (a, b) => (a && b ? a.localeCompare(b, "zh-CN") : 0),
        //   // sortDirections: ["descend", "ascend"],
        // },
        {
          title: this.$t('i18n_d4aea8d7e6'),
          key: 'executeCount',
          width: 140,
          sorter: (a, b) => a.executeCount || 0 - b.executeCount || 0
        },
        {
          title: this.$t('i18n_e7d83a24ba'),
          key: 'succeedCount',
          width: 140,
          sorter: (a, b) => a.succeedCount || 0 - b.succeedCount || 0
        },
        {
          title: this.$t('i18n_d3e480c8c0'),
          key: 'failedCount',
          width: 140,
          sorter: (a, b) => a.failedCount || 0 - b.failedCount || 0
        },
        {
          title: this.$t('i18n_17c06f6a8b'),
          key: 'lastExecuteTime',
          defaultSortOrder: 'descend',
          width: 180,
          sorter: (a, b) => a.lastExecuteTime || 0 - b.lastExecuteTime || 0
        }
      ]
    }
  },
  computed: {
    activePage() {
      return this.$attrs.routerUrl === this.$route.path
    }
  },
  mounted() {},
  methods: {
    parseTime,
    refresh() {
      this.$emit('refresh', {})
    },
    // 前往 cron 详情
    toCronTaskList(cron) {
      const newpage = this.$router.resolve({
        path: '/tools/cron',
        query: {
          ...this.$route.query,
          sPid: 'tools',
          sId: 'cronTools',
          cron
        }
      })
      window.open(newpage.href, '_blank')
    }
  }
}
</script>
