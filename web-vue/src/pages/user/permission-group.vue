<template>
  <div>
    <!-- 数据表格 -->
        <n-card size="small" :body-style="{ padding: '12px' }" style="margin-bottom: 12px">

        <n-space wrap class="search-box">
          <n-input
            v-model:value="listQuery['%name%']"
            :placeholder="$t('i18n_d7ec2d3fea')"
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
        </n-space>
      
    </n-card>
<n-data-table
      :data="list"
      size="medium"
      :columns="columns"
      :pagination="pagination"
      bordered
      :row-key="(row) => row.id"
      @change="changePage"
    >
      
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'operation'">
          <n-space>
            <n-button size="small" type="primary" @click="handleEdit(record)">{{ $t('i18n_95b351c862') }}</n-button>
            <n-button type="primary" danger size="small" @click="handleDelete(record)">{{
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
      width="60vw"
      :title="$t('i18n_95b351c862')"
      :mask-closable="false"
      @ok="handleEditUserOk"
    >
      <n-form ref="editForm" :rules="rules" :model="temp">
        <n-form-item :label="$t('i18n_d7ec2d3fea')" path="name">
          <n-input v-model:value="temp.name" :max-length="50" :placeholder="$t('i18n_d7ec2d3fea')" />
        </n-form-item>
        <n-form-item path="workspace">
          <template #label>
            <n-tooltip>
              <template #trigger>
                <span class="tw">
                  {{ $t('i18n_98d69f8b62') }}

                  <QuestionCircleOutlined v-if="!temp.id" />
                </span>
              </template>
              {{ $t('i18n_9a8eb63daf') }},{{ $t('i18n_85f347f9d0') }}
            </n-tooltip>
          </template>
          <transfer ref="transferRef" :tree-data="workspaceList" :edit-key="temp.targetKeys" />
        </n-form-item>
        <n-form-item path="prohibitExecute">
          <template #label>
            <n-tooltip>
              <template #trigger>
                <span class="tw">
                  {{ $t('i18n_2ae22500c7') }}

                  <QuestionCircleOutlined v-if="!temp.id" />
                </span>
              </template>
              {{ $t('i18n_b56585aa18') }}
            </n-tooltip>
          </template>
          <n-form-item>
            <n-space direction="vertical" style="width: 100%">
              <div v-for="(item, index) in temp.prohibitExecuteArray" :key="item.key">
                <n-space direction="vertical" class="item-info" style="width: 100%">
                  <n-date-picker
                    v-model:formatted-value="item.moments"
                    type="datetimerange"
                    style="width: 100%"
                    :is-date-disabled="
                      (ts) => {
                        const current = dayjs(ts)
                        if (current < dayjs().subtract(1, 'days')) {
                          return true
                        }

                        return temp.prohibitExecuteArray.filter((arrayItem, arrayIndex) => {
                          if (arrayIndex === index) {
                            return false
                          }
                          if (arrayItem.moments && arrayItem.moments.length === 2) {
                            if (current > arrayItem.moments[0] && current < arrayItem.moments[1]) {
                              return true
                            }
                          }
                          return false
                        }).length
                      }
                    "
                    format="yyyy-MM-dd HH:mm:ss"
                    value-format="yyyy-MM-dd HH:mm:ss"
                    :placeholder="$t('i18n_592c595891')"
                    clearable
                  />

                  <div>
                    <n-input v-model:value="item.reason" :placeholder="$t('i18n_1eba2d93fc')" clearable />
                  </div>
                </n-space>

                <div
                  class="item-icon"
                  @click="
                    () => {
                      temp.prohibitExecuteArray.splice(index, 1)
                    }
                  "
                >
                  <MinusCircleOutlined style="color: #ff0000" />
                </div>
              </div>
            </n-space>
            <n-button
              type="primary"
              @click="
                () => {
                  temp.prohibitExecuteArray.push({})
                }
              "
              >{{ $t('i18n_66ab5e9f24') }}
            </n-button>
          </n-form-item>
        </n-form-item>
        <n-form-item path="allowExecute">
          <template #label>
            <n-tooltip>
              <template #trigger>
                <span class="tw">
                  {{ $t('i18n_ef7e3377a0') }}

                  <QuestionCircleOutlined v-if="!temp.id" />
                </span>
              </template>
              {{ $t('i18n_21e4f10399') }},{{ $t('i18n_4c69102fe1') }}
            </n-tooltip>
          </template>
          <n-form-item>
            <n-space direction="vertical" style="width: 100%">
              <div v-for="(item, index) in temp.allowExecuteArray" :key="item.key">
                <n-space direction="vertical" class="item-info" style="width: 100%">
                  <div>
                    <n-select
                      v-model:value="item.week"
                      :placeholder="$t('i18n_d5c2351c0e')"
                      multiple
                      style="width: 100%"
                      :options="weeks.map((weekItem) => ({ label: weekItem.name, value: weekItem.value }))"
                    />
                  </div>
                  <div>
                    <n-space>
                      <n-time-picker
                        v-model:formatted-value="item.startTime"
                        :placeholder="$t('i18n_592c595891')"
                        value-format="HH:mm:ss"
                        format="HH:mm:ss"
                      />
                      <n-time-picker
                        v-model:formatted-value="item.endTime"
                        :placeholder="$t('i18n_f782779e8b')"
                        value-format="HH:mm:ss"
                        format="HH:mm:ss"
                      />
                    </n-space>
                  </div>
                </n-space>
                <div
                  class="item-icon"
                  @click="
                    () => {
                      temp.allowExecuteArray.splice(index, 1)
                    }
                  "
                >
                  <MinusCircleOutlined style="color: #ff0000" />
                </div>
              </div>
            </n-space>
            <n-button
              type="primary"
              @click="
                () => {
                  temp.allowExecuteArray.push({})
                }
              "
              >{{ $t('i18n_66ab5e9f24') }}
            </n-button>
          </n-form-item>
        </n-form-item>

        <n-form-item :label="$t('i18n_3bdd08adab')" path="description">
          <n-input
            v-model:value="temp.description"
            type="textarea"
            :max-length="200"
            :rows="5"
            :placeholder="$t('i18n_3bdd08adab')"
          />
        </n-form-item>
      </n-form>
    </CustomModal>
  </div>
</template>
<script>
import { MinusCircleOutlined, QuestionCircleOutlined } from '@ant-design/icons-vue'

import { workspaceList } from '@/api/user/user'
import { getList, editPermissionGroup, deletePermissionGroup } from '@/api/user/user-permission'
import { getWorkSpaceListAll } from '@/api/workspace'
import { getMonitorOperateTypeList } from '@/api/monitor'
import dayjs from 'dayjs'
import { CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY, parseTime } from '@/utils/const'
import Transfer from '@/components/compositionTransfer/composition-transfer.vue'

export default {
  components: {
    Transfer
  },
  data() {
    return {
      loading: false,
      list: [],
      workspaceList: [],

      methodFeature: [],
      temp: {},
      weeks: [
        { value: 1, name: this.$t('i18n_1603b069c2') },
        { value: 2, name: this.$t('i18n_b5a6a07e48') },
        { value: 3, name: this.$t('i18n_e60725e762') },
        { value: 4, name: this.$t('i18n_170fc8e27c') },
        { value: 5, name: this.$t('i18n_eb79cea638') },
        { value: 6, name: this.$t('i18n_2457513054') },
        { value: 7, name: this.$t('i18n_562d7476ab') }
      ],

      editVisible: false,
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      columns: [
        { title: this.$t('i18n_d7ec2d3fea'), key: 'name', ellipsis: true },
        { title: this.$t('i18n_3bdd08adab'), key: 'description', ellipsis: true },

        {
          title: this.$t('i18n_9baca0054e'),
          key: 'modifyUser',
          ellipsis: true,
          width: 150
        },
        {
          title: this.$t('i18n_1303e638b5'),
          key: 'modifyTimeMillis',
          sorter: true,
          ellipsis: true,
          render: (row) => {
            return parseTime(row['modifyTimeMillis'])
          },
          width: 170
        },
        {
          title: this.$t('i18n_2b6bc0f293'),
          align: 'center',
          key: 'operation',

          width: 120
        }
      ],

      // 表单校验规则
      rules: {
        name: [{ required: true, message: this.$t('i18n_4482773688'), trigger: 'blur' }]
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
    dayjs,
    // 加载数据
    loadData(pointerEvent) {
      this.loading = true
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page
      getList(this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data.result
          this.listQuery.total = res.data.total
        }
        this.loading = false
      })
    },
    // 加载工作空间数据
    loadWorkSpaceListAll() {
      return new Promise((callback) => {
        this.workspaceList = []
        getWorkSpaceListAll().then((res) => {
          if (res.code === 200) {
            res.data.forEach((element) => {
              const children = this.methodFeature.map((item) => {
                return {
                  key: element.id + '-' + item.value,
                  title: item.title + this.$t('i18n_ba6e91fa9e'),
                  parentId: element.id
                }
              })
              children.push({
                key: element.id + '-sshCommandNotLimited',
                title: `SSH ${this.$t('i18n_9dd62c9fa8')}`,
                parentId: element.id
              })
              this.workspaceList.push({
                key: element.id,
                title: element.name,
                children: children,
                parentId: 0
              })
            })
            callback()
          }
        })
      })
    },
    // 加载操作类型数据
    loadOptTypeData() {
      getMonitorOperateTypeList().then((res) => {
        if (res.code === 200) {
          this.methodFeature = res.data.methodFeature
        }
      })
    },

    // 新增权限组
    handleAdd() {
      this.temp = {
        prohibitExecuteArray: [],
        allowExecuteArray: [],
        targetKeys: []
      }

      this.loadWorkSpaceListAll()
      this.editVisible = true
      this.$nextTick(() => {
        this.$refs['editForm'] && this.$refs['editForm'].restoreValidation()
      })
    },
    // 修改权限组
    handleEdit(record) {
      workspaceList(record.id).then((res) => {
        this.loadWorkSpaceListAll().then(() => {
          this.temp = {
            ...record,
            targetKeys: res.data.map((element) => {
              return element.workspaceId
            }),
            prohibitExecuteArray: JSON.parse(record.prohibitExecute).map((item) => {
              return {
                reason: item.reason,
                moments: [item.startTime, item.endTime]
              }
            }),
            allowExecuteArray: JSON.parse(record.allowExecute)
          }
          ;(delete this.temp.prohibitExecute, delete this.temp.allowExecute)
          this.editVisible = true
        })
      })
    },
    // 提交用户数据
    handleEditUserOk() {
      // 检验表单
      this.$refs['editForm'].validate().then(() => {
        const transferRef = this.$refs.transferRef
        const emitKeys = transferRef && transferRef.emitKeys
        const temp = { ...this.temp }
        //
        temp.prohibitExecute = JSON.stringify(
          (temp.prohibitExecuteArray || []).map((item) => {
            return {
              startTime: item.moments && item.moments[0],
              endTime: item.moments && item.moments[1],
              reason: item.reason
            }
          })
        )
        delete temp.prohibitExecuteArray
        //
        temp.allowExecute = JSON.stringify(
          (temp.allowExecuteArray || []).map((item) => {
            return {
              endTime: item.endTime,
              startTime: item.startTime,
              week: item.week
            }
          })
        )
        delete temp.allowExecuteArray
        if (!emitKeys || emitKeys.length <= 0) {
          $notification.error({
            message: this.$t('i18n_b3bda9bf9e')
          })
          return false
        }
        //
        temp.workspace = JSON.stringify(emitKeys)
        delete temp.targetKeys
        // console.log(temp, emitKeys)
        // 需要判断当前操作是【新增】还是【修改】
        this.confirmLoading = true
        editPermissionGroup(temp)
          .then((res) => {
            if (res.code === 200) {
              $notification.success({
                message: res.msg
              })
              this.editVisible = false
              this.$nextTick(() => {
                this.$refs['editForm'] && this.$refs['editForm'].restoreValidation()
              })
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
        content: this.$t('i18n_a52aa984cd'),
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
        onOk: () => {
          return deletePermissionGroup(record.id).then((res) => {
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
    }
  }
}
</script>
<style scoped>
.item-info {
  /* display: inline-block; */
  width: 90%;
}
.item-icon {
  display: inline-block;
  width: 10%;
  text-align: center;
}
</style>
