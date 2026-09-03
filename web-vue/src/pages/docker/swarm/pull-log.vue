<template>
  <div>
    <log-view
      :ref="`logView`"
      :title-name="$t('i18n_6a658517f3')"
      :show="visible || show"
      @close="
        () => {
          $emit('close')
        }
      "
    >
      <template #before>
        <n-space>
          <n-input-number v-model:value="tail" :placeholder="$t('i18n_5734b2db4e')" style="width: 150px">
            <template #prefix>
              <n-tooltip :title="$t('i18n_25b6c22d8a')">{{ $t('i18n_87eb55155a') }} </n-tooltip>
            </template>
          </n-input-number>
          <div>
            {{ $t('i18n_d731dc9325') }}
            <n-switch
              v-model:value="timestamps"
              :checked-label="$t('i18n_4d775d4cd7')"
              :unchecked-label="$t('i18n_2064fc6808')"
            />
          </div>
          <n-button type="primary" size="small" @click="init"><ReloadOutlined /> {{ $t('i18n_694fc5efa9') }} </n-button>
          |
          <n-button type="primary" :disabled="!logId" size="small" @click="download">
            <DownloadOutlined /> {{ $t('i18n_f26ef91424') }}
          </n-button>
          |
        </n-space>
      </template>
    </log-view>
  </div>
</template>
<script>
import { DownloadOutlined, ReloadOutlined } from '@ant-design/icons-vue'

import LogView from '@/components/logView'
import {
  dockerSwarmServicesPullLog,
  dockerSwarmServicesStartLog,
  dockerSwarmServicesDownloaLog
} from '@/api/docker-swarm'
export default {
  components: {
    LogView
  },
  props: {
    dataId: {
      type: String,
      default: ''
    },
    id: {
      type: String,
      default: ''
    },
    type: {
      type: String,
      default: ''
    },
    urlPrefix: {
      type: String,
      default: ''
    },
    visible: {
      type: Boolean,
      default: false
    },
    // 调用方普遍传 :show=，此处兼容 show 别名
    show: {
      type: Boolean,
      default: false
    }
  },
  emits: ['close'],
  data() {
    return {
      logTimer: null,
      logId: '',
      line: 1,
      tail: 500,
      timestamps: false
    }
  },
  beforeUnmount() {
    this.logTimer && clearTimeout(this.logTimer)
  },
  mounted() {
    //
    this.init()
  },
  methods: {
    init() {
      this.logTimer && clearTimeout(this.logTimer)
      this.$refs.logView.clearLogCache()
      this.line = 1
      //
      dockerSwarmServicesStartLog(this.urlPrefix, {
        type: this.type,
        dataId: this.dataId,
        id: this.id,
        tail: this.tail,
        timestamps: this.timestamps
      }).then((res) => {
        if (res.code === 200) {
          this.logId = res.data
          this.pullLog()
        } else {
          this.$refs.logView.appendLine(res.msg)
        }
      })
    },
    nextPull() {
      this.logTimer && clearTimeout(this.logTimer)
      // 加载构建日志
      this.logTimer = setTimeout(() => {
        this.pullLog()
      }, 2000)
    },
    // 加载日志内容
    pullLog() {
      const params = {
        id: this.logId,
        line: this.line
      }
      dockerSwarmServicesPullLog(this.urlPrefix, params).then((res) => {
        let next = true
        if (res.code === 200) {
          // 停止请求
          const dataLines = res.data.dataLines

          this.$refs.logView.appendLine(dataLines)
          this.line = res.data.line
        }
        // 继续拉取日志
        if (next) this.nextPull()
      })
    },
    // 下载
    download() {
      window.open(dockerSwarmServicesDownloaLog(this.urlPrefix, this.logId), '_blank')
    }
  }
}
</script>
