<template>
  <div>
    <n-form ref="releaseFileForm" :rules="releaseFileRules" :model="temp">
      <n-form-item :label="$t('i18n_ce23a42b47')" path="name">
        <n-input v-model:value="temp.name" :placeholder="$t('i18n_5f4c724e61')" :max-length="50" />
      </n-form-item>

      <n-form-item :label="$t('i18n_f98994f7ec')" path="taskType">
        <n-radio-group v-model:value="temp.taskType" @change="taskTypeChange">
          <n-radio :value="0"> SSH </n-radio>
          <n-radio :value="1"> {{ $t('i18n_3bf3c0a8d6') }} </n-radio>
        </n-radio-group>
        <template #help>
          <template v-if="temp.taskType === 0">{{ $t('i18n_28bf369f34') }} </template>
        </template>
      </n-form-item>

      <n-form-item v-if="temp.taskType === 0" path="taskDataIds" :label="$t('i18n_b188393ea7')">
        <n-grid>
          <n-grid-item :span="22">
            <n-select
              v-model:value="temp.taskDataIds"
              filterable
              multiple
              :placeholder="$t('i18n_260a3234f2')"
              :options="sshList.map((ssh) => ({ label: ssh.name, value: ssh.id }))"
            />
          </n-grid-item>
          <n-grid-item :span="1" style="margin-left: 10px">
            <ReloadOutlined @click="loadSshList" />
          </n-grid-item>
        </n-grid>
      </n-form-item>
      <n-form-item v-else-if="temp.taskType === 1" path="taskDataIds" :label="$t('i18n_473badc394')">
        <n-grid>
          <n-grid-item :span="22">
            <n-select
              v-model:value="temp.taskDataIds"
              filterable
              multiple
              :placeholder="$t('i18n_f8a613d247')"
              :options="nodeList.map((ssh) => ({ label: ssh.name, value: ssh.id }))"
            />
          </n-grid-item>
          <n-grid-item :span="1" style="margin-left: 10px">
            <ReloadOutlined @click="loadNodeList" />
          </n-grid-item>
        </n-grid>
      </n-form-item>

      <n-form-item path="releasePathParent" :label="$t('i18n_dbb2df00cf')">
        <template #help>
          <n-tooltip>
            <template #trigger>
              <span class="tw">
                <n-button
                  size="small"
                  text
                  @click="
                    () => {
                      configDir = true
                    }
                  "
                >
                  <InfoCircleOutlined />{{ $t('i18n_1e5533c401') }}
                </n-button>
              </span>
            </template>
            $t('i18n_bfe8fab5cd')
          </n-tooltip>
        </template>
        <n-input-group compact>
          <n-select
            v-model:value="temp.releasePathParent"
            filterable
            clearable
            style="width: 30%"
            :placeholder="$t('i18n_edd716f524')"
            :options="accessList"
          >
            <template #action>
              <ReloadOutlined @click="loadAccesList" />
            </template>
          </n-select>
          <n-form-item>
            <n-input
              v-model:value="temp.releasePathSecondary"
              style="width: 70%"
              :placeholder="$t('i18n_dc0d06f9c7')"
            />
          </n-form-item>
        </n-input-group>
      </n-form-item>

      <n-form-item path="releaseBeforeCommand">
        <template #label>
          {{ $t('i18n_cfb00269fd') }}
          <n-tooltip>
            <template #trigger>
              <QuestionCircleOutlined />
            </template>

            <ul>
              <li>{{ $t('i18n_799ac8bf40') }}</li>
              <li>{{ $t('i18n_5fbde027e3') }}</li>
              <li>{{ $t('i18n_a9c999e0bd') }}</li>
            </ul>
          </n-tooltip>
        </template>
        <template #help>
          <div v-if="scriptTabKey === 'before'">{{ $t('i18n_00de0ae1da') }}</div>
          <div v-else-if="scriptTabKey === 'after'">
            {{ $t('i18n_08ac1eace7') }}
          </div>
        </template>
        <n-form-item>
          <n-tabs v-model:value="scriptTabKey" tab-position="right" type="card">
            <n-tab-pane name="before" :tab="$t('i18n_d0c879f900')">
              <code-editor
                v-model:content="temp.beforeScript"
                height="40vh"
                :show-tool="true"
                :options="{
                  mode: 'shell'
                }"
              >
                <template #tool_before>
                  <n-space>
                    <n-tag>
                      <b>{{ $t('i18n_d0c879f900') }}</b>
                      {{ $t('i18n_1a6aa24e76') }}
                    </n-tag>
                    <n-button
                      text
                      @click="
                        () => {
                          chooseScriptVisible = 1
                        }
                      "
                      >{{ $t('i18n_54f271cd41') }}</n-button
                    >
                  </n-space>
                </template>
              </code-editor>
            </n-tab-pane>
            <n-tab-pane name="after" :tab="$t('i18n_9b1c5264a0')">
              <code-editor
                v-model:content="temp.afterScript"
                height="40vh"
                :show-tool="true"
                :options="{
                  mode: 'shell'
                }"
              >
                <template #tool_before>
                  <n-space>
                    <n-tag>{{ $t('i18n_e7ffc33d05') }}</n-tag>
                    <n-button
                      text
                      @click="
                        () => {
                          chooseScriptVisible = 2
                        }
                      "
                      >{{ $t('i18n_54f271cd41') }}</n-button
                    >
                  </n-space>
                </template>
              </code-editor>
            </n-tab-pane>
          </n-tabs>
        </n-form-item>
      </n-form-item>
      <n-form-item :label="$t('i18n_59cf15fe6b')" path="save2Template">
        <n-radio-group v-model:value="temp.save2Template">
          <n-radio value="">{{ $t('i18n_e2d8fba259') }}</n-radio>
          <n-radio value="id">{{ $t('i18n_f4273e1bb4') }}</n-radio>
          <n-radio value="alias" :disabled="fileType === 2">{{ $t('i18n_8351876236') }}</n-radio>
        </n-radio-group>
        <template #help>
          <div>
            {{ $t('i18n_ca527c48cf') }}
          </div>
          <div>
            {{ $t('i18n_6d110422ce') }}
          </div>
        </template>
      </n-form-item>
    </n-form>
    <!-- 配置授权目录 -->
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
        @cancel="
          () => {
            configDir = false
            loadAccesList()
          }
        "
      ></whiteList>
    </CustomModal>
    <!-- 选择脚本 -->
    <CustomDrawer
      v-if="chooseScriptVisible != 0"
      destroy-on-close
      :title="$t('i18n_a056d9c4b3')"
      placement="right"
      :open="chooseScriptVisible != 0"
      width="70vw"
      :footer-style="{ textAlign: 'right' }"
      @close="
        () => {
          chooseScriptVisible = 0
        }
      "
    >
      <scriptPage
        v-if="chooseScriptVisible"
        ref="scriptPage"
        choose="radio"
        :choose-val="
          chooseScriptVisible === 1
            ? temp.beforeScript?.indexOf('$ref.script.') !== -1
              ? temp.beforeScript?.replace('$ref.script.', '')
              : ''
            : temp.afterScript?.indexOf('$ref.script.') !== -1
              ? temp.afterScript?.replace('$ref.script.', '')
              : ''
        "
        mode="choose"
        @confirm="
          (id) => {
            if (chooseScriptVisible === 1) {
              temp = { ...temp, beforeScript: '$ref.script.' + id }
            } else if (chooseScriptVisible === 2) {
              temp = { ...temp, afterScript: '$ref.script.' + id }
            }
            chooseScriptVisible = 0
          }
        "
        @cancel="
          () => {
            chooseScriptVisible = 0
          }
        "
      ></scriptPage>
      <template #footer>
        <n-space>
          <n-button
            @click="
              () => {
                chooseScriptVisible = false
              }
            "
            >{{ $t('i18n_625fb26b4b') }}</n-button
          >
          <n-button
            type="primary"
            @click="
              () => {
                $refs['scriptPage'].handerConfirm()
              }
            "
            >{{ $t('i18n_e83a256e4f') }}</n-button
          >
        </n-space>
      </template>
    </CustomDrawer>
  </div>
</template>
<script>
import { InfoCircleOutlined, QuestionCircleOutlined, ReloadOutlined } from '@ant-design/icons-vue'

import { getSshListAll } from '@/api/ssh'
import { getDispatchWhiteList } from '@/api/dispatch'
import { getNodeListAll } from '@/api/node'
import codeEditor from '@/components/codeEditor'
import whiteList from '@/pages/dispatch/white-list.vue'
import scriptPage from '@/pages/script/script-list.vue'
import { getTaskTemplate } from '@/api/file-manager/release-task-log'
export default {
  components: {
    codeEditor,
    whiteList,
    scriptPage
  },
  props: {
    fileId: {
      type: String,
      default: ''
    },
    fileType: {
      type: Number,
      default: 0
    },
    alias: {
      type: String,
      default: ''
    }
  },
  emits: ['commit'],
  data() {
    return {
      temp: {},
      releaseFileRules: {
        name: [{ required: true, message: this.$t('i18n_89d18c88a3'), trigger: 'blur' }],

        taskType: [{ required: true, message: this.$t('i18n_29b48a76be'), trigger: 'blur' }],

        releasePath: [
          {
            required: true,
            message: this.$t('i18n_be28f10eb6'),
            trigger: 'blur'
          }
        ],

        taskDataIds: [{ required: true, type: 'array', message: this.$t('i18n_3e51d1bc9c'), trigger: ['blur', 'change'] }]
      },
      sshList: [],
      accessList: [],
      nodeList: [],
      configDir: false,
      scriptTabKey: 'before',
      chooseScriptVisible: 0
    }
  },
  created() {
    this.temp = { taskType: 0 }

    getTaskTemplate({
      id: this.fileId,
      alias: this.alias,
      fileType: this.fileType
    }).then((res) => {
      if (res.code === 200) {
        const data = JSON.parse(res.data?.data || '{}')
        if (data) {
          this.temp = {
            ...this.temp,
            ...data
          }
          this.taskTypeChange().then(() => {
            this.temp = {
              ...this.temp,
              taskDataIds: data.taskDataIds?.split(',')
            }
          })
        } else {
          this.taskTypeChange()
        }
      }
    })
    this.loadAccesList()
  },
  methods: {
    taskTypeChange() {
      const value = this.temp.taskType
      this.temp = { ...this.temp, taskDataIds: undefined }
      if (value === 0) {
        return this.loadSshList()
      } else if (value === 1) {
        return this.loadNodeList()
      }
      return new Promise((resolve) => {
        resolve()
      })
    },
    // 创建任务
    tryCommit() {
      this.$refs['releaseFileForm'].validate().then(() => {
        this.$emit('commit', {
          ...this.temp,
          taskDataIds: this.temp.taskDataIds?.join(',')
        })
      }).catch(() => {})
    },
    // 加载项目授权列表
    loadAccesList() {
      getDispatchWhiteList().then((res) => {
        if (res.code === 200) {
          this.accessList = res.data.outGivingArray || []
        }
      })
    },
    // 加载 SSH 列表
    loadSshList() {
      return new Promise((resolve, reject) => {
        this.sshList = []
        getSshListAll()
          .then((res) => {
            if (res.code === 200) {
              this.sshList = res.data
              resolve()
            }
          })
          .catch((err) => {
            reject(err)
          })
      })
    },
    // 加载节点
    loadNodeList() {
      return getNodeListAll().then((res) => {
        if (res.code === 200) {
          this.nodeList = res.data
        }
      })
    }
  }
}
</script>
<style scoped>
:deep(.n-tab-pane) {
  padding-right: 0 !important;
}
</style>
