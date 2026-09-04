<template>
  <div>
        <n-card size="small" :body-style="{ padding: '12px' }" style="margin-bottom: 12px">

        <n-space wrap class="search-box">
          <n-input
            v-model:value="listQuery['%name%']"
            :placeholder="$t('i18n_a5d1c511d7')"
            class="search-input-item"
            @press-enter="loadData"
          />
          <n-input
            v-model:value="listQuery['%templateTag%']"
            :placeholder="$t('i18n_16a3a4ed35')"
            class="search-input-item"
            @press-enter="loadData"
          />
          <n-select
            v-model:value="listQuery.fileType"
            filterable
            clearable
            :placeholder="$t('i18n_104000e24a')"
            class="search-input-item"
            :options="[
              { label: $t('i18n_26183c99bf'), value: 1 },
              { label: $t('i18n_28f6e7a67b'), value: 2 }
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
        </n-space>
      
    </n-card>
<n-data-table
      size="medium"
      :data="list"
      :columns="columns"
      bordered
      :pagination="pagination"
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

        <template v-else-if="column.key === 'fileId'">
          <n-tooltip>
            <template #trigger>
              <span class="tw">
                <n-button text style="padding: 0" size="small" @click="handleViewFile(record)">{{
                  (text || '').slice(0, 8)
                }}</n-button>
              </span>
            </template>
            text
          </n-tooltip>
        </template>

        <template v-else-if="column.key === 'fileType'">
          <span v-if="text == 2">{{ $t('i18n_28f6e7a67b') }}</span>
          <span v-else>{{ $t('i18n_26183c99bf') }}</span>
        </template>

        <template v-else-if="column.key === 'operation'">
          <n-space>
            <n-button type="primary" danger size="small" @click="handleDelete(record)">{{
              $t('i18n_2f4aaddde3')
            }}</n-button>
          </n-space>
        </template>
      </template>
    </n-data-table>
  </div>
</template>
<script>
import { listTaskTemplate, deleteTaskTemplate } from '@/api/file-manager/release-task-log'
import { CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY, parseTime } from '@/utils/const'

export default {
  components: {},
  data() {
    return {
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      list: [],
      loading: false,
      temp: {},

      confirmLoading: false,
      columns: [
        {
          title: this.$t('i18n_a5d1c511d7'),
          key: 'name',
          ellipsis: true,
          width: 150
        },
        {
          title: this.$t('i18n_16a3a4ed35'),
          key: 'templateTag',
          ellipsis: true,
          width: 150
        },
        {
          title: this.$t('i18n_104000e24a'),
          key: 'fileType',
          width: '100px',
          ellipsis: true
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
          sorter: true,
          ellipsis: true,
          render: (row) => parseTime(row['modifyTimeMillis']),
          width: '170px'
        },

        {
          title: this.$t('i18n_2b6bc0f293'),
          key: 'operation',
          align: 'center',

          fixed: 'right',
          width: '130px'
        }
      ]
    }
  },
  computed: {
    pagination() {
      return COMPUTED_PAGINATION(this.listQuery)
    }
  },
  mounted() {
    this.loadData()
  },
  methods: {
    CHANGE_PAGE,

    parseTime,

    // 获取命令数据
    loadData(pointerEvent) {
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page
      this.loading = true
      listTaskTemplate(this.listQuery).then((res) => {
        if (200 === res.code) {
          this.list = res.data.result
          this.listQuery.total = res.data.total
        }
        this.loading = false
      })
    },

    //  删除命令
    handleDelete(row) {
      $confirm({
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        content: this.$t('i18n_059b86dbe1'),
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
        onOk: () => {
          return deleteTaskTemplate({
            id: row.id
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
    }
  }
}
</script>
