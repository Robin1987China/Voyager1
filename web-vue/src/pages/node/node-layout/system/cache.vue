<template>
  <div class="">
    <n-tabs default-active-key="1" tab-position="left">
      <n-tab-pane name="1" :tab="$t('i18n_3c6248b364')">
        <n-alert :title="$t('i18n_5785f004ea')" style="margin-top: 10px; margin-bottom: 40px" banner />
        <n-timeline>
          <n-timeline-item v-if="temp.dateTime">
            <span>
              {{ $t('i18n_9ddaa182bd') }}{{ temp.dateTime }}
              <n-tag>{{ temp.timeZoneId }}</n-tag>
            </span>
          </n-timeline-item>
          <n-timeline-item>
            <span>{{ $t('i18n_37f1931729') }}{{ renderSize(temp.dataSize) }}</span>
          </n-timeline-item>
          <n-timeline-item v-if="temp.fileSize">
            <n-space>
              <span>{{ $t('i18n_ea3c5c0d25') }}{{ renderSize(temp.fileSize) }}</span>
              <n-button size="small" type="primary" class="btn" @click="clear('fileSize')">{{
                $t('i18n_288f0c404c')
              }}</n-button>
            </n-space>
          </n-timeline-item>
          <n-timeline-item v-if="temp.oldJarsSize">
            <n-space>
              <span>{{ $t('i18n_413d8ba722') }}{{ renderSize(temp.oldJarsSize) }}</span>
              <n-button size="small" type="primary" class="btn" @click="clear('oldJarsSize')">{{
                $t('i18n_288f0c404c')
              }}</n-button>
            </n-space>
          </n-timeline-item>

          <n-timeline-item>
            <n-space>
              <span>{{ $t('i18n_775fde44cf') }}{{ temp.pidPort }}</span>
              <n-button v-if="temp.pidPort" size="small" type="primary" class="btn" @click="clear('pidPort')">{{
                $t('i18n_288f0c404c')
              }}</n-button>
            </n-space>
          </n-timeline-item>
          <n-timeline-item>
            <span>{{ $t('i18n_25f29ebbe6') }}{{ temp.scriptExecLogSize }}</span>
          </n-timeline-item>
          <n-timeline-item>
            <span>{{ $t('i18n_9b9e426d16') }}{{ temp.readFileOnLineCount }}</span>
          </n-timeline-item>
          <n-timeline-item>
            <span>{{ $t('i18n_b6ee682dac') }}{{ temp.pluginSize || 0 }}</span>
          </n-timeline-item>
          <n-timeline-item>
            <div>
              {{ $t('i18n_4ab578f3df') }}
              <template v-if="temp.envVarKeys?.length">
                <n-tag v-for="(item, index) in temp.envVarKeys" :key="index">
                  <n-tooltip>
                    <template #trigger>
                      {{ item }}
                    </template>
                    `${$t('i18n_6835ed12b9')}:${item}`
                  </n-tooltip>
                </n-tag>
              </template>
              <template v-else>-</template>
            </div>
          </n-timeline-item>
          <n-timeline-item>
            <div>
              {{ $t('i18n_eb7f9ceb71')
              }}<template v-if="temp.scriptLibraryTagMap && Object.keys(temp.scriptLibraryTagMap).length">
                <n-tag v-for="(item, key) in temp.scriptLibraryTagMap" :key="key">
                  <n-tooltip>
                    <template #trigger>
                      {{ key }}
                    </template>
                    $t('i18n_3a57a51660', { item: item })
                  </n-tooltip>
                </n-tag>
              </template>
              <template v-else>-</template>
            </div>
          </n-timeline-item>
        </n-timeline>
      </n-tab-pane>
      <n-tab-pane name="2" :tab="$t('i18n_a1bd9760fc')">
        <task-stat :task-list="taskList" @refresh="loadData"
      /></n-tab-pane>
      <n-tab-pane name="3" :tab="$t('i18n_f06f95f8e6')">
        <n-space direction="vertical" style="width: 100%">
          <n-alert :title="$t('i18n_406a2b3538')" type="warning" show-icon>
            <template #description>
              <ul>
                <li>{{ $t('i18n_17a101c23e') }}</li>
                <li>{{ $t('i18n_3929e500e0') }}</li>
                <li>{{ $t('i18n_0a47f12ef2') }}</li>
                <li>{{ $t('i18n_7785d9e038') }}</li>
                <li>{{ $t('i18n_083b8a2ec9') }}</li>
              </ul>
            </template>
          </n-alert>
          <n-list size="small" bordered :data="machineLonelyData.projects">
            <template #renderItem="{ item }">
              <n-list-item>
                <n-space>
                  <span>{{ $t('i18n_fa7f6fccfd') }}{{ item.name }}</span>
                  <span>{{ $t('i18n_116d22f2ab') }}{{ item.id }}</span>
                  <span>{{ $t('i18n_e0fcbca309') }}{{ item.workspaceId }}</span>
                  <span>{{ $t('i18n_2256690a28') }}{{ item.nodeId }}</span>
                  <n-button type="primary" size="small" danger @click="openCorrectLonely(item, 'project')">{{
                    $t('i18n_23231543a4')
                  }}</n-button>
                </n-space>
              </n-list-item>
            </template>
            <template #header>
              <div>{{ $t('i18n_45fbb7e96a') }}</div>
            </template>
          </n-list>
          <n-list size="small" bordered :data="machineLonelyData.scripts">
            <template #renderItem="{ item }">
              <n-list-item
                ><n-space>
                  <span>{{ $t('i18n_b61a7e3ace') }}{{ item.name }}</span>
                  <span>{{ $t('i18n_d0f53484dc') }}{{ item.id }}</span>
                  <span>{{ $t('i18n_e0fcbca309') }}{{ item.workspaceId }}</span>
                  <span>{{ $t('i18n_2256690a28') }}{{ item.nodeId }}</span>
                  <n-button type="primary" size="small" danger @click="openCorrectLonely(item, 'script')">{{
                    $t('i18n_23231543a4')
                  }}</n-button>
                </n-space>
              </n-list-item>
            </template>
            <template #header>
              <div>{{ $t('i18n_c2b2f87aca') }}</div>
            </template>
          </n-list></n-space
        >
      </n-tab-pane>
    </n-tabs>
    <!-- 分配到其他工作空间 -->
    <CustomModal
      v-if="correctLonelyOpen"
      v-model:open="correctLonelyOpen"
      destroy-on-close
      :confirm-loading="confirmLoading"
      :title="$t('i18n_46097a1225')"
      :mask-closable="false"
      @ok="handleCorrectLonely"
    >
      <n-space direction="vertical" style="width: 100%">
        <n-alert :title="$t('i18n_947d983961')" type="warning">
          <template #description>
            <ul>
              <li>{{ $t('i18n_a3f1390bf1') }}</li>
              <li>{{ $t('i18n_2e51ca19eb') }}</li>
            </ul>
          </template>
        </n-alert>
        <n-form :model="temp">
          <n-form-item :label="$t('i18n_7e2b40fc86')" path="nodeId">
            <n-select
              v-model:value="temp.toNodeId"
              filterable
              :disabled="temp.toNodeId && temp.recommend"
              :placeholder="$t('i18n_f8a613d247')"
              :options="
                nodeList.map((item) => ({
                  label: `【${item.workspace && item.workspace.name}】${item.name}`,
                  value: item.id
                }))
              "
            />
          </n-form-item>
        </n-form>
      </n-space>
    </CustomModal>
  </div>
</template>
<script>
import { getNodeCache, clearCache } from '@/api/system'
import TaskStat from '@/pages/system/taskStat'
import { renderSize } from '@/utils/const'
import { machineLonelyData, machineCorrectLonelyData, machineListNode } from '@/api/system/assets-machine'
export default {
  components: {
    TaskStat
  },
  props: {
    machineId: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      temp: {},
      taskList: [],
      machineLonelyDataLoading: true,
      machineLonelyData: {},
      correctLonelyOpen: false,
      confirmLoading: false,
      nodeList: []
    }
  },
  mounted() {
    this.loadData()
    this.listMachineLonelyData()
  },
  methods: {
    // parseTime,
    renderSize,
    // load data
    loadData() {
      getNodeCache({
        machineId: this.machineId
      }).then((res) => {
        if (res.code === 200) {
          this.temp = res.data
          this.taskList = res.data?.taskList
        }
      })
    },
    // clear
    clear(type) {
      const params = {
        type: type,
        machineId: this.machineId
      }
      clearCache(params).then((res) => {
        if (res.code === 200) {
          // 成功
          $notification.success({
            message: res.msg
          })
          this.loadData()
        }
      })
    },
    // 查询孤独数据
    listMachineLonelyData() {
      this.machineLonelyDataLoading = true
      machineLonelyData({
        id: this.machineId
      })
        .then((res) => {
          if (res.code === 200 && res.data) {
            this.machineLonelyData = res.data
          }
        })
        .finally(() => {
          this.machineLonelyDataLoading = false
        })
    },
    // 查询机器关联的节点
    listMachineNode(item) {
      machineListNode({
        id: this.machineId
      }).then((res) => {
        if (res.code === 200) {
          this.nodeList = res.data || []
          if (item) {
            const find = this.nodeList.filter((node) => {
              return node.id === item.nodeId && node.workspaceId === item.workspaceId
            })
            if (find && find.length === 1) {
              this.temp = { ...this.temp, toNodeId: find[0].id, recommend: true }
            }
          }
        }
      })
    },
    // 打开修正窗口
    openCorrectLonely(item, type) {
      this.temp = { type: type, id: this.machineId, dataId: item.id }
      this.correctLonelyOpen = true
      this.listMachineNode(item)
    },
    //确认修正
    handleCorrectLonely() {
      if (!this.temp.toNodeId) {
        $notification.warn({
          message: this.$t('i18n_f8a613d247')
        })
        return false
      }
      this.confirmLoading = true
      machineCorrectLonelyData(this.temp)
        .then((res) => {
          if (res.code == 200) {
            $notification.success({
              message: res.msg
            })
            this.correctLonelyOpen = false
            this.listMachineLonelyData()
          }
        })
        .finally(() => {
          this.confirmLoading = false
        })
    }
  }
}
</script>
