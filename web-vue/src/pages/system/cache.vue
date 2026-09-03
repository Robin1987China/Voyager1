<template>
  <div>
    <n-tabs default-active-key="1">
      <n-tab-pane name="1" :tab="$t('i18n_3c6248b364')">
        <n-descriptions bordered title="" layout="vertical" size="medium">
          <n-descriptions-item :span="3">
            <template #label>
              <n-grid>
                <n-grid-item :span="12"> {{ $t('i18n_02d9819dda') }} </n-grid-item>
                <n-grid-item :span="12" style="text-align: right">
                  <n-button size="small" text @click="refreshCache"
                    >{{ $t('i18n_96d46bd22e') }}<ReloadOutlined
                  /></n-button>
                </n-grid-item>
              </n-grid>
            </template>
            <div style="color: red; font-weight: bold; font-size: 16px">
              <p>{{ $t('i18n_96b78bfb6a') }}</p>
              <p>{{ $t('i18n_7aaee3201a') }}</p>
            </div>
            <n-tag color="orange">{{ $t('i18n_8b3db55fa4') }}{{ temp.clusterId }}</n-tag>
            <n-tag color="blue">{{ $t('i18n_63e975aa63') }}{{ temp.installId }}</n-tag>
          </n-descriptions-item>
          <n-descriptions-item :label="$t('i18n_f71a30c1b9')" :span="1">
            {{ renderSize(temp.dataSize) }} ({{ $t('i18n_c996a472f7') }})
            <n-tooltip>
              <template #trigger>
                <QuestionCircleOutlined />
              </template>

              <ul>
                <li>{{ $t('i18n_73578c680e') }}</li>
                <li>{{ $t('i18n_a0f1bfad78') }}</li>
              </ul>
            </n-tooltip>
          </n-descriptions-item>
          <n-descriptions-item :label="$t('i18n_c89e9681c7')" :span="1">
            <n-space>
              <span>{{ renderSize(temp.cacheFileSize) }} (10{{ $t('i18n_6af7686e31') }})</span>
              <n-button
                v-if="temp.cacheFileSize !== '0'"
                size="small"
                type="primary"
                class="btn"
                @click="clear('serviceCacheFileSize')"
                >{{ $t('i18n_288f0c404c') }}</n-button
              >
            </n-space>
          </n-descriptions-item>
          <n-descriptions-item :label="$t('i18n_ed19a6eb6f')">
            {{ renderSize(temp.cacheBuildFileSize) }} ({{ $t('i18n_c996a472f7') }})
            <n-tooltip>
              <template #trigger>
                <QuestionCircleOutlined />
              </template>

              <ul>
                <li>{{ $t('i18n_d83aae15b5') }}</li>
              </ul>
            </n-tooltip>
          </n-descriptions-item>

          <n-descriptions-item :label="$t('i18n_0d50838436')" :span="1">
            {{ temp.dataPath }}
          </n-descriptions-item>
          <n-descriptions-item :label="$t('i18n_40f8c95345')" :span="1">
            {{ temp.tempPath }}
          </n-descriptions-item>
          <n-descriptions-item :label="$t('i18n_51d6b830d4')">
            {{ temp.buildPath }}
          </n-descriptions-item>

          <n-descriptions-item :label="$t('i18n_7d23ca925c')" :span="1">
            {{ temp.dateTime }} <n-tag>{{ temp.timeZoneId }}</n-tag>
          </n-descriptions-item>
          <n-descriptions-item :label="$t('i18n_815492fd8d')">
            <n-space>
              <span>{{ renderSize(temp.oldJarsSize) }}</span>
              <n-button
                v-if="temp.oldJarsSize !== '0'"
                size="small"
                type="primary"
                class="btn"
                @click="clear('serviceOldJarsSize')"
                >{{ $t('i18n_288f0c404c') }}</n-button
              >
            </n-space>
          </n-descriptions-item>
          <n-descriptions-item :label="$t('i18n_4d351f3c91')">
            <n-space>
              <n-popover>
                <template #trigger>
                  <span class="tw">
                    {{ (temp.errorIp && temp.errorIp.length) || 0 }}
                    <UnorderedListOutlined />
                  </span>
                </template>
                <template #header>{{ $t('i18n_4d351f3c91') }}</template>
                <n-list size="small" bordered :data="temp.errorIp">
                  <template #renderItem="{ item }">
                    <n-list-item>
                      {{ item.key }} <n-tag>{{ item.obj }}{{ $t('i18n_7229ecc631') }}</n-tag>
                      <n-tag>{{ $t('i18n_8f40b41e89') }}{{ formatDuration(item.ttl, '') }}</n-tag>
                    </n-list-item>
                  </template>
                </n-list>
              </n-popover>
              <n-button
                v-if="temp.errorIp && temp.errorIp.length"
                size="small"
                type="primary"
                class="btn"
                @click="clear('serviceIpSize')"
                >{{ $t('i18n_288f0c404c') }}</n-button
              >
            </n-space>
          </n-descriptions-item>
          <n-descriptions-item :label="$t('i18n_8f0bab9a5a')">
            {{ temp.readFileOnLineCount }}
          </n-descriptions-item>
          <n-descriptions-item :label="$t('i18n_c5099cadcd')">
            {{ temp.pluginSize || 0 }}
          </n-descriptions-item>
          <n-descriptions-item :label="$t('i18n_1cc82866a4')">
            {{ temp.shardingSize }}
          </n-descriptions-item>
          <n-descriptions-item :label="$t('i18n_9adf43e181')">
            <n-popover>
              <template #trigger>
                <span class="tw">
                  <span class="tw">
                    <n-space>
                      <span>{{ (temp.buildKeys || []).length }}</span>
                      <UnorderedListOutlined />
                    </n-space>
                  </span>
                </span>
              </template>
              <template #header>{{ $t('i18n_853d8ab485') }}</template>

              <p v-for="item in temp.buildKeys || []" :key="item">{{ item }}</p>
            </n-popover>
          </n-descriptions-item>
          <n-descriptions-item :label="$t('i18n_31ac8d3a5d')">
            <n-popover>
              <template #trigger>
                <span class="tw">
                  <span class="tw">
                    <n-space>
                      <span>{{ (temp.syncFinisKeys || []).length }}</span>
                      <UnorderedListOutlined />
                    </n-space>
                  </span>
                </span>
              </template>
              <template #header>{{ $t('i18n_3a6000f345') }}</template>

              <p v-for="item in temp.syncFinisKeys || []" :key="item">{{ item }}</p>
            </n-popover>
          </n-descriptions-item>
          <n-descriptions-item :label="$t('i18n_87dec8f11e')">
            <n-popover>
              <template #trigger>
                <span class="tw">
                  <span class="tw">
                    <n-space>
                      <span>{{ Object.keys(temp.errorWorkspace || {}).length }}</span>
                      <UnorderedListOutlined />
                    </n-space>
                  </span>
                </span>
              </template>
              <template #header>{{ $t('i18n_87dec8f11e') }}</template>

              <n-collapse>
                <n-collapse-item v-for="(item, key) in temp.errorWorkspace" :key="key" :header="key">
                  <p v-for="(item2, index) in item" :key="index">{{ item2 }}</p>
                  <template #extra>
                    <DeleteOutlined
                      @click="
                        (e) => {
                          handleClearErrorWorkspaceClick(e, key)
                        }
                      "
                    />
                  </template>
                </n-collapse-item>
              </n-collapse>
            </n-popover>
          </n-descriptions-item>
        </n-descriptions>
        <!-- <n-timeline>
          <n-timeline-item> </n-timeline-item>
          <n-timeline-item> </n-timeline-item>
        </n-timeline> -->
      </n-tab-pane>
      <n-tab-pane name="2" :tab="$t('i18n_98e115d868')" force-render>
        <task-stat :task-list="taskList" @refresh="loadData" />
      </n-tab-pane>
      <n-tab-pane name="3" :tab="$t('i18n_43250dc692')">
        <TriggerToken />
      </n-tab-pane>
    </n-tabs>
  </div>
</template>
<script>
import { DeleteOutlined, QuestionCircleOutlined, ReloadOutlined, UnorderedListOutlined } from '@ant-design/icons-vue'

import { getServerCache, clearCache, clearErrorWorkspace, asyncRefreshCache } from '@/api/system'
import TaskStat from '@/pages/system/taskStat'
import TriggerToken from '@/pages/system/trigger-token'
import { renderSize, formatDuration } from '@/utils/const'
export default {
  components: {
    TaskStat,
    TriggerToken
  },
  data() {
    return {
      temp: {},
      taskList: []
    }
  },
  mounted() {
    this.loadData()
    // console.log(Comparator);
  },
  methods: {
    renderSize,
    formatDuration,
    // load data
    loadData() {
      getServerCache().then((res) => {
        if (res.code === 200) {
          this.temp = res.data
          this.taskList = res.data?.taskList
        }
      })
    },
    refreshCache() {
      asyncRefreshCache().then((res) => {
        if (res.code === 200) {
          // 成功
          $notification.success({
            message: res.msg
          })
        }
      })
    },
    // clear
    clear(type) {
      const params = {
        type: type,
        nodeId: ''
      }
      clearCache(params).then((res) => {
        if (res.code === 200) {
          // 成功
          $notification.success({
            message: res.msg
          })
          this.loadData()
        }
      })
    },
    handleClearErrorWorkspaceClick(event, tableName) {
      // If you don't want click extra trigger collapse, you can prevent this:
      event.stopPropagation()
      $confirm({
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        content: this.$t('i18n_c9b0f8e8c8') + tableName + this.$t('i18n_bbcaac136c'),
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
        onOk: () => {
          return clearErrorWorkspace({ tableName }).then((res) => {
            if (res.code === 200) {
              // 成功
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
