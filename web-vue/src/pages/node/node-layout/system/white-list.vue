<template>
  <div>
    <n-alert style="margin-bottom: 20px" :title="$t('i18n_020f31f535')" type="info" />

    <n-form ref="editForm" :model="temp">
      <n-form-item :label="$t('i18n_aabdc3b7c0')" path="project">
        <n-input
          v-model:value="temp.project"
          type="textarea"
          :rows="5"
          style="resize: none"
          :placeholder="$t('i18n_631d5b88ab')"
        />
      </n-form-item>

      <n-form-item :label="$t('i18n_649231bdee')" path="allowEditSuffix">
        <n-input
          v-model:value="temp.allowEditSuffix"
          type="textarea"
          :rows="5"
          style="resize: none"
          :placeholder="$t('i18n_afa8980495')"
        />
      </n-form-item>
      <n-form-item>
        <n-button type="primary" :disabled="submitAble" @click="onSubmit">{{ $t('i18n_939d5345ad') }}</n-button>
      </n-form-item>
    </n-form>
  </div>
</template>
<script>
import { editWhiteList, getWhiteList } from '@/api/node-system'

export default {
  props: {
    machineId: {
      type: String,
      default: ''
    },
    nodeId: {
      type: String,
      default: ''
    }
  },
  emits: ['cancel'],
  data() {
    return {
      temp: {},
      submitAble: false
    }
  },
  mounted() {
    this.loadData()
  },
  methods: {
    // load data
    loadData() {
      getWhiteList({
        machineId: this.machineId,
        nodeId: this.nodeId
      }).then((res) => {
        if (res.code === 200) {
          this.temp = res.data
        }
      })
    },
    // submit
    onSubmit() {
      // disabled submit button
      this.submitAble = true

      editWhiteList({
        ...this.temp,
        machineId: this.machineId,
        nodeId: this.nodeId
      })
        .then((res) => {
          if (res.code === 200) {
            // 成功
            $notification.success({
              message: res.msg
            })
            this.$emit('cancel')
          }
        })
        .finally(() => {
          // button recover
          this.submitAble = false
        })
    }
  }
}
</script>
