<template>
  <div>
    <n-page-header :back-icon="false">
      <!-- 【】\u3010\u3011 -->
      <template #title>
        {{ $t('i18n_60585cf697') }}{{ `\u3010` }}{{ getUserInfo.name }}{{ `\u3011` }}{{ $t('i18n_20a9290498') }}
      </template>
      <template #subTitle>{{ $t('i18n_0af5d9f8e8') }} </template>
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
            <n-button :loading="loading" @click="init">
              <template #icon><ReloadOutlined /></template>
            </n-button>
          </template>
          {{ $t('i18n_498519d1af') }}
        </n-tooltip>
        <!-- // 擅自修改或者删除版权信息有法律风险，请尊重开源协议，不要擅自修改版本信息，否则可能承担法律责任。 -->
        <n-tooltip>
          <template #trigger>
            <n-button @click="toAbout">
              <template #icon><ExclamationCircleOutlined /></template>
            </n-button>
          </template>
          {{ $t('i18n_e166aa424d') }}
        </n-tooltip>
      </template>
      <n-space>
        <span>
          {{ $t('i18n_fbee13a873') }}
          <n-badge color="blue" :count="statData['workspaceCount'] || '0'" show-zero />
        </span>
        <span
          >{{ $t('i18n_5866b4bced') }}<n-badge color="cyan" :count="statData['clusterCount'] || '0'" show-zero
        /></span>
      </n-space>
    </n-page-header>
    <n-divider dashed />

    <n-grid :x-gap="[16, 16]">
      <n-grid-item :span="6">
        <n-card size="small">
          <template #title> {{ $t('i18n_a6bf763ede') }} </template>

          <n-list>
<template v-for="item in ['all', ...Object.keys(nodeStatusMap)]" :key="item">
<n-list-item v-if="item === 'all'">{{ $t('i18n_ec1f13ff6d')
                }}<n-badge
                  :color="item.color"
                  :count="
                    (statData.nodeStat &&
                      statData.nodeStat.reduce(function (sum, item2) {
                        return sum + Number(item2.count)
                      }, 0)) ||
                    '0'
                  "
                  show-zero
                /></n-list-item>
<n-list-item v-else>{{ nodeStatusMap[item] }}：<n-badge
                  :color="Number(item) === 1 ? 'green' : ''"
                  :count="
                    (statData.nodeStat &&
                      statData.nodeStat.find((item2) => {
                        return item2.status === Number(item)
                      }) &&
                      statData.nodeStat.find((item2) => {
                        return item2.status === Number(item)
                      }).count) ||
                    '0'
                  "
                  show-zero
                /></n-list-item>
</template>
</n-list>
        </n-card>
      </n-grid-item>
      <n-grid-item :span="6">
        <n-card size="small">
          <template #title> {{ $t('i18n_4ad6e58ebc') }} </template>

          <n-list>
<template v-for="item in ['all', ...Object.keys(sshStatusMap)]" :key="item">
<n-list-item v-if="item === 'all'">{{ $t('i18n_ec1f13ff6d')
                }}<n-badge
                  :color="item.color"
                  :count="
                    (statData.sshStat &&
                      statData.sshStat.reduce(function (sum, item2) {
                        return sum + Number(item2.count)
                      }, 0)) ||
                    '0'
                  "
                  show-zero
                /></n-list-item>
<n-list-item v-else>{{ sshStatusMap[item].desc }}：<n-badge
                  :color="sshStatusMap[item].color"
                  :count="
                    (statData.sshStat &&
                      statData.sshStat.find((item2) => {
                        return item2.status === Number(item)
                      }) &&
                      statData.sshStat.find((item2) => {
                        return item2.status === Number(item)
                      }).count) ||
                    '0'
                  "
                  show-zero
                /></n-list-item>
</template>
</n-list>
        </n-card>
      </n-grid-item>
      <n-grid-item :span="6">
        <n-card size="small">
          <template #title> {{ $t('i18n_ea58a20cda') }} </template>

          <n-list>
<template v-for="item in ['all', ...Object.keys(dockerStatusMap)]" :key="item">
<n-list-item v-if="item === 'all'">{{ $t('i18n_ec1f13ff6d')
                }}<n-badge
                  :color="item.color"
                  :count="
                    (statData.dockerStat &&
                      statData.dockerStat.reduce(function (sum, item2) {
                        return sum + Number(item2.count)
                      }, 0)) ||
                    '0'
                  "
                  show-zero
                /></n-list-item>
<n-list-item v-else>{{ dockerStatusMap[item].desc }}：<n-badge
                  :color="dockerStatusMap[item].color"
                  :count="
                    (statData.dockerStat &&
                      statData.dockerStat.find((item2) => {
                        return item2.status === Number(item)
                      }) &&
                      statData.dockerStat.find((item2) => {
                        return item2.status === Number(item)
                      }).count) ||
                    '0'
                  "
                  show-zero
                /></n-list-item>
</template>
</n-list>
        </n-card>
      </n-grid-item>
      <n-grid-item :span="6">
        <n-card size="small">
          <template #title> {{ $t('i18n_0da9b12963') }} </template>

          <n-list>
            <n-list-item
              v-for="item in [
                { name: $t('i18n_1149274cbd'), field: 'userCount', color: 'red' },
                { name: $t('i18n_a76b4f5000'), field: 'systemUserCount', color: 'pink' },
                { name: $t('i18n_c03465ca03'), field: 'disableUserCount', color: 'yellow' }
              ]"
              :key="item.field"
            >
              {{ item.name }}：<n-badge :color="item.color" :count="statData[item.field] || '0'" show-zero />
            </n-list-item>
          </n-list>
        </n-card>
      </n-grid-item>
    </n-grid>
  </div>
</template>
<script>
import { ExclamationCircleOutlined, ReloadOutlined } from '@ant-design/icons-vue'

import { statSystemOverview } from '@/api/user/user'

import { statusMap as nodeStatusMap } from '@/api/system/assets-machine'
import { statusMap as sshStatusMap } from '@/api/system/assets-ssh'
import { statusMap as dockerStatusMap } from '@/api/system/assets-docker'
import { useUserStore } from '@/stores/user'
import { mapState } from 'pinia'
import { NEmpty as Empty } from 'naive-ui'

export default {
  components: {},
  data() {
    return {
      Empty,
      dockerStatusMap,
      nodeStatusMap,
      sshStatusMap,
      loading: true,
      statData: {}
    }
  },
  computed: {
    ...mapState(useUserStore, ['getUserInfo'])
  },
  created() {
    this.init()
  },
  methods: {
    init() {
      // 数据
      this.loading = true
      statSystemOverview()
        .then((res) => {
          if (res.code == 200 && res.data) {
            this.statData = res.data || {}
          }
        })
        .finally(() => {
          this.loading = false
        })
    },
    toAbout() {
      this.$router.push({
        path: '/about'
      })
    }
  }
}
</script>
<style scoped>
:deep(.n-divider:not(.n-divider--vertical)) {
  margin: 5px 0;
}
</style>
