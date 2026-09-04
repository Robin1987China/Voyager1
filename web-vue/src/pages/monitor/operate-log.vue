<template>
  <div>
    <!-- 数据表格 -->
        <n-card size="small" :body-style="{ padding: '12px' }" style="margin-bottom: 12px">

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
      
    </n-card>
<n-data-table
      size="medium"
      :data="list"
      :columns="columns"
      :pagination="pagination"
      bordered
      :row-key="(row) => row.id"
      @update:page="(page) => changePage({ ...pagination, current: page })"
      @update:page-size="(pageSize) => changePage({ ...pagination, current: 1, pageSize })"
    >
      
      <template #bodyCell="{ column, text, record }">
        <template v-if="column.key === 'name'">
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
        <template v-else-if="column.key === 'status'">
          <n-switch
            size="small"
            :value="text"
            :checked-label="$t('i18n_cc42dd3170')"
            :unchecked-label="$t('i18n_b15d91274e')"
          />
        </template>

        <template v-else-if="column.key === 'operation'">
          <n-space>
            <n-button size="small" type="primary" @click="handleEdit(record)">{{ $t('i18n_95b351c862') }}</n-button>
            <n-button size="small" type="primary" danger @click="handleDelete(record)">{{
              $t('i18n_2f4aaddde3')
            }}</n-button>
          </n-space>
        </template>
      </template>
    </n-data-table>
    <!-- 编辑区 -->
    <CustomModal
      v-if="editOperateMonitorVisible"
      v-model:open="editOperateMonitorVisible"
      destroy-on-close
      :confirm-loading="confirmLoading"
      width="50vw"
      :title="$t('i18n_ebc2a1956b')"
      :mask-closable="false"
      @ok="handleEditOperateMonitorOk"
    >
      <n-form ref="editMonitorForm" :rules="rules" :model="temp">
        <n-form-item :label="$t('i18n_f976e8fcf4')" path="name">
          <n-input v-model:value="temp.name" :max-length="50" :placeholder="$t('i18n_f976e8fcf4')" />
        </n-form-item>
        <n-form-item :label="$t('i18n_a4f5cae8d2')" path="status">
          <n-switch
            v-model:value="temp.start"
            :checked-label="$t('i18n_8493205602')"
            :unchecked-label="$t('i18n_d58a55bcee')"
          />
        </n-form-item>
        <n-form-item :label="$t('i18n_5e46f842d8')" path="monitorUser">
          <n-transfer
            v-model:value="monitorUserKeys"
            :options="monitorUserList"
            filterable
            @update:value="handleMonitorUserChange"
          >
            <template #render="item">
              <n-tooltip>
                <template #trigger>
                  {{ item.title }}
                </template>
                item.title
              </n-tooltip>
            </template>
          </n-transfer>
        </n-form-item>
        <n-form-item :label="$t('i18n_5cb39287a8')" path="monitorOpt">
          <n-transfer
            v-model:value="classFeatureKeys"
            :options="classFeature"
            filterable
            @update:value="handleClassFeatureChange"
          >
            <template #render="item">
              <n-tooltip>
                <template #trigger>
                  {{ item.title }}
                </template>
                item.title
              </n-tooltip>
            </template>
          </n-transfer>
        </n-form-item>
        <n-form-item :label="$t('i18n_3e7ef69c98')" path="monitorOpt">
          <n-transfer
            v-model:value="methodFeatureKeys"
            :options="methodFeature"
            filterable
            @update:value="handleMethodFeatureChange"
          >
            <template #render="item">
              <n-tooltip>
                <template #trigger>
                  {{ item.title }}
                </template>
                item.title
              </n-tooltip>
            </template>
          </n-transfer>
        </n-form-item>
        <n-form-item path="notifyUser" class="voyager1-monitor-notify">
          <template #label>
            <n-tooltip>
              <template #trigger>
                <span class="tw">
                  {{ $t('i18n_09723d428d') }}

                  <QuestionCircleOutlined v-show="!temp.id" />
                </span>
              </template>
              {{ $t('i18n_067eb0fa04') }}
            </n-tooltip>
          </template>
          <n-transfer
            v-model:value="notifyUserKeys"
            :options="userList"
            filterable
            @update:value="handleNotifyUserChange"
          >
            <template #render="item">
              <n-tooltip>
                <template #trigger>
                  {{ item.title }}
                </template>
                item.title
              </n-tooltip>
            </template>
          </n-transfer>
        </n-form-item>
      </n-form>
    </CustomModal>
  </div>
</template>
<script>
import { QuestionCircleOutlined } from '@ant-design/icons-vue'

import {
  deleteMonitorOperate,
  editMonitorOperate,
  getMonitorOperateLogList,
  getMonitorOperateTypeList
} from '@/api/monitor'
import { getUserListAll } from '@/api/user/user'
import { CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY, parseTime } from '@/utils/const'

export default {
  data() {
    return {
      loading: false,
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      list: [],

      classFeature: [],
      methodFeature: [],
      userList: [],
      monitorUserList: [],
      temp: {},
      notifyUserKeys: [],
      monitorUserKeys: [],
      classFeatureKeys: [],
      methodFeatureKeys: [],
      editOperateMonitorVisible: false,
      columns: [
        {
          title: this.$t('i18n_d7ec2d3fea'),
          key: 'name'
        },
        {
          title: this.$t('i18n_a4f5cae8d2'),
          key: 'status'
        },
        {
          title: this.$t('i18n_9baca0054e'),
          key: 'modifyUser'
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
          align: 'center',
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
      confirmLoading: false
    }
  },
  computed: {
    pagination() {
      return COMPUTED_PAGINATION(this.listQuery)
    }
  },
  watch: {},
  created() {
    this.loadData()
    this.loadOptTypeData()
  },
  methods: {
    // 加载数据
    loadData(pointerEvent) {
      this.loading = true
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page
      getMonitorOperateLogList(this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data.result
          this.listQuery.total = res.data.total
        }
        this.loading = false
      })
    },
    // 加载操作类型数据
    loadOptTypeData() {
      // this.optTypeList = [];
      getMonitorOperateTypeList().then((res) => {
        if (res.code === 200) {
          this.methodFeature = res.data.methodFeature.map((element) => {
            return { value: element.value, label: element.title, disabled: false }
          })
          this.classFeature = res.data.classFeature.map((element) => {
            return { value: element.value, label: element.title, disabled: false }
          })
        }
      })
    },
    // 加载用户列表
    loadUserList() {
      // this.userList = [];
      getUserListAll().then((res) => {
        if (res.code === 200) {
          // res.data.forEach((element) => {
          //   this.userList.push({ key: element.value, title: element.title, disabled: element.disabled || false });
          // });
          this.userList = res.data.map((element) => {
            let canUse = element.email || element.dingDing || element.workWx
            return { value: element.id, label: element.name, disabled: !canUse }
          })
          this.monitorUserList = res.data.map((element) => {
            return { value: element.id, label: element.name }
          })
        }
      })
    },
    // 新增
    handleAdd() {
      this.temp = {
        start: false
      }
      this.notifyUserKeys = []
      this.classFeatureKeys = []
      this.methodFeatureKeys = []
      this.monitorUserKeys = []
      this.loadUserList()
      this.editOperateMonitorVisible = true
    },
    // 修改
    handleEdit(record) {
      this.loadUserList()
      this.temp = Object.assign({}, record)
      this.temp = {
        ...this.temp,
        start: this.temp.status
      }
      this.notifyUserKeys = JSON.parse(this.temp.notifyUser)
      this.classFeatureKeys = JSON.parse(this.temp.monitorFeature)
      this.methodFeatureKeys = JSON.parse(this.temp.monitorOpt)
      this.monitorUserKeys = JSON.parse(this.temp.monitorUser)
      this.editOperateMonitorVisible = true
    },
    // 穿梭框筛选
    filterOption(inputValue, option) {
      return option.title.indexOf(inputValue) > -1
    },
    // 穿梭框 change
    handleNotifyUserChange(targetKeys) {
      this.notifyUserKeys = targetKeys
    },
    // 穿梭框 change
    handleMethodFeatureChange(targetKeys) {
      this.methodFeatureKeys = targetKeys
    },
    handleClassFeatureChange(targetKeys) {
      this.classFeatureKeys = targetKeys
    },

    // 穿梭框 change
    handleMonitorUserChange(targetKeys) {
      this.monitorUserKeys = targetKeys
    },
    // 提交
    handleEditOperateMonitorOk() {
      // 检验表单
      this.$refs['editMonitorForm'].validate().then(() => {
        if (this.monitorUserKeys.length === 0) {
          $notification.error({
            message: this.$t('i18n_83c61f7f9e')
          })
          return false
        }
        if (this.methodFeatureKeys.length === 0) {
          $notification.error({
            message: this.$t('i18n_fabc07a4f1')
          })
          return false
        }
        if (this.classFeatureKeys.length === 0) {
          $notification.error({
            message: this.$t('i18n_c6e4cddba0')
          })
          return false
        }
        if (this.notifyUserKeys.length === 0) {
          $notification.error({
            message: this.$t('i18n_d02a9a85df')
          })
          return false
        }
        // 设置参数
        this.temp.monitorUser = JSON.stringify(this.monitorUserKeys)
        this.temp.monitorOpt = JSON.stringify(this.methodFeatureKeys)
        this.temp.monitorFeature = JSON.stringify(this.classFeatureKeys)
        this.temp.notifyUser = JSON.stringify(this.notifyUserKeys)
        this.temp.start ? (this.temp.status = 'on') : (this.temp.status = 'no')
        this.confirmLoading = false
        editMonitorOperate(this.temp)
          .then((res) => {
            if (res.code === 200) {
              // 成功
              $notification.success({
                message: res.msg
              })
              this.$refs['editMonitorForm'].restoreValidation()
              this.editOperateMonitorVisible = false
              this.loadData()
            }
          })
          .finally(() => {
            this.confirmLoading = false
          })
      }).catch(() => {})
    },
    // 删除
    handleDelete(record) {
      $confirm({
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        content: this.$t('i18n_b63c057330'),
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
        onOk: () => {
          return deleteMonitorOperate(record.id).then((res) => {
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
