<template>
  <n-layout class="log-layout">
    <!-- 侧边栏 文件树 -->
    <n-layout-sider theme="light" class="log-sider" width="20%">
      <n-empty v-if="list.length === 0" />
      <n-tree
        :data="list"
        :label-field="'title'"
        :key-field="'path'"
        :children-field="'children'"
        @update:selected-keys="select"
      ></n-tree>
    </n-layout-sider>
    <!-- 单个文件内容 -->
    <n-layout-content class="log-content">
      <log-view2 :ref="`logView`" height="calc(100vh - 160px - 30px)">
        <template #before>
          <n-space>
            <n-button type="primary" size="small" @click="loadData">{{ $t('i18n_694fc5efa9') }}</n-button>
            <n-button type="primary" danger size="small" :disabled="!temp.path" @click="deleteLog">{{
              $t('i18n_2f4aaddde3')
            }}</n-button>
            <n-button type="primary" size="small" :disabled="!temp.path" @click="downloadLog">{{
              $t('i18n_f26ef91424')
            }}</n-button>
          </n-space>
        </template>
      </log-view2>
    </n-layout-content>
    <!-- 对话框 -->
    <!-- <CustomModal v-if="visible" v-model="visible" title="系统提示" :footer="null">
        <n-space>
          <n-button type="primary" danger @click="deleteLog">删除日志文件</n-button>
          <n-button type="primary" @click="downloadLog">下载日志文件</n-button>
          <n-button @click="visible = false">取消</n-button>
        </n-space>
      </CustomModal> -->
  </n-layout>
</template>
<script>
import { getLogList, downloadFile, deleteLog } from '@/api/system'
import { mapState } from 'pinia'
import { useUserStore } from '@/stores/user'
import { getWebSocketUrl } from '@/api/config'
import { ReconnectSocket } from '@/utils/reconnect-socket'
import LogView2 from '@/components/logView/index2'
import { NEmpty as Empty } from 'naive-ui'
export default {
  components: {
    LogView2
  },
  props: {
    machineId: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      // 主动关闭标记（区分组件卸载/被动断线）
      manualClose: false,
      Empty,
      list: [],
      socket: null,
      // 日志内容
      logContext: 'choose file loading context...',

      replaceFields: {
        children: 'children',
        title: 'title',
        key: 'path'
      },
      // visible: false,
      temp: {}
    }
  },
  computed: {
    ...mapState(useUserStore, ['getLongTermToken']),
    socketUrl() {
      return getWebSocketUrl(
        '/socket/agent_log',
        `userId=${this.getLongTermToken()}&machineId=${this.machineId}&nodeId=system&type=agentLog`
      )
    }
  },
  watch: {},
  created() {
    this.loadData()
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
      this.socket?.close()
    },
    // 加载数据
    loadData() {
      this.list = []
      const params = { machineId: this.machineId }
      getLogList(params).then((res) => {
        if (res.code === 200) {
          res.data.forEach((element) => {
            if (element.children) {
              this.calcTreeNode(element.children)
            }
            // 组装数据
            this.list.push({
              ...element,
              isLeaf: !element.children ? true : false
            })
          })
        }
      })
    },
    // 递归处理节点
    calcTreeNode(list) {
      list.forEach((element) => {
        if (element.children) {
          this.calcTreeNode(element.children)
        } else {
          // 叶子节点
          element.isLeaf = true
        }
      })
    },
    // 选择节点
    select(selectedKeys, { node }) {
      if (this.temp?.path === node.dataRef?.path) {
        return
      }
      if (!node.dataRef.isLeaf) {
        return
      }
      const data = {
        op: 'showlog',
        tomcatId: this.tomcatId,
        fileName: node.dataRef.path
      }
      this.temp = node.dataRef
      this.$refs.logView.clearLogCache()

      this.socket?.close()

      this.socket = new ReconnectSocket(this.socketUrl)
      // 连接成功后
      this.socket.onopen = () => {
        this.socket.send(JSON.stringify(data))
      }
      this.socket.onmessage = (msg) => {
        this.$refs.logView.appendLine(msg.data)
      }
      this.socket.onerror = (err) => {
        console.error(err)
        $notification.error({
          message: `web socket ${this.$t('i18n_7030ff6470')},${this.$t('i18n_226a6f9cdd')}`
        })
      }
      this.socket.onclose = (err) => {
        //当客户端收到服务端发送的关闭连接请求时，触发onclose事件
        if (this.manualClose) {
          return
        }
        console.error(err)
        $$message.warning(this.$t('i18n_1b5bcdf115') + node.dataRef.path)
        // clearInterval(this.heart);
      }
    },
    // // 右键点击
    // rightClick({ node }) {
    //   this.temp = node.dataRef;
    //   // 弹出提示 下载还是删除
    //   this.visible = true;
    // },
    // 下载文件
    downloadLog() {
      // 请求参数
      const params = {
        machineId: this.machineId,
        path: this.temp.path
      }
      // 请求接口拿到 blob
      window.open(downloadFile(params), '_blank')
    },
    // 删除文件
    deleteLog() {
      $confirm({
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        content: this.$t('i18n_3c9eeee356'),
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
        onOk: () => {
          return deleteLog({
            machineId: this.machineId,
            path: this.temp.path
          }).then((res) => {
            if (res.code === 200) {
              $notification.success({
                message: res.msg
              })
              this.visible = false
              this.loadData()
            }
          })
        }
      })
    }
  }
}
</script>
<style scoped>
.log-layout {
  padding: 0;
  margin: 0;
}
.log-sider {
  border: 1px solid #e2e2e2;
  overflow-x: auto;
  height: calc(100vh - 110px);
  /* width: max-content; */
}
.log-content {
  /* margin: 0; */
  padding-left: 15px;
  /* background: #fff; */
  /* height: calc(100vh - 130px); */
  overflow-y: auto;
}
</style>
