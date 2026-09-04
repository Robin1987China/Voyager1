<template>
  <div>
    <!-- 数据表格 -->
        <n-card size="small" :body-style="{ padding: '12px' }" style="margin-bottom: 12px">

        <n-space direction="vertical" style="width: 100%">
          <div>
            <template v-for="(val, key) in groupMap" :key="key">
              <span>{{ key }}：</span>
              <template v-for="(tag, index) in val" :key="`${tag.id}_${key}`">
                <n-tag :color="`${index === 0 ? 'blue' : 'orange'}`">
                  {{ tag.name }}
                </n-tag>
              </template>
            </template>
          </div>
          <div v-if="groupList.filter((item) => !groupMap[item]).length">
            {{ $t('i18n_7dde69267a') }}
            <template v-for="(item, index) in groupList">
              <n-tag v-if="!groupMap[item]" :key="index">{{ item }}</n-tag>
            </template>
          </div>
          <n-space>
            <n-input
              v-model:value="listQuery['%name%']"
              :placeholder="$t('i18n_c3f28b34bb')"
              clearable
              class="search-input-item"
              @press-enter="loadData"
            />
            <n-input
              v-model:value="listQuery['%url%']"
              :placeholder="$t('i18n_8a414f832f')"
              clearable
              class="search-input-item"
              @press-enter="loadData"
            />
            <n-input
              v-model:value="listQuery['%localHostName%']"
              :placeholder="$t('i18n_6707667676')"
              clearable
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

            <n-tooltip>
              <template #trigger>
                <QuestionCircleOutlined />
              </template>

              <ul>
                <li>{{ $t('i18n_5177c276a0') }}</li>
                <li>{{ $t('i18n_649d7fcb73') }}</li>
                <li>{{ $t('i18n_9c84cd926b') }}</li>
              </ul>
            </n-tooltip>
          </n-space>
        </n-space>
      
    </n-card>
<n-data-table
      :data="list"
      :columns="columns"
      size="medium"
      :pagination="pagination"
      bordered
      :row-key="(row) => row.id"
      @change="
        (pagination, filters, sorter) => {
          listQuery = CHANGE_PAGE(listQuery, { pagination, sorter })
          loadData()
        }
      "
    >
      
      <template #bodyCell="{ column, text, record }">
        <template v-if="column.tooltip">
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
        <template v-else-if="column.key === 'url'">
          <n-tooltip>
            <template #trigger>
              <span class="tw">
                <span class="tw">
                  <n-button v-if="record.url" text size="small" @click="openUrl(record.url)">
                    {{ text }}
                  </n-button>
                  <span v-else>{{ record.statusMsg }}</span>
                  <!-- -->
                </span>
              </span>
            </template>
            `${$t('i18n_f668c8c881')}${record.name || ''}/${$t('i18n_df3833270b')}${record.url || ''}/${$t(
            'i18n_8d13037eb7' )}${record.statusMsg || ''}`
          </n-tooltip>
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
      v-if="editVisible"
      v-model:open="editVisible"
      destroy-on-close
      :confirm-loading="confirmLoading"
      :title="$t('i18n_8d3d771ab6')"
      :mask-closable="false"
      @ok="handleEditOk"
    >
      <n-form ref="editForm" :rules="rules" :model="temp">
        <n-form-item :label="$t('i18n_d7ec2d3fea')" path="name">
          <n-input v-model:value="temp.name" :max-length="50" :placeholder="$t('i18n_6a588459d0')" />
        </n-form-item>
        <n-form-item :label="$t('i18n_79c6b6cff7')" path="linkGroups">
          <template #help>
            {{ $t('i18n_4089cfb557') }}
            <div style="color: red">{{ $t('i18n_f9898595a0') }}</div>
          </template>
          <n-select
            v-model:value="temp.linkGroups"
            filterable
            multiple
            clearable
            :placeholder="$t('i18n_79c6b6cff7')"
            :options="groupList"
          />
        </n-form-item>
        <n-form-item :label="$t('i18n_8a414f832f')" path="url">
          <template #help> {{ $t('i18n_fcca8452fe') }} </template>
          <n-input v-model:value="temp.url" :placeholder="$t('i18n_8aebf966b2')" />
        </n-form-item>
      </n-form>
    </CustomModal>
  </div>
</template>
<script>
import { QuestionCircleOutlined } from '@ant-design/icons-vue'

import { CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY, parseTime } from '@/utils/const'
import { getClusterList, deleteCluster, listLinkGroups, editCluster } from '@/api/system/cluster'
export default {
  data() {
    return {
      loading: false,
      list: [],
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      columns: [
        {
          title: this.$t('i18n_ed8ea20fe6'),
          key: 'id',
          ellipsis: true,
          width: '100px'
        },
        {
          title: this.$t('i18n_7329a2637c'),
          key: 'clusterId',
          ellipsis: true,
          width: '100px'
        },
        {
          title: this.$t('i18n_d7ec2d3fea'),
          key: 'name',
          ellipsis: true,
          width: 200
        },
        {
          title: this.$t('i18n_8a414f832f'),
          key: 'url',
          ellipsis: true,
          width: 200
        },
        {
          title: this.$t('i18n_24d695c8e2'),
          key: 'localHostName',
          ellipsis: true,
          width: '100px'
        },
        {
          title: this.$t('i18n_d0b2958432'),
          key: 'voyager1Version',
          ellipsis: true,
          width: '100px'
        },
        {
          title: this.$t('i18n_f68f9b1d1b'),
          key: 'lastHeartbeat',
          sorter: true,
          ellipsis: true,
          render: (row) => parseTime(row['lastHeartbeat']),
          width: '170px'
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

          width: '120px'
        }
      ],

      // 表单校验规则
      rules: {
        name: [{ required: true, message: this.$t('i18n_debdfce084'), trigger: 'blur' }],
        linkGroups: [{ required: true, type: 'array', message: this.$t('i18n_3d3d3ed34c'), trigger: ['blur', 'change'] }]
        // url: [{ required: true, message: "请输入集群访问地址", trigger: "blur" }],
      },
      editVisible: false,
      temp: {},
      groupList: [],
      groupMap: {},
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
  },
  methods: {
    parseTime,
    CHANGE_PAGE,
    // 加载数据
    loadData(pointerEvent) {
      this.loading = true
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page
      getClusterList(this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data.result
          this.listQuery.total = res.data.total
        }
        this.loading = false
      })
    },
    // 删除
    handleDelete(record) {
      $confirm({
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        content: this.$t('i18n_2e0094d663'),
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
        onOk: () => {
          return deleteCluster(record.id).then((res) => {
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
    // 获取所有的分组
    loadGroupList() {
      listLinkGroups().then((res) => {
        if (res.data) {
          this.groupList = res.data.linkGroups || []
          this.groupMap = res.data.groupMap || {}
        }
      })
    },
    // 编辑
    handleEdit(record) {
      this.loadGroupList()
      this.temp = Object.assign({}, record, {
        linkGroups: (record.linkGroup || '').split(',').filter((item) => item)
      })
      this.editVisible = true
    },
    handleEditOk() {
      this.$refs['editForm'].validate().then(() => {
        const newData = { ...this.temp }
        const linkGroups = newData.linkGroups
        if (!linkGroups) {
          $notification.error({
            message: this.$t('i18n_e0d6976b48')
          })
          return false
        }
        delete newData.linkGroups
        newData.linkGroup = linkGroups.join(',')
        this.confirmLoading = true
        editCluster(newData)
          .then((res) => {
            if (res.code === 200) {
              // 成功
              $notification.success({
                message: res.msg
              })
              this.$refs['editForm'].restoreValidation()
              this.editVisible = false
              this.loadData()
            }
          })
          .finally(() => {
            this.confirmLoading = false
          })
      }).catch(() => {})
    },
    //
    openUrl(url) {
      window.open(url)
    }
  }
}
</script>
