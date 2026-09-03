<template>
  <div>
    <!-- 数据表格 -->
    <CustomTable
      is-show-tools
      default-auto-refresh
      :auto-refresh-time="30"
      :active-page="activePage"
      table-name="script-library-no-permission"
      :empty-description="$t('i18n_824914133f')"
      :data="list"
      size="medium"
      :columns="columns"
      :pagination="pagination"
      bordered
      row-key="id"
      :scroll="{
        x: 'max-content'
      }"
      :row-selection="rowSelection"
      @change="changePage"
      @refresh="loadData"
    >
      <template #title>
        <n-space wrap class="search-box">
          <n-input
            v-model:value="listQuery['%tag%']"
            :placeholder="$t('i18n_e17a6882b6')"
            clearable
            class="search-input-item"
            @press-enter="loadData"
          />
          <n-input
            v-model:value="listQuery['%version%']"
            :placeholder="$t('i18n_fe2df04a16')"
            clearable
            class="search-input-item"
            @press-enter="loadData"
          />
          <n-input
            v-model:value="listQuery['%description%']"
            :placeholder="$t('i18n_3bdd08adab')"
            class="search-input-item"
            @press-enter="loadData"
          />

          <n-tooltip>
            <template #trigger>
              <span class="tw">
                <n-button :loading="loading" type="primary" @click="loadData">{{ $t('i18n_e5f71fc31e') }}</n-button>
              </span>
            </template>
            $t('i18n_4838a3bd20')
          </n-tooltip>
        </n-space>
      </template>

      <template #tableBodyCell="{ column, text, record }">
        <template v-if="column.dataIndex === 'nodeId'">
          <n-tooltip placement="topLeft">
            <template #trigger>
              <span class="tw">
                <span class="tw">
                  <span>{{ nodeMap[text] }}</span>
                </span>
              </span>
            </template>
            text
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

        <template v-else-if="column.dataIndex === 'operation'">
          <n-space>
            <n-button size="small" type="primary" @click="handleEdit(record)">{{ $t('i18n_a156349591') }}</n-button>
          </n-space>
        </template>
      </template>
    </CustomTable>
    <!-- pages.system.assets.script-library.ad207008区 -->
    <CustomModal
      v-if="editScriptVisible"
      v-model:open="editScriptVisible"
      destroy-on-close
      :title="$t('i18n_dd1d14efd6')"
      :mask-closable="false"
      width="80vw"
      :footer="false"
    >
      <n-form ref="editScriptForm" :rules="rules" :model="temp">
        <n-form-item :label="$t('i18n_fe2df04a16')" path="id">
          <n-input v-model:value="temp.version" disabled read-only />
        </n-form-item>
        <n-form-item :label="$t('i18n_deea5221aa')" path="tag">
          <n-input v-model:value="temp.tag" :max-length="50" disabled />
        </n-form-item>
        <n-form-item :label="$t('i18n_2d711b09bd')" path="script">
          <n-form-item>
            <code-editor
              v-model:content="temp.script"
              :show-tool="true"
              height="40vh"
              :options="{ mode: 'shell', tabSize: 2, readOnly: true }"
            >
            </code-editor>
          </n-form-item>
        </n-form-item>

        <n-form-item :label="$t('i18n_3bdd08adab')" path="description">
          <n-input
            v-model:value="temp.description"
            type="textarea"
            :max-length="200"
            :rows="3"
            style="resize: none"
            disabled
          />
        </n-form-item>
      </n-form>
    </CustomModal>
  </div>
</template>
<script>
import { getScriptLibraryNoPermissionList } from '@/api/system/script-library'
import codeEditor from '@/components/codeEditor'

import { CRON_DATA_SOURCE } from '@/utils/const-i18n'
import { CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY, parseTime } from '@/utils/const'
import { increaseZIndex } from '@/utils/utils'
// import { getWorkSpaceListAll } from '@/api/workspace'

export default {
  components: {
    codeEditor
  },
  props: {},
  emits: ['scriptConfirm', 'tagConfirm'],

  data() {
    return {
      // choose: this.choose,
      loading: false,
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      CRON_DATA_SOURCE,
      list: [],
      temp: {},
      nodeList: [],
      editScriptVisible: false,
      drawerTitle: '',
      drawerConsoleVisible: false,
      columns: [
        {
          title: this.$t('i18n_deea5221aa'),
          key: 'tag',
          ellipsis: true,
          sorter: true,
          width: 150
        },
        {
          title: this.$t('i18n_fe2df04a16'),
          key: 'version',
          ellipsis: true,
          sorter: true,
          width: '100px'
        },
        {
          title: this.$t('i18n_3bdd08adab'),
          key: 'description',
          ellipsis: true,
          width: 200
        },
        {
          title: this.$t('i18n_1303e638b5'),
          key: 'modifyTimeMillis',
          sorter: true,
          width: '170px',
          ellipsis: true,
          render: (row) => parseTime(row['modifyTimeMillis'])
        },
        {
          title: this.$t('i18n_eca37cb072'),
          key: 'createTimeMillis',
          sorter: true,
          width: '170px',
          ellipsis: true,
          render: (row) => parseTime(row['createTimeMillis'])
        },
        {
          title: this.$t('i18n_95a43eaa59'),
          key: 'createUser',
          ellipsis: true,
          width: '120px'
        },
        {
          title: this.$t('i18n_9baca0054e'),
          key: 'modifyUser',
          ellipsis: true,
          width: '120px'
        },

        {
          title: this.$t('i18n_2b6bc0f293'),
          key: 'operation',
          align: 'center',

          fixed: 'right',
          width: '140px'
        }
      ],

      tableSelections: [],
      selectedRowKeys: [],
      rules: {}
    }
  },
  computed: {
    pagination() {
      return COMPUTED_PAGINATION(this.listQuery)
    },
    activePage() {
      return this.$attrs.routerUrl === this.$route.path
    },
    rowSelection() {
      return {
        onChange: (selectedRowKeys) => {
          this.tableSelections = selectedRowKeys
        },
        selectedRowKeys: this.tableSelections,
        type: 'radio'
      }
    }
  },
  watch: {},
  created() {},
  mounted() {
    this.loadData()
  },
  methods: {
    increaseZIndex,
    // 加载数据
    loadData(pointerEvent) {
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page
      this.loading = true
      getScriptLibraryNoPermissionList(this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data.result
          this.listQuery.total = res.data.total
        }
        this.loading = false
      })
    },
    parseTime,
    // 修改
    handleEdit(record) {
      this.temp = Object.assign({}, record)
      this.editScriptVisible = true
    },
    // 分页、排序、筛选变化时触发
    changePage(pagination, filters, sorter) {
      this.listQuery = CHANGE_PAGE(this.listQuery, { pagination, sorter })
      this.loadData()
    },
    handerScriptConfirm() {
      if (!this.tableSelections.length) {
        $notification.warning({
          message: this.$t('i18n_364bea440e')
        })
        return
      }
      const selectData = this.list.filter((item) => {
        return item.id === this.tableSelections[0]
      })?.[0]
      this.$emit('scriptConfirm', `${selectData.script}`)
    },
    handerTagConfirm() {
      if (!this.tableSelections.length) {
        $notification.warning({
          message: this.$t('i18n_364bea440e')
        })
        return
      }
      const selectData = this.list.filter((item) => {
        return item.id === this.tableSelections[0]
      })?.[0]
      this.$emit('tagConfirm', `${selectData.tag}`)
    }
  }
}
</script>
