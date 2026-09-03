<template>
  <div>
    <div ref="filter" class="filter">
      <n-space>
        <n-date-picker
          v-model:formatted-value="timeRange"
          type="datetimerange"
          :presets="[
            { label: $t('i18n_800dfdd902'), value: [dayjs().startOf('day').valueOf(), dayjs().valueOf()] },
            {
              label: $t('i18n_2f8d6f1584'),
              value: [dayjs().add(-1, 'days').startOf('day').valueOf(), dayjs().add(-1, 'days').endOf('day').valueOf()]
            }
          ]"
          :is-date-disabled="
            (ts) => {
              const current = dayjs(ts)
              return current && current >= dayjs().endOf('day')
            }
          "
          class="filter-item"
          format="yyyy-MM-dd HH:mm:ss"
          value-format="yyyy-MM-dd HH:mm:ss"
          clearable
        />
        <n-button type="primary" @click="handleFilter">{{ $t('i18n_e5f71fc31e') }}</n-button>
        <n-tooltip>
          <template #trigger>
            <QuestionCircleOutlined />
          </template>

          <div>
            <ul>
              <li>{{ $t('i18n_b11b0c93fa') }}</li>
              <li>{{ $t('i18n_413f20d47f') }}</li>
              <li>{{ $t('i18n_7617455241') }}</li>
              <li>{{ $t('i18n_c8b2aabc07') }}</li>
            </ul>
          </div>
        </n-tooltip>
      </n-space>
    </div>
    <div v-if="nodeMonitorLoadStatus == 1" id="historyChart" class="historyChart">loading...</div>
    <n-empty v-else-if="nodeMonitorLoadStatus == -1" :description="$t('i18n_85be08c99a')"> </n-empty>
    <n-skeleton v-else />
  </div>
</template>
<script>
import { QuestionCircleOutlined } from '@ant-design/icons-vue'

import { nodeMonitorData } from '@/api/node'
import { drawChart, generateNodeTopChart, generateNodeNetworkTimeChart, generateNodeNetChart } from '@/api/node-stat'
import dayjs from 'dayjs'
import { useGuideStore } from '@/stores/guide'
import { mapState } from 'pinia'
import { NEmpty as Empty } from 'naive-ui'
export default {
  components: {},
  props: {
    nodeId: {
      type: String,
      default: ''
    },
    machineId: {
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
      Empty,
      timeRange: null,
      historyData: [],
      historyChart: null,
      nodeMonitorLoadStatus: 0
    }
  },
  computed: {
    ...mapState(useGuideStore, ['getThemeView'])
  },
  watch: {},
  mounted() {
    this.handleFilter()
    window.addEventListener('resize', this.resize)
  },
  unmounted() {
    window.removeEventListener('resize', this.resize)
  },
  methods: {
    dayjs,
    // 刷新
    handleFilter() {
      const params = {
        nodeId: this.nodeId,
        machineId: this.machineId
        // time: this.timeRange
      }
      if (this.timeRange && this.timeRange[0]) {
        params.startTime = this.timeRange[0]
        params.endTime = this.timeRange[1]
      } else {
        params.startTime = ''
        params.endTime = ''
      }
      // 加载数据
      nodeMonitorData(params)
        .then((res) => {
          if (res.code === 200) {
            if (res.data && res.data.length) {
              this.nodeMonitorLoadStatus = 1
              this.$nextTick(() => {
                if (this.type === 'networkDelay') {
                  this.historyChart = drawChart(
                    res.data,
                    'historyChart',
                    generateNodeNetworkTimeChart,
                    this.getThemeView()
                  )
                } else if (this.type === 'network-stat') {
                  this.historyChart = drawChart(res.data, 'historyChart', generateNodeNetChart, this.getThemeView())
                } else {
                  this.historyChart = drawChart(res.data, 'historyChart', generateNodeTopChart, this.getThemeView())
                }
              })

              return
            }
          }
          this.nodeMonitorLoadStatus = -1
        })
        .catch(() => {
          this.nodeMonitorLoadStatus = -1
        })
    },
    resize() {
      this.historyChart?.resize()
    }
  }
}
</script>
<style scoped>
.historyChart {
  height: 50vh;
  margin-top: 10px;
}
</style>
