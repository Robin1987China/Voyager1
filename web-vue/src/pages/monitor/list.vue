<template>
  <div>
    <!-- 数据表格 -->
    <CustomTable
      is-show-tools
      default-auto-refresh
      :auto-refresh-time="30"
      table-name="monitor-list"
      :active-page="activePage"
      :data="list"
      size="medium"
      :columns="columns"
      :pagination="pagination"
      bordered
      :scroll="{
        x: 'max-content'
      }"
      @update:value="changePage"
      @refresh="loadData"
    >
      <template #title>
        <n-space wrap class="search-box">
          <n-input
            v-model:value="listQuery['%name%']"
            :placeholder="$t('i18n_f976e8fcf4')"
            class="search-input-item"
            @press-enter="loadData"
          />
          <n-select
            v-model:value="listQuery.status"
            clearable
            :placeholder="$t('i18n_a4f5cae8d2')"
            class="search-input-item"
            :options="[
              { label: $t('i18n_cc42dd3170'), value: 1 },
              { label: $t('i18n_b15d91274e'), value: 0 }
            ]"
          />
          <n-select
            v-model:value="listQuery.autoRestart"
            clearable
            :placeholder="$t('i18n_75528c19c7')"
            class="search-input-item"
            :options="[
              { label: $t('i18n_0a60ac8f02'), value: 1 },
              { label: $t('i18n_c9744f45e7'), value: 0 }
            ]"
          />
          <n-select
            v-model:value="listQuery.alarm"
            clearable
            :placeholder="$t('i18n_db4470d98d')"
            class="search-input-item"
            :options="[
              { label: $t('i18n_11957d12e4'), value: 1 },
              { label: $t('i18n_bb667fdb2a'), value: 0 }
            ]"
          />
          <n-tooltip>
            <template #trigger>
              <span class="tw">
                <n-button type="primary" :loading="loading" @click="loadData">{{ $t('i18n_e5f71fc31e') }}</n-button>
              </span>
            </template>
            $t('i18n_4838a3bd20')
          </n-tooltip>
          <n-button type="primary" @click="handleAdd">{{ $t('i18n_66ab5e9f24') }}</n-button>
        </n-space>
      </template>
      <template #tableBodyCell="{ column, text, record }">
        <template v-if="column.dataIndex === 'name'">
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
        <template v-else-if="column.dataIndex === 'status'">
          <n-switch
            size="small"
            :checked="text"
            disabled
            :checked-label="$t('i18n_cc42dd3170')"
            :unchecked-label="$t('i18n_b15d91274e')"
          />
        </template>
        <template v-else-if="column.dataIndex === 'autoRestart'">
          <n-switch
            size="small"
            :checked="text"
            disabled
            :checked-label="$t('i18n_0a60ac8f02')"
            :unchecked-label="$t('i18n_c9744f45e7')"
          />
        </template>
        <template v-else-if="column.dataIndex === 'alarm'">
          <n-switch
            size="small"
            :checked="text"
            disabled
            :checked-label="$t('i18n_11957d12e4')"
            :unchecked-label="$t('i18n_bb667fdb2a')"
          />
        </template>

        <template v-else-if="column.dataIndex === 'operation'">
          <n-space>
            <n-button type="primary" size="small" @click="handleEdit(record)">{{ $t('i18n_95b351c862') }}</n-button>
            <n-button type="primary" danger size="small" @click="handleDelete(record)">{{
              $t('i18n_2f4aaddde3')
            }}</n-button>
          </n-space>
        </template>
      </template>
    </CustomTable>
    <!-- 编辑区 -->
    <CustomModal
      v-if="editMonitorVisible"
      v-model:open="editMonitorVisible"
      destroy-on-close
      :confirm-loading="confirmLoading"
      width="60%"
      :title="$t('i18n_ebc2a1956b')"
      :mask-closable="false"
      @ok="handleEditMonitorOk"
    >
      <n-form ref="editMonitorForm" :rules="rules" :model="temp">
        <n-form-item :label="$t('i18n_f976e8fcf4')" path="name">
          <n-input v-model:value="temp.name" :max-length="50" :placeholder="$t('i18n_f976e8fcf4')" />
        </n-form-item>

        <n-form-item :label="$t('i18n_a4f5cae8d2')" path="status">
          <n-space size="large">
            <n-switch
              v-model:value="temp.status"
              :checked-label="$t('i18n_8493205602')"
              :unchecked-label="$t('i18n_d58a55bcee')"
            />
            <div>
              {{ $t('i18n_75528c19c7') }}:
              <n-form-item>
                <n-switch
                  v-model:value="temp.autoRestart"
                  :checked-label="$t('i18n_8493205602')"
                  :unchecked-label="$t('i18n_d58a55bcee')"
                />
              </n-form-item>
            </div>
          </n-space>
        </n-form-item>

        <!-- <n-form-item label="自动重启" path="autoRestart">

            </n-form-item> -->

        <!-- <n-form-item label="监控周期" path="cycle">
            <n-radio-group v-model="temp.cycle" name="cycle">
              <n-radio :value="1">1 分钟</n-radio>
              <n-radio :value="5">5 分钟</n-radio>
              <n-radio :value="10">10 分钟</n-radio>
              <n-radio :value="30">30 分钟</n-radio>
            </n-radio-group>
          </n-form-item> -->

        <n-form-item :label="$t('i18n_67e7f9e541')" path="execCron">
          <n-auto-complete
            v-model:value="temp.execCron"
            :placeholder="$t('i18n_5dff0d31d0')"
            :options="CRON_DATA_SOURCE"
          >
            <template #option="item"> {{ item.title }} {{ item.value }} </template>
          </n-auto-complete>
        </n-form-item>
        <n-form-item :label="$t('i18n_0e55a594fd')" path="projects">
          <n-select
            v-model:value="projectKeys"
            multiple
            :placeholder="$t('i18n_ac5f3bfa5b')"
            filterable
            :options="
              nodeProjectGroupList.map((nodeItem) => ({
                type: 'group',
                label: nodeMap[nodeItem.node].name,
                key: nodeItem.node,
                children: nodeItem.projects.map((project) => ({
                  label: `【${project.nodeName}】 ${project.name} - ${project.runMode}`,
                  value: project.id,
                  disabled: !noFileModes.includes(project.runMode)
                }))
              }))
            "
          />
        </n-form-item>
        <n-form-item path="notifyUser" class="voyager1-notify">
          <template #label>
            <n-tooltip>
              <template #trigger>
                <span class="tw">
                  {{ $t('i18n_52409da520') }}

                  <QuestionCircleOutlined v-show="!temp.id" />
                </span>
              </template>
              {{ $t('i18n_067eb0fa04') }}
            </n-tooltip>
          </template>
          <n-transfer
            v-model:value="targetKeys"
            :options="userList"
            :titles="[$t('i18n_43d229617a'), $t('i18n_f08afd1f82')]"
            filterable
            :list-style="{
              width: '18vw'
            }"
            @update:value="handleChange"
          >
            <template #render="item">
              <template v-if="item.disabled">
                <n-tooltip>
                  <template #trigger>
                    <span class="tw">
                      <WarningTwoTone />
                      {{ item.name }}
                    </span>
                  </template>
                  $t('i18n_44876fc0e7')
                </n-tooltip>
              </template>
              <template v-else>
                <n-tooltip>
                  <template #trigger>
                    {{ item.name }}
                  </template>
                  item.name
                </n-tooltip>
              </template>
            </template>
          </n-transfer>
        </n-form-item>
        <n-form-item path="webhook">
          <template #label>
            <n-tooltip>
              <template #trigger>
                <span class="tw">
                  WebHooks

                  <QuestionCircleOutlined v-show="!temp.id" />
                </span>
              </template>

              <ul>
                <li>{{ $t('i18n_74dd7594fc') }}</li>
                <li>{{ $t('i18n_d1f56b0a7e') }}</li>
                <li>
                  runStatus {{ $t('i18n_808c18d2bb') }}({{ $t('i18n_ad9788b17d') }}),false
                  {{ $t('i18n_22e4da4998') }}({{ $t('i18n_2b52fa609c') }})
                </li>
              </ul>
            </n-tooltip>
          </template>
          <n-input v-model:value="temp.webhook" :placeholder="$t('i18n_77373db7d8')" />
        </n-form-item>
        <n-form-item path="useLanguage">
          <template #label>{{ $t('i18n_0b6811e5b1') }}</template>
          <n-select
            v-model:value="temp.useLanguage"
            :placeholder="$t('i18n_9e0c797c04')"
            :options="supportLang.map((item) => ({ label: item.label, value: item.value }))"
          />
        </n-form-item>
        <n-form-item path="useLanguage">
          <template #label>
            <n-tooltip>
              <template #trigger>
                <span class="tw"> {{ $t('i18n_2b696d1fec') }}<QuestionCircleOutlined v-show="!temp.id" /> </span>
              </template>
              {{ $t('i18n_7a28e9cd4a') }}
            </n-tooltip>
          </template>

          <n-input-number v-model:value="temp.silenceTime" :placeholder="$t('i18n_5ae4a8f177')" style="width: 100%">
            <template #suffix>
              <n-select
                v-model:value="temp.silenceUnit"
                style="width: 100px"
                :placeholder="$t('i18n_1a2c905e87')"
                :options="[
                  { label: $t('i18n_249aba7632'), value: 'DAYS' },
                  { label: $t('i18n_2de0d491d0'), value: 'HOURS' },
                  { label: $t('i18n_3a17b7352e'), value: 'MINUTES' },
                  { label: $t('i18n_0c1fec657f'), value: 'SECONDS' }
                ]"
              />
            </template>
          </n-input-number>
        </n-form-item>
      </n-form>
    </CustomModal>
  </div>
</template>
<script>
import { QuestionCircleOutlined, WarningTwoTone } from '@ant-design/icons-vue'

import { deleteMonitor, editMonitor, getMonitorList } from '@/api/monitor'
import { noFileModes } from '@/api/node-project'
import { getUserListAll } from '@/api/user/user'
import { getNodeListAll, getProjectListAll } from '@/api/node'
import { CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY, itemGroupBy, parseTime } from '@/utils/const'
import { CRON_DATA_SOURCE } from '@/utils/const-i18n'
import { supportLang } from '@/i18n'
export default {
  data() {
    return {
      loading: false,
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      CRON_DATA_SOURCE,
      list: [],
      userList: [],
      nodeProjectList: [],
      nodeProjectGroupList: [],
      nodeMap: {},
      targetKeys: [],
      projectKeys: [],
      // tree 选中的值
      checkedKeys: {},
      noFileModes,
      temp: {},
      editMonitorVisible: false,
      columns: [
        {
          title: this.$t('i18n_d7ec2d3fea'),
          key: 'name',
          ellipsis: true
        },
        {
          title: this.$t('i18n_67e7f9e541'),
          key: 'execCron',
          ellipsis: true
        },
        {
          title: this.$t('i18n_a4f5cae8d2'),
          key: 'status',
          ellipsis: true,

          width: 120
        },
        {
          title: this.$t('i18n_75528c19c7'),
          key: 'autoRestart',
          ellipsis: true,

          width: 120
        },
        {
          title: this.$t('i18n_db4470d98d'),
          key: 'alarm',
          ellipsis: true,

          width: 120
        },
        {
          title: this.$t('i18n_9baca0054e'),
          key: 'modifyUser',
          ellipsis: true,
          align: 'center',

          width: 120
        },
        {
          title: this.$t('i18n_1303e638b5'),
          key: 'modifyTimeMillis',
          sorter: true,
          render: (row) => {
            if (!row['modifyTimeMillis'] || row['modifyTimeMillis'] === '0') {
              return ''
            }
            return parseTime(row['modifyTimeMillis'])
          },
          width: 180
        },
        {
          title: this.$t('i18n_2b6bc0f293'),
          key: 'operation',
          ellipsis: true,
          fixed: 'right',
          width: '120px'
        }
      ],

      rules: {
        name: [
          {
            required: true,
            message: this.$t('i18n_c68dc88c51'),
            trigger: 'blur'
          }
        ]
      },
      confirmLoading: false,
      supportLang
    }
  },
  computed: {
    pagination() {
      return COMPUTED_PAGINATION(this.listQuery)
    },
    activePage() {
      return this.$attrs.routerUrl === this.$route.path
    }
  },
  watch: {},
  created() {
    this.loadData()
  },
  methods: {
    // 加载数据
    loadData(pointerEvent) {
      this.loading = true
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page
      getMonitorList(this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data.result
          this.listQuery.total = res.data.total
        }
        this.loading = false
      })
    },
    // 加载用户列表
    loadUserList(fn) {
      getUserListAll().then((res) => {
        if (res.code === 200) {
          this.$nextTick(() => {
            this.userList = res.data.map((element) => {
              let canUse = element.email || element.dingDing || element.workWx
              return { key: element.id, name: element.name, disabled: !canUse }
            })

            fn && fn()
          })
        }
      })
    },
    // 加载节点项目列表
    loadNodeProjectList(fn) {
      this.nodeProjectList = []
      this.nodeProjectGroupList = []
      getProjectListAll().then((res) => {
        if (res.code === 200) {
          getNodeListAll().then((res1) => {
            res1.data.forEach((element) => {
              this.nodeMap[element.id] = element
            })

            this.nodeProjectList = res.data.map((item) => {
              let nodeInfo = res1.data.filter((nodeItem) => nodeItem.id === item.nodeId)
              item.nodeName = nodeInfo.length > 0 ? nodeInfo[0].name : this.$t('i18n_1622dc9b6b')
              return item
            })
            this.nodeProjectGroupList = itemGroupBy(this.nodeProjectList, 'nodeId', 'node', 'projects')
            // console.log(this.nodeProjectGroupList);
            fn && fn()
          })
        }
      })
    },
    // 穿梭框筛选
    filterOption(inputValue, option) {
      return option.name.indexOf(inputValue) > -1
    },
    // 穿梭框 change
    handleChange(targetKeys) {
      this.targetKeys = targetKeys
    },

    // 新增
    handleAdd() {
      this.temp = {}
      this.targetKeys = []
      this.projectKeys = []
      this.editMonitorVisible = true
      this.loadUserList()
      this.loadNodeProjectList()
    },
    // 修改
    handleEdit(record) {
      this.temp = Object.assign({}, record)
      this.temp.projectsTemp = JSON.parse(this.temp.projects)
      this.targetKeys = []
      this.loadUserList(() => {
        this.targetKeys = JSON.parse(this.temp.notifyUser)

        this.loadNodeProjectList(() => {
          // 设置监控项目
          this.projectKeys = this.nodeProjectList
            .filter((item) => {
              return (
                this.temp.projectsTemp.filter((item2) => {
                  let isNode = item.nodeId === item2.node
                  if (!isNode) {
                    return false
                  }
                  return item2.projects.filter((item3) => item.projectId === item3).length > 0
                }).length > 0
              )
            })
            .map((item) => {
              return item.id
            })

          this.editMonitorVisible = true
        })
      })
    },
    handleEditMonitorOk() {
      // 检验表单
      this.$refs['editMonitorForm'].validate().then(() => {
        let projects = this.nodeProjectList.filter((item) => {
          return this.projectKeys.includes(item.id)
        })
        projects = itemGroupBy(projects, 'nodeId', 'node', 'projects')
        projects.map((item) => {
          item.projects = item.projects.map((item) => {
            return item.projectId
          })
          return item
        })

        let targetKeysTemp = this.targetKeys || []
        targetKeysTemp = this.userList
          .filter((item) => {
            return targetKeysTemp.includes(item.key)
          })
          .map((item) => item.key)

        if (targetKeysTemp.length <= 0 && !this.temp.webhook) {
          $notification.warn({
            message: this.$t('i18n_6c24533675')
          })
          return false
        }

        const params = {
          ...this.temp,
          status: this.temp.status ? 'on' : 'off',
          autoRestart: this.temp.autoRestart ? 'on' : 'off',
          projects: JSON.stringify(projects),
          notifyUser: JSON.stringify(targetKeysTemp)
          //useLanguage: this.temp.useLanguage
        }
        this.confirmLoading = true
        editMonitor(params)
          .then((res) => {
            if (res.code === 200) {
              // 成功
              $notification.success({
                message: res.msg
              })
              this.$refs['editMonitorForm'].resetFields()
              this.editMonitorVisible = false
              this.loadData()
            }
          })
          .finally(() => {
            this.confirmLoading = false
          })
      })
    },
    // 删除
    handleDelete(record) {
      $confirm({
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        content: this.$t('i18n_20e0b90021'),
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
        onOk: () => {
          return deleteMonitor(record.id).then((res) => {
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
    // 分页、排序、筛选变化时触发
    changePage(pagination, filters, sorter) {
      this.listQuery = CHANGE_PAGE(this.listQuery, { pagination, sorter })
      this.loadData()
    }
  }
}
</script>
