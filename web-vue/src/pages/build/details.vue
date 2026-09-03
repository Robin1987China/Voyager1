<template>
  <div>
    <n-space direction="vertical" style="width: 100%">
      <n-descriptions bordered size="small">
        <template #title>
          <n-space>
            {{ data.name }}
            <n-tooltip>
              <template #trigger>
                <span class="tw">
                  <n-button text size="small" @click="refresh"> <ReloadOutlined /></n-button>
                </span>
              </template>
              $t('i18n_f33db5e0b2')
            </n-tooltip>
          </n-space>
        </template>

        <n-descriptions-item :label="$t('i18n_829abe5a8d')">
          {{ data.group }}
        </n-descriptions-item>
        <n-descriptions-item :label="$t('i18n_6f854129e9')">
          {{ data.branchName }} {{ data.branchTagName }}
        </n-descriptions-item>
        <n-descriptions-item :label="$t('i18n_17a74824de')">
          <template v-if="data.buildMode === 1">
            <CloudOutlined />
            {{ $t('i18n_685e5de706') }}
          </template>
          <template v-else>
            <CodeOutlined />
            {{ $t('i18n_69c3b873c1') }}
          </template>
        </n-descriptions-item>

        <n-descriptions-item :label="$t('i18n_66aafbdb72')">
          <span v-if="data.buildId <= 0"></span>
          <n-tag v-else color="#108ee9">#{{ data.buildId }}</n-tag>
        </n-descriptions-item>
        <n-descriptions-item :label="$t('i18n_248c9aa7aa')">
          <n-tooltip>
            <template #trigger>
              <span class="tw">
                <n-tag :color="statusColor[data.status]">
                  {{ statusMap[data.status] || $t('i18n_1622dc9b6b') }}

                  <InfoCircleOutlined v-if="data.statusMsg" />
                </n-tag>
              </span>
            </template>
            data.statusMsg
          </n-tooltip>
        </n-descriptions-item>
        <n-descriptions-item :label="$t('i18n_f98994f7ec')"
          >{{ releaseMethodMap[data.releaseMethod] }}
        </n-descriptions-item>
        <n-descriptions-item :label="$t('i18n_db9296212a')">
          {{ data.autoBuildCron }}
        </n-descriptions-item>
        <n-descriptions-item :label="$t('i18n_2f5e828ecd')">
          {{ data.aliasCode }}
        </n-descriptions-item>
        <n-descriptions-item :label="$t('i18n_d175a854a6')">
          <n-tag>{{ data.sourceDirExist ? $t('i18n_df9497ea98') : $t('i18n_d7d11654a7') }}</n-tag>
        </n-descriptions-item>
        <n-descriptions-item :label="$t('i18n_eca37cb072')">
          {{ parseTime(data.createTimeMillis) }}
        </n-descriptions-item>
        <n-descriptions-item :label="$t('i18n_4b96762a7e')">
          {{ parseTime(data.modifyTimeMillis) }}</n-descriptions-item
        >
        <n-descriptions-item :label="$t('i18n_3bcc1c7a20')">{{ data.modifyUser }}</n-descriptions-item>
        <n-descriptions-item :label="$t('i18n_7dfcab648d')" :span="3">
          {{ data.resultDirFile }}
        </n-descriptions-item>
        <n-descriptions-item v-if="tempRepository" :label="$t('i18n_b3ef35a359')" :span="3">{{
          `${tempRepository ? tempRepository.name + '[' + tempRepository.gitUrl + ']' : ''}`
        }}</n-descriptions-item>
        <n-descriptions-item :label="$t('i18n_86e9e4dd58')" :span="3">{{
          data.repositoryLastCommitId
        }}</n-descriptions-item>
      </n-descriptions>

      <!-- <n-grid type="flex" justify="center"> -->
      <!-- <n-divider v-if="listQuery.total > 0" dashed> 构建历史 </n-divider> -->
      <n-card v-if="listQuery.total > 0" :title="$t('i18n_a05c1667ca')" size="small">
        <template #extra>
          <n-pagination
            v-model:page="listQuery.page"
            v-model:page-size="listQuery.limit"
            size="small"
            :page-sizes="PAGE_DEFAULT_SIZW_OPTIONS"
            :item-count="listQuery.total"
            show-size-picker
            @update:page="listHistory"
            @update:page-size="
              (size) => {
                listQuery.limit = size
                listHistory()
              }
            "
          />
        </template>
        <n-timeline mode="alternate" style="width: 100%">
          <n-timeline-item v-for="item in historyList" :key="item.id" :color="statusColor[item.status]">
            <n-space direction="vertical" style="width: 100%">
              <div>
                <n-space>
                  <span :style="`color: ${statusColor[item.status]};`" @click="handleBuildLog(item)">
                    #{{ item.buildNumberId }} <EyeOutlined />
                  </span>
                  <span v-if="item.buildRemark">{{ $t('i18n_65571516e2') }}{{ item.buildRemark }}</span>
                </n-space>
              </div>
              <div>
                <n-tooltip>
                  <template #trigger>
                    <span class="tw">
                      {{ $t('i18n_bec98b4d6a') }}
                      <n-tag :color="statusColor[item.status]">{{
                        statusMap[item.status] || $t('i18n_1622dc9b6b')
                      }}</n-tag>
                    </span>
                  </template>
                  item.statusMsg || statusMap[item.status] || $t('i18n_1622dc9b6b')
                </n-tooltip>
              </div>
              <div v-if="item.repositoryLastCommitId">
                <div>
                  {{ $t('i18n_e235b0d4af') }}{{ (item.repositoryLastCommitId || '').slice(0, 8) }}
                  <span v-if="item.repositoryLastCommitMsg"
                    >{{ $t('i18n_f27822dd8a') }}{{ item.repositoryLastCommitMsg || '' }}</span
                  >
                </div>
              </div>
              <div>
                {{ $t('i18n_14e6d83ff5') }}{{ parseTime(item.startTime) }} ~
                {{ parseTime(item.endTime) }}
              </div>
              <div>
                {{ $t('i18n_b5a1e1f2d1') }}{{ triggerBuildTypeMap[item.triggerBuildType] || $t('i18n_1622dc9b6b') }}
              </div>
              <div>
                {{ $t('i18n_8dbe0c2ffa') }}{{ renderSize(item.resultFileSize) }}({{ $t('i18n_7dfcab648d') }})/{{
                  renderSize(item.buildLogFileSize)
                }}({{ $t('i18n_456d29ef8b') }})
              </div>

              <div>
                {{ $t('i18n_3c014532b1') }}{{ formatDuration((item.endTime || 0) - (item.startTime || 0), '', 2) }}
              </div>
              <div>
                {{ $t('i18n_e8321f5a61') }}
                <n-tag> {{ releaseMethodMap[item.releaseMethod] || $t('i18n_1622dc9b6b') }}</n-tag>
              </div>
              <div>
                {{ $t('i18n_4a5ab3bc72') }}
                <n-space>
                  <n-tooltip>
                    <template #trigger>
                      <span class="tw">
                        <n-button size="small" type="primary" :disabled="!item.hasLog" @click="handleDownload(item)"
                          ><DownloadOutlined />{{ $t('i18n_456d29ef8b') }}</n-button
                        >
                      </span>
                    </template>
                    $t('i18n_b38d7db9b0')
                  </n-tooltip>

                  <n-tooltip>
                    <template #trigger>
                      <span class="tw">
                        <n-button size="small" type="primary" :disabled="!item.hasFile" @click="handleFile(item)">
                          <DownloadOutlined />
                          {{ $t('i18n_7dfcab648d') }}
                        </n-button>
                      </span>
                    </template>
                    $t('i18n_02e35447d4')
                  </n-tooltip>
                  <template v-if="item.releaseMethod !== 5">
                    <n-button
                      size="small"
                      :disabled="!item.hasFile || item.releaseMethod === 0"
                      type="primary"
                      danger
                      @click="handleRollback(item)"
                      >{{ $t('i18n_d00b485b26') }}
                    </n-button>
                  </template>
                  <template v-else>
                    <n-tooltip>
                      <template #trigger>
                        <span class="tw">
                          <n-button size="small" :disabled="true" type="primary" danger
                            >{{ $t('i18n_d00b485b26') }}
                          </n-button>
                        </span>
                      </template>
                      $t('i18n_2d94b9cf0e')
                    </n-tooltip>
                  </template>
                </n-space>
              </div>
            </n-space>
          </n-timeline-item>
        </n-timeline>
      </n-card>
    </n-space>
    <!-- <n-divider v-if="listQuery.total / listQuery.limit > 1" dashed />
      <n-grid-item>

      </n-grid-item> -->
    <!-- </n-grid> -->

    <!-- 构建日志 -->
    <build-log
      v-if="buildLogVisible > 0"
      :temp="temp"
      :show="buildLogVisible != 0"
      @close="
        () => {
          buildLogVisible = 0
        }
      "
    />
  </div>
</template>
<script>
import {
  CloudOutlined,
  CodeOutlined,
  DownloadOutlined,
  EyeOutlined,
  InfoCircleOutlined,
  ReloadOutlined
} from '@ant-design/icons-vue'

import {
  getBuildGet,
  releaseMethodMap,
  statusMap,
  geteBuildHistory,
  statusColor,
  triggerBuildTypeMap,
  downloadBuildFile,
  downloadBuildLog,
  rollback
} from '@/api/build-info'
import {
  parseTime,
  PAGE_DEFAULT_LIST_QUERY,
  PAGE_DEFAULT_SIZW_OPTIONS,
  PAGE_DEFAULT_SHOW_TOTAL,
  renderSize,
  formatDuration
} from '@/utils/const'
import { getRepositoryInfo } from '@/api/repository'
import BuildLog from './log'

export default {
  components: {
    BuildLog
  },
  props: {
    id: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      PAGE_DEFAULT_SIZW_OPTIONS,
      triggerBuildTypeMap,
      releaseMethodMap,
      statusColor,
      statusMap,
      data: {},
      listQuery: Object.assign({ buildDataId: this.id }, PAGE_DEFAULT_LIST_QUERY),
      historyList: [],
      tempRepository: null,
      buildLogVisible: 0
    }
  },
  computed: {},
  created() {
    if (this.id) {
      this.refresh()
    }
  },
  methods: {
    parseTime,
    formatDuration,
    PAGE_DEFAULT_SHOW_TOTAL,
    renderSize,
    refresh() {
      this.getData()
      this.listHistory()
    },
    // 选择仓库
    getRepositpry() {
      getRepositoryInfo({
        id: this.data?.repositoryId
      }).then((res) => {
        if (res.code === 200) {
          this.tempRepository = res.data
        }
      })
    },
    // 获取构建数据
    getData() {
      // 构建基础信息
      getBuildGet({
        id: this.id
      }).then((res) => {
        if (res.data) {
          this.data = res.data
          this.getRepositpry()
        }
      })
    },
    listHistory() {
      // 构建历史
      geteBuildHistory(this.listQuery).then((res) => {
        if (res.code === 200) {
          this.historyList = res.data.result
          this.listQuery.total = res.data.total
        }
      })
    },
    // 下载构建日志
    handleDownload(record) {
      window.open(downloadBuildLog(record.id), '_blank')
    },

    // 下载构建产物
    handleFile(record) {
      window.open(downloadBuildFile(record.id), '_blank')
    },
    // 查看构建日志
    handleBuildLog(record) {
      this.temp = {
        id: record.buildDataId,
        buildId: record.buildNumberId
      }
      this.buildLogVisible = new Date() * Math.random()
    },

    // 回滚
    handleRollback(record) {
      $confirm({
        title: this.$t('i18n_c4535759ee'),
        content: this.$t('i18n_fb61d4d708'),
        okText: this.$t('i18n_e83a256e4f'),
        zIndex: 1009,
        cancelText: this.$t('i18n_625fb26b4b'),
        onOk: () => {
          return rollback(record.id).then((res) => {
            if (res.code === 200) {
              $notification.success({
                message: res.msg
              })
              this.refresh()
              // 弹窗
              this.temp = {
                id: record.buildDataId,
                buildId: res.data
              }
              this.buildLogVisible = new Date() * Math.random()
            }
          })
        }
      })
    }
  }
}
</script>
