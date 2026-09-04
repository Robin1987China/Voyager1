<template>
  <div>
    <n-page-header :back-icon="false">
      <!-- 【】\u3010\u3011 -->
      <template #title>
        {{ $t('i18n_50c7929dd9') }}{{ `\u3010` }} {{ getUserInfo.name }} {{ `\u3011` }}{{ $t('i18n_77834eb6f5') }}
      </template>
      <template #subTitle>
        {{ $t('i18n_5195c0d198', { count: (myWorkspaceList && myWorkspaceList.length) || 0 }) }}
      </template>
      <template #tags>
        <n-tag color="blue">
          <template v-if="getUserInfo.demoUser">{{ $t('i18n_20c8dc0346') }}</template>
          <template v-else-if="getUserInfo.superSystemUser">{{ $t('i18n_302ff00ddb') }}</template>
          <template v-else-if="getUserInfo.systemUser">{{ $t('i18n_b1dae9bc5c') }}</template>
          <template v-else>{{ $t('i18n_7527da8954') }}</template>
        </n-tag>
      </template>
      <template #extra>
        <n-tooltip>
          <template #trigger>
            <n-button @click="init">
              <template #icon><ReloadOutlined /></template>
            </n-button>
          </template>
          {{ $t('i18n_498519d1af') }}
        </n-tooltip>
        <!-- // 擅自修改或者删除版权信息有法律风险，请尊重开源协议，不要擅自修改版本信息，否则可能承担法律责任。 -->
        <n-tooltip v-if="getUserInfo && (getUserInfo.systemUser || getUserInfo.demoUser)">
          <template #trigger>
            <QuestionCircleOutlined style="cursor: pointer" />
          </template>
          {{ $t('i18n_e166aa424d') }}
        </n-tooltip>
      </template>
    </n-page-header>
    <n-divider dashed />

    <n-grid :x-gap="[16, 16]">
      <n-grid-item :span="6">
        <n-card size="small">
          <template #title>
            {{ $t('i18n_1b7cba289a') }}
            <n-tooltip>
              <template #trigger>
                <QuestionCircleOutlined />
              </template>
              {{ $t('i18n_fb1f3b5125') }}
            </n-tooltip>
          </template>
          <n-list>
            <n-list-item v-for="item in statNames" :key="item.field">
              {{ item.name }}：{{ statData[item.field] || '-' }}
            </n-list-item>
          </n-list>
        </n-card>
      </n-grid-item>
      <n-grid-item :span="6">
        <n-card size="small">
          <template #title>
            {{ $t('i18n_7c0ee78130') }}
            <n-tooltip>
              <template #trigger>
                <QuestionCircleOutlined />
              </template>
              $t('i18n_031020489f')
            </n-tooltip>
          </template>
          <n-timeline v-if="buildLog && buildLog.length">
            <n-timeline-item v-for="item in buildLog" :key="item.id" :color="statusColor[item.status]">
              <n-space direction="vertical" :size="1">
                <div>
                  {{ parseTime(item.startTime) }} ~
                  {{ parseTime(item.endTime) }}
                </div>

                <n-grid :x-gap="16">
                  <n-grid-item>
                    <span :style="`color: ${statusColor[item.status]};`" @click="handleBuildLog(item)">
                      #{{ item.buildNumberId }}
                    </span>
                  </n-grid-item>
                  <n-grid-item>
                    <span>{{ item.buildName || '-' }}</span>
                  </n-grid-item>
                  <n-grid-item>
                    <n-tooltip>
                      <template #trigger>
                        <span class="tw">
                          <n-tag :color="statusColor[item.status]" @click="handleBuildLog(item)">
                            {{ statusMap[item.status] || $t('i18n_1622dc9b6b') }}
                          </n-tag>
                        </span>
                      </template>
                      item.statusMsg || statusMap[item.status] || $t('i18n_1622dc9b6b')
                    </n-tooltip>
                  </n-grid-item>
                </n-grid>
              </n-space>
            </n-timeline-item>
          </n-timeline>
          <n-empty v-else :description="$t('i18n_a918bde61d')" />
        </n-card>
      </n-grid-item>
      <n-grid-item :span="6">
        <n-card size="small">
          <template #title> {{ $t('i18n_3fb2e5ec7b') }} </template>
          <template #extra>
            <a href="#" @click="handleUserlog(2)">{{ $t('i18n_0ec9eaf9c3') }}</a>
          </template>
          <n-timeline v-if="loginLog && loginLog.length">
            <n-timeline-item v-for="(item, index) in loginLog" :key="index" :color="item.success ? 'green' : 'red'">
              <n-space direction="vertical" :size="1">
                <div>{{ parseTime(item.createTimeMillis) }}</div>
                <n-space>
                  <n-tag> {{ operateCodeMap[item.operateCode] || $t('i18n_1622dc9b6b') }}</n-tag>
                  <span> IP:{{ item.ip }}</span>
                </n-space>
              </n-space>
            </n-timeline-item>
          </n-timeline>
          <n-empty v-else :description="$t('i18n_0aa60d1169')" />
        </n-card>
      </n-grid-item>
      <n-grid-item :span="6">
        <n-card size="small">
          <template #title>
            {{ $t('i18n_cda84be2f6') }}
            <n-tooltip>
              <template #trigger>
                <QuestionCircleOutlined />
              </template>
              $t('i18n_05510a85b0')
            </n-tooltip>
          </template>
          <template #extra>
            <a href="#" @click="handleUserlog(1)">{{ $t('i18n_0ec9eaf9c3') }}</a>
          </template>
          <n-timeline v-if="operateLog && operateLog.length">
            <n-timeline-item
              v-for="(item, index) in operateLog"
              :key="index"
              :color="item.optStatus === 200 ? 'green' : 'red'"
            >
              <n-space direction="vertical" :size="1">
                <div>{{ parseTime(item.createTimeMillis) }}</div>
                <n-space>
                  <n-tag>{{ classFeatureMap[item.classFeature] }}</n-tag>
                  <n-tag>{{ methodFeatureMap[item.methodFeature] }}</n-tag>
                </n-space>
              </n-space>
            </n-timeline-item>
          </n-timeline>
          <n-empty v-else :description="$t('i18n_935b06789f')" />
        </n-card>
      </n-grid-item>
    </n-grid>
    <!-- 查看操作日志 -->
    <CustomModal
      v-if="viewLogVisible > 0"
      destroy-on-close
      :open="viewLogVisible > 0"
      :width="'90vw'"
      :title="$t('i18n_cda84be2f6')"
      :footer="null"
      :mask-closable="false"
      @cancel="viewLogVisible = 0"
    >
      <div>
        <user-log v-if="viewLogVisible > 0" :open-tab="viewLogVisible"></user-log>
      </div>
    </CustomModal>
    <!-- 构建日志 -->
    <build-log v-if="buildLogVisible > 0" :temp="temp" :show="buildLogVisible != 0" @close="buildLogVisible = 0" />
  </div>
</template>
<script>
import { QuestionCircleOutlined, ReloadOutlined } from '@ant-design/icons-vue'

import { myWorkspace, statWorkspace, recentLogData } from '@/api/user/user'
import BuildLog from '@/pages/build/log'
import { parseTime } from '@/utils/const'
import { operateCodeMap } from '@/api/user/user-login-log'
import { getMonitorOperateTypeList } from '@/api/monitor'
import UserLog from './user-log.vue'
import { useUserStore } from '@/stores/user'
import { mapState } from 'pinia'
import { statusMap, statusColor, triggerBuildTypeMap } from '@/api/build-info'
import { NEmpty as Empty } from 'naive-ui'
export default {
  components: {
    UserLog,
    BuildLog,
    QuestionCircleOutlined,
    ReloadOutlined
  },
  data() {
    return {
      Empty,
      triggerBuildTypeMap,
      statusMap,
      statusColor,
      myWorkspaceList: [],
      loginLog: [],
      operateLog: [],
      buildLog: [],
      operateCodeMap,
      methodFeatureMap: {},
      classFeatureMap: {},
      viewLogVisible: 0,
      // "逻辑节点", "节点项目", "节点脚本", "项目分发", "SSH终端", "SSH命令", "本地脚本", "Docker节点", "动态文件", "静态文件"
      statNames: [
        { name: this.$t('i18n_add91bb395'), field: 'nodeCount' },
        { name: this.$t('i18n_5488c40573'), field: 'projectCount' },
        { name: this.$t('i18n_e0ba3b9145'), field: 'nodeScriptCount' },
        { name: this.$t('i18n_429b8dfb98'), field: 'outGivingCount' },
        { name: `SSH${this.$t('i18n_4722bc0c56')}`, field: 'sshCount' },
        { name: `SSH${this.$t('i18n_ba311d8a6a')}`, field: 'sshCommandCount' },
        { name: this.$t('i18n_3eab0eb8a9'), field: 'scriptCount' },
        { name: `Docker${this.$t('i18n_3bf3c0a8d6')}`, field: 'dockerCount' },
        { name: `Docker${this.$t('i18n_85fe5099f6')}`, field: 'dockerSwarmCount' },
        { name: this.$t('i18n_0a056b0d5a'), field: 'fileCount' }
        // { name: "静态文件", field: "staticFileCount" },
      ],
      statData: {},
      temp: {},
      buildLogVisible: 0
    }
  },
  computed: {
    ...mapState(useUserStore, ['getUserInfo'])
  },
  created() {
    this.init()
  },
  methods: {
    parseTime,
    init() {
      // 工作空间
      myWorkspace().then((res) => {
        if (res.code == 200 && res.data) {
          this.myWorkspaceList = res.data
        }
      })
      // 近期操作记录
      recentLogData().then((res) => {
        if (res.code == 200 && res.data) {
          this.operateLog = res.data.operateLog || []
          this.loginLog = res.data.loginLog || []
          this.buildLog = res.data.buildLog || []
        }
      })
      // 操作方法
      getMonitorOperateTypeList().then((res) => {
        this.methodFeature = res.data.methodFeature
        this.classFeature = res.data.classFeature
        res.data.methodFeature.forEach((item) => {
          this.methodFeatureMap[item.value] = item.title
        })
        res.data.classFeature.forEach((item) => {
          this.classFeatureMap[item.value] = item.title
        })
      })
      // 数据统计
      statWorkspace().then((res) => {
        if (res.code === 200 && res.data) {
          this.statData = res.data || {}
        }
      })
    },
    handleUserlog(val) {
      this.viewLogVisible = val
    },
    // 查看构建日志
    handleBuildLog(record) {
      this.temp = {
        id: record.buildDataId,
        buildId: record.buildNumberId
      }
      this.buildLogVisible = new Date() * Math.random()
    }
  }
}
</script>
<style scoped>
:deep(.n-divider:not(.n-divider--vertical)) {
  margin: 5px 0;
}
</style>
