<template>
  <div>
    <n-timeline>
      <n-timeline-item>
        <span
          >{{ $t('i18n_231f655e35') }}{{ temp.timeStamp }}
          <n-tag v-if="nodeId || machineId">agent</n-tag>
          <n-tag v-else>server</n-tag>
        </span>
      </n-timeline-item>
      <n-timeline-item>
        <span>{{ $t('i18n_e60389f6d6') }}{{ temp.vueTimeStamp }}</span>
      </n-timeline-item>
      <n-timeline-item v-if="!nodeId && !machineId">
        <span>{{ $t('i18n_bdc1fdde6c') }}</span>
        <n-space>
          <n-switch
            v-model:value="temp.joinBetaRelease"
            :checked-label="$t('i18n_c8a2447aa9')"
            :unchecked-label="$t('i18n_ae17005c0c')"
            :disabled="true"
          />
          <template v-if="temp.joinBetaRelease">
            <n-button text @click="handleChangeBetaRelease(false)">{{ $t('i18n_8ef0f6c275') }}</n-button>
          </template>
          <template v-else>
            <n-tooltip>
              <template #trigger>
                <span class="tw">
                  <n-button text @click="handleChangeBetaRelease(true)"
                    ><QuestionCircleOutlined />{{ $t('i18n_d17eac5b5e') }}</n-button
                  >
                </span>
              </template>
              {{ $t('i18n_31bca0fc93') }}
            </n-tooltip>
          </template>
        </n-space>
      </n-timeline-item>
      <n-timeline-item>
        <n-space>
          <span>{{ $t('i18n_07683555af') }}{{ temp.version }} </span>
          <template v-if="temp.upgrade !== undefined">
            <n-tag v-if="temp.upgrade" color="pink" @click="upgrageVerion"
              >{{ $t('i18n_ac2f4259f1') }}{{ temp.newVersion }} {{ temp.newBeta ? '/beta' : '' }}
              <DownloadOutlined />
            </n-tag>
            <n-tag v-else color="orange" @click="checkVersion">
              <RocketOutlined />
            </n-tag>
          </template>
        </n-space>
      </n-timeline-item>
      <n-timeline-item v-if="temp.downloadSource">
        <n-space>
          <span>{{ $t('i18n_3200fba1c6') }}{{ temp.downloadSource }} </span>
          <div v-if="!nodeId && !machineId">
            <n-input
              v-model:value="temp.auth"
              type="password"
              :placeholder="$t('i18n_a55ae13421')"
              @press-enter="changeDownloadAuth"
            >
              <template #prefix>{{ $t('i18n_31353ecf96') }}</template>
            </n-input>
          </div>
        </n-space>
      </n-timeline-item>
      <n-timeline-item>
        <span>{{ $t('i18n_b57ecea951') }}{{ formatDuration(temp.upTime) }}</span>
      </n-timeline-item>
      <n-timeline-item>
        <span>
          {{ $t('i18n_4c096c51a3') }}<n-tag>{{ temp.port }}</n-tag>
        </span>
        <span>&nbsp;&nbsp;</span>
        <span>
          {{ $t('i18n_2b04210d33') }}<n-tag>{{ temp.pid }}</n-tag>
        </span>
      </n-timeline-item>
      <n-timeline-item>
        <n-alert :title="$t('i18n_5785f004ea')" type="warning" show-icon />
      </n-timeline-item>
      <n-timeline-item>
        <span>
          {{ $t('i18n_a9add9b059') }}<n-tag>{{ temp.dataPath }}</n-tag>
        </span>
        <span v-if="temp.jarFile">
          {{ $t('i18n_9ce5d5202a') }}<n-tag>{{ temp.jarFile }}</n-tag>
        </span>
      </n-timeline-item>
    </n-timeline>

    <n-grid>
      <n-grid-item span="22">
        <n-space direction="vertical" style="width: 100%">
          <n-upload
            v-model:file-list="fileList"
            :disabled="!!percentage"
            :custom-request="beforeUpload"
            accept=".jar,.zip"
            @remove="handleRemove"
          >
            <LoadingOutlined v-if="percentage" />
            <n-button v-else><UploadOutlined />{{ $t('i18n_d615ea8e30') }}</n-button>
          </n-upload>
          <n-grid v-if="percentage">
            <n-grid-item span="20">
              <n-progress :percent="percentage" style="width: 100%"></n-progress>
            </n-grid-item>
          </n-grid>
          <n-button type="primary" :disabled="fileList.length === 0 || !!percentage" @click="startUpload">{{
            $t('i18n_3dd6c10ffd')
          }}</n-button>
        </n-space>
      </n-grid-item>
    </n-grid>
  </div>
</template>
<script>
import {
  DownloadOutlined,
  LoadingOutlined,
  QuestionCircleOutlined,
  RocketOutlined,
  UploadOutlined
} from '@ant-design/icons-vue'

import {
  systemInfo,
  uploadUpgradeFile,
  checkVersion,
  remoteUpgrade,
  uploadUpgradeFileMerge,
  changBetaRelease,
  changeDownloadAuth
} from '@/api/system'
import { useGuideStore } from '@/stores/guide'

import {
  RESTART_UPGRADE_WAIT_TIME_COUNT,
  parseTime,
  compareVersion,
  pageBuildInfo,
  formatDuration
} from '@/utils/const'
import { uploadPieces } from '@/utils/upload-pieces'
import { executionRequest } from '@/api/external'

export default {
  name: 'Upgrade',
  components: {
    // MarkdownItVue
  },
  inject: ['globalLoading'],
  props: {
    nodeId: {
      type: String,
      default: ''
    },
    machineId: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      temp: {},

      checkCount: 0,
      fileList: [],
      percentage: 0
    }
  },
  computed: {
    ...mapState(useGuideStore, ['getThemeStyle'])
  },
  mounted() {
    this.loadData()
  },
  beforeUnmount() {},
  methods: {
    uploadPieces,
    formatDuration,
    // 加载数据
    loadData() {
      systemInfo({
        nodeId: this.nodeId,
        machineId: this.machineId
      }).then((res) => {
        this.temp = res.data?.manifest
        //
        // vueTimeStamp
        this.temp = {
          ...this.temp,
          vueTimeStamp: parseTime(this.getMeta('build-time')),
          joinBetaRelease: res.data?.joinBetaRelease,
          auth: res.data?.auth || ''
        }
        this.showVersion(false, res.data?.remoteVersion).then((upgrade) => {
          // 本地网络检测
          this.localCheckVersion(!upgrade)
        })
      })
    },
    getMeta(metaName) {
      const metas = document.getElementsByTagName('meta')
      for (let i = 0; i < metas.length; i++) {
        try {
          if (metas[i].getAttribute('name') === metaName) {
            return metas[i].getAttribute('content')
          }
        } catch (e) {
          console.error(e)
        }
      }
      return ''
    },
    // 处理文件移除
    handleRemove(file) {
      const index = this.fileList.indexOf(file)
      const newFileList = this.fileList.slice()
      newFileList.splice(index, 1)
      this.fileList = newFileList
      return true
    },
    // 准备上传文件
    beforeUpload({ file, onFinish, onError }) {
      // 只允许上传单个文件
      this.fileList = [file]
      return false
    },
    // 开始上传文件
    startUpload() {
      const title = this.$t('i18n_458331a965')
      const alterB = this.$t('i18n_ddf0c97bce')
      const liArray = [this.$t('i18n_a38ed189a2'), this.$t('i18n_a5daa9be44'), this.$t('i18n_a52a10123f')]

      const html = `${title}
      <ul style="color:red;">
        <li>${liArray[0]}<b>${alterB}</b></li>
        <li>${liArray[1]}</li>
        <li>${liArray[2]}</li>
        </ul>
      `

      $confirm({
        title: this.$t('i18n_c4535759ee'),
        content: h('div', null, [h('p', { innerHTML: html }, null)]),
        okText: this.$t('i18n_e83a256e4f'),
        zIndex: 1009,
        cancelText: this.$t('i18n_625fb26b4b'),
        onOk: () => {
          const file = this.fileList[0]
          this.percentage = 0
          uploadPieces({
            file,
            resolveFileProcess: (msg) => {
              this.globalLoading({
                spinning: true,
                tip: msg
              })
            },
            resolveFileEnd: () => {
              this.globalLoading(false)
            },
            process: (process) => {
              this.percentage = Math.max(this.percentage, process)
            },
            success: (uploadData) => {
              // 准备合并
              uploadUpgradeFileMerge({
                ...uploadData[0],
                nodeId: this.nodeId,
                machineId: this.machineId
              })
                .then((res) => {
                  if (res.code === 200) {
                    this.fileList = []
                    this.startCheckUpgradeStatus(res.msg)
                  }
                })
                .finally(() => {
                  setTimeout(() => {
                    this.percentage = 0
                  }, 2000)
                })
            },
            error: (msg) => {
              $notification.error({
                message: msg
              })
            },
            uploadCallback: (formData) => {
              return new Promise((resolve, reject) => {
                formData.append('nodeId', this.nodeId)
                formData.append('machineId', this.machineId)
                // 上传文件
                uploadUpgradeFile(formData)
                  .then((res) => {
                    if (res.code === 200) {
                      resolve()
                    } else {
                      reject()
                    }
                  })
                  .catch(() => {
                    reject()
                  })
              })
            }
          })
          // const formData = new FormData();
          // formData.append("file", this.fileList[0]);

          // // 上传文件
          // uploadUpgradeFile(formData).then((res) => {
          //   if (res.code === 200) {
          //     $notification.success({
          //       message: res.msg,
          //     });

          //     this.startCheckUpgradeStatus(res.msg);
          //   }
          // });
          // this.fileList = [];
        }
      })
    },
    startCheckUpgradeStatus(msg) {
      this.checkCount = 0

      this.globalLoading({
        spinning: true,
        tip: (msg || this.$t('i18n_589060f38e')) + this.$t('i18n_9ba71275d3')
      })
      //
      this.timer = setInterval(() => {
        systemInfo({
          nodeId: this.nodeId,
          machineId: this.machineId
        })
          .then((res) => {
            let manifest = res.data?.manifest
            if (res.code === 200 && manifest?.timeStamp !== this.temp.timeStamp) {
              this.globalLoading({
                spinning: false
              })
              clearInterval(this.timer)
              $notification.success({
                message: this.$t('i18n_e64d788d11')
              })
              this.temp = manifest
              setTimeout(() => {
                location.reload()
              }, 1000)
            } else {
              if (this.checkCount > RESTART_UPGRADE_WAIT_TIME_COUNT) {
                $notification.warning({
                  message: this.$t('i18n_2a38b6c0ae') + (res.msg || '')
                })
                this.globalLoading({
                  spinning: false
                })
                clearInterval(this.timer)
              }
            }
          })
          .catch((error) => {
            console.error(error)
            if (this.checkCount > RESTART_UPGRADE_WAIT_TIME_COUNT) {
              this.globalLoading({
                spinning: false
              })
              clearInterval(this.timer)
              $notification.error({
                message: this.$t('i18n_2191afee6e')
              })
            } else {
              this.globalLoading({
                spinning: true,
                tip: (msg || this.$t('i18n_589060f38e')) + this.$t('i18n_9ba71275d3')
              })
            }
          })
        this.checkCount = this.checkCount + 1
      }, 2000)
    },
    // 检查新版本
    checkVersion() {
      checkVersion({
        nodeId: this.nodeId,
        machineId: this.machineId
      }).then((res) => {
        if (res.code === 200) {
          this.showVersion(true, res.data).then((upgrade) => {
            // 远程检测失败才本地检测
            if (!upgrade) {
              this.localCheckVersion(true)
            }
          })
        }
      })
    },
    // 本地网络检测
    localCheckVersion(tip) {
      //console.log(compareVersion("1.0.0", "1.0.1"), compareVersion("2.4.3", "2.4.2"));
      //console.log(compareVersion("1.0.2", "dev"));
      const buildInfo = pageBuildInfo()

      const url = this.temp?.joinBetaRelease
        ? 'https://voyager1.top/docs/beta-versions.json'
        : 'https://voyager1.top/docs/release-versions.json'

      executionRequest(url, {
        ...buildInfo,
        type: this.nodeId || this.machineId ? 'agent' : 'server'
      }).then((data) => {
        if (!data || !data.tag_name) {
          return
        }

        const tagName = data.tag_name.replace('v', '')
        const upgrade = compareVersion(this.temp.version, tagName) < 0

        if (upgrade && tip) {
          //
          const dUrl = data.downloadUrl || 'https://voyager1.top'
          const htmlAref = `<a href='${dUrl}' target='_blank'>${dUrl}</a>`
          const title = this.$t('i18n_2314f99795')
          const tip = this.$t('i18n_ab3615a5ad')
          const html = `${title} ${tagName} ${htmlAref} ${tip}`

          $notification.success({
            duration: 10,
            message: h('div', null, [h('p', { innerHTML: html }, null)])
          })
        }
      })
    },
    showVersion(tip, data) {
      return new Promise((resolve) => {
        if (!data) {
          this.temp = { ...this.temp, upgrade: false }
          if (tip) {
            $notification.success({
              message: this.$t('i18n_e6cde5a4bc')
            })
          }
          resolve(false)
          return
        }
        this.temp = {
          ...this.temp,
          upgrade: data.upgrade,
          newVersion: data.tagName,
          newBeta: data.beta,
          downloadSource: data.downloadSource
        }

        if (tip) {
          $notification.success({
            message: this.temp.upgrade ? this.$t('i18n_2314f99795') + data.tagName : this.$t('i18n_e6cde5a4bc')
          })
        }
        resolve(data.upgrade)
      })
    },
    // 升级
    upgrageVerion() {
      const title = this.$t('i18n_ec6e39a177')
      const alterB = this.$t('i18n_ddf0c97bce')
      const li = [this.$t('i18n_a94feac256'), this.$t('i18n_b55f286cba'), this.$t('i18n_a52a10123f')]
      const downloadSource = this.$t('i18n_917381e4a5')
      const html = `${title}
      <ul style="color:red;">
        <li style="display: ${this.temp.downloadSource ? 'revert' : 'none'};">${downloadSource}<b>${
          this.temp.downloadSource
        }</b></li>
        <li>${li[0]}</li>
        <li>${li[1]}<b>${alterB}</b></li>
        <li>${li[2]}</li>
      </ul>
      `
      $confirm({
        title: this.$t('i18n_c4535759ee'),
        content: h('div', null, [h('p', { innerHTML: html }, null)]),
        okText: this.$t('i18n_e83a256e4f'),
        zIndex: 1009,
        cancelText: this.$t('i18n_625fb26b4b'),
        onOk: () => {
          return remoteUpgrade({
            nodeId: this.nodeId,
            machineId: this.machineId
          }).then((res) => {
            if (res.code === 200) {
              $notification.success({
                message: res.msg
              })
              this.startCheckUpgradeStatus(res.msg)
            }
          })
        }
      })
    },
    // components.upgrade.index.e17b386beta计划
    handleChangeBetaRelease(beta) {
      let html
      if (beta) {
        const title = this.$t('i18n_d64cf79bd4')
        const li = [this.$t('i18n_d31d625029'), this.$t('i18n_d8db440b83'), this.$t('i18n_73b7e8e09e')]

        html = `${title}
        <ul <ul style="color:red;">
          <li><b>${li[0]}</b></li>
          <li><b>${li[1]}</b></li>
          <li>${li[2]}</li>
        </ul>
        `
      } else {
        html = this.$t('i18n_87659a4953')
      }

      $confirm({
        title: this.$t('i18n_c4535759ee'),
        content: h('div', {}, [h('p', { innerHTML: html })]),
        okText: this.$t('i18n_e83a256e4f'),
        zIndex: 1009,
        cancelText: this.$t('i18n_625fb26b4b'),
        onOk: () => {
          return changBetaRelease({
            beta: beta
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
    },
    changeDownloadAuth() {
      $confirm({
        title: this.$t('i18n_c4535759ee'),
        content: this.$t('i18n_d2913cea31'),

        okText: this.$t('i18n_e83a256e4f'),
        zIndex: 1009,
        cancelText: this.$t('i18n_625fb26b4b'),
        onOk: () => {
          return changeDownloadAuth({
            auth: this.temp.auth
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
