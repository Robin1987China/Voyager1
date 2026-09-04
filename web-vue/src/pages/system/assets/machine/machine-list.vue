<template>
  <div>
    <!-- <n-card :body-style="{ padding: '10px' }"> -->
    <!-- 卡片视图 -->
    <!-- <template v-if="layoutType === 'card'"> </template> -->
    <!-- 表格视图 -->
    <!-- <template v-else-if="layoutType === 'table'"> -->
    <CustomTable
      is-show-tools
      default-auto-refresh
      :auto-refresh-time="30"
      table-name="buildList"
      :empty-description="$t('i18n_de3394b14e')"
      :active-page="activePage"
      :columns="columns"
      :data="list"
      bordered
      size="medium"
      row-key="id"
      :pagination="pagination"
      :row-selection="rowSelection"
      :scroll="{
        x: 'max-content'
      }"
      @change="changePage"
      @refresh="getMachineList"
      @change-table-layout="
        (layoutType) => {
          tableSelections = []
          listQuery = CHANGE_PAGE(listQuery, {
            pagination: { limit: layoutType === 'card' ? 8 : 10 }
          })
          getMachineList()
        }
      "
    >
      <template #title>
        <n-space wrap class="search-box">
          <n-input
            v-model:value="listQuery['%name%']"
            class="search-input-item"
            :placeholder="$t('i18n_e4013f8b81')"
            @press-enter="getMachineList"
          />
          <n-input
            v-model:value="listQuery['%voyager1Url%']"
            class="search-input-item"
            :placeholder="$t('i18n_c1786d9e11')"
            @press-enter="getMachineList"
          />
          <n-input
            v-model:value="listQuery['%voyager1Version%']"
            class="search-input-item"
            :placeholder="$t('i18n_a912a83e6f')"
            @press-enter="getMachineList"
          />
          <n-select
            v-model:value="listQuery.groupName"
            filterable
            clearable
            :placeholder="$t('i18n_829abe5a8d')"
            class="search-input-item"
            :options="groupList"
          />
          <n-select
            v-model:value="listQuery['order_field']"
            clearable
            :placeholder="$t('i18n_88f5c7ac4a')"
            class="search-input-item"
            :options="[
              { label: $t('i18n_204222d167'), value: 'networkDelay' },
              { label: 'cpu', value: 'osOccupyCpu' },
              { label: $t('i18n_1d650a60a5'), value: 'osOccupyDisk' },
              { label: $t('i18n_9932551cd5'), value: 'osOccupyMemory' },
              { label: $t('i18n_a001a226fd'), value: 'modifyTimeMillis' },
              { label: $t('i18n_eca37cb072'), value: 'createTimeMillis' }
            ]"
          />
          <n-button :loading="loading" type="primary" @click="getMachineList">{{ $t('i18n_e5f71fc31e') }}</n-button>
          <n-button type="primary" @click="addMachine">{{ $t('i18n_66ab5e9f24') }}</n-button>

          <n-dropdown
            v-if="tableSelections && tableSelections.length"
            :options="[
              { label: $t('i18n_5c89a5353d'), key: '1', props: { onClick: () => syncToWorkspaceShow() } },
              { label: $t('i18n_542a0e7db4'), key: '2', props: { onClick: () => syncNodeWhiteConfig() } },
              { label: $t('i18n_51c92e6956'), key: '3', props: { onClick: () => syncNodeConfig() } }
            ]"
          >
            <n-button type="primary"> {{ $t('i18n_7f7c624a84') }} <DownOutlined /> </n-button>
          </n-dropdown>
          <n-tooltip v-else>
            <template #trigger>
              <span class="tw">
                <n-button :disabled="true" type="primary"> {{ $t('i18n_7f7c624a84') }}<DownOutlined /></n-button>
              </span>
            </template>
            $t('i18n_98cd2bdc03')
          </n-tooltip>
        </n-space>
      </template>
      <template #tableHelp>
        <n-tooltip>
          <template #trigger>
            <QuestionCircleOutlined />
          </template>

          <ul>
            <li>{{ $t('i18n_9b74c734e5') }}</li>
            <li>{{ $t('i18n_e1fefde80f') }}</li>
            <li>{{ $t('i18n_39b68185f0') }}</li>
          </ul>
        </n-tooltip>
      </template>
      <template #tableBodyCell="{ column, text, record }">
        <template v-if="column.dataIndex === 'name'">
          <n-tooltip>
            <template #trigger>
              <span class="tw">
                <n-button style="padding: 0" text size="small" @click="showMachineInfo(record)">
                  {{ text }}
                </n-button>
              </span>
            </template>
            text
          </n-tooltip>
        </template>
        <template v-else-if="column.tooltip">
          <n-tooltip>
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
        <template v-else-if="column.dataIndex === 'status'">
          <n-tooltip>
            <template #trigger>
              <span class="tw">
                <n-tag :color="record.status === 1 ? 'green' : 'pink'" style="margin-right: 0">
                  {{ statusMap[record.status] }}
                </n-tag>
              </span>
            </template>
            `${$t('i18n_e703c7367c')}${statusMap[record.status]} ${ record.statusMsg ? $t('i18n_8d13037eb7') +
            record.statusMsg : $t('i18n_77e100e462') } `
          </n-tooltip>
        </template>
        <template v-else-if="column.duration">
          <n-tooltip placement="topLeft">
            <template #trigger>
              <span class="tw">
                <span class="tw">
                  <span>{{ formatDuration(text, '', 2) }}</span>
                </span>
              </span>
            </template>
            formatDuration(text)
          </n-tooltip>
        </template>
        <template v-else-if="column.duration2">
          <n-tooltip placement="topLeft">
            <template #trigger>
              <span class="tw">
                <span class="tw">
                  <span>{{ formatDuration((text || 0) * 1000, '', 2) }}</span>
                </span>
              </span>
            </template>
            formatDuration((text || 0) * 1000)
          </n-tooltip>
        </template>
        <template v-else-if="column.percent2Number">
          <n-tooltip placement="topLeft">
            <template #trigger>
              <span class="tw">
                <span class="tw">
                  <span>{{ (text && formatPercent2Number(text) + '%') || '-' }}</span>
                </span>
              </span>
            </template>
            `${(text && formatPercent2Number(text) + '%') || '-'}`
          </n-tooltip>
        </template>

        <template v-else-if="column.dataIndex === 'operation'">
          <n-space>
            <n-button type="primary" size="small" @click="handleEdit(record)">{{ $t('i18n_95b351c862') }}</n-button>
            <n-button type="primary" size="small" @click="syncToWorkspaceShow(record)">{{
              $t('i18n_e39de3376e')
            }}</n-button>
            <n-button type="primary" danger size="small" @click="deleteMachineInfo(record)">{{
              $t('i18n_2f4aaddde3')
            }}</n-button>
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
                        <span style="cursor: pointer" @click="showMachineInfo(item)">
                          {{ item.name }}
                        </span>
                      </span>
                    </span>
                  </template>

                  <div>{{ $t('i18n_5d83794cfa') }}{{ item.name }}</div>
                  <div>{{ $t('i18n_c1786d9e11') }}：{{ item.voyager1Url }}</div>
                </n-tooltip>
              </n-grid-item>
              <n-grid-item :span="7" style="text-align: right" class="text-overflow-hidden">
                <n-tooltip>
                  <template #trigger>
                    <span class="tw">
                      <n-tag :color="item.status === 1 ? 'green' : 'pink'" style="margin-right: 0">
                        {{ statusMap[item.status] }}</n-tag
                      >
                    </span>
                  </template>
                  `${$t('i18n_e703c7367c')}${statusMap[item.status]} ${ item.statusMsg ? $t('i18n_8d13037eb7') +
                  item.statusMsg : $t('i18n_77e100e462') } `
                </n-tooltip>
              </n-grid-item>
            </n-grid>
          </template>

          <n-tooltip>
            <template #trigger>
              <span class="tw">
                <n-grid class="item-info">
                  <n-grid-item :span="6" class="title text-overflow-hidden">{{ $t('i18n_2027743b8d') }}</n-grid-item>
                  <n-grid-item :span="18" class="content text-overflow-hidden">
                    <n-button
                      :disabled="!item.osName"
                      style="padding: 0; height: auto"
                      text
                      size="small"
                      @click="showMachineInfo(item)"
                    >
                      {{ item.osName || '-' }}
                    </n-button>
                  </n-grid-item>
                </n-grid>
              </span>
            </template>
            item.osName
          </n-tooltip>
          <n-tooltip>
            <template #trigger>
              <span class="tw">
                <n-grid class="item-info">
                  <n-grid-item :span="6" class="title text-overflow-hidden">{{ $t('i18n_3006a3da65') }}</n-grid-item>
                  <n-grid-item :span="18" class="content text-overflow-hidden">
                    {{ item.osVersion || '-' }}
                  </n-grid-item>
                </n-grid>
              </span>
            </template>
            item.osVersion
          </n-tooltip>
          <n-tooltip>
            <template #trigger>
              <span class="tw">
                <n-grid class="item-info">
                  <n-grid-item :span="6" class="title text-overflow-hidden">{{ $t('i18n_9e96d9c8d3') }}</n-grid-item>
                  <n-grid-item :span="18" class="content text-overflow-hidden">
                    {{ item.osLoadAverage || '-' }}
                  </n-grid-item>
                </n-grid>
              </span>
            </template>
            item.osLoadAverage
          </n-tooltip>
          <n-tooltip>
            <template #trigger>
              <span class="tw">
                <n-grid class="item-info">
                  <n-grid-item :span="6" class="title text-overflow-hidden">{{ $t('i18n_4a346aae15') }}</n-grid-item>
                  <n-grid-item :span="18" class="content text-overflow-hidden">
                    <n-button
                      :disabled="!item.voyager1Version"
                      style="padding: 0; height: auto"
                      text
                      size="small"
                      @click="showMachineUpgrade(item)"
                    >
                      {{ item.voyager1Version || '-' }}
                    </n-button>
                  </n-grid-item>
                </n-grid>
              </span>
            </template>
            item.voyager1Version
          </n-tooltip>
          <n-grid type="flex" align="middle" justify="center" style="margin-top: 10px">
            <n-button-group>
              <n-button type="primary" size="small" @click="handleEdit(item)">
                {{ $t('i18n_95b351c862') }}
              </n-button>
              <n-button type="primary" size="small" @click="showMachineInfo(item)">{{
                $t('i18n_f26225bde6')
              }}</n-button>
              <n-button type="primary" size="small" @click="syncToWorkspaceShow(item)">{{
                $t('i18n_e39de3376e')
              }}</n-button>
              <n-button type="primary" size="small" @click="viewMachineNode(item)">{{
                $t('i18n_3bf3c0a8d6')
              }}</n-button>
              <n-button size="small" @click="deleteMachineInfo(item)">{{ $t('i18n_2f4aaddde3') }}</n-button>
            </n-button-group>
          </n-grid>
        </n-card>
      </template>
    </CustomTable>
    <!-- </template> -->
    <!-- </n-card> -->
    <!-- 编辑区 -->
    <CustomModal
      v-if="editVisible"
      v-model:open="editVisible"
      destroy-on-close
      :confirm-loading="confirmLoading"
      width="50%"
      :title="$t('i18n_6eb39e706c')"
      :mask-closable="false"
      @ok="handleEditOk"
    >
      <n-form ref="editNodeForm" :rules="rules" :model="temp">
        <n-form-item :label="$t('i18n_e4013f8b81')" path="name">
          <n-input v-model:value="temp.name" :max-length="50" :placeholder="$t('i18n_e4013f8b81')" />
        </n-form-item>
        <n-form-item :label="$t('i18n_93e1df604a')" path="groupName">
          <custom-select
            v-model:value="temp.groupName"
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

        <n-form-item path="voyager1Url">
          <template #label>
            <n-tooltip>
              <template #trigger>
                <span class="tw">
                  {{ $t('i18n_c1786d9e11') }}

                  <QuestionCircleOutlined v-show="!temp.id" />
                </span>
              </template>
              {{ $t('i18n_899fe0c5dd') }}3
              <ul>
                <li>{{ $t('i18n_9c3c05d91b') }}</li>
                <li>{{ $t('i18n_1ece1616bf') }}</li>
              </ul>
            </n-tooltip>
          </template>
          <template #help>{{ $t('i18n_6f8da7dcca') }}</template>
          <n-input v-model:value="temp.voyager1Url" :placeholder="$t('i18n_1235b052ff')">
            <template #prefix>
              <n-select
                v-model:value="temp.voyager1Protocol"
                :placeholder="$t('i18n_e825ec7800')"
                style="width: 160px"
                :options="[
                  { label: 'Http://', value: 'Http' },
                  { label: 'Https://', value: 'Https' }
                ]"
              />
            </template>
          </n-input>
        </n-form-item>

        <n-form-item :label="$t('i18n_86fb7b5421')" path="loginName">
          <n-input v-model:value="temp.voyager1Username" :placeholder="$t('i18n_f8460626f0')" />
          <template #help>{{ $t('i18n_eec342f34e') }}</template>
        </n-form-item>
        <n-form-item :path="`${temp.id ? 'loginPwd-update' : 'loginPwd'}`">
          <template #label>
            <n-tooltip>
              <template #trigger>
                <span class="tw">
                  {{ $t('i18n_8bd3f73502') }}

                  <QuestionCircleOutlined v-show="!temp.id" />
                </span>
              </template>

              {{ $t('i18n_1062619d5a') }}_authorize.json
              {{ $t('i18n_ff3bdecc5e') }}
            </n-tooltip>
          </template>
          <n-input v-model:value="temp.voyager1Password" type="password" :placeholder="$t('i18n_e5a63852fd')" />
        </n-form-item>

        <n-collapse>
          <n-collapse-item key="1" :header="$t('i18n_9ab433e930')">
            <n-form-item :label="$t('i18n_04edc35414')" path="templateNode" help="">
              <n-switch
                v-model:value="temp.templateNode"
                :checked-label="$t('i18n_0a60ac8f02')"
                :unchecked-label="$t('i18n_c9744f45e7')"
                default-checked
              />
              {{ $t('i18n_8e34aa1a59') }},{{ $t('i18n_715ec3b393') }}
            </n-form-item>

            <n-form-item :label="$t('i18n_67425c29a5')" path="timeOut">
              <n-input-number
                v-model:value="temp.voyager1Timeout"
                :min="0"
                :placeholder="$t('i18n_84d331a137')"
                style="width: 100%"
              />
            </n-form-item>

            <n-form-item :label="$t('i18n_fc954d25ec')" path="voyager1HttpProxy">
              <n-input v-model:value="temp.voyager1HttpProxy" :placeholder="$t('i18n_dcf14deb0e')">
                <template #prefix>
                  <n-select
                    v-model:value="temp.voyager1HttpProxyType"
                    :placeholder="$t('i18n_b04070fe42')"
                    default-value="HTTP"
                    style="width: 100px"
                    :options="[
                      { label: 'HTTP', value: 'HTTP' },
                      { label: 'SOCKS', value: 'SOCKS' },
                      { label: 'DIRECT', value: 'DIRECT' }
                    ]"
                  />
                </template>
              </n-input>
            </n-form-item>

            <n-form-item :label="$t('i18n_7156088c6e')" path="transportEncryption">
              <n-select
                v-model:value="temp.transportEncryption"
                filterable
                default-value="0"
                :placeholder="$t('i18n_3c8eada338')"
                :options="[
                  { label: $t('i18n_8a3e316cd7'), value: 0 },
                  { label: 'BASE64', value: 1 },
                  { label: 'AES', value: 2 }
                ]"
              />
            </n-form-item>
          </n-collapse-item>
        </n-collapse>
      </n-form>
    </CustomModal>
    <!-- 机器信息组件 -->

    <machine-info
      v-if="drawerVisible"
      :machine-id="temp.id"
      :name="temp.name"
      @close="
        () => {
          drawerVisible = false
        }
      "
    />
    <!-- 机器在线升级相关信息 -->
    <machine-info
      v-if="drawerUpgradeVisible"
      :machine-id="temp.id"
      :name="temp.name"
      tab="upgrade"
      @close="
        () => {
          drawerUpgradeVisible = false
        }
      "
    />

    <!-- 分配到其他工作空间 -->
    <CustomModal
      v-if="syncToWorkspaceVisible"
      v-model:open="syncToWorkspaceVisible"
      destroy-on-close
      :confirm-loading="confirmLoading"
      :title="$t('i18n_ef8525efce')"
      :mask-closable="false"
      @ok="handleSyncToWorkspace"
    >
      <n-form :model="temp">
        <n-form-item :label="$t('i18n_b4a8c78284')" path="workspaceId">
          <n-select
            v-model:value="temp.workspaceId"
            filterable
            :placeholder="$t('i18n_b3bda9bf9e')"
            :options="workspaceList.map((item) => ({ label: item.name, value: item.id }))"
          />
        </n-form-item>
      </n-form>
    </CustomModal>

    <!-- 查看机器关联节点 -->
    <CustomModal
      v-if="viewLinkNode"
      v-model:open="viewLinkNode"
      destroy-on-close
      width="50%"
      :title="$t('i18n_222316382d')"
      :footer="null"
      :mask-closable="false"
    >
      <n-space direction="vertical" style="width: 100%">
        <n-alert v-if="nodeList && nodeList.length" :title="$t('i18n_566c67e764')" type="info" show-icon />
        <n-list bordered>
<n-list-item v-for="(item, index) in nodeList" :key="index" style="display: block">
              <n-grid>
                <n-grid-item :span="10">{{ $t('i18n_5d83794cfa') }}{{ item.name }}</n-grid-item>
                <n-grid-item :span="10"
                  >{{ $t('i18n_2358e1ef49') }}{{ item.workspace && item.workspace.name }}</n-grid-item
                >
                <n-grid-item :span="4">
                  <n-button text @click="toNode(item.id, item.name, item.workspace && item.workspace.id)">
                    <LoginOutlined /> </n-button
                ></n-grid-item>
              </n-grid>
            </n-list-item>
</n-list>
      </n-space>
    </CustomModal>
    <!-- 分发节点授权 -->
    <CustomModal
      v-if="whiteConfigVisible"
      v-model:open="whiteConfigVisible"
      destroy-on-close
      :confirm-loading="confirmLoading"
      width="50%"
      :title="$t('i18n_e19cc5ed70')"
      :mask-closable="false"
      @ok="onSubmitWhitelist"
    >
      <n-alert
        :title="`${$t('i18n_6fa1229ea9')},${$t('i18n_acf14aad3c')},${$t('i18n_332ba869d9')}`"
        style="margin-top: 10px; margin-bottom: 20px"
        banner
      />
      <n-form ref="editWhiteForm" :model="temp">
        <n-form-item :label="$t('i18n_04edc35414')">
          <n-select
            v-model:value="temp.templateNodeId"
            filterable
            :placeholder="$t('i18n_8d92fb62a7')"
            :options="templateNodeList.map((item) => ({ label: item.name, value: item.id }))"
            @update:value="(id) => loadWhitelistData(id)"
          />
        </n-form-item>
        <n-form-item :label="$t('i18n_aabdc3b7c0')" path="project">
          <n-input
            v-model:value="temp.project"
            type="textarea"
            :rows="5"
            style="resize: none"
            :placeholder="$t('i18n_631d5b88ab')"
          />
        </n-form-item>

        <n-form-item :label="$t('i18n_649231bdee')" path="allowEditSuffix">
          <n-input
            v-model:value="temp.allowEditSuffix"
            type="textarea"
            :rows="5"
            style="resize: none"
            :placeholder="$t('i18n_afa8980495')"
          />
        </n-form-item>
      </n-form>
    </CustomModal>
    <!-- 分发机器配置 -->
    <CustomModal
      v-if="nodeConfigVisible"
      v-model:open="nodeConfigVisible"
      destroy-on-close
      :confirm-loading="confirmLoading"
      width="50%"
      :title="$t('i18n_6f8907351b')"
      :mask-closable="false"
    >
      <template #footer>
        <n-space>
          <n-button type="primary" :disabled="!temp.content" @click="onNodeSubmit(false)">{{
            $t('i18n_be5fbbe34c')
          }}</n-button>
          <n-button type="primary" :disabled="!temp.content" @click="onNodeSubmit(true)">{{
            $t('i18n_6aab88d6a3')
          }}</n-button>
        </n-space>
      </template>
      <n-alert
        :title="`${$t('i18n_10c385b47e')},${$t('i18n_acf14aad3c')},${$t('i18n_332ba869d9')}`"
        style="margin-top: 10px; margin-bottom: 20px"
        banner
      />
      <n-form ref="editNodeConfigForm" :model="temp">
        <n-form-item :label="$t('i18n_798f660048')">
          <n-select
            v-model:value="temp.templateNodeId"
            filterable
            :placeholder="$t('i18n_353c7f29da')"
            :options="templateNodeList.map((item) => ({ label: item.name, value: item.id }))"
            @update:value="(id) => loadNodeConfig(id)"
          />
        </n-form-item>

        <n-form-item>
          <code-editor
            v-model:content="temp.content"
            height="40vh"
            :options="{ mode: 'yaml', tabSize: 2 }"
          ></code-editor>
        </n-form-item>
      </n-form>
    </CustomModal>
  </div>
</template>
<script>
import { DownOutlined, LoginOutlined, QuestionCircleOutlined } from '@ant-design/icons-vue'

import {
  machineListData,
  machineListGroup,
  statusMap,
  machineEdit,
  machineDelete,
  machineDistribute,
  machineListNode,
  machineListTemplateNode,
  saveWhitelist,
  saveNodeConfig
} from '@/api/system/assets-machine'
import {
  CHANGE_PAGE,
  COMPUTED_PAGINATION,
  PAGE_DEFAULT_LIST_QUERY,
  // PAGE_DEFAULT_SHOW_TOTAL,
  formatDuration,
  parseTime,
  formatPercent2Number
  // getCachePageLimit
} from '@/utils/const'
import CustomSelect from '@/components/customSelect'
import { useAppStore } from '@/stores/app'
import { mapState } from 'pinia'
import machineInfo from './machine-func'
import { getWorkSpaceListAll } from '@/api/workspace'
// import Upgrade from "@/pages/node/node-layout/system/upgrade.vue";

import { getWhiteList } from '@/api/node-system'
import { getConfigData } from '@/api/system'
import codeEditor from '@/components/codeEditor'

export default {
  components: {
    CustomSelect,
    machineInfo,

    codeEditor
  },
  data() {
    return {
      statusMap,
      listQuery: Object.assign({ order: 'descend', order_field: 'networkDelay' }, PAGE_DEFAULT_LIST_QUERY, {}),
      // sizeOptions: ['8', '12', '16', '20', '24'],
      list: [],
      groupList: [],
      loading: true,
      editVisible: false,
      syncToWorkspaceVisible: false,
      temp: {},
      rules: {
        name: [{ required: true, message: this.$t('i18n_cbdc4f58f6'), trigger: 'blur' }]
      },
      drawerVisible: false,
      drawerUpgradeVisible: false,
      workspaceList: [],
      viewLinkNode: false,
      nodeList: [],
      layoutType: null,
      columns: [
        {
          title: this.$t('i18n_d7ec2d3fea'),
          key: 'name',
          width: 150,
          ellipsis: true
        },
        {
          title: this.$t('i18n_cdc478d90c'),
          key: 'osName',
          width: 150,
          ellipsis: true
        },
        {
          title: this.$t('i18n_6707667676'),
          key: 'hostName',
          width: 150,
          ellipsis: true
        },
        {
          title: this.$t('i18n_c1786d9e11'),
          key: 'voyager1Url',
          width: 150,
          sorter: true,
          ellipsis: true
        },
        {
          title: this.$t('i18n_b37b786351'),
          key: 'groupName',
          ellipsis: true,
          width: '100px'
        },
        {
          title: this.$t('i18n_3fea7ca76c'),
          key: 'status',
          align: 'center',
          width: '100px',
          ellipsis: true
        },
        {
          title: this.$t('i18n_64eee9aafa'),
          sorter: true,
          key: 'osSystemUptime',
          width: 150,
          ellipsis: true,
          duration2: true
        },
        {
          title: `CPU${this.$t('i18n_2f97ed65db')}`,
          sorter: true,
          align: 'center',
          key: 'osOccupyCpu',
          width: '100px',
          ellipsis: true,
          percent2Number: true
        },
        {
          title: this.$t('i18n_883848dd37'),
          sorter: true,
          align: 'center',
          key: 'osOccupyMemory',
          width: '100px',
          ellipsis: true,
          percent2Number: true
        },
        {
          title: this.$t('i18n_ed145eba38'),
          sorter: true,
          align: 'center',
          key: 'osOccupyDisk',
          width: '100px',
          ellipsis: true,
          percent2Number: true
        },
        {
          title: this.$t('i18n_2482a598a3'),
          key: 'voyager1Version',
          width: '100px',
          ellipsis: true
        },
        {
          title: this.$t('i18n_04edc35414'),
          key: 'templateNode',
          width: '90px',
          align: 'center',
          ellipsis: true,
          render: (row) => {
            return row['templateNode'] ? this.$t('i18n_0a60ac8f02') : this.$t('i18n_c9744f45e7')
          }
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
          render: (row) => parseTime(row['modifyTimeMillis']),
          sorter: true,
          width: '170px'
        },
        {
          title: this.$t('i18n_2b6bc0f293'),
          key: 'operation',
          width: '120px',
          fixed: 'right',
          align: 'center'
        }
      ],

      tableSelections: [],
      whiteConfigVisible: false,
      nodeConfigVisible: false,
      templateNodeList: [],
      confirmLoading: false
    }
  },
  computed: {
    ...mapState(useAppStore, ['getCollapsed']),
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
        selectedRowKeys: this.tableSelections
      }
    }
  },
  mounted() {
    this.loadGroupList()
    this.getMachineList()
  },
  methods: {
    parseTime,
    formatDuration,
    formatPercent2Number,
    CHANGE_PAGE,
    // PAGE_DEFAULT_SHOW_TOTAL,
    // getCachePageLimit,
    // 获取所有的分组
    loadGroupList() {
      machineListGroup().then((res) => {
        if (res.data) {
          this.groupList = res.data
        }
      })
    },
    getMachineList(pointerEvent) {
      this.loading = true
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page
      machineListData(this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data.result
          this.listQuery.total = res.data.total
        }
        this.loading = false
      })
    },
    // 分页、排序、筛选变化时触发
    changePage(pagination, filters, sorter) {
      this.listQuery = CHANGE_PAGE(this.listQuery, { pagination, sorter })
      this.getMachineList()
    },
    addMachine() {
      this.temp = {
        // 默认设置节点地址协议
        voyager1Protocol: 'Http'
      }
      this.editVisible = true
    },
    // 修改
    handleEdit(record) {
      this.temp = Object.assign({}, record)
      delete this.temp.statusMsg
      this.editVisible = true
    },
    // 提交节点数据
    handleEditOk() {
      // 检验表单
      this.$refs['editNodeForm'].validate().then(() => {
        // 提交数据
        this.confirmLoading = true
        machineEdit(this.temp)
          .then((res) => {
            if (res.code === 200) {
              // 成功
              $notification.success({
                message: res.msg
              })
              this.$refs['editNodeForm'].restoreValidation()
              this.editVisible = false
              this.loadGroupList()
              this.getMachineList()
            }
          })
          .finally(() => {
            this.confirmLoading = false
          })
      }).catch(() => {})
    },
    showMachineInfo(item) {
      this.temp = { ...item }
      this.drawerVisible = true
    },
    // 删除机器
    deleteMachineInfo(item) {
      $confirm({
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        content: this.$t('i18n_9c66f7b345'),
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
        onOk: () =>
          machineDelete({
            id: item.id
          }).then((res) => {
            if (res.code === 200) {
              $notification.success({
                message: res.msg
              })
              this.getMachineList()
            }
          })
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
    syncToWorkspaceShow(item) {
      this.syncToWorkspaceVisible = true
      this.loadWorkSpaceListAll()
      if (item) {
        this.temp = {
          ids: item.id
        }
      }
    },
    handleSyncToWorkspace() {
      if (!this.temp.workspaceId) {
        $notification.warn({
          message: this.$t('i18n_b3bda9bf9e')
        })
        return false
      }
      if (!this.temp.ids) {
        this.temp = { ...this.temp, ids: this.tableSelections.join(',') }
        this.tableSelections = []
      }
      // 同步
      this.confirmLoading = true
      machineDistribute(this.temp)
        .then((res) => {
          if (res.code == 200) {
            $notification.success({
              message: res.msg
            })

            this.syncToWorkspaceVisible = false
            return false
          }
        })
        .finally(() => {
          this.confirmLoading = false
        })
    },
    // 显示节点版本信息
    showMachineUpgrade(item) {
      this.temp = { ...item }
      this.drawerUpgradeVisible = true
    },
    // 查看机器关联的节点
    viewMachineNode(item) {
      machineListNode({
        id: item.id
      }).then((res) => {
        if (res.code === 200) {
          this.viewLinkNode = true
          this.nodeList = res.data
        }
      })
    },
    toNode(nodeId, name, wid) {
      const newpage = this.$router.resolve({
        path: '/node/list',
        query: {
          ...this.$route.query,
          nodeId: nodeId,
          pId: 'manage',
          id: 'manageList',
          wid: wid,
          searchNodeName: name
        }
      })
      window.open(newpage.href, '_blank')
    },

    syncNodeWhiteConfig() {
      if (!this.tableSelections || this.tableSelections.length <= 0) {
        $notification.warn({
          message: this.$t('i18n_d82b19148f')
        })
        return
      }
      machineListTemplateNode().then((res) => {
        //
        if (res.code === 200) {
          if (res.data && res.data.length) {
            this.whiteConfigVisible = true
            this.templateNodeList = res.data
            this.temp = {
              ...this.temp,
              templateNodeId: this.templateNodeList[0].id
            }
            this.loadWhitelistData(this.temp.templateNodeId)
          } else {
            $notification.warn({
              message: this.$t('i18n_d7ef19d05b')
            })
          }
        }
      })
    },

    // 加载节点授权分发配置
    loadWhitelistData(id) {
      getWhiteList({
        machineId: id
      }).then((res) => {
        if (res.code === 200 && res.data) {
          this.temp = Object.assign({}, this.temp, res.data)
          // { ...thie.temp,res.data };
        }
      })
    },
    onSubmitWhitelist() {
      this.confirmLoading = true
      saveWhitelist({
        ...this.temp,
        ids: this.tableSelections.join(',')
      })
        .then((res) => {
          if (res.code === 200) {
            // 成功
            $notification.success({
              message: res.msg
            })
            this.tableSelections = []
            this.whiteConfigVisible = false
          }
        })
        .finally(() => {
          this.confirmLoading = false
        })
    },
    syncNodeConfig() {
      if (!this.tableSelections || this.tableSelections.length <= 0) {
        $notification.warn({
          message: this.$t('i18n_1e07b9f9ce')
        })
        return
      }
      machineListTemplateNode().then((res) => {
        //
        if (res.code === 200) {
          if (res.data && res.data.length) {
            this.nodeConfigVisible = true
            this.templateNodeList = res.data
            this.temp = {
              ...this.temp,
              templateNodeId: this.templateNodeList[0].id
            }
            this.loadNodeConfig(this.temp.templateNodeId)
          } else {
            $notification.warn({
              message: this.$t('i18n_d7ef19d05b')
            })
          }
        }
      })
    },

    // 修改模版节点
    loadNodeConfig(id) {
      getConfigData({ machineId: id }).then((res) => {
        if (res.code === 200) {
          this.temp = { ...this.temp, content: res.data.content }
        }
      })
    },
    // submit
    onNodeSubmit(restart) {
      $confirm({
        title: this.$t('i18n_c4535759ee'),
        content: restart ? this.$t('i18n_0cf4f0ba82') : this.$t('i18n_863a95c914'),
        okText: this.$t('i18n_e83a256e4f'),
        zIndex: 1009,
        cancelText: this.$t('i18n_625fb26b4b'),
        onOk: () => {
          this.confirmLoading = true
          return saveNodeConfig({
            ...this.temp,
            restart: restart,
            ids: this.tableSelections.join(',')
          })
            .then((res) => {
              if (res.code === 200) {
                // 成功
                $notification.success({
                  message: res.msg
                })
                this.nodeConfigVisible = false
                this.tableSelections = []
              }
            })
            .finally(() => {
              this.confirmLoading = false
            })
        }
      })
    }
  }
}
</script>
<style scoped>
.item-info {
  padding: 4px 0;
}
.item-info .title {
}
.item-info .content {
}
</style>
