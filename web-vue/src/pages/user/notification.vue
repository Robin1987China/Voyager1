<template>
  <n-form :model="temp">
    <n-form-item :label="$t('i18n_3fea7ca76c')" path="enabled">
      <n-switch
        v-model:value="temp.enabled"
        :checked-label="$t('i18n_cc42dd3170')"
        :unchecked-label="$t('i18n_b15d91274e')"
      />
    </n-form-item>
    <n-form-item :label="$t('i18n_32c65d8d74')" path="title">
      <n-input v-model:value="temp.title" type="text" :placeholder="$t('i18n_0728fee230')" />
      <template #help> {{ $t('i18n_d263a9207f') }}</template>
    </n-form-item>
    <n-form-item :label="$t('i18n_2d711b09bd')" path="content">
      <n-input type="textarea" :placeholder="$t('i18n_cca4454cf8')" />
      <template #help> {{ $t('i18n_d263a9207f') }}</template>
    </n-form-item>
    <n-form-item :label="$t('i18n_b15d91274e')" path="closable">
      <n-switch
        v-model:value="temp.closable"
        :checked-label="$t('i18n_faaa995a8b')"
        :unchecked-label="$t('i18n_0bf9f55e9d')"
      />
    </n-form-item>
    <n-form-item :label="$t('i18n_e78e4b2dc4')" path="enabled">
      <n-radio-group v-model:value="temp.level" name="radioGroup">
        <n-radio value="info">{{ $t('i18n_4b027f3979') }}</n-radio>
        <n-radio value="warning">{{ $t('i18n_900c70fa5f') }}</n-radio>
        <n-radio value="error">{{ $t('i18n_7030ff6470') }}</n-radio>
      </n-radio-group>
    </n-form-item>
    <n-form-item>
      <n-button type="primary" class="btn" @click="onSubmit()">{{ $t('i18n_be5fbbe34c') }}</n-button>
    </n-form-item>
  </n-form>
</template>
<script lang="ts" setup>
import { UserNotificationType, getUserNotification, saveUserNotification } from '@/api/user/user-notification'

import { useI18n } from 'vue-i18n'
const { t: $t } = useI18n()
const defaultValue = {
  level: 'info',
  closable: true,
  title: $t('i18n_1432c7fcdb'),
  enabled: false
} as UserNotificationType

const temp = ref<UserNotificationType>(defaultValue)

onMounted(() => {
  getUserNotification().then((res) => {
    if (res.code === 200) {
      if (Object.keys(res.data).length) {
        temp.value = res.data || defaultValue
      } else {
        temp.value = defaultValue
      }
    }
  })
})

const onSubmit = () => {
  saveUserNotification(temp.value).then((res) => {
    if (res.code === 200) {
      $notification.success({
        message: res.msg
      })
    }
  })
}
</script>
