<template>
  <!-- console -->
  <div>
    <log-view2
      ref="logView"
      :title-name="$t('i18n_cac26240b5')"
      :show="visible"
      @close="
        () => {
          $emit('close')
        }
      "
    >
      <template #before>
        <n-space>
          <div>
            <n-input-number v-model:value="tail" :placeholder="$t('i18n_5734b2db4e')" style="width: 150px">
              <template #prefix>
                <n-tooltip :title="$t('i18n_25b6c22d8a')">{{ $t('i18n_87eb55155a') }} </n-tooltip>
              </template>
            </n-input-number>
          </div>
          <div>
            {{ $t('i18n_d731dc9325') }}
            <n-switch
              v-model:value="timestamps"
              :checked-label="$t('i18n_4d775d4cd7')"
              :unchecked-label="$t('i18n_2064fc6808')"
            />
          </div>
          <n-button type="primary" size="small" @click="initWebSocket"
            ><ReloadOutlined /> {{ $t('i18n_694fc5efa9') }}
          </n-button>
          |
          <n-button type="primary" :disabled="!logId" size="small" @click="download">
            <DownloadOutlined /> {{ $t('i18n_f26ef91424') }}
          </n-button>
          |
        </n-space>
      </template>
    </log-view2>
  </div>
</template>
<script>
import { DownloadOutlined, ReloadOutlined } from '@ant-design/icons-vue'

import { mapState } from 'pinia'
import { getWebSocketUrl } from '@/api/config'
import { ReconnectSocket } from '@/utils/reconnect-socket'
import { dockerContainerDownloaLog } from '@/api/docker-api'
import { useUserStore } from '@/stores/user'
import { useAppStore } from '@/stores/app'
import LogView2 from '@/components/logView'

export default {
  components: {
    LogView2
  },
  props: {
    id: {
      type: String,
      default: ''
    },
    containerId: { type: String, default: '' },
    machineDockerId: {
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
    }
  },
  emits: ['close'],
  data() {
    return {
      // 主动关闭标记（区分组件卸载/被动断线）
      manualClose: false,
      socket: null,
      tail: 500,
      timestamps: false,
      logId: ''
    }
  },
  computed: {
    ...mapState(useUserStore, ['getLongTermToken']),
    ...mapState(useAppStore, ['getWorkspaceId']),
    socketUrl() {
      return getWebSocketUrl(
        '/socket/docker_log',
        `userId=${this.getLongTermToken()}&id=${this.id}&machineDockerId=${
          this.machineDockerId
        }&type=dockerLog&nodeId=system&workspaceId=${this.getWorkspaceId()}`
      )
    }
  },
  mounted() {
    this.initWebSocket()
    // 监听窗口关闭事件，当窗口关闭时，主动去关闭websocket连接，防止连接还没断开就关闭窗口，server端会抛异常。
    window.onbeforeunload = () => {
      this.close()
    }
  },
  beforeUnmount() {
    this.close()
  },
  methods: {
    close() {
      // 标记主动关闭，避免 onclose 误弹“连接已关闭”
      this.manualClose = true
      clearInterval(this.heart)
      this.socket?.close()
    },
    // 初始化
    initWebSocket() {
      this.close()
      this.$refs.logView.clearLogCache()
      //
      let tail = parseInt(this.tail)
      this.tail = isNaN(tail) ? 500 : tail
      //
      this.socket = new ReconnectSocket(this.socketUrl)

      // 连接成功后
      this.socket.onopen = () => {
        this.sendMsg('showlog')
      }
      this.socket.onerror = (err) => {
        console.error(err)
        $notification.error({
          message: `web socket ${this.$t('i18n_7030ff6470')},${this.$t('i18n_226a6f9cdd')}`
        })
        clearInterval(this.heart)
      }
      this.socket.onclose = (err) => {
        //当客户端收到服务端发送的关闭连接请求时，触发onclose事件
        if (this.manualClose) {
          return
        }
        console.error(err)
        clearInterval(this.heart)
        $$message.warning(this.$t('i18n_948171025e'))
      }
      this.socket.onmessage = (msg) => {
        if (msg.data.indexOf('code') > -1 && msg.data.indexOf('msg') > -1) {
          try {
            const res = JSON.parse(msg.data)
            if (res.code === 200 && res.msg === 'VOYAGER1_MSG_UUID') {
              this.logId = res.data
              return
            }
          } catch (e) {
            console.error(e)
          }
          //   return;
        }
        const msgLine = msg.data || ''
        // this.$refs.logView.appendLine(msgLine.substring(0, msgLine.lastIndexOf('\n')))
        this.$refs.logView.appendLine(msgLine)
        clearInterval(this.heart)
        // 创建心跳，防止掉线
        this.heart = setInterval(() => {
          this.sendMsg('heart')
        }, 5000)
      }
    },
    // 发送消息
    sendMsg(op) {
      const data = {
        op: op,
        containerId: this.containerId,
        tail: this.tail,
        timestamps: this.timestamps
      }
      this.socket.send(JSON.stringify(data))
    },
    // 下载
    download() {
      window.open(dockerContainerDownloaLog(this.urlPrefix, this.logId), '_blank')
    }
  }
}
</script>
