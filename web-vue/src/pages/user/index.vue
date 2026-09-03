<template>
  <div>
    <!-- 数据表格 -->
    <CustomTable
      is-show-tools
      default-auto-refresh
      :auto-refresh-time="30"
      :active-page="activePage"
      table-name="systemUserList"
      :empty-description="$t('i18n_0f189dbaa4')"
      :loading="loading"
      :data="list"
      :columns="columns"
      :pagination="pagination"
      bordered
      row-key="id"
      :scroll="{
        x: 'max-content'
      }"
      @change="changePage"
      @refresh="loadData"
    >
      <template #title>
        <n-space wrap class="search-box">
          <n-input
            v-model:value="listQuery.id"
            :placeholder="$t('i18n_1c9d3cb687')"
            class="search-input-item"
            @press-enter="loadData"
          />
          <n-input
            v-model:value="listQuery['%name%']"
            :placeholder="$t('i18n_819767ada1')"
            class="search-input-item"
            @press-enter="loadData"
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
          <n-button type="primary" @click="systemNotificationOpen = true">{{ $t('i18n_7c223eb6e9') }}</n-button>
        </n-space>
      </template>
      <template #tableBodyCell="{ column, text, record }">
        <template v-if="column.dataIndex === 'operation'">
          <n-space>
            <n-button size="small" type="primary" @click="handleEdit(record)">{{ $t('i18n_95b351c862') }}</n-button>
            <n-dropdown
              :options="[
                {
                  label: $t('i18n_2f4aaddde3'),
                  key: '0',
                  disabled: record.parent === 'sys',
                  props: { onClick: () => handleDelete(record) }
                },
                {
                  label: $t('i18n_fa7ffa2d21'),
                  key: '1',
                  disabled: record.pwdErrorCount === 0,
                  props: { onClick: () => handleUnlock(record) }
                },
                {
                  label: $t('i18n_0719aa2bb0'),
                  key: '2',
                  disabled: record.parent === 'sys',
                  props: { onClick: () => restUserPwdHander(record) }
                }
              ]"
            >
              <a @click="(e) => e.preventDefault()"> {{ $t('i18n_0ec9eaf9c3') }} <DownOutlined /> </a>
            </n-dropdown>
          </n-space>
        </template>
        <template v-else-if="column.dataIndex === 'systemUser'">
          <n-switch
            size="small"
            :checked-label="$t('i18n_0a60ac8f02')"
            :unchecked-label="$t('i18n_c9744f45e7')"
            disabled
            :checked="record.systemUser == 1"
          />
        </template>
        <template v-else-if="column.dataIndex === 'status'">
          <n-switch
            size="small"
            :checked-label="$t('i18n_7854b52a88')"
            :unchecked-label="$t('i18n_710ad08b11')"
            disabled
            :checked="record.status != 0"
          />
        </template>

        <template v-else-if="column.dataIndex === 'id'">
          <n-tooltip>
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

        <template v-else-if="column.dataIndex === 'email'">
          <n-tooltip>
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
    </CustomTable>
    <!-- 编辑区 -->
    <CustomModal
      v-if="editUserVisible"
      v-model:open="editUserVisible"
      destroy-on-close
      :confirm-loading="confirmLoading"
      width="60vw"
      :title="$t('i18n_5a0346c4b1')"
      :mask-closable="false"
      @ok="handleEditUserOk"
    >
      <n-alert
        v-if="!permissionGroup || !permissionGroup.length"
        :title="$t('i18n_4b027f3979')"
        type="warning"
        show-icon
        style="margin-bottom: 10px"
      >
        <template #description>{{ $t('i18n_d9531a5ac3') }}</template>
      </n-alert>
      <n-form ref="editUserForm" :rules="rules" :model="temp">
        <n-form-item :label="$t('i18n_878aebf9b2')" path="id">
          <n-input
            v-model:value="temp.id"
            :max-length="50"
            :placeholder="$t('i18n_f175274df0')"
            :disabled="createOption == false"
            @change="checkTipUserName"
          />
        </n-form-item>

        <n-form-item :label="$t('i18n_23eb0e6024')" path="name">
          <n-input v-model:value="temp.name" :max-length="50" :placeholder="$t('i18n_23eb0e6024')" />
        </n-form-item>
        <n-form-item path="systemUser">
          <template #label>
            <n-tooltip>
              <template #trigger>
                <span class="tw">
                  {{ $t('i18n_b1dae9bc5c') }}

                  <QuestionCircleOutlined v-if="createOption" />
                </span>
              </template>
              {{ $t('i18n_b328609814') }}
            </n-tooltip>
          </template>
          <n-grid>
            <n-grid-item :span="4">
              <n-tooltip>
                <template #trigger>
                  <span class="tw">
                    <n-switch
                      :checked="temp.systemUser == 1"
                      :disabled="temp.parent === 'sys'"
                      :checked-label="$t('i18n_0a60ac8f02')"
                      :unchecked-label="$t('i18n_c9744f45e7')"
                      default-checked
                      @change="
                        (checked) => {
                          temp.systemUser = checked ? 1 : 0
                        }
                      "
                    />
                  </span>
                </template>
                $t('i18n_b328609814')
              </n-tooltip>
            </n-grid-item>
            <n-grid-item :span="4" style="text-align: right">
              <n-tooltip>
                <template #trigger>
                  <span class="tw">
                    <QuestionCircleOutlined v-if="createOption" />
                    {{ $t('i18n_bec98b4d6a') }}
                  </span>
                </template>
                {{ $t('i18n_fa624c8420') }}
              </n-tooltip>
            </n-grid-item>
            <n-grid-item :span="4">
              <n-form-item>
                <n-switch
                  :checked="temp.status != 0"
                  :disabled="temp.parent === 'sys'"
                  :checked-label="$t('i18n_7854b52a88')"
                  :unchecked-label="$t('i18n_710ad08b11')"
                  default-checked
                  @change="
                    (checked) => {
                      temp.status = checked ? 1 : 0
                    }
                  "
                />
              </n-form-item>
            </n-grid-item>
          </n-grid>
        </n-form-item>
        <n-form-item :label="$t('i18n_f49dfdace4')" path="permissionGroup">
          <n-select
            v-model:value="temp.permissionGroup"
            filterable
            :placeholder="$t('i18n_72d14a3890')"
            multiple
            :options="permissionGroup.map((item) => ({ label: item.name, value: item.id }))"
          />
        </n-form-item>
      </n-form>
    </CustomModal>
    <CustomModal
      v-if="showUserPwd"
      v-model:open="showUserPwd"
      destroy-on-close
      :title="$t('i18n_318ce9ea8b')"
      :mask-closable="false"
      :footer="null"
    >
      <n-result status="success" :title="temp.title">
        <template #subTitle>
          <div>
            {{ $t('i18n_5684fd7d3d') }}
            <n-p style="margin-bottom: 0">
              <b style="color: red; font-size: 20px">
                {{ temp.randomPwd }}
              </b>
              <copy-text :text="temp.randomPwd" />
            </n-p>
            {{ $t('i18n_12d2c0aead') }}
          </div>
          <div style="color: red">{{ $t('i18n_c7e0803a17') }}</div>
        </template>
      </n-result>
    </CustomModal>
    <!-- 系统公告  -->
    <CustomModal
      v-if="systemNotificationOpen"
      v-model:open="systemNotificationOpen"
      destroy-on-close
      :title="$t('i18n_6428be07e9')"
      :mask-closable="false"
      width="50vw"
      :footer="null"
    >
      <notification />
    </CustomModal>
  </div>
</template>
<script>
import { DownOutlined, QuestionCircleOutlined } from '@ant-design/icons-vue'

import { h } from 'vue'
import { NIcon } from 'naive-ui'

import { deleteUser, editUser, getUserList, unlockUser, restUserPwd } from '@/api/user/user'
import { getUserPermissionListAll } from '@/api/user/user-permission'
import { CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY, parseTime } from '@/utils/const'
import notification from './notification.vue'
export default {
  components: {
    notification
  },
  data() {
    return {
      loading: false,
      list: [],
      temp: {},

      createOption: true,
      editUserVisible: false,
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      permissionGroup: [],
      columns: [
        {
          title: 'ID',
          key: 'id',
          ellipsis: true,
          width: 100
        },
        { title: this.$t('i18n_23eb0e6024'), key: 'name', ellipsis: true, width: 100 },
        {
          title: this.$t('i18n_b1dae9bc5c'),
          key: 'systemUser',
          align: 'center',
          ellipsis: true,
          width: 90
        },
        {
          title: this.$t('i18n_3fea7ca76c'),
          key: 'status',
          align: 'center',
          ellipsis: true,
          width: 90
        },
        {
          title: this.$t('i18n_3bc5e602b2'),
          key: 'email',
          ellipsis: true,
          width: 100
        },
        {
          title: this.$t('i18n_26ca20b161'),
          key: 'source',
          ellipsis: true,
          width: 90
        },
        {
          title: this.$t('i18n_b6076a055f'),
          key: 'pwdErrorCount',
          ellipsis: true,
          width: 90
        },
        { title: this.$t('i18n_95a43eaa59'), key: 'parent', ellipsis: true, width: 150 },

        {
          title: this.$t('i18n_1303e638b5'),
          key: 'modifyTimeMillis',
          sorter: true,
          ellipsis: true,
          render: (row) => {
            return parseTime(row['modifyTimeMillis'])
          },
          width: '170px'
        },
        {
          title: this.$t('i18n_eca37cb072'),
          key: 'createTimeMillis',
          sorter: true,
          render: (row) => {
            return parseTime(row['createTimeMillis'] || row['optTime'])
          },
          width: '170px'
        },
        {
          title: this.$t('i18n_2b6bc0f293'),
          align: 'center',
          key: 'operation',
          fixed: 'right',
          width: '120px'
        }
      ],

      // 表单校验规则
      rules: {
        id: [{ required: true, message: this.$t('i18n_693a06987c'), trigger: 'blur' }],
        name: [{ required: true, message: this.$t('i18n_c00fb0217d'), trigger: 'blur' }],
        permissionGroup: [{ required: true, message: this.$t('i18n_e8073b3843'), trigger: 'blur' }]
      },
      showUserPwd: false,
      confirmLoading: false,
      systemNotificationOpen: false
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
      getUserList(this.listQuery)
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

    // 新增用户
    handleAdd() {
      this.temp = { systemUser: 0 }
      this.createOption = true
      this.listUserPermissionListAll()
      this.editUserVisible = true
      this.$refs['editUserForm'] && this.$refs['editUserForm'].resetFields()
    },
    //
    listUserPermissionListAll() {
      getUserPermissionListAll().then((res) => {
        if (res.code === 200 && res.data) {
          this.permissionGroup = res.data
        }
        if (!this.permissionGroup || this.permissionGroup.length <= 0)
          $notification.warn({
            message: this.$t('i18n_d4744ce461')
          })
      })
    },
    // 修改用户
    handleEdit(record) {
      this.createOption = false
      this.temp = {
        ...record,
        permissionGroup: (record.permissionGroup || '').split('@').filter((item) => item),
        status: record.status === undefined ? 1 : record.status
      }
      this.listUserPermissionListAll()
      this.editUserVisible = true
      this.$refs['editUserForm'] && this.$refs['editUserForm'].resetFields()
    },
    // 提交用户数据
    handleEditUserOk() {
      // 检验表单
      this.$refs['editUserForm'].validate().then(() => {
        const paramsTemp = Object.assign({}, this.temp)

        paramsTemp.type = this.createOption ? 'add' : 'edit'
        paramsTemp.permissionGroup = (paramsTemp.permissionGroup || []).join('@')

        // 需要判断当前操作是【新增】还是【修改】
        this.confirmLoading = true
        editUser(paramsTemp)
          .then((res) => {
            if (res.code === 200) {
              if (paramsTemp.type === 'add') {
                this.temp = {
                  title: this.$t('i18n_2d2238d216'),
                  randomPwd: res.data.randomPwd
                }

                this.showUserPwd = true
              } else {
                $notification.success({
                  message: res.msg
                })
              }

              this.editUserVisible = false
              this.loadData()
            }
          })
          .finally(() => {
            this.confirmLoading = false
          })
      })
    },
    // 删除用户
    handleDelete(record) {
      $confirm({
        title: this.$t('i18n_c4535759ee'),
        content: this.$t('i18n_45f8d5a21d'),
        zIndex: 1009,
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
        onOk: () => {
          return deleteUser(record.id).then((res) => {
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
    // 解锁
    handleUnlock(record) {
      $confirm({
        title: this.$t('i18n_c4535759ee'),
        content: this.$t('i18n_bc2f1beb44'),
        zIndex: 1009,
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
        onOk: () => {
          return unlockUser(record.id).then((res) => {
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
    },
    //
    checkTipUserName() {
      if (this.temp?.id === 'demo') {
        $confirm({
          title: this.$t('i18n_c4535759ee'),
          zIndex: 1009,
          content: `demo ${this.$t('i18n_a8f44c3188')},${this.$t('i18n_c5f9a96133')}`,
          okText: this.$t('i18n_e83a256e4f'),
          cancelText: this.$t('i18n_625fb26b4b'),

          onCancel: () => {
            this.temp.id = ''
          }
        })
      }
    },
    //
    restUserPwdHander(record) {
      $confirm({
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        content: this.$t('i18n_be2109e5b1'),
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
        onOk: () => {
          return restUserPwd(record.id).then((res) => {
            if (res.code === 200) {
              this.temp = {
                title: this.$t('i18n_2c5b0e86e6'),
                randomPwd: res.data.randomPwd
              }
              this.showUserPwd = true
            }
          })
        }
      })
    }
  }
}
</script>
<style scoped>
/* .filter {
  margin-bottom: 10px;
} */
</style>
