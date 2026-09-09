<template>
  <div>
    <n-card :bordered="false" class="about-card">
      <div class="about-logo">
        <n-h2 style="margin: 0">Voyager1</n-h2>
        <n-tag type="info" size="small">Java Project Online Manage</n-tag>
      </div>
      <n-divider />
      <n-descriptions :column="1" label-placement="left" size="small">
        <n-descriptions-item :label="$t('i18n_fe2df04a16')">{{ info.v || '-' }}</n-descriptions-item>
        <n-descriptions-item :label="$t('i18n_5f3bebeddf')">{{ info.t || '-' }}</n-descriptions-item>
        <n-descriptions-item :label="$t('i18n_7f3a9c2e51')">{{ info.e || '-' }}</n-descriptions-item>
      </n-descriptions>
      <n-divider />
      <n-text depth="3" style="font-size: 12px">
        {{ $t('i18n_2f0e4aa9f4') }}
      </n-text>
      <br />
      <n-text depth="3" style="font-size: 12px">Licensed under the Apache License, Version 2.0</n-text>
    </n-card>
  </div>
</template>

<script lang="ts" setup>
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { pageBuildInfo } from '@/utils/const'

const { t: $t } = useI18n()
const info = ref<Record<string, any>>({})

onMounted(() => {
  info.value = pageBuildInfo()
})
</script>

<style scoped>
.about-card {
  max-width: 520px;
  margin: 24px auto;
}
.about-logo {
  text-align: center;
  padding: 12px 0;
}
</style>
