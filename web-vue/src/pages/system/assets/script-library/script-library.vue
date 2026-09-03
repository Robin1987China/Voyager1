<template>
  <div>
    <!-- 数据表格 -->
    <CustomTable
      is-show-tools
      default-auto-refresh
      :auto-refresh-time="30"
      :active-page="activePage"
      table-name="script-library"
      :empty-description="$t('i18n_ef9c90d393')"
      :data="list"
      size="medium"
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
          <n-button type="primary" @click="createScript">{{ $t('i18n_d9ac9228e8') }}</n-button>
        </n-space>
      </template>
      <template #tableHelp>
        <n-tooltip>
          <template #trigger>
            <QuestionCircleOutlined />
          </template>

          <div>{{ $t('i18n_5936ed11ab') }}</div>

          <div>
            <ul>
              <li>{{ $t('i18n_fd93f7f3d7') }}</li>
              <li>
                {{ $t('i18n_beafc90157') }}
              </li>
              <li>
                {{ $t('i18n_3f1d478da4') }}
              </li>
            </ul>
          </div>
        </n-tooltip>
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
            <n-button size="small" type="primary" @click="handleEdit(record)">{{ $t('i18n_95b351c862') }}</n-button>
            <n-button size="small" type="primary" danger @click="handleDelete(record)">{{
              $t('i18n_2f4aaddde3')
            }}</n-button>
          </n-space>
        </template>
      </template>
    </CustomTable>
    <!-- pages.system.assets.script-library.ad207008区 -->
    <CustomModal
      v-if="editScriptVisible"
      v-model:open="editScriptVisible"
      destroy-on-close
      :title="$t('i18n_f038f48ce5')"
      :mask-closable="false"
      width="80vw"
      :confirm-loading="confirmLoading"
      @ok="handleEditScriptOk"
    >
      <n-form ref="editScriptForm" :rules="rules" :model="temp">
        <n-form-item v-if="temp.id" :label="$t('i18n_fe2df04a16')" path="id">
          <n-input v-model:value="temp.version" disabled read-only />
        </n-form-item>
        <n-form-item :label="$t('i18n_deea5221aa')" path="tag">
          <n-input
            v-model:value="temp.tag"
            :max-length="50"
            :placeholder="$t('i18n_8c4db236e1')"
            :disabled="!!temp.id"
          />
        </n-form-item>
        <n-form-item :label="$t('i18n_2d711b09bd')" path="script">
          <n-form-item>
            <code-editor
              v-model:content="temp.script"
              :show-tool="true"
              height="40vh"
              :options="{ mode: 'shell', tabSize: 2 }"
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
            :placeholder="$t('i18n_bd6c436195')"
          />
        </n-form-item>

        <n-form-item>
          <template #label>
            <n-tooltip>
              <template #trigger>
                <span class="tw">
                  {{ $t('i18n_2606b9d0d2') }}
                  <QuestionCircleOutlined v-show="!temp.id" />
                </span>
              </template>
              {{ $t('i18n_73b7b05e6e') }}
            </n-tooltip>
          </template>
          <template #help>{{ $t('i18n_0c2487d394') }}</template>
          <n-select
            v-model:value="temp.chooseNode"
            filterable
            :placeholder="$t('i18n_8e6a77838a')"
            multiple
            :options="nodeList.map((item) => ({ label: item.name, value: item.id }))"
            @keyup.enter="searchMachineList"
          />
        </n-form-item>
      </n-form>
    </CustomModal>
  </div>
</template>
<script>
import { QuestionCircleOutlined } from '@ant-design/icons-vue'

import { getScriptLibraryList, editScriptLibrary, delScriptLibrary } from '@/api/system/script-library'
import codeEditor from '@/components/codeEditor'
import { machineSearch } from '@/api/system/assets-machine'
import { CRON_DATA_SOURCE } from '@/utils/const-i18n'
import { CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY, parseTime } from '@/utils/const'

// import { getWorkSpaceListAll } from '@/api/workspace'

export default {
  components: {
    codeEditor
  },
  props: {},

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

      rules: {
        // name: [{ required: true, message: this.$tl('p.inputScriptName'), trigger: 'blur' }],
        // context: [{ required: true, message: this.$tl('p.inputScriptContent'), trigger: 'blur' }]
      },
      confirmLoading: false
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
    // this.columns.push(
    // );
  },
  mounted() {
    // this.calcTableHeight();

    this.loadData()
  },
  methods: {
    // 加载数据
    loadData(pointerEvent) {
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page
      this.loading = true
      getScriptLibraryList(this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data.result
          this.listQuery.total = res.data.total
        }
        this.loading = false
      })
    },
    parseTime,
    // 获取所有节点
    searchMachineList(name) {
      machineSearch({
        name: name,
        limit: 10,
        appendIds: this.temp.machineIds || ''
      }).then((res) => {
        this.nodeList = res.data || []
      })
    },
    createScript() {
      this.temp = {}

      this.editScriptVisible = true
      this.searchMachineList()
    },
    // 修改
    handleEdit(record) {
      this.temp = Object.assign({}, record)

      //this.commandParams = data?.defArgs ? JSON.parse(data.defArgs) : []

      this.temp = {
        ...this.temp,
        chooseNode: record?.machineIds ? record.machineIds.split(',') : []
      }
      this.editScriptVisible = true
      this.searchMachineList()
      // getScriptItem({
      //   id: record.id
      // }).then((res) => {
      //   if (res.code === 200) {
      //     const data = res.data.data
      //   }
      // })
    },
    // 提交 Script 数据
    handleEditScriptOk() {
      // 检验表单
      this.$refs['editScriptForm'].validate().then(() => {
        // 提交数据
        this.temp.machineIds = this.temp?.chooseNode?.join(',')
        delete this.temp.nodeList
        this.confirmLoading = true
        editScriptLibrary(this.temp)
          .then((res) => {
            if (res.code === 200) {
              // 成功
              $notification.success({
                message: res.msg
              })

              this.editScriptVisible = false
              this.loadData()
              this.$refs['editScriptForm'].resetFields()
            }
          })
          .finally(() => {
            this.confirmLoading = false
          })
      })
    },
    handleDelete(record) {
      $confirm({
        title: this.$t('i18n_c4535759ee'),
        content: this.$t('i18n_a9886f95b6'),
        zIndex: 1009,
        okText: this.$t('i18n_38cf16f220'),
        cancelText: this.$t('i18n_625fb26b4b'),
        onOk: () => {
          return delScriptLibrary({
            id: record.id
          }).then((res) => {
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
