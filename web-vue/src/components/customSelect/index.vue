<template>
  <div>
    <n-select
      v-model:value="selected"
      :style="selStyle"
      :disabled="disabled"
      filterable
      :placeholder="selectPlaceholder"
      :options="optionList"
      @update:value="selectChange"
    >
      <template v-if="canReload" #action> <ReloadOutlined @click="refreshSelect" /></template>
      <template #header>
        <n-space>
          <n-input ref="inputRef" v-model:value="selectInput" :maxlength="maxLength" :placeholder="inputPlaceholder" />
          <n-button text @click="addInput(selectInput)">
            <template #icon> <PlusOutlined /> </template>{{ $t('i18n_66ab5e9f24') }}
          </n-button>
          <slot name="suffix"></slot>
        </n-space>
        <n-divider />
      </template>
    </n-select>
  </div>
</template>
<script>
import { ReloadOutlined } from '@ant-design/icons-vue'

import { PlusOutlined } from '@ant-design/icons-vue'
import { t } from '@/i18n/index'
export default {
  components: {
    ReloadOutlined,
    PlusOutlined
  },
  props: {
    value: {
      type: [String, Number, Array],
      default: undefined
    },
    disabled: {
      type: Boolean,
      default: false
    },
    data: {
      type: Array,
      default: () => []
    },
    inputPlaceholder: {
      type: String,
      default: function () {
        return t('i18n_101a86bc84')
      }
    },
    selectPlaceholder: {
      type: String,
      default: function () {
        return t('i18n_708c9d6d2a')
      }
    },
    selStyle: { type: String, default: '' },

    maxLength: {
      type: Number,
      default: 200
    },
    canReload: {
      type: Boolean,
      default: false
    }
  },
  emits: ['update:value', 'onRefreshSelect', 'change', 'addOption'],

  data() {
    return {
      selectInput: '',

      optionList: [],
      selected: ''
    }
  },
  watch: {
    value: {
      handler(v) {
        this.selected = v
      },
      immediate: true
    },
    data: {
      handler(v) {
        this.optionList = this.normalizeOptions(v)
      },
      deep: true,
      immediate: true
    }
  },

  methods: {
    // naive n-select 需要 {label, value} 结构，兼容传入的字符串数组
    normalizeOptions(list) {
      return (list || []).map((item) => {
        if (item !== null && typeof item === 'object' && 'value' in item) {
          return item
        }
        return { label: String(item), value: item }
      })
    },
    selectChange(v) {
      this.$emit('update:value', v)
      this.$emit('change', v)
    },
    addInput(v) {
      if (!v) {
        return
      }
      let index = this.optionList.findIndex((item) => item.value === v)
      if (index === -1) {
        this.optionList = [...this.optionList, { label: String(v), value: v }]
      }
      this.selectInput = ''
      this.selected = v
      //
      this.selectChange(v)
      // 对外仍回传原始值数组，保持字符串数组契约
      this.$emit(
        'addOption',
        this.optionList.map((item) => item.value)
      )
    },
    refreshSelect() {
      this.$emit('onRefreshSelect')
    }
  }
}
</script>
