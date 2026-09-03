<template>
  <div class="">
    <template v-if="useSuggestions">
      <n-result :title="$t('i18n_79698c57a2')">
        <template #subTitle> {{ $t('i18n_0373ba5502') }} </template>
        <template #extra>
          <n-button type="primary" @click="fastInstallNodeShow">{{ $t('i18n_70b5b45591') }} </n-button>
          <router-link to="/system/assets/machine-list">
            <n-button key="console" type="primary">{{ $t('i18n_c494fbec77') }}</n-button></router-link
          >
        </template>
        <n-alert :title="$t('i18n_81c1dff69c')" type="info" show-icon>
          <template #description>
            <ol>
              <li>{{ $t('i18n_da317c3682') }}</li>
              <li>{{ $t('i18n_9c3a5e1dad') }}</li>
            </ol>
          </template>
        </n-alert>
      </n-result>
    </template>
    <template v-else>
      <!-- <n-card :body-style="{ padding: '10px' }"> -->
      <CustomTable
        is-show-tools
        default-auto-refresh
        :auto-refresh-time="30"
        :active-page="activePage"
        table-name="nodeSearch"
        :empty-description="$t('i18n_17b4c9c631')"
        :columns="columns"
        :data="list"
        bordered
        size="medium"
        row-key="id"
        :pagination="pagination"
        :scroll="{
          x: 'max-content'
        }"
        :row-selection="rowSelection"
        @change="
          (pagination, filters, sorter) => {
            listQuery = CHANGE_PAGE(listQuery, {
              pagination,
              sorter
            })
            loadData()
          }
        "
        @refresh="loadData"
        @change-table-layout="
          (layoutType) => {
            tableSelections = []
            listQuery = CHANGE_PAGE(listQuery, {
              pagination: { limit: layoutType === 'card' ? 8 : 10 }
            })
            loadData()
          }
        "
      >
        <template #title>
          <n-space>
            <n-input v-model:value="listQuery['%name%']" :placeholder="$t('i18n_b1785ef01e')" @press-enter="loadData" />

            <n-select
              v-model:value="listQuery.group"
              filterable
              clearable
              :placeholder="$t('i18n_829abe5a8d')"
              class="search-input-item"
              :options="groupList"
            />
            <n-tooltip>
              <template #trigger>
                <span class="tw">
                  <n-button :loading="loading" type="primary" @click="loadData">{{ $t('i18n_e5f71fc31e') }}</n-button>
                </span>
              </template>
              $t('i18n_4838a3bd20')
            </n-tooltip>
            <n-button
              type="primary"
              @click="
                () => {
                  fastInstallNode = true
                }
              "
              >{{ $t('i18n_70b5b45591') }}
            </n-button>
            <n-button
              v-if="layoutType === 'table'"
              type="primary"
              :disabled="!tableSelections || !tableSelections.length"
              @click="syncToWorkspaceShow"
              >{{ $t('i18n_398ce396cd') }}</n-button
            >
            <n-tooltip v-else>
              <template #trigger>
                <span class="tw">
                  <n-button :disabled="true" type="primary"> {{ $t('i18n_398ce396cd') }} </n-button>
                </span>
              </template>
              $t('i18n_68af00bedb')
            </n-tooltip>
          </n-space>
        </template>
        <template #tableHelp>
          <n-tooltip placement="bottom">
            <template #trigger>
              <QuestionCircleOutlined />
            </template>

            <div>
              <ul>
                <li>{{ $t('i18n_c75b14a04e') }}</li>
                <li>{{ $t('i18n_8b6e758e4c') }}</li>
                <li>{{ $t('i18n_4642113bba') }}</li>
                <li>{{ $t('i18n_2213206d43') }}</li>
                <li>{{ $t('i18n_ee4fac2f3c') }}</li>
              </ul>
            </div>
          </n-tooltip>
        </template>
        <template #tableBodyCell="{ column, text, record, index }">
          <template v-if="column.dataIndex === 'url'">
            <n-tooltip placement="top-start">
              <template #trigger>
                <span class="tw">
                  <template v-if="record.machineNodeData">
                    <span
                      >{{ record.machineNodeData.voyager1Protocol }}://{{ record.machineNodeData.voyager1Url }}</span
                    >
                  </template>
                  <span v-else> - </span>
                </span>
              </template>
              {{ text }}
            </n-tooltip>
          </template>
          <template v-else-if="column.dataIndex === 'name'">
            <template v-if="record.openStatus !== 1">
              <n-tooltip>
                <template #trigger>
                  <span class="tw">
                    <span class="tw">
                      <span>{{ text || '' }}</span>
                    </span>
                  </span>
                </template>
                `${text}`
              </n-tooltip>
            </template>
            <template v-else>
              <n-tooltip @click="handleNode(record)">
                <template #trigger>
                  <span class="tw">
                    <span class="tw">
                      <n-button text style="padding: 0" size="small">
                        <FullscreenOutlined /><span>{{ text }}</span>
                      </n-button>
                    </span>
                  </span>
                </template>
                `${text} ${$t('i18n_8a4dbe88b8')}`
              </n-tooltip>
            </template>
          </template>
          <template v-else-if="column.dataIndex === 'status'">
            <n-tooltip placement="top-start">
              <template #trigger>
                <span class="tw">
                  <template v-if="record.openStatus === 1">
                    <n-tag
                      :color="record.machineNodeData && record.machineNodeData.status === 1 ? 'green' : 'pink'"
                      style="margin-right: 0"
                    >
                      {{ statusMap[record.machineNodeData && record.machineNodeData.status] || $t('i18n_1622dc9b6b') }}
                    </n-tag>
                  </template>
                  <n-tag v-else>{{ $t('i18n_4637765b0a') }}</n-tag>
                </span>
              </template>
              {{
                `${statusMap[record.machineNodeData && record.machineNodeData.status] || $t('i18n_1622dc9b6b')} ${
                  record.machineNodeData && record.machineNodeData.statusMsg
                }`
              }}
            </n-tooltip>
          </template>
          <template v-else-if="column.dataIndex === 'osName'">
            <n-tooltip placement="topLeft">
              <template #trigger>
                <span class="tw">
                  <span class="tw">
                    <span>{{ record.machineNodeData && record.machineNodeData.osName }}</span>
                  </span>
                </span>
              </template>
              text
            </n-tooltip>
          </template>
          <template v-else-if="column.dataIndex === 'javaVersion'">
            <n-tooltip placement="topLeft">
              <template #trigger>
                <span class="tw">
                  <span class="tw">
                    <span>{{ record.machineNodeData && record.machineNodeData.javaVersion }}</span>
                  </span>
                </span>
              </template>
              record.machineNodeData && record.machineNodeData.javaVersion
            </n-tooltip>
          </template>
          <template v-else-if="column.dataIndex === 'jvmInfo'">
            <n-tooltip placement="topLeft">
              <template #trigger>
                <span class="tw">
                  <span class="tw">
                    <span
                      >{{ renderSize(record.machineNodeData && record.machineNodeData.jvmFreeMemory) }}
                      /
                      {{ renderSize(record.machineNodeData && record.machineNodeData.jvmTotalMemory) }}</span
                    >
                  </span>
                </span>
              </template>
              `${$t('i18n_3574d38d3e')}${renderSize( record.machineNodeData && record.machineNodeData.jvmFreeMemory )}
              ${$t('i18n_a0a3d583b9')}${renderSize( record.machineNodeData && record.machineNodeData.jvmTotalMemory )}`
            </n-tooltip>
          </template>

          <template v-else-if="column.dataIndex === 'runTime'">
            <n-tooltip placement="topLeft">
              <template #trigger>
                <span class="tw">
                  <span class="tw">
                    <span>{{
                      formatDuration(record.machineNodeData && record.machineNodeData.voyager1Uptime, '', 2)
                    }}</span>
                  </span>
                </span>
              </template>
              formatDuration(record.machineNodeData && record.machineNodeData.voyager1Uptime)
            </n-tooltip>
          </template>
          <template v-else-if="column.dataIndex === 'voyager1ProjectCount'">
            <div v-if="record.machineNodeData && record.machineNodeData.status === 1" @click="syncNode(record)">
              <n-tooltip placement="top-start">
                <template #trigger>
                  <span class="tw">
                    <n-tag>{{ text || 0 }} </n-tag>
                    <SyncOutlined />
                  </span>
                </template>

                <ul>
                  <li>{{ $t('i18n_56d9d84bff') }}{{ text || 0 }}</li>
                  <li>{{ $t('i18n_af98c31607') }}{{ record.machineNodeData.voyager1ProjectCount }}</li>
                  <li>{{ $t('i18n_143bfbc3a1') }}</li>
                </ul>
              </n-tooltip>
            </div>
            <span v-else>-</span>
          </template>
          <template v-else-if="column.dataIndex === 'voyager1ScriptCount'">
            <div v-if="record.machineNodeData && record.machineNodeData.status === 1" @click="syncNodeScript(record)">
              <n-tooltip placement="top-start">
                <template #trigger>
                  <span class="tw">
                    <n-tag>{{ text || 0 }} </n-tag>
                    <SyncOutlined />
                  </span>
                </template>

                <ul>
                  <li>{{ $t('i18n_cc5dccd757') }}{{ text || 0 }}</li>
                  <li>{{ $t('i18n_375118fad1') }}{{ record.machineNodeData.voyager1ScriptCount }}</li>
                  <li>{{ $t('i18n_5baaef6996') }}</li>
                </ul>
              </n-tooltip>
            </div>
            <span v-else>-</span>
          </template>

          <template v-else-if="column.dataIndex === 'operation'">
            <n-space>
              <n-tooltip>
                <template #trigger>
                  <span class="tw">
                    <n-button
                      size="small"
                      type="primary"
                      :disabled="record.openStatus !== 1"
                      @click="handleNode(record)"
                      >{{ $t('i18n_08b55fea3c') }}</n-button
                    >
                  </span>
                </template>
                $t('i18n_e96705ead1')
              </n-tooltip>
              <n-tooltip>
                <template #trigger>
                  <span class="tw">
                    <n-button size="small" type="primary" :disabled="!record.sshId" @click="handleTerminal(record)"
                      ><CodeOutlined />{{ $t('i18n_4722bc0c56') }}</n-button
                    >
                  </span>
                </template>
                $t('i18n_7f0abcf48d')
              </n-tooltip>

              <n-dropdown
                :options="[
                  { label: $t('i18n_95b351c862'), key: '0', props: { onClick: () => handleEdit(record) } },
                  { label: $t('i18n_2f4aaddde3'), key: '1', props: { onClick: () => handleDelete(record) } },
                  { label: $t('i18n_663393986e'), key: '2', props: { onClick: () => handleUnbind(record) } },
                  { type: 'divider', key: 'd1' },
                  {
                    label: $t('i18n_3d43ff1199'),
                    key: '3',
                    disabled: (listQuery.page - 1) * listQuery.limit + (index + 1) <= 1,
                    props: { onClick: () => sortItemHander(record, index, 'top') }
                  },
                  {
                    label: $t('i18n_315eacd193'),
                    key: '4',
                    disabled: (listQuery.page - 1) * listQuery.limit + (index + 1) <= 1,
                    props: { onClick: () => sortItemHander(record, index, 'up') }
                  },
                  {
                    label: $t('i18n_17acd250da'),
                    key: '5',
                    disabled: (listQuery.page - 1) * listQuery.limit + (index + 1) === listQuery.total,
                    props: { onClick: () => sortItemHander(record, index, 'down') }
                  }
                ]"
              >
                <a @click="(e) => e.preventDefault()">
                  {{ $t('i18n_0ec9eaf9c3') }}
                  <DownOutlined />
                </a>
              </n-dropdown>
            </n-space>
          </template>
        </template>
        <template #cardBodyCell="{ item }">
          <n-card :head-style="{ padding: '0 6px' }" :body-style="{ padding: '10px' }">
            <template #title>
              <n-grid :x-gap="[4, 0]">
                <n-grid-item :span="17" style="overflow: hidden; text-overflow: ellipsis; white-space: nowrap">
                  <n-tooltip>
                    <template #trigger>
                      <span class="tw">
                        <span class="tw">
                          <n-button text style="padding: 0" size="small" @click="handleNode(item)">
                            <span> {{ item.name }}</span>
                          </n-button>
                        </span>
                      </span>
                    </template>

                    {{ $t('i18n_8a4dbe88b8') }}
                    <div>{{ $t('i18n_5d83794cfa') }}{{ item.name }}</div>
                    <div>{{ $t('i18n_cab7517cb4') }}{{ item.url }}</div>
                  </n-tooltip>
                </n-grid-item>
                <n-grid-item :span="7" style="text-align: right">
                  <n-tooltip>
                    <template #trigger>
                      <span class="tw">
                        <n-tag
                          :color="item.machineNodeData && item.machineNodeData.status === 1 ? 'green' : 'pink'"
                          style="margin-right: 0"
                        >
                          {{ statusMap[item.machineNodeData && item.machineNodeData.status] }}
                        </n-tag>
                      </span>
                    </template>

                    <div>
                      {{ $t('i18n_e703c7367c') }}{{ statusMap[item.machineNodeData && item.machineNodeData.status] }}
                    </div>
                    <div>
                      {{ $t('i18n_fb3a2241bb') }}{{ (item.machineNodeData && item.machineNodeData.statusMsg) || '' }}
                    </div>
                  </n-tooltip>
                </n-grid-item>
              </n-grid>
            </template>

            <n-grid :x-gap="[8, 8]">
              <n-grid-item :span="8" style="text-align: center">
                <n-tooltip @click="handleHistory(item, 'nodeTop')">
                  <template #trigger>
                    <n-progress
                      type="circle"
                      :size="80"
                      :stroke-color="{
                        '0%': '#87d068',
                        '30%': '#87d068',
                        '100%': '#108ee9'
                      }"
                      status="active"
                      :percent="item.occupyCpu"
                    />
                  </template>
                  `CPU ${$t('i18n_b0fa44acbb')}${item.occupyCpu}%`
                </n-tooltip>
              </n-grid-item>
              <n-grid-item :span="8" style="text-align: center">
                <n-tooltip @click="handleHistory(item, 'nodeTop')">
                  <template #trigger>
                    <n-progress
                      type="circle"
                      :size="80"
                      :stroke-color="{
                        '0%': '#87d068',
                        '30%': '#87d068',
                        '100%': '#108ee9'
                      }"
                      status="active"
                      :percent="item.occupyDisk"
                    />
                  </template>
                  `${$t('i18n_570eb1c04f')}${item.occupyDisk}%`
                </n-tooltip>
              </n-grid-item>
              <n-grid-item :span="8" style="text-align: center">
                <n-tooltip @click="handleHistory(item, 'nodeTop')">
                  <template #trigger>
                    <n-progress
                      :size="80"
                      type="circle"
                      :stroke-color="{
                        '0%': '#87d068',
                        '30%': '#87d068',
                        '100%': '#108ee9'
                      }"
                      status="active"
                      :percent="item.occupyMemory"
                    />
                  </template>
                  `${$t('i18n_09e7d24952')}${item.occupyMemory}%`
                </n-tooltip>
              </n-grid-item>
            </n-grid>

            <n-grid :x-gap="[8, 8]" style="text-align: center">
              <n-grid-item :span="8">
                <n-tooltip @click="handleHistory(item, 'networkDelay')">
                  <template #trigger>
                    <span class="tw">
                      <n-statistic
                        :title="$t('i18n_db732ecb48')"
                        :value="item.machineNodeData && item.machineNodeData.networkDelay"
                        :value-style="statValueStyle"
                        :formatter="
                          (v) => {
                            return (
                              formatDuration(item.machineNodeData && item.machineNodeData.networkDelay, '', 2) || '-'
                            )
                          }
                        "
                      />
                    </span>
                  </template>
                  `${ $t('i18n_db732ecb48') + (formatDuration(item.machineNodeData && item.machineNodeData.networkDelay,
                  '', 2) || '-') + $t('i18n_69384c9d71') }`
                </n-tooltip>
              </n-grid-item>
              <n-grid-item :span="8">
                <n-tooltip>
                  <template #trigger>
                    <span class="tw">
                      <n-statistic
                        :title="$t('i18n_9f70e40e04')"
                        :value-style="statValueStyle"
                        :formatter="
                          (v) => {
                            return (
                              formatDuration(item.machineNodeData && item.machineNodeData.voyager1Uptime, '', 2) || '-'
                            )
                          }
                        "
                      />
                    </span>
                  </template>
                  formatDuration(item.machineNodeData && item.machineNodeData.voyager1Uptime, '', 1) || '-'
                </n-tooltip>
              </n-grid-item>
              <n-grid-item :span="8">
                <n-tooltip>
                  <template #trigger>
                    <span class="tw">
                      <n-statistic
                        :title="$t('i18n_a001a226fd')"
                        :value-style="statValueStyle"
                        :formatter="
                          (v) => {
                            return parseTime(item.machineNodeData && item.machineNodeData.modifyTimeMillis, 'HH:mm:ss')
                          }
                        "
                      />
                    </span>
                  </template>
                  `${parseTime(item.machineNodeData && item.machineNodeData.modifyTimeMillis)}`
                </n-tooltip>
              </n-grid-item>
            </n-grid>
          </n-card>
        </template>
      </CustomTable>
      <!-- <template v-else-if="layoutType === 'card'">
          <n-grid :x-gap="[16, 16]">
            <template v-if="list && list.length">
              <n-grid-item v-for="item in list" :key="item.id" :span="6">

              </n-grid-item>
            </template>
            <n-grid-item v-else :span="24">
              <n-empty  description="没有任何节点" />
            </n-grid-item>
          </n-grid>


        </template> -->
      <!-- </n-card> -->
    </template>

    <!-- 编辑区 -->
    <customModal
      v-if="editNodeVisible"
      v-model:open="editNodeVisible"
      destroy-on-close
      width="50%"
      :title="$t('i18n_61e7fa1227')"
      :confirm-loading="confirmLoading"
      :mask-closable="false"
      @ok="handleEditNodeOk"
    >
      <n-form ref="editNodeForm" :rules="rules" :model="temp">
        <n-form-item :label="$t('i18n_b1785ef01e')" path="name">
          <n-input v-model:value="temp.name" :max-length="50" :placeholder="$t('i18n_b1785ef01e')" />
        </n-form-item>
        <n-form-item :label="$t('i18n_1014b33d22')" path="group">
          <custom-select
            v-model:value="temp.group"
            :data="groupList"
            :input-placeholder="$t('i18n_bd0362bed3')"
            :select-placeholder="$t('i18n_9cac799f2f')"
          >
            <template #suffix>
              <n-tooltip>
                <template #trigger>
                  <QuestionCircleOutlined />
                </template>

                <div>
                  {{ $t('i18n_bd7c7abc8c') }}
                </div>
              </n-tooltip>
            </template>
          </custom-select>
        </n-form-item>

        <n-form-item :label="$t('i18n_b86224e030')" path="openStatus">
          <n-switch
            :checked="temp.openStatus == 1"
            :checked-label="$t('i18n_7854b52a88')"
            :unchecked-label="$t('i18n_5c56a88945')"
            default-checked
            @change="
              (checked) => {
                temp.openStatus = checked ? 1 : 0
              }
            "
          />
        </n-form-item>
        <n-form-item :label="$t('i18n_c5aae76124')" path="sshId">
          <n-select
            v-model:value="temp.sshId"
            filterable
            :placeholder="$t('i18n_260a3234f2')"
            :options="sshList.map((ssh) => ({ label: ssh.name, value: ssh.id }))"
          />
        </n-form-item>
      </n-form>
    </customModal>
    <!-- 管理节点 -->
    <NodeFunc v-if="drawerVisible" :id="temp.id" :name="temp.name" @close="onClose"></NodeFunc>
    <!-- Terminal -->
    <customModal
      v-if="terminalVisible"
      v-model:open="terminalVisible"
      :body-style="{
        padding: '0 10px',
        paddingTop: '10px',
        marginRight: '10px',
        height: `70vh`
      }"
      width="80%"
      title="Terminal"
      :footer="null"
      :mask-closable="false"
    >
      <terminal1 v-if="terminalVisible" :ssh-id="temp.sshId" :node-id="temp.id" />
    </customModal>

    <!-- 快速安装插件端 -->
    <CustomModal
      v-if="fastInstallNode"
      v-model:open="fastInstallNode"
      destroy-on-close
      width="80%"
      :title="$t('i18n_8f7a163ee9')"
      :footer="null"
      :mask-closable="false"
      @cancel="
        () => {
          fastInstallNode = false
          loadData()
        }
      "
    >
      <fastInstall v-if="fastInstallNode"></fastInstall>
    </CustomModal>
    <!-- 同步到其他工作空间 -->
    <customModal
      v-if="syncToWorkspaceVisible"
      v-model:open="syncToWorkspaceVisible"
      destroy-on-close
      :confirm-loading="confirmLoading"
      :title="$t('i18n_1a44b9e2f7')"
      :mask-closable="false"
      @ok="handleSyncToWorkspace"
    >
      <n-alert :title="$t('i18n_947d983961')" type="warning">
        <template #description>
          <ul>
            <li>{{ $t('i18n_6a4a0f2b3b') }}</li>
            <li>{{ $t('i18n_2611dd8703') }}</li>
            <li>{{ $t('i18n_cd998f12fa') }}</li>
          </ul>
        </template>
      </n-alert>
      <n-form :model="temp">
        <n-form-item> </n-form-item>
        <n-form-item :label="$t('i18n_b4a8c78284')" path="workspaceId">
          <n-select
            v-model:value="temp.workspaceId"
            filterable
            :placeholder="$t('i18n_b3bda9bf9e')"
            :options="workspaceList.map((item) => ({ label: item.name, value: item.id }))"
          />
        </n-form-item>
      </n-form>
    </customModal>
    <!-- 历史监控 -->
    <customModal
      v-if="monitorVisible"
      v-model:open="monitorVisible"
      destroy-on-close
      width="75%"
      :title="`${temp.name}${$t('i18n_5068552b18')}`"
      :footer="null"
      :mask-closable="false"
    >
      <node-top v-if="monitorVisible" :type="temp.type" :node-id="temp.id"></node-top>
    </customModal>
  </div>
</template>
<script>
import {
  CodeOutlined,
  DownOutlined,
  FullscreenOutlined,
  QuestionCircleOutlined,
  SyncOutlined
} from '@ant-design/icons-vue'

import { mapState } from 'pinia'
import {
  deleteNode,
  editNode,
  getNodeGroupAll,
  getNodeList,
  syncProject,
  syncToWorkspace,
  unbind,
  sortItem
} from '@/api/node'
import { getSshListAll } from '@/api/ssh'
import { syncScript } from '@/api/node-other'
import NodeFunc from './node-func'
import Terminal1 from '@/pages/ssh/terminal'
import {
  CHANGE_PAGE,
  COMPUTED_PAGINATION,
  PAGE_DEFAULT_LIST_QUERY,
  formatDuration,
  renderSize,
  formatPercent2Number,
  parseTime
  // PAGE_DEFAULT_SHOW_TOTAL,
  // getCachePageLimit
} from '@/utils/const'
import { getWorkSpaceListAll } from '@/api/workspace'
import CustomSelect from '@/components/customSelect'
import fastInstall from './fast-install.vue'
import { statusMap } from '@/api/system/assets-machine'
import NodeTop from '@/pages/node/node-layout/node-top'
import { useUserStore } from '@/stores/user'
import { useAppStore } from '@/stores/app'
import { NEmpty as Empty } from 'naive-ui'
export default {
  components: {
    NodeFunc,
    Terminal1,
    CustomSelect,
    fastInstall,
    NodeTop
  },
  data() {
    return {
      loading: true,
      Empty,
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      statusMap,
      sshList: [],
      list: [],
      sizeOptions: ['8', '12', '16', '20', '24'],
      groupList: [],
      // refreshInterval: 5,
      // deadline: 0,
      temp: {},
      monitorVisible: false,
      layoutType: null,
      editNodeVisible: false,
      drawerVisible: false,
      terminalVisible: false,

      fastInstallNode: false,
      syncToWorkspaceVisible: false,

      columns: [
        {
          title: this.$t('i18n_b1785ef01e'),
          key: 'name',
          width: 200,
          sorter: true,
          key: 'name',
          ellipsis: true
        },
        {
          title: this.$t('i18n_3fea7ca76c'),
          key: 'status',
          width: '100px',
          ellipsis: true
        },
        {
          title: this.$t('i18n_c1786d9e11'),
          key: 'url',
          key: 'url',
          width: '190px',
          ellipsis: true
        },
        {
          title: this.$t('i18n_cdc478d90c'),
          key: 'osName',
          key: 'osName',
          width: '100px',
          ellipsis: true
        },
        {
          title: `JDK ${this.$t('i18n_fe2df04a16')}`,
          key: 'javaVersion',
          width: 100,
          key: 'javaVersion',
          ellipsis: true
        },
        {
          title: `JVM ${this.$t('i18n_d8c7e04c8e')}`,
          key: 'jvmInfo',
          width: 100,
          ellipsis: true
        },
        // { title: "JVM 剩余内存", key: "machineNodeData.jvmFreeMemory", ellipsis: true, },

        {
          title: this.$t('i18n_607558dbd4'),
          key: 'voyager1ProjectCount',
          width: '90px'
        },
        {
          title: this.$t('i18n_e39f4a69f4'),
          key: 'voyager1ScriptCount',
          width: '90px'
        },

        {
          title: this.$t('i18n_5fffcb255d'),
          key: 'runTime',
          width: '100px',
          key: 'runTime',
          ellipsis: true
        },
        {
          title: this.$t('i18n_eca37cb072'),
          key: 'createTimeMillis',
          ellipsis: true,
          sorter: true,
          render: (row) => parseTime(row['createTimeMillis']),
          width: '170px'
        },
        {
          title: this.$t('i18n_1303e638b5'),
          key: 'modifyTimeMillis',
          ellipsis: true,
          sorter: true,
          render: (row) => parseTime(row['modifyTimeMillis']),
          width: '170px'
        },
        {
          title: this.$t('i18n_c35c1a1330'),
          key: 'sortValue',
          sorter: true,
          width: '80px'
        },
        {
          title: this.$t('i18n_2b6bc0f293'),
          key: 'operation',
          key: 'operation',
          fixed: 'right',
          width: '200px',

          align: 'center'
        }
      ],

      rules: {
        name: [{ required: true, message: this.$t('i18n_32cb0ec70e'), trigger: 'blur' }]
      },
      workspaceList: [],
      tableSelections: [],
      statValueStyle: {
        fontSize: '14px',
        overflow: 'hidden',
        textOverflow: 'ellipsis',
        whiteSpace: 'nowrap'
      },
      confirmLoading: false
    }
  },
  computed: {
    ...mapState(useUserStore, ['getUserInfo']),
    ...mapState(useAppStore, ['getWorkspaceId']),
    activePage() {
      return this.$attrs.routerUrl === this.$route.path
    },
    pagination() {
      return COMPUTED_PAGINATION(this.listQuery)
    },
    useSuggestions() {
      if (this.loading) {
        // 加载中不提示
        return false
      }
      if (!this.getUserInfo || !this.getUserInfo.systemUser) {
        // 没有登录或者不是超级管理员
        return false
      }
      if (this.listQuery.page !== 1 || this.listQuery.total > 0) {
        // 不是第一页 或者总记录数大于 0
        return false
      }
      // 判断是否存在搜索条件
      const nowKeys = Object.keys(this.listQuery)
      const defaultKeys = Object.keys(PAGE_DEFAULT_LIST_QUERY)
      const dictOrigin = nowKeys.filter((item) => !defaultKeys.includes(item))
      return dictOrigin.length === 0
    },
    rowSelection() {
      return {
        onChange: (selectedRowKeys) => {
          this.tableSelections = selectedRowKeys
        },
        selectedRowKeys: this.tableSelections
      }
    }
  },
  watch: {},
  created() {
    const searchNodeName = this.$route.query.searchNodeName
    if (searchNodeName) {
      this.listQuery = { ...this.listQuery, '%name%': searchNodeName }
    }

    this.loadData()
    this.loadGroupList()
  },

  methods: {
    formatDuration,
    renderSize,
    // PAGE_DEFAULT_SHOW_TOTAL,
    parseTime,
    CHANGE_PAGE,
    // 获取所有的分组
    loadGroupList() {
      getNodeGroupAll().then((res) => {
        if (res.data) {
          this.groupList = res.data
        }
      })
    },
    // 加载 SSH 列表
    loadSshList() {
      getSshListAll().then((res) => {
        if (res.code === 200) {
          this.sshList = res.data
        }
      })
    },
    // 加载数据
    loadData(pointerEvent) {
      return new Promise((resolve) => {
        this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page
        this.loading = true
        getNodeList(this.listQuery)
          .then((res) => {
            if (res.code === 200) {
              this.list =
                res.data.result &&
                res.data.result.map((item) => {
                  // console.log(item);
                  item.occupyCpu = formatPercent2Number(item.machineNodeData?.osOccupyCpu)

                  item.occupyDisk = formatPercent2Number(item.machineNodeData?.osOccupyDisk)
                  item.occupyMemory = formatPercent2Number(item.machineNodeData?.osOccupyMemory)
                  return item
                })
              this.listQuery.total = res.data.total
              let nodeId = this.$route.query.nodeId
              this.list.map((item) => {
                if (nodeId === item.id) {
                  this.handleNode(item)
                }
              })

              resolve()
              // this.refreshInterval = 30
              // this.deadline = Date.now() + this.refreshInterval * 1000
            }
          })
          .finally(() => {
            this.loading = false
          })
      })
    },

    // 进入终端
    handleTerminal(record) {
      this.temp = Object.assign({}, record)
      this.terminalVisible = true
    },
    // 修改
    handleEdit(record) {
      this.temp = Object.assign({}, record)
      this.loadSshList()
      // this.temp.tempGroup = "";
      this.editNodeVisible = true
    },
    // 提交节点数据
    handleEditNodeOk() {
      // 检验表单
      this.$refs['editNodeForm'].validate().then(() => {
        // 提交数据
        this.confirmLoading = true
        editNode(this.temp)
          .then((res) => {
            if (res.code === 200) {
              // 成功
              $notification.success({
                message: res.msg
              })
              this.$refs['editNodeForm'].resetFields()
              this.editNodeVisible = false
              this.loadData()
              this.loadGroupList()
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
        zIndex: 1009,
        content: this.$t('i18n_6636793319'),
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
        onOk: () => {
          return deleteNode(record.id).then((res) => {
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
    // 解绑
    handleUnbind(record) {
      const html = `
      <b style='font-size: 20px;'>${this.$t('i18n_db5cafdc67')}</b>
      <ul style='font-size: 20px;color:red;font-weight: bold;'>
        <li>${this.$t('i18n_eeef8ced69')}</li>
        <li>${this.$t('i18n_5c93055d9c')}</li>
        <li>${this.$t('i18n_27d0c8772c')}</li>
      </ul>

      `
      $confirm({
        title: this.$t('i18n_9362e6ddf8'),
        zIndex: 1009,
        content: h('div', null, [h('p', { innerHTML: html }, null)]),
        okButtonProps: { size: 'small', danger: true, type: 'primary' },
        cancelButtonProps: { type: 'primary' },
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
        onOk: () => {
          return unbind(record.id).then((res) => {
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
    // 管理节点
    handleNode(record) {
      this.temp = Object.assign({}, record)

      this.drawerVisible = true
      let nodeId = this.$route.query.nodeId
      if (nodeId !== record.id) {
        this.$router.push({
          query: { ...this.$route.query, nodeId: record.id }
        })
      }
    },
    syncNode(node) {
      syncProject(node.id).then((res) => {
        if (res.code == 200) {
          $notification.success({
            message: res.msg
          })
          return false
        }
      })
    },
    syncNodeScript(node) {
      syncScript({
        nodeId: node.id
      }).then((res) => {
        if (res.code == 200) {
          $notification.success({
            message: res.msg
          })
        }
      })
    },
    // 关闭抽屉层
    onClose() {
      this.drawerVisible = false
      let query = Object.assign({}, this.$route.query)
      ;(delete query.nodeId, delete query.id, delete query.pId)
      this.$router.replace({
        query: query
      })
    },

    // 加载工作空间数据
    loadWorkSpaceListAll() {
      getWorkSpaceListAll().then((res) => {
        if (res.code === 200) {
          this.workspaceList = res.data
        }
      })
    },

    // 同步到其他工作情况
    syncToWorkspaceShow() {
      this.syncToWorkspaceVisible = true
      this.loadWorkSpaceListAll()
      this.temp = {
        workspaceId: undefined
      }
    },
    //
    handleSyncToWorkspace() {
      if (!this.temp.workspaceId) {
        $notification.warn({
          message: this.$t('i18n_b3bda9bf9e')
        })
        return false
      }
      // 同步
      this.confirmLoading = true
      syncToWorkspace({
        ids: this.tableSelections.join(','),
        toWorkspaceId: this.temp.workspaceId
      })
        .then((res) => {
          if (res.code == 200) {
            $notification.success({
              message: res.msg
            })
            this.tableSelections = []
            this.syncToWorkspaceVisible = false
            return false
          }
        })
        .finally(() => {
          this.confirmLoading = false
        })
    },
    // 排序
    sortItemHander(record, index, method) {
      const msgData = {
        top: this.$t('i18n_0079d91f95'),
        up: this.$t('i18n_b166a66d67'),
        down: this.$t('i18n_7a7e25e9eb')
      }
      let msg = msgData[method] || this.$t('i18n_49574eee58')
      if (!record.sortValue) {
        msg += `${this.$t('i18n_57c0a41ec6')},${this.$t('i18n_066f903d75')},${this.$t('i18n_c4e2cd2266')}`
      }
      // console.log(this.list, index, this.list[method === "top" ? index : method === "up" ? index - 1 : index + 1]);
      const compareId = this.list[method === 'top' ? index : method === 'up' ? index - 1 : index + 1].id
      $confirm({
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        content: msg,
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
        onOk: () => {
          return sortItem({
            id: record.id,
            method: method,
            compareId: compareId
          }).then((res) => {
            if (res.code == 200) {
              $notification.success({
                message: res.msg
              })
              this.loadData()
            }
          })
        }
      })
    },
    // // 切换视图
    // changeLayout() {
    //   if (!this.layoutType) {
    //     const layoutType = localStorage.getItem('tableLayout')
    //     // 默认表格
    //     this.layoutType = layoutType === 'card' ? 'card' : 'table'
    //   } else {
    //     this.layoutType = this.layoutType === 'card' ? 'table' : 'card'
    //     localStorage.setItem('tableLayout', this.layoutType)
    //   }
    //   this.listQuery = {
    //     ...this.listQuery,
    //     limit: this.layoutType === 'card' ? 8 : getCachePageLimit()
    //   }
    //   this.loadData()
    // },
    onFinish() {
      if (this.drawerVisible) {
        // 打开节点 不刷新
        return
      }
      if (this.$attrs.routerUrl !== this.$route.path) {
        // 重新计算倒计时
        // this.deadline = Date.now() + this.refreshInterval * 1000
        return
      }
      this.loadData()
    },
    // 历史图表
    handleHistory(record, type) {
      this.monitorVisible = true
      this.temp = record
      this.temp = { ...this.temp, type }
    },
    fastInstallNodeShow() {
      this.fastInstallNode = true
    }
  }
}
</script>
