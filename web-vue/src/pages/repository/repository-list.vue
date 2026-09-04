<template>
  <div>
    <!-- 表格 -->
    <CustomTable
      is-show-tools
      default-auto-refresh
      :auto-refresh-time="30"
      :active-page="activePage"
      table-name="repository-list"
      :empty-description="$t('i18n_e07cbb381c')"
      size="medium"
      :columns="columns"
      :data="list"
      bordered
      row-key="id"
      :row-selection="choose ? rowSelection : null"
      :pagination="pagination"
      :scroll="{
        x: 'max-content'
      }"
      @change="
        (pagination, filters, sorter) => {
          listQuery = CHANGE_PAGE(listQuery, { pagination, sorter })
          loadData()
        }
      "
      @refresh="loadData"
    >
      
      <template #toolPrefix>
        <n-button type="primary" size="small" @click="handlerExportData"
          ><DownloadOutlined />{{ $t('i18n_55405ea6ff') }}</n-button
        >
        <n-dropdown
          :options="[{ label: $t('i18n_2e505d23f7'), key: '0', props: { onClick: () => handlerImportTemplate() } }]"
        >
          <n-upload name="file" accept=".csv" :show-file-list="false" :multiple="false" :custom-request="beforeUpload">
            <n-button type="primary" size="small"
              ><UploadOutlined /> {{ $t('i18n_8d9a071ee2') }} <DownOutlined />
            </n-button>
          </n-upload>
        </n-dropdown>
      </template>
      <template #tableBodyCell="{ column, text, record, index }">
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

        <template v-else-if="column.dataIndex === 'repoType'">
          <span v-if="text === 0">GIT</span>
          <span v-else-if="text === 1">SVN</span>
          <span v-else>{{ $t('i18n_1622dc9b6b') }}</span>
        </template>
        <template v-else-if="column.dataIndex === 'protocol'">
          <span v-if="text === 0">HTTP(S)</span>
          <span v-else-if="text === 1">SSH</span>
          <!-- if no protocol value, get a default value from gitUrl -->
          <span v-else>{{ record.gitUrl.indexOf('http') > -1 ? 'HTTP(S)' : 'SSH' }}</span>
        </template>
        <template v-else-if="column.dataIndex === 'workspaceId'">
          <n-tag v-if="text === 'GLOBAL'">{{ $t('i18n_2be75b1044') }}</n-tag>
          <n-tag v-else>{{ $t('i18n_98d69f8b62') }}</n-tag>
        </template>
        <template v-else-if="column.dataIndex === 'operation'">
          <n-space>
            <n-button type="primary" size="small" @click="handleEdit(record)">{{ $t('i18n_95b351c862') }}</n-button>
            <n-button v-if="global" type="primary" size="small" @click="viewBuild(record)">{{
              $t('i18n_1c3cf7f5f0')
            }}</n-button>
            <n-button type="primary" danger size="small" @click="handleDelete(record)">{{
              $t('i18n_2f4aaddde3')
            }}</n-button>

            <n-dropdown
              :options="[
                {
                  label: $t('i18n_3d43ff1199'),
                  key: '0',
                  disabled: (listQuery.page - 1) * listQuery.limit + (index + 1) <= 1,
                  props: { onClick: () => sortItemHander(record, index, 'top') }
                },
                {
                  label: $t('i18n_315eacd193'),
                  key: '1',
                  disabled: (listQuery.page - 1) * listQuery.limit + (index + 1) <= 1,
                  props: { onClick: () => sortItemHander(record, index, 'up') }
                },
                {
                  label: $t('i18n_17acd250da'),
                  key: '2',
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
    <!-- 编辑区 -->
    <CustomModal
      v-if="editVisible"
      v-model:open="editVisible"
      destroy-on-close
      :confirm-loading="confirmLoading"
      :title="$t('i18n_ed39deafd8')"
      :mask-closable="false"
      width="60%"
      @ok="handleEditOk"
    >
      <n-form ref="editForm" :rules="rules" :model="temp">
        <n-form-item :label="$t('i18n_f967131d9d')" path="name">
          <n-input v-model:value="temp.name" :max-length="50" :placeholder="$t('i18n_f967131d9d')" />
        </n-form-item>
        <n-form-item :label="$t('i18n_829abe5a8d')" path="group">
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
        <n-form-item :label="$t('i18n_e4bea943de')" path="gitUrl">
          <n-input-group compact>
            <n-form-item>
              <n-select
                v-model:value="temp.repoType"
                style="width: 20%"
                name="repoType"
                :placeholder="$t('i18n_4ce606413e')"
                :options="[
                  { label: 'GIT', value: 0 },
                  { label: 'SVN', value: 1 }
                ]"
              />
            </n-form-item>
            <n-input
              v-model:value="temp.gitUrl"
              style="width: 80%"
              :max-length="250"
              :placeholder="$t('i18n_e4bea943de')"
            />
          </n-input-group>
        </n-form-item>
        <n-form-item :label="$t('i18n_faa1ad5e5c')" path="protocol">
          <n-radio-group v-model:value="temp.protocol" name="protocol">
            <n-radio :value="0">HTTP(S)</n-radio>
            <n-radio :value="1">SSH</n-radio>
          </n-radio-group>
        </n-form-item>
        <!-- HTTP(S) protocol use password -->
        <template v-if="temp.protocol === 0">
          <n-form-item path="userName">
            <template #label>
              <n-tooltip>
                <template #trigger>
                  <span class="tw">
                    {{ $t('i18n_7035c62fb0') }}

                    <QuestionCircleOutlined v-if="!temp.id" />
                  </span>
                </template>

                {{ $t('i18n_f0a1428f65') }}<b>$ref.wEnv.xxxx</b> xxxx {{ $t('i18n_c1b72e7ded') }}
              </n-tooltip>
            </template>

            <custom-input
              :input="temp.userName"
              :env-list="envVarList"
              type="text"
              :placeholder="`${$t('i18n_fc4e2c6151')}`"
              @change="
                (v) => {
                  temp = { ...temp, userName: v }
                }
              "
            >
            </custom-input>
          </n-form-item>
          <n-form-item path="password">
            <template #label>
              <n-tooltip>
                <template #trigger>
                  <span class="tw">
                    {{ $t('i18n_a810520460') }}

                    <QuestionCircleOutlined v-if="!temp.id" />
                  </span>
                </template>

                {{ $t('i18n_63dd96a28a') }}<b>$ref.wEnv.xxxx</b> xxxx {{ $t('i18n_c1b72e7ded') }}
              </n-tooltip>
            </template>

            <custom-input
              :input="temp.password"
              :env-list="envVarList"
              :placeholder="`${!temp.id ? $t('i18n_2646b813e8') : $t('i18n_b90a30dd20')}`"
              @change="
                (v) => {
                  temp = { ...temp, password: v }
                }
              "
            >
            </custom-input>
            <template #help>
              <n-tooltip v-if="temp.id">
                <template #trigger>
                  <span class="tw">
                    <n-button style="margin: 5px" size="small" type="primary" danger @click="restHideField(temp)">{{
                      $t('i18n_4403fca0c0')
                    }}</n-button>
                  </span>
                </template>
                $t('i18n_b408105d69')
              </n-tooltip>
            </template>
          </n-form-item>
        </template>
        <n-form-item v-if="temp.repoType === 1 && temp.protocol === 1" :label="$t('i18n_7035c62fb0')" path="userName">
          <n-input v-model:value="temp.userName" :placeholder="$t('i18n_f04a289502')">
            <template #prefix>
              <UserOutlined />
            </template>
            <template #suffix>
              <n-tooltip v-if="temp.id">
                <template #trigger>
                  <span class="tw">
                    <n-button size="small" type="primary" danger @click="restHideField(temp)">{{
                      $t('i18n_4403fca0c0')
                    }}</n-button>
                  </span>
                </template>
                $t('i18n_b408105d69')
              </n-tooltip>
            </template>
          </n-input>
        </n-form-item>
        <!-- SSH protocol use rsa private key -->
        <template v-if="temp.protocol === 1">
          <n-form-item path="password">
            <template #label>
              <n-tooltip>
                <template #trigger>
                  <span class="tw">
                    {{ $t('i18n_a810520460') }}

                    <QuestionCircleOutlined v-if="!temp.id" />
                  </span>
                </template>

                {{ $t('i18n_63dd96a28a') }}<b>$ref.wEnv.xxxx</b> xxxx {{ $t('i18n_c1b72e7ded') }}
              </n-tooltip>
            </template>
            <custom-input
              :input="temp.password"
              :env-list="envVarList"
              :placeholder="`${$t('i18n_45028ad61d')}`"
              @change="
                (v) => {
                  temp = { ...temp, password: v }
                }
              "
            >
            </custom-input>
          </n-form-item>
          <n-form-item :label="$t('i18n_d0eddb45e2')" path="rsaPrv">
            <n-tooltip placement="top-start">
              <template #trigger>
                <n-input
                  v-model:value="temp.rsaPrv"
                  type="textarea"
                  :auto-size="{ minRows: 3, maxRows: 3 }"
                  :placeholder="$t('i18n_d7ee59f327')"
                ></n-input>
              </template>

              <div>
                <p style="color: #faa">
                  {{ $t('i18n_43c61e76e7') }} "{{ $t('i18n_3bc5e602b2') }}" {{ $t('i18n_9e560a4162') }} <br />{{
                    $t('i18n_8c66392870')
                  }}
                  "{{ $t('i18n_3bc5e602b2') }}" {{ $t('i18n_d0a864909b') }}<br />
                </p>
                <p>{{ $t('i18n_8fb7785809') }}</p>
                <p>{{ $t('i18n_0af04cdc22') }}</p>
                <p>
                  1. {{ $t('i18n_f5d0b69533') }}: <br />-----BEGIN RSA PRIVATE KEY-----
                  <br />
                  ..... <br />
                  -----END RSA PRIVATE KEY-----
                </p>
                <p>
                  2. {{ $t('i18n_becc848a54') }}: {{ $t('i18n_4c9bb42608') }}) {{ $t('i18n_bcc4f9e5ca') }}:
                  <br />file:/Users/Hotstrip/.ssh/id_rsa
                </p>
              </div>
            </n-tooltip>
          </n-form-item>
          <!-- 公钥暂时没用到 -->
          <n-form-item v-if="false" :label="$t('i18n_b939d47e23')" path="rsaPub">
            <n-input
              v-model:value="temp.rsaPub"
              type="textarea"
              :auto-size="{ minRows: 3, maxRows: 3 }"
              :placeholder="$t('i18n_db686f0328')"
            ></n-input>
          </n-form-item>
        </template>
        <n-form-item v-if="workspaceId !== 'GLOBAL'" :label="$t('i18n_fffd3ce745')" path="global">
          <n-radio-group v-model:value="temp.global">
            <n-radio :value="true"> {{ $t('i18n_2be75b1044') }}</n-radio>
            <n-radio :value="false"> {{ $t('i18n_691b11e443') }}</n-radio>
          </n-radio-group>
        </n-form-item>

        <n-form-item :label="$t('i18n_67425c29a5')" path="timeout">
          <n-input-number
            v-model:value="temp.timeout"
            :min="0"
            :placeholder="$t('i18n_ea9f824647')"
            style="width: 100%"
          />
        </n-form-item>
      </n-form>
    </CustomModal>
    <CustomModal
      v-if="giteeImportVisible"
      v-model:open="giteeImportVisible"
      destroy-on-close
      :title="$t('i18n_c8633b4b77')"
      width="80%"
      :footer="null"
      :mask-closable="false"
    >
      <n-form ref="giteeImportForm" :rules="giteeImportFormRules" :model="giteeImportForm">
        <n-form-item path="token" :label="$t('i18n_8ba971a184')" :help="$t('i18n_e30a93415b')">
          <n-form-item>
            <n-tooltip>
              <template #trigger>
                <span class="tw">
                  <n-input-group compact>
                    <n-select
                      v-model:value="giteeImportForm.type"
                      style="width: 10%"
                      :options="Object.keys(providerData)"
                      @update:value="importChange"
                    />

                    <n-input
                      v-model:value="giteeImportForm.token"
                      style="width: 90%; margin-top: 1px"
                      enter-button
                      :loading="importLoading"
                      :placeholder="importTypePlaceholder[giteeImportForm.type]"
                      @keyup.enter="handleGiteeImportFormOk"
                    />
                  </n-input-group>
                </span>
              </template>
              `${giteeImportForm.type} ${$t('i18n_32d0576d85')}${importTypePlaceholder[giteeImportForm.type]}`
            </n-tooltip>
          </n-form-item>
        </n-form-item>
        <n-form-item path="address" :label="$t('i18n_7650487a87')">
          <n-input v-model:value="giteeImportForm.address" :placeholder="$t('i18n_9412eb8f99')" />
        </n-form-item>
        <n-form-item
          v-if="providerData[giteeImportForm.type]?.query"
          path="condition"
          :label="$t('i18n_e5f71fc31e')"
          :help="$t('i18n_bf0e1e0c16', { slot1: $t('i18n_e5f71fc31e') })"
        >
          <n-input v-model:value="giteeImportForm.condition" :placeholder="$t('i18n_e72f2b8806')" />
        </n-form-item>
      </n-form>
            <n-card size="small" :body-style="{ padding: '12px' }" style="margin-bottom: 12px">

        <n-space wrap class="search-box">
          <n-input
            v-model:value="listQuery['%name%']"
            class="search-input-item"
            :placeholder="$t('i18n_f967131d9d')"
            @press-enter="loadData"
          />
          <n-input
            v-model:value="listQuery['%gitUrl%']"
            class="search-input-item"
            :placeholder="$t('i18n_e4bea943de')"
            @press-enter="loadData"
          />
          <n-select
            v-model:value="listQuery.repoType"
            clearable
            :placeholder="$t('i18n_4ce606413e')"
            class="search-input-item"
            :options="[
              { label: 'GIT', value: '0' },
              { label: 'SVN', value: '1' }
            ]"
          />
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
                <n-button type="primary" :loading="loading" @click="loadData">{{ $t('i18n_e5f71fc31e') }}</n-button>
              </span>
            </template>
            $t('i18n_4838a3bd20')
          </n-tooltip>
          <n-button type="primary" @click="handleAdd">{{ $t('i18n_66ab5e9f24') }}</n-button>
          <n-tooltip>
            <template #trigger>
              <span class="tw">
                <n-button type="primary" @click="handleAddGitee"
                  ><QuestionCircleOutlined />{{ $t('i18n_e354969500') }}</n-button
                >
              </span>
            </template>
            {{ $t('i18n_77c262950c') }}
          </n-tooltip>
        </n-space>
      
      </n-card>
<n-data-table
        :loading="importLoading"
        size="medium"
        :columns="reposColumns"
        :data="repos"
        bordered
        :row-key="(row) => row.full_name"
        :pagination="reposPagination"
        @change="reposChange"
      >
        <template #bodyCell="{ column, text, record }">
          <template v-if="column.key === 'private'">
            <n-switch size="small" :disabled="true" :value="record.private" />
          </template>
          <template v-else-if="column.key === 'name'">
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
          <template v-else-if="column.key === 'full_name'">
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
          <template v-else-if="column.key === 'url'">
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
          <template v-else-if="column.key === 'description'">
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

          <template v-else-if="column.key === 'operation'">
            <n-button type="primary" size="small" :disabled="record.exists" @click="handleGiteeRepoAdd(record)">{{
              record.exists ? $t('i18n_cb951984f2') : $t('i18n_66ab5e9f24')
            }}</n-button>
          </template>
        </template>
      </n-data-table>
    </CustomModal>
    <!-- 选择仓库确认区域 -->
    <!-- <div style="padding-top: 50px" v-if="this.choose">
      <div
        :style="{
          position: 'absolute',
          right: 0,
          bottom: 0,
          width: '100%',
          borderTop: '1px solid #e9e9e9',
          padding: '10px 16px',
          background: '#fff',
          textAlign: 'right',
          zIndex: 1
        }"
      >
        <n-space>
          <n-button
            @click="
              () => {
                this.$emit('cancel')
              }
            "
          >
            取消
          </n-button>
          <n-button type="primary" @click="handerConfirm"> 确定 </n-button>
        </n-space>
      </div> -->
    <!-- </div> -->
    <!-- 关联构建 -->
    <CustomModal
      v-if="viewBuildVisible"
      v-model:open="viewBuildVisible"
      destroy-on-close
      width="80vw"
      :title="$t('i18n_1c13276448')"
      :mask-closable="false"
      :footer="null"
    >
      <buildList-component v-if="viewBuildVisible" :repository-id="temp.id" :full-content="false" />
      <n-spin v-else>loading....</n-spin>
    </CustomModal>
  </div>
</template>
<script>
import {
  DownOutlined,
  DownloadOutlined,
  QuestionCircleOutlined,
  UploadOutlined,
  UserOutlined
} from '@ant-design/icons-vue'

import { h } from 'vue'
import { NIcon } from 'naive-ui'

import CustomInput from '@/components/customInput'
import {
  providerInfo,
  authorizeRepos,
  deleteRepository,
  editRepository,
  getRepositoryList,
  restHideField,
  sortItem,
  exportData,
  importTemplate,
  importData,
  listRepositoryGroup
} from '@/api/repository'
import { CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY, parseTime } from '@/utils/const'
import { getWorkspaceEnvAll } from '@/api/workspace'
import CustomSelect from '@/components/customSelect'
export default {
  components: {
    CustomInput,
    CustomSelect,
    DownOutlined,
    DownloadOutlined,
    QuestionCircleOutlined,
    UploadOutlined,
    UserOutlined,
    buildListComponent: defineAsyncComponent(() => import('@/pages/build/list-info'))
  },
  props: {
    choose: {
      type: Boolean,
      default: false
    },
    workspaceId: {
      type: String,
      default: ''
    },
    global: {
      type: Boolean,
      default: false
    },
    chooseVal: {
      type: String,
      default: ''
    }
  },
  emits: ['cancel', 'confirm'],
  data() {
    return {
      loading: false,
      PAGE_DEFAULT_SIZW_OPTIONS: ['15', '20', '25', '30', '35', '40', '50'],
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      list: [],
      groupList: [],
      providerData: {
        gitee: {
          baseUrl: 'https://gitee.com',
          name: 'gitee',
          query: true
        }
      },
      total: 0,
      temp: {},
      isSystem: false,
      editVisible: false,
      giteeImportVisible: false,
      repos: [],
      username: null,

      columns: [
        {
          title: this.$t('i18n_f967131d9d'),
          key: 'name',
          width: 200,
          sorter: true,
          ellipsis: true
        },
        {
          title: this.$t('i18n_b37b786351'),
          key: 'group',
          ellipsis: true,
          width: '100px'
        },
        {
          title: this.$t('i18n_e4bea943de'),
          key: 'gitUrl',
          width: 300,
          sorter: true,
          ellipsis: true
        },
        {
          title: this.$t('i18n_4ce606413e'),
          key: 'repoType',
          width: 100,
          sorter: true,
          ellipsis: true
        },
        {
          title: this.$t('i18n_faa1ad5e5c'),
          key: 'protocol',
          width: 100,
          sorter: true,
          ellipsis: true
        },
        {
          title: this.$t('i18n_fffd3ce745'),
          key: 'workspaceId',
          ellipsis: true,

          width: '90px'
        },
        {
          title: this.$t('i18n_95a43eaa59'),
          key: 'createUser',
          ellipsis: true,
          width: '120px'
        },
        {
          title: this.$t('i18n_9baca0054e'),
          key: 'modifyUser',
          ellipsis: true,
          width: '120px'
        },
        {
          title: this.$t('i18n_eca37cb072'),
          key: 'createTimeMillis',
          sorter: true,
          render: (row) => parseTime(row['createTimeMillis']),
          width: '170px'
        },
        {
          title: this.$t('i18n_1303e638b5'),
          key: 'modifyTimeMillis',
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
          fixed: 'right',
          align: 'center',
          width: this.global ? '240px' : '180px'
        }
      ],

      reposColumns: [
        {
          title: this.$t('i18n_f967131d9d'),
          key: 'name',
          ellipsis: true
        },
        {
          title: this.$t('i18n_42b6bd1b2f'),
          key: 'full_name',
          ellipsis: true
        },
        {
          title: 'GitUrl',
          key: 'url',
          ellipsis: true
        },

        {
          title: this.$t('i18n_3bdd08adab'),
          key: 'description',

          ellipsis: true
        },
        {
          title: this.$t('i18n_3dc5185d81'),
          key: 'private',
          width: 80,
          ellipsis: true
        },
        {
          title: this.$t('i18n_2b6bc0f293'),
          key: 'operation',
          width: 100,

          align: 'left'
        }
      ],

      giteeImportForm: Object.assign({}, PAGE_DEFAULT_LIST_QUERY, {
        limit: 15,
        type: 'gitee',
        address: 'https://gitee.com'
      }),
      giteeImportFormRules: {
        token: [{ required: true, message: this.$t('i18n_76530bff27'), trigger: 'blur' }]
        // address: [{ required: true, message: "请填写平台地址", trigger: "blur" }],
      },
      rules: {
        name: [{ required: true, message: this.$t('i18n_9f0de3800b'), trigger: 'blur' }],
        gitUrl: [{ required: true, message: this.$t('i18n_0cf81d77bb'), trigger: 'blur' }]
      },
      importTypePlaceholder: {
        gitee: this.$t('i18n_233fb56ab2'),
        github: this.$t('i18n_4b1835640f'),
        gitlab_v3: this.$t('i18n_5bd1d267a9'),
        gitlab: this.$t('i18n_5bd1d267a9'),
        gitea: this.$t('i18n_cd1aedc667'),
        gogs: this.$t('i18n_cd1aedc667'),
        other: this.$t('i18n_76530bff27')
      },
      tableSelections: [],
      envVarList: [],

      viewBuildVisible: false,
      confirmLoading: false,
      importLoading: false
    }
  },
  computed: {
    // 分页
    pagination() {
      return COMPUTED_PAGINATION(this.listQuery)
    },
    reposPagination() {
      return COMPUTED_PAGINATION(this.giteeImportForm, this.PAGE_DEFAULT_SIZW_OPTIONS)
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
        type: 'radio'
      }
    }
  },
  watch: {},
  created() {
    this.loadData()
    //
    providerInfo().then((response) => {
      if (response.code === 200) {
        this.providerData = response.data
      }
    })
    this.getWorkEnvList()
    this.loadGroupList()

    if (this.chooseVal) {
      this.tableSelections = [this.chooseVal]
    }
  },
  methods: {
    CHANGE_PAGE,
    // 分组数据
    loadGroupList() {
      listRepositoryGroup().then((res) => {
        if (res.data) {
          this.groupList = res.data
        }
      })
    },
    getWorkEnvList() {
      getWorkspaceEnvAll({
        workspaceId: this.workspaceId + (this.global ? ',GLOBAL' : '')
      }).then((res) => {
        if (res.code === 200) {
          this.envVarList = res.data
        }
      })
    },
    // 加载数据
    loadData(pointerEvent) {
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page
      this.loading = true
      if (this.workspaceId) {
        this.listQuery = { ...this.listQuery, workspaceId: this.workspaceId }
      }
      getRepositoryList(this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data.result
          this.listQuery.total = res.data.total
        }
        this.loading = false
      })
    },
    importChange(value) {
      this.giteeImportForm.address = this.providerData[value].baseUrl
    },
    // // 筛选
    // handleFilter() {
    //   this.loadData();
    // },
    // 新增
    handleAdd() {
      this.temp = {
        repoType: 0,
        protocol: 0
      }
      if (!this.global) {
        this.temp = { ...this.temp, workspaceId: 'GLOBAL', global: true }
      }

      this.editVisible = true
    },
    handleAddGitee() {
      this.giteeImportVisible = true
    },
    // 下载导入模板
    handlerImportTemplate() {
      window.open(importTemplate(), '_blank')
    },
    handlerExportData() {
      window.open(exportData({ ...this.listQuery, workspaceId: this.workspaceId }), '_blank')
    },
    beforeUpload({ file, onFinish, onError }) {
      const formData = new FormData()
      formData.append('file', file.file)
      formData.append('workspaceId', this.workspaceId)
      importData(formData)
        .then((res) => {
          if (res.code === 200) {
            $notification.success({
              message: res.msg
            })
            this.loadData()
            onFinish && onFinish()
          }
        })
        .catch(onError)
    },
    handleGiteeImportFormOk() {
      this.$refs['giteeImportForm'].validate().then(() => {
        this.importLoading = true
        authorizeRepos(this.giteeImportForm)
          .then((res) => {
            if (res.code === 200) {
              // 成功
              //this.username = res.data.username;
              this.giteeImportForm.total = res.data.total
              this.repos = res.data.result
            }
          })
          .finally(() => {
            this.importLoading = false
          })
      }).catch(() => {})
    },
    reposChange(pagination) {
      this.giteeImportForm.page = pagination.current
      this.giteeImportForm.limit = pagination.pageSize
      this.handleGiteeImportFormOk()
    },
    handleGiteeRepoAdd(record) {
      let data = {
        repoType: 0,
        protocol: 0,
        userName: record.username,
        password: this.giteeImportForm.token,
        name: record.name,
        gitUrl: record.url
      }
      if (!this.global) {
        data = { ...data, workspaceId: 'GLOBAL', global: true }
      }
      editRepository(data).then((res) => {
        if (res.code === 200) {
          // 成功
          $notification.success({
            message: res.msg
          })
          record.exists = true
          this.loadData()
        }
      })
    },
    // 修改
    handleEdit(record) {
      this.temp = Object.assign({}, record)
      if (this.temp.protocol === undefined) {
        this.temp.protocol = this.temp.gitUrl.indexOf('http') > -1 ? 0 : 1
      }
      this.temp = {
        ...this.temp,
        global: record.workspaceId === 'GLOBAL',
        workspaceId: ''
      }
      this.editVisible = true
    },
    // 提交节点数据
    handleEditOk() {
      // 检验表单
      this.$refs['editForm'].validate().then(() => {
        // 提交数据
        this.confirmLoading = true
        editRepository(this.temp)
          .then((res) => {
            if (res.code === 200) {
              // 成功
              $notification.success({
                message: res.msg
              })
              this.editVisible = false
              this.loadData()
              this.$refs['editForm'].restoreValidation()
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
        content: this.$t('i18n_7dfc7448ec'),
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
        zIndex: 1009,
        onOk: () => {
          return deleteRepository({
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

    // 清除隐藏字段
    restHideField(record) {
      $confirm({
        title: this.$t('i18n_c4535759ee'),
        content: this.$t('i18n_664c205cc3'),
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
        zIndex: 1009,
        onOk: () => {
          return restHideField(record.id).then((res) => {
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

    // 排序
    sortItemHander(record, index, method) {
      const msgData = {
        top: this.$t('i18n_0079d91f95'),
        up: this.$t('i18n_b166a66d67'),
        down: this.$t('i18n_7a7e25e9eb')
      }
      let msg = msgData[method] || this.$t('i18n_49574eee58')
      if (!record.sortValue) {
        msg += ` ${this.$t('i18n_57c0a41ec6')},${this.$t('i18n_066f903d75')},${this.$t('i18n_c4e2cd2266')}`
      }
      // console.log(this.list, index, this.list[method === "top" ? index : method === "up" ? index - 1 : index + 1]);
      const compareId = this.list[method === 'top' ? index : method === 'up' ? index - 1 : index + 1].id
      $confirm({
        title: this.$t('i18n_c4535759ee'),
        content: msg,
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
        zIndex: 1009,
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
              return false
            }
          })
        }
      })
    },
    // 确认
    handerConfirm() {
      if (!this.tableSelections.length) {
        $notification.warning({
          message: this.$t('i18n_be381ac957')
        })
        return
      }
      const selectData = this.list.filter((item) => {
        return item.id === this.tableSelections[0]
      })[0]

      this.$emit('confirm', `${selectData.id}`)
    },
    // 查看关联构建
    viewBuild(data) {
      this.temp = { id: data.id }
      this.viewBuildVisible = true
    }
  }
}
</script>
<style scoped>
/* .filter {
  margin-bottom: 10px;
}

.btn-add {
  margin-left: 10px;
  margin-right: 0;
} */
</style>
