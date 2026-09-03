<template>
  <div>
    <!-- 编辑区 -->
    <n-spin :tip="$t('i18n_2770db3a99')" :spinning="loading">
      <n-form ref="editProjectForm" :rules="rules" :model="temp">
        <n-form-item :label="$t('i18n_4fdd2213b5')" path="id">
          <template #help>{{ $t('i18n_e2b0f27424') }}</template>

          <n-input
            v-if="temp.type === 'edit'"
            v-model:value="temp.id"
            :max-length="50"
            :disabled="temp.type === 'edit'"
            :placeholder="$t('i18n_7ce511154f')"
          />
          <template v-else>
            <n-input
              v-model:value="temp.id"
              :max-length="50"
              :placeholder="$t('i18n_7ce511154f')"
              @keyup.enter="
                () => {
                  temp = { ...temp, id: randomStr(6) }
                }
              "
            >
              <template #enterButton>
                <n-button type="primary"> {{ $t('i18n_6709f4548f') }} </n-button>
              </template>
            </n-input>
          </template>
        </n-form-item>

        <n-form-item :label="$t('i18n_738a41f965')" path="name">
          <n-grid>
            <n-grid-item :span="10">
              <n-input v-model:value="temp.name" :max-length="50" :placeholder="$t('i18n_738a41f965')" />
            </n-grid-item>
            <n-grid-item :span="4" style="text-align: right">{{ $t('i18n_1b973fc4d1') }}</n-grid-item>
            <n-grid-item :span="10">
              <n-form-item>
                <custom-select
                  v-model:value="temp.group"
                  :max-length="50"
                  :data="groupList"
                  :input-placeholder="$t('i18n_bd0362bed3')"
                  :select-placeholder="$t('i18n_3e8c9c54ee')"
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
            </n-grid-item>
          </n-grid>
        </n-form-item>
        <n-form-item path="runMode">
          <template #label>
            <n-tooltip>
              <template #trigger>
                <span class="tw">
                  {{ $t('i18n_17d444b642') }}

                  <QuestionCircleOutlined v-show="temp.type !== 'edit'" />
                </span>
              </template>

              <ul>
                <li><b>Dsl</b> {{ $t('i18n_2356fe4af2') }}</li>
                <li><b>ClassPath</b> java -classpath xxx {{ $t('i18n_fa4aa1b93b') }}</li>
                <li><b>Jar</b> java -jar xxx {{ $t('i18n_fa4aa1b93b') }}</li>
                <li>
                  <b>JarWar</b> java -jar Springboot war
                  {{ $t('i18n_fa4aa1b93b') }}
                </li>
                <li>
                  <b>JavaExtDirsCp</b> java -Djava.ext.dirs=lib -cp conf:run.jar $MAIN_CLASS
                  {{ $t('i18n_fa4aa1b93b') }}
                </li>
                <li><b>File</b> {{ $t('i18n_5d6f47d670') }},{{ $t('i18n_61955b0e4b') }}</li>
              </ul>
            </n-tooltip>
          </template>
          <n-select
            v-model:value="temp.runMode"
            :placeholder="$t('i18n_26a3378645')"
            :options="runModeArray.map((item) => ({ label: `[${item.name}] ${item.desc}`, value: item.name }))"
            @update:value="changeRunMode"
          />
        </n-form-item>
        <template v-if="temp.runMode === 'Link'">
          <n-form-item :label="$t('i18n_be166de983')" path="linkId">
            <n-select
              v-model:value="temp.linkId"
              :placeholder="$t('i18n_1ba141c9ac')"
              :options="
                projectList.map((item) => ({
                  label: `[${item.runMode}] ${item.name}`,
                  value: item.projectId,
                  disabled: item.runMode === 'File' || item.runMode === 'Link'
                }))
              "
              @update:value="changeLinkId"
            />
          </n-form-item>
        </template>
        <template v-else>
          <n-form-item path="whitelistDirectory">
            <template #label>
              <n-tooltip>
                <template #trigger>
                  <span class="tw">
                    {{ $t('i18n_aabdc3b7c0') }}

                    <QuestionCircleOutlined v-show="temp.type !== 'edit'" />
                  </span>
                </template>

                <ul>
                  <li>{{ $t('i18n_f89cc4807e') }}</li>
                  <li>{{ $t('i18n_94763baf5f') }}</li>
                  <li>{{ $t('i18n_fe828cefd9') }}</li>
                  <li>
                    {{ $t('i18n_556499017a') }} <br />&nbsp;&nbsp;<b>{{ $t('i18n_67141abed6') }}</b>
                  </li>
                </ul>
              </n-tooltip>
            </template>
            <template #help>
              <div>
                {{ $t('i18n_fde1b6fb37') }}
                <n-button
                  text
                  size="small"
                  @click="
                    () => {
                      configDir = true
                    }
                  "
                >
                  <InfoCircleOutlined /> {{ $t('i18n_23b444d24c') }}
                </n-button>
              </div>
            </template>
            <n-input-group compact>
              <n-select
                v-model:value="temp.whitelistDirectory"
                style="width: 50%"
                :placeholder="$t('i18n_1d38b2b2bc')"
                :options="accessList"
              />
              <n-form-item>
                <n-input v-model:value="temp.lib" style="width: 50%" :placeholder="$t('i18n_1dc518bddb')" />
              </n-form-item>
            </n-input-group>
            <template #extra>
              <!-- <span class="lib-exist" v-show="temp.libExist">{{ temp.libExistMsg }}</span> -->
            </template>
          </n-form-item>

          <n-form-item v-show="filePath !== ''" :label="$t('i18n_8283f063d7')">
            <n-alert :title="filePath" type="success" />
          </n-form-item>
        </template>
        <n-form-item v-show="temp.runMode === 'Dsl'" path="dslContent">
          <template #label>
            <n-tooltip>
              <template #trigger>
                <span class="tw">
                  DSL {{ $t('i18n_2d711b09bd') }}

                  <QuestionCircleOutlined v-show="temp.type !== 'edit'" />
                </span>
              </template>

              <p>{{ $t('i18n_73d8160821') }}</p>
              <p>{{ $t('i18n_3517aa30c2') }}</p>
              <p>
                <b>status</b>
                {{ $t('i18n_ca69dad8fc') }}:$pid <b>$pid {{ $t('i18n_07a8af8c03') }}</b
                >{{ $t('i18n_d2f484ff7e') }}
              </p>
              <p>{{ $t('i18n_9f52492fbc') }}</p>
            </n-tooltip>
          </template>
          <template #help>
            <!-- <n-space>
              <template #split>
                <n-divider type="vertical" />
              </template> -->
            <div>
              scriptId{{ $t('i18n_21da885538') }}
              <n-button
                text
                size="small"
                @click="
                  () => {
                    drawerVisible = true
                  }
                "
              >
                {{ $t('i18n_35134b6f94') }}
              </n-button>
            </div>
            <div>{{ $t('i18n_6a359e2ab3') }}</div>
            <!-- </n-space> -->
          </template>
          <n-form-item>
            <code-editor
              v-show="dslEditTabKey === 'content'"
              v-model:content="temp.dslContent"
              height="40vh"
              :show-tool="true"
              :options="{ mode: 'yaml', tabSize: 2 }"
              :placeholder="$t('i18n_1c8190b0eb')"
            >
              <template #tool_before>
                <n-radio-group v-model:value="dslEditTabKey" size="small">
                  <n-radio-button value="content">DSL {{ $t('i18n_224e2ccda8') }}</n-radio-button>
                  <n-radio-button value="demo">{{ $t('i18n_da79c2ec32') }}</n-radio-button>
                </n-radio-group>
              </template>
            </code-editor>
            <code-editor
              v-show="dslEditTabKey === 'demo'"
              v-model:content="PROJECT_DSL_DEFATUL"
              height="40vh"
              :show-tool="true"
              :options="{ mode: 'yaml', tabSize: 2, readOnly: true }"
            >
              <template #tool_before>
                <n-radio-group v-model:value="dslEditTabKey" size="small">
                  <n-radio-button value="content">DSL {{ $t('i18n_224e2ccda8') }}</n-radio-button>
                  <n-radio-button value="demo">{{ $t('i18n_da79c2ec32') }}</n-radio-button>
                </n-radio-group>
              </template>
            </code-editor>
          </n-form-item>
        </n-form-item>
        <n-form-item v-show="noFileModes.includes(temp.runMode) && temp.runMode !== 'Link'">
          <template #label>
            <n-tooltip>
              <template #trigger>
                <span class="tw">
                  {{ $t('i18n_2ce44aba57') }}

                  <QuestionCircleOutlined v-show="temp.type !== 'edit'" />
                </span>
              </template>

              <ul>
                <li>{{ $t('i18n_12934d1828') }}</li>
                <li>{{ $t('i18n_138776a1dc') }}</li>
                <li>{{ $t('i18n_95c5c939e4') }}</li>
              </ul>
            </n-tooltip>
          </template>
          <n-select v-model:value="temp.logPath" :placeholder="$t('i18n_1d38b2b2bc')" :options="accessList" />
        </n-form-item>

        <n-form-item v-show="noFileModes.includes(temp.runMode) && temp.runMode !== 'Link'">
          <template #label>
            <n-tooltip>
              <template #trigger>
                <span class="tw">
                  {{ $t('i18n_340eb70415') }}
                  <QuestionCircleOutlined v-show="temp.type !== 'edit'" />
                </span>
              </template>

              <ul>
                <li>{{ $t('i18n_401c396b51') }}</li>
                <li>
                  {{ $t('i18n_aef1a0752a') }}
                </li>
              </ul>
            </n-tooltip>
          </template>
          <n-input v-model:value="temp.logCharset" :placeholder="$t('i18n_c6c2497dbe')" />
        </n-form-item>

        <n-form-item
          v-show="
            (javaModes.includes(temp.runMode) && temp.runMode !== 'Jar') ||
            (javaModes.includes(linkProjectData.runMode) && linkProjectData.runMode !== 'Jar')
          "
          label="Main Class"
          path="mainClass"
        >
          <n-input v-model:value="temp.mainClass" :placeholder="$t('i18n_ef800ed466')" />
        </n-form-item>
        <n-form-item
          v-show="
            (javaModes.includes(temp.runMode) && temp.runMode === 'JavaExtDirsCp') ||
            (javaModes.includes(linkProjectData.runMode) && linkProjectData.runMode === 'JavaExtDirsCp')
          "
          label="JavaExtDirsCp"
          path="javaExtDirsCp"
        >
          <n-input
            v-model:value="temp.javaExtDirsCp"
            :placeholder="`-Dext.dirs=xxx: -cp xx  ${$t('i18n_c53021f06d')}:xx】`"
          />
        </n-form-item>
        <n-form-item
          v-show="javaModes.includes(temp.runMode) || javaModes.includes(linkProjectData.runMode)"
          :label="$t('i18n_497bc3532b')"
          path="jvm"
        >
          <n-input
            v-model:value="temp.jvm"
            type="textarea"
            :auto-size="{ minRows: 3, maxRows: 3 }"
            :placeholder="$t('i18n_eef3653e9a', { slot1: $t('i18n_3d0a2df9ec'), slot2: $t('i18n_eb5bab1c31') })"
          />
        </n-form-item>
        <n-form-item
          v-show="javaModes.includes(temp.runMode) || javaModes.includes(linkProjectData.runMode)"
          :label="$t('i18n_e5098786d3')"
          path="args"
        >
          <n-input
            v-model:value="temp.args"
            type="textarea"
            :auto-size="{ minRows: 3, maxRows: 3 }"
            :placeholder="`Main ${$t('i18n_6a9231c3ba')}. ${$t('i18n_848e4e21da')}.port=8080`"
          />
        </n-form-item>
        <n-form-item
          v-if="temp.runMode === 'Dsl' || linkProjectData.runMode === 'Dsl'"
          path="dslEnv"
          :label="$t('i18n_fba5f4f19a')"
        >
          <!-- <n-input
            v-model:value="temp.dslEnv"
            placeholder="DSL{{$t('i18n_3867e350eb')}},{{$t('i18n_9324290bfe')}}=values1&keyvalue2"
          /> -->
          <parameter-widget v-model:value="temp.dslEnv"></parameter-widget>
        </n-form-item>

        <n-form-item v-show="noFileModes.includes(temp.runMode)" path="autoStart">
          <template #label>
            <n-tooltip>
              <template #trigger>
                <span class="tw">
                  {{ $t('i18n_8388c637f6') }}

                  <QuestionCircleOutlined v-show="temp.type !== 'edit'" />
                </span>
              </template>
              {{ $t('i18n_d4e03f60a9') }}
            </n-tooltip>
          </template>
          <template #help>
            <div>
              {{ $t('i18n_71584de972') }}<b>{{ $t('i18n_1e4a59829d') }}</b
              >{{ $t('i18n_0360fffb40') }}
            </div>
          </template>
          <div>
            <n-switch
              v-model:value="temp.autoStart"
              :checked-label="$t('i18n_8493205602')"
              :unchecked-label="$t('i18n_d58a55bcee')"
            />
            {{ $t('i18n_1022c545d1') }}
          </div>
        </n-form-item>

        <n-form-item path="disableScanDir">
          <template #label>
            <n-tooltip>
              <template #trigger> {{ $t('i18n_df59a2804d') }} </template>
            </n-tooltip>
          </template>
          <template #help>
            <div>{{ $t('i18n_b7c139ed75') }}</div>
          </template>
          <div>
            <n-switch
              v-model:value="temp.disableScanDir"
              :checked-label="$t('i18n_ced3d28cd1')"
              :unchecked-label="$t('i18n_56525d62ac')"
            />
          </div>
        </n-form-item>

        <n-form-item v-show="noFileModes.includes(temp.runMode)" path="token">
          <template #label>
            <n-tooltip>
              <template #trigger>
                <span class="tw">
                  WebHooks

                  <QuestionCircleOutlined v-show="temp.type !== 'edit'" />
                </span>
              </template>

              <ul>
                <li>{{ $t('i18n_a24d80c8fa') }}</li>
                <li>{{ $t('i18n_b91961bf0b') }}</li>
                <li>type {{ $t('i18n_5a63277941') }}</li>
                <li>DSL {{ $t('i18n_f8f456eb9a') }}</li>
              </ul>
            </n-tooltip>
          </template>
          <n-input v-model:value="temp.token" :placeholder="$t('i18n_6c776e9d91')" />
        </n-form-item>

        <n-form-item
          v-if="temp.runCommand"
          v-show="temp.type === 'edit' && javaModes.includes(temp.runMode)"
          :label="$t('i18n_ce559ba296')"
          path="runCommand"
        >
          <n-alert :title="temp.runCommand || $t('i18n_d81bb206a8')" type="success" />
        </n-form-item>
      </n-form>
    </n-spin>
    <!-- 配置节点授权目录 -->
    <CustomModal
      v-if="configDir"
      v-model:open="configDir"
      destroy-on-close
      :title="`${$t('i18n_eee6510292')}`"
      :footer="null"
      :mask-closable="false"
      @cancel="
        () => {
          configDir = false
        }
      "
    >
      <whiteList
        v-if="configDir"
        :node-id="nodeId"
        @cancel="
          () => {
            configDir = false
            loadAccesList()
          }
        "
      ></whiteList>
    </CustomModal>
    <!-- 管理节点 -->
    <NodeFunc
      v-if="drawerVisible"
      :id="nodeId"
      :name="$t('i18n_35134b6f94')"
      :tabs="['scripct']"
      @close="
        () => {
          drawerVisible = false
        }
      "
    ></NodeFunc>
  </div>
</template>
<script>
import { InfoCircleOutlined, QuestionCircleOutlined } from '@ant-design/icons-vue'

import CustomSelect from '@/components/customSelect'
import NodeFunc from '@/pages/node/node-func'
import codeEditor from '@/components/codeEditor'
import { randomStr } from '@/utils/const'
import { PROJECT_DSL_DEFATUL } from '@/utils/const-i18n'
import whiteList from '@/pages/node/node-layout/system/white-list.vue'

import {
  editProject,
  getProjectAccessList,
  getProjectData,
  javaModes,
  runModeArray,
  noFileModes,
  getProjectGroupAll
} from '@/api/node-project'
import { getProjectListAll } from '@/api/node'

export default {
  components: {
    CustomSelect,
    whiteList,
    codeEditor,
    NodeFunc
  },
  props: {
    projectId: {
      type: String,
      default: ''
    },
    nodeId: {
      type: String,
      default: ''
    },
    data: { type: Object, default: null }
  },
  emits: ['close'],
  data() {
    return {
      accessList: [],
      groupList: [],
      runModeArray,
      projectList: [],
      javaModes,
      noFileModes,
      PROJECT_DSL_DEFATUL,
      configDir: false,
      temp: {},
      drawerVisible: false,
      rules: {
        id: [{ required: true, message: this.$t('i18n_646a518953'), trigger: 'blur' }],

        name: [{ required: true, message: this.$t('i18n_4371e2b426'), trigger: 'blur' }],

        runMode: [{ required: true, message: this.$t('i18n_4310e9ed7d'), trigger: 'blur' }],

        whitelistDirectory: [{ required: true, message: this.$t('i18n_1d38b2b2bc'), trigger: 'blur' }],

        lib: [{ required: true, message: this.$t('i18n_d9657e2b5f'), trigger: 'blur' }]
      },
      linkProjectData: {},
      loading: true,
      dslEditTabKey: 'content'
    }
  },
  computed: {
    filePath() {
      return (this.temp.whitelistDirectory || '') + (this.temp.lib || '')
    }
  },
  watch: {
    nodeId: {
      deep: true,

      handler() {
        this.initData()
      },

      immediate: true
    }
  },
  mounted() {
    // this.initData();
  },
  methods: {
    randomStr,
    initData() {
      this.loadAccesList()
      this.loadGroupList()
      this.$refs['editProjectForm']?.resetFields()

      if (this.projectId) {
        // 修改
        const params = {
          id: this.projectId,
          nodeId: this.nodeId
        }

        getProjectData(params)
          .then((res) => {
            if (res.code === 200 && res.data) {
              this.temp = {
                ...res.data,
                type: 'edit'
              }
            } else {
              if (this.data) {
                // 复制项目
                this.temp = { ...this.temp, ...this.data, type: 'add' }
              }
            }
            if (this.temp.runMode === 'Link') {
              this.listProjectList()
            }
          })
          .finally(() => {
            this.loading = false
          })
      } else {
        // 新增
        this.temp = {
          type: 'add',
          logPath: ''
        }
        this.loading = false
      }
    },
    // 修改软链项目
    changeLinkId() {
      this.linkProjectData = this.projectList.find((item) => item.projectId === this.temp.linkId) || {}
    },
    // 修改运行模式
    changeRunMode() {
      if (this.temp.runMode === 'Link') {
        this.listProjectList()
      }
    },
    // 加载项目
    listProjectList() {
      if (this.projectList.length) {
        return
      }
      getProjectListAll({
        nodeId: this.nodeId
      }).then((res) => {
        if (res.code === 200) {
          this.projectList = res.data || []
          this.changeLinkId()
        }
      })
    },
    // 加载项目授权列表
    loadAccesList() {
      getProjectAccessList(this.nodeId).then((res) => {
        if (res.code === 200) {
          this.accessList = res.data
        }
      })
    },
    loadGroupList() {
      getProjectGroupAll().then((res) => {
        if (res.data) {
          this.groupList = res.data
        }
      })
    },

    // 提交
    handleOk() {
      return new Promise((resolve, reject) => {
        if (this.temp.outGivingProject) {
          $notification.warning({
            message: this.$t('i18n_869b506d66')
          })
          reject(false)
          return
        }
        // 检验表单
        this.$refs['editProjectForm']
          .validate()
          .then(() => {
            const params = {
              ...this.temp,
              nodeId: this.nodeId
            }
            // 删除旧数据
            delete params.javaCopyItemList
            editProject(params).then((res) => {
              if (res.code === 200) {
                $notification.success({
                  message: res.msg
                })
                resolve(true)
                this.$emit('close')
              } else {
                reject(false)
              }
            })
          })
          .catch(() => {
            reject(false)
          })
      })
    }

    // //检查节点是否存在
    // checkLibIndexExist() {
    //   // 检查是否输入完整
    //   if (this.temp.lib && this.temp.lib.length !== 0 && this.temp.whitelistDirectory && this.temp.whitelistDirectory.length !== 0) {
    //     const params = {
    //       nodeId: this.node.id,
    //       id: this.temp.id,
    //       newLib: this.temp.whitelistDirectory + this.temp.lib,
    //     };
    //     nodeJudgeLibExist(params).then((res) => {
    //       // if (res.code === 401) {
    //       //   this.temp = { ...this.temp, libExist: true, libExistMsg: res.msg };
    //       // }
    //       if (res.code !== 200) {
    //         $notification.warning({
    //           message: "提示",
    //           description: res.msg,
    //         });
    //         this.temp = { ...this.temp, libExist: true, libExistMsg: res.msg };
    //       } else {
    //         this.temp = { ...this.temp, libExist: false, libExistMsg: "" };
    //       }
    //     });
    //   }
    // },
    // handleReadFile() {

    // },
  }
}
</script>
