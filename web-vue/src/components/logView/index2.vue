<template>
  <div
    :style="{
      marginTop: marginTop,
      minHeight: height,
      height: height
    }"
  >
    <div class="log-filter">
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
    </div>
    <!-- <pre class="log-view" :id="`${this.id}`" :style="`height:${this.height}`">{{ defText }}</pre> -->
    <viewPre ref="viewPre" :height="`calc(${height} - 35px - 20px)`" :config="temp"></viewPre>
  </div>
</template>
<script>
import { DeleteOutlined } from '@ant-design/icons-vue'

import viewPre from './view-pre.vue'

export default {
  // name: 'LogView',
  components: {
    viewPre
  },
  props: {
    height: {
      type: String,
      default: '50vh'
    },
    marginTop: {
      type: String,
      default: '0'
    },
    extendBar: {
      type: Boolean,
      default: true
    }
  },
  data() {
    return {
      temp: {
        logScroll: true,
        // 自动换行
        wordBreak: false
      }
    }
  },
  computed: {
    regModifier() {
      return this.regModifiers.join('')
    }
  },
  mounted() {
    const cacehJson = localStorage.getItem('log-view-cache') || '{}'
    try {
      const cacheData = JSON.parse(cacehJson)
      this.temp = Object.assign({}, this.temp, cacheData)
    } catch (e) {
      console.error(e)
    }
  },
  methods: {
    appendLine(data) {
      this.$refs.viewPre.appendLine(data)
    },
    clearLogCache() {
      this.$refs.viewPre.clearLogCache()
    },
    onChange() {
      localStorage.setItem('log-view-cache', JSON.stringify(this.temp))
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
:deep(.n-checkbox) {
  display: flex;
  align-items: center;
}
</style>
