<template>
  <div>
        <n-card size="small" :body-style="{ padding: '12px' }" style="margin-bottom: 12px">

        <n-space wrap class="search-box">
          <n-input
            v-model:value="listQuery['nodeId']"
            placeholder="id"
            class="search-input-item"
            @press-enter="loadData"
          />
          <n-input
            v-model:value="listQuery['nodeName']"
            :placeholder="$t('i18n_d7ec2d3fea')"
            class="search-input-item"
            @press-enter="loadData"
          />
          <n-select
            v-model:value="listQuery['nodeRole']"
            filterable
            clearable
            :placeholder="$t('i18n_464f3d4ea3')"
            class="search-input-item"
            :options="[
              { label: $t('i18n_41e9f0c9c6'), value: 'worker' },
              { label: $t('i18n_a6269ede6c'), value: 'manager' }
            ]"
          />

          <n-button type="primary" :loading="loading" @click="loadData">{{ $t('i18n_e5f71fc31e') }}</n-button>
          <n-statistic format="s" :title="$t('i18n_0f8403d07e')" :value="countdownTime" @finish="loadData">
            <template #suffix>
              <div style="font-size: 12px">{{ $t('i18n_ee6ce96abb') }}</div>
            </template>
          </n-statistic>
        </n-space>
      
    </n-card>
<n-data-table
      :data="list"
      size="medium"
      :columns="columns"
      bordered
      :row-key="(row) => row.id"
      :pagination="false"
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

        <template v-else-if="column.key === 'hostname'">
          <n-popover placement="top-start">
            <template #trigger>
              <span class="tw">
                <span class="tw">
                  <span>{{ record.description && record.description.hostname }}</span>
                </span>
              </span>
            </template>
            <template #header>{{
              `${$t('i18n_07a0e44145')}${record.description && record.description.hostname}`
            }}</template>

            <p>
              {{ $t('i18n_a472019766') }}: <n-tag>{{ record.id }}</n-tag>
            </p>
            <template v-if="record.description && record.description.resources">
              <p>
                nanoCPUs:
                <n-tag>{{ record.description.resources.nanoCPUs }}</n-tag>
              </p>
              <p>
                memoryBytes:
                <n-tag>{{ record.description.resources.memoryBytes }}</n-tag>
              </p>
            </template>
            <template v-if="record.description && record.description.engine">
              <p>
                {{ $t('i18n_fe2df04a16') }}:
                <n-tag>{{ record.description.engine.engineVersion }}</n-tag>
              </p>
            </template>
          </n-popover>
        </template>

        <template v-else-if="column.key === 'state'">
          <n-tooltip placement="top-start">
            <template #trigger>
              <span class="tw">
                <n-tag
                  :color="
                    (record.spec && record.spec.availability) === 'ACTIVE' &&
                    record.status &&
                    record.status.state === 'READY'
                      ? 'green'
                      : 'red'
                  "
                >
                  {{ record.status && record.status.state }}
                  <template v-if="record.spec">{{ record.spec.availability }}</template>
                </n-tag>
              </span>
            </template>
            {{
              `${$t('i18n_9b3e947cc9')}${record.status && record.status.state} ${$t('i18n_fb91527ce5')}${
                record.spec ? record.spec.availability || '' : ''
              }`
            }}
          </n-tooltip>
        </template>
        <!-- 角色显示 -->
        <template v-else-if="column.key === 'role'">
          <n-tooltip placement="topLeft">
            <template #trigger>
              <span class="tw">
                <n-tag
                  :color="`${record.managerStatus && record.managerStatus.reachability === 'REACHABLE' ? 'green' : ''}`"
                >
                  {{ record.spec && record.spec.role }}
                </n-tag>
              </span>
            </template>
            `${$t('i18n_20f32e1979')}${record.spec && record.spec.role} ${ record.managerStatus &&
            record.managerStatus.reachability === 'REACHABLE' ? $t('i18n_88c5680d0d') +
            record.managerStatus.reachability : '' }`
          </n-tooltip>
        </template>
        <template v-else-if="column.key === 'address'">
          <n-tooltip placement="topLeft">
            <template #trigger>
              {{ record.status && record.status.address }}
            </template>
            record.status && record.status.address
          </n-tooltip>
        </template>

        <template v-else-if="column.key === 'os'">
          <n-tooltip placement="topLeft">
            <template #trigger>
              <span class="tw">
                <span class="tw">
                  <span>
                    <n-tag
                      >{{ record.description && record.description.platform && record.description.platform.os }}-{{
                        record.description && record.description.platform && record.description.platform.architecture
                      }}
                    </n-tag>
                  </span>
                </span>
              </span>
            </template>
            text
          </n-tooltip>
        </template>
        <template v-else-if="column.key === 'updatedAt'">
          <n-tooltip placement="topLeft">
            <template #trigger>
              <span class="tw">
                <span class="tw">
                  <span>
                    {{ text }}
                  </span>
                </span>
              </span>
            </template>
            `${$t('i18n_bf94b97d1a')}${text} ${$t('i18n_312f45014a')}${record.createdAt}`
          </n-tooltip>
        </template>

        <template v-else-if="column.key === 'operation'">
          <n-space>
            <template v-if="record.managerStatus && record.managerStatus.leader">
              <n-button size="small" type="primary" @click="handleEdit(record)">{{ $t('i18n_8347a927c0') }}</n-button>
              <n-tooltip>
                <template #trigger>
                  <span class="tw">
                    <n-button size="small" type="primary" danger :disabled="true">{{ $t('i18n_b3b1f709d4') }}</n-button>
                  </span>
                </template>
                $t('i18n_28c1c35cd9')
              </n-tooltip>
            </template>
            <template v-else>
              <n-button size="small" type="primary" @click="handleEdit(record)">{{ $t('i18n_8347a927c0') }}</n-button>
              <n-button size="small" type="primary" danger @click="handleLeava(record)">{{
                $t('i18n_b3b1f709d4')
              }}</n-button>
            </template>
          </n-space>
        </template>
      </template>
    </n-data-table>
    <!-- 编辑节点 -->
    <CustomModal
      v-if="editVisible"
      v-model:open="editVisible"
      destroy-on-close
      :confirm-loading="confirmLoading"
      :title="$t('i18n_61e7fa1227')"
      :mask-closable="false"
      @ok="handleEditOk"
    >
      <n-form ref="editForm" :rules="rules" :model="temp">
        <n-form-item :label="$t('i18n_464f3d4ea3')" path="role">
          <n-radio-group v-model:value="temp.role" name="role" :disabled="temp.leader">
            <n-radio value="WORKER"> {{ $t('i18n_41e9f0c9c6') }}</n-radio>
            <n-radio value="MANAGER"> {{ $t('i18n_a6269ede6c') }} </n-radio>
          </n-radio-group>
        </n-form-item>
        <n-form-item :label="$t('i18n_3fea7ca76c')" path="availability">
          <n-radio-group v-model:value="temp.availability" name="availability">
            <n-radio value="ACTIVE"> {{ $t('i18n_fe32def462') }}</n-radio>
            <n-radio value="PAUSE"> {{ $t('i18n_8d63ef388e') }} </n-radio>
            <n-radio value="DRAIN"> {{ $t('i18n_f113c10ade') }} </n-radio>
          </n-radio-group>
        </n-form-item>
      </n-form>
    </CustomModal>
  </div>
</template>
<script>
import { dockerSwarmNodeList, dockerSwarmNodeUpdate } from '@/api/docker-swarm'
import { dockerSwarmNodeLeave } from '@/api/system/assets-docker'

export default {
  components: {},
  props: {
    id: {
      type: String,
      default: ''
    },
    visible: {
      type: Boolean,
      default: false
    },
    urlPrefix: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      loading: false,
      listQuery: {},
      list: [],
      temp: {},
      editVisible: false,
      initSwarmVisible: false,
      rules: {
        role: [{ required: true, message: this.$t('i18n_9d7d471b77'), trigger: 'blur' }],
        availability: [{ required: true, message: this.$t('i18n_4c7c58b208'), trigger: 'blur' }]
      },

      columns: [
        {
          title: this.$t('i18n_faaadc447b'),
          width: 80,
          ellipsis: true,
          align: 'center',
          render: (row, index) => `${index + 1}`
        },
        // { title: "节点Id", key: "id", ellipsis: true, },
        {
          title: this.$t('i18n_6707667676'),
          key: 'hostname',
          ellipsis: true
        },
        {
          title: this.$t('i18n_c1786d9e11'),
          width: 150,
          key: 'address',
          ellipsis: true
        },
        {
          title: this.$t('i18n_3fea7ca76c'),
          width: 140,
          key: 'state',
          ellipsis: true
        },
        {
          title: this.$t('i18n_464f3d4ea3'),
          width: 110,
          key: 'role',
          ellipsis: true
        },

        {
          title: this.$t('i18n_996dc32a98'),
          width: 140,
          align: 'center',
          key: 'os',
          ellipsis: true
        },
        // {
        //   title: "资源",
        //   key: "description.resources",
        //   ellipsis: true,

        //   width: 170,
        // },
        {
          title: this.$t('i18n_1303e638b5'),
          key: 'updatedAt',

          ellipsis: true,

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

      countdownTime: Date.now(),
      confirmLoading: false
    }
  },
  computed: {},
  beforeUnmount() {},
  mounted() {
    this.loadData()
  },
  methods: {
    // 加载数据
    loadData() {
      if (!this.visible) {
        return
      }
      this.loading = true

      this.listQuery.id = this.id
      dockerSwarmNodeList(this.urlPrefix, this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data
        }
        this.loading = false
        this.countdownTime = Date.now() + 5 * 1000
      })
    },
    handleEdit(record) {
      this.editVisible = true
      this.temp = {
        nodeId: record.id,
        role: record.spec.role,
        availability: record.spec.availability,
        leader: record.managerStatus && record.managerStatus.leader
      }
    },
    handleEditOk() {
      this.$refs['editForm'].validate().then(() => {
        this.temp.id = this.id
        this.confirmLoading = true
        dockerSwarmNodeUpdate(this.urlPrefix, this.temp)
          .then((res) => {
            if (res.code === 200) {
              // 成功
              $notification.success({
                message: res.msg
              })
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
    handleLeava(record) {
      $confirm({
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        content: this.$t('i18n_f5399c620e'),
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
        onOk: () => {
          return dockerSwarmNodeLeave({
            nodeId: record.id,
            id: this.id
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
<style scoped>
:deep(.n-statistic .n-statistic-value__content),
:deep(.n-statistic .n-statistic-value__prefix),
:deep(.n-statistic .n-statistic-value__suffix) {
  font-size: 16px;
}
</style>
