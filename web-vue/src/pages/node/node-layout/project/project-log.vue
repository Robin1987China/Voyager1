<template>
  <div>
    <n-space direction="vertical" style="width: 100%">
      <n-tag style="display: inline-grid">
        {{ $t('i18n_a17bc8d947') }}
        <b>voyager1.project.log.auto-backup-to-file: false</b>
      </n-tag>

      <n-tag v-if="project.logPath" color="orange">
        {{ $t('i18n_32a19ce88b') }}: {{ project.logPath }}
        <template v-if="project.logSize">
          {{ $t('i18n_3402926291') }}{{ project.logSize }}
          <n-button text size="small" @click="handleDownload">
            <DownloadOutlined />{{ $t('i18n_55405ea6ff') }}
          </n-button>
        </template>
      </n-tag>

      <n-tag v-if="project.logBackPath" color="orange">{{ $t('i18n_c34175dbef') }}{{ project.logBackPath }}</n-tag>

      <!-- 数据表格 -->
      <n-data-table
        :data="logBackList"
        :loading="loading"
        :columns="columns"
        :pagination="false"
        bordered
        >
        <template #bodyCell="{ column, text, row, record }">
          <template v-if="column.key === 'filename'">
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
          <template v-else-if="column.key === 'fileSizeLong'">
            <n-tooltip placement="topLeft">
              <template #trigger>
                {{ text ? renderSize(text) : row.fileSize }}
              </template>
              `${text ? renderSize(text) : row.fileSize}`
            </n-tooltip>
          </template>
          <template v-else-if="column.key === 'modifyTimeLong'">
            <n-tooltip>
              <template #trigger>
                <span class="tw">
                  <span class="tw">
                    <span>{{ parseTime(record.modifyTimeLong) }}</span>
                  </span>
                </span>
              </template>
              `${parseTime(record.modifyTimeLong)}}`
            </n-tooltip>
          </template>
          <template v-else-if="column.key === 'operation'">
            <n-space>
              <n-button type="primary" @click="handleDownloadLogback(record)">{{ $t('i18n_f26ef91424') }}</n-button>
              <n-button type="primary" danger @click="handleDelete(record)">{{ $t('i18n_2f4aaddde3') }}</n-button>
            </n-space>
          </template>
        </template>
      </n-data-table>
    </n-space>
  </div>
</template>
<script>
import { DownloadOutlined } from '@ant-design/icons-vue'

import {
  getLogBackList,
  deleteProjectLogBackFile,
  downloadProjectLogBackFile,
  getProjectLogSize,
  downloadProjectLogFile
} from '@/api/node-project'
import { renderSize, parseTime } from '@/utils/const'
export default {
  props: {
    nodeId: {
      type: String,
      default: ''
    },
    projectId: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      loading: true,
      project: {},
      logBackList: [],
      columns: [
        {
          title: this.$t('i18n_d2e2560089'),
          key: 'filename',
          width: 150,
          ellipsis: true
        },
        {
          title: this.$t('i18n_1303e638b5'),
          key: 'modifyTimeLong',
          width: 150,
          ellipsis: true
        },
        {
          title: this.$t('i18n_396b7d3f91'),
          key: 'fileSizeLong',
          width: 100,
          ellipsis: true
        },
        {
          title: this.$t('i18n_2b6bc0f293'),
          key: 'operation',
          align: 'center',
          fixed: 'right',
          width: '130px'
        }
      ]
    }
  },
  mounted() {
    this.loadFileSize()
    this.loadData()
  },
  methods: {
    renderSize,
    parseTime,
    // 加载日志文件大小
    loadFileSize() {
      const params = {
        nodeId: this.nodeId,
        id: this.projectId
      }
      getProjectLogSize(params).then((res) => {
        if (res.code === 200) {
          this.project = { ...this.project, logSize: res.data.logSize }
        }
      })
    },
    loadData() {
      this.loading = true
      const params = {
        nodeId: this.nodeId,
        id: this.projectId
      }
      getLogBackList(params).then((res) => {
        if (res.code === 200) {
          this.logBackList = res.data.array
          this.project = {
            ...this.project,
            logPath: res.data.logPath,
            logBackPath: res.data.logBackPath
          }
        }
        this.loading = false
      })
    },
    // 下载日志文件
    handleDownload() {
      // 请求参数
      const params = {
        nodeId: this.nodeId,
        id: this.projectId
      }
      // 请求接口拿到 blob
      window.open(downloadProjectLogFile(params), '_blank')
    },
    // 下载日志备份文件
    handleDownloadLogback(record) {
      // 请求参数
      const params = {
        nodeId: this.nodeId,
        id: this.projectId,

        key: record.filename
      }
      // 请求接口拿到 blob
      window.open(downloadProjectLogBackFile(params), '_blank')
    },
    // 删除日志备份文件
    handleDelete(record) {
      $confirm({
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        content: this.$t('i18n_3a6bc88ce0'),
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
        onOk: () => {
          return deleteProjectLogBackFile({
            nodeId: this.nodeId,
            id: this.projectId,

            name: record.filename
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
    }
  }
}
</script>
