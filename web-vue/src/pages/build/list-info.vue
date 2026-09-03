<template>
  <div>
    <!-- 表格 -->
    <!-- <n-card :body-style="{ padding: '10px' }"> -->

    <!-- <template v-if="layoutType === 'card'">
      <template v-if="list && list.length">
        <n-grid :x-gap="[16, 16]">
          <n-grid-item v-for="item in list" :key="item.id" :span="6"> </n-grid-item>
        </n-grid>
      </template>
      <template v-else>
        <n-empty  description="没有任何构建" />
      </template>
    </template> -->
    <!-- <template v-else-if="layoutType === 'table'"> -->
    <CustomTable
      is-show-tools
      default-auto-refresh
      :auto-refresh-time="5"
      table-name="buildList"
      :empty-description="$t('i18n_1c2e9d0c76')"
      :active-page="activePage"
      :layout="layout"
      size="medium"
      :columns="columns"
      :data="list"
      bordered
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
        <n-space wrap class="search-box">
          <n-input
            v-model:value="listQuery['%name%']"
            clearable
            class="search-input-item"
            :placeholder="$t('i18n_50a299c847')"
            @press-enter="loadData"
          />
          <n-select
            v-model:value="listQuery.status"
            filterable
            clearable
            :placeholder="$t('i18n_3fea7ca76c')"
            class="search-input-item"
            :options="Object.entries(statusMap).map(([key, val]) => ({ label: val, value: key }))"
          />
          <n-select
            v-model:value="listQuery.releaseMethod"
            filterable
            clearable
            :placeholder="$t('i18n_f98994f7ec')"
            class="search-input-item"
            :options="Object.entries(releaseMethodMap).map(([key, val]) => ({ label: val, value: key }))"
          />
          <n-select
            v-model:value="listQuery.group"
            filterable
            clearable
            :placeholder="$t('i18n_829abe5a8d')"
            class="search-input-item"
            :options="groupList"
          />
          <n-input
            v-model:value="listQuery['%resultDirFile%']"
            clearable
            class="search-input-item"
            :placeholder="$t('i18n_c972010694')"
            @press-enter="loadData"
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
          <template v-if="tableSelections && tableSelections.length">
            <n-dropdown
              :options="[
                { label: $t('i18n_67e3d3e09c'), key: '1', props: { onClick: () => batchBuild() } },
                { label: $t('i18n_32112950da'), key: '2', props: { onClick: () => batchCancel() } },
                { label: $t('i18n_7fb62b3011'), key: '3', props: { onClick: () => handleBatchDelete() } }
              ]"
            >
              <n-button type="primary"> {{ $t('i18n_7f7c624a84') }}<DownOutlined /> </n-button>
            </n-dropdown>
          </template>
          <n-tooltip v-else>
            <template #trigger>
              <span class="tw">
                <n-button :disabled="true" type="primary"> {{ $t('i18n_7f7c624a84') }} <DownOutlined /> </n-button>
              </span>
            </template>
            $t('i18n_98357846a2')
          </n-tooltip>

          <!-- <n-button v-if="!layout" type="primary" @click="changeLayout">
            <template #icon>
              <LayoutOutlined v-if="layoutType === 'card'" />
              <TableOutlined v-else />
            </template>
            {{ layoutType === 'card' ? '卡片' : '表格' }}
          </n-button>

          <n-statistic
            v-if="!choose"
            format=" s 秒"
            title="刷新倒计时"
            :value="countdownTime"
            @finish="silenceLoadData"
          />
          -->
        </n-space>
      </template>
      <template #cardBodyCell="{ item }">
        <n-card :head-style="{ padding: '0 6px' }" :body-style="{ padding: '10px' }">
          <template #title>
            <n-grid :x-gap="[4, 0]">
              <n-grid-item :span="17" style="overflow: hidden; text-overflow: ellipsis; white-space: nowrap">
                <n-button text style="padding: 0" size="small" @click="handleDetails(item)">
                  <span> {{ item.name }}</span>
                </n-button>
              </n-grid-item>
              <n-grid-item :span="7" style="text-align: right" class="text-overflow-hidden">
                <n-tooltip>
                  <template #trigger>
                    <span class="tw">
                      <n-tag :color="statusColor[item.status]" style="margin-right: 0">
                        {{ statusMap[item.status] || $t('i18n_903b25f64e') }}</n-tag
                      >
                    </span>
                  </template>
                  `${$t('i18n_e703c7367c')}${statusMap[item.status]} ${ item.statusMsg ? $t('i18n_8d13037eb7') +
                  item.statusMsg : '' }`
                </n-tooltip>
              </n-grid-item>
            </n-grid>
          </template>

          <n-tooltip>
            <template #trigger>
              <span class="tw">
                <n-grid class="item-info">
                  <n-grid-item :span="6" class="title text-overflow-hidden">{{ $t('i18n_88ab27cfd0') }}</n-grid-item>
                  <n-grid-item :span="18" class="content text-overflow-hidden">
                    {{ item.branchName }} {{ item.branchTagName }}</n-grid-item
                  >
                </n-grid>
              </span>
            </template>

            <div v-if="item.branchTagName">
              <div>{{ $t('i18n_8086beecb3') }}{{ item.branchTagName }}</div>
              <div>{{ $t('i18n_ca774ec5b4') }}{{ item.repositoryLastCommitId }}</div>
            </div>
            <div v-else>
              <div>{{ $t('i18n_f240f9d69c') }}{{ item.branchName }}</div>
              <div>{{ $t('i18n_ca774ec5b4') }}{{ item.repositoryLastCommitId }}</div>
            </div>
          </n-tooltip>
          <n-tooltip>
            <template #trigger>
              <span class="tw">
                <n-grid class="item-info">
                  <n-grid-item :span="6" class="title text-overflow-hidden">{{ $t('i18n_cc637e17a0') }}</n-grid-item>
                  <n-grid-item :span="18" class="content text-overflow-hidden">
                    {{ item.resultDirFile }}
                  </n-grid-item>
                </n-grid>
              </span>
            </template>
            item.resultDirFile
          </n-tooltip>

          <n-grid class="item-info">
            <n-grid-item :span="6" class="title text-overflow-hidden">{{ $t('i18n_b5d0091ae3') }}:</n-grid-item>
            <n-grid-item :span="18" class="content text-overflow-hidden">
              <n-tag v-if="item.buildId <= 0">-</n-tag>
              <n-tag v-else color="#108ee9" @click="handleBuildLog(item)">#{{ item.buildId }}</n-tag>
            </n-grid-item>
          </n-grid>

          <n-grid class="item-info">
            <n-grid-item :span="6" class="title text-overflow-hidden">{{ $t('i18n_c530a094f9') }}</n-grid-item>
            <n-grid-item :span="18" class="content text-overflow-hidden">
              <template v-if="item.buildMode === 1">
                <CloudOutlined />
                {{ $t('i18n_685e5de706') }}
              </template>
              <template v-else>
                <CodeOutlined />
                {{ $t('i18n_69c3b873c1') }}
              </template>
            </n-grid-item>
          </n-grid>
          <n-grid class="item-info">
            <n-grid-item :span="6" class="title text-overflow-hidden">{{ $t('i18n_65894da683') }}</n-grid-item>
            <n-grid-item :span="18" class="content text-overflow-hidden">
              {{ releaseMethodMap[item.releaseMethod] }}
            </n-grid-item>
          </n-grid>

          <n-grid type="flex" align="middle" justify="center" style="margin-top: 10px">
            <n-button-group>
              <n-button
                v-if="item.status === 1 || item.status === 4 || item.status === 9"
                size="small"
                type="primary"
                danger
                @click="handleStopBuild(item)"
                >{{ $t('i18n_095e938e2a') }}
              </n-button>
              <n-dropdown
                v-else
                :options="[
                  {
                    label: $t('i18n_16b5e7b472'),
                    key: '0',
                    props: {
                      onClick: () => reqStartBuild({ id: item.id, buildEnvParameter: item.buildEnvParameter }, true)
                    }
                  },
                  {
                    label: $t('i18n_f1fdaffdf0'),
                    key: '1',
                    props: {
                      onClick: () => reqStartBuild({ id: item.id, buildEnvParameter: item.buildEnvParameter }, false)
                    }
                  }
                ]"
              >
                <n-button size="small" type="primary" @click="handleConfirmStartBuild(item)">
                  {{ $t('i18n_fcba60e773') }}
                  <DownOutlined />
                </n-button>
              </n-dropdown>
              <n-dropdown
                :options="[
                  { label: $t('i18n_17a74824de'), key: '0', props: { onClick: () => handleEdit(item, 0) } },
                  { label: $t('i18n_6ea1fe6baa'), key: '1', props: { onClick: () => handleEdit(item, 1) } },
                  { label: $t('i18n_a2ae15f8a7'), key: '2', props: { onClick: () => handleEdit(item, 2) } },
                  { label: $t('i18n_3c91490844'), key: '3', props: { onClick: () => handleEdit(item, 3) } },
                  { label: $t('i18n_9ab433e930'), key: '4', props: { onClick: () => handleEdit(item, 4) } }
                ]"
              >
                <n-button size="small" type="primary" @click="handleEdit(item)">{{ $t('i18n_95b351c862') }}</n-button>
              </n-dropdown>
              <n-button size="small" @click="handleDelete(item)">{{ $t('i18n_2f4aaddde3') }}</n-button>
              <n-tooltip placement="leftBottom">
                <template #trigger>
                  <span class="tw">
                    <n-button size="small" :disabled="!item.sourceDirExist" @click="handleClear(item)"
                      >{{ $t('i18n_c37ac7f024') }}
                    </n-button>
                  </span>
                </template>
                $t('i18n_19675b9d36')
              </n-tooltip>
            </n-button-group>
          </n-grid>
        </n-card>
      </template>
      <template #tableBodyCell="{ column, text, record, index }">
        <template v-if="column.dataIndex === 'name'">
          <n-tooltip placement="topLeft" @click="handleDetails(record)">
            <template #trigger>
              <span class="tw">
                <n-button text style="padding: 0" size="small"> <FullscreenOutlined />{{ text }}</n-button>
              </span>
            </template>
            `${$t('i18n_d7ec2d3fea')}${text} ${$t('i18n_84632d372f')}`
          </n-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'branchName'">
          <n-tooltip placement="top-start">
            <template #trigger>
              <span class="tw">
                <span class="tw">
                  <span v-if="record.branchTagName"><TagOutlined />{{ record.branchTagName }}</span>
                  <span v-else>{{ text }}</span>
                </span>
              </span>
            </template>

            <div v-if="record.branchTagName">
              <div>{{ $t('i18n_8086beecb3') }}{{ record.branchTagName }}</div>
              <div>{{ $t('i18n_ca774ec5b4') }}{{ record.repositoryLastCommitId }}</div>
            </div>
            <div v-else>
              <div>{{ $t('i18n_f240f9d69c') }}{{ text }}</div>
              <div>{{ $t('i18n_ca774ec5b4') }}{{ record.repositoryLastCommitId }}</div>
            </div>
          </n-tooltip>
        </template>

        <template v-else-if="column.dataIndex === 'buildMode'">
          <n-tooltip placement="topLeft">
            <template #trigger>
              <span class="tw">
                <CloudOutlined v-if="text === 1" />
                <CodeOutlined v-else />
              </span>
            </template>
            text === 1 ? $t('i18n_685e5de706') : $t('i18n_69c3b873c1')
          </n-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'releaseMethod'">
          <n-tooltip>
            <template #trigger>
              <span class="tw">
                <span class="tw">
                  <span>{{ releaseMethodMap[text] }}</span>
                </span>
              </span>
            </template>

            <ul>
              <li>{{ $t('i18n_65894da683') }}{{ releaseMethodMap[text] }}</li>
              <li>{{ $t('i18n_113576ce91') }}{{ record.resultDirFile }}</li>
              <li v-if="record.buildMode !== 1">{{ $t('i18n_1160ab56fd') }}{{ record.script }}</li>
            </ul>
          </n-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'status'">
          <n-tooltip>
            <template #trigger>
              <span class="tw">
                <n-tag
                  :color="statusColor[record.status]"
                  :title="record.statusMsg || statusMap[text] || $t('i18n_1622dc9b6b')"
                  >{{ statusMap[text] || $t('i18n_1622dc9b6b') }}</n-tag
                >
              </span>
            </template>
            record.statusMsg || statusMap[text] || $t('i18n_1622dc9b6b')
          </n-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'buildId'">
          <n-tooltip placement="topLeft">
            <template #trigger>
              <span class="tw">
                <span class="tw">
                  <span v-if="record.buildId <= 0"></span>
                  <n-tag v-else color="#108ee9" @click="handleBuildLog(record)">#{{ text }}</n-tag>
                </span>
              </span>
            </template>
            text + ` ( ${$t('i18n_aac62bc255')} ) `
          </n-tooltip>
        </template>
        <template v-else-if="column.tooltip">
          <n-tooltip placement="topLeft">
            <template #trigger>
              <span class="tw">
                <span class="tw">
                  <span>{{ text || '' }}</span>
                </span>
              </span>
            </template>
            text
          </n-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'operation'">
          <n-space>
            <n-button size="small" @click="openPipeline(record)">Pipeline</n-button>
            <n-button
              v-if="record.status === 1 || record.status === 4 || record.status === 9"
              size="small"
              type="primary"
              danger
              @click="handleStopBuild(record)"
              >{{ $t('i18n_095e938e2a') }}
            </n-button>
            <n-dropdown
              v-else
              :options="[
                {
                  label: $t('i18n_16b5e7b472'),
                  key: '0',
                  props: {
                    onClick: () => reqStartBuild({ id: record.id, buildEnvParameter: record.buildEnvParameter }, true)
                  }
                },
                {
                  label: $t('i18n_f1fdaffdf0'),
                  key: '1',
                  props: {
                    onClick: () => reqStartBuild({ id: record.id, buildEnvParameter: record.buildEnvParameter }, false)
                  }
                }
              ]"
            >
              <n-button size="small" type="primary" @click="handleConfirmStartBuild(record)"
                >{{ $t('i18n_fcba60e773') }}<DownOutlined
              /></n-button>
            </n-dropdown>
            <n-dropdown
              :options="[
                { label: $t('i18n_17a74824de'), key: '0', props: { onClick: () => handleEdit(record, 0) } },
                { label: $t('i18n_6ea1fe6baa'), key: '1', props: { onClick: () => handleEdit(record, 1) } },
                { label: $t('i18n_a2ae15f8a7'), key: '2', props: { onClick: () => handleEdit(record, 2) } },
                { label: $t('i18n_3c91490844'), key: '3', props: { onClick: () => handleEdit(record, 3) } },
                { label: $t('i18n_9ab433e930'), key: '4', props: { onClick: () => handleEdit(record, 4) } }
              ]"
            >
              <n-button size="small" type="primary" @click="handleEdit(record, 1)">{{
                $t('i18n_95b351c862')
              }}</n-button>
            </n-dropdown>
            <n-dropdown
              :options="[
                { label: $t('i18n_79d3abe929'), key: '0', props: { onClick: () => copyItem(record) } },
                {
                  label: $t('i18n_635391aa5d'),
                  key: '1',
                  disabled: !record.resultHasFile,
                  props: { onClick: () => handleDownloadFile(record) }
                },
                { label: $t('i18n_2f4aaddde3'), key: '2', props: { onClick: () => handleDelete(record) } },
                {
                  label: $t('i18n_c37ac7f024'),
                  key: '3',
                  disabled: !record.sourceDirExist,
                  props: { onClick: () => handleClear(record) }
                },
                { type: 'divider', key: 'd1' },
                {
                  label: $t('i18n_3d43ff1199'),
                  key: '4',
                  disabled: (listQuery.page - 1) * listQuery.limit + (index + 1) <= 1,
                  props: { onClick: () => sortItemHander(record, index, 'top') }
                },
                {
                  label: $t('i18n_315eacd193'),
                  key: '5',
                  disabled: (listQuery.page - 1) * listQuery.limit + (index + 1) <= 1,
                  props: { onClick: () => sortItemHander(record, index, 'up') }
                },
                {
                  label: $t('i18n_17acd250da'),
                  key: '6',
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
    </CustomTable>
    <!-- </template> -->
    <!-- </n-card> -->

    <!-- 编辑区 -->
    <build-item
      v-if="editBuildVisible != 0"
      :id="temp.id"
      :visible-type="editBuildVisible"
      :edit-steps="editSteps"
      :data="temp"
      @close="
        () => {
          editBuildVisible = 0
        }
      "
      @build="
        (build, buildId, buildEnvParameter) => {
          editBuildVisible = 0
          loadData()
          loadGroupList()
          if (build) {
            reqStartBuild({ id: buildId, buildEnvParameter: buildEnvParameter || temp.buildEnvParameter }, true)
          }
        }
      "
    ></build-item>
    <!-- 构建日志 -->
    <build-log
      v-if="buildLogVisible > 0"
      :temp="temp"
      :show="buildLogVisible != 0"
      @close="
        () => {
          buildLogVisible = 0
        }
      "
    />
    <!-- 构建确认 -->
    <CustomModal
      v-if="buildConfirmVisible"
      v-model:open="buildConfirmVisible"
      destroy-on-close
      :confirm-loading="confirmLoading"
      width="40vw"
      :title="$t('i18n_0a1d18283e')"
      :mask-closable="false"
      @ok="handleStartBuild"
    >
      <n-form :model="temp">
        <n-form-item :label="$t('i18n_d7ec2d3fea')" path="name">
          <n-input v-model:value="temp.name" read-only disabled />
        </n-form-item>
        <n-form-item :label="$t('i18n_bfc04cfda7')" path="branchName">
          <custom-select
            v-model:value="temp.branchName"
            :data="branchList"
            :disabled="temp.branchTagName ? true : false"
            :can-reload="true"
            :input-placeholder="$t('i18n_c618659cea')"
            :select-placeholder="$t('i18n_121e76bb63')"
            @on-refresh-select="loadBranchListById(temp.repositoryId)"
          >
            <template #inputTips>
              <div>
                {{ $t('i18n_89f5ca6928') }}(AntPathMatcher)
                <ul>
                  <li>? {{ $t('i18n_9973159a4d') }}</li>
                  <li>* {{ $t('i18n_32f882ae24') }}</li>
                  <li>** {{ $t('i18n_45b88fc569') }}</li>
                </ul>
              </div>
            </template>
          </custom-select>
        </n-form-item>
        <n-form-item
          v-if="(branchTagList && branchTagList.length) || (temp.branchTagName && temp.branchTagName.length)"
          :label="$t('i18n_977bfe8508')"
          path="branchTagName"
        >
          <custom-select
            v-model:value="temp.branchTagName"
            :data="branchTagList"
            :can-reload="true"
            :input-placeholder="$t('i18n_30e6f71a18')"
            :select-placeholder="$t('i18n_2d58b0e650')"
            @on-refresh-select="loadBranchListById(temp.repositoryId)"
          >
            <template #inputTips>
              <div>
                {{ $t('i18n_89f5ca6928') }}(AntPathMatcher)
                <ul>
                  <li>? {{ $t('i18n_9973159a4d') }}</li>
                  <li>* {{ $t('i18n_32f882ae24') }}</li>
                  <li>** {{ $t('i18n_45b88fc569') }}</li>
                </ul>
              </div>
            </template>
          </custom-select>
        </n-form-item>
        <n-form-item path="resultDirFile" :label="$t('i18n_c972010694')">
          <n-input v-model:value="temp.resultDirFile" :placeholder="$t('i18n_2bef5b58ab')" />
        </n-form-item>
        <n-form-item path="checkRepositoryDiff" :label="$t('i18n_0b23d2f584')" help="">
          <n-space>
            <n-switch
              v-model:value="temp.checkRepositoryDiff"
              :checked-label="$t('i18n_0a60ac8f02')"
              :unchecked-label="$t('i18n_c9744f45e7')"
            />
            <span>
              <n-tooltip>
                <template #trigger>
                  <QuestionCircleOutlined />
                </template>
                {{ $t('i18n_4cbc5505c7') }}
              </n-tooltip>
              {{ $t('i18n_1d263b7efb') }}
            </span>
          </n-space>
        </n-form-item>

        <n-form-item
          v-if="temp.releaseMethod === 1 || temp.releaseMethod === 2"
          path="projectSecondaryDirectory"
          :label="$t('i18n_871cc8602a')"
        >
          <n-input v-model:value="temp.projectSecondaryDirectory" :placeholder="$t('i18n_9c99e8bec9')" />
        </n-form-item>
        <n-form-item :label="$t('i18n_3867e350eb')" path="buildEnvParameter" :help="$t('i18n_220650a1f5')">
          <n-input
            v-model:value="temp.buildEnvParameter"
            type="textarea"
            :placeholder="$t('i18n_b3913b9bb7')"
            :auto-size="{ minRows: 3, maxRows: 5 }"
          />
        </n-form-item>
        <n-form-item :label="$t('i18n_d1498d9dbf')" path="buildRemark" :help="$t('i18n_111e786daa')">
          <n-input
            v-model:value="temp.buildRemark"
            type="textarea"
            :max-length="240"
            :placeholder="$t('i18n_7777a83497')"
            :auto-size="{ minRows: 2, maxRows: 5 }"
          />
        </n-form-item>
        <n-form-item
          v-if="dispatchProjectList && dispatchProjectList.length"
          path="selectProject"
          :label="$t('i18n_c4e0c6b6fe')"
          :help="$t('i18n_25be899f66')"
        >
          <n-select
            v-model:value="temp.dispatchSelectProjectArray"
            multiple
            :placeholder="$t('i18n_b29fd18c93')"
            :options="
              dispatchProjectList.map((item) => ({
                label: `${item.nodeName}-${item.cacheProjectName || item.projectId}`,
                value: `${item.projectId}@${item.nodeId}`
              }))
            "
          />
        </n-form-item>
      </n-form>
    </CustomModal>
  </div>
</template>
<script>
import {
  CloudOutlined,
  CodeOutlined,
  DownOutlined,
  FullscreenOutlined,
  LayoutOutlined,
  QuestionCircleOutlined,
  TableOutlined,
  TagOutlined
} from '@ant-design/icons-vue'

import { h } from 'vue'
import { NIcon } from 'naive-ui'

import BuildLog from './log'
import BuildItem from './item'
import CustomSelect from '@/components/customSelect'
import {
  clearBuid,
  deleteBuild,
  getBuildGroupAll,
  getBuildList,
  releaseMethodMap,
  downloadBuildFileByBuild,
  startBuild,
  statusMap,
  statusColor,
  stopBuild,
  sortItem,
  getBranchList,
  deleteatchBuild
} from '@/api/build-info'
import { getDispatchProject } from '@/api/dispatch'

import {
  CHANGE_PAGE,
  COMPUTED_PAGINATION,
  PAGE_DEFAULT_LIST_QUERY,
  parseTime
  // PAGE_DEFAULT_SHOW_TOTAL,
  // getCachePageLimit
} from '@/utils/const'
import { NEmpty as Empty } from 'naive-ui'

export default {
  components: {
    BuildLog,
    BuildItem,
    CustomSelect
  },
  props: {
    repositoryId: {
      type: String,
      default: ''
    },
    fullContent: {
      type: Boolean,
      default: true
    },
    choose: {
      // "radio"
      type: String,
      default: ''
    },
    layout: {
      type: String,
      default: ''
    }
  },
  emits: ['cancel', 'confirm'],
  data() {
    return {
      Empty,
      sizeOptions: ['8', '12', '16', '20', '24'],
      releaseMethodMap,
      loading: false,
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      // 动态列表参数
      groupList: [],
      list: [],
      statusMap,
      statusColor,
      branchTagList: [],
      branchList: [],
      temp: {},
      // 页面控制变量
      editBuildVisible: 0,
      editSteps: null,
      buildLogVisible: 0,
      buildConfirmVisible: false,
      columns: [
        {
          title: this.$t('i18n_d7ec2d3fea'),
          key: 'name',
          sorter: true,
          width: 200,
          ellipsis: true
        },
        {
          title: this.$t('i18n_829abe5a8d'),
          key: 'group',
          width: 100,
          ellipsis: true
        },
        {
          title: this.$t('i18n_f4bbbaf882'),
          key: 'branchName',
          ellipsis: true,
          width: 100
        },

        {
          title: this.$t('i18n_7220e4d5f9'),
          key: 'buildMode',
          align: 'center',
          width: '80px',
          sorter: true,
          ellipsis: true
        },
        {
          title: this.$t('i18n_3fea7ca76c'),
          key: 'status',
          align: 'center',
          width: '100px',
          ellipsis: true
        },
        {
          title: this.$t('i18n_b5d0091ae3'),
          key: 'buildId',
          width: '90px',
          ellipsis: true,
          align: 'center'
        },

        {
          title: this.$t('i18n_f98994f7ec'),
          key: 'releaseMethod',
          width: '100px',
          ellipsis: true
        },
        {
          title: this.$t('i18n_7dfcab648d'),
          key: 'resultDirFile',
          width: 100,
          ellipsis: true
        },
        {
          title: this.$t('i18n_db9296212a'),
          key: 'autoBuildCron',
          width: 100,
          ellipsis: true
        },
        {
          title: this.$t('i18n_9baca0054e'),
          key: 'modifyUser',
          width: '130px',
          ellipsis: true,
          sorter: true
        },

        {
          title: this.$t('i18n_eca37cb072'),
          key: 'createTimeMillis',
          sorter: true,
          ellipsis: true,
          render: (row) => parseTime(row['createTimeMillis']),
          width: '160px'
        },
        {
          title: this.$t('i18n_1303e638b5'),
          key: 'modifyTimeMillis',
          sorter: true,
          render: (row) => parseTime(row['modifyTimeMillis']),
          width: '160px'
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
          width: '200px',

          align: 'center',
          fixed: 'right'
        }
      ],

      // countdownTime: Date.now(),
      // refreshInterval: 5,
      tableSelections: [],
      dispatchProjectList: [],
      // layoutType: null,
      confirmLoading: false
    }
  },
  computed: {
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
        selectedRowKeys: this.tableSelections,
        type: this.choose || 'checkbox'
      }
    }
  },
  watch: {},
  created() {
    // if (this.layout) {
    // this.layoutType = this.layout
    this.loadData()
    // } else {
    // this.changeLayout()
    // }
    this.loadGroupList()
    //
    // this.countdownTime = Date.now() + this.refreshInterval * 1000
  },
  methods: {
    CHANGE_PAGE,

    // PAGE_DEFAULT_SHOW_TOTAL,
    // getCachePageLimit,
    // 分组数据
    loadGroupList() {
      getBuildGroupAll().then((res) => {
        if (res.data) {
          this.groupList = res.data
        }
      })
    },
    // 加载数据
    loadData(pointerEvent) {
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page
      this.listQuery.repositoryId = this.repositoryId
      this.loading = true
      getBuildList(this.listQuery)
        .then((res) => {
          if (res.code === 200) {
            this.list = res.data.result
            this.listQuery.total = res.data.total
            // 重新计算倒计时
            // this.countdownTime = Date.now() + this.refreshInterval * 1000
          }
        })
        .finally(() => {
          this.loading = false
        })
    },
    // silenceLoadData() {
    //   if (this.$attrs.routerUrl !== this.$route.path) {
    //     // 重新计算倒计时
    //     this.countdownTime = Date.now() + this.refreshInterval * 1000
    //     return
    //   }
    //   this.loading = true
    //   getBuildList(this.listQuery, false)
    //     .then((res) => {
    //       if (res.code === 200) {
    //         this.list = res.data.result
    //         this.listQuery.total = res.data.total
    //         // 重新计算倒计时
    //         this.countdownTime = Date.now() + this.refreshInterval * 1000
    //       }
    //     })
    //     .finally(() => {
    //       this.loading = false
    //     })
    // },

    // 新增
    handleAdd() {
      this.temp = {}
      this.editBuildVisible = 2
      this.editSteps = 0
    },
    // 复制
    copyItem(record) {
      const temp = Object.assign({}, record)
      delete temp.id
      delete temp.triggerToken
      temp.name = temp.name + this.$t('i18n_0428b36ab1')
      this.temp = temp
      this.editBuildVisible = 2
      this.editSteps = 1
      // this.handleEdit(temp, 1)
    },
    handleEdit(record, steps) {
      this.temp = { id: record.id, buildEnvParameter: record.buildEnvParameter }
      this.editBuildVisible = 2

      this.editSteps = steps
    },
    handleDetails(record) {
      this.editBuildVisible = 1
      this.editSteps = 2
      this.temp = { id: record.id, buildEnvParameter: record.buildEnvParameter }
    },
    loadBranchListById(id) {
      this.branchList = []
      this.branchTagList = []
      const params = {
        repositoryId: id
      }
      getBranchList(params).then((res) => {
        if (res.code === 200) {
          this.branchList = res.data?.branch || []
          this.branchTagList = res.data?.tags || []
        }
      })
    },
    // 删除
    handleDelete(record) {
      $confirm({
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        content: this.$t('i18n_48281fd3f0'),
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
        onOk: () => {
          // 删除
          return deleteBuild(record.id).then((res) => {
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
    // 批量删除
    handleBatchDelete() {
      if (!this.tableSelections || this.tableSelections.length <= 0) {
        $notification.warning({
          message: this.$t('i18n_5d817c403e')
        })
        return
      }
      $confirm({
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        content: this.$t('i18n_79076b6882'),
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
        onOk: () => {
          // 删除
          return deleteatchBuild({ ids: this.tableSelections.join(',') }).then((res) => {
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
    // 清除构建
    handleClear(record) {
      $confirm({
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        content: this.$t('i18n_e15f22df2d'),
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
        onOk: () => {
          return clearBuid(record.id).then((res) => {
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
    // 开始构建
    handleConfirmStartBuild(record) {
      this.dispatchProjectList = []
      // 判断构建方式
      if (record.releaseMethod === 1) {
        // 节点分发
        getDispatchProject(record.releaseMethodDataId, true).then((res) => {
          if (res.code === 200) {
            this.dispatchProjectList = res.data?.projectList || []

            this.showBuildConfirm(record)
          }
        })
      } else {
        this.showBuildConfirm(record)
      }
      // console.log(record);
    },
    showBuildConfirm(record) {
      this.temp = Object.assign({}, record)
      this.buildConfirmVisible = true
      this.branchList = []
      this.branchTagList = []
      //
      try {
        const extraData = JSON.parse(record.extraData) || {}
        this.temp = {
          ...this.temp,
          checkRepositoryDiff: extraData.checkRepositoryDiff,
          projectSecondaryDirectory: extraData.projectSecondaryDirectory
        }
      } catch (e) {
        //
      }
    },
    handleStartBuild() {
      this.confirmLoading = true
      this.reqStartBuild(
        {
          id: this.temp.id,
          buildRemark: this.temp.buildRemark,
          resultDirFile: this.temp.resultDirFile,
          branchTagName: this.temp.branchTagName,
          branchName: this.temp.branchName,
          checkRepositoryDiff: this.temp.checkRepositoryDiff,
          projectSecondaryDirectory: this.temp.projectSecondaryDirectory,
          buildEnvParameter: this.temp.buildEnvParameter,
          dispatchSelectProject:
            (this.temp.dispatchSelectProjectArray && this.temp.dispatchSelectProjectArray.join(',')) || ''
        },
        true
      )
        .then(() => {
          this.buildConfirmVisible = false
        })
        .finally(() => {
          this.confirmLoading = false
        })
    },
    reqStartBuild(data, openLog) {
      return new Promise((resolve) => {
        startBuild(data).then((res) => {
          if (res.code === 200) {
            $notification.success({
              message: res.msg
            })
            this.loadData()
            if (openLog) {
              // 自动打开构建日志
              this.handleBuildLog({
                id: data.id,
                buildId: res.data
              })
            }
            resolve()
          }
        })
      })
    },
    // 停止构建
    handleStopBuild(record) {
      $confirm({
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        // TODO 后续抽优化
        content: this.$t('i18n_25f6a95de3') + record.name + this.$t('i18n_c16ab7c424'),
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
        onOk: () => {
          this.temp = Object.assign({}, record)
          return stopBuild(this.temp.id).then((res) => {
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
    // 查看构建日志
    handleBuildLog(record) {
      this.temp = {
        id: record.id,
        buildId: record.buildId
      }
      this.buildLogVisible = new Date() * Math.random()
    },
    // 关闭日志对话框
    closeBuildLogModel() {
      this.loadData()
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
        msg += this.$t('i18n_461e675921')
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
    // 下载构建产物
    handleDownloadFile(record) {
      window.open(downloadBuildFileByBuild(record.id, record.buildId), '_blank')
    },
    // 批量构建
    batchBuild() {
      if (!this.tableSelections || this.tableSelections.length <= 0) {
        $notification.warning({
          message: this.$t('i18n_5d817c403e')
        })
        return
      }
      $confirm({
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        content: this.$t('i18n_9341881037'),
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
        onOk: () => {
          this.tableSelections.forEach((item) => {
            startBuild({
              id: item,
              buildEnvParameter: this.list.find((item2) => item2.id === item)?.buildEnvParameter
            }).then((res) => {
              if (res.code === 200) {
                //
              }
            })
          })
          this.tableSelections = []
          this.loadData()
        }
      })
    },
    // 批量取消构建
    batchCancel() {
      if (!this.tableSelections || this.tableSelections.length <= 0) {
        $notification.warning({
          message: this.$t('i18n_5d817c403e')
        })
        return
      }
      $confirm({
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        content: this.$t('i18n_2d3fd578ce'),
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
        onOk: () => {
          this.tableSelections.forEach((item) => {
            stopBuild(item).then((res) => {
              if (res.code === 200) {
                //
              }
            })
          })
          this.tableSelections = []
          this.loadData()
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
    // 选择确认
    handerConfirm() {
      if (!this.tableSelections.length) {
        $notification.warning({
          message: this.$t('i18n_2b4cf3d74e')
        })
        return
      }
      const selectData = this.list.filter((item) => {
        return this.tableSelections.indexOf(item.id) > -1
      })
      if (!selectData.length) {
        $notification.warning({
          message: this.$t('i18n_2b4cf3d74e')
        })
        return
      }
      this.$emit('confirm', selectData)
    }
  }
}
</script>
<style scoped>
.item-info {
  padding: 4px 0;
}

</style>
