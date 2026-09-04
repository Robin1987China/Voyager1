<template>
  <div>
    <!-- 数据表格 -->
    <CustomTable
      is-show-tools
      default-auto-refresh
      :auto-refresh-time="5"
      table-name="assets-docker-list"
      :empty-description="$t('i18n_5fea80e369')"
      :active-page="activePage"
      size="medium"
      :data="list"
      :columns="columns"
      :pagination="pagination"
      bordered
      row-key="id"
      :row-selection="rowSelection"
      :scroll="{
        x: 'max-content'
      }"
      @change="changePage"
      @refresh="loadData"
    >
      <template #title>
        <n-space wrap class="search-box">
          <n-input
            v-model:value="listQuery['%name%']"
            :placeholder="$t('i18n_d7ec2d3fea')"
            class="search-input-item"
            @press-enter="loadData"
          />
          <n-input
            v-model:value="listQuery['%host%']"
            placeholder="host"
            class="search-input-item"
            @press-enter="loadData"
          />
          <n-input
            v-model:value="listQuery['%swarmId%']"
            :placeholder="$t('i18n_7329a2637c')"
            class="search-input-item"
            @press-enter="loadData"
          />
          <n-select
            v-model:value="listQuery.groupName"
            filterable
            clearable
            :placeholder="$t('i18n_829abe5a8d')"
            class="search-input-item"
            :options="groupList"
          />
          <n-tooltip>
            <template #trigger>
              <span class="tw">
                <n-button type="primary" :loading="loading" @click="loadData">{{ $t('i18n_e5f71fc31e') }}</n-button>
              </span>
            </template>
            $t('i18n_4838a3bd20')
          </n-tooltip>
          <n-button type="primary" @click="handleAdd">{{ $t('i18n_66ab5e9f24') }}</n-button>
          <n-button :disabled="!tableSelections.length" type="primary" @click="syncToWorkspaceShow()">
            {{ $t('i18n_82d2c66f47') }}</n-button
          >
          <n-tooltip>
            <template #trigger>
              <span class="tw">
                <n-button dashed @click="handleTryLocalDocker">
                  <QuestionCircleOutlined />{{ $t('i18n_91985e3574') }}
                </n-button>
              </span>
            </template>
            $t('i18n_bbd63a893c')
          </n-tooltip>
        </n-space>
      </template>
      <template #tableBodyCell="{ column, text, record }">
        <template v-if="column.dataIndex === 'name'">
          <n-tooltip>
            <template #trigger>
              <span class="tw">
                <n-button style="padding: 0" text size="small" @click="handleEdit(record)"> {{ text }}</n-button>
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

        <template v-else-if="column.dataIndex === 'swarmId'">
          <template v-if="text">
            <n-tooltip v-if="record.swarmControlAvailable">
              <template #trigger>
                <ClusterOutlined />
              </template>
              $t('i18n_a6269ede6c')
            </n-tooltip>
            <n-tooltip v-else>
              <template #trigger>
                <BlockOutlined />
              </template>
              $t('i18n_41e9f0c9c6')
            </n-tooltip>
            <n-popover>
              <template #trigger>
                {{ text }}
              </template>
              <template #header>{{ $t('i18n_32e05f01f4') }}</template>

              <p>{{ $t('i18n_2a24902516') }}{{ record.swarmId }}</p>
              <p>{{ $t('i18n_c5e7257212') }}{{ record.swarmNodeId }}</p>
              <p>{{ $t('i18n_ccea973fc7') }}{{ record.swarmNodeAddr }}</p>
              <p>{{ $t('i18n_39e4138e30') }}{{ parseTime(record.swarmCreatedAt) }}</p>
              <p>{{ $t('i18n_b4750210ef') }}{{ parseTime(record.swarmUpdatedAt) }}</p>
            </n-popover>
          </template>
        </template>

        <template v-else-if="column.dataIndex === 'tlsVerify'">
          <n-tooltip placement="top-start">
            <template #trigger>
              <span class="tw">
                <template v-if="record.tlsVerify">
                  <template v-if="record.certExist">
                    <n-switch
                      v-model:value="record.tlsVerify"
                      size="small"
                      :disabled="true"
                      :checked-label="$t('i18n_8493205602')"
                      :unchecked-label="$t('i18n_d58a55bcee')"
                    />
                  </template>
                  <n-tag v-else color="red"> {{ $t('i18n_88b79928e7') }} </n-tag>
                </template>
                <template v-else>
                  <n-switch
                    v-model:value="record.tlsVerify"
                    size="small"
                    :disabled="true"
                    :checked-label="$t('i18n_8493205602')"
                    :unchecked-label="$t('i18n_d58a55bcee')"
                  />
                </template>
              </span>
            </template>
            {{ record.tlsVerify ? $t('i18n_1058a0be42') + record.certInfo : $t('i18n_007f23e18f') }}
          </n-tooltip>
        </template>

        <template v-else-if="column.dataIndex === 'status'">
          <n-tooltip>
            <template #trigger>
              <span class="tw">
                <n-tag :color="statusMap[record.status].color">{{
                  statusMap[record.status].desc || $t('i18n_1622dc9b6b')
                }}</n-tag>
              </span>
            </template>
            record.failureMsg
          </n-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'operation'">
          <n-space>
            <n-button size="small" type="primary" :disabled="record.status !== 1" @click="handleConsole(record)">{{
              $t('i18n_b5c3770699')
            }}</n-button>
            <template v-if="!record.swarmId && record.status === 1">
              <n-popover>
                <template #trigger>
                  <span class="tw">
                    <n-button size="small" type="primary"><EditOutlined />{{ $t('i18n_85fe5099f6') }}</n-button>
                  </span>
                </template>
                <template #header>{{ $t('i18n_9a4b872895') }}</template>

                <p>
                  <n-button size="small" type="primary" @click="initSwarm(record)">{{
                    $t('i18n_374cd1f7b7')
                  }}</n-button>
                </p>
                <p>
                  <n-button size="small" type="primary" @click="joinSwarm(record)">{{
                    $t('i18n_55cf956586')
                  }}</n-button>
                </p>
              </n-popover>
            </template>
            <template v-else>
              <n-button
                size="small"
                :disabled="parseInt(record.status) !== 1"
                type="primary"
                @click="handleSwarmConsole(record)"
                ><SelectOutlined />{{ $t('i18n_85fe5099f6') }}</n-button
              >
            </template>
            <n-button size="small" type="primary" @click="syncToWorkspaceShow(record)">{{
              $t('i18n_e39de3376e')
            }}</n-button>
            <n-button size="small" type="primary" @click="viewWorkspaceDataHander(record)">{{
              $t('i18n_1c3cf7f5f0')
            }}</n-button>
            <n-dropdown
              :options="[
                { label: $t('i18n_95b351c862'), key: '0', props: { onClick: () => handleEdit(record) } },
                { label: $t('i18n_2f4aaddde3'), key: '1', props: { onClick: () => handleDelete(record) } },
                {
                  label: $t('i18n_e54029e15b'),
                  key: '2',
                  disabled: !record.swarmId || record.status !== 1,
                  props: { onClick: () => handleLeaveForce(record) }
                }
              ]"
            >
              <a @click="(e) => e.preventDefault()"> {{ $t('i18n_0ec9eaf9c3') }} <DownOutlined /> </a>
            </n-dropdown>
          </n-space>
        </template>
      </template>
    </CustomTable>
    <!-- 编辑区 -->
    <CustomModal
      v-if="editVisible"
      v-model:open="editVisible"
      destroy-on-close
      width="50%"
      :title="$t('i18n_657969aa0f')"
      :confirm-loading="confirmLoading"
      :mask-closable="false"
      @ok="handleEditOk"
    >
      <n-form ref="editForm" :rules="rules" :model="temp">
        <n-space direction="vertical" style="width: 100%">
          <n-alert banner>
            <template #message>
              <template v-if="temp.enableSsh">
                <ul>
                  <li>SSH {{ $t('i18n_051fa113dd') }}</li>
                  <li>
                    docker {{ $t('i18n_bb8d265c7e') }}.09
                    {{ $t('i18n_1810e84971') }}
                  </li>
                  <li>{{ $t('i18n_6aa7403b18') }}</li>
                  <li>
                    {{ $t('i18n_92c6aa6db9') }}>{{ $t('i18n_e049546ff3') }}
                    <b>ssh/monitor-script.sh</b>
                    {{ $t('i18n_f0eb685a84') }}
                  </li>
                </ul>
              </template>
              <template v-else>
                <ul>
                  <li>
                    {{ $t('i18n_c46938460b') }}<b style="color: red">{{ $t('i18n_7010264d22') }}</b>
                  </li>
                  <li>
                    {{ $t('i18n_ba1f68b5dd') }}
                    <b style="color: red">docker {{ $t('i18n_47768ed092') }}</b>
                  </li>
                  <li>
                    {{ $t('i18n_fdba50ca2d') }}<b style="color: red"> {{ $t('i18n_7e000409bb') }} </b>
                  </li>
                  <li>
                    {{ $t('i18n_5bb5b33ae4') }}<b style="color: red">{{ $t('i18n_73ed447971') }}</b
                    >（{{ $t('i18n_9c942ea972') }}
                  </li>
                  <li>
                    {{ $t('i18n_8b1512bf3a') }}<b style="color: red">{{ $t('i18n_8fd9daf8e9') }}</b>
                  </li>
                  <li>
                    {{ $t('i18n_ba8d1dca4a') }}<b style="color: red">{{ $t('i18n_0e25ab3b51') }}</b>
                  </li>
                </ul>
              </template>
            </template>
          </n-alert>
          <div></div>
        </n-space>
        <n-form-item :label="$t('i18n_a51cd0898f')" path="name">
          <n-input v-model:value="temp.name" :placeholder="$t('i18n_a51cd0898f')" />
        </n-form-item>
        <n-form-item :label="$t('i18n_829abe5a8d')" path="groupName">
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
        <n-form-item :label="$t('i18n_0c7369bbee')" path="enableSsh">
          <n-switch
            v-model:value="temp.enableSsh"
            :checked-label="$t('i18n_8493205602')"
            :unchecked-label="$t('i18n_d58a55bcee')"
          />
          <template #help>
            <div v-if="temp.enableSsh">
              {{ $t('i18n_b9c1616fd5') }}
            </div>
          </template>
        </n-form-item>
        <n-form-item v-if="temp.enableSsh" :label="$t('i18n_cbd9ffe1b8')" path="sshUseSudo">
          <n-switch
            v-model:value="temp.sshUseSudo"
            :checked-label="$t('i18n_8493205602')"
            :unchecked-label="$t('i18n_d58a55bcee')"
          />
          <template #help>{{ $t('i18n_5ab9bf3591') }}</template>
        </n-form-item>
        <n-form-item v-if="temp.enableSsh" :label="$t('i18n_a5617f0369')" path="enableSsh">
          <n-select
            v-model:value="temp.machineSshId"
            filterable
            remote
            :loading="searchSshListLoading"
            :placeholder="$t('i18n_a5617f0369')"
            class="search-input-item"
            :options="
              sshList.map((item) => ({
                label: `${item.name}(${item.host}) [${(item.dockerInfo && JSON.parse(item.dockerInfo) && JSON.parse(item.dockerInfo).version) || $t('i18n_f3365fbf4d')}]`,
                value: item.id,
                disabled: !item.dockerInfo
              }))
            "
            @search="sshListData"
          >
            <template #empty>
              <n-spin v-if="searchSshListLoading" size="small" />
              <span v-else>{{ $t('i18n_e5fae81ed4') }}</span>
            </template>
          </n-select>
          <template #help>{{ $t('i18n_09d14694e7') }}</template>
        </n-form-item>
        <template v-if="!temp.enableSsh">
          <n-form-item label="host" path="host">
            <n-input v-model:value="temp.host" :placeholder="`${$t('i18n_dc32f465da')}://127.0.0.1:2375`" />
          </n-form-item>

          <n-form-item :label="$t('i18n_2780a6a3cf')" path="tlsVerify">
            <n-switch
              v-model:value="temp.tlsVerify"
              :checked-label="$t('i18n_8493205602')"
              :unchecked-label="$t('i18n_d58a55bcee')"
            />
          </n-form-item>
          <n-form-item
            v-if="temp.tlsVerify"
            :label="$t('i18n_cbce8e96cf')"
            path="certInfo"
            :help="$t('i18n_b515d55aab')"
          >
            <n-input
              v-model:value="temp.certInfo"
              :placeholder="$t('i18n_f1d8533c7f')"
              :enter-button="$t('i18n_63b6b36c71')"
              @keyup.enter="
                () => {
                  certificateVisible = true
                }
              "
            />
          </n-form-item>
        </template>

        <n-collapse>
          <n-collapse-item key="1" :header="$t('i18n_9ab433e930')">
            <n-form-item :label="$t('i18n_56071a4fa6')" path="heartbeatTimeout">
              <n-input-number
                v-model:value="temp.heartbeatTimeout"
                style="width: 100%"
                :placeholder="$t('i18n_b513f53eb4')"
              />
            </n-form-item>
            <n-form-item :label="$t('i18n_e4bea943de')" path="registryUrl">
              <n-input v-model:value="temp.registryUrl" :placeholder="$t('i18n_e4bea943de')" />
            </n-form-item>
            <n-form-item :label="$t('i18n_b4c83b0b56')" path="registryUsername">
              <n-input v-model:value="temp.registryUsername" :placeholder="$t('i18n_b4c83b0b56')" />
            </n-form-item>
            <n-form-item :label="$t('i18n_791870de48')" path="registryPassword">
              <n-input v-model:value="temp.registryPassword" type="password" :placeholder="$t('i18n_791870de48')" />
            </n-form-item>
            <n-form-item :label="$t('i18n_feda0df7ef')" path="registryEmail">
              <n-input v-model:value="temp.registryEmail" :placeholder="$t('i18n_feda0df7ef')" />
            </n-form-item>
          </n-collapse-item>
        </n-collapse>
      </n-form>
    </CustomModal>
    <!-- 创建集群 -->
    <CustomModal
      v-if="initSwarmVisible"
      v-model:open="initSwarmVisible"
      destroy-on-close
      :title="$t('i18n_f2d05944ad')"
      :confirm-loading="confirmLoading"
      :mask-closable="false"
      @ok="handleSwarm"
    >
      <n-form ref="initForm" :rules="rules" :model="temp">
        <n-alert :title="$t('i18n_947d983961')" type="warning">
          <template #description> {{ $t('i18n_9d5b1303e0') }} </template>
        </n-alert>
      </n-form>
    </CustomModal>
    <!-- 加入集群 -->
    <CustomModal
      v-if="joinSwarmVisible"
      v-model:open="joinSwarmVisible"
      destroy-on-close
      :title="$t('i18n_0006600738')"
      :confirm-loading="confirmLoading"
      :mask-closable="false"
      @ok="handleSwarmJoin"
    >
      <n-form ref="joinForm" :rules="rules" :model="temp">
        <n-form-item :label="$t('i18n_e4d0ebcd58')" path="managerId">
          <n-select
            v-model:value="temp.managerId"
            filterable
            clearable
            :placeholder="$t('i18n_96c28c4f17')"
            :options="swarmList.map((item) => ({ label: item.name, value: item.id }))"
            @update:value="
              (v) => {
                tempList = swarmList.filter((item) => {
                  return item.id === v
                })
                if (tempList.length) {
                  temp = { ...temp, remoteAddr: tempList[0].swarmNodeAddr }
                } else {
                  temp = { ...temp, remoteAddr: '' }
                }
              }
            "
          />
        </n-form-item>

        <n-form-item v-if="temp.remoteAddr" :label="$t('i18n_2e740698cf')" path="remoteAddr"
          ><n-input v-model:value="temp.remoteAddr" :placeholder="$t('i18n_77017a3140')" />
        </n-form-item>

        <n-form-item :label="$t('i18n_464f3d4ea3')" path="role">
          <n-radio-group v-model:value="temp.role" name="role">
            <n-radio value="worker"> {{ $t('i18n_41e9f0c9c6') }}</n-radio>
            <n-radio value="manager"> {{ $t('i18n_a6269ede6c') }} </n-radio>
          </n-radio-group>
        </n-form-item>
      </n-form>
    </CustomModal>
    <!-- 控制台 -->

    <console
      v-if="consoleVisible"
      :show="consoleVisible"
      :machine-docker-id="temp.id"
      url-prefix="/system/assets/docker"
      @close="onClose"
    ></console>
    <!-- </n-drawer> -->
    <!-- 集群控制台 -->

    <swarm-console
      v-if="swarmConsoleVisible"
      :id="temp.id"
      :show="swarmConsoleVisible"
      :init-menu="temp.menuKey"
      url-prefix="/system/assets"
      @close="onSwarmClose"
    ></swarm-console>

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
        <n-form-item> </n-form-item>
        <n-form-item :label="$t('i18n_0128cdaaa3')" path="type">
          <n-radio-group v-model:value="temp.type">
            <n-radio value="docker"> docker </n-radio>
            <n-radio value="swarm" :disabled="temp.swarmId === true ? false : true">
              {{ $t('i18n_85fe5099f6') }}
            </n-radio>
          </n-radio-group>
        </n-form-item>
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
    <!-- 查看 docker 关联工作空间的信息 -->
    <CustomModal
      v-if="viewWorkspaceDocker"
      v-model:open="viewWorkspaceDocker"
      destroy-on-close
      width="50%"
      :title="$t('i18n_9086111cff')"
      :footer="null"
      :mask-closable="false"
    >
      <n-space direction="vertical" style="width: 100%">
        <n-alert
          v-if="
            workspaceDockerData && (workspaceDockerData.dockerList?.length || workspaceDockerData.swarmList?.length)
          "
          :title="$t('i18n_1b03b0c1ff')"
          type="info"
          show-icon
        />
        <n-tabs>
          <n-tab-pane name="1" tab="docker">
            <n-list bordered>
<n-list-item v-for="(item, index) in workspaceDockerData && workspaceDockerData.dockerList" :key="index" style="display: block">
                  <n-grid>
                    <n-grid-item :span="10">Docker {{ $t('i18n_5b47861521') }}{{ item.name }}</n-grid-item>
                    <n-grid-item :span="10"
                      >{{ $t('i18n_2358e1ef49') }}{{ item.workspace && item.workspace.name }}</n-grid-item
                    >
                    <n-grid-item :span="4"> </n-grid-item>
                  </n-grid>
                </n-list-item>
</n-list>
          </n-tab-pane>
          <n-tab-pane name="2" :tab="$t('i18n_85fe5099f6')">
            <n-list bordered>
<n-list-item v-for="(item, index) in workspaceDockerData && workspaceDockerData.swarmList" :key="index" style="display: block">
                  <n-grid>
                    <n-grid-item :span="10">{{ $t('i18n_f668c8c881') }}{{ item.name }}</n-grid-item>
                    <n-grid-item :span="10"
                      >{{ $t('i18n_2358e1ef49') }}{{ item.workspace && item.workspace.name }}</n-grid-item
                    >
                    <n-grid-item :span="4"> </n-grid-item>
                  </n-grid>
                </n-list-item>
</n-list>
          </n-tab-pane>
        </n-tabs>
      </n-space>
    </CustomModal>
    <!-- 选择证书文件 -->
    <CustomDrawer
      v-if="certificateVisible"
      destroy-on-close
      :title="`${$t('i18n_38a12e7196')}`"
      placement="right"
      :open="certificateVisible"
      width="85vw"
      :footer-style="{ textAlign: 'right' }"
      @close="
        () => {
          certificateVisible = false
        }
      "
    >
      <certificate
        v-if="certificateVisible"
        ref="certificate"
        :show-all="true"
        @confirm="
          (certInfo) => {
            temp = { ...temp, certInfo: certInfo }
            certificateVisible = false
          }
        "
        @cancel="
          () => {
            certificateVisible = false
          }
        "
      ></certificate>
      <template #footer>
        <n-space>
          <n-button
            @click="
              () => {
                certificateVisible = false
              }
            "
          >
            {{ $t('i18n_625fb26b4b') }}
          </n-button>
          <n-button
            type="primary"
            @click="
              () => {
                $refs['certificate'].handerConfirm()
              }
            "
          >
            {{ $t('i18n_e83a256e4f') }}
          </n-button>
        </n-space>
      </template>
    </CustomDrawer>
  </div>
</template>
<script>
import {
  BlockOutlined,
  ClusterOutlined,
  DownOutlined,
  EditOutlined,
  QuestionCircleOutlined,
  SelectOutlined
} from '@ant-design/icons-vue'

import { h } from 'vue'
import { NIcon } from 'naive-ui'

import {
  dockerList,
  editDocker,
  tryLocalDocker,
  deleteDcoker,
  initDockerSwarm,
  joinDockerSwarm,
  dockerSwarmListAll,
  dcokerSwarmLeaveForce,
  machineDockerDistribute,
  dockerListWorkspace,
  dockerListGroup,
  statusMap
} from '@/api/system/assets-docker'
import { machineSshSearchData } from '@/api/system/assets-ssh'
import { CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY, parseTime } from '@/utils/const'
import { getWorkSpaceListAll } from '@/api/workspace'
import Console from '@/pages/docker/console'
import SwarmConsole from '@/pages/docker/swarm/console.vue'

import certificate from '@/pages/certificate/list.vue'
import CustomSelect from '@/components/customSelect'

export default {
  components: {
    Console,
    SwarmConsole,
    certificate,
    CustomSelect
  },
  props: {},
  data() {
    return {
      loading: false,
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      statusMap,
      list: [],
      groupList: [],
      temp: {},
      editVisible: false,
      templateVisible: false,
      consoleVisible: false,
      swarmConsoleVisible: false,
      initSwarmVisible: false,
      joinSwarmVisible: false,
      swarmList: [],
      sshList: [],
      columns: [
        {
          title: this.$t('i18n_d7ec2d3fea'),
          key: 'name',
          ellipsis: true,

          width: 150
        },
        {
          title: 'host',
          key: 'host',
          ellipsis: true,

          width: 150
        },
        {
          title: `docker${this.$t('i18n_fe2df04a16')}`,
          key: 'dockerVersion',
          ellipsis: true,
          width: '100px'
        },

        {
          title: this.$t('i18n_3fea7ca76c'),
          key: 'status',
          ellipsis: true,
          align: 'center',
          width: '100px'
        },
        {
          title: 'TLS',
          key: 'tlsVerify',
          width: '80px',
          align: 'center',
          ellipsis: true
        },
        {
          title: this.$t('i18n_b37b786351'),
          key: 'groupName',
          ellipsis: true,
          width: '100px'
        },
        {
          title: this.$t('i18n_85fe5099f6'),
          key: 'swarmId',
          ellipsis: true
        },
        // { title: "apiVersion", key: "apiVersion", width: 100, ellipsis: true, },
        {
          title: this.$t('i18n_3bcc1c7a20'),
          key: 'modifyUser',
          width: 120,
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
          sorter: true,
          ellipsis: true,
          render: (row) => parseTime(row['modifyTimeMillis']),
          width: '170px'
        },
        {
          title: this.$t('i18n_2b6bc0f293'),
          key: 'operation',

          fixed: 'right',
          align: 'center',
          width: '320px'
        }
      ],

      rules: {
        // id: [{ required: true, message: "Please input ID", trigger: "blur" }],
        name: [{ required: true, message: this.$t('i18n_f63870fdb0'), trigger: 'blur' }],
        // host: [{ required: true, message: "请填写容器地址", trigger: "blur" }],

        managerId: [
          {
            required: true,
            message: this.$t('i18n_f97a4d2591'),
            trigger: 'blur'
          }
        ],

        role: [{ required: true, message: this.$t('i18n_9d7d471b77'), trigger: 'blur' }],
        remoteAddr: [
          { required: true, message: this.$t('i18n_5d07edd921'), trigger: 'blur' },
          {
            pattern:
              /^(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])\.(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])\.(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])\.(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])$/,
            message: this.$t('i18n_a8754e3e90')
          }
        ]
      },
      syncToWorkspaceVisible: false,
      workspaceList: [],
      viewWorkspaceDocker: false,
      workspaceDockerData: {
        dockerList: [],
        swarmList: []
      },
      tableSelections: [],
      certificateVisible: false,
      confirmLoading: false,
      searchSshListLoading: false
    }
  },
  computed: {
    pagination() {
      return COMPUTED_PAGINATION(this.listQuery)
    },
    rowSelection() {
      return {
        onChange: (selectedRowKeys) => {
          this.tableSelections = selectedRowKeys
        },
        selectedRowKeys: this.tableSelections
      }
    },
    activePage() {
      return this.$attrs.routerUrl === this.$route.path
    }
  },
  mounted() {
    this.loadData()
    this.loadGroupList()
  },
  methods: {
    //
    parseTime,
    // 获取所有的分组
    loadGroupList() {
      dockerListGroup().then((res) => {
        if (res.data) {
          this.groupList = res.data
        }
      })
    },
    sshListData(name) {
      this.searchSshListLoading = true
      machineSshSearchData({
        limit: 10,
        mustId: this.temp?.machineSshId,
        name: name
      })
        .then((res) => {
          if (res.code === 200) {
            this.sshList = res.data || []
          }
        })
        .finally(() => {
          this.searchSshListLoading = false
        })
    },
    // 加载数据
    loadData(pointerEvent) {
      this.loading = true
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page

      dockerList(this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data.result
          this.listQuery.total = res.data.total
          //
          const dockerId = this.$route.query.dockerId
          const type = this.$route.query.type
          this.list.map((item) => {
            if (dockerId === item.id && type === 'docker') {
              this.handleConsole(item)
            } else if (dockerId === item.id && type === 'swarm') {
              this.handleSwarmConsole(item)
            }
          })
        }
        this.loading = false
      })
    },
    // 新增
    handleAdd() {
      this.temp = {}
      this.editVisible = true
      this.sshListData()
      this.$refs['editForm']?.restoreValidation()
    },
    // 控制台
    handleConsole(record) {
      this.temp = { ...record }
      this.consoleVisible = true

      let dockerId = this.$route.query.dockerId
      if (dockerId !== record.id) {
        this.$router.push({
          query: { ...this.$route.query, dockerId: record.id, type: 'docker' }
        })
      }
    },
    // 集群控制台
    handleSwarmConsole(record) {
      this.temp = { ...record }
      this.swarmConsoleVisible = true
      let dockerId = this.$route.query.dockerId
      if (dockerId !== record.id) {
        this.$router.push({
          query: { ...this.$route.query, dockerId: record.id, type: 'swarm' }
        })
      }
    },
    // 关闭抽屉层
    onClose() {
      this.consoleVisible = false
      const query = Object.assign({}, this.$route.query)
      delete query.dockerId
      delete query.type
      this.$router.replace({
        query: query
      })
    },
    onSwarmClose() {
      this.swarmConsoleVisible = false
      const query = Object.assign({}, this.$route.query)
      delete query.dockerId
      delete query.type
      this.$router.replace({
        query: query
      })
    },
    // 修改
    handleEdit(record) {
      this.temp = { ...record }
      this.editVisible = true
      this.sshListData()
      // this.temp = { ...this.temp };

      this.$refs['editForm']?.restoreValidation()
    },

    // 提交  数据
    handleEditOk() {
      // 检验表单

      this.$refs['editForm'].validate().then(() => {
        const temp = Object.assign({}, this.temp)
        if (temp.enableSsh) {
          if (!temp.machineSshId) {
            $$message.warning(this.$t('i18n_e257dd2607'))
            return false
          }
        } else {
          if (!temp.host) {
            $$message.warning(this.$t('i18n_90154854b6'))
            return false
          }
        }
        this.confirmLoading = true
        editDocker(temp)
          .then((res) => {
            if (res.code === 200) {
              // 成功
              $notification.success({
                message: res.msg
              })
              this.editVisible = false
              this.loadData()
              this.loadGroupList()
            }
          })
          .finally(() => {
            this.confirmLoading = false
          })
      }).catch(() => {})
    },
    // 删除
    handleDelete(record) {
      $confirm({
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        content: this.$t('i18n_3ae4ddf245'),
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
        onOk: () => {
          return deleteDcoker({
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
    // 强制解绑
    handleLeaveForce(record) {
      const html = `
      <h1 style='color:red;'>${this.$t('i18n_b87c9acca3')}</h1>
      <h3 style='color:red;'>${this.$t('i18n_c163613a0d')}</h3>
      <ul style='color:red;'>
        <li>${this.$t('i18n_eb969648aa')}</li>
        <li>${this.$t('i18n_eb969648aa')}</li>
        <li style='font-weight: bold;'>${this.$t('i18n_5a8727305e')}</li>
        <li>${this.$t('i18n_04412d2a22')}</li>
      </ul>
      `
      $confirm({
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        content: h('div', null, [h('p', { innerHTML: html }, null)]),
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
        onOk: () => {
          return dcokerSwarmLeaveForce({
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
    },

    // 创建集群
    initSwarm(record) {
      this.temp = {
        id: record.id
      }
      this.initSwarmVisible = true
      this.$refs['initForm']?.restoreValidation()
    },
    handleSwarm() {
      this.$refs['initForm'].validate().then(() => {
        this.confirmLoading = true
        initDockerSwarm(this.temp)
          .then((res) => {
            if (res.code === 200) {
              // 成功
              $notification.success({
                message: res.msg
              })
              this.initSwarmVisible = false
              this.loadData()
            }
          })
          .finally(() => {
            this.confirmLoading = false
          })
      }).catch(() => {})
    },
    // 加入集群
    joinSwarm(record) {
      dockerSwarmListAll().then((res) => {
        this.swarmList = res.data
        this.temp = {
          id: record.id
        }
        this.joinSwarmVisible = true
        this.$refs['joinForm']?.restoreValidation()
      })
    },
    // 处理加入集群
    handleSwarmJoin() {
      this.$refs['joinForm'].validate().then(() => {
        this.confirmLoading = true

        joinDockerSwarm(this.temp)
          .then((res) => {
            if (res.code === 200) {
              // 成功
              $notification.success({
                message: res.msg
              })
              this.joinSwarmVisible = false
              this.loadData()
            }
          })
          .finally(() => {
            this.confirmLoading = false
          })
      }).catch(() => {})
    },
    //
    handleTryLocalDocker() {
      tryLocalDocker().then((res) => {
        if (res.code === 200) {
          // 成功
          $notification.success({
            message: res.msg
          })
          this.loadData()
        }
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
          ids: item.id,
          swarmId: item.swarmId ? true : false
        }
      } else {
        this.temp = {
          swarmId: true
        }
      }
    },
    handleSyncToWorkspace() {
      if (!this.temp.type) {
        $notification.warn({
          message: this.$t('i18n_dabdc368f5')
        })
        return false
      }
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
      machineDockerDistribute(this.temp)
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
    // 查看工作空间的 docker 信息
    viewWorkspaceDataHander(item) {
      this.workspaceDockerData = {}
      dockerListWorkspace({
        id: item.id
      }).then((res) => {
        if (res.code === 200) {
          this.viewWorkspaceDocker = true
          this.workspaceDockerData = res.data
        }
      })
    }
  }
}
</script>
