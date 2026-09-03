<template>
  <CustomModal
    v-if="visibleModel"
    v-model:open="visibleModel"
    destroy-on-close
    :width="style.width"
    :body-style="style.bodyStyle"
    :style="style.style"
    :footer="null"
    :mask-closable="false"
    @cancel="close"
  >
    <template #title>
      <n-page-header :title="titleName" :back-icon="false" style="padding: 0">
        <template #subTitle>
          <n-grid type="flex" align="middle">
            <n-grid-item>
              <slot name="before"></slot>
            </n-grid-item>

            <n-grid-item v-if="extendBar" style="padding-left: 10px">
              <n-space>
                <n-tooltip>
                  <template #trigger>
                    <span class="tw">
                      <n-button type="primary" size="small" @click="clearLogCache"
                        ><DeleteOutlined />{{ $t('i18n_288f0c404c') }}</n-button
                      >
                    </span>
                  </template>
                  $t('i18n_65f66dfe97')
                </n-tooltip>
                <!-- <n-tooltip>
<template #trigger>

                  <n-switch v-model="temp.wordBreak" checked-children="自动换行" un-checked-children="不换行" @change="onChange" />
                
</template>
内容超过边界自动换行
</n-tooltip> -->
                <n-tooltip>
                  <template #trigger>
                    <n-switch
                      v-model:value="temp.logScroll"
                      :checked-label="$t('i18n_e0ce74fcac')"
                      :unchecked-label="$t('i18n_18b34cf50d')"
                      @change="onChange"
                    />
                  </template>
                  $t('i18n_0693e17fc1')
                </n-tooltip>
              </n-space>
            </n-grid-item>
          </n-grid>
        </template>
      </n-page-header>
    </template>

    <viewPre ref="viewPre" :height="`calc(${style.bodyStyle.height} - 40px)`" :config="temp"></viewPre>
  </CustomModal>
</template>
<script>
import { DeleteOutlined } from '@ant-design/icons-vue'

import viewPre from './view-pre'
import { mapState } from 'pinia'
import { useGuideStore } from '@/stores/guide'
export default {
  name: 'LogView',
  components: {
    viewPre
    // VNodes: {
    //   functional: true,
    //   render: (h, ctx) => ctx.props.vnodes,
    // },
  },

  props: {
    titleName: {
      type: String,
      default: ''
    },
    marginTop: {
      type: String,
      default: '0'
    },
    extendBar: {
      type: Boolean,
      default: true
    },
    visible: {
      type: Boolean,
      default: false
    },
    // 调用方普遍传 :show=，此处兼容 show 别名
    show: {
      type: Boolean,
      default: false
    }
  },
  emits: ['close'],
  data() {
    return {
      temp: {
        logScroll: true,
        // 自动换行
        wordBreak: false
      },
      visibleModel: false
    }
  },
  computed: {
    ...mapState(useGuideStore, ['getFullscreenViewLogStyle']),
    regModifier() {
      return this.regModifiers.join('')
    },
    style() {
      return this.getFullscreenViewLogStyle()
    }
  },
  watch: {
    visible(v) {
      this.visibleModel = v
    },
    show(v) {
      this.visibleModel = v
    }
  },
  created() {
    this.visibleModel = this.visible || this.show
  },
  mounted() {
    const cacheJson = localStorage.getItem('log-view-cache') || '{}'
    try {
      const cacheData = JSON.parse(cacheJson)
      this.temp = Object.assign({}, this.temp, cacheData)
    } catch (e) {
      console.error(e)
    }
  },
  methods: {
    appendLine(data) {
      this.$refs.viewPre?.appendLine(data)
    },
    clearLogCache() {
      this.$refs.viewPre?.clearLogCache()
    },
    onChange() {
      localStorage.setItem('log-view-cache', JSON.stringify(this.temp))
    },
    close() {
      this.visibleModel = false
      this.$emit('close')
    }
  }
}
</script>
<style scoped>
.log-filter {
  padding: 0 10px;
  padding-top: 0;
  padding-bottom: 10px;
  line-height: 0;
}
</style>
