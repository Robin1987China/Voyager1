<template>
  <div>
    <n-data-table
      :data="list"
      size="medium"
      :columns="columns"
      bordered
      :row-key="(row) => row.id"
      :pagination="false"
      :scroll="{
        x: 'max-content'
      }"
    >
      <template #title>
        <n-space wrap class="search-box">
          <n-input
            v-model:value="listQuery['serviceId']"
            placeholder="id"
            class="search-input-item"
            @press-enter="loadData"
          />
          <n-input
            v-model:value="listQuery['serviceName']"
            :placeholder="$t('i18n_d7ec2d3fea')"
            class="search-input-item"
            @press-enter="loadData"
          />

          <n-button type="primary" :loading="loading" @click="loadData">{{ $t('i18n_e5f71fc31e') }}</n-button>
          <n-button type="primary" @click="handleAdd">{{ $t('i18n_d9ac9228e8') }}</n-button>
          <n-statistic format="s" :title="$t('i18n_0f8403d07e')" :value="countdownTime" @finish="loadData">
            <template #suffix>
              <div style="font-size: 12px">{{ $t('i18n_ee6ce96abb') }}</div>
            </template>
          </n-statistic>
        </n-space>
      </template>

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
        <template v-else-if="column.dataIndex === 'id'">
          <n-tooltip placement="topLeft" @click="handleLog(record)">
            <template #trigger>
              <span class="tw">
                <span class="tw">
                  <span>{{ text }}</span>
                  <EyeOutlined />
                </span>
              </span>
            </template>
            text
          </n-tooltip>
        </template>

        <template v-else-if="column.dataIndex === 'status'">
          <n-tooltip placement="top-start">
            <template #trigger>
              <span class="tw">
                <n-tag :color="(record.spec && record.spec.availability) === 'ACTIVE' ? 'green' : 'red'">
                  {{ text }}
                  <template v-if="record.spec">{{ record.spec.availability }}</template>
                </n-tag>
              </span>
            </template>
            {{
              `${$t('i18n_9b3e947cc9')}${text} ${$t('i18n_fb91527ce5')}${
                record.spec ? record.spec.availability || '' : ''
              }`
            }}
          </n-tooltip>
        </template>
        <!-- 角色显示 -->
        <template v-else-if="column.dataIndex === 'role'">
          <n-tooltip placement="topLeft">
            <template #trigger>
              <span class="tw">
                <n-tag
                  :color="`${record.managerStatus && record.managerStatus.reachability === 'REACHABLE' ? 'green' : ''}`"
                >
                  {{ text }}
                </n-tag>
              </span>
            </template>
            `${$t('i18n_20f32e1979')}${text} ${ record.managerStatus && record.managerStatus.reachability ===
            'REACHABLE' ? $t('i18n_88c5680d0d') + record.managerStatus.reachability : '' }`
          </n-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'address'">
          <n-tooltip placement="topLeft">
            <template #trigger>
              <span class="tw">
                <CloudServerOutlined v-if="item.managerStatus && item.managerStatus.leader" />

                {{ text }}
              </span>
            </template>
            text
          </n-tooltip>
        </template>

        <template v-else-if="column.dataIndex === 'os'">
          <n-tooltip placement="topLeft">
            <template #trigger>
              <span class="tw">
                <span class="tw">
                  <span>
                    <n-tag
                      >{{ text }}-{{
                        item.description && item.description.platform && item.description.platform.architecture
                      }}</n-tag
                    >
                  </span>
                </span>
              </span>
            </template>
            text
          </n-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'updatedAt'">
          <n-tooltip placement="topLeft">
            <template #trigger>
              {{ text }}
            </template>
            `${$t('i18n_bf94b97d1a')}${text} ${$t('i18n_312f45014a')}${record.createdAt}`
          </n-tooltip>
        </template>

        <template v-else-if="column.replicas">
          <n-tooltip placement="topLeft">
            <template #trigger>
              <span class="tw">
                <n-tag @click="handleTask(record, 'RUNNING')">{{ text }}</n-tag>

                <ReadOutlined @click="handleTask(record)" />
              </span>
            </template>
            `${$t('i18n_ce07501354')},${$t('i18n_c0e498a259')}`
          </n-tooltip>
        </template>

        <template v-else-if="column.dataIndex === 'operation'">
          <n-space>
            <n-button size="small" type="primary" @click="handleEdit(record)">{{ $t('i18n_8347a927c0') }}</n-button>
            <n-button size="small" type="primary" danger @click="handleDel(record)">{{
              $t('i18n_2f4aaddde3')
            }}</n-button>
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
      :title="$t('i18n_cc51f34aa4')"
      width="70vw"
      :mask-closable="false"
      @ok="handleEditOk"
    >
      <n-form ref="editForm" :rules="rules" :model="temp">
        <n-form-item :label="$t('i18n_8f3747c057')" path="name">
          <template #help
            ><span v-if="!temp.serviceId">{{ $t('i18n_9ee9d48699') }}</span></template
          >
          <n-input
            v-model:value="temp.name"
            :disabled="temp.serviceId ? true : false"
            :placeholder="$t('i18n_8f3747c057')"
          />
        </n-form-item>
        <n-form-item :label="$t('i18n_44c4aaa1d9')" path="mode">
          <template #help
            ><span v-if="!temp.serviceId">{{ $t('i18n_9ee9d48699') }}</span></template
          >
          <n-radio-group v-model:value="temp.mode" name="mode" :disabled="temp.serviceId ? true : false">
            <n-radio value="REPLICATED">{{ $t('i18n_0428b36ab1') }}</n-radio>
            <n-radio value="GLOBAL">{{ $t('i18n_0c1de8295a') }} </n-radio>
          </n-radio-group>
          <n-form-item>
            <template v-if="temp.mode === 'REPLICATED'">
              {{ $t('i18n_532495b65b') }}:
              <n-input-number v-model:value="temp.replicas" :min="1" />
            </template>
          </n-form-item>
        </n-form-item>
        <n-form-item :label="$t('i18n_bbf2775521')" path="image">
          <n-input v-model:value="temp.image" :placeholder="$t('i18n_bbf2775521')" />
        </n-form-item>
        <n-form-item label="hostname" path="hostname">
          <n-input v-model:value="temp.hostname" :placeholder="$t('i18n_f9361945f3')" />
        </n-form-item>
        <n-form-item :label="$t('i18n_38aa9dc2a0')" path="">
          <n-form-item>
            <n-tabs>
              <n-tab-pane name="port" :tab="$t('i18n_c76cfefe72')">
                <n-form-item :label="$t('i18n_d9435aa802')" path="endpointResolutionMode">
                  <n-radio-group
                    v-model:value="temp.endpointResolutionMode"
                    name="endpointResolutionMode"
                    @change="
                      () => {
                        temp.exposedPorts = temp.exposedPorts.map((item) => {
                          if (temp.endpointResolutionMode === 'DNSRR') {
                            item.publishMode = 'host'
                          }
                          return item
                        })
                      }
                    "
                  >
                    <n-radio value="VIP">VIP</n-radio>
                    <n-radio value="DNSRR">DNSRR </n-radio>
                  </n-radio-group>
                </n-form-item>
                <n-form-item>
                  <n-grid v-for="(item, index) in temp.exposedPorts" :key="index">
                    <n-grid-item :span="21">
                      <n-input-group>
                        <n-grid>
                          <n-grid-item :span="7">
                            <n-radio-group v-model:value="item.publishMode" name="publishMode">
                              <n-radio value="ingress" :disabled="temp.endpointResolutionMode === 'DNSRR'">{{
                                $t('i18n_75fc7de737')
                              }}</n-radio>
                              <n-radio value="host">{{ $t('i18n_652273694e') }}</n-radio>
                            </n-radio-group>
                          </n-grid-item>
                          <n-grid-item :span="7">
                            <n-input
                              v-model:value="item.publishedPort"
                              :addon-before="$t('i18n_c76cfefe72')"
                              :placeholder="$t('i18n_c76cfefe72')"
                            >
                            </n-input>
                          </n-grid-item>
                          <n-grid-item :span="8" :offset="1">
                            <n-input
                              v-model:value="item.targetPort"
                              :addon-before="$t('i18n_22c799040a')"
                              :placeholder="$t('i18n_31691a647c', { slot1: $t('i18n_22c799040a') })"
                            >
                              <template #suffix>
                                <n-select
                                  v-model:value="item.protocol"
                                  :placeholder="$t('i18n_0739b9551d')"
                                  :options="[
                                    { label: 'TCP', value: 'TCP' },
                                    { label: 'UDP', value: 'UDP' },
                                    { label: 'SCTP', value: 'SCTP' }
                                  ]"
                                />
                              </template>
                            </n-input>
                          </n-grid-item>
                        </n-grid>
                      </n-input-group>
                    </n-grid-item>
                    <n-grid-item :span="2" :offset="1">
                      <n-space>
                        <MinusCircleOutlined
                          v-if="temp.exposedPorts && temp.exposedPorts.length > 1"
                          @click="
                            () => {
                              temp.exposedPorts.splice(index, 1)
                            }
                          "
                        />

                        <PlusSquareOutlined
                          @click="
                            () => {
                              temp.exposedPorts.push({
                                protocol: 'TCP',
                                publishMode: 'host'
                              })
                            }
                          "
                        />
                      </n-space>
                    </n-grid-item>
                  </n-grid>
                </n-form-item>
              </n-tab-pane>

              <n-tab-pane name="volumes" :tab="$t('i18n_640374b7ae')">
                <n-form-item>
                  <n-grid v-for="(item, index) in temp.volumes" :key="index">
                    <n-grid-item :span="21">
                      <n-input-group>
                        <n-grid>
                          <n-grid-item :span="7">
                            <n-radio-group v-model:value="item.type" name="publishMode">
                              <n-radio value="VOLUME">VOLUME</n-radio>
                              <n-radio value="BIND">BIND</n-radio>
                            </n-radio-group>
                          </n-grid-item>
                          <n-grid-item :span="7">
                            <n-input
                              v-model:value="item.source"
                              :addon-before="$t('i18n_ad4b4a5b3b')"
                              :placeholder="$t('i18n_ec537c957a', { slot1: $t('i18n_ad4b4a5b3b') })"
                            />
                          </n-grid-item>
                          <n-grid-item :span="8" :offset="1">
                            <n-input
                              v-model:value="item.target"
                              :addon-before="$t('i18n_22c799040a')"
                              :placeholder="$t('i18n_368ffad051', { slot1: $t('i18n_22c799040a') })"
                            />
                          </n-grid-item>
                        </n-grid>
                      </n-input-group>
                    </n-grid-item>
                    <n-grid-item :span="2" :offset="1">
                      <n-space>
                        <MinusCircleOutlined
                          v-if="temp.volumes && temp.volumes.length > 1"
                          @click="
                            () => {
                              temp.volumes.splice(index, 1)
                            }
                          "
                        />

                        <PlusSquareOutlined
                          @click="
                            () => {
                              temp.volumes.push({})
                            }
                          "
                        />
                      </n-space>
                    </n-grid-item>
                  </n-grid>
                </n-form-item>
              </n-tab-pane>

              <n-tab-pane name="args" :tab="$t('i18n_3d0a2df9ec')">
                <n-form-item>
                  <n-grid v-for="(item, index) in temp.args" :key="index">
                    <n-grid-item :span="20">
                      <n-input
                        v-model:value="item.value"
                        :addon-before="$t('i18n_bfed4943c5')"
                        :placeholder="$t('i18n_d65d977f1d')"
                      />
                    </n-grid-item>

                    <n-grid-item :span="2" :offset="1">
                      <n-space>
                        <MinusCircleOutlined
                          v-if="temp.args && temp.args.length > 1"
                          @click="
                            () => {
                              temp.args.splice(index, 1)
                            }
                          "
                        />

                        <PlusSquareOutlined
                          @click="
                            () => {
                              temp.args.push({})
                            }
                          "
                        />
                      </n-space>
                    </n-grid-item>
                  </n-grid>
                </n-form-item>
              </n-tab-pane>
              <n-tab-pane name="command" :tab="$t('i18n_ddf7d2a5ce')">
                <n-form-item>
                  <n-grid v-for="(item, index) in temp.commands" :key="index">
                    <n-grid-item :span="20">
                      <n-input
                        v-model:value="item.value"
                        :addon-before="$t('i18n_579a6d0d92')"
                        :placeholder="$t('i18n_2a6a516f9d')"
                      />
                    </n-grid-item>

                    <n-grid-item :span="2" :offset="1">
                      <n-space>
                        <MinusCircleOutlined
                          v-if="temp.commands && temp.commands.length > 1"
                          @click="
                            () => {
                              temp.commands.splice(index, 1)
                            }
                          "
                        />
                        <PlusSquareOutlined
                          @click="
                            () => {
                              temp.commands.push({})
                            }
                          "
                        />
                      </n-space>
                    </n-grid-item>
                  </n-grid>
                </n-form-item>
              </n-tab-pane>
              <n-tab-pane name="env" :tab="$t('i18n_3867e350eb')">
                <n-form-item>
                  <n-grid v-for="(item, index) in temp.envs" :key="index">
                    <n-grid-item :span="10">
                      <n-input
                        v-model:value="item.name"
                        :addon-before="$t('i18n_d7ec2d3fea')"
                        :placeholder="$t('i18n_7cb8d163bb')"
                      />
                    </n-grid-item>
                    <n-grid-item :span="10" :offset="1">
                      <n-input
                        v-model:value="item.value"
                        :addon-before="$t('i18n_9a2ee7044f')"
                        :placeholder="$t('i18n_9a2ee7044f')"
                      />
                    </n-grid-item>
                    <n-grid-item :span="2" :offset="1">
                      <n-space>
                        <MinusCircleOutlined
                          v-if="temp.envs && temp.envs.length > 1"
                          @click="
                            () => {
                              temp.envs.splice(index, 1)
                            }
                          "
                        />

                        <PlusSquareOutlined
                          @click="
                            () => {
                              temp.envs.push({})
                            }
                          "
                        />
                      </n-space>
                    </n-grid-item>
                  </n-grid>
                </n-form-item>
              </n-tab-pane>
              <n-tab-pane v-if="temp.update" :tab="$t('i18n_a84a45b352')">
                <n-form-item :label="$t('i18n_07a828310b')" path="parallelism">
                  <n-input-number
                    v-model:value="temp.update.parallelism"
                    style="width: 80%"
                    :min="0"
                    :placeholder="$t('i18n_31eb055c9c')"
                  />
                </n-form-item>
                <n-form-item :label="$t('i18n_db732ecb48')" path="delay">
                  <template #help>
                    <span style="padding-left: 20%">{{ $t('i18n_e2adcc679a') }}</span>
                  </template>
                  <n-input-number
                    v-model:value="temp.update.delay"
                    style="width: 80%"
                    :min="1"
                    :placeholder="$t('i18n_85ec12ccd3')"
                  />
                </n-form-item>
                <n-form-item :label="$t('i18n_b3fe677b5f')" path="maxFailureRatio">
                  <n-input-number
                    v-model:value="temp.update.maxFailureRatio"
                    style="width: 80%"
                    :min="0"
                    :placeholder="`${$t('i18n_b3fe677b5f')}${$t('i18n_c7c4e4632f')}`"
                  />
                </n-form-item>
                <n-form-item :label="$t('i18n_fa2f7a8927')" path="failureAction">
                  <n-radio-group v-model:value="temp.update.failureAction" name="failureAction">
                    <n-radio value="PAUSE">{{ $t('i18n_8d63ef388e') }}</n-radio>
                    <n-radio value="CONTINUE">{{ $t('i18n_27ca568be2') }}</n-radio>
                    <n-radio value="ROLLBACK">{{ $t('i18n_d00b485b26') }}</n-radio>
                  </n-radio-group>
                </n-form-item>
                <n-form-item :label="$t('i18n_d5c68a926e')" path="order">
                  <n-radio-group v-model:value="temp.update.order" name="order">
                    <n-radio value="STOP_FIRST">{{ $t('i18n_0647b5fc26') }}</n-radio>
                    <n-radio value="START_FIRST">{{ $t('i18n_42fd64c157') }}</n-radio>
                  </n-radio-group>
                </n-form-item>
                <n-form-item :label="$t('i18n_9aff624153')" path="monitor">
                  <n-input-number
                    v-model:value="temp.update.monitor"
                    style="width: 80%"
                    :min="1"
                    :placeholder="$t('i18n_f6d6ab219d')"
                  />
                </n-form-item>
              </n-tab-pane>
              <n-tab-pane v-if="temp.rollback" :tab="$t('i18n_ad780debbc')">
                <n-form-item :label="$t('i18n_07a828310b')" path="parallelism">
                  <n-input-number
                    v-model:value="temp.rollback.parallelism"
                    style="width: 80%"
                    :min="0"
                    :placeholder="$t('i18n_31eb055c9c')"
                  />
                </n-form-item>
                <n-form-item :label="$t('i18n_db732ecb48')" path="delay">
                  <template #help>
                    <span style="padding-left: 20%">{{ $t('i18n_e2adcc679a') }}</span>
                  </template>
                  <n-input-number
                    v-model:value="temp.rollback.delay"
                    style="width: 80%"
                    :min="1"
                    :placeholder="$t('i18n_6a66d4cdf3')"
                  />
                </n-form-item>
                <n-form-item :label="$t('i18n_b3fe677b5f')" path="maxFailureRatio">
                  <n-input-number
                    v-model:value="temp.rollback.maxFailureRatio"
                    style="width: 80%"
                    :min="0"
                    :placeholder="`${$t('i18n_b3fe677b5f')}${$t('i18n_c7c4e4632f')}`"
                  />
                </n-form-item>
                <n-form-item :label="$t('i18n_fa2f7a8927')" path="failureAction">
                  <n-radio-group v-model:value="temp.rollback.failureAction" name="failureAction">
                    <n-radio value="PAUSE">{{ $t('i18n_8d63ef388e') }}</n-radio>
                    <n-radio value="CONTINUE">{{ $t('i18n_27ca568be2') }}</n-radio>
                    <n-radio value="ROLLBACK">{{ $t('i18n_d00b485b26') }}</n-radio>
                  </n-radio-group>
                </n-form-item>
                <n-form-item :label="$t('i18n_d5c68a926e')" path="order">
                  <n-radio-group v-model:value="temp.rollback.order" name="order">
                    <n-radio value="STOP_FIRST">{{ $t('i18n_0647b5fc26') }}</n-radio>
                    <n-radio value="START_FIRST">{{ $t('i18n_42fd64c157') }}</n-radio>
                  </n-radio-group>
                </n-form-item>
                <n-form-item :label="$t('i18n_9aff624153')" path="monitor">
                  <n-input-number
                    v-model:value="temp.rollback.monitor"
                    style="width: 80%"
                    :min="1"
                    :placeholder="$t('i18n_f6d6ab219d')"
                  />
                </n-form-item>
              </n-tab-pane>
              <n-tab-pane v-if="temp.resources" :tab="$t('i18n_eee83a9211')">
                <n-form-item :label="$t('i18n_3711cbf638')">
                  <n-grid>
                    <n-grid-item :span="8">
                      <n-input
                        v-model:value="temp.resources.reservations.nanoCPUs"
                        addon-before="CPUs"
                        :placeholder="$t('i18n_9e6b699597')"
                      />
                    </n-grid-item>
                    <n-grid-item :span="8" :offset="1">
                      <n-input
                        v-model:value="temp.resources.reservations.memoryBytes"
                        addon-before="memory"
                        :placeholder="$t('i18n_18eb76c8a0')"
                      />
                    </n-grid-item>
                  </n-grid>
                </n-form-item>
                <n-form-item :label="$t('i18n_87db69bd44')">
                  <n-grid>
                    <n-grid-item :span="8">
                      <n-input
                        v-model:value="temp.resources.limits.nanoCPUs"
                        addon-before="CPUs"
                        :placeholder="$t('i18n_9e6b699597')"
                      />
                    </n-grid-item>
                    <n-grid-item :span="8" :offset="1">
                      <n-input
                        v-model:value="temp.resources.limits.memoryBytes"
                        addon-before="memory"
                        :placeholder="$t('i18n_18eb76c8a0')"
                      />
                    </n-grid-item>
                  </n-grid>
                </n-form-item>
              </n-tab-pane>
            </n-tabs>
          </n-form-item>
        </n-form-item>
      </n-form>
    </CustomModal>
    <!-- 查看任务 -->
    <CustomModal
      v-if="taskVisible"
      v-model:open="taskVisible"
      destroy-on-close
      :title="$t('i18n_13f931c5d9')"
      width="80vw"
      :footer="null"
      :mask-closable="false"
    >
      <swarm-task
        v-if="taskVisible"
        :id="id"
        :show="taskVisible"
        :task-state="temp.state"
        :service-id="temp.id"
        :url-prefix="urlPrefix"
      />
    </CustomModal>
    <!-- 查看日志 -->

    <pull-log
      v-if="logVisible > 0"
      :id="id"
      :show="logVisible != 0"
      :data-id="temp.id"
      type="service"
      :url-prefix="urlPrefix"
      @close="
        () => {
          logVisible = 0
        }
      "
    />
  </div>
</template>
<script>
import {
  CloudServerOutlined,
  EyeOutlined,
  MinusCircleOutlined,
  PlusSquareOutlined,
  ReadOutlined
} from '@ant-design/icons-vue'

import { dockerSwarmServicesDel, dockerSwarmServicesEdit, dockerSwarmServicesList } from '@/api/docker-swarm'
import SwarmTask from './task'
import PullLog from './pull-log'
import { renderSize } from '@/utils/const'

export default {
  components: { SwarmTask, PullLog },
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
      temp: {
        update: {},
        rollback: {}
      },
      editVisible: false,
      initSwarmVisible: false,
      taskVisible: false,
      logVisible: 0,
      confirmLoading: false,
      rules: {
        name: [{ required: true, message: this.$t('i18n_4e7e04b15d'), trigger: 'blur' }],
        mode: [{ required: true, message: this.$t('i18n_922b76febd'), trigger: 'blur' }],
        image: [{ required: true, message: this.$t('i18n_b9af769752'), trigger: 'blur' }]
      },
      columns: [
        {
          title: this.$t('i18n_faaadc447b'),
          width: 80,
          ellipsis: true,
          align: 'center',
          render: (row, index) => `${index + 1}`
        },
        {
          title: this.$t('i18n_a75b96584d'),
          key: 'id',
          ellipsis: true
        },
        {
          title: this.$t('i18n_d7ec2d3fea'),
          key: ['spec', 'name'],
          ellipsis: true
        },
        {
          title: this.$t('i18n_44c4aaa1d9'),
          key: ['spec', 'mode', 'mode'],
          ellipsis: true,
          width: 120
        },
        // { title: "网络模式", key: "spec.endpointSpec.mode", ellipsis: true, width: 120, },
        {
          title: this.$t('i18n_532495b65b'),
          key: ['spec', 'mode', 'replicated', 'replicas'],
          align: 'center',
          width: 90,
          ellipsis: true,
          replicas: true
        },
        {
          title: this.$t('i18n_d9435aa802'),
          key: ['spec', 'endpointSpec', 'mode'],
          ellipsis: true,
          width: 100
        },

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

      countdownTime: Date.now()
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
      dockerSwarmServicesList(this.urlPrefix, this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data
        }
        this.loading = false
        this.countdownTime = Date.now() + 5 * 1000
      })
    },
    //  任务
    handleTask(record, state) {
      this.taskVisible = true
      this.temp = record
      this.temp = { ...this.temp, state: state || '' }
    },
    // 日志
    handleLog(record) {
      this.logVisible = new Date() * Math.random()
      this.temp = record
    },
    //  创建服务
    handleAdd() {
      this.editVisible = true
      this.temp = {
        mode: 'REPLICATED',
        replicas: 1,
        endpointResolutionMode: 'VIP',
        exposedPorts: [
          {
            publishMode: 'host',
            protocol: 'TCP'
          }
        ],

        volumes: [
          {
            type: 'VOLUME'
          }
        ],

        args: [{}],
        commands: [{}],
        envs: [{}],
        update: {},
        rollback: {},
        resources: {
          limits: {},
          reservations: {}
        }
      }
    },
    // 编辑
    handleEdit(record) {
      const spec = record.spec
      if (!spec) {
        $notification.error({
          message: this.$t('i18n_534115e981')
        })
        return
      }
      this.editVisible = true
      let image = spec.taskTemplate?.containerSpec?.image

      if (image && image.includes('@')) {
        image = image.split('@')[0]
      }
      this.temp = {
        serviceId: record.id,
        name: spec.name,
        hostname: spec.taskTemplate?.containerSpec?.hostname,
        mode: spec.mode?.mode,
        replicas: spec.mode?.replicated?.replicas,
        image: image,
        version: record.version?.index,
        endpointResolutionMode: spec.endpointSpec?.mode,
        exposedPorts: [
          {
            publishMode: 'host',
            protocol: 'TCP'
          }
        ],

        volumes: [{ type: 'VOLUME' }],
        args: [{}],
        commands: [{}],
        envs: [{}],
        update: {},
        rollback: {},
        resources: {}
      }

      const args = spec.taskTemplate?.containerSpec?.args
      const mounts = spec.taskTemplate?.containerSpec?.mounts
      const command = spec.taskTemplate?.containerSpec?.command
      const env = spec.taskTemplate?.containerSpec?.env
      const limits = spec.taskTemplate?.resources?.limits
      const reservations = spec.taskTemplate?.resources?.reservations
      const ports = spec.endpointSpec?.ports
      const updateConfig = spec.updateConfig
      const rollbackConfig = spec.rollbackConfig
      if (args) {
        this.temp = {
          ...this.temp,
          args: args.map((item) => {
            return {
              value: item
            }
          })
        }
      }
      if (command) {
        this.temp = {
          ...this.temp,
          commands: command.map((item) => {
            return {
              value: item
            }
          })
        }
      }
      if (env) {
        this.temp = {
          ...this.temp,
          envs: env.map((item) => {
            return {
              name: item.split('=')[0],
              value: item.split('=')[1]
            }
          })
        }
      }
      if (ports) {
        this.temp = { ...this.temp, exposedPorts: ports }
      }
      if (mounts) {
        this.temp = { ...this.temp, volumes: mounts }
      }
      if (updateConfig) {
        this.temp = { ...this.temp, update: updateConfig }
      }
      if (rollbackConfig) {
        this.temp = { ...this.temp, rollback: rollbackConfig }
      }
      let resources = { limits: {}, reservations: {} }
      if (limits) {
        limits.memoryBytes = renderSize(limits.memoryBytes)
        resources = { ...resources, limits: limits }
      }
      if (reservations) {
        reservations.memoryBytes = renderSize(reservations.memoryBytes)
        resources = { ...resources, reservations: reservations }
      }
      this.temp = { ...this.temp, resources: resources }
    },
    handleEditOk() {
      this.$refs['editForm'].validate().then(() => {
        this.temp.id = this.id
        const temp = Object.assign({}, this.temp)
        temp.volumes = (this.temp.volumes || []).filter((item) => {
          return item.source && item.target
        })
        // 处理端口
        temp.exposedPorts = (this.temp.exposedPorts || []).filter((item) => {
          return item.publishedPort && item.targetPort
        })
        this.confirmLoading = true
        dockerSwarmServicesEdit(this.urlPrefix, temp)
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
      })
    },
    // 删除
    handleDel(record) {
      $confirm({
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        content: this.$t('i18n_a4266aea79'),
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
        onOk: () => {
          return dockerSwarmServicesDel(this.urlPrefix, {
            serviceId: record.id,
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
