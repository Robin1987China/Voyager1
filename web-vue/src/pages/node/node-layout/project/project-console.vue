<template>
  <div>
    <!-- console -->
    <log-view2 :ref="`logView`" height="calc(100vh - 140px)">
      <template #before>
        <n-space>
          <n-button size="small" :disabled="project.status" :loading="optButtonLoading" type="primary" @click="start">{{
            $t('i18n_8e54ddfe24')
          }}</n-button>
          <n-button
            size="small"
            :disabled="!project.status"
            :loading="optButtonLoading"
            type="primary"
            danger
            @click="restart"
            >{{ $t('i18n_01b4e06f39') }}</n-button
          >
          <n-button
            size="small"
            :disabled="!project.status"
            :loading="optButtonLoading"
            type="primary"
            danger
            @click="stop"
            >{{ $t('i18n_095e938e2a') }}</n-button
          >
          <template v-if="project.runMode === 'Dsl'">
            <template v-if="canReload">
              <n-popover>
                <template #trigger>
                  <span class="tw">
                    <n-button size="small" :loading="optButtonLoading" type="primary" @click="reload">{{
                      $t('i18n_aaeb54633e')
                    }}</n-button>
                  </span>
                </template>
                <template #header>{{ $t('i18n_8b2e274414') }}</template>

                <template v-if="project.lastReloadResult">
                  <p>
                    <n-tag v-if="project.lastReloadResult.success" color="green">{{ $t('i18n_330363dfc5') }}</n-tag>
                    <n-tag v-else color="green">{{ $t('i18n_330363dfc5') }}</n-tag>
                  </p>
                  <p v-for="(item, index) in project.lastReloadResult.msgs" :key="index">
                    {{ item }}
                  </p>
                </template>
                <template v-else>{{ $t('i18n_14dcfcc4fa') }}</template>
              </n-popover>
            </template>
            <template v-else>
              <n-button size="small" :disabled="true" :loading="optButtonLoading" type="primary">{{
                $t('i18n_aaeb54633e')
              }}</n-button>
            </template>
          </template>
          <n-button size="small" type="primary" @click="goFile">{{ $t('i18n_8780e6b3d1') }}</n-button>
          <n-dropdown v-if="project.dslProcessInfo" :options="dslProcessOptions">
            <n-button size="small" type="primary"> {{ $t('i18n_ce40cd6390') }} <DownOutlined /> </n-button>
          </n-dropdown>
          <n-button
            size="small"
            @click="
              (e) => {
                e.preventDefault()
                handleLogBack()
              }
            "
          >
            <!-- <n-tag> -->
            {{ $t('i18n_76aebf3cc6') }}: {{ project.logSize || '-' }}
            <!-- 更多 -->
            <FullscreenOutlined />
            <!-- </n-tag> -->
          </n-button>

          |
        </n-space>
      </template>
    </log-view2>
    <!-- 日志备份 -->
    <CustomModal
      v-if="lobbackVisible"
      v-model:open="lobbackVisible"
      destroy-on-close
      :title="$t('i18n_15f01c43e8')"
      width="850px"
      :footer="null"
      :mask-closable="false"
    >
      <ProjectLog v-if="lobbackVisible" :node-id="nodeId" :project-id="projectId"></ProjectLog>
    </CustomModal>
    <!-- 编辑区 -->
    <ScriptEdit
      v-if="editScriptVisible"
      :node-id="nodeId"
      :script-id="temp.scriptId"
      @close="
        () => {
          editScriptVisible = false
        }
      "
    ></ScriptEdit>
  </div>
</template>
<script>
import { DownOutlined, FullscreenOutlined } from '@ant-design/icons-vue'

import { h } from 'vue'
import { NButton, NSpace, NTag } from 'naive-ui'
import { EditOutlined, ExclamationCircleOutlined } from '@ant-design/icons-vue'
import { getProjectData, getProjectLogSize } from '@/api/node-project'
import { getWebSocketUrl } from '@/api/config'
import { mapState } from 'pinia'
import { useUserStore } from '@/stores/user'
import { useAppStore } from '@/stores/app'
import LogView2 from '@/components/logView/index2'
import ProjectLog from './project-log'
import ScriptEdit from '@/pages/node/script-edit'
export default {
  components: {
    LogView2,
    ProjectLog,
    ScriptEdit
  },
  props: {
    nodeId: {
      type: String,
      default: ''
    },
    projectId: {
      type: String,
      default: ''
    },
    id: {
      type: String,
      default: ''
    }
  },
  emits: ['goFile'],
  data() {
    return {
      project: {},
      optButtonLoading: true,
      loading: false,
      socket: null,
      logExist: false,
      lobbackVisible: false,
      canReload: false,
      heart: null,
      editScriptVisible: false
    }
  },
  computed: {
    ...mapState(useUserStore, ['getLongTermToken']),
    ...mapState(useAppStore, ['getWorkspaceId']),
    dslProcessOptions() {
      return (this.project.dslProcessInfo || []).map((item, index) => ({
        key: index,
        label: () => this.renderDslProcess(item)
      }))
    },
    socketUrl() {
      return getWebSocketUrl(
        '/socket/console',
        `userId=${this.getLongTermToken()}&id=${this.id}&nodeId=${
          this.nodeId
        }&type=console&workspaceId=${this.getWorkspaceId()}`
      )
    }
  },
  mounted() {
    this.loadProject()
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
    renderDslProcess(item) {
      if (item.status) {
        const children = [h(NTag, null, { default: () => item.process })]
        if (item.type === 'file') {
          children.push(h('span', null, this.$t('i18n_4df483b9c7') + item.scriptId))
        } else if (item.type === 'script') {
          children.push(
            h(
              NButton,
              {
                text: true,
                size: 'small',
                onClick: () => {
                  this.temp = { scriptId: item.scriptId }
                  this.editScriptVisible = true
                }
              },
              { default: () => [h(EditOutlined), ' ' + this.$t('i18n_e0ba3b9145')] }
            )
          )
        } else if (item.type === 'library') {
          children.push(
            h(
              NButton,
              { text: true, size: 'small', disabled: true },
              { default: () => this.$t('i18n_91a10b8776') + item.scriptId }
            )
          )
        }
        return h('div', null, children)
      }
      return h(NSpace, null, {
        default: () => [h(NTag, null, { default: () => item.process }), h(ExclamationCircleOutlined), item.msg]
      })
    },
    close() {
      this.socket?.close()

      clearInterval(this.heart)
    },
    // 加载项目
    loadProject(loading) {
      const params = {
        id: this.projectId,
        nodeId: this.nodeId
      }
      getProjectData(params, loading).then((res) => {
        if (res.code === 200) {
          this.project = { ...this.project, ...res.data }

          // 加载日志文件大小
          this.loadFileSize()
        }
      })
    },
    // 初始化
    initWebSocket() {
      //this.logContext = "";
      if (
        !this.socket ||
        this.socket.readyState !== this.socket.OPEN ||
        this.socket.readyState !== this.socket.CONNECTING
      ) {
        this.socket = new WebSocket(this.socketUrl)
      }
      // 连接成功后
      this.socket.onopen = () => {
        this.sendMsg('status')
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
        console.error(err)
        $$message.warning(this.$t('i18n_d6cdafe552'))
        clearInterval(this.heart)
      }
      this.socket.onmessage = (msg) => {
        if (msg.data.indexOf('VOYAGER1_MSG') > -1 && msg.data.indexOf('op') > -1) {
          // console.log(msg.data);
          const res = JSON.parse(msg.data)
          if (
            res.op === 'stop' ||
            res.op === 'start' ||
            res.op === 'restart' ||
            res.op === 'status' ||
            res.op === 'reload'
          ) {
            this.optButtonLoading = false
            $$message.info(res.msg)
            if (res.code === 200) {
              // 如果操作是启动或者停止
              if (res.op === 'stop') {
                this.project = { ...this.project, status: false }
              } else if (res.op === 'start') {
                this.project = { ...this.project, status: true }
              } else if (res.op === 'status') {
                // 如果是 status
                this.project = { ...this.project, status: true }
              }
              if (res.op === 'reload') {
                // 刷新项目信息（reload页面消息）
                this.loadProject()
              }
            } else {
              this.project = { ...this.project, status: false }
            }
            this.canReload = res.canReload
            if (res.data) {
              this.$refs.logView.appendLine(res.data.statusMsg)
              if (res.data.msgs) {
                res.data.msgs.forEach((element) => {
                  this.$refs.logView.appendLine(element)
                })
              }
              res.data.ports && this.$refs.logView.appendLine(this.$t('i18n_b6c9619081') + res.data.ports)
              res.data.pids && this.$refs.logView.appendLine(this.$t('i18n_2b04210d33') + res.data.pids.join(','))
            }
            this.$refs.logView.appendLine(res.op + ' ' + res.msg)
            return
          }
        }
        this.$refs.logView.appendLine(msg.data)

        clearInterval(this.heart)
        // 创建心跳，防止掉线
        this.heart = setInterval(() => {
          this.sendMsg('heart')
          this.loadFileSize()
        }, 5000)
      }
    },
    // 发送消息
    sendMsg(op) {
      const data = {
        op: op,
        projectId: this.projectId
      }
      this.socket.send(JSON.stringify(data))
      if (op === 'stop' || op === 'start' || op === 'restart') {
        this.optButtonLoading = true
      }
    },

    // 加载日志文件大小
    loadFileSize() {
      const params = {
        nodeId: this.nodeId,
        id: this.projectId
      }
      getProjectLogSize(params).then((res) => {
        if (res.code === 200) {
          this.project = { ...this.project, logSize: res.data.logSize }
          if (!this.logExist && res.data?.logSize) {
            this.sendMsg('showlog')
            this.logExist = true
          }
        }
      })
    },
    // 启动
    start() {
      this.sendMsg('start')
    },
    // 重载
    reload() {
      this.sendMsg('reload')
    },
    // 重启
    restart() {
      $confirm({
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        content: this.$t('i18n_989f1f2b61'),
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
        onOk: () => {
          this.sendMsg('restart')
        }
      })
    },
    // 停止
    stop() {
      $confirm({
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        content: this.$t('i18n_010865ca50'),
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
        onOk: () => {
          this.sendMsg('stop')
        }
      })
    },

    // 日志备份列表
    handleLogBack() {
      // 设置显示的数据
      // this.detailData = [];
      this.lobbackVisible = true
    },

    goFile() {
      this.$emit('goFile')
    }
  }
}
</script>
