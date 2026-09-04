<template>
  <div>
    <n-space direction="vertical" style="width: 100%">
      <n-alert :title="$t('i18n_c8c6e37071')" type="info" show-icon>
        <template #description>
          <ul>
            <li>{{ $t('i18n_cf38e8f9fd') }}</li>
            <li>{{ $t('i18n_a4f629041c') }}</li>
          </ul>
        </template>
      </n-alert>
      <!-- <n-alert title=",不支持软链" type="info" /> -->

      <n-form ref="editForm" :model="temp" @submit.prevent="onSubmit">
        <n-form-item :label="$t('i18n_28e1eec677')" path="outGiving">
          <template #help>{{ $t('i18n_5b1f0fd370') }}</template>
          <n-input
            v-model:value="temp.outGiving"
            type="textarea"
            :rows="5"
            style="resize: none"
            :placeholder="$t('i18n_9b78491b25')"
          />
        </n-form-item>
        <n-form-item :label="$t('i18n_6f7ee71e77')" path="staticDir">
          <template #help>{{ $t('i18n_3f8cedd1d7') }}</template>
          <n-input
            v-model:value="temp.staticDir"
            type="textarea"
            :rows="5"
            style="resize: none"
            :placeholder="$t('i18n_ec7ef29bdf')"
          />
        </n-form-item>
        <n-form-item :label="$t('i18n_95dbee0207')" path="allowRemoteDownloadHost">
          <template #help>{{ $t('i18n_aadf9d7028') }}</template>
          <n-input
            v-model:value="temp.allowRemoteDownloadHost"
            type="textarea"
            :rows="5"
            style="resize: none"
            :placeholder="$t('i18n_c32e7adb20')"
          />
        </n-form-item>
        <n-form-item>
          <n-button type="primary" attr-type="submit" :disabled="submitAble">{{ $t('i18n_939d5345ad') }}</n-button>
        </n-form-item>
      </n-form>
    </n-space>
  </div>
</template>
<script>
import { getDispatchWhiteList, editDispatchWhiteList } from '@/api/dispatch'
export default {
  props: {
    workspaceId: {
      type: String,
      default: ''
    }
  },
  emits: ['cancel'],
  data() {
    return {
      temp: {},
      submitAble: true
    }
  },
  mounted() {
    this.loadData()
  },
  methods: {
    // load data
    loadData() {
      this.loading = true
      getDispatchWhiteList({ workspaceId: this.workspaceId }).then((res) => {
        if (res.code === 200) {
          this.temp = res.data
          this.submitAble = false
        }
      })
    },
    // submit
    onSubmit() {
      // disabled submit button
      this.submitAble = true
      editDispatchWhiteList({
        ...this.temp,
        workspaceId: this.workspaceId
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
