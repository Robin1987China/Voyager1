<template>
  <div>
    <CustomModal
      destroy-on-close
      :open="true"
      :title="$t('i18n_c446efd80d')"
      :confirm-loading="confirmLoading"
      :mask-closable="false"
      width="80vw"
      @ok="handleEditScriptOk"
      @cancel="
        () => {
          $emit('close')
        }
      "
    >
      <n-alert
        v-if="!nodeList || !nodeList.length"
        :title="$t('i18n_4b027f3979')"
        type="warning"
        show-icon
        style="margin-bottom: 10px"
      >
        <template #description>{{ $t('i18n_50453eeb9e') }}</template>
      </n-alert>
      <n-form ref="editScriptForm" :rules="rules" :model="temp">
        <n-alert v-if="temp.scriptType === 'server-sync'" :title="$t('i18n_a33a2a4a90')" banner />
        <n-form-item :label="$t('i18n_7e2b40fc86')">
          <n-select
            v-model:value="temp.nodeId"
            :disabled="!!temp.nodeId"
            clearable
            :placeholder="$t('i18n_f8a613d247')"
            :options="nodeList.map((node) => ({ label: node.name, value: node.id }))"
          />
        </n-form-item>
        <template v-if="temp.nodeId">
          <n-form-item :label="$t('i18n_e747635151')" path="name">
            <n-input v-model:value="temp.name" :placeholder="$t('i18n_d7ec2d3fea')" />
          </n-form-item>
          <n-form-item :label="$t('i18n_4d9c3a0ed0')" path="context">
            <template #help>{{ $t('i18n_a77cc03013') }}</template>
            <n-form-item>
              <code-editor v-model:content="temp.context" height="40vh" :show-tool="true" :options="{ mode: 'shell' }">
                <template #tool_before>
                  <n-button text @click="scriptLibraryVisible = true">{{ $t('i18n_f685377a22') }}</n-button>
                </template>
              </code-editor>
            </n-form-item>
          </n-form-item>

          <n-form-item :label="$t('i18n_2171d1b07d')">
            <n-space style="width: 100%" direction="vertical">
              <n-grid v-for="(item, index) in commandParams" :key="item.key">
                <n-grid-item :span="22">
                  <n-space style="width: 100%" direction="vertical">
                    <n-input
                      v-model:value="item.desc"
                      :addon-before="$t('i18n_417fa2c2be', { index: index + 1 })"
                      :placeholder="`${$t('i18n_55721d321c')},${$t('i18n_2b1015e902')},${$t('i18n_72d4ade571')}`"
                    />
                    <n-input
                      v-model:value="item.value"
                      :addon-before="$t('i18n_620489518c', { index: index + 1 })"
                      :placeholder="`${$t('i18n_bfed4943c5')}${$t('i18n_e9f2c62e54')}`"
                    />
                  </n-space>
                </n-grid-item>
                <n-grid-item :span="2">
                  <n-grid type="flex" justify="center" align="middle">
                    <n-grid-item>
                      <MinusCircleOutlined style="color: #ff0000" @click="() => commandParams.splice(index, 1)" />
                    </n-grid-item>
                  </n-grid>
                </n-grid-item>
              </n-grid>
              <n-button type="primary" @click="() => commandParams.push({})">{{ $t('i18n_4c0eead6ff') }}</n-button>
            </n-space>
          </n-form-item>
          <n-form-item :label="$t('i18n_fffd3ce745')" path="global">
            <n-radio-group v-model:value="temp.global">
              <n-radio :value="true"> {{ $t('i18n_2be75b1044') }}</n-radio>
              <n-radio :value="false"> {{ $t('i18n_691b11e443') }}</n-radio>
            </n-radio-group>
          </n-form-item>
          <n-form-item :label="$t('i18n_6b2e348a2b')" path="autoExecCron">
            <n-auto-complete
              v-model:value="temp.autoExecCron"
              :placeholder="$t('i18n_5dff0d31d0')"
              :options="CRON_DATA_SOURCE"
            >
              <template #option="item"> {{ item.title }} {{ item.value }} </template>
            </n-auto-complete>
          </n-form-item>
          <n-form-item :label="$t('i18n_3bdd08adab')" path="description">
            <n-input
              v-model:value="temp.description"
              type="textarea"
              :rows="3"
              style="resize: none"
              :placeholder="$t('i18n_ae653ec180')"
            />
          </n-form-item>
        </template>
      </n-form>
    </CustomModal>
    <!-- pages.node.script-edit.a36f20d3 -->
    <CustomDrawer
      v-if="scriptLibraryVisible"
      destroy-on-close
      :title="$t('i18n_53bdd93fd6')"
      placement="right"
      :open="scriptLibraryVisible"
      width="85vw"
      :footer-style="{ textAlign: 'right' }"
      @close="
        () => {
          scriptLibraryVisible = false
        }
      "
    >
      <ScriptLibraryNoPermission
        v-if="scriptLibraryVisible"
        ref="scriptLibraryRef"
        @script-confirm="
          (script) => {
            temp = { ...temp, context: script }
            scriptLibraryVisible = false
          }
        "
        @tag-confirm="
          (tag) => {
            temp = { ...temp, context: (temp.context || '') + `\nG@(\&quot;${tag}\&quot;)` }
            scriptLibraryVisible = false
          }
        "
      ></ScriptLibraryNoPermission>
      <template #footer>
        <n-space>
          <n-button
            @click="
              () => {
                scriptLibraryVisible = false
              }
            "
            >{{ $t('i18n_625fb26b4b') }}</n-button
          >
          <n-button
            type="primary"
            @click="
              () => {
                $refs['scriptLibraryRef'].handerScriptConfirm()
              }
            "
            >{{ $t('i18n_f71316d0dd') }}</n-button
          >
          <n-button
            type="primary"
            @click="
              () => {
                $refs['scriptLibraryRef'].handerTagConfirm()
              }
            "
            >{{ $t('i18n_9300692fac') }}</n-button
          >
        </n-space>
      </template>
    </CustomDrawer>
  </div>
</template>
<script>
import { MinusCircleOutlined } from '@ant-design/icons-vue'

import codeEditor from '@/components/codeEditor'
import { editScript, itemScript } from '@/api/node-other'
import { CRON_DATA_SOURCE } from '@/utils/const-i18n'
import { getNodeListAll } from '@/api/node'
import ScriptLibraryNoPermission from '@/pages/system/assets/script-library/no-permission'
export default {
  components: {
    codeEditor,
    ScriptLibraryNoPermission
  },
  props: {
    nodeId: {
      type: String,
      default: undefined
    },
    scriptId: {
      type: String,
      default: ''
    }
  },
  emits: ['close'],
  data() {
    return {
      temp: {},
      CRON_DATA_SOURCE,
      commandParams: [],
      nodeList: [],
      rules: {
        name: [{ required: true, message: this.$t('i18n_fb7b9876a6'), trigger: 'blur' }],
        context: [{ required: true, message: this.$t('i18n_da1cb76e87'), trigger: 'blur' }]
      },
      confirmLoading: false,
      scriptLibraryVisible: false
    }
  },
  mounted() {
    // 查询节点
    getNodeListAll().then((res) => {
      if (res.code === 200 && res.data) {
        this.nodeList = res.data
        // res.data.forEach((item) => {
        //   this.nodeMap[item.id] = item.name;
        // });
      }
      this.handleEdit()
    })
  },
  methods: {
    // 修改
    handleEdit() {
      this.$refs['editScriptForm']?.restoreValidation()
      if (this.scriptId && this.nodeId) {
        itemScript({
          id: this.scriptId,
          nodeId: this.nodeId
        }).then((res) => {
          if (res.code === 200) {
            this.temp = Object.assign({}, res.data, {
              global: res.data.workspaceId === 'GLOBAL',
              workspaceId: ''
            })
            this.temp.nodeId = this.nodeId
            this.commandParams = this.temp.defArgs ? JSON.parse(this.temp.defArgs) : []
            //
          }
        })
      } else {
        this.temp = { global: false, type: 'add', nodeId: this.nodeId }
      }
    },
    // 提交 Script 数据
    handleEditScriptOk() {
      if (this.temp.scriptType === 'server-sync') {
        $notification.warning({
          message: this.$t('i18n_a33a2a4a90')
        })
        return
      }
      if (!this.temp.nodeId) {
        $notification.warning({
          message: this.$t('i18n_d1f0fac71d')
        })
        return
      }
      // 检验表单
      this.$refs['editScriptForm'].validate().then(() => {
        if (this.commandParams && this.commandParams.length > 0) {
          for (let i = 0; i < this.commandParams.length; i++) {
            if (!this.commandParams[i].desc) {
              $notification.error({
                message: this.$t('i18n_8ae2b9915c') + (i + 1) + this.$t('i18n_c583b707ba')
              })
              return false
            }
          }
          this.temp.defArgs = JSON.stringify(this.commandParams)
        } else {
          this.temp.defArgs = ''
        }
        // 提交数据
        this.confirmLoading = true
        editScript(this.temp)
          .then((res) => {
            if (res.code === 200) {
              // 成功
              $notification.success({
                message: res.msg
              })

              this.$emit('close')
            }
          })
          .finally(() => {
            this.confirmLoading = false
          })
      }).catch(() => {})
    }
  }
}
</script>
