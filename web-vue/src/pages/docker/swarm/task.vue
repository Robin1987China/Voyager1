<template>
  <div>
        <n-card size="small" :body-style="{ padding: '12px' }" style="margin-bottom: 12px">

        <n-space wrap class="search-box">
          <n-input
            v-if="!serviceId"
            v-model:value="listQuery['serviceId']"
            :placeholder="$t('i18n_dbb166cf29')"
            class="search-input-item"
            @press-enter="loadData"
          />
          <n-input
            v-model:value="listQuery['taskName']"
            :placeholder="$t('i18n_78caf7115c')"
            class="search-input-item"
            @press-enter="loadData"
          />
          <n-input
            v-model:value="listQuery['taskId']"
            :placeholder="$t('i18n_ac0158db83')"
            class="search-input-item"
            @press-enter="loadData"
          />
          <n-input
            v-model:value="listQuery['taskNode']"
            :placeholder="$t('i18n_c90a1f37ce')"
            class="search-input-item"
            @press-enter="loadData"
          />

          <n-tooltip>
            <template #trigger>
              <span class="tw">
                <n-select
                  v-model:value="listQuery['taskState']"
                  filterable
                  clearable
                  :placeholder="$t('i18n_3fea7ca76c')"
                  class="search-input-item"
                  :options="[
                    ...Object.entries(TASK_STATE).map(([key, item]) => ({ label: `${item}- ${key}`, value: key })),
                    { label: $t('i18n_3fea7ca76c'), value: '' }
                  ]"
                />
              </span>
            </template>
            TASK_STATE[listQuery['taskState']]
          </n-tooltip>
          <n-button type="primary" :loading="loading" @click="loadData">{{ $t('i18n_e5f71fc31e') }}</n-button>
          <n-statistic format="s" :title="$t('i18n_0f8403d07e')" :value="countdownTime" @finish="loadData">
            <template #suffix>
              <div style="font-size: 12px">{{ $t('i18n_ee6ce96abb') }}</div>
            </template>
          </n-statistic>
        </n-space>
      
    </n-card>
<n-data-table
      :data="list"
      size="medium"
      :columns="columns"
      bordered
      :row-key="(row) => row.id"
      :pagination="false"
      >
      

      <template #bodyCell="{ column, text, record }">
        <template v-if="column.tooltip">
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

        <template v-else-if="column.key === 'address'">
          <n-tooltip placement="topLeft">
            <template #trigger>
              <span class="tw">
                <CloudServerOutlined v-if="record.managerStatus && record.managerStatus.leader" />

                {{ text }}
              </span>
            </template>
            text
          </n-tooltip>
        </template>
        <template v-else-if="column.key === 'desiredState'">
          <n-popover placement="top-start">
            <template #trigger>
              <span class="tw">
                <n-tag :color="`${record.status && record.status.err ? 'orange' : text === 'RUNNING' ? 'green' : ''}`">
                  {{ text }}
                </n-tag>
              </span>
            </template>
            <template #header>{{ `${$t('i18n_ec989813ed')}${TASK_STATE[text]}` }}</template>

            <p>
              {{ $t('i18n_e703c7367c') }}<n-tag>{{ text }}-{{ TASK_STATE[text] }}</n-tag>
            </p>
            <p v-if="record.status && record.status.err">{{ $t('i18n_f66335b5bf') }}{{ record.status.err }}</p>
            <p v-if="record.status && record.status.state">
              {{ $t('i18n_bec98b4d6a') }}<n-tag>{{ record.status.state }}</n-tag>
            </p>

            <p v-if="record.status && record.status.message">
              {{ $t('i18n_a90cf0796b') }}<n-tag>{{ record.status.message }} </n-tag>
            </p>
            <p v-if="record.status && record.status.timestamp">
              {{ $t('i18n_780fb9f3d0') }}<n-tag>{{ parseTime(record.status.timestamp) }} </n-tag>
            </p>
          </n-popover>
        </template>

        <template v-else-if="column.key === 'os'">
          <n-tooltip placement="topLeft">
            <template #trigger>
              <span class="tw">
                <span class="tw">
                  <span>
                    <n-tag
                      >{{ text }}-{{
                        record.description && record.description.platform && record.description.platform.architecture
                      }}
                    </n-tag>
                  </span>
                </span>
              </span>
            </template>
            text
          </n-tooltip>
        </template>
        <template v-else-if="column.key === 'updatedAt'">
          <n-tooltip placement="topLeft">
            <template #trigger>
              <span class="tw">
                <span class="tw">
                  <span>
                    {{ parseTime(text) }}
                  </span>
                </span>
              </span>
            </template>
            `${$t('i18n_bf94b97d1a')}${text} ${$t('i18n_312f45014a')}${record.createdAt}`
          </n-tooltip>
        </template>

        <template v-else-if="column.key === 'operation'">
          <n-space>
            <n-button size="small" type="primary" @click="handleLog(record)">{{ $t('i18n_456d29ef8b') }}</n-button>
          </n-space>
        </template>
      </template>
    </n-data-table>

    <!-- 查看日志 -->

    <pull-log
      v-if="logVisible > 0"
      :id="id"
      :show="logVisible != 0"
      :data-id="temp.id"
      type="taks"
      :url-prefix="urlPrefix"
      @close="
        () => {
          logVisible = 0
        }
      "
    />
  </div>
</template>
<script>
import { CloudServerOutlined } from '@ant-design/icons-vue'

import { dockerSwarmServicesTaskList, TASK_STATE } from '@/api/docker-swarm'
import { parseTime } from '@/utils/const'
import PullLog from './pull-log'

export default {
  components: { PullLog },
  props: {
    id: {
      type: String,
      default: ''
    },
    serviceId: { type: String, default: '' },
    taskState: {
      type: String,
      default: ''
    },
    visible: {
      type: Boolean,
      default: false
    },
    urlPrefix: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      TASK_STATE,
      loading: false,
      listQuery: {},
      list: [],
      temp: {},
      editVisible: false,
      initSwarmVisible: false,
      autoUpdateTime: null,
      logVisible: 0,
      rules: {
        role: [{ required: true, message: this.$t('i18n_9d7d471b77'), trigger: 'blur' }],
        availability: [{ required: true, message: this.$t('i18n_4c7c58b208'), trigger: 'blur' }]
      },
      columns: [
        {
          title: this.$t('i18n_faaadc447b'),
          width: '80px',
          ellipsis: true,
          align: 'center',
          render: (row, index) => `${index + 1}`
        },
        {
          title: this.$t('i18n_6da242ea50'),
          key: 'id',
          ellipsis: true
        },
        {
          title: this.$t('i18n_a472019766'),
          key: 'nodeId',
          ellipsis: true
        },
        {
          title: this.$t('i18n_b7ec1d09c4'),
          key: 'serviceId',
          ellipsis: true
        },
        {
          title: this.$t('i18n_3477228591'),
          key: ['spec', 'containerSpec', 'image'],
          ellipsis: true,
          width: 120
        },
        // { title: "副本数", key: "spec.mode.replicated.replicas", width: 90, ellipsis: true,  },
        // { title: "端点", key: "spec.endpointSpec.mode", ellipsis: true, width: 100, },
        // { title: "节点地址", width: 150, key: "status.address", ellipsis: true,  },
        {
          title: this.$t('i18n_3fea7ca76c'),
          width: 140,
          key: 'desiredState',
          ellipsis: true
        },
        {
          title: this.$t('i18n_4604d50234'),
          width: 150,
          key: ['status', 'err'],
          ellipsis: true
        },
        {
          title: 'slot',
          width: '80px',
          key: 'slot',
          ellipsis: true
        },

        // { title: "系统类型", width: 140, align: "center", key: "description.platform.os", ellipsis: true,  },
        // {
        //   title: "创建时间",
        //   key: "createdAt",

        //   ellipsis: true,

        //   width: 170,
        // },
        {
          title: this.$t('i18n_1303e638b5'),
          key: 'updatedAt',
          ellipsis: true,

          sorter: (a, b) => new Date(a.updatedAt).getTime() - new Date(b.updatedAt).getTime(),
          defaultSortOrder: 'descend',
          width: '180px'
        },
        {
          title: this.$t('i18n_2b6bc0f293'),
          key: 'operation',
          fixed: 'right',
          align: 'center',
          width: '80px'
        }
      ],

      countdownTime: Date.now()
    }
  },
  computed: {},
  beforeUnmount() {},
  mounted() {
    this.listQuery.taskState = this.taskState
    this.loadData()
  },
  methods: {
    parseTime,
    // 加载数据
    loadData() {
      if (!this.visible) {
        return
      }
      this.loading = true
      if (this.serviceId) {
        this.listQuery.serviceId = this.serviceId
      }
      this.listQuery.id = this.id
      dockerSwarmServicesTaskList(this.urlPrefix, this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data
        }
        this.loading = false
        this.countdownTime = Date.now() + 5 * 1000
      })
    },
    // 日志
    handleLog(record) {
      this.logVisible = new Date() * Math.random()
      this.temp = record
    }
  }
}
</script>
<style scoped>
:deep(.n-statistic .n-statistic-value__content),
:deep(.n-statistic .n-statistic-value__prefix),
:deep(.n-statistic .n-statistic-value__suffix) {
  font-size: 16px;
}
</style>
