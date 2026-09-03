<template>
  <div>
    <n-tabs v-model:value="tabKey">
      <n-tab-pane :key="1" :tab="$t('i18n_cda84be2f6')">
        <!-- 数据表格 -->
        <n-data-table
          size="medium"
          :data="operatelist"
          :columns="operatecolumns"
          :pagination="operatecpagination"
          bordered
          :row-key="(row) => row.id"
          @change="
            (pagination, filters, sorter) => {
              operatelistQuery = CHANGE_PAGE(operatelistQuery, {
                pagination,
                sorter
              })
              operaterloadData()
            }
          "
        >
          <template #title>
            <n-space>
              <n-select
                v-model:value="operatelistQuery.classFeature"
                filterable
                clearable
                :placeholder="$t('i18n_8432a98819')"
                class="search-input-item"
                :options="classFeature.map((item) => ({ label: item.title, value: item.value }))"
              />
              <n-select
                v-model:value="operatelistQuery.methodFeature"
                filterable
                clearable
                :placeholder="$t('i18n_a9de52acb0')"
                class="search-input-item"
                :options="methodFeature.map((item) => ({ label: item.title, value: item.value }))"
              />
              <n-date-picker
                type="datetimerange"
                format="yyyy-MM-dd HH:mm:ss"
                clearable
                @update:value="
                  (value, dateString) => {
                    operatelistQuery.createTimeMillis = `${dateString[0]} ~ ${dateString[1]}`
                  }
                "
              />
              <n-tooltip>
                <template #trigger>
                  <span class="tw">
                    <n-button type="primary" :loading="operateloading" @click="operaterloadData">{{
                      $t('i18n_e5f71fc31e')
                    }}</n-button>
                  </span>
                </template>
                $t('i18n_4838a3bd20')
              </n-tooltip>
            </n-space>
          </template>
          <template #bodyCell="{ column, text }">
            <template v-if="column.dataIndex === 'classFeature'">
              <n-tooltip placement="topLeft">
                <template #trigger>
                  <span class="tw">
                    <span class="tw">
                      <span>{{ classFeatureMap[text] }}</span>
                    </span>
                  </span>
                </template>
                classFeatureMap[text]
              </n-tooltip>
            </template>
            <template v-else-if="column.dataIndex === 'methodFeature'">
              <n-tooltip placement="topLeft">
                <template #trigger>
                  <span class="tw">
                    <span class="tw">
                      <span>{{ methodFeatureMap[text] }}</span>
                    </span>
                  </span>
                </template>
                methodFeatureMap[text]
              </n-tooltip>
            </template>

            <template v-else-if="column.dataIndex === 'username'">
              <n-tooltip placement="topLeft">
                <template #trigger>
                  <span class="tw">
                    <span class="tw">
                      <span>{{ text || item.userId }}</span>
                    </span>
                  </span>
                </template>
                text
              </n-tooltip>
            </template>

            <template v-else-if="column.dataIndex === 'optStatus'">
              <n-tooltip placement="topLeft">
                <template #trigger>
                  <span class="tw">
                    <span class="tw">
                      <span>{{ text }}</span>
                    </span>
                  </span>
                </template>
                `${$t('i18n_be4b9241ec')},${$t('i18n_69056f4792')},${$t('i18n_27b36afd36')}`
              </n-tooltip>
            </template>
            <template v-else-if="column.tooltip">
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
          </template>
        </n-data-table>
      </n-tab-pane>
      <n-tab-pane :key="2" :tab="$t('i18n_3fb2e5ec7b')">
        <n-data-table
          size="medium"
          :data="loginlist"
          :columns="logincolumns"
          :pagination="loginpagination"
          bordered
          :row-key="(row) => row.id"
          @change="
            (pagination, filters, sorter) => {
              loginlistQuery = CHANGE_PAGE(loginlistQuery, {
                pagination,
                sorter
              })
              loginloadData()
            }
          "
        >
          <template #title>
            <n-space>
              <n-input
                v-model:value="loginlistQuery['%username%']"
                :placeholder="$t('i18n_819767ada1')"
                class="search-input-item"
                @press-enter="loginloadData"
              />
              <n-input
                v-model:value="loginlistQuery['%ip%']"
                :placeholder="$t('i18n_b38d6077d6')"
                class="search-input-item"
                @press-enter="loginloadData"
              />
              <n-date-picker
                type="datetimerange"
                format="yyyy-MM-dd HH:mm:ss"
                clearable
                @update:value="
                  (value, dateString) => {
                    loginlistQuery.createTimeMillis = `${dateString[0]} ~ ${dateString[1]}`
                  }
                "
              />
              <n-tooltip>
                <template #trigger>
                  <span class="tw">
                    <n-button type="primary" :loading="loginloading" @click="loginloadData">{{
                      $t('i18n_e5f71fc31e')
                    }}</n-button>
                  </span>
                </template>
                $t('i18n_4838a3bd20')
              </n-tooltip>
            </n-space>
          </template>
          <template #bodyCell="{ column, text }">
            <template v-if="column.dataIndex === 'success'">
              <n-tooltip placement="topLeft">
                <template #trigger>
                  <span class="tw">
                    <n-tag v-if="text" color="green">{{ $t('i18n_330363dfc5') }}</n-tag>
                    <n-tag v-else color="pink">{{ $t('i18n_acd5cb847a') }}</n-tag>
                  </span>
                </template>
                text ? $t('i18n_330363dfc5') : $t('i18n_acd5cb847a')
              </n-tooltip>
            </template>
            <template v-else-if="column.tooltip">
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

            <template v-else-if="column.dataIndex === 'operateCode'">
              <n-tooltip placement="topLeft">
                <template #trigger>
                  {{ operateCode[text] || $t('i18n_1622dc9b6b') }}
                </template>
                operateCode[text] || $t('i18n_1622dc9b6b')
              </n-tooltip>
            </template>
          </template>
        </n-data-table>
      </n-tab-pane>
    </n-tabs>
  </div>
</template>
<script>
import { CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY, parseTime } from '@/utils/const'
import { listOperaterLog, listLoginLog } from '@/api/user/user'
import { getMonitorOperateTypeList } from '@/api/monitor'
import { operateCodeMap } from '@/api/user/user-login-log'
export default {
  props: {
    openTab: {
      type: Number,
      default: 1
    }
  },
  data() {
    return {
      operateloading: false,
      operatelist: [],
      operatelistQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      methodFeature: [],
      classFeature: [],
      methodFeatureMap: {},
      classFeatureMap: {},
      operatecolumns: [
        {
          title: this.$t('i18n_6b0bc6432d'),
          key: 'username',
          ellipsis: true
        },
        { title: 'IP', key: 'ip', ellipsis: true, width: '130px' },
        {
          title: this.$t('i18n_3bf3c0a8d6'),
          key: 'nodeId',
          width: 120,
          ellipsis: true
        },
        {
          title: this.$t('i18n_5a1419b7a2'),
          key: 'dataName',
          /*width: 240,*/
          ellipsis: true
        },
        {
          title: this.$t('i18n_4524ed750d'),
          key: 'workspaceName',
          /*width: 240,*/
          ellipsis: true
        },
        // { title: "数据 ID", key: "dataId", /*width: 240,*/ ellipsis: true,},
        {
          title: this.$t('i18n_8432a98819'),
          key: 'classFeature',
          /*width: 240,*/
          ellipsis: true
        },
        {
          title: this.$t('i18n_a9de52acb0'),
          key: 'methodFeature',
          /*width: 240,*/
          ellipsis: true
        },
        {
          title: this.$t('i18n_771d897d9a'),
          key: 'optStatus',
          width: 90
        },
        {
          title: this.$t('i18n_7e951d56d9'),
          key: 'createTimeMillis',
          sorter: true,
          customRender: ({ text, item }) => {
            return parseTime(text || item.optTime)
          },
          width: '170px'
        }
      ],

      loginloading: false,
      loginlist: [],
      operateCode: operateCodeMap,
      loginlistQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      logincolumns: [
        {
          title: this.$t('i18n_30acd20d6e'),
          key: 'modifyUser',
          ellipsis: true
        },
        {
          title: this.$t('i18n_dfb8d511c7'),
          key: 'username',
          ellipsis: true
        },
        {
          title: 'IP',
          key: 'ip',
          ellipsis: true
        },
        {
          title: this.$t('i18n_912302cb02'),
          key: 'userAgent',
          ellipsis: true
        },
        {
          title: this.$t('i18n_5e9f2dedca'),
          key: 'success',
          ellipsis: true,
          width: '100px'
        },
        {
          title: this.$t('i18n_64c083c0a9'),
          key: 'operateCode',
          /*width: 240,*/ ellipsis: true
        },

        {
          title: this.$t('i18n_9fca7c455f'),
          key: 'createTimeMillis',
          sorter: true,
          customRender: ({ text, item }) => {
            return parseTime(text || item.optTime)
          },
          width: '170px'
        }
      ],

      tabKey: 1
    }
  },
  computed: {
    operatecpagination() {
      return COMPUTED_PAGINATION(this.operatelistQuery)
    },
    loginpagination() {
      return COMPUTED_PAGINATION(this.loginlistQuery)
    }
  },
  created() {
    if (this.openTab) {
      this.tabKey = this.openTab
    }
    this.operaterloadData()
    this.loginloadData()
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
  },
  methods: {
    CHANGE_PAGE,
    // 加载数据
    operaterloadData(pointerEvent) {
      this.operateloading = true
      this.operatelistQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.operatelistQuery.page
      listOperaterLog(this.operatelistQuery).then((res) => {
        if (res.code === 200) {
          this.operatelist = res.data.result
          this.operatelistQuery.total = res.data.total
        }
        this.operateloading = false
      })
    },
    loginloadData(pointerEvent) {
      this.loginloading = true
      this.loginlistQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.loginlistQuery.page
      listLoginLog(this.loginlistQuery).then((res) => {
        if (res.code === 200) {
          this.loginlist = res.data.result
          this.loginlistQuery.total = res.data.total
        }
        this.loginloading = false
      })
    }
  }
}
</script>
