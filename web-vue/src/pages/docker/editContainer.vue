<template>
  <div>
    <!-- stats -->
    <n-grid>
      <n-grid-item :span="12">
        <n-descriptions bordered :columns="1" size="small">
          <n-descriptions-item label="CPUS">
            {{
              (statsData.cpuStats && statsData.cpuStats.percpuUsage) ||
              (statsData.cpuStats && statsData.cpuStats.onlineCpus)
            }}
          </n-descriptions-item>
          <n-descriptions-item label="CPU %">
            {{
              (
                ((((statsData.cpuStats && statsData.cpuStats.cpuUsage && statsData.cpuStats.cpuUsage.totalUsage) || 0) -
                  ((statsData.precpuStats &&
                    statsData.precpuStats.cpuUsage &&
                    statsData.precpuStats.cpuUsage.totalUsage) ||
                    0)) /
                  ((statsData.cpuStats && statsData.cpuStats.systemCpuUsage) || 0) -
                  ((statsData.precpuStats && statsData.precpuStats.systemCpuUsage) || 0)) *
                100.0
              ).toFixed(4)
            }}
            %
          </n-descriptions-item>
          <n-descriptions-item label="MEM USAGE">
            {{
              renderSize(
                ((statsData.memoryStats && statsData.memoryStats.usage) || 0) -
                  ((statsData.memoryStats && statsData.memoryStats.stats && statsData.memoryStats.stats.cache) || 0)
              )
            }}
          </n-descriptions-item>
          <n-descriptions-item label="MEM LIMIT">
            {{ renderSize((statsData.memoryStats && statsData.memoryStats.limit) || 0) }}
          </n-descriptions-item>
          <!-- memoryRatio -->
          <n-descriptions-item label="MEM %">
            {{
              (
                ((((statsData.memoryStats && statsData.memoryStats.usage) || 0) -
                  ((statsData.memoryStats && statsData.memoryStats.stats && statsData.memoryStats.stats.cache) || 0)) /
                  (statsData.memoryStats && statsData.memoryStats.limit)) *
                100.0
              ).toFixed(4)
            }}
            %
          </n-descriptions-item>
          <!-- // rx_bytes 网卡接收流量 -->
          <!-- // tx_bytes 网卡输出流量 -->
          <n-descriptions-item label="NET I/O rx">
            <div v-for="(item, index) in Object.keys(statsData.networks || {})" :key="index">
              <n-tooltip>
                <template #trigger>
                  {{ renderSize(statsData.networks[item] && statsData.networks[item].rxBytes) || 0 }}
                </template>
                `${item} ${$t('i18n_3e54c81ca2')}`
              </n-tooltip>
            </div>
          </n-descriptions-item>
          <n-descriptions-item label="NET I/O tx">
            <div v-for="(item, index) in Object.keys(statsData.networks || {})" :key="index">
              <n-tooltip>
                <template #trigger>
                  {{ renderSize(statsData.networks[item] && statsData.networks[item].txBytes) || 0 }}
                </template>
                `${item} ${$t('i18n_97ecc1bbe9')}`
              </n-tooltip>
            </div>
          </n-descriptions-item>
          <n-descriptions-item label="BLOCK I/O">
            <n-tooltip>
              <template #trigger>
                {{
                  renderSize(
                    statsData.blkioStats &&
                      statsData.blkioStats.ioServiceBytesRecursive &&
                      statsData.blkioStats.ioServiceBytesRecursive[0] &&
                      statsData.blkioStats.ioServiceBytesRecursive[0].value
                  ) || 0
                }}
              </template>
              `${ (statsData.blkioStats && statsData.blkioStats.ioServiceBytesRecursive &&
              statsData.blkioStats.ioServiceBytesRecursive[0] && statsData.blkioStats.ioServiceBytesRecursive[0].op) ||
              'blkioStats' }`
            </n-tooltip>
            /
            <n-tooltip>
              <template #trigger>
                {{
                  renderSize(
                    statsData.blkioStats &&
                      statsData.blkioStats.ioServiceBytesRecursive &&
                      statsData.blkioStats.ioServiceBytesRecursive[1] &&
                      statsData.blkioStats.ioServiceBytesRecursive[1].value
                  ) || 0
                }}
              </template>
              `${ (statsData.blkioStats && statsData.blkioStats.ioServiceBytesRecursive &&
              statsData.blkioStats.ioServiceBytesRecursive[1] && statsData.blkioStats.ioServiceBytesRecursive[1].op) ||
              'blkioStats' }`
            </n-tooltip>
          </n-descriptions-item>

          <!-- // 进程或线程的数量 -->
          <n-descriptions-item label="PIDS">
            {{ statsData.pidsStats && statsData.pidsStats.current }}
          </n-descriptions-item>
        </n-descriptions></n-grid-item
      >
      <n-grid-item :span="12">
        <n-form ref="editForm" :model="temp">
          <n-form-item path="blkioWeight">
            <template #label>
              Block IO {{ $t('i18n_4aac559105') }}
              <n-tooltip>
                <template #trigger>
                  <QuestionCircleOutlined />
                </template>
                Block IO {{ $t('i18n_eaf987eea0') }}
              </n-tooltip>
            </template>
            <n-input-number
              v-model:value="temp.blkioWeight"
              style="width: 100%"
              :placeholder="$t('i18n_41d0ecbabd')"
              :min="0"
              :max="1000"
            />
          </n-form-item>
          <n-form-item path="cpuShares">
            <template #label>
              CPU {{ $t('i18n_4aac559105') }}
              <n-tooltip>
                <template #trigger>
                  <QuestionCircleOutlined />
                </template>
                {{ $t('i18n_9ac4765895') }}
              </n-tooltip>
            </template>
            <n-input-number v-model:value="temp.cpuShares" style="width: 100%" :placeholder="$t('i18n_9ac4765895')" />
          </n-form-item>
          <n-form-item path="cpusetCpus">
            <template #label>
              {{ $t('i18n_2ef1c35be8') }}
              <n-tooltip>
                <template #trigger>
                  <QuestionCircleOutlined />
                </template>
                {{ $t('i18n_0b76afbf5d') }},1）。
              </n-tooltip>
            </template>
            <n-input v-model:value="temp.cpusetCpus" style="width: 100%" :placeholder="$t('i18n_9c55e8e0f3')" />
          </n-form-item>
          <n-form-item path="cpusetMems">
            <template #label>
              CpusetMems
              <n-tooltip>
                <template #trigger>
                  <QuestionCircleOutlined />
                </template>
                {{ $t('i18n_470e9baf32') }}{{ $t('i18n_b28c17d2a6') }}
              </n-tooltip>
            </template>
            <n-input v-model:value="temp.cpusetMems" style="width: 100%" :placeholder="$t('i18n_8c7d19b32a')" />
          </n-form-item>
          <n-form-item path="cpuPeriod">
            <template #label>
              CPU {{ $t('i18n_2d842318fb') }}
              <n-tooltip>
                <template #trigger>
                  <QuestionCircleOutlined />
                </template>
                CPU {{ $t('i18n_6e02ee7aad') }}
              </n-tooltip>
            </template>
            <n-input-number v-model:value="temp.cpuPeriod" style="width: 100%" :placeholder="$t('i18n_c325ddecb1')" />
          </n-form-item>
          <n-form-item path="cpuQuota">
            <template #label>
              CPU {{ $t('i18n_19fcb9eb25') }}
              <n-tooltip>
                <template #trigger>
                  <QuestionCircleOutlined />
                </template>
                {{ $t('i18n_df1da2dc59') }}
              </n-tooltip>
            </template>
            <n-input-number v-model:value="temp.cpuQuota" style="width: 100%" :placeholder="$t('i18n_df1da2dc59')" />
          </n-form-item>

          <n-form-item path="memory">
            <template #label>
              {{ $t('i18n_9932551cd5') }}
              <n-tooltip>
                <template #trigger>
                  <QuestionCircleOutlined />
                </template>
                {{ $t('i18n_0b3edfaf28') }}
              </n-tooltip>
            </template>
            <n-input v-model:value="temp.memory" style="width: 100%" :placeholder="$t('i18n_0b3edfaf28')" />
          </n-form-item>
          <n-form-item path="memorySwap">
            <template #label>
              {{ $t('i18n_c983743f56') }}
              <n-tooltip>
                <template #trigger>
                  <QuestionCircleOutlined />
                </template>
                {{ $t('i18n_ebc96f0a5d') }}
              </n-tooltip>
            </template>
            <n-input v-model:value="temp.memorySwap" style="width: 100%" :placeholder="$t('i18n_ebc96f0a5d')" />
          </n-form-item>
          <n-form-item path="memoryReservation">
            <template #label>
              {{ $t('i18n_c0d38f475f') }}
              <n-tooltip>
                <template #trigger>
                  <QuestionCircleOutlined />
                </template>
                {{ $t('i18n_0a63bf5b41') }}
              </n-tooltip>
            </template>
            <n-input v-model:value="temp.memoryReservation" style="width: 100%" :placeholder="$t('i18n_0a63bf5b41')" />
          </n-form-item>
        </n-form>
      </n-grid-item>
    </n-grid>
  </div>
</template>
<script>
import { QuestionCircleOutlined } from '@ant-design/icons-vue'

import { dockerContainerStats, dockerInspectContainer, dockerUpdateContainer } from '@/api/docker-api'
import { renderSize } from '@/utils/const'

export default {
  props: {
    id: {
      type: String,
      default: ''
    },

    urlPrefix: {
      type: String,
      default: ''
    },
    machineDockerId: {
      type: String,
      default: ''
    },
    containerId: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      statsData: {},
      temp: {}
    }
  },
  computed: {
    reqDataId() {
      return this.id || this.machineDockerId
    }
  },
  mounted() {
    this.editContainer()
  },
  methods: {
    renderSize,
    // 编辑容器
    editContainer() {
      dockerContainerStats(this.urlPrefix, {
        id: this.reqDataId,
        containerId: this.containerId
      }).then((res2) => {
        if (res2.code === 200) {
          this.statsData = (res2.data && res2.data[this.containerId]) || {}
          dockerInspectContainer(this.urlPrefix, {
            id: this.reqDataId,
            containerId: this.containerId
          }).then((res) => {
            if (res.code === 200) {
              //this.editVisible = true

              const hostConfig = res.data.hostConfig || {}
              const data = {
                containerId: this.containerId,
                cpusetCpus: hostConfig.cpusetCpus,
                cpusetMems: hostConfig.cpusetMems,
                cpuPeriod: hostConfig.cpuPeriod,
                cpuShares: hostConfig.cpuShares,
                cpuQuota: hostConfig.cpuQuota,
                blkioWeight: hostConfig.blkioWeight,
                memoryReservation: renderSize(hostConfig.memoryReservation, hostConfig.memoryReservation),
                // Deprecated: This field is deprecated as the kernel 5.4 deprecated kmem.limit_in_bytes.
                // kernelMemory: hostConfig.kernelMemory,
                memory: renderSize(hostConfig.memory, hostConfig.memory),
                memorySwap: renderSize(hostConfig.memorySwap, hostConfig.memorySwap)
              }

              this.temp = Object.assign({}, data)
            }
          })
        }
        // console.log(res);
      })
    },
    handleEditOk() {
      return new Promise((ok, reject) => {
        this.$refs['editForm']
          .validate()
          .then(() => {
            const temp = Object.assign({}, this.temp, { id: this.reqDataId })
            dockerUpdateContainer(this.urlPrefix, temp)
              .then((res) => {
                if (res.code === 200) {
                  $notification.success({
                    message: res.msg
                  })
                  //this.editVisible = false
                  ok()
                } else {
                  reject()
                }
              })
              .catch(() => {
                reject()
              })
          })
          .catch(() => {
            reject()
          })
      })
    }
  }
}
</script>
