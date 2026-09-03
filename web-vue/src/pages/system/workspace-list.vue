<template>
  <div>
    <!-- 数据表格 -->
    <n-data-table
      :data="list"
      :columns="columns"
      size="medium"
      :pagination="pagination"
      bordered
      :row-key="(row) => row.id"
      :scroll="{
        x: 'max-content'
      }"
      @change="changePage"
    >
      <template #title>
        <n-space>
          <n-input
            v-model:value="listQuery['id']"
            :placeholder="$t('i18n_ab7f78ba4c')"
            clearable
            class="search-input-item"
            @press-enter="loadData"
          />
          <n-input
            v-model:value="listQuery['%name%']"
            :placeholder="$t('i18n_6a588459d0')"
            clearable
            class="search-input-item"
            @press-enter="loadData"
          />
          <n-select
            v-model:value="listQuery.group"
            filterable
            clearable
            :placeholder="$t('i18n_829abe5a8d')"
            class="search-input-item"
            :options="groupList"
          />
          <n-select
            v-model:value="listQuery.clusterInfoId"
            filterable
            clearable
            :placeholder="$t('i18n_85fe5099f6')"
            class="search-input-item"
            :options="clusterList.map((item) => ({ label: item.name, value: item.id }))"
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
          <n-tooltip>
            <template #trigger>
              <QuestionCircleOutlined />
            </template>

            <ul>
              <li>{{ $t('i18n_da509a213f') }}</li>
              <li>{{ $t('i18n_97cb3c4b2e') }}</li>
            </ul>
          </n-tooltip>
        </n-space>
      </template>
      <template #bodyCell="{ column, text, record }">
        <template v-if="column.dataIndex === 'description'">
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
        <template v-else-if="column.dataIndex === 'name'">
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
        <template v-else-if="column.dataIndex === 'clusterInfoId'">
          <n-tooltip placement="top-start">
            <template #trigger>
              <span class="tw">
                <span class="tw">
                  <span>{{
                    clusterList.find((item) => {
                      return item.id === text
                    }) &&
                    clusterList.find((item) => {
                      return item.id === text
                    }).name
                  }}</span>
                </span>
              </span>
            </template>
            {{
              (clusterList.find((item) => {
                return item.id === text
              }) &&
                clusterList.find((item) => {
                  return item.id === text
                }).name) ||
              ''
            }}
          </n-tooltip>
        </template>

        <template v-else-if="column.dataIndex === 'operation'">
          <n-space>
            <n-button size="small" type="primary" @click="handleEdit(record)">{{ $t('i18n_95b351c862') }}</n-button>
            <n-button size="small" type="primary" @click="configMeun(record)">{{ $t('i18n_4ccbdc5301') }}</n-button>
            <n-button size="small" type="primary" @click="configWhiteDir(record)">{{ $t('i18n_3d48c9da09') }}</n-button>
            <n-button size="small" type="primary" @click="viewEnvVar(record)">{{ $t('i18n_ddc7d28b7b') }}</n-button>

            <n-tooltip v-if="record.id === 'DEFAULT'">
              <template #trigger>
                <span class="tw">
                  <n-button size="small" type="primary" danger :disabled="true">{{ $t('i18n_2f4aaddde3') }}</n-button>
                </span>
              </template>
              $t('i18n_0c0633c367')
            </n-tooltip>
            <n-button v-else size="small" type="primary" danger @click="handleDelete(record)">{{
              $t('i18n_2f4aaddde3')
            }}</n-button>
          </n-space>
        </template>
      </template>
    </n-data-table>
    <!-- 编辑区 -->
    <CustomModal
      v-if="editVisible"
      v-model:open="editVisible"
      destroy-on-close
      :confirm-loading="confirmLoading"
      :title="$t('i18n_fa8e673c50')"
      :mask-closable="false"
      @ok="handleEditOk"
    >
      <n-alert :title="$t('i18n_c8c6e37071')" type="info" show-icon>
        <template #description>
          <ul>
            <li>{{ $t('i18n_a89646d060') }}</li>
            <li>{{ $t('i18n_207243d77a') }}</li>
            <li>{{ $t('i18n_67aa2d01b9') }}</li>
          </ul>
        </template>
      </n-alert>
      <n-form ref="editForm" :rules="rules" :model="temp" style="padding-top: 15px">
        <n-form-item :label="$t('i18n_d7ec2d3fea')" path="name">
          <n-input v-model:value="temp.name" :max-length="50" :placeholder="$t('i18n_6a588459d0')" />
        </n-form-item>
        <n-form-item :label="$t('i18n_9b5f172ebe')" path="clusterInfoId">
          <n-select
            v-model:value="temp.clusterInfoId"
            filterable
            clearable
            :placeholder="$t('i18n_9b5f172ebe')"
            :options="clusterList.map((item) => ({ label: item.name, value: item.id }))"
          />
        </n-form-item>
        <n-form-item :label="$t('i18n_829abe5a8d')" path="group">
          <custom-select
            v-model:value="temp.group"
            :data="groupList"
            :input-placeholder="$t('i18n_bd0362bed3')"
            :select-placeholder="$t('i18n_9cac799f2f')"
          >
            <template #suffix>
              <n-tooltip>
                <template #trigger>
                  <QuestionCircleOutlined />
                </template>

                <div>
                  {{ $t('i18n_bd7c7abc8c') }}
                </div>
              </n-tooltip>
            </template>
          </custom-select>
        </n-form-item>

        <n-form-item :label="$t('i18n_3bdd08adab')" path="description">
          <n-input
            v-model:value="temp.description"
            type="textarea"
            :max-length="200"
            :rows="5"
            :placeholder="$t('i18n_4645575b77')"
          />
        </n-form-item>
      </n-form>
    </CustomModal>
    <!-- 环境变量 -->
    <CustomModal
      v-if="envVarListVisible"
      v-model:open="envVarListVisible"
      destroy-on-close
      :title="`${temp.name} ${$t('i18n_f7e8d887d6')}`"
      width="80vw"
      :footer="null"
      :mask-closable="false"
    >
      <workspaceEnv v-if="envVarListVisible" ref="workspaceEnv" :workspace-id="temp.id" />
    </CustomModal>
    <!-- 工作空间菜单 -->
    <CustomModal
      v-if="configMenuVisible"
      v-model:open="configMenuVisible"
      destroy-on-close
      :confirm-loading="confirmLoading"
      :title="`${temp.name} ${$t('i18n_25182fb439')}`"
      :mask-closable="false"
      @ok="onSubmitMenus"
    >
      <n-form ref="editWhiteForm" :model="menusConfigData">
        <n-grid type="flex" justify="center">
          <n-alert
            :title="`${$t('i18n_6228294517')}`"
            style="margin-top: 10px; margin-bottom: 20px; width: 100%"
            banner
          />
          <n-grid-item :span="20">
            <n-card :title="$t('i18n_a75f781415')" :bordered="true">
              <n-tree
                v-if="menusConfigData.serverMenus"
                v-model:checked-keys="menusConfigData.serverMenuKeys"
                checkable
                :data="menusConfigData.serverMenus"
                :label-field="'title'"
                :key-field="'id'"
                :children-field="'childs'"
                :render-prefix="(info) => h(Icon, { type: info.option.icon_v3 })"
              />
            </n-card>
          </n-grid-item>
        </n-grid>
      </n-form>
    </CustomModal>
    <!-- 配置授权目录 -->
    <CustomModal
      v-if="configDir"
      v-model:open="configDir"
      destroy-on-close
      :title="`${$t('i18n_eee6510292')}`"
      :footer="null"
      width="60vw"
      :mask-closable="false"
      @cancel="
        () => {
          configDir = false
        }
      "
    >
      <whiteList
        v-if="configDir"
        :workspace-id="temp.id"
        @cancel="
          () => {
            configDir = false
          }
        "
      ></whiteList>
    </CustomModal>
    <!-- 删除工作空间检查 -->
    <CustomModal
      v-if="preDeleteVisible"
      v-model:open="preDeleteVisible"
      destroy-on-close
      :confirm-loading="confirmLoading"
      :title="`${$t('i18n_aec7b550e2')}`"
      :mask-closable="false"
      @ok="handleDeleteOk"
      @cancel="
        () => {
          preDeleteVisible = false
        }
      "
    >
      <n-alert :title="$t('i18n_a35740ae41')" type="error" show-icon>
        <template #description> {{ $t('i18n_6b46e2bfae') }},{{ $t('i18n_86b7eb5e83') }}</template>
      </n-alert>

      <n-tree
        :data="treeData"
        :label-field="'name'"
        :key-field="'id'"
        :children-field="'children'"
        :show-line="true"
        :render-label="
          (info) => {
            const d = info.option
            return h('span', null, [
              d.count === 0 ? h(CheckOutlined, { style: 'color: green' }) : h(CloseOutlined, { style: 'color: red' }),
              ' ' + d.name,
              d.count > 0
                ? h('span', null, [
                    h(
                      NTag,
                      { color: 'pink' },
                      { default: () => `${$t('i18n_df9497ea98')} ${d.count} ${$t('i18n_f932eff53e')}` }
                    ),
                    h(
                      NTag,
                      d.workspaceBind === 2
                        ? { color: 'cyan' }
                        : d.workspaceBind === 3
                          ? { color: 'blue' }
                          : { color: 'purple' },
                      {
                        default: () =>
                          d.workspaceBind === 2
                            ? $t('i18n_686a19db6a')
                            : d.workspaceBind === 3
                              ? $t('i18n_9c3a3e1b03')
                              : $t('i18n_ab006f89e7')
                      }
                    )
                  ])
                : null
            ])
          }
        "
      />
    </CustomModal>
  </div>
</template>
<script>
import { QuestionCircleOutlined } from '@ant-design/icons-vue'

import { h } from 'vue'
import { NTag } from 'naive-ui'
import { CheckOutlined, CloseOutlined } from '@ant-design/icons-vue'
import {
  deleteWorkspace,
  preDeleteWorkspace,
  editWorkSpace,
  getWorkSpaceList,
  getMenusConfig,
  saveMenusConfig,
  getWorkSpaceGroupList
} from '@/api/workspace'
import Icon from '@/components/Icon'
import { CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY, parseTime } from '@/utils/const'
import { listClusterAll } from '@/api/system/cluster'
import workspaceEnv from './workspace-env.vue'
import CustomSelect from '@/components/customSelect'
import whiteList from '@/pages/dispatch/white-list.vue'
export default {
  components: {
    workspaceEnv,
    CustomSelect,
    whiteList,
    Icon
    // VNodes: {
    //   props: {
    //     vnodes: {
    //       type: Object,
    //       required: true
    //     }
    //   },
    //   render() {
    //     return this.vnodes
    //   }
    // }
  },
  data() {
    return {
      loading: true,
      list: [],
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      editVisible: false,
      envVarListVisible: false,
      temp: {},
      columns: [
        {
          title: this.$t('i18n_d7ec2d3fea'),
          key: 'name',
          ellipsis: true,
          width: 200
        },
        {
          title: this.$t('i18n_3bdd08adab'),
          key: 'description',
          ellipsis: true,
          width: 200
        },
        {
          title: this.$t('i18n_b37b786351'),
          key: 'group',
          ellipsis: true,
          width: '100px'
        },
        {
          title: this.$t('i18n_85fe5099f6'),
          key: 'clusterInfoId',
          ellipsis: true,
          width: '100px'
        },
        {
          title: this.$t('i18n_9baca0054e'),
          key: 'modifyUser',
          ellipsis: true,

          width: 120
        },
        {
          title: this.$t('i18n_eca37cb072'),
          key: 'createTimeMillis',
          sorter: true,
          ellipsis: true,
          render: (row) => parseTime(row['createTimeMillis']),
          width: '170px'
        },
        {
          title: this.$t('i18n_1303e638b5'),
          key: 'modifyTimeMillis',
          render: (row) => parseTime(row['modifyTimeMillis']),
          sorter: true,
          width: '170px'
        },
        {
          title: this.$t('i18n_2b6bc0f293'),
          key: 'operation',
          fixed: 'right',
          align: 'center',

          width: '320px'
        }
      ],

      // 表单校验规则
      rules: {
        name: [{ required: true, message: this.$t('i18n_b153126fc2'), trigger: 'blur' }],
        description: [{ required: true, message: this.$t('i18n_36b5d427e4'), trigger: 'blur' }],
        clusterInfoId: [{ required: true, message: this.$t('i18n_aad7450231'), trigger: 'blur' }]
      },
      configMenuVisible: false,
      replaceFields: { children: 'childs', title: 'title', key: 'id' },
      menusConfigData: {},
      groupList: [],
      configDir: false,
      preDeleteVisible: false,
      preDeleteReplaceFields: {
        children: 'children',
        title: 'name',
        key: 'id'
      },
      treeData: [],
      clusterList: [],
      confirmLoading: false
    }
  },
  computed: {
    pagination() {
      return COMPUTED_PAGINATION(this.listQuery)
    }
  },
  created() {
    this.loadData()
    this.loadGroupList()
    this.loadClusterList()
  },
  methods: {
    // 获取所有集群
    loadClusterList() {
      return new Promise((resolve) => {
        listClusterAll().then((res) => {
          if (res.data && res.code === 200) {
            this.clusterList = res.data || []
            resolve()
          }
        })
      })
    },
    // 获取所有的分组
    loadGroupList() {
      getWorkSpaceGroupList().then((res) => {
        if (res.data) {
          this.groupList = res.data
        }
      })
    },
    // 加载数据
    loadData(pointerEvent) {
      this.loading = true
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page
      getWorkSpaceList(this.listQuery)
        .then((res) => {
          if (res.code === 200) {
            this.list = res.data.result
            this.listQuery.total = res.data.total
          }
        })
        .finally(() => {
          this.loading = false
        })
    },

    viewEnvVar(record) {
      this.temp = Object.assign({}, record)
      // this.envTemp = {
      //   workspaceId: this.temp.id,
      // };
      // this.envVarListQuery.workspaceId = record.id;
      this.envVarListVisible = true
      this.$nextTick(() => {
        this.$refs.workspaceEnv.loadDataEnvVar()
      })
    },
    handleAdd() {
      this.loadGroupList()
      this.temp = {}
      this.$refs['editForm'] && this.$refs['editForm'].resetFields()
      this.loadClusterList().then(() => {
        if (this.clusterList.length === 1) {
          this.temp = { ...this.temp, clusterInfoId: this.clusterList[0].id }
        }
        this.editVisible = true
      })
    },
    handleEdit(record) {
      this.loadGroupList()
      this.$refs['editForm'] && this.$refs['editForm'].resetFields()
      this.loadClusterList().then(() => {
        const defData = {}
        if (this.clusterList.length === 1) {
          defData.clusterInfoId = this.clusterList[0].id
        }
        this.temp = Object.assign({}, record, defData)
        this.editVisible = true
      })
    },
    handleEditOk() {
      this.$refs['editForm'].validate().then(() => {
        editWorkSpace(this.temp).then((res) => {
          if (res.code === 200) {
            // 成功
            $notification.success({
              message: res.msg
            })

            this.editVisible = false
            this.loadData()
          }
        })
      })
    },
    // 分页、排序、筛选变化时触发
    changePage(pagination, filters, sorter) {
      this.listQuery = CHANGE_PAGE(this.listQuery, { pagination, sorter })
      this.loadData()
    },
    // 删除
    handleDelete(record) {
      this.temp = { ...record }

      preDeleteWorkspace(this.temp.id).then((res) => {
        this.treeData = res.data?.children || []
        this.preDeleteVisible = true
      })
    },
    handleDeleteOk() {
      // 删除
      this.confirmLoading = true
      deleteWorkspace(this.temp.id)
        .then((res) => {
          if (res.code === 200) {
            $notification.success({
              message: res.msg
            })
            this.preDeleteVisible = false
            this.loadData()
          }
        })
        .finally(() => {
          this.confirmLoading = false
        })
    },
    configMeun(record) {
      this.temp = Object.assign({}, record)

      // 加载菜单配置信息
      // loadMenusConfig(id) {},
      getMenusConfig({
        workspaceId: record.id
      }).then((res) => {
        if (res.code !== 200) {
          return
        }
        this.menusConfigData = res.data

        this.menusConfigData.serverMenus = this.menusConfigData?.serverMenus.map((item) => {
          // item.scopedSlots = { icon: 'custom' }
          item.childs?.map((item2) => {
            item2.id = item.id + ':' + item2.id
            return item2
          })
          return item
        })

        if (!this.menusConfigData?.serverMenuKeys) {
          //
          const serverMenuKeys = []
          this.menusConfigData.serverMenus.forEach((item) => {
            serverMenuKeys.push(item.id)
            if (item.childs) {
              item.childs.forEach((item2) => {
                serverMenuKeys.push(item2.id)
              })
            }
          })
          this.menusConfigData = {
            ...this.menusConfigData,
            serverMenuKeys: serverMenuKeys
          }
        }

        this.configMenuVisible = true
      })
    },
    onSubmitMenus() {
      this.confirmLoading = true
      saveMenusConfig({
        serverMenuKeys: this.menusConfigData.serverMenuKeys.join(','),

        workspaceId: this.temp.id
      })
        .then((res) => {
          if (res.code === 200) {
            // 成功
            $notification.success({
              message: res.msg
            })
            this.configMenuVisible = false
          }
        })
        .finally(() => {
          this.confirmLoading = false
        })
    },
    // 配置节点授权
    configWhiteDir(record) {
      this.temp = Object.assign({}, record)
      this.configDir = true
    }
  }
}
</script>
