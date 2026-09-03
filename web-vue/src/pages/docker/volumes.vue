<template>
  <n-data-table size="medium" :data="list" :columns="columns" :pagination="false" bordered :scroll-x="'max-content'">
    <template #title>
      <n-space>
        <n-input
          v-model:value="listQuery['name']"
          :placeholder="$t('i18n_d7ec2d3fea')"
          class="search-input-item"
          @press-enter="loadData"
        />

        <div>
          {{ $t('i18n_a09375d96c') }}
          <n-switch
            v-model:value="listQuery['dangling']"
            :checked-label="$t('i18n_0a60ac8f02')"
            :unchecked-label="$t('i18n_c9744f45e7')"
          />
        </div>

        <n-button type="primary" :loading="loading" @click="loadData">{{ $t('i18n_e5f71fc31e') }}</n-button>
      </n-space>
    </template>
    <template #bodyCell="{ column, text, record }">
      <template v-if="column.dataIndex === 'CreatedAt'">
        <n-tooltip placement="topLeft">
          <template #trigger>
            <span class="tw">
              <span class="tw">
                <span>{{ parseTime(record.rawValues && record.rawValues['CreatedAt']) }}</span>
              </span>
            </span>
          </template>
          record.rawValues && record.rawValues['CreatedAt']
        </n-tooltip>
      </template>

      <template v-else-if="column.dataIndex === 'name'">
        <n-popover v-if="record.labels">
          <template #trigger>
            <PushpinOutlined />
          </template>
          <template #header>{{ $t('i18n_3a3ff2c936') }}</template>

          <p v-for="(value, key) in record.labels" :key="key">{{ key }}<ArrowRightOutlined />{{ value }}</p>
        </n-popover>

        <n-tooltip>
          <template #trigger>
            {{ text }}
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

      <template v-else-if="column.id">
        <n-tooltip>
          <template #trigger>
            <span class="tw">
              <span class="tw">
                <span> {{ text.split(':')[1].slice(0, 12) }}</span>
              </span>
            </span>
          </template>
          text
        </n-tooltip>
      </template>
      <template v-else-if="column.dataIndex === 'operation'">
        <n-space>
          <n-tooltip>
            <template #trigger>
              <span class="tw">
                <n-button size="small" text @click="doAction(record, 'remove')"><DeleteOutlined /></n-button>
              </span>
            </template>
            $t('i18n_2f4aaddde3')
          </n-tooltip>
        </n-space>
      </template>
    </template>
  </n-data-table>
</template>
<script>
import { ArrowRightOutlined, DeleteOutlined, PushpinOutlined } from '@ant-design/icons-vue'

import { renderSize, parseTime } from '@/utils/const'
import { dockerVolumesList, dockerVolumesRemove } from '@/api/docker-api'
export default {
  props: {
    id: {
      type: String,
      default: ''
    },
    machineDockerId: {
      type: String,
      default: ''
    },
    urlPrefix: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      list: [],
      loading: false,
      listQuery: {
        dangling: false
      },
      renderSize,
      columns: [
        {
          title: this.$t('i18n_faaadc447b'),
          width: 80,
          ellipsis: true,
          align: 'center',
          render: (row, index) => `${index + 1}`
        },
        {
          title: this.$t('i18n_d7ec2d3fea'),
          key: 'name',
          ellipsis: true
        },
        {
          title: this.$t('i18n_1c61dfb86f'),
          key: 'mountpoint',
          ellipsis: true
        },
        {
          title: this.$t('i18n_226b091218'),
          key: 'driver',
          ellipsis: true,
          width: 80
        },
        {
          title: this.$t('i18n_eca37cb072'),
          key: 'CreatedAt',
          ellipsis: true,
          width: 180,
          sorter: (a, b) => new Date(a.rawValues.CreatedAt).getTime() - new Date(b.rawValues.CreatedAt).getTime(),
          defaultSortOrder: 'descend'
        },
        {
          title: this.$t('i18n_2b6bc0f293'),
          key: 'operation',
          fixed: 'right',
          width: '80px'
        }
      ],

      action: {
        remove: {
          msg: this.$t('i18n_022b6ea624'),
          api: dockerVolumesRemove
        }
      }
    }
  },
  computed: {
    reqDataId() {
      return this.id || this.machineDockerId
    }
  },
  mounted() {
    this.loadData()
  },
  methods: {
    parseTime,
    // 加载数据
    loadData() {
      this.loading = true
      //this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page;
      this.listQuery.id = this.reqDataId
      dockerVolumesList(this.urlPrefix, this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data
        }
        this.loading = false
      })
    },
    doAction(record, actionKey) {
      const action = this.action[actionKey]
      if (!action) {
        return
      }
      $confirm({
        title: this.$t('i18n_c4535759ee'),
        content: action.msg,
        zIndex: 1009,
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
        onOk: () => {
          return action
            .api(this.urlPrefix, {
              id: this.reqDataId,
              volumeName: record.name
            })
            .then((res) => {
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
