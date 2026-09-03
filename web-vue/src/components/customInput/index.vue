<template>
  <div>
    <template v-if="inputData.indexOf(refTag) === -1 && type == 'password'">
      <n-input
        v-model:value="inputData"
        type="password"
        :placeholder="placeholder"
        :disabled="!!selectData"
        @update:value="inputChange"
      >
        <template #prefix>
          <n-tooltip>
            <template #trigger>
              <n-select
                v-model:value="selectData"
                :placeholder="$t('i18n_3a1052ccfc')"
                :options="selectOptions"
                style="width: 120px"
                @update:value="selectChange"
              />
            </template>
            {{ $t('i18n_e6551a2295') }}
            <ul v-if="!envList.length">
              {{
                $t('i18n_7afb02ed93')
              }}
            </ul>
          </n-tooltip>
        </template>
      </n-input>
    </template>
    <template v-else>
      <n-input
        v-model:value="inputData"
        :placeholder="placeholder"
        :disabled="!!selectData"
        @update:value="inputChange"
      >
        <template #prefix>
          <n-tooltip>
            <template #trigger>
              <n-select
                v-model:value="selectData"
                :placeholder="$t('i18n_3a1052ccfc')"
                :options="selectOptions"
                style="width: 120px"
                @update:value="selectChange"
              />
            </template>
            {{ $t('i18n_e6551a2295') }}
            <ul v-if="!envList.length">
              {{
                $t('i18n_7afb02ed93')
              }}
            </ul>
          </n-tooltip>
        </template>
      </n-input>
    </template>
  </div>
</template>
<script>
import { t } from '@/i18n/index'
export default {
  components: {},
  props: {
    input: {
      type: String,
      default: ''
    },
    envList: {
      type: Array,
      default: () => []
    },
    placeholder: {
      type: String,
      default: function () {
        return t('i18n_101a86bc84')
      }
    },
    type: {
      type: String,
      default: 'password'
    }
  },
  emits: ['change'],
  data() {
    return {
      inputData: '',
      refTag: '$ref.wEnv.',
      selectData: null
    }
  },
  computed: {
    selectOptions() {
      return [
        { label: this.$t('i18n_e76e6a13dd'), value: '' },
        ...this.envList.map((item) => ({ label: item.name, value: item.name }))
      ]
    }
  },
  watch: {
    input: {
      deep: true,

      handler(v) {
        this.inputData = v
        if (v.indexOf(this.refTag) == -1) {
          this.selectData = null
        } else {
          // this.selectData = v.replace(this.refTag)
        }
      },

      immediate: true
    }
  },
  methods: {
    selectChange(v) {
      this.selectData = v
      const newV = v ? this.refTag + v : ''
      this.$emit('change', newV)
    },
    inputChange() {
      this.$emit('change', this.inputData)
    }
  }
}
</script>
