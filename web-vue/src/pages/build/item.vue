<template>
  <div>
    <!-- 编辑区 -->
    <CustomDrawer
      destroy-on-close
      :open="true"
      :body-style="{
        padding: '0'
      }"
      :header-style="{
        padding: '0 10px'
      }"
      width="70vw"
      :mask-closable="false"
      :footer-style="{ textAlign: 'right' }"
      @close="
        () => {
          $emit('close')
        }
      "
    >
      <template #title>
        <template v-if="id">
          <n-menu
            v-model:value="menuKey"
            mode="horizontal"
            class="menu"
            :options="infoMenuOptions"
            @update:value="menuClick"
          />
        </template>
        <template v-else>
          <n-menu
            v-model:value="menuKey"
            mode="horizontal"
            class="menu"
            :options="addMenuOptions"
            @update:value="menuClick"
          />
        </template>
      </template>

      <div class="layout-content">
        <detailsPage v-if="id" v-show="menuKey.includes('info')" :id="id" />
        <editBuildPage
          v-show="menuKey.includes('edit')"
          :id="id"
          ref="editBuild"
          v-model:edit-steps="stepsCurrent"
          :data="data"
          @confirm="
            (build, buildId, buildEnvParameter) => {
              $emit('build', build, buildId, buildEnvParameter)
            }
          "
          @change-edit-steps="
            (current) => {
              stepsCurrent = current
            }
          "
          @change-build-mode="
            (buildMode1) => {
              buildMode = buildMode1
              getEnvironmentList()
            }
          "
          @save-change="
            (loading) => {
              saveLoading = loading
            }
          "
        ></editBuildPage>
        <triggerPage v-if="id" v-show="menuKey.includes('trigger')" :id="id" />

        <div v-show="menuKey.includes('environment')">
          <n-list size="small" bordered :data="Object.keys(environment)">
            <template #renderItem="{ item }">
              <n-list-item style="display: block">
                <n-grid :wrap="true">
                  <n-grid-item :span="12" :flex="12" class="text-overflow-hidden">
                    <n-tooltip placement="topLeft">
                      <template #trigger>
                        {{ item }}
                      </template>
                      item
                    </n-tooltip>
                  </n-grid-item>

                  <n-grid-item :span="12" :flex="12" class="text-overflow-hidden">
                    <n-tooltip placement="topLeft">
                      <template #trigger>
                        <span class="tw">
                          <EyeInvisibleOutlined v-if="environment[item].privacy" />
                          <CodeOutlined v-if="environment[item].system" />
                          {{ environment[item].value }}
                        </span>
                      </template>
                      environment[item].privacy ? $t('i18n_b12d003367') : environment[item].value
                    </n-tooltip>
                  </n-grid-item>
                </n-grid>
              </n-list-item>
            </template>
            <template #header>
              <b>{{ $t('i18n_c0ad27a701') }}</b>
              <n-alert type="warning">
                <template #message>
                  <div>{{ $t('i18n_f11569cfa9') }}</div>
                  <div>
                    {{ $t('i18n_a2741f6eb3')
                    }}<n-tag v-for="(item, index) in privacyVariableKeywords" :key="index">{{ item }}</n-tag
                    >{{ $t('i18n_a17b905126') }}
                  </div>
                </template>
              </n-alert>
            </template>
            <!-- <template #footer>
        <div>Footer</div>
      </template> -->
          </n-list>
        </div>
      </div>
      <!-- <template> </template> -->

      <template v-if="menuKey.includes('edit')" #footer>
        <n-space>
          <n-button
            @click="
              () => {
                $emit('close')
              }
            "
          >
            {{ $t('i18n_625fb26b4b') }}
          </n-button>
          <n-tooltip v-if="id">
            <template #trigger>
              <span class="tw">
                <n-button :loading="saveLoading" @click="$refs.editBuild.refresh()">
                  {{ $t('i18n_694fc5efa9') }}</n-button
                >
              </span>
            </template>
            $t('i18n_18c7e2556e')
          </n-tooltip>
          <n-divider type="vertical" />
          <n-button
            type="primary"
            :disabled="stepsCurrent === 0"
            @click="
              () => {
                stepsCurrent = stepsCurrent - 1
              }
            "
            >{{ $t('i18n_eeb6908870') }}</n-button
          >
          <n-button
            type="primary"
            :disabled="stepsCurrent === 4"
            @click="
              () => {
                stepsCurrent = stepsCurrent + 1
              }
            "
            >{{ $t('i18n_38ce27d846') }}</n-button
          >
          <n-divider type="vertical" />

          <n-button type="primary" :loading="saveLoading" @click="$refs.editBuild.handleEditBuildOk(false)">
            {{ $t('i18n_be5fbbe34c') }}
          </n-button>
          <n-button type="primary" :loading="saveLoading" @click="$refs.editBuild.handleEditBuildOk(true)">
            {{ $t('i18n_a577822cdd') }}
          </n-button>
        </n-space>
      </template>
    </CustomDrawer>
  </div>
</template>
<script>
import { CodeOutlined, EyeInvisibleOutlined } from '@ant-design/icons-vue'

import { h } from 'vue'
import { NIcon } from 'naive-ui'
import { ApiOutlined, EditOutlined, InfoOutlined, UnorderedListOutlined } from '@ant-design/icons-vue'

import detailsPage from './details.vue'
import editBuildPage from './edit.vue'
import triggerPage from './trigger.vue'
import { getBuildEnvironment } from '@/api/build-info'
export default {
  components: {
    detailsPage,
    triggerPage,
    editBuildPage
  },
  props: {
    id: {
      type: String,
      default: ''
    },
    visibleType: {
      type: Number,
      default: 0
    },
    data: {
      type: Object,
      default: null
    },
    editSteps: {
      type: Number,
      default: 0
    }
  },
  emits: ['close', 'build'],
  data() {
    return {
      menuKey: ['info'],
      stepsCurrent: this.editSteps,
      environment: {},
      privacyVariableKeywords: [],
      buildMode: null,
      saveLoading: false
    }
  },
  computed: {
    // 菜单 options 需在脚本作用域生成（模板内联引用不到模块导入的 h/NIcon）
    infoMenuOptions() {
      return [
        {
          label: this.$t('i18n_224aef211c'),
          key: 'info',
          icon: () => h(NIcon, null, { default: () => h(InfoOutlined) })
        },
        {
          label: this.$t('i18n_e54c5ecb54'),
          key: 'edit',
          icon: () => h(NIcon, null, { default: () => h(EditOutlined) })
        },
        {
          label: this.$t('i18n_4696724ed3'),
          key: 'trigger',
          icon: () => h(NIcon, null, { default: () => h(ApiOutlined) })
        },
        {
          label: this.$t('i18n_3867e350eb'),
          key: 'environment',
          icon: () => h(NIcon, null, { default: () => h(UnorderedListOutlined) })
        }
      ]
    },
    addMenuOptions() {
      return [
        {
          label: this.$t('i18n_44a6891817'),
          key: 'edit',
          icon: () => h(NIcon, null, { default: () => h(EditOutlined) })
        },
        {
          label: this.$t('i18n_3867e350eb'),
          key: 'environment',
          icon: () => h(NIcon, null, { default: () => h(UnorderedListOutlined) })
        }
      ]
    }
  },
  created() {
    const array = ['info', 'edit', 'trigger']
    if (this.id) {
      this.menuKey = [array[this.visibleType - 1]]
    } else {
      this.menuKey = [array[1]]
    }
    this.getEnvironmentList()
  },
  methods: {
    menuClick(item) {
      this.menuKey = item.key
    },
    onClose() {
      this.$emit('close')
    },
    // 获取可用环境变量
    getEnvironmentList() {
      // 构建基础信息
      getBuildEnvironment({
        id: this.id,
        buildMode: this.buildMode
      }).then((res) => {
        if (res.data) {
          this.environment = res.data?.data || {}
          this.privacyVariableKeywords = res.data?.privacyVariableKeywords || []
        }
      })
    }
  }
}
</script>
<style scoped>
.menu {
  border-bottom: 0;
}

.layout-content {
  padding: 0;
  margin: 15px;
}
</style>
